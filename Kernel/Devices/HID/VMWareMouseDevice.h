/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/HID/I8042Controller.h>
#include <Kernel/Devices/HID/PS2MouseDevice.h>

namespace Kernel {

class VMWareMouseDevice final : public PS2MouseDevice {
public:
    static RefPtr<VMWareMouseDevice> try_to_initialize(const I8042Controller&);
    virtual ~VMWareMouseDevice() override;

    // ^I8042Device
    virtual void irq_handle_byte_read(u8 byte) override;

    // FIXME: We expose this constructor to make try_create_device helper to work
    explicit VMWareMouseDevice(const I8042Controller&);
};

}
