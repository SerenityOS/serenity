/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/Drivers/USBDriver.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBEndpoint.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Devices/Input/HID/Device.h>

namespace Kernel::USB {

class TransportInterface final : public HID::TransportInterface {
public:
    static ErrorOr<NonnullOwnPtr<TransportInterface>> create(NonnullOwnPtr<InterruptInPipe> in_pipe)
    {
        return TRY(adopt_nonnull_own_or_enomem(new (nothrow) TransportInterface(move(in_pipe))));
    }

    virtual ErrorOr<void> start_receiving_input_reports(InputReportCallback&& callback) override
    {
        m_input_report_callback = move(callback);

        (void)TRY(m_in_pipe->submit_interrupt_in_transfer(m_in_pipe->max_packet_size(), m_in_pipe->poll_interval(), [max_packet_size = m_in_pipe->max_packet_size(), this](Transfer* transfer) {
            m_input_report_callback(ReadonlyBytes { transfer->buffer().as_ptr(), max_packet_size });
        }));

        return {};
    }

private:
    TransportInterface(NonnullOwnPtr<InterruptInPipe> in_pipe)
        : m_in_pipe(move(in_pipe))
    {
    }

    NonnullOwnPtr<InterruptInPipe> m_in_pipe;
    InputReportCallback m_input_report_callback;
};

class HIDInterface : public RefCounted<HIDInterface> {
public:
    static ErrorOr<NonnullRefPtr<HIDInterface>> create(USB::Device& usb_device, ::HID::ParsedReportDescriptor parsed_descriptor, NonnullOwnPtr<InterruptInPipe> in_pipe)
    {
        auto transport_interface = TRY(TransportInterface::create(move(in_pipe)));
        auto hid_device = TRY(HID::Device::create(move(transport_interface), move(parsed_descriptor)));
        return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) HIDInterface(usb_device, move(hid_device))));
    }

    USB::Device const& device() const { return m_usb_device; }

private:
    HIDInterface(USB::Device& usb_device, NonnullOwnPtr<HID::Device> hid_device)
        : m_usb_device(usb_device)
        , m_hid_device(move(hid_device))
    {
    }

    USB::Device& m_usb_device;
    NonnullOwnPtr<HID::Device> m_hid_device;

    IntrusiveListNode<HIDInterface, NonnullRefPtr<HIDInterface>> m_list_node;

public:
    using List = IntrusiveList<&HIDInterface::m_list_node>;
};

class HIDDriver final : public Driver {
public:
    HIDDriver()
        : Driver("USB HID"sv)
    {
    }

    static void init();

    virtual ~HIDDriver() override = default;

    virtual ErrorOr<void> probe(USB::Device&) override;
    virtual void detach(USB::Device&) override;

private:
    HIDInterface::List m_hid_interfaces;
};

USB_DEVICE_DRIVER(HIDDriver);

void HIDDriver::init()
{
    auto driver = MUST(adopt_nonnull_lock_ref_or_enomem(new (nothrow) HIDDriver()));
    USBManagement::register_driver(driver);
}

static constexpr u8 DESCRIPTOR_TYPE_HID = 0x21;
static constexpr u8 DESCRIPTOR_TYPE_HID_REPORT = 0x22;

struct HIDDescriptor {
    struct DescriptorInfo {
        u8 type;
        LittleEndian<u16> length;
    };

    USBDescriptorCommon descriptor_header;
    LittleEndian<u16> hid_version;
    u8 country_code;
    u8 number_of_descriptors;
    // FIXME: This should be a flexible array member, but GCC doesn't like it being one for some reason.
    //        We currently only use the first (guaranteed to be present) entry anyways.
    DescriptorInfo descriptor_info[1];
};
static_assert(AssertSize<HIDDescriptor, 9>());
static_assert(AssertSize<HIDDescriptor::DescriptorInfo, 3>());

