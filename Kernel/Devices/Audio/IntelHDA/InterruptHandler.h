/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>

namespace Kernel::Audio::IntelHDA {

class Controller;

class InterruptHandler
    : public PCI::IRQHandler
    , public RefCounted<InterruptHandler> {
public:
    static ErrorOr<NonnullRefPtr<InterruptHandler>> create(Controller& controller)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) InterruptHandler(controller));
    }

    // ^PCI::IRQHandler
    virtual StringView purpose() const override { return "IntelHDA IRQ Handler"sv; }

private:
    InterruptHandler(Controller& controller);

    // ^PCI::IRQHandler
    virtual bool handle_irq() override;

    Controller& m_controller;
};

}
