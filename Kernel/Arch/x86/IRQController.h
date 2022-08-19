/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Types.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

namespace Kernel {

class GenericInterruptHandler;

enum class IRQControllerType {
    i8259 = 1,   /* Intel 8259 Dual PIC */
    i82093AA = 2 /* Intel 82093AA I/O ADVANCED PROGRAMMABLE INTERRUPT CONTROLLER (IOAPIC) */
};

class IRQController : public AtomicRefCounted<IRQController> {
public:
    virtual ~IRQController() = default;

    virtual void enable(GenericInterruptHandler const&) = 0;
    virtual void disable(GenericInterruptHandler const&) = 0;
    virtual void hard_disable() { m_hard_disabled = true; }
    virtual bool is_vector_enabled(u8 number) const = 0;
    virtual bool is_enabled() const = 0;
    bool is_hard_disabled() const { return m_hard_disabled; }
    virtual void eoi(GenericInterruptHandler const&) const = 0;
    virtual void spurious_eoi(GenericInterruptHandler const&) const = 0;
    virtual size_t interrupt_vectors_count() const = 0;
    virtual u32 gsi_base() const = 0;
    virtual u16 get_isr() const = 0;
    virtual u16 get_irr() const = 0;
    virtual StringView model() const = 0;
    virtual IRQControllerType type() const = 0;

protected:
    IRQController() = default;
    virtual void initialize() = 0;
    bool m_hard_disabled { false };
};
}
