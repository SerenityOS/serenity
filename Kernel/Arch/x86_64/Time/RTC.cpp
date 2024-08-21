/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/CMOS.h>
#include <Kernel/Arch/x86_64/IO.h>
#include <Kernel/Arch/x86_64/NonMaskableInterruptDisabler.h>
#include <Kernel/Arch/x86_64/Time/RTC.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {
#define IRQ_TIMER 8
#define MAX_FREQUENCY 8000

NonnullLockRefPtr<RealTimeClock> RealTimeClock::create(Function<void()> callback)
{
    return adopt_lock_ref(*new RealTimeClock(move(callback)));
}
RealTimeClock::RealTimeClock(Function<void()> callback)
    : HardwareTimer(IRQ_TIMER, move(callback))
{
    InterruptDisabler disabler;
    NonMaskableInterruptDisabler nmi_disabler;
    enable_irq();
    CMOS::write(0x8B, CMOS::read(0xB) | 0x40);
    reset_to_default_ticks_per_second();
}
bool RealTimeClock::handle_irq()
{
    auto result = HardwareTimer::handle_irq();
    CMOS::read(0x8C);
    return result;
}

void RealTimeClock::reset_to_default_ticks_per_second()
{
    InterruptDisabler disabler;
    bool success = try_to_set_frequency(1024);
    VERIFY(success);
}

// FIXME: This is a quick & dirty log base 2 with a parameter. Please provide something better in the future.
static int quick_log2(size_t number)
{
    int count = 0;
    while (number >>= 1)
        count++;
    return count;
}

bool RealTimeClock::try_to_set_frequency(size_t frequency)
{
    InterruptDisabler disabler;
    if (!is_capable_of_frequency(frequency))
        return false;
    disable_irq();
    u8 previous_rate = CMOS::read(0x8A);
    u8 rate = quick_log2(32768 / frequency) + 1;
    dbgln("RTC: Set rate to {}", rate);
    CMOS::write(0x8A, (previous_rate & 0xF0) | rate);
    m_frequency = frequency;
    dbgln("RTC: Set frequency to {} Hz", frequency);
    enable_irq();
    return true;
}
bool RealTimeClock::is_capable_of_frequency(size_t frequency) const
{
    VERIFY(frequency != 0);
    if (frequency > MAX_FREQUENCY)
        return false;
    if (32768 % frequency)
        return false;

    u16 divider = 32768 / frequency;
    return (divider <= 16384 && divider >= 4); // Frequency can be in range of 2 Hz to 8 KHz
}
size_t RealTimeClock::calculate_nearest_possible_frequency(size_t frequency) const
{
    VERIFY(frequency != 0);
    return frequency;
}

}
