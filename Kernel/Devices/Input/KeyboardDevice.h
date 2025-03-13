/*
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2021, Edwin Hoksberg <mail@edwinhoksberg.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/Input/Device.h>
#include <Kernel/Devices/Input/ScanCodeEvent.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class KeyboardDevice : public InputDevice {
    friend class Device;

public:
    using Event = KeyEvent;

    static ErrorOr<NonnullRefPtr<KeyboardDevice>> try_to_initialize();

    virtual ~KeyboardDevice() override;

    // ^CharacterDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return EINVAL; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }

    // ^File
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;

    void update_modifier(u8 modifier, bool state)
    {
        if (state)
            m_modifiers |= modifier;
        else
            m_modifiers &= ~modifier;
    }

    u8 modifiers() const { return m_modifiers; }

    void handle_input_event(KeyEvent);
    bool num_lock_on() const { return m_num_lock_on; }

protected:
    KeyboardDevice();
    mutable Spinlock<LockRank::None> m_queue_lock {};
    CircularQueue<Event, 16> m_queue;
    // ^CharacterDevice
    virtual StringView class_name() const override { return "KeyboardDevice"sv; }

    u8 m_modifiers { 0 };
    bool m_caps_lock_to_ctrl_pressed { false };
    bool m_caps_lock_on { false };
    bool m_num_lock_on { false };

    void key_state_changed(u8 raw, bool pressed);
};
}
