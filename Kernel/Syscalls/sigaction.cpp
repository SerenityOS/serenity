/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$sigprocmask(int how, Userspace<sigset_t const*> set, Userspace<sigset_t*> old_set)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::sigaction));
    auto* current_thread = Thread::current();
    u32 previous_signal_mask;
    if (set) {
        auto set_value = TRY(copy_typed_from_user(set));
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
    if (old_set) {
        TRY(copy_to_user(old_set, &previous_signal_mask));
    }
    return 0;
}

ErrorOr<FlatPtr> Process::sys$sigpending(Userspace<sigset_t*> set)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto pending_signals = Thread::current()->pending_signals();
    TRY(copy_to_user(set, &pending_signals));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$sigaction(int signum, Userspace<sigaction const*> user_act, Userspace<sigaction*> user_old_act)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::sigaction));
    if (signum < 1 || signum >= NSIG || signum == SIGKILL || signum == SIGSTOP)
        return EINVAL;

    InterruptDisabler disabler; // FIXME: This should use a narrower lock. Maybe a way to ignore signals temporarily?
    auto& action = m_signal_action_data[signum];
    if (user_old_act) {
        sigaction old_act {};
        old_act.sa_flags = action.flags;
        old_act.sa_sigaction = reinterpret_cast<decltype(old_act.sa_sigaction)>(action.handler_or_sigaction.as_ptr());
        old_act.sa_mask = action.mask;
        TRY(copy_to_user(user_old_act, &old_act));
    }
    if (user_act) {
        auto act = TRY(copy_typed_from_user(user_act));
        action.mask = act.sa_mask;
        action.flags = act.sa_flags;
        action.handler_or_sigaction = VirtualAddress { reinterpret_cast<void*>(act.sa_sigaction) };
    }
    return 0;
}

ErrorOr<FlatPtr> Process::sys$sigreturn(RegisterState& registers)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    // Here, we restore the state pushed by dispatch signal and asm_signal_trampoline.
    auto stack_ptr = registers.userspace_sp();

    // Stack state (created by the signal trampoline):
    // saved_return_value, ucontext, signal_info, fpu_state?.

    // The FPU state is at the top here, pop it off and restore it.
    // FIXME: The stack alignment is off by 8 bytes here, figure this out and remove this excessively aligned object.
    alignas(alignof(FPUState) * 2) FPUState data {};
    TRY(copy_from_user(&data, bit_cast<FPUState const*>(stack_ptr)));
    Thread::current()->fpu_state() = data;
    stack_ptr += sizeof(FPUState);

    stack_ptr += sizeof(siginfo); // We don't need this here.

    auto ucontext = TRY(copy_typed_from_user<__ucontext>(stack_ptr));
    stack_ptr += sizeof(__ucontext);

    auto saved_return_value = TRY(copy_typed_from_user<FlatPtr>(stack_ptr));

    Thread::current()->m_signal_mask = ucontext.uc_sigmask;
    Thread::current()->m_currently_handled_signal = 0;
#if ARCH(X86_64)
    auto sp = registers.rsp;
#endif

    copy_ptrace_registers_into_kernel_registers(registers, static_cast<PtraceRegisters const&>(ucontext.uc_mcontext));

#if ARCH(X86_64)
    registers.set_userspace_sp(registers.rsp);
    registers.rsp = sp;
#endif

    return saved_return_value;
}

