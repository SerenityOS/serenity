/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2021, Edwin Hoksberg <mail@edwinhoksberg.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/Devices/TTY/ConsoleManagement.h>
#include <Kernel/Devices/TTY/VirtualConsole.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

static constexpr KeyCode unshifted_key_map[0x80] = {
    Key_Invalid,
    Key_Escape,
    Key_1,
    Key_2,
    Key_3,
    Key_4,
    Key_5,
    Key_6,
    Key_7,
    Key_8,
    Key_9,
    Key_0,
    Key_Minus,
    Key_Equal,
    Key_Backspace,
    Key_Tab, // 15
    Key_Q,
    Key_W,
    Key_E,
    Key_R,
    Key_T,
    Key_Y,
    Key_U,
    Key_I,
    Key_O,
    Key_P,
    Key_LeftBracket,
    Key_RightBracket,
    Key_Return,  // 28
    Key_Control, // 29
    Key_A,
    Key_S,
    Key_D,
    Key_F,
    Key_G,
    Key_H,
    Key_J,
    Key_K,
    Key_L,
    Key_Semicolon,
    Key_Apostrophe,
    Key_Backtick,
    Key_LeftShift, // 42
    Key_Backslash,
    Key_Z,
    Key_X,
    Key_C,
    Key_V,
    Key_B,
    Key_N,
    Key_M,
    Key_Comma,
    Key_Period,
    Key_Slash,
    Key_RightShift, // 54
    Key_Asterisk,
    Key_Alt,      // 56
    Key_Space,    // 57
    Key_CapsLock, // 58
    Key_F1,
    Key_F2,
    Key_F3,
    Key_F4,
    Key_F5,
    Key_F6,
    Key_F7,
    Key_F8,
    Key_F9,
    Key_F10,
    Key_NumLock,
    Key_Invalid, // 70
    Key_Home,
    Key_Up,
    Key_PageUp,
    Key_Minus,
    Key_Left,
    Key_Invalid,
    Key_Right, // 77
    Key_Plus,
    Key_End,
    Key_Down, // 80
    Key_PageDown,
    Key_Insert,
    Key_Delete, // 83
    Key_Invalid,
    Key_Invalid,
    Key_Backslash,
    Key_F11,
    Key_F12,
    Key_Invalid,
    Key_Invalid,
    Key_Super,
    Key_Invalid,
    Key_Menu,
};

static constexpr KeyCode shifted_key_map[0x100] = {
    Key_Invalid,
    Key_Escape,
    Key_ExclamationPoint,
    Key_AtSign,
    Key_Hashtag,
    Key_Dollar,
    Key_Percent,
    Key_Circumflex,
    Key_Ampersand,
    Key_Asterisk,
    Key_LeftParen,
    Key_RightParen,
    Key_Underscore,
    Key_Plus,
    Key_Backspace,
    Key_Tab,
    Key_Q,
    Key_W,
    Key_E,
    Key_R,
    Key_T,
    Key_Y,
    Key_U,
    Key_I,
    Key_O,
    Key_P,
    Key_LeftBrace,
    Key_RightBrace,
    Key_Return,
    Key_Control,
    Key_A,
    Key_S,
    Key_D,
    Key_F,
    Key_G,
    Key_H,
    Key_J,
    Key_K,
    Key_L,
    Key_Colon,
    Key_DoubleQuote,
    Key_Tilde,
    Key_LeftShift, // 42
    Key_Pipe,
    Key_Z,
    Key_X,
    Key_C,
    Key_V,
    Key_B,
    Key_N,
    Key_M,
    Key_LessThan,
    Key_GreaterThan,
    Key_QuestionMark,
    Key_RightShift, // 54
    Key_Asterisk,
    Key_Alt,
    Key_Space,    // 57
    Key_CapsLock, // 58
    Key_F1,
    Key_F2,
    Key_F3,
    Key_F4,
    Key_F5,
    Key_F6,
    Key_F7,
    Key_F8,
    Key_F9,
    Key_F10,
    Key_NumLock,
    Key_Invalid, // 70
    Key_Home,
    Key_Up,
    Key_PageUp,
    Key_Minus,
    Key_Left,
    Key_Invalid,
    Key_Right, // 77
    Key_Plus,
    Key_End,
    Key_Down, // 80
    Key_PageDown,
    Key_Insert,
    Key_Delete, // 83
    Key_Invalid,
    Key_Invalid,
    Key_Pipe,
    Key_F11,
    Key_F12,
    Key_Invalid,
    Key_Invalid,
    Key_Super,
    Key_Invalid,
    Key_Menu,
};

