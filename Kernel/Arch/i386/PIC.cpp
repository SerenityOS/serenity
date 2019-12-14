#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/PIC.h>
#include <Kernel/IO.h>

// The slave 8259 is connected to the master's IRQ2 line.
// This is really only to enhance clarity.
#define SLAVE_INDEX 2

#define PIC0_CTL 0x20
#define PIC0_CMD 0x21
#define PIC1_CTL 0xA0
#define PIC1_CMD 0xA1

#ifdef DEBUG_PIC
static bool initialized;
#endif

namespace PIC {

void disable(u8 irq)
{
    InterruptDisabler disabler;
    u8 imr;
    if (irq & 8) {
        imr = IO::in8(PIC1_CMD);
        imr |= 1 << (irq - 8);
        IO::out8(PIC1_CMD, imr);
    } else {
        imr = IO::in8(PIC0_CMD);
        imr |= 1 << irq;
        IO::out8(PIC0_CMD, imr);
    }
}

void enable(u8 irq)
{
    InterruptDisabler disabler;
    u8 imr;
    if (irq & 8) {
        imr = IO::in8(PIC1_CMD);
        imr &= ~(1 << (irq - 8));
        IO::out8(PIC1_CMD, imr);
    } else {
        imr = IO::in8(PIC0_CMD);
        imr &= ~(1 << irq);
        IO::out8(PIC0_CMD, imr);
    }
}

void eoi(u8 irq)
{
    if (irq & 8)
        IO::out8(PIC1_CTL, 0x20);
    IO::out8(PIC0_CTL, 0x20);
}

void initialize()
{
#ifdef DEBUG_PIC
    ASSERT(!initialized);
#endif

    /* ICW1 (edge triggered mode, cascading controllers, expect ICW4) */
    IO::out8(PIC0_CTL, 0x11);
    IO::out8(PIC1_CTL, 0x11);

    /* ICW2 (upper 5 bits specify ISR indices, lower 3 idunno) */
    IO::out8(PIC0_CMD, IRQ_VECTOR_BASE);
    IO::out8(PIC1_CMD, IRQ_VECTOR_BASE + 0x08);

    /* ICW3 (configure master/slave relationship) */
    IO::out8(PIC0_CMD, 1 << SLAVE_INDEX);
    IO::out8(PIC1_CMD, SLAVE_INDEX);

    /* ICW4 (set x86 mode) */
    IO::out8(PIC0_CMD, 0x01);
    IO::out8(PIC1_CMD, 0x01);

    // Mask -- start out with all IRQs disabled.
    IO::out8(PIC0_CMD, 0xff);
    IO::out8(PIC1_CMD, 0xff);

    // ...except IRQ2, since that's needed for the master to let through slave interrupts.
    enable(2);

    kprintf("PIC(i8259): cascading mode, vectors 0x%b-0x%b\n", IRQ_VECTOR_BASE, IRQ_VECTOR_BASE + 0x08);

#ifdef DEBUG_PIC
    initialized = true;
#endif
}

u16 get_isr()
{
    IO::out8(PIC0_CTL, 0x0b);
    IO::out8(PIC1_CTL, 0x0b);
    u8 isr0 = IO::in8(PIC0_CTL);
    u8 isr1 = IO::in8(PIC1_CTL);
    return (isr1 << 8) | isr0;
}

u16 get_irr()
{
    IO::out8(PIC0_CTL, 0x0a);
    IO::out8(PIC1_CTL, 0x0a);
    u8 irr0 = IO::in8(PIC0_CTL);
    u8 irr1 = IO::in8(PIC1_CTL);
    return (irr1 << 8) | irr0;
}

}

