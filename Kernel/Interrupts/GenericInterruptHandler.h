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

#include <AK/HashTable.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>

namespace Kernel {

enum class HandlerPurpose : u8 {
    IRQHandler = 1,
    SharedIRQHandler = 2,
    UnhandledInterruptHandler = 3,
};

class GenericInterruptHandler {
public:
    static GenericInterruptHandler& from(u8 interrupt_number);
    virtual ~GenericInterruptHandler();
    virtual void handle_interrupt(RegisterState& regs) = 0;

    u8 interrupt_number() const { return m_interrupt_number; }

    bool is_enabled() const { return m_enabled; }

    size_t get_invoking_count() const { return m_invoking_count; }

    virtual size_t sharing_devices_count() const = 0;
    virtual bool is_shared_handler() const = 0;
    virtual bool is_sharing_with_others() const = 0;

    virtual HandlerPurpose purpose() const = 0;

    virtual bool eoi() = 0;
    void increment_invoking_counter();

protected:
    void enable_interrupt_vector();
    void disable_interrupt_vector();
    void change_interrupt_number(u8 number);
    explicit GenericInterruptHandler(u8 interrupt_number);

private:
    size_t m_invoking_count { 0 };
    bool m_enabled { false };
    u8 m_interrupt_number { 0 };
};
}