void KeyboardDevice::handle_scan_code_input_event(ScanCodeEvent event)
{
    m_entropy_source.add_random_event(event.scan_code_value);
    switch (event.scan_code_value) {
    case 0x38:
        if (event.e0_prefix)
            update_modifier(Mod_AltGr, event.pressed);
        else
            update_modifier(Mod_Alt, event.pressed);
        break;
    case 0x1d:
        update_modifier(Mod_Ctrl, event.pressed);
        break;
    case 0x5b:
        m_left_super_pressed = event.pressed;
        update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case 0x5c:
        m_right_super_pressed = event.pressed;
        update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case 0x2a:
        m_left_shift_pressed = event.pressed;
        update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    case 0x36:
        m_right_shift_pressed = event.pressed;
        update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    case 0x1c:
    case 0x35:
        if (event.e0_prefix)
            update_modifier(Mod_Keypad, event.pressed);
        break;
    case 0x37:
    case 0x47:
    case 0x48:
    case 0x49:
    case 0x4a:
    case 0x4b:
    case 0x4c:
    case 0x4d:
    case 0x4e:
    case 0x4f:
    case 0x50:
    case 0x51:
    case 0x52:
    case 0x53:
        if (!event.e0_prefix)
            update_modifier(Mod_Keypad, event.pressed);
        break;
    }

    KeyCode key = (m_modifiers & Mod_Shift) ? shifted_key_map[event.scan_code_value] : unshifted_key_map[event.scan_code_value];

    if ((m_modifiers == (Mod_Alt | Mod_Shift) || m_modifiers == (Mod_Ctrl | Mod_Alt | Mod_Shift)) && key == Key_F12) {
        // Alt+Shift+F12 pressed, dump some kernel state to the debug console.
        ConsoleManagement::the().switch_to_debug();
        Scheduler::dump_scheduler_state(m_modifiers == (Mod_Ctrl | Mod_Alt | Mod_Shift));
    }

    if ((m_modifiers & Mod_Alt) != 0 && key >= Key_1 && key < Key_1 + ConsoleManagement::s_max_virtual_consoles) {
        // FIXME: Do something sanely here if we can't allocate a work queue?
        MUST(g_io_work->try_queue([key]() {
            ConsoleManagement::the().switch_to(key - Key_1);
        }));
    }

    if (key == Key_NumLock && event.pressed)
        m_num_lock_on = !m_num_lock_on;

    if (m_num_lock_on && !event.e0_prefix) {
        if (event.scan_code_value >= 0x47 && event.scan_code_value <= 0x53) {
            u8 index = event.scan_code_value - 0x47;
            constexpr KeyCode numpad_key_map[13] = { Key_7, Key_8, Key_9, Key_Invalid, Key_4, Key_5, Key_6, Key_Invalid, Key_1, Key_2, Key_3, Key_0, Key_Comma };
            KeyCode newKey = numpad_key_map[index];

            if (newKey != Key_Invalid) {
                key = newKey;
            }
        }
    }

    Event queued_event;
    queued_event.key = key;
    queued_event.scancode = event.e0_prefix ? 0xe000 + event.scan_code_value : event.scan_code_value;
    queued_event.flags = m_modifiers;
    queued_event.e0_prefix = event.e0_prefix;
    queued_event.caps_lock_on = m_caps_lock_on;
    queued_event.code_point = HIDManagement::the().get_char_from_character_map(queued_event, m_num_lock_on);

    // If using a non-QWERTY layout, queued_event.key needs to be updated to be the same as event.code_point
    KeyCode mapped_key = code_point_to_key_code(queued_event.code_point);
    if (mapped_key != KeyCode::Key_Invalid) {
        queued_event.key = mapped_key;
        key = mapped_key;
    }

    if (!g_caps_lock_remapped_to_ctrl && key == Key_CapsLock && event.pressed)
        m_caps_lock_on = !m_caps_lock_on;

    if (g_caps_lock_remapped_to_ctrl && key == Key_CapsLock) {
        m_caps_lock_to_ctrl_pressed = event.pressed;
        update_modifier(Mod_Ctrl, m_caps_lock_to_ctrl_pressed);
    }

    if (event.pressed)
        queued_event.flags |= Is_Press;

    {
        SpinlockLocker locker(HIDManagement::the().m_client_lock);
        if (HIDManagement::the().m_client)
            HIDManagement::the().m_client->on_key_pressed(queued_event);
    }

    {
        SpinlockLocker lock(m_queue_lock);
        m_queue.enqueue(queued_event);
    }

    evaluate_block_conditions();
}

