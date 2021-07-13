/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Syscall.h>
#include <Kernel/Arch/x86/Interrupts.h>
#include <Kernel/Arch/x86/TrapFrame.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

extern "C" void syscall_handler(TrapFrame*) __attribute__((used));
extern "C" void syscall_asm_entry();

NEVER_INLINE NAKED void syscall_asm_entry()
{
    // clang-format off
#if ARCH(I386)
    asm(
        "    pushl $0x0\n"
        "    pusha\n"
        "    pushl %ds\n"
        "    pushl %es\n"
        "    pushl %fs\n"
        "    pushl %gs\n"
        "    pushl %ss\n"
        "    mov $" __STRINGIFY(GDT_SELECTOR_DATA0) ", %ax\n"
        "    mov %ax, %ds\n"
        "    mov %ax, %es\n"
        "    mov $" __STRINGIFY(GDT_SELECTOR_PROC) ", %ax\n"
        "    mov %ax, %gs\n"
        "    cld\n"
        "    xor %esi, %esi\n"
        "    xor %edi, %edi\n"
        "    pushl %esp \n" // set TrapFrame::regs
        "    subl $" __STRINGIFY(TRAP_FRAME_SIZE - 4) ", %esp \n"
        "    movl %esp, %ebx \n"
        "    pushl %ebx \n" // push pointer to TrapFrame
        "    call enter_trap_no_irq \n"
        "    movl %ebx, 0(%esp) \n" // push pointer to TrapFrame
        "    call syscall_handler \n"
        "    movl %ebx, 0(%esp) \n" // push pointer to TrapFrame
        "    jmp common_trap_exit \n");
#elif ARCH(X86_64)
    asm(
        "    pushq $0x0\n"
        "    pushq %r15\n"
        "    pushq %r14\n"
        "    pushq %r13\n"
        "    pushq %r12\n"
        "    pushq %r11\n"
        "    pushq %r10\n"
        "    pushq %r9\n"
        "    pushq %r8\n"
        "    pushq %rax\n"
        "    pushq %rcx\n"
        "    pushq %rdx\n"
        "    pushq %rbx\n"
        "    pushq %rsp\n"
        "    pushq %rbp\n"
        "    pushq %rsi\n"
        "    pushq %rdi\n"
        "    pushq %rsp \n" /* set TrapFrame::regs */
        "    subq $" __STRINGIFY(TRAP_FRAME_SIZE - 8) ", %rsp \n"
        "    movq %rsp, %rdi \n"
        "    cld\n"
        "    call enter_trap_no_irq \n"
        "    movq %rsp, %rdi \n"
        "    call syscall_handler\n"
        "    jmp common_trap_exit \n");
#endif
    // clang-format on
}

