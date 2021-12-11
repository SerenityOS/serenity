/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/IRQController.h>

namespace Kernel {

class IRQHandler : public GenericInterruptHandler {
public:
    virtual ~IRQHandler();

    virtual bool handle_interrupt(const RegisterState& regs) override { return handle_irq(regs); }
    virtual bool handle_irq(const RegisterState&) = 0;

    void enable_irq();
    void disable_irq();

    virtual bool eoi() override;

    virtual HandlerType type() const override { return HandlerType::IRQHandler; }
    virtual StringView purpose() const override { return "IRQ Handler"sv; }
    virtual StringView controller() const override { return m_responsible_irq_controller->model(); }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }
    virtual bool is_sharing_with_others() const override { return m_shared_with_others; }

protected:
    void change_irq_number(u8 irq);
    explicit IRQHandler(u8 irq);

private:
    bool m_shared_with_others { false };
    bool m_enabled { false };
    RefPtr<IRQController> m_responsible_irq_controller;
};

}
