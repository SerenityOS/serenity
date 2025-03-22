/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Arch/x86_64/ISABus/I8042Controller.h>
#include <Kernel/Devices/Input/PS2/MouseDevice.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class VMWareMouseDevice final : public PS2MouseDevice {
public:
    friend class Device;
    static ErrorOr<NonnullOwnPtr<VMWareMouseDevice>> try_to_initialize(SerialIOController const&, SerialIOController::PortIndex, MouseDevice const&);
    virtual ~VMWareMouseDevice() override;

    // ^PS2Device
    virtual void handle_byte_read_from_serial_input(u8 byte) override;

private:
    VMWareMouseDevice(SerialIOController const&, SerialIOController::PortIndex, MouseDevice const&);
};

}