static ErrorOr<NonnullRefPtr<HIDInterface>> initialize_hid_interface(USB::Device& device, USB::USBConfiguration const& configuration, USB::USBInterface const& interface)
{
    TRY(device.set_configuration_and_interface(interface));

    OwnPtr<InterruptInPipe> in_pipe;

    // FIXME: Make .endpoints() return USBEndpoints instead of USBEndpointDescriptors and use .is_interrupt() + .is_in().
    for (auto const& endpoint_descriptor : interface.endpoints()) {
        if (((endpoint_descriptor.endpoint_attributes_bitmap & USBEndpoint::ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_MASK) == USBEndpoint::ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_INTERRUPT)
            && ((endpoint_descriptor.endpoint_address & USBEndpoint::ENDPOINT_ADDRESS_DIRECTION_MASK) == USBEndpoint::ENDPOINT_ADDRESS_DIRECTION_IN)) {
            in_pipe = TRY(InterruptInPipe::create(device.controller(), device, endpoint_descriptor.endpoint_address & USBEndpoint::ENDPOINT_ADDRESS_NUMBER_MASK, endpoint_descriptor.max_packet_size, endpoint_descriptor.poll_interval_in_frames));
        }
    }

    if (!in_pipe) {
        dbgln("USB HID: No interrupt in endoint found");
        return EINVAL;
    }

    // Find the HID descriptor in the descriptor hierarchy for the interface. The HID descriptor contains the length of the HID report descriptor.
    Optional<size_t> report_descriptor_length;
    TRY(configuration.for_each_descriptor_in_interface(interface,
        [&report_descriptor_length](ReadonlyBytes descriptor_data) -> ErrorOr<IterationDecision> {
            auto const& descriptor_header = *reinterpret_cast<USBDescriptorCommon const*>(descriptor_data.data());

            // 7.1 Standard Requests: The HID descriptor shall be interleaved between the Interface and Endpoint descriptors for HID Interfaces.
            if (descriptor_header.descriptor_type == DESCRIPTOR_TYPE_ENDPOINT)
                return IterationDecision::Break;

            if (descriptor_header.descriptor_type != DESCRIPTOR_TYPE_HID)
                return IterationDecision::Continue;

            if (descriptor_data.size() < sizeof(HIDDescriptor)) {
                dbgln("USB HID: Invalid HID descriptor size");
                return EINVAL;
            }

            auto descriptor = *reinterpret_cast<HIDDescriptor const*>(descriptor_data.data());

            if (descriptor.number_of_descriptors < 1) {
                dbgln("USB HID: Invalid HID descriptor count");
                return EINVAL;
            }

            if (descriptor.descriptor_info[0].type != DESCRIPTOR_TYPE_HID_REPORT) {
                dbgln("USB HID: First descriptor is not a report descriptor");
                return EINVAL;
            }

            report_descriptor_length = descriptor.descriptor_info[0].length;

            return IterationDecision::Break;
        }));

    if (!report_descriptor_length.has_value()) {
        dbgln("USB HID: No HID descriptor found");
        return EINVAL;
    }

    auto report_descriptor_buffer = TRY(FixedArray<u8>::create(report_descriptor_length.release_value()));
    auto transfer_length = TRY(device.control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST | USB_REQUEST_TYPE_STANDARD | USB_REQUEST_RECIPIENT_INTERFACE,
        USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_HID_REPORT << 8), interface.descriptor().interface_id, report_descriptor_buffer.size(), report_descriptor_buffer.data()));

    if (transfer_length < report_descriptor_buffer.size())
        return EIO;

    ::HID::ReportDescriptorParser report_descriptor_parser { report_descriptor_buffer.span() };
    auto parsed_descriptor = TRY(report_descriptor_parser.parse());
    return TRY(HIDInterface::create(device, parsed_descriptor, in_pipe.release_nonnull()));
}

ErrorOr<void> HIDDriver::probe(USB::Device& device)
{
    for (auto const& configuration : device.configurations()) {
        for (auto const& interface : configuration.interfaces()) {
            if (interface.descriptor().interface_class_code != USB_CLASS_HID)
                continue;

            // Only use the default alternate setting for now.
            // FIXME: Maybe check other alternate settings?
            if (interface.descriptor().alternate_setting != 0)
                continue;

            dmesgln("USB HID: Found HID interface of device {:04x}:{:04x} at interface ID {}", device.device_descriptor().vendor_id, device.device_descriptor().product_id, interface.descriptor().interface_id);

            auto hid_interface_or_error = initialize_hid_interface(device, configuration, interface);
            if (hid_interface_or_error.is_error()) {
                dmesgln("USB HID: Failed to initialize interface {} of device {:04x}:{:04x}", interface.descriptor().interface_id, device.device_descriptor().vendor_id, device.device_descriptor().product_id);
                continue;
            }

            m_hid_interfaces.append(hid_interface_or_error.release_value());
        }

        // FIXME: We currently always use the first configuration with at least one HID interface that we successfully initialized.
        //        Maybe check other configurations as well?
        if (!m_hid_interfaces.is_empty())
            return {};
    }

    return ENOTSUP;
}

void HIDDriver::detach(USB::Device& device)
{
    auto&& hid_interface = AK::find_if(m_hid_interfaces.begin(), m_hid_interfaces.end(), [&device](auto& interface) { return &interface.device() == &device; });
    m_hid_interfaces.remove(*hid_interface);
}

}
