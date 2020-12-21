/*
 * Copyright (c) 2020, the SerenityOS developers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Types.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Time/HardwareTimer.h>

namespace Kernel {

class APICTimer final : public HardwareTimer<GenericInterruptHandler> {
public:
    static APICTimer* initialize(u8, HardwareTimerBase&);
    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::LocalAPICTimer; }
    virtual const char* model() const override { return "LocalAPIC"; }
    virtual size_t ticks_per_second() const override;

    virtual bool is_periodic() const override { return m_timer_mode == APIC::TimerMode::Periodic; }
    virtual bool is_periodic_capable() const override { return true; }
    virtual void set_periodic() override;
    virtual void set_non_periodic() override;
    virtual void disable() override { }

    virtual void reset_to_default_ticks_per_second() override;
    virtual bool try_to_set_frequency(size_t frequency) override;
    virtual bool is_capable_of_frequency(size_t frequency) const override;
    virtual size_t calculate_nearest_possible_frequency(size_t frequency) const override;

    void enable_local_timer();
    void disable_local_timer();

private:
    explicit APICTimer(u8, Function<void(const RegisterState&)>);

    bool calibrate(HardwareTimerBase&);

    u32 m_timer_period { 0 };
    APIC::TimerMode m_timer_mode { APIC::TimerMode::Periodic };
};

}
