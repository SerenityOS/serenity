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
#include <Kernel/Devices/PIT.h>
#include <Kernel/Interrupts/PIC.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Thread.h>
#include <LibBareMetal/IO.h>

#define IRQ_TIMER 0
namespace Kernel {

//#define PIT_DEBUG

static PIT* s_the;

void PIT::initialize()
{
    if (s_the == nullptr) {
        s_the = new PIT();
    }
}

PIT& PIT::the()
{
    ASSERT(s_the != nullptr);
    return *s_the;
}

inline static void reset_countdown(u16 timer_reload)
{
    IO::out8(PIT_CTL, TIMER0_SELECT | WRITE_WORD | MODE_COUNTDOWN);
    IO::out8(TIMER0_CTL, LSB(timer_reload));
    IO::out8(TIMER0_CTL, MSB(timer_reload));
}

void PIT::handle_irq(RegisterState& regs)
{
#ifdef PIT_DEBUG
    dbg() << "PIT: Debugging Interrupt.";
#endif
    if (++m_ticks_this_second >= TICKS_PER_SECOND) {
        // FIXME: Synchronize with the RTC somehow to prevent drifting apart.
        ++m_seconds_since_boot;
        m_ticks_this_second = 0;
    }
    Scheduler::timer_tick(regs);
}

u32 PIT::ticks_this_second() const
{
    return m_ticks_this_second;
}

PIT::PIT()
    : HardwareTimer(IRQ_TIMER)
    , m_default_timer_reload(BASE_FREQUENCY / TICKS_PER_SECOND)
    , m_ticks_this_second(0)
{

    IO::out8(PIT_CTL, TIMER0_SELECT | WRITE_WORD | MODE_SQUARE_WAVE);

    kprintf("PIT: %u Hz, square wave (%x)\n", TICKS_PER_SECOND, m_default_timer_reload);

    IO::out8(TIMER0_CTL, LSB(m_default_timer_reload));
    IO::out8(TIMER0_CTL, MSB(m_default_timer_reload));

    enable_irq();
}

}
