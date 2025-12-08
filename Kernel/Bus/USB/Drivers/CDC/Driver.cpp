/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/MemoryStream.h>
#include <Kernel/Bus/USB/Drivers/CDC/Codes.h>
#include <Kernel/Bus/USB/Drivers/USBDriver.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBInterface.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Library/Assertions.h>

namespace Kernel::USB::CDC {

class CDCDriver final : public Driver {
public:
    CDCDriver()
        : Driver("USB CDC"sv)
    {
    }

    static void init();

    virtual ~CDCDriver() override = default;

    virtual ErrorOr<void> probe(USB::Device&) override;
    virtual void detach(USB::Device&) override { }
};

USB_DEVICE_DRIVER(CDCDriver);

void CDCDriver::init()
{
    auto driver = MUST(adopt_nonnull_lock_ref_or_enomem(new (nothrow) CDCDriver()));
    MUST(USBManagement::register_driver(driver));
}

static ErrorOr<void> dump_cdc_data_interface(USB::USBConfiguration const&, USB::USBInterface const& interface)
{
    auto protocol_code = interface.descriptor().interface_protocol;

    dmesgln("USB CDC:       Data Interface, protocol: ({:#02X}) {}", protocol_code, protocol_code_to_string(CDC::DataProtocolCode { protocol_code }));
    dmesgln("USB CDC:       Endpoints: {}", interface.endpoints().size());

    return {};
}

static ErrorOr<void> dump_cdc_functional_descriptor(USB::USBConfiguration const& configuration, USB::USBInterface const&, ReadonlyBytes raw_descriptor)
{
    auto& device = const_cast<USB::Device&>(configuration.device()); // FIXME: Evil!!

    auto const& descriptor_header = *reinterpret_cast<USBDescriptorCommon const*>(raw_descriptor.data());
    VERIFY(descriptor_header.descriptor_type == static_cast<u8>(CDC::ClassSpecificDescriptorCodes::CS_Interface));

    auto subtype = raw_descriptor[2];
    dmesgln("USB CDC:         ({:#02X}) {}, length: {}", subtype, class_specific_interface_descriptor_to_string(static_cast<CDC::ClassSpecificInterfaceDescriptorCodes>(subtype)), descriptor_header.length);

    switch (static_cast<CDC::ClassSpecificInterfaceDescriptorCodes>(subtype)) {
    case CDC::ClassSpecificInterfaceDescriptorCodes::Union: {
        // Union Functional Descriptor
        // After the header and subtype, the first byte is the master/controlling interface,
        // followed by a list subordinate interfaces.
        auto master_interface = raw_descriptor[3];
        dmesgln("USB CDC:           Control Interface: {}", master_interface);
        dmesgln("USB CDC:           Subordinates: {}", raw_descriptor.slice(sizeof(USBDescriptorCommon) + 2));
    } break;
    case CDC::ClassSpecificInterfaceDescriptorCodes::EthernetNetworking: {
        auto stream = FixedMemoryStream(raw_descriptor.slice(sizeof(USBDescriptorCommon) + 1));
        u8 i_macaddr = TRY(stream.read_value<u8>());
        u32 bm_stats = TRY(stream.read_value<LittleEndian<u32>>());
        u16 w_max_segment_size = TRY(stream.read_value<LittleEndian<u16>>());
        u16 w_number_mc_filters = TRY(stream.read_value<LittleEndian<u16>>());
        u8 b_number_power_filters = TRY(stream.read_value<u8>());
        dmesgln("USB CDC:           MAC Address String Index: {}", i_macaddr);
        auto maybe_mac_address = device.get_string_descriptor(i_macaddr);
        if (maybe_mac_address.is_error()) {
            dmesgln("USB CDC:           Failed to get MAC address string descriptor");
        } else {
            dmesgln("USB CDC:           MAC Address: {}", maybe_mac_address.value()->view());
        }
        dmesgln("USB CDC:           Statistics Bitmap: {:#08X}", bm_stats);
        dmesgln("USB CDC:           Max Segment Size: {}", w_max_segment_size);
        if (w_number_mc_filters & (1 << 15)) {
            // Perfect filtering is supported
            dmesgln("USB CDC:           Number of Multicast Filters: Perfect filtering supported");
        } else {
            dmesgln("USB CDC:           Number of Multicast Filters: {}", w_number_mc_filters & ~(1 << 15));
        }
        dmesgln("USB CDC:           Number of Power Filters: {}", b_number_power_filters);
    } break;
    default:
        break;
    }
    return {};
}

static ErrorOr<void> dump_cdc_interface(USB::USBConfiguration const& configuration, USB::USBInterface const& interface)
{
    auto subclass_code = interface.descriptor().interface_sub_class_code;
    auto protocol_code = interface.descriptor().interface_protocol;

    dmesgln("USB CDC:       {}, protocol: ({:#02X}) {}", subclass_code_to_string(CDC::SubclassCode { subclass_code }), protocol_code, protocol_code_to_string(CDC::CommunicationProtocolCode { protocol_code }));
    dmesgln("USB CDC:       Endpoints: {}", interface.endpoints().size());

    dmesgln("USB CDC:       Functional Descriptors:");
    return configuration.for_each_descriptor_in_interface(interface, [&](ReadonlyBytes raw_descriptor) -> ErrorOr<IterationDecision> {
        auto const& descriptor_header = *reinterpret_cast<USBDescriptorCommon const*>(raw_descriptor.data());
        if (descriptor_header.descriptor_type == static_cast<u8>(CDC::ClassSpecificDescriptorCodes::CS_Interface))
            TRY(dump_cdc_functional_descriptor(configuration, interface, raw_descriptor));
        return IterationDecision::Continue;
    });
}

static ErrorOr<void> dump_interface(USB::USBConfiguration const& configuration, USB::USBInterface const& interface)
{
    if (interface.descriptor().interface_class_code != USB_CLASS_CDC_DATA && interface.descriptor().interface_class_code != USB_CLASS_COMMUNICATIONS_AND_CDC_CONTROL)
        return {};

    auto& device = const_cast<USB::Device&>(configuration.device()); // FIXME: Evil!!
    auto maybe_interface_string = device.get_string_descriptor(interface.descriptor().interface_string_descriptor_index);
    dmesgln("USB CDC:     Interface {}.{} ({})", interface.descriptor().interface_id, interface.descriptor().alternate_setting, maybe_interface_string.is_error() ? "No String Descriptor"sv : maybe_interface_string.value()->view());

    switch (interface.descriptor().interface_class_code) {
    case USB_CLASS_CDC_DATA:
        return dump_cdc_data_interface(configuration, interface);
    case USB_CLASS_COMMUNICATIONS_AND_CDC_CONTROL:
        return dump_cdc_interface(configuration, interface);
    default:
        VERIFY_NOT_REACHED();
    }
}

static ErrorOr<bool> dump_configuration(USB::USBConfiguration const& configuration)
{
    dmesgln("USB CDC:   Configuration {}", configuration.descriptor().configuration_value);
    for (auto const& interface : configuration.interfaces()) {
        TRY(dump_interface(configuration, interface));
    }

    return false;
}

ErrorOr<void> CDCDriver::probe(USB::Device& device)
{

    if (device.device_descriptor().device_class != USB_CLASS_COMMUNICATIONS_AND_CDC_CONTROL) {
        return ENOTSUP;
    }
    // Note: SubClass and Protocol should be 0,
    //       further classification is done on the interface level.
    bool handled = false;
    dmesgln("USB CDC: Found Device {}:{}", device.device_descriptor().vendor_id, device.device_descriptor().product_id);
    for (auto const& configuration : device.configurations()) {
        handled |= TRY(dump_configuration(configuration));
    }

    if (handled)
        return {};
    return ENOTSUP;
}

}
