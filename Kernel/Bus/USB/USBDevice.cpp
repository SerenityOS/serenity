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

ErrorOr<NonnullLockRefPtr<Device>> Device::try_create(USBController& controller, u8 port, DeviceSpeed speed)
{
    auto device = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) Device(controller, port, speed)));
    device->set_default_pipe(TRY(ControlPipe::create(controller, device, 0, 8)));
    auto sysfs_node = TRY(SysFSUSBDeviceInformation::create(*device));
    device->m_sysfs_device_info_node.with([&](auto& node) {
        node = move(sysfs_node);
    });
    TRY(controller.initialize_device(*device));

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

Device::Device(USBController const& controller, u8 port, DeviceSpeed speed)
    : m_device_port(port)
    , m_device_speed(speed)
    , m_address(0)
    , m_controller(controller)
{
}

Device::Device(NonnullLockRefPtr<USBController> controller, u8 address, u8 port, DeviceSpeed speed)
    : m_device_port(port)
    , m_device_speed(speed)
    , m_address(address)
    , m_controller(move(controller))
{
}

Device::Device(Device const& device)
    : m_device_port(device.port())
    , m_device_speed(device.speed())
    , m_address(device.address())
    , m_device_descriptor(device.device_descriptor())
    , m_configurations(device.configurations())
    , m_controller(device.controller())
{
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

}
