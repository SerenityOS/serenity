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

int Process::sys$sigreturn(RegisterState& registers)
{
    REQUIRE_PROMISE(stdio);
    SmapDisabler disabler;

    //Here, we restore the state pushed by dispatch signal and asm_signal_trampoline.
    u32* stack_ptr = (u32*)registers.userspace_esp;
    u32 smuggled_eax = *stack_ptr;

    //pop the stored eax, ebp, return address, handler and signal code
    stack_ptr += 5;

    Thread::current()->m_signal_mask = *stack_ptr;
    stack_ptr++;

    //pop edi, esi, ebp, esp, ebx, edx, ecx and eax
    memcpy(&registers.edi, stack_ptr, 8 * sizeof(FlatPtr));
    stack_ptr += 8;

    registers.eip = *stack_ptr;
    stack_ptr++;

    registers.eflags = *stack_ptr;
    stack_ptr++;

    registers.userspace_esp = registers.esp;
    return smuggled_eax;
}

}
