/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Function.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Time/HardwareTimerType.h>
#include <Kernel/Time/TimeDeviceBase.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

enum class TimerMode {
    OneShot,
    Periodic,
};

template<typename InterruptHandlerType>
class HardwareTimer;

class HardwareTimerBase : public virtual TimeDeviceBase {
public:
    virtual ~HardwareTimerBase() = default;

    virtual Function<void()> set_callback(Function<void()>) = 0;

    virtual TimerMode mode() const = 0;
    virtual void set_mode(TimerMode) = 0;
    virtual bool supports_mode(TimerMode) const = 0;

    virtual void disable() = 0;

    virtual ErrorOr<void> try_to_set_frequency(size_t new_frequency)
    {
        if (new_frequency == frequency())
            return {};

        return Error::from_errno(ENOTSUP);
    }

    virtual bool is_capable_of_frequency(size_t new_frequency) const { return new_frequency == frequency(); }
    virtual size_t calculate_nearest_possible_frequency(size_t) const { return frequency(); }
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

protected:
    HardwareTimer(InterruptNumber irq_number, Function<void()> callback = nullptr)
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

protected:
    HardwareTimer(InterruptNumber irq_number, Function<void()> callback = nullptr)
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

private:
    Function<void()> m_callback;
};

}
