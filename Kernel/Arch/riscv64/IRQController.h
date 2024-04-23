/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/StringView.h>
#include <AK/Types.h>

#include <Kernel/Interrupts/GenericInterruptHandler.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel {

class IRQController : public AtomicRefCounted<IRQController> {
public:
    virtual ~IRQController() = default;

    virtual void enable(GenericInterruptHandler const&) = 0;
    virtual void disable(GenericInterruptHandler const&) = 0;

    virtual void eoi(GenericInterruptHandler const&) = 0;

    virtual u8 pending_interrupt() const = 0;

    virtual StringView model() const = 0;

protected:
    IRQController() = default;
};

}
