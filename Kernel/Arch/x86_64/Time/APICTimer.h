/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Time/HardwareTimer.h>

namespace Kernel {

class APICTimer final : public HardwareTimer<GenericInterruptHandler> {
public:
    static APICTimer* initialize(u8, HardwareTimerBase&);
    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::LocalAPICTimer; }
    virtual StringView model() const override { return "LocalAPIC"sv; }

    virtual bool is_periodic() const override { return m_timer_mode == APIC::TimerMode::Periodic; }
    virtual bool is_periodic_capable() const override { return true; }
    virtual void set_periodic() override;
    virtual void set_non_periodic() override;
    virtual void disable() override { }

    virtual void reset_to_default_ticks_per_second() override;
    virtual bool try_to_set_frequency(size_t frequency) override;
    virtual bool is_capable_of_frequency(size_t frequency) const override;
    virtual size_t calculate_nearest_possible_frequency(size_t frequency) const override;

    void will_be_destroyed() override { HardwareTimer<GenericInterruptHandler>::will_be_destroyed(); }
    void enable_local_timer();
    void disable_local_timer();

private:
    explicit APICTimer(u8, Function<void()>);

    bool calibrate(HardwareTimerBase&);

    u32 m_timer_period { 0 };
    APIC::TimerMode m_timer_mode { APIC::TimerMode::Periodic };
};

}
