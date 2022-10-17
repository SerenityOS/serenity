/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/NeverDestroyed.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#include <Kernel/Arch/aarch64/RPi/Timer.h>

namespace Kernel::RPi {

// "12.1 System Timer Registers" / "10.2 System Timer Registers"
struct TimerRegisters {
    u32 control_and_status;
    u32 counter_low;
    u32 counter_high;
    u32 compare[4];
};

// Bits of the `control_and_status` register.
// See "CS register" in Broadcom doc for details.
enum FlagBits {
    SystemTimerMatch0 = 1 << 0,
    SystemTimerMatch1 = 1 << 1,
    SystemTimerMatch2 = 1 << 2,
    SystemTimerMatch3 = 1 << 3,
};

Timer::Timer()
    : HardwareTimer(1)
    , m_registers(MMIO::the().peripheral<TimerRegisters>(0x3000))
{
    // FIXME: Actually query the frequency of the timer. By default it is 100MHz.
    m_frequency = 1e6;

    set_interrupt_interval_usec(m_frequency / OPTIMAL_TICKS_PER_SECOND_RATE);
    enable_interrupt_mode();
}

Timer::~Timer() = default;

NonnullLockRefPtr<Timer> Timer::initialize()
{
    return adopt_lock_ref(*new Timer);
}

u64 Timer::microseconds_since_boot()
{
    u32 high = m_registers->counter_high;
    u32 low = m_registers->counter_low;
    if (high != m_registers->counter_high) {
        high = m_registers->counter_high;
        low = m_registers->counter_low;
    }
    return (static_cast<u64>(high) << 32) | low;
}

bool Timer::handle_irq(RegisterState const& regs)
{
    auto result = HardwareTimer::handle_irq(regs);

    set_compare(TimerID::Timer1, microseconds_since_boot() + m_interrupt_interval);
    clear_interrupt(TimerID::Timer1);

    return result;
};

u64 Timer::update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only)
{
    // Should only be called by the time keeper interrupt handler!
    u64 current_value = microseconds_since_boot();
    u64 delta_ticks = m_main_counter_drift;
    if (current_value >= m_main_counter_last_read) {
        delta_ticks += current_value - m_main_counter_last_read;
    } else {
        // the counter wrapped around
        delta_ticks += (NumericLimits<u64>::max() - m_main_counter_last_read + 1) + current_value;
    }

    u64 ticks_since_last_second = (u64)ticks_this_second + delta_ticks;
    auto ticks_per_second = frequency();
    seconds_since_boot += ticks_since_last_second / ticks_per_second;
    ticks_this_second = ticks_since_last_second % ticks_per_second;

    if (!query_only) {
        m_main_counter_drift = 0;
        m_main_counter_last_read = current_value;
    }

    // Return the time passed (in ns) since last time update_time was called
    return (delta_ticks * 1000000000ull) / ticks_per_second;
}

void Timer::enable_interrupt_mode()
{
    set_compare(TimerID::Timer1, microseconds_since_boot() + m_interrupt_interval);
    enable_irq();
}

void Timer::set_interrupt_interval_usec(u32 interrupt_interval)
{
    m_interrupt_interval = interrupt_interval;
}

void Timer::clear_interrupt(TimerID id)
{
    m_registers->control_and_status = 1 << to_underlying(id);
}

void Timer::set_compare(TimerID id, u32 compare)
{
    m_registers->compare[to_underlying(id)] = compare;
}

class SetClockRateMboxMessage : Mailbox::Message {
public:
    u32 clock_id;
    u32 rate_hz;
    u32 skip_setting_turbo;

    SetClockRateMboxMessage()
        : Mailbox::Message(0x0003'8002, 12)
    {
        clock_id = 0;
        rate_hz = 0;
        skip_setting_turbo = 0;
    }
};

u32 Timer::set_clock_rate(ClockID clock_id, u32 rate_hz, bool skip_setting_turbo)
{
    struct __attribute__((aligned(16))) {
        Mailbox::MessageHeader header;
        SetClockRateMboxMessage set_clock_rate;
        Mailbox::MessageTail tail;
    } message_queue;

    message_queue.set_clock_rate.clock_id = static_cast<u32>(clock_id);
    message_queue.set_clock_rate.rate_hz = rate_hz;
    message_queue.set_clock_rate.skip_setting_turbo = skip_setting_turbo ? 1 : 0;

    if (!Mailbox::the().send_queue(&message_queue, sizeof(message_queue))) {
        dbgln("Timer::set_clock_rate() failed!");
        return 0;
    }

    return message_queue.set_clock_rate.rate_hz;
}

}
