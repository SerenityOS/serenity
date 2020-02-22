/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/String.h>
#include <AK/Types.h>

namespace Kernel {

enum class IRQControllerType {
    i8259 = 1,   /* Intel 8259 Dual PIC */
    i82093AA = 2 /* Intel 82093AA I/O ADVANCED PROGRAMMABLE INTERRUPT CONTROLLER (IOAPIC) */
};

class IRQController {
public:
    virtual ~IRQController() {}

    virtual void enable(u8 number) = 0;
    virtual void disable(u8 number) = 0;
    virtual void hard_disable() { m_hard_disabled = true; }
    virtual bool is_vector_enabled(u8 number) const = 0;
    bool is_enabled() const { return m_enabled && !m_hard_disabled; }
    bool is_hard_disabled() const { return m_hard_disabled; }
    virtual void eoi(u8 number) const = 0;
    virtual u32 get_gsi_base() const = 0;
    virtual u16 get_isr() const = 0;
    virtual u16 get_irr() const = 0;
    virtual const char* model() const = 0;
    virtual IRQControllerType type() const = 0;

protected:
    IRQController() {}
    virtual void initialize() = 0;
    bool m_enabled { false };
    bool m_hard_disabled { false };
};
}
