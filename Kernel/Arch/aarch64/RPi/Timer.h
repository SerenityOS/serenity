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
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Time/HardwareTimer.h>
#include <Kernel/Time/TimeSource.h>

namespace Kernel::RPi {

struct TimerRegisters;

class Timer final : public HardwareTimer<IRQHandler>
    , public TimeSource {
public:
    Timer(Memory::TypedMapping<TimerRegisters volatile>, InterruptNumber interrupt_number);
    virtual ~Timer();

    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::RPiTimer; }
    virtual StringView model() const override { return "RPi Timer"sv; }

    virtual TimerMode mode() const override { return TimerMode::OneShot; }
    virtual void set_mode(TimerMode) override { }
    virtual bool supports_mode(TimerMode mode) const override { return mode == TimerMode::OneShot; }

    virtual void disable() override;

    virtual u64 read_counter_raw() const override;
    virtual size_t frequency() const override;

    void set_interrupt_interval_usec(u32);
    void enable_interrupt_mode();

    enum class ClockID {
        Reserved = 0,
        EMMC = 1,
        UART = 2,
        ARM = 3,
        CORE = 4,
        V3D = 5,
        H264 = 6,
        ISP = 7,
        SDRAM = 8,
        PIXEL = 9,
        PWM = 10,
        HEVC = 11,
        EMMC2 = 12,
        M2MC = 13,
        PIXEL_BVB = 14,
    };
    static u32 set_clock_rate(ClockID, u32 rate_hz, bool skip_setting_turbo = true);
    static u32 get_clock_rate(ClockID);

private:
    enum class TimerID : u32 {
        Timer0 = 0,
        Timer1 = 1,
        Timer2 = 2,
        Timer3 = 3,
    };
    void set_compare(TimerID, u32 compare);
    void clear_interrupt(TimerID);

    //^ IRQHandler
    virtual bool handle_irq() override;

    Memory::TypedMapping<TimerRegisters volatile> m_registers;
    u32 m_interrupt_interval { 0 };

    size_t m_frequency { 0 };
};

}
