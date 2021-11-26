/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/DoublyLinkedList.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/HID/HIDDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Random.h>

namespace Kernel {

class MouseDevice : public HIDDevice {
public:
    virtual ~MouseDevice() override;

    // ^CharacterDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const OpenFileDescription&, size_t) const override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; }
    virtual bool can_write(const OpenFileDescription&, size_t) const override { return true; }

    // ^HIDDevice
    virtual Type instrument_type() const override { return Type::Mouse; }

protected:
    MouseDevice();
    // ^CharacterDevice
    virtual StringView class_name() const override { return "MouseDevice"sv; }

    mutable Spinlock m_queue_lock;
    CircularQueue<MousePacket, 100> m_queue;
};

}
