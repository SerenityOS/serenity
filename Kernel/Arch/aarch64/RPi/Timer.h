/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Time/HardwareTimer.h>

namespace Kernel::RPi {

struct TimerRegisters;

class Timer final : public HardwareTimer<IRQHandler> {
public:
    virtual ~Timer();

    static NonnullLockRefPtr<Timer> initialize();

    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::RPiTimer; }
    virtual StringView model() const override { return "RPi Timer"sv; }
    virtual size_t ticks_per_second() const override { return m_frequency; }

    virtual bool is_periodic() const override { TODO_AARCH64(); }
    virtual bool is_periodic_capable() const override { TODO_AARCH64(); }
    virtual void set_periodic() override { TODO_AARCH64(); }
    virtual void set_non_periodic() override { TODO_AARCH64(); }
    virtual void disable() override { TODO_AARCH64(); }

    virtual void reset_to_default_ticks_per_second() override { TODO_AARCH64(); }
    virtual bool try_to_set_frequency(size_t) override { TODO_AARCH64(); }
    virtual bool is_capable_of_frequency(size_t) const override { TODO_AARCH64(); }
    virtual size_t calculate_nearest_possible_frequency(size_t) const override { TODO_AARCH64(); }

    // FIXME: Share code with HPET::update_time
    u64 update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only);

    u64 microseconds_since_boot();

    void set_interrupt_interval_usec(u32);
    void enable_interrupt_mode();

private:
    Timer();

    enum class TimerID : u32 {
        Timer0 = 0,
        Timer1 = 1,
        Timer2 = 2,
        Timer3 = 3,
    };
    void set_compare(TimerID, u32 compare);
    void clear_interrupt(TimerID);

    //^ IRQHandler
    virtual bool handle_irq(RegisterState const&) override;

    TimerRegisters volatile* m_registers;
    u32 m_interrupt_interval { 0 };

    u64 m_main_counter_last_read { 0 };
    u64 m_main_counter_drift { 0 };
};

}
