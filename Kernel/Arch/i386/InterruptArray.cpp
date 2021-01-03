/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/Assertions.h>
#include <AK/Singleton.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/InterruptArray.h>

namespace Kernel {
static AK::Singleton<InterruptArray> s_the;

InterruptArray& InterruptArray::the()
{
    return *s_the;
}

void InterruptArray::initialize(u32 cpu)
{
    if (cpu == 0) {
        ASSERT(!s_the.is_initialized());
        s_the.ensure_instance();
    } else {
        ASSERT(s_the.is_initialized());
    }
}

InterruptArray::InterruptArray()
{
}

RefPtr<GenericInterruptHandler> InterruptArray::interrupt_handler(u8 number) const
{
    return m_interrupt_handlers[number];
}
void InterruptArray::set_interrupt_handler(u8 number, GenericInterruptHandler& handler)
{
    m_interrupt_handlers[number] = handler;
}

bool InterruptArray::is_initialized()
{
    return s_the.is_initialized();
}

}
