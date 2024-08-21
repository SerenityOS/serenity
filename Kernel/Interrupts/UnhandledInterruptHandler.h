/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

namespace Kernel {
class UnhandledInterruptHandler final : public GenericInterruptHandler {
public:
    explicit UnhandledInterruptHandler(u8 interrupt_vector);
    virtual ~UnhandledInterruptHandler();

    virtual bool handle_interrupt() override;

    [[noreturn]] virtual bool eoi() override;

    virtual HandlerType type() const override { return HandlerType::UnhandledInterruptHandler; }
    virtual StringView purpose() const override { return "Unhandled Interrupt Handler"sv; }
    virtual StringView controller() const override { VERIFY_NOT_REACHED(); }

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }

private:
};
}
