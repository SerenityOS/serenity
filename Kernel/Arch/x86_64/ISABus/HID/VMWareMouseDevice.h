/*
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Arch/x86_64/ISABus/I8042Controller.h>
#include <Kernel/Bus/SerialIO/PS2/Controller.h>
#include <Kernel/Bus/SerialIO/PS2/Device.h>
#include <Kernel/Devices/HID/MouseDevice.h>
#include <Kernel/Random.h>

namespace Kernel {

class VMWareMouseDevice final : public PS2Device {
public:
    friend class DeviceManagement;

    static ErrorOr<NonnullOwnPtr<PS2Device>> probe_and_initialize_instance(PS2Controller&, PS2PortIndex, PS2DeviceType);

    virtual ~VMWareMouseDevice() override;

    // ^PS2Device
    virtual void handle_byte_read_from_serial_input(u8 byte) override;

private:
    VMWareMouseDevice(PS2Controller const&, PS2PortIndex, PS2DeviceType, MouseDevice const&);

    NonnullRefPtr<MouseDevice> const m_mouse_device;
};

}
