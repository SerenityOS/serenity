/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/SmapDisabler.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$sigprocmask(int how, Userspace<const sigset_t*> set, Userspace<sigset_t*> old_set)
{
    REQUIRE_PROMISE(sigaction);
    auto current_thread = Thread::current();
    u32 previous_signal_mask;
    if (set) {
        sigset_t set_value;
        if (!copy_from_user(&set_value, set))
            return EFAULT;
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
            return EINVAL;
        }
    } else {
        previous_signal_mask = current_thread->signal_mask();
    }
    if (old_set && !copy_to_user(old_set, &previous_signal_mask))
        return EFAULT;
    return 0;
}

KResultOr<int> Process::sys$sigpending(Userspace<sigset_t*> set)
{
    REQUIRE_PROMISE(stdio);
    auto pending_signals = Thread::current()->pending_signals();
    if (!copy_to_user(set, &pending_signals))
        return EFAULT;
    return 0;
}

KResultOr<int> Process::sys$sigaction(int signum, Userspace<const sigaction*> user_act, Userspace<sigaction*> user_old_act)
{
    REQUIRE_PROMISE(sigaction);
    if (signum < 1 || signum >= 32 || signum == SIGKILL || signum == SIGSTOP)
        return EINVAL;

    InterruptDisabler disabler; // FIXME: This should use a narrower lock. Maybe a way to ignore signals temporarily?
    auto& action = Thread::current()->m_signal_action_data[signum];
    if (user_old_act) {
        sigaction old_act {};
        old_act.sa_flags = action.flags;
        old_act.sa_sigaction = reinterpret_cast<decltype(old_act.sa_sigaction)>(action.handler_or_sigaction.as_ptr());
        if (!copy_to_user(user_old_act, &old_act))
            return EFAULT;
    }
    if (user_act) {
        sigaction act {};
        if (!copy_from_user(&act, user_act))
            return EFAULT;
        action.flags = act.sa_flags;
        action.handler_or_sigaction = VirtualAddress { reinterpret_cast<void*>(act.sa_sigaction) };
    }
    return 0;
}

KResultOr<int> Process::sys$sigreturn([[maybe_unused]] RegisterState& registers)
{
    REQUIRE_PROMISE(stdio);
    SmapDisabler disabler;

#if ARCH(I386)
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

    registers.eflags = (registers.eflags & ~safe_eflags_mask) | (*stack_ptr & safe_eflags_mask);
    stack_ptr++;

    registers.userspace_esp = registers.esp;
    return smuggled_eax;
#else
    PANIC("sys$sigreturn() not implemented.");
#endif
}

}
