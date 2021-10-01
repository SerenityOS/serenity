/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>
#include <Kernel/Thread.h>
#include <Kernel/Time/PIT.h>
#include <Kernel/Time/TimeManagement.h>

#define IRQ_TIMER 0
namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<PIT> PIT::initialize(Function<void(const RegisterState&)> callback)
{
    return adopt_ref(*new PIT(move(callback)));
}

[[maybe_unused]] inline static void reset_countdown(u16 timer_reload)
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

    dmesgln("PIT: {} Hz, square wave ({:#08x})", OPTIMAL_TICKS_PER_SECOND_RATE, BASE_FREQUENCY / OPTIMAL_TICKS_PER_SECOND_RATE);
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
    VERIFY_NOT_REACHED();
}
void PIT::set_non_periodic()
{
    // FIXME: Implement it...
    VERIFY_NOT_REACHED();
}

void PIT::reset_to_default_ticks_per_second()
{
    InterruptDisabler disabler;
    bool success = try_to_set_frequency(OPTIMAL_TICKS_PER_SECOND_RATE);
    VERIFY(success);
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
    VERIFY(frequency != 0);
    return frequency <= BASE_FREQUENCY;
}
size_t PIT::calculate_nearest_possible_frequency(size_t frequency) const
{
    VERIFY(frequency != 0);
    return frequency;
}

}
