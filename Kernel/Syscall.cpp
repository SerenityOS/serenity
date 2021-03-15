/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/API/Syscall.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

extern "C" void syscall_handler(TrapFrame*);
extern "C" void syscall_asm_entry();

// clang-format off
#if ARCH(I386)
asm(
    ".globl syscall_asm_entry\n"
    "syscall_asm_entry:\n"
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
    "    mov %ax, %fs\n"
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
    ".globl syscall_asm_entry\n"
    "syscall_asm_entry:\n"
    "    cli\n"
    "    hlt\n");
#endif
// clang-format on

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
static Handler s_syscall_table[] = {
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
};
#undef __ENUMERATE_SYSCALL

KResultOr<FlatPtr> handle(RegisterState& regs, FlatPtr function, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3)
{
    VERIFY_INTERRUPTS_ENABLED();
    auto current_thread = Thread::current();
    auto& process = current_thread->process();
    current_thread->did_syscall();

    if (function == SC_abort || function == SC_exit || function == SC_exit_thread) {
        // These syscalls need special handling since they never return to the caller.

        if (auto* tracer = process.tracer(); tracer && tracer->is_tracing_syscalls()) {
            regs.eax = 0;
            tracer->set_trace_syscalls(false);
            process.tracer_trap(*current_thread, regs); // this triggers SIGTRAP and stops the thread!
        }

        switch (function) {
        case SC_abort:
            process.sys$abort();
            break;
        case SC_exit:
            process.sys$exit(arg1);
            break;
        case SC_exit_thread:
            process.sys$exit_thread(arg1);
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

void syscall_handler(TrapFrame* trap)
{
    auto& regs = *trap->regs;
    auto current_thread = Thread::current();
    VERIFY(current_thread->previous_mode() == Thread::PreviousMode::UserMode);
    auto& process = current_thread->process();

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

    if ((regs.eflags & (iopl_mask)) != 0) {
        PANIC("Syscall from process with IOPL != 0");
    }

    // NOTE: We take the big process lock before inspecting memory regions.
    process.big_lock().lock();

    if (!MM.validate_user_stack(process, VirtualAddress(regs.userspace_esp))) {
        dbgln("Invalid stack pointer: {:p}", regs.userspace_esp);
        handle_crash(regs, "Bad stack on syscall entry", SIGSTKFLT);
    }

    auto* calling_region = MM.find_region_from_vaddr(process.space(), VirtualAddress(regs.eip));
    if (!calling_region) {
        dbgln("Syscall from {:p} which has no associated region", regs.eip);
        handle_crash(regs, "Syscall from unknown region", SIGSEGV);
    }

    if (calling_region->is_writable()) {
        dbgln("Syscall from writable memory at {:p}", regs.eip);
        handle_crash(regs, "Syscall from writable memory", SIGSEGV);
    }

    if (process.space().enforces_syscall_regions() && !calling_region->is_syscall_region()) {
        dbgln("Syscall from non-syscall region");
        handle_crash(regs, "Syscall from non-syscall region", SIGSEGV);
    }

    auto function = regs.eax;
    auto arg1 = regs.edx;
    auto arg2 = regs.ecx;
    auto arg3 = regs.ebx;

    auto result = Syscall::handle(regs, function, arg1, arg2, arg3);
    if (result.is_error())
        regs.eax = result.error();
    else
        regs.eax = result.value();

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
