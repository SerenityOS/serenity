/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Time/HardwareTimer.h>
#include <Kernel/Time/TimeSource.h>

// https://developer.arm.com/-/media/Arm%20Developer%20Community/PDF/Learn%20the%20Architecture/Generic%20Timer.pdf

namespace Kernel {

class ARMv8Timer final : public HardwareTimer<IRQHandler>
    , public TimeSource {
public:
    static ErrorOr<NonnullLockRefPtr<ARMv8Timer>> initialize(InterruptNumber interrupt_number, u32 frequency);

    static bool is_initialized();
    static ARMv8Timer& the();

    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::ARMv8Timer; }
    virtual StringView model() const override { return "ARMv8 Timer"sv; }
    virtual size_t frequency() const override { return m_frequency; }

    virtual TimerMode mode() const override { return TimerMode::OneShot; }
    virtual void set_mode(TimerMode) override { }
    virtual bool supports_mode(TimerMode mode) const override { return mode == TimerMode::OneShot; }

    virtual u64 read_counter_raw() const override;

    virtual void disable() override;

private:
    ARMv8Timer(InterruptNumber interrupt_number, u32 frequency);

    static void start_timer(u32 delta);

    // ^IRQHandler
    bool handle_irq() override;

    Function<void()> m_callback;
    u64 m_frequency { 0 };
    u32 m_interrupt_interval { 0 };
};

}
