/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/USB/USBPipe.h>

namespace Kernel::USB {

class USBController;

//
// Some nice info from FTDI on device enumeration and how some of this
// glues together:
//
// https://www.ftdichip.com/Support/Documents/TechnicalNotes/TN_113_Simplified%20Description%20of%20USB%20Device%20Enumeration.pdf
class Device : public RefCounted<Device> {
public:
    enum class DeviceSpeed : u8 {
        FullSpeed = 0,
        LowSpeed
    };

    static ErrorOr<NonnullRefPtr<Device>> try_create(USBController const&, u8, DeviceSpeed);

    Device(USBController const&, u8, DeviceSpeed, NonnullOwnPtr<Pipe> default_pipe);
    Device(Device const& device, NonnullOwnPtr<Pipe> default_pipe);
    virtual ~Device();

    ErrorOr<void> enumerate_device();

    u8 port() const { return m_device_port; }
    DeviceSpeed speed() const { return m_device_speed; }

    u8 address() const { return m_address; }

    const USBDeviceDescriptor& device_descriptor() const { return m_device_descriptor; }

    USBController& controller() { return *m_controller; }
    USBController const& controller() const { return *m_controller; }

protected:
    Device(NonnullRefPtr<USBController> controller, u8 address, u8 port, DeviceSpeed speed, NonnullOwnPtr<Pipe> default_pipe);

    u8 m_device_port { 0 };     // What port is this device attached to. NOTE: This is 1-based.
    DeviceSpeed m_device_speed; // What speed is this device running at
    u8 m_address { 0 };         // USB address assigned to this device

    // Device description
    u16 m_vendor_id { 0 };                   // This device's vendor ID assigned by the USB group
    u16 m_product_id { 0 };                  // This device's product ID assigned by the USB group
    USBDeviceDescriptor m_device_descriptor; // Device Descriptor obtained from USB Device

    NonnullRefPtr<USBController> m_controller;
    NonnullOwnPtr<Pipe> m_default_pipe; // Default communication pipe (endpoint0) used during enumeration

private:
    IntrusiveListNode<Device, NonnullRefPtr<Device>> m_hub_child_node;

public:
    using List = IntrusiveList<&Device::m_hub_child_node>;
};
}