ErrorOr<NonnullRefPtr<KeyboardDevice>> KeyboardDevice::try_to_initialize()
{
    return *TRY(DeviceManagement::try_create_device<KeyboardDevice>());
}

// FIXME: UNMAP_AFTER_INIT is fine for now, but for hot-pluggable devices
// like USB keyboards, we need to remove this
UNMAP_AFTER_INIT KeyboardDevice::KeyboardDevice()
    : HIDDevice(85, HIDManagement::the().generate_minor_device_number_for_keyboard())
{
}

// FIXME: UNMAP_AFTER_INIT is fine for now, but for hot-pluggable devices
// like USB keyboards, we need to remove this
UNMAP_AFTER_INIT KeyboardDevice::~KeyboardDevice() = default;

bool KeyboardDevice::can_read(OpenFileDescription const&, u64) const
{
    return !m_queue.is_empty();
}

ErrorOr<size_t> KeyboardDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    size_t nread = 0;
    SpinlockLocker lock(m_queue_lock);
    while (nread < size) {
        if (m_queue.is_empty())
            break;
        // Don't return partial data frames.
        if (size - nread < sizeof(Event))
            break;
        auto event = m_queue.dequeue();

        lock.unlock();

        auto result = TRY(buffer.write_buffered<sizeof(Event)>(sizeof(Event), [&](Bytes bytes) {
            memcpy(bytes.data(), &event, sizeof(Event));
            return bytes.size();
        }));
        VERIFY(result == sizeof(Event));
        nread += sizeof(Event);

        lock.lock();
    }
    return nread;
}

ErrorOr<void> KeyboardDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case KEYBOARD_IOCTL_GET_NUM_LOCK: {
        auto output = static_ptr_cast<bool*>(arg);
        return copy_to_user(output, &m_num_lock_on);
    }
    case KEYBOARD_IOCTL_SET_NUM_LOCK: {
        // In this case we expect the value to be a boolean and not a pointer.
        auto num_lock_value = static_cast<u8>(arg.ptr());
        if (num_lock_value != 0 && num_lock_value != 1)
            return EINVAL;
        m_num_lock_on = !!num_lock_value;
        return {};
    }
    case KEYBOARD_IOCTL_GET_CAPS_LOCK: {
        auto output = static_ptr_cast<bool*>(arg);
        return copy_to_user(output, &m_caps_lock_on);
    }
    case KEYBOARD_IOCTL_SET_CAPS_LOCK: {
        auto caps_lock_value = static_cast<u8>(arg.ptr());
        if (caps_lock_value != 0 && caps_lock_value != 1)
            return EINVAL;
        m_caps_lock_on = !!caps_lock_value;
        return {};
    }
    default:
        return EINVAL;
    };
}

}
