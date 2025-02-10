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

ErrorOr<NonnullLockRefPtr<Device>> Device::try_create(USBController& controller, Hub const& hub, u8 port, DeviceSpeed speed)
{
    auto device = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) Device(controller, &hub, port, speed)));
    device->set_default_pipe(TRY(ControlPipe::create(controller, device, 0, 8)));
    TRY(controller.initialize_device(*device));

    auto sysfs_node = TRY(SysFSUSBDeviceInformation::create(*device));
    device->m_sysfs_device_info_node.with([&](auto& node) {
        node = move(sysfs_node);
    });
    // Attempt to find a driver for this device. If one is found, we call the driver's
    // "probe" function, which initialises the local state for the device driver.
    // It is currently the driver's responsibility to search the configuration/interface
    // and take the appropriate action.
    for (auto& driver : USBManagement::available_drivers()) {
        // FIXME: Some devices have multiple configurations, for which we may have a better driver,
        //        than the first we find, or we have a vendor specific driver for the device,
        //        so we want a prioritization mechanism here
        auto result = driver->probe(device);
        if (result.is_error())
            continue;
        dbgln_if(USB_DEBUG, "Found driver {} for device {:04x}:{:04x}!", driver->name(), device->device_descriptor().vendor_id, device->device_descriptor().product_id);
        device->set_driver(driver);
        break;
    }

    return device;
}

Device::Device(USBController const& controller, Hub const* hub, u8 port, DeviceSpeed speed)
    : m_device_port(port)
    , m_device_speed(speed)
    , m_address(0)
    , m_controller(controller)
    , m_hub(hub)
{
}

Device::Device(USBController const& controller, Hub const* hub, u8 port, DeviceSpeed speed, u8 address, USBDeviceDescriptor const& descriptor)
    : m_device_port(port)
    , m_device_speed(speed)
    , m_address(address)
    , m_device_descriptor(descriptor)
    , m_controller(controller)
    , m_hub(hub)
{
}

Device::Device(Device const& device)
    : m_device_port(device.port())
    , m_device_speed(device.speed())
    , m_address(device.address())
    , m_controller_identifier(device.controller_identifier())
    , m_device_descriptor(device.device_descriptor())
    , m_controller(device.controller())
    , m_hub(device.hub())
{
    // FIXME: This can definitely OOM
    m_configurations.ensure_capacity(device.configurations().size());
    for (auto const& configuration : device.configurations()) {
        m_configurations.unchecked_append(configuration.copy());
        m_configurations.last().set_device({}, *this);
    }

    // FIXME: Do we need to enter our selves into the hubs children list or sysfs list?
}

Device::~Device() = default;

void Device::set_default_pipe(NonnullOwnPtr<ControlPipe> pipe)
{
    VERIFY(!m_default_pipe);
    m_default_pipe = move(pipe);
}

ErrorOr<size_t> Device::control_transfer(u8 request_type, u8 request, u16 value, u16 index, u16 length, void* data)
{
    return TRY(m_default_pipe->submit_control_transfer(request_type, request, value, index, length, data));
}

ErrorOr<void> Device::set_configuration(USBConfiguration const& configuration)
{
    if (m_was_configured.was_set() && m_current_configuration != configuration.configuration_id())
        return EALREADY;

    if (!m_was_configured.was_set()) {
        m_was_configured.set();
        m_current_configuration = configuration.configuration_id();

        TRY(control_transfer(USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE | USB_REQUEST_TYPE_STANDARD | USB_REQUEST_RECIPIENT_DEVICE, USB_REQUEST_SET_CONFIGURATION,
            m_current_configuration, 0, 0, nullptr));

        // FIXME: On xHCI we should set up the all endpoints for the configuration here
        //        Currently we set them up on the first transfer, which works good enough for now
    }

    return {};
}

ErrorOr<void> Device::set_configuration_and_interface(USBInterface const& interface)
{
    auto const& configuration = interface.configuration();
    TRY(set_configuration(configuration));

    // FIXME: When we use the default alternate_setting of interface/the current alternate setting, we don't need to SET_INTERFACE it
    //        but that gets a bit difficult to track
    auto result = control_transfer(USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE | USB_REQUEST_TYPE_STANDARD | USB_REQUEST_RECIPIENT_INTERFACE, USB_REQUEST_SET_INTERFACE,
        interface.descriptor().alternate_setting, interface.descriptor().interface_id, 0, nullptr);
    if (result.is_error()) {
        auto error = result.release_error();
        if (error.code() == ESHUTDOWN) {
            // USB 2.0 Specification Section 9.4.10 Set Interface
            // "If a device only supports a default setting for the specified interface, then a STALL may be returned in the Status stage of the request."
            // This means the interface should already have the desired alternate setting selected.
        } else {
            return error;
        }
    }

    // FIXME: As in activate_configuration, we should set up changed endpoints on xHCI here

    return {};
}

}
