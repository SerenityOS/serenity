/*
 * Copyright (c) 2021-2023, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/USB/DeviceInformation.h>
#include <Kernel/Library/StdLib.h>

namespace Kernel::USB {

ErrorOr<NonnullLockRefPtr<Device>> Device::try_create(USBController const& controller, u8 port, DeviceSpeed speed)
{
    auto pipe = TRY(ControlPipe::create(controller, 0, 8, 0));
    auto device = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) Device(controller, port, speed, move(pipe))));
    auto sysfs_node = TRY(SysFSUSBDeviceInformation::create(*device));
    device->m_sysfs_device_info_node.with([&](auto& node) {
        node = move(sysfs_node);
    });
    TRY(device->enumerate_device());

    // Attempt to find a driver for this device. If one is found, we call the driver's
    // "probe" function, which initialises the local state for the device driver.
    // It is currently the driver's responsibility to search the configuration/interface
    // and take the appropriate action.
    for (auto& driver : USBManagement::the().available_drivers()) {
        // FIXME: Some devices have multiple configurations, for which we may have a better driver,
        //        than the first we find, or we have a vendor specific driver for the device,
        //        so we want a prioritization mechanism here
        auto result = driver->probe(device);
        if (result.is_error())
            continue;
        dbgln_if(USB_DEBUG, "Found driver {} for device {:04x}:{:04x}!", driver->name(), device->m_vendor_id, device->m_product_id);
        device->set_driver(driver);
        break;
    }

    return device;
}

Device::Device(USBController const& controller, u8 port, DeviceSpeed speed, NonnullOwnPtr<ControlPipe> default_pipe)
    : m_device_port(port)
    , m_device_speed(speed)
    , m_address(0)
    , m_controller(controller)
    , m_default_pipe(move(default_pipe))
{
}

Device::Device(NonnullLockRefPtr<USBController> controller, u8 address, u8 port, DeviceSpeed speed, NonnullOwnPtr<ControlPipe> default_pipe)
    : m_device_port(port)
    , m_device_speed(speed)
    , m_address(address)
    , m_controller(move(controller))
    , m_default_pipe(move(default_pipe))
{
}

Device::Device(Device const& device, NonnullOwnPtr<ControlPipe> default_pipe)
    : m_device_port(device.port())
    , m_device_speed(device.speed())
    , m_address(device.address())
    , m_device_descriptor(device.device_descriptor())
    , m_configurations(device.configurations())
    , m_controller(device.controller())
    , m_default_pipe(move(default_pipe))
{
}

Device::~Device() = default;

ErrorOr<void> Device::enumerate_device()
{
    USBDeviceDescriptor dev_descriptor {};

    // Send 8-bytes to get at least the `max_packet_size` from the device
    constexpr u8 short_device_descriptor_length = 8;
    auto transfer_length = TRY(m_default_pipe->submit_control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, short_device_descriptor_length, &dev_descriptor));

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

    transfer_length = TRY(m_default_pipe->submit_control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_DEVICE << 8), 0, sizeof(USBDeviceDescriptor), &dev_descriptor));

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
    transfer_length = TRY(m_default_pipe->submit_control_transfer(USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE, USB_REQUEST_SET_ADDRESS, new_address, 0, 0, nullptr));

    // This has to be set after we send out the "Set Address" request because it might be sent to the root hub.
    // The root hub uses the address to intercept requests to itself.
    m_address = new_address;
    m_default_pipe->set_device_address(new_address);

    dbgln_if(USB_DEBUG, "USB Device: Set address to {}", m_address);

    memcpy(&m_device_descriptor, &dev_descriptor, sizeof(USBDeviceDescriptor));

    // Fetch the configuration descriptors from the device
    m_configurations.ensure_capacity(m_device_descriptor.num_configurations);
    for (auto configuration = 0u; configuration < m_device_descriptor.num_configurations; configuration++) {
        USBConfigurationDescriptor configuration_descriptor;
        transfer_length = TRY(m_default_pipe->submit_control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST, USB_REQUEST_GET_DESCRIPTOR, (DESCRIPTOR_TYPE_CONFIGURATION << 8u) | configuration, 0, sizeof(USBConfigurationDescriptor), &configuration_descriptor));

        if constexpr (USB_DEBUG) {
            dbgln("USB Configuration Descriptor {}", configuration);
            dbgln("Total Length: {}", configuration_descriptor.total_length);
            dbgln("Number of interfaces: {}", configuration_descriptor.number_of_interfaces);
            dbgln("Configuration Value: {}", configuration_descriptor.configuration_value);
            dbgln("Attributes Bitmap: {:08b}", configuration_descriptor.attributes_bitmap);
            dbgln("Maximum Power: {}mA", configuration_descriptor.max_power_in_ma * 2u); // This value is in 2mA steps
        }

        USBConfiguration device_configuration(*this, configuration_descriptor);
        TRY(device_configuration.enumerate_interfaces());
        m_configurations.append(device_configuration);
    }

    return {};
}

ErrorOr<size_t> Device::control_transfer(u8 request_type, u8 request, u16 value, u16 index, u16 length, void* data)
{
    return TRY(m_default_pipe->submit_control_transfer(request_type, request, value, index, length, data));
}

}
