/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Time/HardwareTimer.h>

// https://developer.arm.com/-/media/Arm%20Developer%20Community/PDF/Learn%20the%20Architecture/Generic%20Timer.pdf

namespace Kernel {

class ARMv8Timer final : public HardwareTimer<IRQHandler> {
public:
    static ErrorOr<NonnullLockRefPtr<ARMv8Timer>> initialize(u8 interrupt_number);

    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::ARMv8Timer; }
    virtual StringView model() const override { return "ARMv8 Timer"sv; }
    virtual size_t ticks_per_second() const override { return m_frequency; }

    virtual bool is_periodic() const override { return false; }
    virtual bool is_periodic_capable() const override { return false; }
    virtual void set_periodic() override { }
    virtual void set_non_periodic() override { }
    virtual void disable() override;

    virtual void reset_to_default_ticks_per_second() override { }
    virtual bool try_to_set_frequency(size_t frequency) override { return frequency == m_frequency; }
    virtual bool is_capable_of_frequency(size_t frequency) const override { return frequency == m_frequency; }
    virtual size_t calculate_nearest_possible_frequency(size_t) const override { return m_frequency; }

    // FIXME: Share code with HPET::update_time
    u64 update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only);

private:
    ARMv8Timer(u8 interrupt_number);

    static u64 current_ticks();
    static void start_timer(u32 delta);

    // ^IRQHandler
    bool handle_irq() override;

    Function<void()> m_callback;
    u64 m_frequency { 0 };
    u32 m_interrupt_interval { 0 };

    u64 m_main_counter_last_read { 0 };
    u64 m_main_counter_drift { 0 };
};

}
