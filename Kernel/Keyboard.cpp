#include "types.h"
#include "i386.h"
#include "IO.h"
#include "PIC.h"
#include "Keyboard.h"
#include "VirtualConsole.h"
#include <AK/Assertions.h>

#define IRQ_KEYBOARD             1

#define I8042_BUFFER             0x60
#define I8042_STATUS             0x64

#define SET_LEDS                 0xED
#define DATA_AVAILABLE           0x01
#define I8042_ACK                0xFA

#define MOD_ALT     1
#define MOD_CTRL    2
#define MOD_SHIFT   4

static char map[0x80] =
{
    0, 0, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', 0x08, 0,
    'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', '\n', 0,
    'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'', '`', 0, '\\',
    'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/',
    0, 0, 0, ' '
};

static char shift_map[0x80] =
{
    0, 0, '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', 0, 0,
    'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', '\n', 0,
    'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '"', '~', 0, '|',
    'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>', '?',
    0, 0, 0, ' '
};

static KeyCode unshifted_key_map[0x80] =
{
    Key_Invalid, Key_Invalid,
    Key_1, Key_2, Key_3, Key_4, Key_5, Key_6, Key_7, Key_8, Key_9, Key_0, Key_Minus, Key_Equal, Key_Backspace,
    Key_Invalid, //15
    Key_Q, Key_W, Key_E, Key_R, Key_T, Key_Y, Key_U, Key_I, Key_O, Key_P, Key_LeftBracket, Key_RightBracket,
    Key_Return, // 28
    Key_Control, // 29
    Key_A, Key_S, Key_D, Key_F, Key_G, Key_H, Key_J, Key_K, Key_L, Key_Semicolon, Key_Apostrophe, Key_Backtick,
    Key_Shift,
    Key_Backslash,
    Key_Z, Key_X, Key_C, Key_V, Key_B, Key_N, Key_M, Key_Comma, Key_Period, Key_Slash,
    Key_Alt,
    Key_Invalid, Key_Invalid,
    Key_Space
};

static KeyCode shifted_key_map[0x100] =
{
    Key_Invalid, Key_Invalid,
    Key_ExclamationPoint, Key_AtSign, Key_Hashtag, Key_Dollar, Key_Percent, Key_Circumflex, Key_Ampersand, Key_Asterisk, Key_LeftParen, Key_RightParen, Key_Underscore, Key_Plus, Key_Backspace,
    Key_Invalid,
    Key_Q, Key_W, Key_E, Key_R, Key_T, Key_Y, Key_U, Key_I, Key_O, Key_P, Key_LeftBrace, Key_RightBrace,
    Key_Return,
    Key_Control,
    Key_A, Key_S, Key_D, Key_F, Key_G, Key_H, Key_J, Key_K, Key_L, Key_Colon, Key_DoubleQuote, Key_Tilde,
    Key_Shift,
    Key_Pipe,
    Key_Z, Key_X, Key_C, Key_V, Key_B, Key_N, Key_M, Key_LessThan, Key_GreaterThan, Key_QuestionMark,
    Key_Alt,
    Key_Invalid, Key_Invalid,
    Key_Space
};


void Keyboard::key_state_changed(byte raw, bool pressed)
{
    Event event;
    event.key = (m_modifiers & Mod_Shift) ? shifted_key_map[raw] : unshifted_key_map[raw];
    event.character = (m_modifiers & Mod_Shift) ? shift_map[raw] : map[raw];
    event.flags = m_modifiers;
    if (pressed)
        event.flags |= Is_Press;
    if (m_client)
        m_client->on_key_pressed(event);
    m_queue.enqueue(event);
}

void Keyboard::handle_irq()
{
    while (IO::in8(0x64) & 1) {
        byte raw = IO::in8(0x60);
        byte ch = raw & 0x7f;
        bool pressed = !(raw & 0x80);

        switch (ch) {
        case 0x38: update_modifier(Mod_Alt, pressed); break;
        case 0x1d: update_modifier(Mod_Ctrl, pressed); break;
        case 0x2a: update_modifier(Mod_Shift, pressed); break;
        case 0xfa: /* i8042 ack */ break;
        default:
            if (m_modifiers & MOD_ALT) {
                switch (map[ch]) {
                case '1':
                case '2':
                case '3':
                case '4':
                    VirtualConsole::switch_to(map[ch] - '0' - 1);
                    break;
                default:
                    break;
                }
            }
            key_state_changed(ch, pressed);
        }
    }
}

static Keyboard* s_the;

Keyboard& Keyboard::the()
{
    ASSERT(s_the);
    return *s_the;
}

Keyboard::Keyboard()
    : IRQHandler(IRQ_KEYBOARD)
    , CharacterDevice(85, 1)
{
    s_the = this;

    // Empty the buffer of any pending data.
    // I don't care what you've been pressing until now!
    while (IO::in8(I8042_STATUS ) & DATA_AVAILABLE)
        IO::in8(I8042_BUFFER);

    enable_irq();
}

Keyboard::~Keyboard()
{
}

bool Keyboard::can_read(Process&) const
{
    return !m_queue.is_empty();
}

ssize_t Keyboard::read(Process&, byte* buffer, size_t size)
{
    ssize_t nread = 0;
    while ((size_t)nread < size) {
        if (m_queue.is_empty())
            break;
        // Don't return partial data frames.
        if ((size - nread) < sizeof(Event))
            break;
        auto event = m_queue.dequeue();
        memcpy(buffer, &event, sizeof(Event));
        nread += sizeof(Event);
    }
    return nread;
}

ssize_t Keyboard::write(Process&, const byte*, size_t)
{
    return 0;
}

KeyboardClient::~KeyboardClient()
{
}
