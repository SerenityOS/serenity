#include "types.h"
#include "i386.h"
#include "IO.h"
#include "VGA.h"
#include "PIC.h"
#include "Keyboard.h"
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

static char map[0x100] =
{
    0, 0, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', 0, 0,
    'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', 0, 0,
    'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'', '`', 0, '\\',
    'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/',
};

static char shift_map[0x100] =
{
    0, 0, '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', 0, 0,
    'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', 0, 0,
    'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '"', '~', 0, '|',
    'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>', '?',
};


void Keyboard::handleIRQ()
{
    while (IO::in8(0x64) & 1) {
        BYTE ch = IO::in8(0x60);
        switch (ch) {
        case 0x38: m_modifiers |= MOD_ALT; break;
        case 0xB8: m_modifiers &= ~MOD_ALT; break;
        case 0x1D: m_modifiers |= MOD_CTRL; break;
        case 0x9D: m_modifiers &= ~MOD_CTRL; break;
        case 0x2A: m_modifiers |= MOD_SHIFT; break;
        case 0xAA: m_modifiers &= ~MOD_SHIFT; break;
        case 0x1C: /* enter */ kprintf("\n"); break;
        case 0xFA: /* i8042 ack */ break;
        default:
            if (ch & 0x80) {
                // key has been depressed
                break;
            }
            if (!m_modifiers)
                kprintf("%c", map[ch]);
            else if (m_modifiers & MOD_SHIFT)
                kprintf("%c", shift_map[ch]);
            else if (m_modifiers & MOD_CTRL)
                kprintf("^%c", shift_map[ch]);
        }
        //break;
    }
}

Keyboard::Keyboard()
    : IRQHandler(IRQ_KEYBOARD)
{
    // Empty the buffer of any pending data.
    // I don't care what you've been pressing until now!
    while (IO::in8(I8042_STATUS ) & DATA_AVAILABLE)
        IO::in8(I8042_BUFFER);

    enableIRQ();
}

Keyboard::~Keyboard()
{
    ASSERT_NOT_REACHED();
}

