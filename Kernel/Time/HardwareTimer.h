/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

enum class HardwareTimerType {
    i8253 = 0x1,                  /* PIT */
    RTC = 0x2,                    /* Real Time Clock */
    HighPrecisionEventTimer = 0x3 /* also known as IA-PC HPET */
};

class HardwareTimer
    : public RefCounted<HardwareTimer>
    , public IRQHandler {
public:
    virtual HardwareTimerType timer_type() const = 0;
    virtual const char* model() const = 0;
    virtual const char* purpose() const override;

    void set_callback(Function<void(const RegisterState&)>);

    virtual size_t ticks_per_second() const = 0;

    virtual bool is_periodic() const = 0;
    virtual bool is_periodic_capable() const = 0;
    virtual void set_periodic() = 0;
    virtual void set_non_periodic() = 0;

    virtual void reset_to_default_ticks_per_second() = 0;
    virtual bool try_to_set_frequency(size_t frequency) = 0;
    virtual bool is_capable_of_frequency(size_t frequency) const = 0;
    virtual size_t calculate_nearest_possible_frequency(size_t frequency) const = 0;

protected:
    HardwareTimer(u8 irq_number, Function<void(const RegisterState&)> = nullptr);
    //^IRQHandler
    virtual void handle_irq(const RegisterState&) override;
    u64 m_frequency { OPTIMAL_TICKS_PER_SECOND_RATE };

private:
    Function<void(const RegisterState&)> m_callback;
};

}
