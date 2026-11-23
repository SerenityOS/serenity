/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Time/HardwareTimer.h>
#include <Kernel/Time/TimeSource.h>

namespace Kernel {

class TSC : public TimeSource {
public:
    static ErrorOr<NonnullLockRefPtr<TSC>> create(HardwareTimerBase& calibration_reference);
    static bool is_supported_and_invariant();

    TSC(HardwareTimerBase& calibration_reference);

    virtual StringView model() const override { return "TSC"sv; }
    virtual HardwareTimerType timer_type() const override { return HardwareTimerType::TSC; }

    virtual u64 read_counter_raw() const override;
    virtual u64 frequency() const override { return m_frequency; }

private:
    u64 m_frequency { 0 };
};

}
