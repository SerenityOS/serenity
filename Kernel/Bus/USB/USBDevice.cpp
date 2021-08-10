/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/StdLib.h>

namespace Kernel::USB {

KResultOr<NonnullRefPtr<Device>> Device::try_create(USBController const& controller, PortNumber port, DeviceSpeed speed)
{
    auto pipe_or_error = Pipe::try_create_pipe(controller, Pipe::Type::Control, Pipe::Direction::Bidirectional, 0, 8, 0);
    if (pipe_or_error.is_error())
        return pipe_or_error.error();

    auto device = AK::try_create<Device>(controller, port, speed, pipe_or_error.release_value());
    if (!device)
        return ENOMEM;

    auto enumerate_result = device->enumerate();
    if (enumerate_result.is_error())
        return enumerate_result;

    return device.release_nonnull();
}

Device::Device(USBController const& controller, PortNumber port, DeviceSpeed speed, NonnullOwnPtr<Pipe> default_pipe)
    : m_device_port(port)
    , m_device_speed(speed)
    , m_address(0)
    , m_controller(controller)
    , m_default_pipe(move(default_pipe))
{
}

KResult Device::enumerate()
{
    USBDeviceDescriptor dev_descriptor {};

    // Send 8-bytes to get at least the `max_packet_size` from the device
    auto transfer_length_or_error = m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, 8, &dev_descriptor);

    if (transfer_length_or_error.is_error())
        return transfer_length_or_error.error();

    auto transfer_length = transfer_length_or_error.release_value();

    // FIXME: This shouldn't crash! Do some correct error handling on me please!
    VERIFY(transfer_length > 0);

    // Ensure that this is actually a valid device descriptor...
    VERIFY(dev_descriptor.descriptor_header.descriptor_type == DESCRIPTOR_TYPE_DEVICE);
    m_default_pipe->set_max_packet_size(dev_descriptor.max_packet_size);

    transfer_length_or_error = m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, sizeof(USBDeviceDescriptor), &dev_descriptor);

    if (transfer_length_or_error.is_error())
        return transfer_length_or_error.error();

    transfer_length = transfer_length_or_error.release_value();

    // FIXME: This shouldn't crash! Do some correct error handling on me please!
    VERIFY(transfer_length > 0);

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

    m_address = m_controller->allocate_address();

    // Attempt to set devices address on the bus
    transfer_length_or_error = m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE, USB_REQUEST_SET_ADDRESS, m_address, 0, 0, nullptr);

    if (transfer_length_or_error.is_error())
        return transfer_length_or_error.error();

    transfer_length = transfer_length_or_error.release_value();

    VERIFY(transfer_length > 0);

    memcpy(&m_device_descriptor, &dev_descriptor, sizeof(USBDeviceDescriptor));
    return KSuccess;
}

Device::~Device()
{
}

}
