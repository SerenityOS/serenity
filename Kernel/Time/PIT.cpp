/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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
#include <Kernel/IO.h>
#include <Kernel/Interrupts/PIC.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Thread.h>
#include <Kernel/Time/PIT.h>
#include <Kernel/Time/TimeManagement.h>

#define IRQ_TIMER 0
namespace Kernel {

NonnullRefPtr<PIT> PIT::initialize(Function<void(const RegisterState&)> callback)
{
    return adopt(*new PIT(move(callback)));
}

inline static void reset_countdown(u16 timer_reload)
{
    IO::out8(PIT_CTL, TIMER0_SELECT | WRITE_WORD | MODE_COUNTDOWN);
    IO::out8(TIMER0_CTL, LSB(timer_reload));
    IO::out8(TIMER0_CTL, MSB(timer_reload));
}

PIT::PIT(Function<void(const RegisterState&)> callback)
    : HardwareTimer(IRQ_TIMER, move(callback))
    , m_periodic(true)
{
    IO::out8(PIT_CTL, TIMER0_SELECT | WRITE_WORD | MODE_SQUARE_WAVE);

    klog() << "PIT: " << OPTIMAL_TICKS_PER_SECOND_RATE << " Hz, square wave (" << String::format("%x", BASE_FREQUENCY / OPTIMAL_TICKS_PER_SECOND_RATE) << ")";
    reset_to_default_ticks_per_second();
    enable_irq();
}

size_t PIT::ticks_per_second() const
{
    return m_frequency;
}

void PIT::set_periodic()
{
    // FIXME: Implement it...
    ASSERT_NOT_REACHED();
}
void PIT::set_non_periodic()
{
    // FIXME: Implement it...
    ASSERT_NOT_REACHED();
}

void PIT::reset_to_default_ticks_per_second()
{
    InterruptDisabler disabler;
    bool success = try_to_set_frequency(OPTIMAL_TICKS_PER_SECOND_RATE);
    ASSERT(success);
}

bool PIT::try_to_set_frequency(size_t frequency)
{
    InterruptDisabler disabler;
    if (!is_capable_of_frequency(frequency))
        return false;
    disable_irq();
    size_t reload_value = BASE_FREQUENCY / frequency;
    IO::out8(TIMER0_CTL, LSB(reload_value));
    IO::out8(TIMER0_CTL, MSB(reload_value));
    m_frequency = frequency;
    enable_irq();
    return true;
}
bool PIT::is_capable_of_frequency(size_t frequency) const
{
    ASSERT(frequency != 0);
    return frequency <= BASE_FREQUENCY;
}
size_t PIT::calculate_nearest_possible_frequency(size_t frequency) const
{
    ASSERT(frequency != 0);
    return frequency;
}

}
