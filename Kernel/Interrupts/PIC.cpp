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
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Interrupts/PIC.h>
#include <LibBareMetal/IO.h>

namespace Kernel {

// The slave 8259 is connected to the master's IRQ2 line.
// This is really only to enhance clarity.
#define SLAVE_INDEX 2

#define PIC0_CTL 0x20
#define PIC0_CMD 0x21
#define PIC1_CTL 0xA0
#define PIC1_CMD 0xA1

#define ICW1_ICW4 0x01      /* ICW4 (not) needed */
#define ICW1_SINGLE 0x02    /* Single (cascade) mode */
#define ICW1_INTERVAL4 0x04 /* Call address interval 4 (8) */
#define ICW1_LEVEL 0x08     /* Level triggered (edge) mode */
#define ICW1_INIT 0x10      /* Initialization - required! */

#define ICW4_8086 0x01       /* 8086/88 (MCS-80/85) mode */
#define ICW4_AUTO 0x02       /* Auto (normal) EOI */
#define ICW4_BUF_SLAVE 0x08  /* Buffered mode/slave */
#define ICW4_BUF_MASTER 0x0C /* Buffered mode/master */
#define ICW4_SFNM 0x10       /* Special fully nested (not) */

bool inline static is_all_masked(u8 reg)
{
    return reg == 0xFF;
}

void PIC::disable(u8 irq)
{
    InterruptDisabler disabler;
    if (is_hard_disabled())
        return;
    u8 imr;
    if (irq >= 8) {
        imr = IO::in8(PIC1_CMD);
        imr |= 1 << (irq - 8);
        IO::out8(PIC1_CMD, imr);
    } else {
        imr = IO::in8(PIC0_CMD);
        imr |= 1 << irq;
        IO::out8(PIC0_CMD, imr);
    }

    if (is_all_masked(imr))
        m_enabled = false;
}

PIC::PIC()
{
    initialize();
}

bool PIC::is_vector_enabled(u8 irq) const
{
    u8 imr;
    if (irq >= 8) {
        imr = IO::in8(PIC1_CMD);
        imr &= 1 << (irq - 8);
    } else {
        imr = IO::in8(PIC0_CMD);
        imr &= 1 << irq;
    }
    return imr != 0;
}

void PIC::enable(u8 irq)
{
    InterruptDisabler disabler;
    if (is_hard_disabled())
        return;
    u8 imr;
    if (irq >= 8) {
        imr = IO::in8(PIC1_CMD);
        imr &= ~(1 << (irq - 8));
        IO::out8(PIC1_CMD, imr);
    } else {
        imr = IO::in8(PIC0_CMD);
        imr &= ~(1 << irq);
        IO::out8(PIC0_CMD, imr);
    }
    m_enabled = true;
}

void PIC::eoi(u8 irq) const
{
    InterruptDisabler disabler;
    if (is_hard_disabled())
        return;
    if (irq >= 8)
        IO::out8(PIC1_CTL, 0x20);
    IO::out8(PIC0_CTL, 0x20);
}

void PIC::complete_eoi() const
{
    IO::out8(PIC1_CTL, 0x20);
    IO::out8(PIC0_CTL, 0x20);
}

void PIC::hard_disable()
{
    InterruptDisabler disabler;
    remap(0x20);
    IO::out8(PIC0_CMD, 0xff);
    IO::out8(PIC1_CMD, 0xff);
    IRQController::hard_disable();
}

void PIC::remap(u8 offset)
{
    /* ICW1 (edge triggered mode, cascading controllers, expect ICW4) */
    IO::out8(PIC0_CTL, ICW1_INIT | ICW1_ICW4);
    IO::out8(PIC1_CTL, ICW1_INIT | ICW1_ICW4);

    /* ICW2 (upper 5 bits specify ISR indices, lower 3 idunno) */
    IO::out8(PIC0_CMD, offset);
    IO::out8(PIC1_CMD, offset + 0x08);

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
}

void PIC::initialize()
{
    /* ICW1 (edge triggered mode, cascading controllers, expect ICW4) */
    IO::out8(PIC0_CTL, ICW1_INIT | ICW1_ICW4);
    IO::out8(PIC1_CTL, ICW1_INIT | ICW1_ICW4);

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

    kprintf("PIC(i8259): cascading mode, vectors 0x%b-0x%b\n", IRQ_VECTOR_BASE, IRQ_VECTOR_BASE + 0xf);
}

u16 PIC::get_isr() const
{
    IO::out8(PIC0_CTL, 0x0b);
    IO::out8(PIC1_CTL, 0x0b);
    u8 isr0 = IO::in8(PIC0_CTL);
    u8 isr1 = IO::in8(PIC1_CTL);
    return (isr1 << 8) | isr0;
}

u16 PIC::get_irr() const
{
    IO::out8(PIC0_CTL, 0x0a);
    IO::out8(PIC1_CTL, 0x0a);
    u8 irr0 = IO::in8(PIC0_CTL);
    u8 irr1 = IO::in8(PIC1_CTL);
    return (irr1 << 8) | irr0;
}
}
