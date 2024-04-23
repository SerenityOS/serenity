/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/riscv64/IRQController.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class PLIC final : public IRQController {
public:
    PLIC(PhysicalAddress, size_t size, u32 interrupt_count);

    virtual void enable(GenericInterruptHandler const&) override;
    virtual void disable(GenericInterruptHandler const&) override;

    virtual void eoi(GenericInterruptHandler const&) override;

    virtual u8 pending_interrupt() const override;

    virtual StringView model() const override { return "PLIC"sv; }

private:
    enum {
        M_MODE_CONTEXT = 0,
        S_MODE_CONTEXT,
        CONTEXTS_PER_HART,
    };
    static constexpr size_t interrupt_context = (0 * CONTEXTS_PER_HART) + S_MODE_CONTEXT; // We assume we only have hart 0, change this once we support SMP

    struct RegisterMap {
        u32 interrupt_priority[1024];
        u32 interrupt_pending_bitmap[32];
        u32 reserved1[992];
        u32 interrupt_enable_bitmap[15872][32];
        u32 reserved2[14336];
        struct {
            u32 priority_threshold;
            u32 claim_complete;
            u32 reserved3[1022];
        } contexts[15872];
    };
    static_assert(AssertSize<RegisterMap, 0x4000000>());

    void initialize();

    Memory::TypedMapping<RegisterMap volatile> m_registers;
    u32 m_interrupt_count { 0 };
};

}
