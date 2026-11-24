/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/Time/HPETComparator.h>
#include <Kernel/Debug.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Library/Assertions.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<HPETComparator> HPETComparator::create(Badge<HPET>, u8 number, InterruptNumber irq, bool periodic_capable, bool is_64bit_capable)
{
    auto timer = adopt_lock_ref(*new HPETComparator(number, irq, periodic_capable, is_64bit_capable));
    timer->register_interrupt_handler();
    return timer;
}

UNMAP_AFTER_INIT HPETComparator::HPETComparator(u8 number, InterruptNumber irq, bool periodic_capable, bool is_64bit_capable)
    : HardwareTimer(irq)
    , m_periodic(false)
    , m_periodic_capable(periodic_capable)
    , m_enabled(false)
    , m_is_64bit_capable(is_64bit_capable)
    , m_comparator_number(number)
{
}

void HPETComparator::disable()
{
    if (!m_enabled)
        return;
    m_enabled = false;
    HPET::the().toggle_comparator(*this, false);
}

void HPETComparator::set_periodic()
{
    VERIFY(m_periodic_capable);

    m_periodic = true;
    m_enabled = true;
    HPET::the().set_comparator_periodic(*this, true);
}

void HPETComparator::set_non_periodic()
{
    m_periodic = false;
    m_enabled = true;
    HPET::the().set_comparator_periodic(*this, false);
}

bool HPETComparator::handle_irq()
{
    auto result = HardwareTimer::handle_irq();

    if (!is_periodic())
        set_new_countdown();

    return result;
}

void HPETComparator::set_new_countdown()
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(m_frequency <= HPET::the().frequency());
    HPET::the().update_non_periodic_comparator_value(*this);
}

ErrorOr<void> HPETComparator::try_to_set_frequency(size_t frequency)
{
    InterruptDisabler disabler;
    if (!is_capable_of_frequency(frequency)) {
        dbgln("HPETComparator: not capable of frequency: {}", frequency);
        return Error::from_errno(ENOTSUP);
    }

    VERIFY(frequency <= HPET::the().frequency());
    m_frequency = frequency;
    m_enabled = true;

    dbgln_if(HPET_COMPARATOR_DEBUG, "HPET Comparator: Max frequency {} Hz, want to set {} Hz, periodic: {}", HPET::the().frequency(), frequency, is_periodic());

    if (is_periodic())
        HPET::the().update_periodic_comparator_value();
    else
        HPET::the().update_non_periodic_comparator_value(*this);

    HPET::the().toggle_comparator(*this, true);
    enable_irq(); // Enable if we haven't already
    return {};
}
bool HPETComparator::is_capable_of_frequency(size_t frequency) const
{
    return frequency <= HPET::the().frequency();
}
size_t HPETComparator::calculate_nearest_possible_frequency(size_t frequency) const
{
    // NOTE: HPET::update_(non_)periodic_comparator_value will calculate the best counter based on the desired frequency.
    if (frequency > HPET::the().frequency())
        return HPET::the().frequency();
    return frequency;
}

u64 HPETComparator::current_raw() const
{
    return HPET::the().read_main_counter();
}

u64 HPETComparator::raw_to_ns(u64 raw_delta) const
{
    return HPET::the().raw_counter_ticks_to_ns(raw_delta);
}

size_t HPETComparator::ticks_per_second() const
{
    return m_frequency;
}

}
