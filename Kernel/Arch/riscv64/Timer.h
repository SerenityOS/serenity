/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Time/HardwareTimer.h>

namespace Kernel::RISCV64 {

class Timer final : public HardwareTimerBase {
public:
    static NonnullLockRefPtr<Timer> initialize();

    static bool is_initialized();
    static Timer& the();

    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::RISCVTimer; }
    virtual StringView model() const override { return "RISC-V Timer"sv; }
    virtual size_t ticks_per_second() const override { return m_frequency; }

    virtual TimerMode mode() const override { return TimerMode::OneShot; }
    virtual void set_mode(TimerMode) override { }
    virtual bool supports_mode(TimerMode mode) const override { return mode == TimerMode::OneShot; }

    virtual void disable() override;

    virtual void will_be_destroyed() override { }
    virtual Function<void()> set_callback(Function<void()> callback) override
    {
        auto previous_callback = move(m_callback);
        m_callback = move(callback);
        return previous_callback;
    }

    // FIXME: Share code with HPET::update_time
    u64 update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only);
    void handle_interrupt();

private:
    Timer();

    static u64 current_ticks();
    static void set_compare(u64 compare);

    Function<void()> m_callback;
    u64 m_frequency { 0 };
    u32 m_interrupt_interval { 0 };

    u64 m_main_counter_last_read { 0 };
    u64 m_main_counter_drift { 0 };
};

}
