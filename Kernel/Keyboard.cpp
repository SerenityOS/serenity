#include "types.h"
#include "i386.h"
#include "IO.h"
#include "IPC.h"
#include "Task.h"
#include "VGA.h"
#include "PIC.h"
#include "Keyboard.h"

#define IRQ_KEYBOARD             1

#define I8042_BUFFER             0x60
#define I8042_STATUS             0x64

#define SET_LEDS                 0xED
#define DATA_AVAILABLE           0x01
#define I8042_ACK                0xFA

extern "C" void handleKeyboardInterrupt();
extern "C" void keyboard_ISR();

static BYTE s_ledState;

asm(
    ".globl keyboard_ISR \n"
    "keyboard_ISR: \n"
    "    pusha\n"
    "    pushw %ds\n"
    "    pushw %es\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    popw %ds\n"
    "    popw %es\n"
    "    call handleKeyboardInterrupt\n"
    "    popw %es\n"
    "    popw %ds\n"
    "    popa\n"
    "    iret\n"
);

void handleKeyboardInterrupt()
{
    IRQHandlerScope scope(IRQ_KEYBOARD);
    Keyboard::handleInterrupt();
}

namespace Keyboard {

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

static BYTE s_modifiers;

void handleInterrupt()
{
    while (IO::in8(0x64) & 1) {
        BYTE ch = IO::in8(0x60);
        switch (ch) {
        case 0x38: s_modifiers |= MOD_ALT; break;
        case 0xB8: s_modifiers &= ~MOD_ALT; break;
        case 0x1D: s_modifiers |= MOD_CTRL; break;
        case 0x9D: s_modifiers &= ~MOD_CTRL; break;
        case 0x2A: s_modifiers |= MOD_SHIFT; break;
        case 0xAA: s_modifiers &= ~MOD_SHIFT; break;
        case 0x1C: /* enter */ kprintf("\n"); break;
        case 0xFA: /* i8042 ack */ break;
        default:
            if (ch & 0x80) {
                // key has been depressed
                break;
            }
            if (!s_modifiers)
                kprintf("%c", map[ch]);
            else if (s_modifiers & MOD_SHIFT)
                kprintf("%c", shift_map[ch]);
            else if (s_modifiers & MOD_CTRL)
                kprintf("^%c", shift_map[ch]);
        }
        //break;
    }
}

void initialize()
{
    s_modifiers = 0;
    s_ledState = 0;

    // Empty the buffer of any pending data.
    // I don't care what you've been pressing until now!
    while (IO::in8(I8042_STATUS ) & DATA_AVAILABLE)
        IO::in8(I8042_BUFFER);

    registerInterruptHandler(IRQ_VECTOR_BASE + IRQ_KEYBOARD, keyboard_ISR);

    PIC::enable(IRQ_KEYBOARD);
}

void setLED(LED led)
{
    s_ledState |= (BYTE)led & 7;

    while (IO::in8(I8042_STATUS) & DATA_AVAILABLE);
    IO::out8(I8042_BUFFER, SET_LEDS);
    while (IO::in8(I8042_BUFFER) != I8042_ACK);
    IO::out8(I8042_BUFFER, s_ledState);
}

void unsetLED(LED led)
{
    s_ledState &= ~((BYTE)led & 7);

    while (IO::in8(I8042_STATUS) & DATA_AVAILABLE);
    IO::out8(I8042_BUFFER, SET_LEDS);
    while (IO::in8(I8042_BUFFER) != I8042_ACK);
    IO::out8(I8042_BUFFER, s_ledState);
}

}
