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

#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$sigprocmask(int how, Userspace<const sigset_t*> set, Userspace<sigset_t*> old_set)
{
    REQUIRE_PROMISE(sigaction);
    auto current_thread = Thread::current();
    u32 previous_signal_mask;
    if (set) {
        sigset_t set_value;
        if (!copy_from_user(&set_value, set))
            return -EFAULT;
        switch (how) {
        case SIG_BLOCK:
            previous_signal_mask = current_thread->signal_mask_block(set_value, true);
            break;
        case SIG_UNBLOCK:
            previous_signal_mask = current_thread->signal_mask_block(set_value, false);
            break;
        case SIG_SETMASK:
            previous_signal_mask = current_thread->update_signal_mask(set_value);
            break;
        default:
            return -EINVAL;
        }
    } else {
        previous_signal_mask = current_thread->signal_mask();
    }
    if (old_set && !copy_to_user(old_set, &previous_signal_mask))
        return -EFAULT;
    return 0;
}

int Process::sys$sigpending(Userspace<sigset_t*> set)
{
    REQUIRE_PROMISE(stdio);
    auto pending_signals = Thread::current()->pending_signals();
    if (!copy_to_user(set, &pending_signals))
        return -EFAULT;
    return 0;
}

int Process::sys$sigaction(int signum, const sigaction* act, sigaction* old_act)
{
    REQUIRE_PROMISE(sigaction);
    if (signum < 1 || signum >= 32 || signum == SIGKILL || signum == SIGSTOP)
        return -EINVAL;
    InterruptDisabler disabler; // FIXME: This should use a narrower lock. Maybe a way to ignore signals temporarily?
    auto& action = Thread::current()->m_signal_action_data[signum];
    if (old_act) {
        if (!copy_to_user(&old_act->sa_flags, &action.flags))
            return -EFAULT;
        if (!copy_to_user(&old_act->sa_sigaction, &action.handler_or_sigaction, sizeof(action.handler_or_sigaction)))
            return -EFAULT;
    }
    if (!copy_from_user(&action.flags, &act->sa_flags))
        return -EFAULT;
    if (!copy_from_user(&action.handler_or_sigaction, &act->sa_sigaction, sizeof(action.handler_or_sigaction)))
        return -EFAULT;
    return 0;
}

#ifndef SIGNAL_DEBUG
#    define SIG_VERBOSE(...)    \
        do {                    \
            dbgln(__VA_ARGS__); \
        } while (0)
#else
#    define SIG_VERBOSE(...)
#endif

int Process::sys$sigreturn(RegisterState& registers)
{
    REQUIRE_PROMISE(stdio);
    SmapDisabler disabler;

    //Here, we restore the state pushed by dispatch_signal and asm_signal_trampoline.
    u32* stack_ptr = (u32*)registers.userspace_esp;
    SIG_VERBOSE("We pulled the stack pointer {} out of argument RegisterState", stack_ptr);

    // Pop.. 2 words off the stack. That contain... things we don't care about.
    stack_ptr += 2;

    auto local_pop = [&]([[maybe_unused]] const char* value = nullptr) {
        SIG_VERBOSE("Popping {} {:#08x} from user stack", value ? value : "value", *stack_ptr);
        ++stack_ptr;
    };

    u32 smuggled_eax = *stack_ptr;
    local_pop("smuggled eax");

    //pop the trampoline ebp, return address, handler, signal code, and siginfo_t*, and ucontext_t*
    // FIXME: This feels really... awkward
    for (size_t i = 0; i < 6; ++i) {
        local_pop();
    }

    stack_ptr -= sizeof(ucontext_t);
    ucontext_t* user_context = (ucontext_t*)stack_ptr;
    for (size_t i = 0; i < sizeof(user_context->uc_mcontext) / 4; ++i) {
        SIG_VERBOSE("uc_mcontext register {}: {:#08x}", i, ((u32*)(&(user_context->uc_mcontext)))[i]);
    }

    registers.eip = *stack_ptr;
    local_pop("eip");

    registers.eflags = (registers.eflags & ~safe_eflags_mask) | (*stack_ptr & safe_eflags_mask);
    local_pop("eflags");

#define __PRINT_MACRO(x) #x
#define PRINT_MACRO(x) __PRINT_MACRO(x)

#define SET_REG(reg)                                                                           \
    do {                                                                                       \
        registers.reg = user_context->uc_mcontext.reg;                                         \
        dbgln("In RegisterState, set register " PRINT_MACRO(reg) " to {:08x}", registers.reg); \
    } while (0)

    Thread::current()->m_signal_mask = user_context->uc_sigmask;
    SET_REG(edi);
    SET_REG(esi);
    SET_REG(ebp);
    SET_REG(esp);
    SET_REG(ebx);
    SET_REG(edx);
    SET_REG(ecx);
    SET_REG(eax);
    SET_REG(eip);
    SET_REG(eflags);

    /*
    registers.edi = user_context->uc_mcontext.edi;
    registers.esi = user_context->uc_mcontext.edi;
    registers.ebp = user_context->uc_mcontext.edi;
    registers.esp = user_context->uc_mcontext.edi;
    registers.ebx = user_context->uc_mcontext.edi;
    registers.edx = user_context->uc_mcontext.edi;
    registers.ecx = user_context->uc_mcontext.edi;
    registers.eax = user_context->uc_mcontext.eax;
    registers.eip = user_context->uc_mcontext.eip;
    registers.eflags = user_context->uc_mcontext.eflags;
*/

    // Check stack alignment. ... If this is off, it doesn't seem to crash?
    // Yet? We don't have any programs that have super-aligned stack requirements yet
    stack_ptr += sizeof(siginfo_t) / 4;
    if ((u32)stack_ptr != registers.esp && round_up_to_power_of_two((u32)stack_ptr, 16) != registers.esp) {
        dbgln("Unusual stack on signal return! Expected: {:#08x} Actual: {:#08x}", stack_ptr, registers.esp);
    }
    SIG_VERBOSE("New esp for userspace: {:#08x}", registers.esp);

    // This is a common eflags value on sigreturn...
    // Alignment issues might cause an EFAULT loop in this case
    if (registers.eip == 0x220) {
        dbgln("Suspicious userspace instruction pointer to return to! {:#08x}", registers.eip);
        ASSERT_NOT_REACHED();
    }
    registers.userspace_esp = registers.esp;
    return smuggled_eax;
}
}
