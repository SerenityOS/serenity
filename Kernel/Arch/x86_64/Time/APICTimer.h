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
    static APICTimer* initialize(InterruptNumber, TimeSource&);
    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::LocalAPICTimer; }
    virtual StringView model() const override { return "LocalAPIC"sv; }

    // FIXME: Implement support for changing timer modes
    virtual TimerMode mode() const override { return m_timer_mode; }
    virtual void set_mode(TimerMode) override { }
    virtual bool supports_mode(TimerMode mode) const override { return mode == TimerMode::Periodic; }

    virtual void disable() override { }

    virtual size_t frequency() const override;

    void will_be_destroyed() override { HardwareTimer<GenericInterruptHandler>::will_be_destroyed(); }
    void enable_local_timer();
    void disable_local_timer();

private:
    explicit APICTimer(InterruptNumber, Function<void()>);

    u32 m_timer_period { 0 };
    TimerMode m_timer_mode { TimerMode::Periodic };
    size_t m_frequency { 0 };
};

}
