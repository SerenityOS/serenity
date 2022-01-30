/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2021, Edwin Hoksberg <mail@edwinhoksberg.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/DoublyLinkedList.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/HID/HIDDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Random.h>

namespace Kernel {

class KeyboardDevice : public HIDDevice {
public:
    using Event = KeyEvent;

    virtual ~KeyboardDevice() override;

    // ^CharacterDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const OpenFileDescription&, u64) const override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; }
    virtual bool can_write(const OpenFileDescription&, u64) const override { return true; }

    // ^HIDDevice
    virtual Type instrument_type() const override { return Type::Keyboard; }

    // ^File
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;

    void update_modifier(u8 modifier, bool state)
    {
        if (state)
            m_modifiers |= modifier;
        else
            m_modifiers &= ~modifier;
    }

protected:
    KeyboardDevice();
    mutable Spinlock m_queue_lock;
    CircularQueue<Event, 16> m_queue;
    // ^CharacterDevice
    virtual StringView class_name() const override { return "KeyboardDevice"sv; }

    u8 m_modifiers { 0 };
    bool m_caps_lock_to_ctrl_pressed { false };
    bool m_caps_lock_on { false };
    bool m_num_lock_on { false };
    bool m_has_e0_prefix { false };
    bool m_left_shift_pressed { false };
    bool m_right_shift_pressed { false };
    bool m_left_super_pressed { false };
    bool m_right_super_pressed { false };

    void key_state_changed(u8 raw, bool pressed);
};
}
