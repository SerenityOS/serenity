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
#include <Kernel/Debug.h>
#include <Kernel/Time/HPETComparator.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

NonnullRefPtr<HPETComparator> HPETComparator::create(u8 number, u8 irq, bool periodic_capable)
{
    return adopt(*new HPETComparator(number, irq, periodic_capable));
}

HPETComparator::HPETComparator(u8 number, u8 irq, bool periodic_capable)
    : HardwareTimer(irq)
    , m_periodic(false)
    , m_periodic_capable(periodic_capable)
    , m_enabled(false)
    , m_comparator_number(number)
{
}

void HPETComparator::disable()
{
    if (!m_enabled)
        return;
    m_enabled = false;
    HPET::the().disable(*this);
}

void HPETComparator::set_periodic()
{
    ASSERT(m_periodic_capable);
    m_periodic = true;
    m_enabled = true;
    HPET::the().enable_periodic_interrupt(*this);
}
void HPETComparator::set_non_periodic()
{
    ASSERT(m_periodic_capable);
    m_periodic = false;
    m_enabled = true;
    HPET::the().disable_periodic_interrupt(*this);
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
    HPET::the().update_non_periodic_comparator_value(*this);
}

size_t HPETComparator::ticks_per_second() const
{
    return m_frequency;
}

void HPETComparator::reset_to_default_ticks_per_second()
{
    dbgln("reset_to_default_ticks_per_second");
    m_frequency = OPTIMAL_TICKS_PER_SECOND_RATE;
    if (!is_periodic())
        set_new_countdown();
    else
        try_to_set_frequency(m_frequency);
}
bool HPETComparator::try_to_set_frequency(size_t frequency)
{
    InterruptDisabler disabler;
    if (!is_capable_of_frequency(frequency)) {
        dbgln("HPETComparator: not cable of frequency: {}", frequency);
        return false;
    }

    auto hpet_frequency = HPET::the().frequency();
    ASSERT(frequency <= hpet_frequency);
    m_frequency = frequency;
    m_enabled = true;

    dbgln_if(HPET_COMPARATOR_DEBUG, "HPET Comparator: Max frequency {} Hz, want to set {} Hz, periodic: {}", hpet_frequency, frequency, is_periodic());

    if (is_periodic()) {
        HPET::the().update_periodic_comparator_value();
    } else {
        HPET::the().update_non_periodic_comparator_value(*this);
    }
    enable_irq(); // Enable if we haven't already
    return true;
}
bool HPETComparator::is_capable_of_frequency(size_t frequency) const
{
    if (frequency > HPET::the().frequency())
        return false;
    // HPET::update_periodic_comparator_value and HPET::update_non_periodic_comparator_value
    // calculate the best counter based on the desired frequency.
    return true;
}
size_t HPETComparator::calculate_nearest_possible_frequency(size_t frequency) const
{
    if (frequency > HPET::the().frequency())
        return HPET::the().frequency();
    // HPET::update_periodic_comparator_value and HPET::update_non_periodic_comparator_value
    // calculate the best counter based on the desired frequency.
    return frequency;
}

}
