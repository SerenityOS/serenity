#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessTracer.h>
#include <Kernel/Syscall.h>

extern "C" void syscall_trap_entry(RegisterDump);
extern "C" void syscall_trap_handler();

asm(
    ".globl syscall_trap_handler \n"
    "syscall_trap_handler:\n"
    "    pushl $0x0\n"
    "    pusha\n"
    "    pushw %ds\n"
    "    pushw %es\n"
    "    pushw %fs\n"
    "    pushw %gs\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    popw %ds\n"
    "    popw %es\n"
    "    popw %fs\n"
    "    popw %gs\n"
    "    call syscall_trap_entry\n"
    "    popw %gs\n"
    "    popw %gs\n"
    "    popw %fs\n"
    "    popw %es\n"
    "    popw %ds\n"
    "    popa\n"
    "    add $0x4, %esp\n"
    "    iret\n");

namespace Syscall {

static int handle(RegisterDump&, u32 function, u32 arg1, u32 arg2, u32 arg3);

void initialize()
{
    register_user_callable_interrupt_handler(0x82, syscall_trap_handler);
    kprintf("Syscall: int 0x82 handler installed\n");
}

#pragma GCC diagnostic ignored "-Wcast-function-type"
typedef int (Process::*Handler)(u32, u32, u32);
#define __ENUMERATE_SYSCALL(x) reinterpret_cast<Handler>(&Process::sys$##x),
static Handler s_syscall_table[] = {
    ENUMERATE_SYSCALLS
};
#undef __ENUMERATE_SYSCALL

int handle(RegisterDump& regs, u32 function, u32 arg1, u32 arg2, u32 arg3)
{
    ASSERT_INTERRUPTS_ENABLED();
    auto& process = current->process();
    process.did_syscall();

    if (function == SC_exit || function == SC_exit_thread) {
        // These syscalls need special handling since they never return to the caller.
        cli();
        if (auto* tracer = process.tracer())
            tracer->did_syscall(function, arg1, arg2, arg3, 0);
        if (function == SC_exit)
            process.sys$exit((int)arg1);
        else
            process.sys$exit_thread((int)arg1);
        ASSERT_NOT_REACHED();
        return 0;
    }

    if (function == SC_fork)
        return process.sys$fork(regs);

    if (function == SC_sigreturn)
        return process.sys$sigreturn(regs);

    if (function >= Function::__Count) {
        dbg() << process << ": Unknown syscall %u requested (" << arg1 << ", " << arg2 << ", " << arg3 << ")";
        return -ENOSYS;
    }

    return (process.*(s_syscall_table[function]))(arg1, arg2, arg3);
}

}

void syscall_trap_entry(RegisterDump regs)
{
    auto& process = current->process();
    process.big_lock().lock();
    u32 function = regs.eax;
    u32 arg1 = regs.edx;
    u32 arg2 = regs.ecx;
    u32 arg3 = regs.ebx;
    regs.eax = (u32)Syscall::handle(regs, function, arg1, arg2, arg3);
    if (auto* tracer = process.tracer())
        tracer->did_syscall(function, arg1, arg2, arg3, regs.eax);
    process.big_lock().unlock();
}
