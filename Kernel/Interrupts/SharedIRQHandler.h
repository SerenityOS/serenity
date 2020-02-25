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
#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

namespace Kernel {
class IRQHandler;
class SharedIRQHandler final : public GenericInterruptHandler {
public:
    static void initialize(u8 interrupt_number);
    virtual ~SharedIRQHandler();
    virtual void handle_interrupt(RegisterState& regs) override;

    void register_handler(GenericInterruptHandler&);
    void unregister_handler(GenericInterruptHandler&);

    virtual bool eoi() override;

    virtual size_t sharing_devices_count() const override { return m_handlers.size(); }
    virtual bool is_shared_handler() const override { return true; }
    virtual bool is_sharing_with_others() const override { return false; }

    virtual HandlerPurpose purpose() const override { return HandlerPurpose::SharedIRQHandler; }

private:
    void enable_interrupt_vector();
    void disable_interrupt_vector();
    explicit SharedIRQHandler(u8 interrupt_number);
    bool m_enabled;
    HashTable<GenericInterruptHandler*> m_handlers;
};
}
