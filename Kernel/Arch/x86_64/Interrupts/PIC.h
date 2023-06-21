/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Arch/x86_64/IRQController.h>

namespace Kernel {

static constexpr size_t pic_disabled_vector_base = 0x20;
static constexpr size_t pic_disabled_vector_end = 0x2f;

class PIC final : public IRQController {
public:
    PIC();
    virtual void enable(GenericInterruptHandler const&) override;
    virtual void disable(GenericInterruptHandler const&) override;
    virtual void hard_disable() override;
    virtual void eoi(GenericInterruptHandler const&) const override;
    virtual bool is_vector_enabled(u8 number) const override;
    virtual bool is_enabled() const override;
    virtual void spurious_eoi(GenericInterruptHandler const&) const override;
    virtual u16 get_isr() const override;
    virtual u16 get_irr() const override;
    virtual u32 gsi_base() const override { return 0; }
    virtual size_t interrupt_vectors_count() const override { return 16; }
    virtual StringView model() const override { return "Dual i8259"sv; }
    virtual IRQControllerType type() const override { return IRQControllerType::i8259; }

private:
    u16 m_cached_irq_mask { 0xffff };
    void eoi_interrupt(u8 irq) const;
    void enable_vector(u8 number);
    void remap(u8 offset);
    void complete_eoi() const;
    virtual void initialize() override;
};

}
