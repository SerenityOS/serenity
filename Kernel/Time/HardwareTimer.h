/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Function.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

enum class HardwareTimerType {
    // x86
    i8253,                   // PIT
    RTC,                     // Real Time Clock
    HighPrecisionEventTimer, // also known as IA-PC HPET
    LocalAPICTimer,          // Local APIC

    // AArch64
    RPiTimer,
    ARMv8Timer,

    // RISC-V
    RISCVTimer,
};

template<typename InterruptHandlerType>
class HardwareTimer;

class HardwareTimerBase : public AtomicRefCounted<HardwareTimerBase> {
public:
    virtual ~HardwareTimerBase() = default;

    // We need to create a virtual will_be_destroyed here because we derive
    // from RefCounted<HardwareTimerBase> here, which means that RefCounted<>
    // will only call will_be_destroyed if we define it here. The derived
    // classes then should forward this to e.g. GenericInterruptHandler.
    virtual void will_be_destroyed() = 0;

    virtual StringView model() const = 0;
    virtual HardwareTimerType timer_type() const = 0;
    virtual Function<void()> set_callback(Function<void()>) = 0;

    virtual bool is_periodic() const = 0;
    virtual bool is_periodic_capable() const = 0;
    virtual void set_periodic() = 0;
    virtual void set_non_periodic() = 0;
    virtual void disable() = 0;
    virtual bool can_query_raw() const { return false; }
    virtual u64 current_raw() const { return 0; }
    virtual u64 raw_to_ns(u64) const { return 0; }

    virtual size_t ticks_per_second() const = 0;

    virtual void reset_to_default_ticks_per_second() = 0;
    virtual bool try_to_set_frequency(size_t frequency) = 0;
    virtual bool is_capable_of_frequency(size_t frequency) const = 0;
    virtual size_t calculate_nearest_possible_frequency(size_t frequency) const = 0;
};

template<>
class HardwareTimer<IRQHandler>
    : public HardwareTimerBase
    , public IRQHandler {
public:
    virtual void will_be_destroyed() override
    {
        IRQHandler::will_be_destroyed();
    }

    virtual StringView purpose() const override
    {
        if (TimeManagement::the().is_system_timer(*this))
            return "System Timer"sv;
        return model();
    }

    virtual Function<void()> set_callback(Function<void()> callback) override
    {
        disable_irq();
        auto previous_callback = move(m_callback);
        m_callback = move(callback);
        enable_irq();
        return previous_callback;
    }

    virtual size_t ticks_per_second() const override { return m_frequency; }

protected:
    HardwareTimer(u8 irq_number, Function<void()> callback = nullptr)
        : IRQHandler(irq_number)
        , m_callback(move(callback))
    {
    }

    virtual bool handle_irq() override
    {
        // Note: if we have an IRQ on this line, it's going to be the timer always
        if (m_callback) {
            m_callback();
            return true;
        }
        return false;
    }

    u64 m_frequency { OPTIMAL_TICKS_PER_SECOND_RATE };

private:
    Function<void()> m_callback;
};

template<>
class HardwareTimer<GenericInterruptHandler>
    : public HardwareTimerBase
    , public GenericInterruptHandler {
public:
    virtual void will_be_destroyed() override
    {
        GenericInterruptHandler::will_be_destroyed();
    }

    virtual StringView purpose() const override
    {
        return model();
    }

    virtual Function<void()> set_callback(Function<void()> callback) override
    {
        auto previous_callback = move(m_callback);
        m_callback = move(callback);
        return previous_callback;
    }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }
    virtual HandlerType type() const override { return HandlerType::IRQHandler; }
    virtual StringView controller() const override { return {}; }
    virtual bool eoi() override;
    virtual size_t ticks_per_second() const override { return m_frequency; }

protected:
    HardwareTimer(u8 irq_number, Function<void()> callback = nullptr)
        : GenericInterruptHandler(irq_number)
        , m_callback(move(callback))
    {
    }

    virtual bool handle_interrupt() override
    {
        // Note: if we have an IRQ on this line, it's going to be the timer always
        if (m_callback) {
            m_callback();
            return true;
        }
        return false;
    }

    u64 m_frequency { OPTIMAL_TICKS_PER_SECOND_RATE };

private:
    Function<void()> m_callback;
};

}
