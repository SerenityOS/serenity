/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/IRQController.h>

namespace Kernel {
class IRQHandler;
class SharedIRQHandler final : public GenericInterruptHandler {
public:
    static void initialize(u8 interrupt_number);
    virtual ~SharedIRQHandler();
    virtual bool handle_interrupt(RegisterState const& regs) override;

    void register_handler(GenericInterruptHandler&);
    void unregister_handler(GenericInterruptHandler&);

    virtual bool eoi() override;

    void enumerate_handlers(Function<void(GenericInterruptHandler&)>&);

    virtual size_t sharing_devices_count() const override { return m_handlers.size_slow(); }
    virtual bool is_shared_handler() const override { return true; }
    virtual bool is_sharing_with_others() const override { return false; }

    virtual HandlerType type() const override { return HandlerType::SharedIRQHandler; }
    virtual StringView purpose() const override { return "Shared IRQ Handler"sv; }
    virtual StringView controller() const override { return m_responsible_irq_controller->model(); }

private:
    void enable_interrupt_vector();
    void disable_interrupt_vector();
    explicit SharedIRQHandler(u8 interrupt_number);
    bool m_enabled { true };
    GenericInterruptHandler::List m_handlers;
    RefPtr<IRQController> m_responsible_irq_controller;
};
}
