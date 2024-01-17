/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/riscv64/IRQController.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/TypedMapping.h>

// https://github.com/riscv/riscv-plic-spec/releases/download/1.0.0/riscv-plic-1.0.0.pdf
// Or chapter 10 of https://sifive.cdn.prismic.io/sifive/f24c0f97-cd86-4a88-9f2d-af23e8e32a10_u74mc_core_complex_manual_21G1.pdf

namespace Kernel::RISCV64 {

static constexpr size_t PLIC_MAX_SOURCE_COUNT = 1024;
static constexpr size_t PLIC_MAX_CONTEXT_COUNT = 0x3e00;
static constexpr size_t PLIC_SOURCE_BITS_PER_ENABLE_OR_PENDING_REGISTER = 32;

// Chapter 3. Memory Map
struct PLICRegisters {
    struct Context {
        u32 priority_threshold;
        u32 claim_complete;
        u32 _reserved[(0x1000 - 8) / 4];
    };

    u32 priority[PLIC_MAX_SOURCE_COUNT];
    u32 pending[PLIC_MAX_SOURCE_COUNT / PLIC_SOURCE_BITS_PER_ENABLE_OR_PENDING_REGISTER];
    u32 _reserved0[0x3e0];
    u32 enable_for_context[PLIC_MAX_CONTEXT_COUNT][PLIC_MAX_SOURCE_COUNT / PLIC_SOURCE_BITS_PER_ENABLE_OR_PENDING_REGISTER];
    u32 _reserved1[0x3800];
    Context context[PLIC_MAX_CONTEXT_COUNT];
};

static_assert(AssertSize<PLICRegisters, 0x400'0000>());
static_assert(__builtin_offsetof(PLICRegisters, priority[0]) == 0);
static_assert(__builtin_offsetof(PLICRegisters, pending[0]) == 0x1000);
static_assert(__builtin_offsetof(PLICRegisters, pending[(PLIC_MAX_SOURCE_COUNT - 32) / 32]) == 0x107c);
static_assert(__builtin_offsetof(PLICRegisters, enable_for_context[0][0 / 32]) == 0x2000);
static_assert(__builtin_offsetof(PLICRegisters, enable_for_context[0][(PLIC_MAX_SOURCE_COUNT - 32) / 32]) == 0x207c);
static_assert(__builtin_offsetof(PLICRegisters, enable_for_context[PLIC_MAX_CONTEXT_COUNT - 1][992 / 32]) == 0x1f'1ffc);
static_assert(__builtin_offsetof(PLICRegisters, context[0].priority_threshold) == 0x20'0000);
static_assert(__builtin_offsetof(PLICRegisters, context[0].claim_complete) == 0x20'0004);
static_assert(__builtin_offsetof(PLICRegisters, context[1].priority_threshold) == 0x20'1000);
static_assert(__builtin_offsetof(PLICRegisters, context[1].claim_complete) == 0x20'1004);
static_assert(__builtin_offsetof(PLICRegisters, context[PLIC_MAX_CONTEXT_COUNT - 1].priority_threshold) == 0x3ff'f000);
static_assert(__builtin_offsetof(PLICRegisters, context[PLIC_MAX_CONTEXT_COUNT - 1].claim_complete) == 0x3ff'f004);
static_assert(AssertSize<PLICRegisters::Context, 0x1000>());

class PLIC final : public IRQController {
public:
    static ErrorOr<NonnullLockRefPtr<PLIC>> try_to_initialize(PhysicalAddress);

    virtual void enable(GenericInterruptHandler const&) override;
    virtual void disable(GenericInterruptHandler const&) override;

    virtual size_t claim() const override;
    virtual void eoi(GenericInterruptHandler const&) const override;

    virtual StringView model() const override { return "RISC-V PLIC"sv; }

private:
    PLIC(Memory::TypedMapping<PLICRegisters volatile>&&);

    mutable Memory::TypedMapping<PLICRegisters volatile> m_regs;
};

}
