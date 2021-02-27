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
    i8253 = 0x1,                   /* PIT */
    RTC = 0x2,                     /* Real Time Clock */
    HighPrecisionEventTimer = 0x3, /* also known as IA-PC HPET */
    LocalAPICTimer = 0x4           /* Local APIC */
};

template<typename InterruptHandlerType>
class HardwareTimer;

class HardwareTimerBase
    : public RefCounted<HardwareTimerBase> {
public:
    virtual ~HardwareTimerBase() { }

    // We need to create a virtual will_be_destroyed here because we derive
    // from RefCounted<HardwareTimerBase> here, which means that RefCounted<>
    // will only call will_be_destroyed if we define it here. The derived
    // classes then should forward this to e.g. GenericInterruptHandler.
    virtual void will_be_destroyed() = 0;

    virtual const char* model() const = 0;
    virtual HardwareTimerType timer_type() const = 0;
    virtual Function<void(const RegisterState&)> set_callback(Function<void(const RegisterState&)>) = 0;

    virtual bool is_periodic() const = 0;
    virtual bool is_periodic_capable() const = 0;
    virtual void set_periodic() = 0;
    virtual void set_non_periodic() = 0;
    virtual void disable() = 0;
    virtual u32 frequency() const = 0;

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

    virtual const char* purpose() const override
    {
        if (TimeManagement::the().is_system_timer(*this))
            return "System Timer";
        return model();
    }

    virtual Function<void(const RegisterState&)> set_callback(Function<void(const RegisterState&)> callback) override
    {
        disable_irq();
        auto previous_callback = move(m_callback);
        m_callback = move(callback);
        enable_irq();
        return previous_callback;
    }

    virtual u32 frequency() const override { return (u32)m_frequency; }

protected:
    HardwareTimer(u8 irq_number, Function<void(const RegisterState&)> callback = nullptr)
        : IRQHandler(irq_number)
        , m_callback(move(callback))
    {
    }

    virtual void handle_irq(const RegisterState& regs) override
    {
        if (m_callback)
            m_callback(regs);
    }

    u64 m_frequency { OPTIMAL_TICKS_PER_SECOND_RATE };

private:
    Function<void(const RegisterState&)> m_callback;
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

    virtual const char* purpose() const override
    {
        return model();
    }

    virtual Function<void(const RegisterState&)> set_callback(Function<void(const RegisterState&)> callback) override
    {
        auto previous_callback = move(m_callback);
        m_callback = move(callback);
        return previous_callback;
    }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }
    virtual bool is_sharing_with_others() const { return false; }
    virtual HandlerType type() const override { return HandlerType::IRQHandler; }
    virtual const char* controller() const override { return nullptr; }
    virtual bool eoi() override;

    virtual u32 frequency() const override { return (u32)m_frequency; }

protected:
    HardwareTimer(u8 irq_number, Function<void(const RegisterState&)> callback = nullptr)
        : GenericInterruptHandler(irq_number)
        , m_callback(move(callback))
    {
    }

    virtual void handle_interrupt(const RegisterState& regs) override
    {
        if (m_callback)
            m_callback(regs);
    }

    u64 m_frequency { OPTIMAL_TICKS_PER_SECOND_RATE };

private:
    Function<void(const RegisterState&)> m_callback;
};

}
