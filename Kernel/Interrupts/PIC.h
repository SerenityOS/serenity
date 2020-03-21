/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Interrupts/IRQController.h>

namespace Kernel {
class PIC final : public IRQController {
public:
    PIC();
    virtual void enable(const GenericInterruptHandler&) override;
    virtual void disable(const GenericInterruptHandler&) override;
    virtual void hard_disable() override;
    virtual void eoi(const GenericInterruptHandler&) const override;
    virtual bool is_vector_enabled(u8 number) const override;
    virtual bool is_enabled() const override;
    virtual void spurious_eoi(const GenericInterruptHandler&) const override;
    virtual u16 get_isr() const override;
    virtual u16 get_irr() const override;
    virtual u32 gsi_base() const override { return 0; }
    virtual size_t interrupt_vectors_count() const { return 16; }
    virtual const char* model() const override { return "Dual i8259"; }
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
