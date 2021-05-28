/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

class GenericInterruptHandeler;

extern "C" void interrupt_common_asm_entry();

#define GENERATE_GENERIC_INTERRUPT_HANDLER_ASM_ENTRY(isr_number)                  \
    extern "C" void interrupt_##isr_number##_asm_entry();                         \
    static void interrupt_##isr_number##_asm_entry_dummy() __attribute__((used)); \
    NEVER_INLINE void interrupt_##isr_number##_asm_entry_dummy()                  \
    {                                                                             \
        asm(".globl interrupt_" #isr_number "_asm_entry\n"                        \
            "interrupt_" #isr_number "_asm_entry:\n"                              \
            "    pushw $" #isr_number "\n"                                        \
            "    pushw $0\n"                                                      \
            "    jmp interrupt_common_asm_entry\n");                              \
    }

void register_interrupt_handler(u8 number, void (*handler)());
void register_user_callable_interrupt_handler(u8 number, void (*handler)());
GenericInterruptHandler& get_interrupt_handler(u8 interrupt_number);
void register_generic_interrupt_handler(u8 number, GenericInterruptHandler&);
void unregister_generic_interrupt_handler(u8 number, GenericInterruptHandler&);

}
