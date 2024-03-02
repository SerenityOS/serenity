/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Audio/IntelHDA/Controller.h>
#include <Kernel/Devices/Audio/IntelHDA/InterruptHandler.h>

namespace Kernel::Audio::IntelHDA {

ErrorOr<NonnullRefPtr<InterruptHandler>> InterruptHandler::create(Controller& controller, PCI::Device& device, u8 interrupt_line)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) InterruptHandler(controller, device, interrupt_line));
}

InterruptHandler::InterruptHandler(Controller& controller, PCI::Device& device, u8 interrupt_line)
    : PCI::IRQHandler(device, interrupt_line)
    , m_controller(controller)
{
    enable_irq();
}

bool InterruptHandler::handle_irq(RegisterState const&)
{
    auto result_or_error = m_controller.handle_interrupt({});
    if (result_or_error.is_error()) {
        dmesgln("IntelHDA: Error during interrupt handling: {}", result_or_error.release_error());
        return false;
    }
    return result_or_error.release_value();
}

}
