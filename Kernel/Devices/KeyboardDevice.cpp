/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/IO.h>
#include <Kernel/TTY/VirtualConsole.h>

namespace Kernel {

#define IRQ_KEYBOARD 1

static const KeyCode unshifted_key_map[0x80] = {
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
    Key_Tab, //15
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

static const KeyCode shifted_key_map[0x100] = {
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

static const KeyCode numpad_key_map[13] = { Key_7, Key_8, Key_9, Key_Invalid, Key_4, Key_5, Key_6, Key_Invalid, Key_1, Key_2, Key_3, Key_0, Key_Comma };

void KeyboardDevice::key_state_changed(u8 scan_code, bool pressed)
{
    KeyCode key = (m_modifiers & Mod_Shift) ? shifted_key_map[scan_code] : unshifted_key_map[scan_code];

    if (key == Key_NumLock && pressed)
        m_num_lock_on = !m_num_lock_on;

    if (m_num_lock_on && !m_has_e0_prefix) {
        if (scan_code >= 0x47 && scan_code <= 0x53) {
            u8 index = scan_code - 0x47;
            KeyCode newKey = numpad_key_map[index];

            if (newKey != Key_Invalid) {
                key = newKey;
            }
        }
    }

    if (key == Key_CapsLock && pressed)
        m_caps_lock_on = !m_caps_lock_on;

    Event event;
    event.key = key;
    event.scancode = m_has_e0_prefix ? 0xe000 + scan_code : scan_code;
    event.flags = m_modifiers;
    event.e0_prefix = m_has_e0_prefix;
    event.caps_lock_on = m_caps_lock_on;
    event.code_point = m_character_map.get_char(event);

    if (pressed)
        event.flags |= Is_Press;
    if (m_client)
        m_client->on_key_pressed(event);

    {
        ScopedSpinLock lock(m_queue_lock);
        m_queue.enqueue(event);
    }

    m_has_e0_prefix = false;

    evaluate_block_conditions();
}

void KeyboardDevice::handle_irq(const RegisterState&)
{
    // The controller will read the data and call irq_handle_byte_read
    // for the appropriate device
    m_controller.irq_process_input_buffer(I8042Controller::Device::Keyboard);
}

void KeyboardDevice::irq_handle_byte_read(u8 byte)
{
    u8 ch = byte & 0x7f;
    bool pressed = !(byte & 0x80);

    m_entropy_source.add_random_event(byte);

    if (byte == 0xe0) {
        m_has_e0_prefix = true;
        return;
    }

#if KEYBOARD_DEBUG
    dbgln("Keyboard::irq_handle_byte_read: {:#02x} {}", ch, (pressed ? "down" : "up"));
#endif
    switch (ch) {
    case 0x38:
        if (m_has_e0_prefix)
            update_modifier(Mod_AltGr, pressed);
        else
            update_modifier(Mod_Alt, pressed);
        break;
    case 0x1d:
        update_modifier(Mod_Ctrl, pressed);
        break;
    case 0x5b:
        update_modifier(Mod_Super, pressed);
        break;
    case 0x2a:
    case 0x36:
        update_modifier(Mod_Shift, pressed);
        break;
    }
    switch (ch) {
    case I8042_ACK:
        break;
    default:
        if (m_modifiers & Mod_Alt) {
            switch (ch) {
            case 0x02 ... 0x07: // 1 to 6
                VirtualConsole::switch_to(ch - 0x02);
                break;
            default:
                key_state_changed(ch, pressed);
                break;
            }
        } else {
            key_state_changed(ch, pressed);
        }
    }
}

static AK::Singleton<KeyboardDevice> s_the;

KeyboardDevice& KeyboardDevice::the()
{
    return *s_the;
}

// clang-format off
static const Keyboard::CharacterMapData DEFAULT_CHARACTER_MAP =
{
    .map = {
        0,    '\033',    '1',    '2',    '3',    '4',    '5',    '6',    '7',    '8',    '9',    '0',    '-',    '=',    0x08,
                '\t',    'q',    'w',    'e',    'r',    't',    'y',    'u',    'i',    'o',    'p',    '[',    ']',    '\n',
                   0,    'a',    's',    'd',    'f',    'g',    'h',    'j',    'k',    'l',    ';',   '\'',    '`',       0,
                '\\',    'z',    'x',    'c',    'v',    'b',    'n',    'm',    ',',    '.',    '/',      0,    '*',       0,
                 ' ',      0,      0,
     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '\\', 0, 0, 0,

    },

    .shift_map = {
        0,    '\033',    '!',    '@',    '#',    '$',    '%',    '^',    '&',    '*',    '(',    ')',    '_',    '+',    0x08,
                '\t',    'Q',    'W',    'E',    'R',    'T',    'Y',    'U',    'I',    'O',    'P',    '{',    '}',    '\n',
                   0,    'A',    'S',    'D',    'F',    'G',    'H',    'J',    'K',    'L',    ':',    '"',    '~',       0,
                 '|',    'Z',    'X',    'C',    'V',    'B',    'N',    'M',    '<',    '>',    '?',      0,    '*',       0,
                 ' ',      0,      0,
     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '|',  0, 0, 0,

    },

    .alt_map = {
        0,    '\033',    '1',    '2',    '3',    '4',    '5',    '6',    '7',    '8',    '9',    '0',    '-',    '=',    0x08,
                '\t',    'q',    'w',    'e',    'r',    't',    'y',    'u',    'i',    'o',    'p',    '[',    ']',    '\n',
                   0,    'a',    's',    'd',    'f',    'g',    'h',    'j',    'k',    'l',    ';',   '\'',    '`',       0,
                '\\',    'z',    'x',    'c',    'v',    'b',    'n',    'm',    ',',    '.',    '/',      0,    '*',       0,
                 ' ',      0,      0,

     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '\\', 0, 0, 0,

    },

    .altgr_map = {
        0,    '\033',    '1',    '2',    '3',    '4',    '5',    '6',    '7',    '8',    '9',    '0',    '-',    '=',    0x08,
                '\t',    'q',    'w',    'e',    'r',    't',    'y',    'u',    'i',    'o',    'p',    '[',    ']',    '\n',
                   0,    'a',    's',    'd',    'f',    'g',    'h',    'j',    'k',    'l',    ';',   '\'',    '`',       0,
                '\\',    'z',    'x',    'c',    'v',    'b',    'n',    'm',    ',',    '.',    '/',      0,    '*',       0,
                 ' ',      0,      0,
     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '\\', 0, 0, 0,
    },

    .shift_altgr_map = {
        0,    '\033',    '1',    '2',    '3',    '4',    '5',    '6',    '7',    '8',    '9',    '0',    '-',    '=',    0x08,
                '\t',    'q',    'w',    'e',    'r',    't',    'y',    'u',    'i',    'o',    'p',    '[',    ']',    '\n',
                   0,    'a',    's',    'd',    'f',    'g',    'h',    'j',    'k',    'l',    ';',   '\'',    '`',       0,
                '\\',    'z',    'x',    'c',    'v',    'b',    'n',    'm',    ',',    '.',    '/',      0,    '*',       0,
                 ' ',      0,      0,
     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '\\', 0, 0, 0,
    },
};
// clang-format on

UNMAP_AFTER_INIT KeyboardDevice::KeyboardDevice()
    : IRQHandler(IRQ_KEYBOARD)
    , CharacterDevice(85, 1)
    , m_controller(I8042Controller::the())
    , m_character_map("en-us", DEFAULT_CHARACTER_MAP)
{
}

UNMAP_AFTER_INIT KeyboardDevice::~KeyboardDevice()
{
}

UNMAP_AFTER_INIT bool KeyboardDevice::initialize()
{
    if (!m_controller.reset_device(I8042Controller::Device::Keyboard)) {
        dbgln("KeyboardDevice: I8042 controller failed to reset device");
        return false;
    }
    return true;
}

bool KeyboardDevice::can_read(const FileDescription&, size_t) const
{
    return !m_queue.is_empty();
}

KResultOr<size_t> KeyboardDevice::read(FileDescription&, size_t, UserOrKernelBuffer& buffer, size_t size)
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

        ssize_t n = buffer.write_buffered<sizeof(Event)>(sizeof(Event), [&](u8* data, size_t data_bytes) {
            memcpy(data, &event, sizeof(Event));
            return (ssize_t)data_bytes;
        });
        if (n < 0)
            return KResult((ErrnoCode)-n);
        VERIFY((size_t)n == sizeof(Event));
        nread += sizeof(Event);

        lock.lock();
    }
    return nread;
}

KResultOr<size_t> KeyboardDevice::write(FileDescription&, size_t, const UserOrKernelBuffer&, size_t)
{
    return 0;
}

KeyboardClient::~KeyboardClient()
{
}

void KeyboardDevice::set_maps(const Keyboard::CharacterMapData& character_map_data, const String& character_map_name)
{
    m_character_map.set_character_map_data(character_map_data);
    m_character_map.set_character_map_name(character_map_name);
    dbgln("New Character map '{}' passed in by client.", character_map_name);
}

}
