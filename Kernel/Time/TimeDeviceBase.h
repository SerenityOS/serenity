/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Function.h>
#include <Kernel/Time/HardwareTimerType.h>

namespace Kernel {

class HardwareTimerBase;

class TimeDeviceBase : public AtomicRefCounted<TimeDeviceBase> {
public:
    virtual ~TimeDeviceBase() = default;

    // We need to create a virtual will_be_destroyed here because we derive
    // from RefCounted<TimeDeviceBase> here, which means that RefCounted<>
    // will only call will_be_destroyed if we define it here. The derived
    // classes then should forward this to e.g. GenericInterruptHandler.
    virtual void will_be_destroyed() { }

    virtual StringView model() const = 0;
    virtual HardwareTimerType timer_type() const = 0;

    virtual size_t frequency() const = 0;

protected:
    enum class CounterDirection {
        Up,
        Down,
    };

    u64 measure_frequency(HardwareTimerBase& source, AK::Function<u64()> read_counter, CounterDirection direction) const;
};

}
