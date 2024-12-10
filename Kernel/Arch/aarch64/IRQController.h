/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Types.h>

namespace Kernel {

class GenericInterruptHandler;

class IRQController : public AtomicRefCounted<IRQController> {
public:
    virtual ~IRQController() = default;

    virtual void enable(GenericInterruptHandler const&) = 0;
    virtual void disable(GenericInterruptHandler const&) = 0;

    virtual void eoi(GenericInterruptHandler const&) = 0;

    virtual Optional<size_t> pending_interrupt() const = 0;

    virtual StringView model() const = 0;

protected:
    IRQController() = default;
};

}
