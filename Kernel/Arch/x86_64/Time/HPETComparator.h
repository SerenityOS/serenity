/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86_64/Time/HPET.h>
#include <Kernel/Time/HardwareTimer.h>

namespace Kernel {
class HPETComparator final : public HardwareTimer<IRQHandler> {
    friend class HPET;

public:
    static NonnullLockRefPtr<HPETComparator> create(u8 number, u8 irq, bool periodic_capable, bool is_64bit_capable);

    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::HighPrecisionEventTimer; }
    virtual StringView model() const override { return "HPET"sv; }

    u8 comparator_number() const { return m_comparator_number; }
    bool is_enabled() const { return m_enabled; }
    bool is_64bit_capable() const { return m_is_64bit_capable; }

    virtual bool is_periodic() const override { return m_periodic; }
    virtual bool is_periodic_capable() const override { return m_periodic_capable; }
    virtual void set_periodic() override;
    virtual void set_non_periodic() override;
    virtual void disable() override;
    virtual bool can_query_raw() const override { return true; }
    virtual u64 current_raw() const override;
    virtual u64 raw_to_ns(u64) const override;

    virtual void reset_to_default_ticks_per_second() override;
    virtual bool try_to_set_frequency(size_t frequency) override;
    virtual bool is_capable_of_frequency(size_t frequency) const override;
    virtual size_t calculate_nearest_possible_frequency(size_t frequency) const override;

private:
    void set_new_countdown();
    virtual bool handle_irq() override;
    HPETComparator(u8 number, u8 irq, bool periodic_capable, bool is_64bit_capable);
    bool m_periodic : 1;
    bool m_periodic_capable : 1;
    bool m_enabled : 1;
    bool m_is_64bit_capable : 1;
    u8 m_comparator_number { 0 };
};
}
