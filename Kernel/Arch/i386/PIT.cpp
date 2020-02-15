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

#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/PIC.h>
#include <Kernel/Arch/i386/PIT.h>
#include <Kernel/Scheduler.h>
#include <LibBareMetal/IO.h>

#define IRQ_TIMER 0

extern "C" void timer_interrupt_entry();
extern "C" void timer_interrupt_handler(RegisterState);

asm(
    ".globl timer_interrupt_entry \n"
    "timer_interrupt_entry: \n"
    "    pushl $0x0\n"
    "    pusha\n"
    "    pushl %ds\n"
    "    pushl %es\n"
    "    pushl %fs\n"
    "    pushl %gs\n"
    "    pushl %ss\n"
    "    mov $0x10, %ax\n"
    "    mov %ax, %ds\n"
    "    mov %ax, %es\n"
    "    cld\n"
    "    call timer_interrupt_handler\n"
    "    add $0x4, %esp\n"
    "    popl %gs\n"
    "    popl %fs\n"
    "    popl %es\n"
    "    popl %ds\n"
    "    popa\n"
    "    add $0x4, %esp\n"
    "    iret\n");

static u32 s_ticks_this_second;
static u32 s_seconds_since_boot;

void timer_interrupt_handler(RegisterState regs)
{
    clac();
    IRQHandlerScope scope(IRQ_TIMER);
    if (++s_ticks_this_second >= TICKS_PER_SECOND) {
        // FIXME: Synchronize with the RTC somehow to prevent drifting apart.
        ++s_seconds_since_boot;
        s_ticks_this_second = 0;
    }
    Scheduler::timer_tick(regs);
}

namespace PIT {

u32 ticks_this_second()
{
    return s_ticks_this_second;
}

u32 seconds_since_boot()
{
    return s_seconds_since_boot;
}

void initialize()
{
    u16 timer_reload;

    IO::out8(PIT_CTL, TIMER0_SELECT | WRITE_WORD | MODE_SQUARE_WAVE);

    timer_reload = (BASE_FREQUENCY / TICKS_PER_SECOND);

    kprintf("PIT: %u Hz, square wave (%x)\n", TICKS_PER_SECOND, timer_reload);

    IO::out8(TIMER0_CTL, LSB(timer_reload));
    IO::out8(TIMER0_CTL, MSB(timer_reload));

    register_interrupt_handler(IRQ_VECTOR_BASE + IRQ_TIMER, timer_interrupt_entry);

    PIC::enable(IRQ_TIMER);
}

}
