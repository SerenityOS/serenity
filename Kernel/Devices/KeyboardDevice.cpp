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
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/TTY/VirtualConsole.h>
#include <LibBareMetal/IO.h>

//#define KEYBOARD_DEBUG

namespace Kernel {

#define IRQ_KEYBOARD 1
#define I8042_BUFFER 0x60
#define I8042_STATUS 0x64
#define I8042_ACK 0xFA
#define I8042_BUFFER_FULL 0x01
#define I8042_WHICH_BUFFER 0x20
#define I8042_MOUSE_BUFFER 0x20
#define I8042_KEYBOARD_BUFFER 0x00

char* map;
char* shift_map;
char* alt_map;
char* altgr_map;

static const char en_map[0x80] = {
    0,
    '\033',
    '1',
    '2',
    '3',
    '4',
    '5',
    '6',
    '7',
    '8',
    '9',
    '0',
    '-',
    '=',
    0x08,
    '\t',
    'q',
    'w',
    'e',
    'r',
    't',
    'y',
    'u',
    'i',
    'o',
    'p',
    '[',
    ']',
    '\n',
    0,
    'a',
    's',
    'd',
    'f',
    'g',
    'h',
    'j',
    'k',
    'l',
    ';',
    '\'',
    '`',
    0,
    '\\',
    'z',
    'x',
    'c',
    'v',
    'b',
    'n',
    'm',
    ',',
    '.',
    '/',
    0,
    '*',
    0,
    ' ',
    0,
    0,
    //60
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    //70
    0,
    0,
    0,
    0,
    '-',
    0,
    0,
    0,
    '+',
    0,
    //80
    0,
    0,
    0,
    0,
    0,
    0,
    '\\',
    0,
    0,
    0,
};

static const char en_shift_map[0x80] = {
    0,
    '\033',
    '!',
    '@',
    '#',
    '$',
    '%',
    '^',
    '&',
    '*',
    '(',
    ')',
    '_',
    '+',
    0x08,
    '\t',
    'Q',
    'W',
    'E',
    'R',
    'T',
    'Y',
    'U',
    'I',
    'O',
    'P',
    '{',
    '}',
    '\n',
    0,
    'A',
    'S',
    'D',
    'F',
    'G',
    'H',
    'J',
    'K',
    'L',
    ':',
    '"',
    '~',
    0,
    '|',
    'Z',
    'X',
    'C',
    'V',
    'B',
    'N',
    'M',
    '<',
    '>',
    '?',
    0,
    '*',
    0,
    ' ',
    0,
    0,
    //60
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    //70
    0,
    0,
    0,
    0,
    '-',
    0,
    0,
    0,
    '+',
    0,
    //80
    0,
    0,
    0,
    0,
    0,
    0,
    '|',
    0,
    0,
    0,
};

static const char numpad_map[13] = { '7', '8', '9', 0, '4', '5', '6', 0, '1', '2', '3', '0', ',' };

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
    Key_Logo,
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
    Key_Logo,
};

static const KeyCode numpad_key_map[13] = { Key_7, Key_8, Key_9, Key_Invalid, Key_4, Key_5, Key_6, Key_Invalid, Key_1, Key_2, Key_3, Key_0, Key_Comma };

