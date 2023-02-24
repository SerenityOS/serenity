/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Arch/x86_64/ISABus/HID/PS2MouseDevice.h>
#include <Kernel/Arch/x86_64/ISABus/I8042Controller.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class VMWareMouseDevice final : public PS2MouseDevice {
public:
    friend class DeviceManagement;
    static ErrorOr<NonnullOwnPtr<VMWareMouseDevice>> try_to_initialize(I8042Controller const&, MouseDevice const&);
    virtual ~VMWareMouseDevice() override;

    // ^PS2Device
    virtual void irq_handle_byte_read(u8 byte) override;

private:
    VMWareMouseDevice(I8042Controller const&, MouseDevice const&);
};

}
