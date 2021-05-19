/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/IO.h>
#include <Kernel/TTY/VirtualConsole.h>

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
    Key_Invalid,
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
    Key_Invalid,
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

void KeyboardDevice::key_state_changed(u8 scan_code, bool pressed)
{
    KeyCode key = (m_modifiers & Mod_Shift) ? shifted_key_map[scan_code] : unshifted_key_map[scan_code];

    if (key == Key_NumLock && pressed)
        m_num_lock_on = !m_num_lock_on;

    if (m_num_lock_on && !m_has_e0_prefix) {
        if (scan_code >= 0x47 && scan_code <= 0x53) {
            u8 index = scan_code - 0x47;
            constexpr KeyCode numpad_key_map[13] = { Key_7, Key_8, Key_9, Key_Invalid, Key_4, Key_5, Key_6, Key_Invalid, Key_1, Key_2, Key_3, Key_0, Key_Comma };
            KeyCode newKey = numpad_key_map[index];

            if (newKey != Key_Invalid) {
                key = newKey;
            }
        }
    }

    if (!g_caps_lock_remapped_to_ctrl && key == Key_CapsLock && pressed)
        m_caps_lock_on = !m_caps_lock_on;

    if (g_caps_lock_remapped_to_ctrl && key == Key_CapsLock)
        m_caps_lock_to_ctrl_pressed = pressed;

    if (g_caps_lock_remapped_to_ctrl)
        update_modifier(Mod_Ctrl, m_caps_lock_to_ctrl_pressed);

    Event event;
    event.key = key;
    event.scancode = m_has_e0_prefix ? 0xe000 + scan_code : scan_code;
    event.flags = m_modifiers;
    event.e0_prefix = m_has_e0_prefix;
    event.caps_lock_on = m_caps_lock_on;
    event.code_point = HIDManagement::the().character_map().get_char(event);

    if (pressed)
        event.flags |= Is_Press;
    if (HIDManagement::the().m_client)
        HIDManagement::the().m_client->on_key_pressed(event);

    {
        ScopedSpinLock lock(m_queue_lock);
        m_queue.enqueue(event);
    }

    m_has_e0_prefix = false;

    evaluate_block_conditions();
}

// FIXME: UNMAP_AFTER_INIT is fine for now, but for hot-pluggable devices
// like USB keyboards, we need to remove this
UNMAP_AFTER_INIT KeyboardDevice::KeyboardDevice()
    : HIDDevice(85, HIDManagement::the().generate_minor_device_number_for_keyboard())
{
}

// FIXME: UNMAP_AFTER_INIT is fine for now, but for hot-pluggable devices
// like USB keyboards, we need to remove this
UNMAP_AFTER_INIT KeyboardDevice::~KeyboardDevice()
{
}

bool KeyboardDevice::can_read(const FileDescription&, size_t) const
{
    return !m_queue.is_empty();
}

KResultOr<size_t> KeyboardDevice::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    size_t nread = 0;
    ScopedSpinLock lock(m_queue_lock);
    while (nread < size) {
        if (m_queue.is_empty())
            break;
        // Don't return partial data frames.
        if ((size - nread) < (ssize_t)sizeof(Event))
            break;
        auto event = m_queue.dequeue();

        lock.unlock();

        auto result = buffer.write_buffered<sizeof(Event)>(sizeof(Event), [&](u8* data, size_t data_bytes) {
            memcpy(data, &event, sizeof(Event));
            return data_bytes;
        });
        if (result.is_error())
            return result.error();
        VERIFY(result.value() == sizeof(Event));
        nread += sizeof(Event);

        lock.lock();
    }
    return nread;
}

KResultOr<size_t> KeyboardDevice::write(FileDescription&, u64, const UserOrKernelBuffer&, size_t)
{
    return 0;
}

}