ErrorOr<Memory::VirtualRange> Process::remap_range_as_stack(FlatPtr address, size_t size)
{
    // FIXME: This duplicates a lot of logic from sys$mprotect, this should be abstracted out somehow
    // NOTE: We shrink the given range to page boundaries (instead of expanding it), as sigaltstack's manpage suggests
    // using malloc() to allocate the stack region, and many heap implementations (including ours) store heap chunk
    // metadata in memory just before the vended pointer, which we would end up zeroing.
    auto range_to_remap = TRY(Memory::shrink_range_to_page_boundaries(address, size));
    if (!range_to_remap.size())
        return EINVAL;

    if (!is_user_range(range_to_remap))
        return EFAULT;

    return address_space().with([&](auto& space) -> ErrorOr<Memory::VirtualRange> {
        if (auto* whole_region = space->find_region_from_range(range_to_remap)) {
            if (!whole_region->is_mmap())
                return EPERM;
            if (!whole_region->vmobject().is_anonymous() || whole_region->is_shared())
                return EINVAL;
            whole_region->unsafe_clear_access();
            whole_region->set_readable(true);
            whole_region->set_writable(true);
            whole_region->set_stack(true);
            whole_region->set_syscall_region(false);
            whole_region->clear_to_zero();
            whole_region->remap();

            return range_to_remap;
        }

        if (auto* old_region = space->find_region_containing(range_to_remap)) {
            if (!old_region->is_mmap())
                return EPERM;
            if (!old_region->vmobject().is_anonymous() || old_region->is_shared())
                return EINVAL;

            // Remove the old region from our regions tree, since were going to add another region
            // with the exact same start address.
            auto region = space->take_region(*old_region);
            region->unmap();

            // This vector is the region(s) adjacent to our range.
            // We need to allocate a new region for the range we wanted to change permission bits on.
            auto adjacent_regions = TRY(space->try_split_region_around_range(*region, range_to_remap));

            size_t new_range_offset_in_vmobject = region->offset_in_vmobject() + (range_to_remap.base().get() - region->range().base().get());
            auto* new_region = TRY(space->try_allocate_split_region(*region, range_to_remap, new_range_offset_in_vmobject));
            new_region->unsafe_clear_access();
            new_region->set_readable(true);
            new_region->set_writable(true);
            new_region->set_stack(true);
            new_region->set_syscall_region(false);
            new_region->clear_to_zero();

            // Map the new regions using our page directory (they were just allocated and don't have one).
            for (auto* adjacent_region : adjacent_regions) {
                TRY(adjacent_region->map(space->page_directory()));
            }
            TRY(new_region->map(space->page_directory()));

            return range_to_remap;
        }

        if (auto const& regions = TRY(space->find_regions_intersecting(range_to_remap)); regions.size()) {
            size_t full_size_found = 0;
            // Check that all intersecting regions are compatible.
            for (auto const* region : regions) {
                if (!region->is_mmap())
                    return EPERM;
                if (!region->vmobject().is_anonymous() || region->is_shared())
                    return EINVAL;
                full_size_found += region->range().intersect(range_to_remap).size();
            }

            if (full_size_found != range_to_remap.size())
                return ENOMEM;

            // Finally, iterate over each region, either updating its access flags if the range covers it wholly,
            // or carving out a new subregion with the appropriate access flags set.
            for (auto* old_region : regions) {
                auto const intersection_to_remap = range_to_remap.intersect(old_region->range());
                // If the region is completely covered by range, simply update the access flags
                if (intersection_to_remap == old_region->range()) {
                    old_region->unsafe_clear_access();
                    old_region->set_readable(true);
                    old_region->set_writable(true);
                    old_region->set_stack(true);
                    old_region->set_syscall_region(false);
                    old_region->clear_to_zero();
                    old_region->remap();
                    continue;
                }
                // Remove the old region from our regions tree, since were going to add another region
                // with the exact same start address.
                auto region = space->take_region(*old_region);
                region->unmap();

                // This vector is the region(s) adjacent to our range.
                // We need to allocate a new region for the range we wanted to change permission bits on.
                auto adjacent_regions = TRY(space->try_split_region_around_range(*old_region, intersection_to_remap));

                // Since the range is not contained in a single region, it can only partially cover its starting and ending region,
                // therefore carving out a chunk from the region will always produce a single extra region, and not two.
                VERIFY(adjacent_regions.size() == 1);

                size_t new_range_offset_in_vmobject = old_region->offset_in_vmobject() + (intersection_to_remap.base().get() - old_region->range().base().get());
                auto* new_region = TRY(space->try_allocate_split_region(*region, intersection_to_remap, new_range_offset_in_vmobject));

                new_region->unsafe_clear_access();
                new_region->set_readable(true);
                new_region->set_writable(true);
                new_region->set_stack(true);
                new_region->set_syscall_region(false);
                new_region->clear_to_zero();

                // Map the new region using our page directory (they were just allocated and don't have one) if any.
                TRY(adjacent_regions[0]->map(space->page_directory()));

                TRY(new_region->map(space->page_directory()));
            }

            return range_to_remap;
        }

        return EINVAL;
    });
}

