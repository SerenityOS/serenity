/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Find.h>
#include <Kernel/Bus/USB/Drivers/HID/Codes.h>
#include <Kernel/Bus/USB/Drivers/HID/MouseDriver.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBEndpoint.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/HID/Management.h>

namespace Kernel::USB {

// https://www.usb.org/sites/default/files/hid1_11.pdf

USB_DEVICE_DRIVER(MouseDriver);

void MouseDriver::init()
{
    auto driver = MUST(adopt_nonnull_lock_ref_or_enomem(new MouseDriver()));
    USBManagement::register_driver(driver);
}

ErrorOr<void> MouseDriver::checkout_interface(USB::Device& device, USBInterface const& interface)
{
    auto const& descriptor = interface.descriptor();
    if (descriptor.interface_class_code == USB_CLASS_HID
        && descriptor.interface_sub_class_code == to_underlying(HID::SubclassCode::BootProtocol)
        && descriptor.interface_protocol == to_underlying(HID::InterfaceProtocol::Mouse)) {
        dmesgln("USB HID Mouse Interface for device {:04x}:{:04x} found", device.device_descriptor().vendor_id, device.device_descriptor().product_id);
        return initialize_device(device, interface);
    }
    return ENOTSUP;
}

ErrorOr<void> MouseDriver::probe(USB::Device& device)
{
    if (device.device_descriptor().device_class != USB_CLASS_DEVICE
        || device.device_descriptor().device_sub_class != 0x00
        || device.device_descriptor().device_protocol != 0x00)
        return ENOTSUP;
    // FIXME: Are we guaranteed to have one USB configuration for a mouse device?
    if (device.configurations().size() != 1)
        return ENOTSUP;

    // FIXME: If we have multiple USB configurations for a mouse device, find the appropriate one.
    for (auto const& interface : device.configurations()[0].interfaces()) {
        if (!checkout_interface(device, interface).is_error())
            return {};
    }

    return ENOTSUP;
}

ErrorOr<void> MouseDriver::initialize_device(USB::Device& device, USBInterface const& interface)
{
    if (interface.endpoints().size() != 1)
        return ENOTSUP;
    // FIXME: Should we check other configurations?
    TRY(device.set_configuration_and_interface(interface));

    auto const& endpoint_descriptor = interface.endpoints()[0];
    auto interrupt_in_pipe = TRY(USB::InterruptInPipe::create(device.controller(), device, endpoint_descriptor.endpoint_address & 0xf, endpoint_descriptor.max_packet_size, 10));

    // We only support the boot protocol, so switch to it. By default the report protocol is used (see 7.2.6 Set_Protocol Request).
    TRY(device.control_transfer(
        USB_REQUEST_RECIPIENT_INTERFACE | USB_REQUEST_TYPE_CLASS | USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE,
        to_underlying(HID::Request::SET_PROTOCOL), to_underlying(HID::Protocol::Boot), interface.descriptor().interface_id, 0, nullptr));

    auto mouse_device = TRY(USBMouseDevice::try_create_instance(device, endpoint_descriptor.max_packet_size, move(interrupt_in_pipe)));
    HIDManagement::the().attach_standalone_hid_device(*mouse_device);
    m_interfaces.append(mouse_device);
    return {};
}

void MouseDriver::detach(USB::Device& device)
{
    auto&& mouse_device = AK::find_if(m_interfaces.begin(), m_interfaces.end(), [&device](auto& interface) { return &interface.device() == &device; });

    HIDManagement::the().detach_standalone_hid_device(*mouse_device);
    m_interfaces.remove(*mouse_device);
}

}
