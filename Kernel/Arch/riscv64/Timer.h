/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
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
    static Timer& the();

    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::RISCVTimer; }
    virtual StringView model() const override { return "RISC-V Timer"sv; }
    virtual size_t ticks_per_second() const override { return m_frequency; }

    virtual bool is_periodic() const override { TODO_RISCV64(); }
    virtual bool is_periodic_capable() const override { TODO_RISCV64(); }
    virtual void set_periodic() override { TODO_RISCV64(); }
    virtual void set_non_periodic() override { TODO_RISCV64(); }
    virtual void disable() override { TODO_RISCV64(); }

    virtual void reset_to_default_ticks_per_second() override { TODO_RISCV64(); }
    virtual bool try_to_set_frequency(size_t) override { TODO_RISCV64(); }
    virtual bool is_capable_of_frequency(size_t) const override { TODO_RISCV64(); }
    virtual size_t calculate_nearest_possible_frequency(size_t) const override { TODO_RISCV64(); }

    virtual void will_be_destroyed() override { }
    virtual Function<void(RegisterState const&)> set_callback(Function<void(RegisterState const&)> callback) override
    {
        auto previous_callback = move(m_callback);
        m_callback = move(callback);
        return previous_callback;
    }

    // FIXME: Share code with HPET::update_time
    u64 update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only);
    void handle_interrupt(RegisterState const&);

private:
    Timer();

    static u64 current_ticks();
    static void set_compare(u64 compare);

    Function<void(RegisterState const&)> m_callback;
    u64 m_frequency { 0 };
    u32 m_interrupt_interval { 0 };

    u64 m_main_counter_last_read { 0 };
    u64 m_main_counter_drift { 0 };
};

}
