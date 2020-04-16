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

#include <Kernel/Assertions.h>
#include <Kernel/Time/HPETComparator.h>
#include <Kernel/Time/TimeManagement.h>

//#define HPET_COMPARATOR_DEBUG

namespace Kernel {

NonnullRefPtr<HPETComparator> HPETComparator::create(u8 number, u8 irq, bool periodic_capable)
{
    return adopt(*new HPETComparator(number, irq, periodic_capable));
}

HPETComparator::HPETComparator(u8 number, u8 irq, bool periodic_capable)
    : HardwareTimer(irq)
    , m_periodic(false)
    , m_periodic_capable(periodic_capable)
    , m_comparator_number(number)
{
}

void HPETComparator::set_periodic()
{
    ASSERT(m_periodic_capable);
    HPET::the().enable_periodic_interrupt(*this);
    m_periodic = true;
}
void HPETComparator::set_non_periodic()
{
    ASSERT(m_periodic_capable);
    HPET::the().disable_periodic_interrupt(*this);
    m_periodic = false;
}

void HPETComparator::handle_irq(const RegisterState& regs)
{
    HardwareTimer::handle_irq(regs);
    if (!is_periodic())
        set_new_countdown();
}

void HPETComparator::set_new_countdown()
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(m_frequency <= HPET::the().frequency());
    HPET::the().set_non_periodic_comparator_value(*this, HPET::the().frequency() / m_frequency);
}

size_t HPETComparator::ticks_per_second() const
{
    return m_frequency;
}

void HPETComparator::reset_to_default_ticks_per_second()
{
    ASSERT(is_capable_of_frequency(OPTIMAL_TICKS_PER_SECOND_RATE));
    m_frequency = OPTIMAL_TICKS_PER_SECOND_RATE;
    if (!is_periodic())
        set_new_countdown();
    else
        try_to_set_frequency(m_frequency);
}
bool HPETComparator::try_to_set_frequency(size_t frequency)
{
    InterruptDisabler disabler;
    if (!is_capable_of_frequency(frequency))
        return false;
    disable_irq();
    auto hpet_frequency = HPET::the().frequency();
    ASSERT(frequency <= hpet_frequency);
#ifdef HPET_COMPARATOR_DEBUG
    dbg() << "HPET Comparator: Max frequency - " << hpet_frequency << " Hz, want to set " << frequency << " Hz";
#endif
    if (is_periodic())
        HPET::the().set_periodic_comparator_value(*this, hpet_frequency / frequency);
    else {
        HPET::the().set_non_periodic_comparator_value(*this, hpet_frequency / frequency);
        HPET::the().enable(*this);
    }
    m_frequency = frequency;
    enable_irq();
    return true;
}
bool HPETComparator::is_capable_of_frequency(size_t frequency) const
{
    if (frequency > HPET::the().frequency())
        return false;
    if ((HPET::the().frequency() % frequency) != 0)
        return false;
    return true;
}
size_t HPETComparator::calculate_nearest_possible_frequency(size_t frequency) const
{
    if (frequency >= HPET::the().frequency())
        return HPET::the().frequency();
    // FIXME: Use better math here
    return (frequency + (HPET::the().frequency() % frequency));
}

}
