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
#include <AK/Types.h>
#include <Kernel/Time/HPET.h>
#include <Kernel/Time/HardwareTimer.h>

namespace Kernel {
class HPETComparator final : public HardwareTimer {
    friend class HPET;

public:
    static NonnullRefPtr<HPETComparator> create(u8 number, u8 irq, bool periodic_capable);

    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::HighPrecisionEventTimer; }
    virtual const char* model() const override { return "HPET"; }

    u8 comparator_number() const { return m_comparator_number; }

    virtual size_t ticks_per_second() const override;

    virtual bool is_periodic() const override { return m_periodic; }
    virtual bool is_periodic_capable() const override { return m_periodic_capable; }
    virtual void set_periodic() override;
    virtual void set_non_periodic() override;

    virtual void reset_to_default_ticks_per_second() override;
    virtual bool try_to_set_frequency(size_t frequency) override;
    virtual bool is_capable_of_frequency(size_t frequency) const override;
    virtual size_t calculate_nearest_possible_frequency(size_t frequency) const override;

private:
    void set_new_countdown();
    virtual void handle_irq(const RegisterState&) override;
    HPETComparator(u8 number, u8 irq, bool periodic_capable);
    bool m_periodic : 1;
    bool m_periodic_capable : 1;
    bool m_edge_triggered : 1;
    u8 m_comparator_number { 0 };
};
}
