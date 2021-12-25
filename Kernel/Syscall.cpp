/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/Arch/x86/Interrupts.h>
#include <Kernel/Arch/x86/TrapFrame.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/PCSpeaker.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Panic.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/ThreadTracer.h>

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

static ErrorOr<FlatPtr> handle(RegisterState&, FlatPtr function, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3, FlatPtr arg4);

UNMAP_AFTER_INIT void initialize()
{
    register_user_callable_interrupt_handler(syscall_vector, syscall_asm_entry);
}

using Handler = auto (Process::*)(FlatPtr, FlatPtr, FlatPtr, FlatPtr) -> ErrorOr<FlatPtr>;
using HandlerWithRegisterState = auto (Process::*)(RegisterState&) -> ErrorOr<FlatPtr>;

struct HandlerMetadata {
    Handler handler;
    NeedsBigProcessLock needs_lock;
};

#define __ENUMERATE_SYSCALL(sys_call, needs_lock) { bit_cast<Handler>(&Process::sys$##sys_call), needs_lock },
static const HandlerMetadata s_syscall_table[] = {
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
};
#undef __ENUMERATE_SYSCALL

ErrorOr<FlatPtr> handle(RegisterState& regs, FlatPtr function, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3, FlatPtr arg4)
{
    VERIFY_INTERRUPTS_ENABLED();
    auto current_thread = Thread::current();
    auto& process = current_thread->process();
    current_thread->did_syscall();

    PerformanceManager::add_syscall_event(*current_thread, regs);

    if (function >= Function::__Count) {
        dbgln("Unknown syscall {} requested ({:p}, {:p}, {:p}, {:p})", function, arg1, arg2, arg3, arg4);
        return ENOSYS;
    }

    const auto syscall_metadata = s_syscall_table[function];
    if (syscall_metadata.handler == nullptr) {
        dbgln("Null syscall {} requested, you probably need to rebuild this program!", function);
        return ENOSYS;
    }

    MutexLocker mutex_locker;
    const auto needs_big_lock = syscall_metadata.needs_lock == NeedsBigProcessLock::Yes;
    if (needs_big_lock) {
        mutex_locker.attach_and_lock(process.big_lock());
    };

    if (function == SC_exit || function == SC_exit_thread) {
        // These syscalls need special handling since they never return to the caller.
        // In these cases the process big lock will get released on the exit of the thread.

        if (auto* tracer = process.tracer(); tracer && tracer->is_tracing_syscalls()) {
            regs.set_return_reg(0);
            tracer->set_trace_syscalls(false);
            process.tracer_trap(*current_thread, regs); // this triggers SIGTRAP and stops the thread!
        }

        switch (function) {
        case SC_exit:
            process.sys$exit(arg1);
        case SC_exit_thread:
            process.sys$exit_thread(arg1, arg2, arg3);
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ErrorOr<FlatPtr> result { FlatPtr(nullptr) };
    if (function == SC_fork || function == SC_sigreturn) {
        // These syscalls want the RegisterState& rather than individual parameters.
        auto handler = bit_cast<HandlerWithRegisterState>(syscall_metadata.handler);
        result = (process.*(handler))(regs);
    } else {
        result = (process.*(syscall_metadata.handler))(arg1, arg2, arg3, arg4);
    }

    return result;
}

}

static const Array<u16, Syscall::Function::__Count> s_syscall_notes {
    // To be *slightly* musically-meaningful, we have to acknowledge that tones sound equidistant
    // when they have a constant *factor* between their frequencies.
    // An octave is factor 2, a semitone is factor 2**(1/12).
    // The following python code was used to generate the initial list:
    //     for i in range(151):
    //         freq = 80 * (2 ** (i / 24))
    //         print('{},'.format(round(freq)))
    // 80 Hz to 6089 Hz seem to be a good range, hence the scaling.
    // clang-format off
    80, 82, 85, 87, 90, 92, 95, 98, 101, 104, 107, 110,
    113, 116, 120, 123, 127, 131, 135, 138, 143, 147, 151, 155,
    160, 165, 170, 174, 180, 185, 190, 196, 202, 207, 214, 220,
    226, 233, 240, 247, 254, 261, 269, 277, 285, 293, 302, 311,
    320, 329, 339, 349, 359, 370, 381, 392, 403, 415, 427, 440,
    453, 466, 479, 494, 508, 523, 538, 554, 570, 587, 604, 622,
    640, 659, 678, 698, 718, 739, 761, 783, 806, 830, 854, 879,
    905, 932, 959, 987, 1016, 1046, 1076, 1108, 1140, 1174, 1208, 1244,
    1280, 1318, 1356, 1396, 1437, 1479, 1522, 1567, 1613, 1660, 1709, 1759,
    1810, 1863, 1918, 1974, 2032, 2091, 2153, 2216, 2281, 2348, 2416, 2487,
    2560, 2635, 2712, 2792, 2874, 2958, 3044, 3134, 3225, 3320, 3417, 3517,
    3620, 3726, 3836, 3948, 4064, 4183, 4305, 4432, 4561, 4695, 4833, 4974,
    5120, 5270, 5424, 5583, 5747, 5915, 6089
    // clang-format on
    // The next frequencies are 6267, 6451, 6640, 6834, 7035, 7241, 7453, 7671, and 7896.
};

NEVER_INLINE void syscall_handler(TrapFrame* trap)
{
    // Make sure SMAP protection is enabled on syscall entry.
    clac();

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

    // Apply a random offset in the range 0-255 to the stack pointer,
    // to make kernel stacks a bit less deterministic.
    u32 lsw;
    u32 msw;
    read_tsc(lsw, msw);

    auto* ptr = (char*)__builtin_alloca(lsw & 0xff);
    asm volatile(""
                 : "=m"(*ptr));

    static constexpr FlatPtr iopl_mask = 3u << 12;

    FlatPtr flags = regs.flags();
    if ((flags & (iopl_mask)) != 0) {
        PANIC("Syscall from process with IOPL != 0");
    }

    Memory::MemoryManager::validate_syscall_preconditions(process.address_space(), regs);

    FlatPtr function;
    FlatPtr arg1;
    FlatPtr arg2;
    FlatPtr arg3;
    FlatPtr arg4;
    regs.capture_syscall_params(function, arg1, arg2, arg3, arg4);

    if (kernel_command_line().is_audiate_syscalls_enabled_cached() && function < Syscall::Function::__Count) {
        PCSpeaker::tone_on(s_syscall_notes[function]);
        auto result = Thread::current()->sleep(Time::from_milliseconds(20));
        (void)result;
        PCSpeaker::tone_off();
    }

    auto result = Syscall::handle(regs, function, arg1, arg2, arg3, arg4);

    if (result.is_error()) {
        regs.set_return_reg(-result.error().code());
    } else {
        regs.set_return_reg(result.value());
    }

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

    VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
}

}
