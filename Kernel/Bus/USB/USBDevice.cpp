/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/StdLib.h>

namespace Kernel::USB {

ErrorOr<NonnullRefPtr<Device>> Device::try_create(USBController const& controller, u8 port, DeviceSpeed speed)
{
    auto pipe = TRY(Pipe::try_create_pipe(controller, Pipe::Type::Control, Pipe::Direction::Bidirectional, 0, 8, 0));
    auto device = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Device(controller, port, speed, move(pipe))));
    TRY(device->enumerate_device());
    return device;
}

Device::Device(USBController const& controller, u8 port, DeviceSpeed speed, NonnullOwnPtr<Pipe> default_pipe)
    : m_device_port(port)
    , m_device_speed(speed)
    , m_address(0)
    , m_controller(controller)
    , m_default_pipe(move(default_pipe))
{
}

Device::Device(NonnullRefPtr<USBController> controller, u8 address, u8 port, DeviceSpeed speed, NonnullOwnPtr<Pipe> default_pipe)
    : m_device_port(port)
    , m_device_speed(speed)
    , m_address(address)
    , m_controller(move(controller))
    , m_default_pipe(move(default_pipe))
{
}

Device::Device(Device const& device, NonnullOwnPtr<Pipe> default_pipe)
    : m_device_port(device.port())
    , m_device_speed(device.speed())
    , m_address(device.address())
    , m_device_descriptor(device.device_descriptor())
    , m_controller(device.controller())
    , m_default_pipe(move(default_pipe))
{
}

Device::~Device()
{
}

ErrorOr<void> Device::enumerate_device()
{
    USBDeviceDescriptor dev_descriptor {};

    // Send 8-bytes to get at least the `max_packet_size` from the device
    constexpr u8 short_device_descriptor_length = 8;
    auto transfer_length = TRY(m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, short_device_descriptor_length, &dev_descriptor));

    // FIXME: This be "not equal to" instead of "less than", but control transfers report a higher transfer length than expected.
    if (transfer_length < short_device_descriptor_length) {
        dbgln("USB Device: Not enough bytes for short device descriptor. Expected {}, got {}.", short_device_descriptor_length, transfer_length);
        return EIO;
    }

    if constexpr (USB_DEBUG) {
        dbgln("USB Short Device Descriptor:");
        dbgln("Descriptor length: {}", dev_descriptor.descriptor_header.length);
        dbgln("Descriptor type: {}", dev_descriptor.descriptor_header.descriptor_type);

        dbgln("Device Class: {:02x}", dev_descriptor.device_class);
        dbgln("Device Sub-Class: {:02x}", dev_descriptor.device_sub_class);
        dbgln("Device Protocol: {:02x}", dev_descriptor.device_protocol);
        dbgln("Max Packet Size: {:02x} bytes", dev_descriptor.max_packet_size);
    }

    // Ensure that this is actually a valid device descriptor...
    VERIFY(dev_descriptor.descriptor_header.descriptor_type == DESCRIPTOR_TYPE_DEVICE);
    m_default_pipe->set_max_packet_size(dev_descriptor.max_packet_size);

    transfer_length = TRY(m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, sizeof(USBDeviceDescriptor), &dev_descriptor));

    // FIXME: This be "not equal to" instead of "less than", but control transfers report a higher transfer length than expected.
    if (transfer_length < sizeof(USBDeviceDescriptor)) {
        dbgln("USB Device: Unexpected device descriptor length. Expected {}, got {}.", sizeof(USBDeviceDescriptor), transfer_length);
        return EIO;
    }

    // Ensure that this is actually a valid device descriptor...
    VERIFY(dev_descriptor.descriptor_header.descriptor_type == DESCRIPTOR_TYPE_DEVICE);

    if constexpr (USB_DEBUG) {
        dbgln("USB Device Descriptor for {:04x}:{:04x}", dev_descriptor.vendor_id, dev_descriptor.product_id);
        dbgln("Device Class: {:02x}", dev_descriptor.device_class);
        dbgln("Device Sub-Class: {:02x}", dev_descriptor.device_sub_class);
        dbgln("Device Protocol: {:02x}", dev_descriptor.device_protocol);
        dbgln("Max Packet Size: {:02x} bytes", dev_descriptor.max_packet_size);
        dbgln("Number of configurations: {:02x}", dev_descriptor.num_configurations);
    }

    auto new_address = m_controller->allocate_address();

    // Attempt to set devices address on the bus
    transfer_length = TRY(m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE, USB_REQUEST_SET_ADDRESS, new_address, 0, 0, nullptr));

    // This has to be set after we send out the "Set Address" request because it might be sent to the root hub.
    // The root hub uses the address to intercept requests to itself.
    m_address = new_address;
    m_default_pipe->set_device_address(new_address);

    dbgln_if(USB_DEBUG, "USB Device: Set address to {}", m_address);

    memcpy(&m_device_descriptor, &dev_descriptor, sizeof(USBDeviceDescriptor));
    return {};
}

}
