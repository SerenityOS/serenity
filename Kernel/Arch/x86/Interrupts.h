/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
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

namespace Kernel {

class GenericInterruptHandeler;

extern "C" void interrupt_common_asm_entry();

#define GENERATE_GENERIC_INTERRUPT_HANDLER_ASM_ENTRY(isr_number) \
    extern "C" void interrupt_##isr_number##_asm_entry();        \
    asm(".globl interrupt_" #isr_number "_asm_entry\n"           \
        "interrupt_" #isr_number "_asm_entry:\n"                 \
        "    pushw $" #isr_number "\n"                           \
        "    pushw $0\n"                                         \
        "    jmp interrupt_common_asm_entry\n");

void register_interrupt_handler(u8 number, void (*handler)());
void register_user_callable_interrupt_handler(u8 number, void (*handler)());
GenericInterruptHandler& get_interrupt_handler(u8 interrupt_number);
void register_generic_interrupt_handler(u8 number, GenericInterruptHandler&);
void unregister_generic_interrupt_handler(u8 number, GenericInterruptHandler&);

}
