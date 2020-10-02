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
#include <Kernel/CMOS.h>
#include <Kernel/IO.h>
#include <Kernel/Time/RTC.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {
#define IRQ_TIMER 8
#define MAX_FREQUENCY 8000

NonnullRefPtr<RealTimeClock> RealTimeClock::create(Function<void(const RegisterState&)> callback)
{
    return adopt(*new RealTimeClock(move(callback)));
}
RealTimeClock::RealTimeClock(Function<void(const RegisterState&)> callback)
    : HardwareTimer(IRQ_TIMER, move(callback))
{
    InterruptDisabler disabler;
    NonMaskableInterruptDisabler nmi_disabler;
    enable_irq();
    CMOS::write(0x8B, CMOS::read(0xB) | 0x40);
    reset_to_default_ticks_per_second();
}
void RealTimeClock::handle_irq(const RegisterState& regs)
{
    HardwareTimer::handle_irq(regs);
    CMOS::read(0x8C);
}

size_t RealTimeClock::ticks_per_second() const
{
    return m_frequency;
}

void RealTimeClock::reset_to_default_ticks_per_second()
{
    InterruptDisabler disabler;
    bool success = try_to_set_frequency(1024);
    ASSERT(success);
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
    dbg() << "RTC: Set rate to " << rate;
    CMOS::write(0x8A, (previous_rate & 0xF0) | rate);
    m_frequency = frequency;
    dbg() << "RTC: Set frequency to " << frequency << " Hz";
    enable_irq();
    return true;
}
bool RealTimeClock::is_capable_of_frequency(size_t frequency) const
{
    ASSERT(frequency != 0);
    if (frequency > MAX_FREQUENCY)
        return false;
    if (32768 % frequency)
        return false;

    u16 divider = 32768 / frequency;
    return (divider <= 16384 && divider >= 4); // Frequency can be in range of 2 Hz to 8 KHz
}
size_t RealTimeClock::calculate_nearest_possible_frequency(size_t frequency) const
{
    ASSERT(frequency != 0);
    return frequency;
}

}
