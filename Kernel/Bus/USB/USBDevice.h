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
    enum class PortNumber : u8 {
        Port1 = 0,
        Port2
    };

    enum class DeviceSpeed : u8 {
        FullSpeed = 0,
        LowSpeed
    };

public:
    static KResultOr<NonnullRefPtr<Device>> try_create(USBController const&, PortNumber, DeviceSpeed);

    Device(USBController const&, PortNumber, DeviceSpeed, NonnullOwnPtr<Pipe> default_pipe);
    ~Device();

    KResult enumerate();

    PortNumber port() const { return m_device_port; }
    DeviceSpeed speed() const { return m_device_speed; }

    u8 address() const { return m_address; }

    const USBDeviceDescriptor& device_descriptor() const { return m_device_descriptor; }

private:
    PortNumber m_device_port;   // What port is this device attached to
    DeviceSpeed m_device_speed; // What speed is this device running at
    u8 m_address { 0 };         // USB address assigned to this device

    // Device description
    u16 m_vendor_id { 0 };                   // This device's vendor ID assigned by the USB group
    u16 m_product_id { 0 };                  // This device's product ID assigned by the USB group
    USBDeviceDescriptor m_device_descriptor; // Device Descriptor obtained from USB Device

    NonnullRefPtr<USBController> m_controller;
    NonnullOwnPtr<Pipe> m_default_pipe; // Default communication pipe (endpoint0) used during enumeration
};
}
