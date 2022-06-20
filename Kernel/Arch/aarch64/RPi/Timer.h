/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Interrupts/IRQHandler.h>

namespace Kernel::RPi {

struct TimerRegisters;

class Timer : public IRQHandler {
public:
    Timer();
    static Timer& the();

    u64 microseconds_since_boot();

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
    virtual bool handle_irq(RegisterState const&) override;

    TimerRegisters volatile* m_registers;
    u32 m_interrupt_interval { 0 };
    u32 m_current_timer_value { 0 };
};

}
