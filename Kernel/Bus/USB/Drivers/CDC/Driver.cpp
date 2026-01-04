/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
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
#include <Kernel/Net/USB/CDCECM.h>

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

struct Function {
    USB::USBInterface const& control_interface;
    Vector<u8> data_interface_ids;

    SubclassCode interface_sub_class() const
    {
        return SubclassCode { control_interface.descriptor().interface_sub_class_code };
    }

    CommunicationProtocolCode interface_protocol() const
    {
        return CommunicationProtocolCode { control_interface.descriptor().interface_protocol };
    }
};

struct Driver {
    StringView name;
    SubclassCode interface_sub_class;
    CommunicationProtocolCode interface_protocol;
    ClassSpecificInterfaceDescriptorCodes required_functional_descriptor;
    ErrorOr<void> (*driver_init)(USB::Device&, USB::USBInterface const&, Vector<u8> const&);
};

constexpr auto drivers = to_array<Driver>({
    { .name = "ECM"sv, .interface_sub_class = CDC::SubclassCode::EthernetNetworkingControlModel, .interface_protocol = CDC::CommunicationProtocolCode::NoSpecificProtocol, .required_functional_descriptor = CDC::ClassSpecificInterfaceDescriptorCodes::EthernetNetworking, .driver_init = &create_ecm_network_adapter },
});

static ErrorOr<bool> select_drivers(USB::Device& device, Vector<Function> functions)
{
    bool handled = false;
    for (auto const& function : functions) {
        for (auto const& driver : drivers) {
            if (function.interface_sub_class() != driver.interface_sub_class)
                continue;
            if (function.interface_protocol() != driver.interface_protocol)
                continue;

            // Check for required functional descriptor
            bool has_required_descriptor = false;
            TRY(function.control_interface.configuration().for_each_descriptor_in_interface(function.control_interface, [&](ReadonlyBytes raw_descriptor) -> ErrorOr<IterationDecision> {
                auto const& descriptor_header = *reinterpret_cast<USBDescriptorCommon const*>(raw_descriptor.data());
                if (descriptor_header.descriptor_type == static_cast<u8>(CDC::ClassSpecificDescriptorCodes::CS_Interface)) {
                    auto subtype = raw_descriptor[2];
                    if (subtype == static_cast<u8>(driver.required_functional_descriptor)) {
                        has_required_descriptor = true;
                        return IterationDecision::Break;
                    }
                }
                return IterationDecision::Continue;
            }));
            if (!has_required_descriptor)
                continue;

            dmesgln("USB CDC: Trying to initialize driver {}", driver.name);
            auto result = driver.driver_init(device, function.control_interface, function.data_interface_ids);
            if (result.is_error()) {
                dmesgln("USB CDC: Failed to initialize driver {}: {}", driver.name, result.error());
                continue;
            }
            handled = true;
        }
    }
    return handled;
}

static ErrorOr<bool> dump_configuration(USB::Device& device, USB::USBConfiguration const& configuration)
{
    dmesgln("USB CDC:   Configuration {}", configuration.descriptor().configuration_value);
    Vector<Function> functions;
    for (auto const& interface : configuration.interfaces()) {
        TRY(dump_interface(configuration, interface));

        if (interface.descriptor().interface_class_code != USB_CLASS_COMMUNICATIONS_AND_CDC_CONTROL) {
            continue;
        }

        Function function { interface, {} };
        // Find associated data interfaces
        // Those are linked via the Union Functional Descriptor
        TRY(configuration.for_each_descriptor_in_interface(interface, [&](ReadonlyBytes raw_descriptor) -> ErrorOr<IterationDecision> {
            auto const& descriptor_header = *reinterpret_cast<USBDescriptorCommon const*>(raw_descriptor.data());
            if (descriptor_header.descriptor_type == static_cast<u8>(CDC::ClassSpecificDescriptorCodes::CS_Interface)) {
                auto subtype = raw_descriptor[2];
                if (subtype == static_cast<u8>(CDC::ClassSpecificInterfaceDescriptorCodes::Union)) {
                    auto b_master_interface = raw_descriptor[3];
                    if (b_master_interface != interface.descriptor().interface_id) {
                        // Not sure why we would see this, but the spec seems to allow it
                        return IterationDecision::Continue;
                    }
                    Span data_interfaces = raw_descriptor.slice(sizeof(USBDescriptorCommon) + 2);
                    TRY(function.data_interface_ids.try_extend(data_interfaces));
                }
                return IterationDecision::Continue;
            }
            return IterationDecision::Continue;
        }));
        TRY(functions.try_append(move(function)));
    }

    return select_drivers(device, move(functions));
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
        handled |= TRY(dump_configuration(device, configuration));
    }

    if (handled)
        return {};
    return ENOTSUP;
}

}
