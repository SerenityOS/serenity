/*
 * Copyright (c) 2023, Pankaj R <dev@pankajraghav.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Arch/IRQController.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Library/LockRefPtr.h>

namespace Kernel::PCI {

class IRQHandler : public GenericInterruptHandler {
public:
    virtual ~IRQHandler() = default;

    virtual bool handle_interrupt() override;
    virtual bool handle_irq() = 0;

    void enable_irq();
    void disable_irq();

    virtual bool eoi() override;

    virtual HandlerType type() const override { return HandlerType::IRQHandler; }
    virtual StringView purpose() const override { return "IRQ Handler"sv; }
    virtual StringView controller() const override { return m_responsible_irq_controller.is_null() ? "PCI-MSI"sv : m_responsible_irq_controller->model(); }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }
    void set_shared_with_others(bool status) { m_shared_with_others = status; }

protected:
    IRQHandler(PCI::Device& device, u8 irq);

private:
    bool m_shared_with_others { false };
    bool m_enabled { false };
    LockRefPtr<IRQController> m_responsible_irq_controller { nullptr };
    PCI::Device& device;
};

}