ErrorOr<FlatPtr> Process::sys$sigaltstack(Userspace<stack_t const*> user_ss, Userspace<stack_t*> user_old_ss)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::sigaction));

    if (user_old_ss) {
        stack_t old_ss_value {};
        if (Thread::current()->m_alternative_signal_stack.has_value()) {
            old_ss_value.ss_sp = Thread::current()->m_alternative_signal_stack->base().as_ptr();
            old_ss_value.ss_size = Thread::current()->m_alternative_signal_stack->size();
            if (Thread::current()->is_in_alternative_signal_stack())
                old_ss_value.ss_flags = SS_ONSTACK;
            else
                old_ss_value.ss_flags = 0;
        } else {
            old_ss_value.ss_flags = SS_DISABLE;
        }
        TRY(copy_to_user(user_old_ss, &old_ss_value));
    }

    if (user_ss) {
        auto ss = TRY(copy_typed_from_user(user_ss));

        if (Thread::current()->is_in_alternative_signal_stack())
            return EPERM;

        if (ss.ss_flags == SS_DISABLE) {
            Thread::current()->m_alternative_signal_stack.clear();
        } else if (ss.ss_flags == 0) {
            if (ss.ss_size <= MINSIGSTKSZ)
                return ENOMEM;
            if (Checked<FlatPtr>::addition_would_overflow((FlatPtr)ss.ss_sp, ss.ss_size))
                return ENOMEM;

            // In order to preserve compatibility with our MAP_STACK, W^X and syscall region
            // protections, sigaltstack ranges are carved out of their regions, zeroed, and
            // turned into read/writable MAP_STACK-enabled regions.
            // This is inspired by OpenBSD's solution: https://man.openbsd.org/sigaltstack.2
            Thread::current()->m_alternative_signal_stack = TRY(remap_range_as_stack((FlatPtr)ss.ss_sp, ss.ss_size));
        } else {
            return EINVAL;
        }
    }

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigtimedwait.html
ErrorOr<FlatPtr> Process::sys$sigtimedwait(Userspace<sigset_t const*> set, Userspace<siginfo_t*> info, Userspace<timespec const*> timeout)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::sigaction));

    sigset_t set_value;
    TRY(copy_from_user(&set_value, set));

    Thread::BlockTimeout block_timeout = {};
    if (timeout) {
        auto timeout_time = TRY(copy_time_from_user(timeout));
        block_timeout = Thread::BlockTimeout(false, &timeout_time);
    }

    siginfo_t info_value = {};
    auto block_result = Thread::current()->block<Thread::SignalBlocker>(block_timeout, set_value, info_value);
    if (block_result.was_interrupted())
        return EINTR;
    // We check for an unset signal instead of directly checking for a timeout interruption
    // in order to allow polling the pending signals by setting the timeout to 0.
    if (info_value.si_signo == SIGINVAL) {
        VERIFY(block_result == Thread::BlockResult::InterruptedByTimeout);
        return EAGAIN;
    }

    if (info)
        TRY(copy_to_user(info, &info_value));
    return info_value.si_signo;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigsuspend.html
ErrorOr<FlatPtr> Process::sys$sigsuspend(Userspace<sigset_t const*> mask)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);

    auto sigmask = TRY(copy_typed_from_user(mask));

    auto* current_thread = Thread::current();

    u32 previous_signal_mask = current_thread->update_signal_mask(sigmask);
    ScopeGuard rollback_signal_mask([&]() {
        current_thread->update_signal_mask(previous_signal_mask);
    });

    // TODO: Ensure that/check if we never return if the action is to terminate the process.
    // TODO: Ensure that/check if we only return after an eventual signal-catching function returns.
    Thread::BlockTimeout timeout = {};
    siginfo_t siginfo = {};
    if (current_thread->block<Thread::SignalBlocker>(timeout, ~sigmask, siginfo).was_interrupted())
        return EINTR;

    return 0;
}

}