void KeyboardDevice::key_state_changed(u8 raw, bool pressed)
{
    KeyCode key = (m_modifiers & Mod_Shift) ? shifted_key_map[raw] : unshifted_key_map[raw];
    char character = (m_modifiers & Mod_Shift) ? shift_map[raw] : (m_modifiers & Mod_Alt) ? alt_map[raw] : (m_modifiers & Mod_AltGr) ? altgr_map[raw] : map[raw];

    if (key == Key_NumLock && pressed)
        m_num_lock_on = !m_num_lock_on;

    if (m_num_lock_on && !m_has_e0_prefix) {
        if (raw >= 0x47 && raw <= 0x53) {
            u8 index = raw - 0x47;
            KeyCode newKey = numpad_key_map[index];

            if (newKey != Key_Invalid) {
                key = newKey;
                character = numpad_map[index];
            }
        }
    } else {
        if (m_has_e0_prefix) {
            if (key == Key_Slash) {
                character = '/'; // On Turkish-QWERTY Keyboard Key_Slash mapped to '.' char, if e0 prefix is true remap to '/' char
            }
        }
    }

    if (key == Key_CapsLock && pressed)
        m_caps_lock_on = !m_caps_lock_on;

    if (m_caps_lock_on && (m_modifiers == 0 || m_modifiers == Mod_Shift)) {
        if (character >= 'a' && character <= 'z')
            character &= ~0x20;
        else if (character >= 'A' && character <= 'Z')
            character |= 0x20;
    }

    Event event;
    event.key = key;
    event.character = static_cast<u8>(character);
    event.flags = m_modifiers;
    if (pressed)
        event.flags |= Is_Press;
    if (m_client)
        m_client->on_key_pressed(event);
    m_queue.enqueue(event);

    m_has_e0_prefix = false;
}

void KeyboardDevice::handle_irq(RegisterState&)
{
    for (;;) {
        u8 status = IO::in8(I8042_STATUS);
        if (!(((status & I8042_WHICH_BUFFER) == I8042_KEYBOARD_BUFFER) && (status & I8042_BUFFER_FULL)))
            return;
        u8 raw = IO::in8(I8042_BUFFER);
        u8 ch = raw & 0x7f;
        bool pressed = !(raw & 0x80);

        if (raw == 0xe0) {
            m_has_e0_prefix = true;
            return;
        }

#ifdef KEYBOARD_DEBUG
        dbgprintf("Keyboard::handle_irq: %b %s\n", ch, pressed ? "down" : "up");
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
            update_modifier(Mod_Logo, pressed);
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
                switch (map[ch]) {
                case '1':
                case '2':
                case '3':
                case '4':
                    VirtualConsole::switch_to(map[ch] - '0' - 1);
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
}

static KeyboardDevice* s_the;

KeyboardDevice& KeyboardDevice::the()
{
    ASSERT(s_the);
    return *s_the;
}

KeyboardDevice::KeyboardDevice()
    : IRQHandler(IRQ_KEYBOARD)
    , CharacterDevice(85, 1)
{
    s_the = this;

    KeyboardDevice::set_maps(en_map, en_shift_map, en_map, en_map);

    // Empty the buffer of any pending data.
    // I don't care what you've been pressing until now!
    while (IO::in8(I8042_STATUS) & I8042_BUFFER_FULL)
        IO::in8(I8042_BUFFER);

    enable_irq();
}

KeyboardDevice::~KeyboardDevice()
{
}

bool KeyboardDevice::can_read(const FileDescription&) const
{
    return !m_queue.is_empty();
}

ssize_t KeyboardDevice::read(FileDescription&, u8* buffer, ssize_t size)
{
    ssize_t nread = 0;
    while (nread < size) {
        if (m_queue.is_empty())
            break;
        // Don't return partial data frames.
        if ((size - nread) < (ssize_t)sizeof(Event))
            break;
        auto event = m_queue.dequeue();
        memcpy(buffer, &event, sizeof(Event));
        nread += sizeof(Event);
    }
    return nread;
}

ssize_t KeyboardDevice::write(FileDescription&, const u8*, ssize_t)
{
    return 0;
}

KeyboardClient::~KeyboardClient()
{
}

void KeyboardDevice::set_maps(const char* n_map, const char* n_shift_map, const char* n_alt_map, const char* n_altgr_map)
{
    kfree(map);
    kfree(shift_map);
    kfree(alt_map);
    kfree(altgr_map);

    map = (char*)kmalloc(0x80);
    shift_map = (char*)kmalloc(0x80);
    alt_map = (char*)kmalloc(0x80);
    altgr_map = (char*)kmalloc(0x80);

    for (int i = 0; i < 0x80; i++) {
        map[i] = n_map[i];
        shift_map[i] = n_shift_map[i];
        alt_map[i] = n_alt_map[i];
        altgr_map[i] = n_altgr_map[i];
    }
}

}