namespace Syscall {

static KResultOr<FlatPtr> handle(RegisterState&, FlatPtr function, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3);

UNMAP_AFTER_INIT void initialize()
{
    register_user_callable_interrupt_handler(syscall_vector, syscall_asm_entry);
}

#pragma GCC diagnostic ignored "-Wcast-function-type"
typedef KResultOr<FlatPtr> (Process::*Handler)(FlatPtr, FlatPtr, FlatPtr);
typedef KResultOr<FlatPtr> (Process::*HandlerWithRegisterState)(RegisterState&);
#define __ENUMERATE_SYSCALL(x) reinterpret_cast<Handler>(&Process::sys$##x),
static const Handler s_syscall_table[] = {
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
};
#undef __ENUMERATE_SYSCALL

KResultOr<FlatPtr> handle(RegisterState& regs, FlatPtr function, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3)
{
    VERIFY_INTERRUPTS_ENABLED();
    auto current_thread = Thread::current();
    auto& process = current_thread->process();
    current_thread->did_syscall();

    if (function == SC_exit || function == SC_exit_thread) {
        // These syscalls need special handling since they never return to the caller.

        if (auto* tracer = process.tracer(); tracer && tracer->is_tracing_syscalls()) {
#if ARCH(I386)
            regs.eax = 0;
#else
            regs.rax = 0;
#endif
            tracer->set_trace_syscalls(false);
            process.tracer_trap(*current_thread, regs); // this triggers SIGTRAP and stops the thread!
        }

        switch (function) {
        case SC_exit:
            process.sys$exit(arg1);
            break;
        case SC_exit_thread:
            process.sys$exit_thread(arg1, arg2, arg3);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    if (function == SC_fork || function == SC_sigreturn) {
        // These syscalls want the RegisterState& rather than individual parameters.
        auto handler = (HandlerWithRegisterState)s_syscall_table[function];
        return (process.*(handler))(regs);
    }

    if (function >= Function::__Count) {
        dbgln("Unknown syscall {} requested ({:08x}, {:08x}, {:08x})", function, arg1, arg2, arg3);
        return ENOSYS;
    }

    if (s_syscall_table[function] == nullptr) {
        dbgln("Null syscall {} requested, you probably need to rebuild this program!", function);
        return ENOSYS;
    }

    return (process.*(s_syscall_table[function]))(arg1, arg2, arg3);
}

}

NEVER_INLINE void syscall_handler(TrapFrame* trap)
{
    auto& regs = *trap->regs;
    auto current_thread = Thread::current();
    VERIFY(current_thread->previous_mode() == Thread::PreviousMode::UserMode);
    auto& process = current_thread->process();
    if (process.is_dying()) {
        // It's possible this thread is just about to make a syscall while another is
        // is killing our process.
        current_thread->die_if_needed();
        return;
    }

    if (auto tracer = process.tracer(); tracer && tracer->is_tracing_syscalls()) {
        tracer->set_trace_syscalls(false);
        process.tracer_trap(*current_thread, regs); // this triggers SIGTRAP and stops the thread!
    }

    current_thread->yield_if_stopped();

    // Make sure SMAP protection is enabled on syscall entry.
    clac();

    // Apply a random offset in the range 0-255 to the stack pointer,
    // to make kernel stacks a bit less deterministic.
    u32 lsw;
    u32 msw;
    read_tsc(lsw, msw);

    auto* ptr = (char*)__builtin_alloca(lsw & 0xff);
    asm volatile(""
                 : "=m"(*ptr));

    static constexpr FlatPtr iopl_mask = 3u << 12;

    FlatPtr flags;
#if ARCH(I386)
    flags = regs.eflags;
#else
    flags = regs.rflags;
#endif

    if ((flags & (iopl_mask)) != 0) {
        PANIC("Syscall from process with IOPL != 0");
    }

    // NOTE: We take the big process lock before inspecting memory regions.
    process.big_lock().lock();

    VirtualAddress userspace_sp;
#if ARCH(I386)
    userspace_sp = VirtualAddress { regs.userspace_esp };
#else
    userspace_sp = VirtualAddress { regs.userspace_rsp };
#endif
    if (!MM.validate_user_stack(process, userspace_sp)) {
        dbgln("Invalid stack pointer: {:p}", userspace_sp);
        handle_crash(regs, "Bad stack on syscall entry", SIGSTKFLT);
    }

    VirtualAddress ip;
#if ARCH(I386)
    ip = VirtualAddress { regs.eip };
#else
    ip = VirtualAddress { regs.rip };
#endif

    auto* calling_region = MM.find_user_region_from_vaddr(process.space(), ip);
    if (!calling_region) {
        dbgln("Syscall from {:p} which has no associated region", ip);
        handle_crash(regs, "Syscall from unknown region", SIGSEGV);
    }

    if (calling_region->is_writable()) {
        dbgln("Syscall from writable memory at {:p}", ip);
        handle_crash(regs, "Syscall from writable memory", SIGSEGV);
    }

    if (process.space().enforces_syscall_regions() && !calling_region->is_syscall_region()) {
        dbgln("Syscall from non-syscall region");
        handle_crash(regs, "Syscall from non-syscall region", SIGSEGV);
    }

#if ARCH(I386)
    auto function = regs.eax;
    auto arg1 = regs.edx;
    auto arg2 = regs.ecx;
    auto arg3 = regs.ebx;
#else
    auto function = regs.rax;
    auto arg1 = regs.rdx;
    auto arg2 = regs.rcx;
    auto arg3 = regs.rbx;
#endif

    auto result = Syscall::handle(regs, function, arg1, arg2, arg3);
    if (result.is_error()) {
#if ARCH(I386)
        regs.eax = result.error();
#else
        regs.rax = result.error();
#endif
    } else {
#if ARCH(I386)
        regs.eax = result.value();
#else
        regs.rax = result.value();
#endif
    }

    process.big_lock().unlock();

    if (auto tracer = process.tracer(); tracer && tracer->is_tracing_syscalls()) {
        tracer->set_trace_syscalls(false);
        process.tracer_trap(*current_thread, regs); // this triggers SIGTRAP and stops the thread!
    }

    current_thread->yield_if_stopped();

    current_thread->check_dispatch_pending_signal();

    // If the previous mode somehow changed something is seriously messed up...
    VERIFY(current_thread->previous_mode() == Thread::PreviousMode::UserMode);

    // Check if we're supposed to return to userspace or just die.
    current_thread->die_if_needed();

    VERIFY(!g_scheduler_lock.own_lock());
}

}
