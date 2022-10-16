/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Bus/USB/USBConfiguration.h>
#include <Kernel/Bus/USB/USBPipe.h>

namespace Kernel {
class SysFSUSBDeviceInformation;
}

namespace Kernel::USB {

class USBController;
class USBConfiguration;

//
// Some nice info from FTDI on device enumeration and how some of this
// glues together:
//
// https://www.ftdichip.com/Support/Documents/TechnicalNotes/TN_113_Simplified%20Description%20of%20USB%20Device%20Enumeration.pdf
class Hub;
class Device : public AtomicRefCounted<Device> {
public:
    enum class DeviceSpeed : u8 {
        FullSpeed = 0,
        LowSpeed
    };

    static ErrorOr<NonnullLockRefPtr<Device>> try_create(USBController const&, u8, DeviceSpeed);

    Device(USBController const&, u8, DeviceSpeed, NonnullOwnPtr<ControlPipe> default_pipe);
    Device(Device const& device, NonnullOwnPtr<ControlPipe> default_pipe);
    virtual ~Device();

    ErrorOr<void> enumerate_device();

    u8 port() const { return m_device_port; }
    DeviceSpeed speed() const { return m_device_speed; }

    u8 address() const { return m_address; }

    USBDeviceDescriptor const& device_descriptor() const { return m_device_descriptor; }

    USBController& controller() { return *m_controller; }
    USBController const& controller() const { return *m_controller; }

    ErrorOr<size_t> control_transfer(u8 request_type, u8 request, u16 value, u16 index, u16 length, void* data);

    Vector<USBConfiguration> const& configurations() const { return m_configurations; }

    SysFSUSBDeviceInformation& sysfs_device_info_node(Badge<USB::Hub>) { return *m_sysfs_device_info_node; }

protected:
    Device(NonnullLockRefPtr<USBController> controller, u8 address, u8 port, DeviceSpeed speed, NonnullOwnPtr<ControlPipe> default_pipe);

    u8 m_device_port { 0 };     // What port is this device attached to. NOTE: This is 1-based.
    DeviceSpeed m_device_speed; // What speed is this device running at
    u8 m_address { 0 };         // USB address assigned to this device

    // Device description
    u16 m_vendor_id { 0 };                      // This device's vendor ID assigned by the USB group
    u16 m_product_id { 0 };                     // This device's product ID assigned by the USB group
    USBDeviceDescriptor m_device_descriptor {}; // Device Descriptor obtained from USB Device
    Vector<USBConfiguration> m_configurations;  // Configurations for this device

    NonnullLockRefPtr<USBController> m_controller;
    NonnullOwnPtr<ControlPipe> m_default_pipe; // Default communication pipe (endpoint0) used during enumeration

private:
    IntrusiveListNode<Device, NonnullLockRefPtr<Device>> m_hub_child_node;

protected:
    LockRefPtr<SysFSUSBDeviceInformation> m_sysfs_device_info_node;

public:
    using List = IntrusiveList<&Device::m_hub_child_node>;
};
}
