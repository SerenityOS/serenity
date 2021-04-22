/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

namespace Kernel {
class UnhandledInterruptHandler final : public GenericInterruptHandler {
public:
    explicit UnhandledInterruptHandler(u8 interrupt_vector);
    virtual ~UnhandledInterruptHandler();

    virtual void handle_interrupt(const RegisterState&) override;

    [[noreturn]] virtual bool eoi() override;

    virtual HandlerType type() const override { return HandlerType::UnhandledInterruptHandler; }
    virtual const char* purpose() const override { return "Unhandled Interrupt Handler"; }
    virtual const char* controller() const override { VERIFY_NOT_REACHED(); }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }
    virtual bool is_sharing_with_others() const override { return false; }

private:
};
}
