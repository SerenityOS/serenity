#include "i386.h"
#include "VGA.h"
#include "Task.h"
#include "Syscall.h"

struct RegisterDump {
    WORD gs;
    WORD fs;
    WORD es;
    WORD ds;
    DWORD edi;
    DWORD esi;
    DWORD ebp;
    DWORD esp;
    DWORD ebx;
    DWORD edx;
    DWORD ecx;
    DWORD eax;
    DWORD eip;
    WORD cs;
    WORD __csPadding;
    DWORD eflags;
} PACKED;

extern "C" void syscall_entry();
extern "C" void syscall_ISR();
extern volatile RegisterDump* syscallRegDump;

asm(
    ".globl syscall_ISR \n"
    ".globl syscallRegDump \n"
    "syscallRegDump: \n"
    ".long 0\n"
    "syscall_ISR:\n"
    "    pusha\n"
    "    pushw %ds\n"
    "    pushw %es\n"
    "    pushw %fs\n"
    "    pushw %gs\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    popw %ds\n"
    "    popw %es\n"
    "    popw %fs\n"
    "    popw %gs\n"
    "    mov %esp, syscallRegDump\n"
    "    call syscall_entry\n"
    "    popw %gs\n"
    "    popw %fs\n"
    "    popw %es\n"
    "    popw %ds\n"
    "    popa\n"
    "    iret\n"
);

namespace Syscall {

void initialize()
{
    registerUserCallableInterruptHandler(0x80, syscall_ISR);

    kprintf("syscall: int 0x80 handler installed\n");
}

DWORD invoke(DWORD function)
{
    DWORD result;
    asm("int $0x80":"=a"(result):"a"(function));
    return result;
}

DWORD invoke(DWORD function, DWORD arg1)
{
    DWORD result;
    asm("int $0x80":"=a"(result):"a"(function),"d"(arg1));
    return result;
}

DWORD invoke(DWORD function, DWORD arg1, DWORD arg2)
{
    DWORD result;
    asm("int $0x80":"=a"(result):"a"(function),"d"(arg1),"c"(arg2));
    return result;
}

DWORD invoke(DWORD function, DWORD arg1, DWORD arg2, DWORD arg3)
{
    DWORD result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"(arg1),"c"(arg2),"b"(arg3));
    return result;
}

DWORD handle(DWORD function, DWORD arg1, DWORD arg2, DWORD arg3)
{
    switch (function) {
    case Syscall::Yield:
        yield();
        break;
    case 0x1235: // putch
        kprintf( "%c", arg1 & 0xFF );
        break;
    case Syscall::Sleep:
        //kprintf("syscall: sleep(%d)\n", arg1);
        current->sys$sleep(arg1);
        break;
    case Syscall::PosixOpen:
        Task::checkSanity("syscall");
        kprintf("syscall: open('%s', %u)\n", arg1, arg2);
        return current->sys$open((const char*)arg1, (size_t)arg2);
    case Syscall::PosixClose:
        kprintf("syscall: close(%d)\n", arg1);
        return current->sys$close((int)arg1);
    case Syscall::PosixRead:
        kprintf("syscall: read(%d, %p, %u)\n", arg1, arg2, arg3);
        return current->sys$read((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::PosixSeek:
        // FIXME: This has the wrong signature, should be like lseek()
        kprintf("syscall: seek(%d, %p, %u)\n", arg1, arg2, arg3);
        return current->sys$read((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::PosixKill:
        kprintf("syscall: kill(%d, %d)\n", arg1, arg2);
        return current->sys$kill((pid_t)arg1, (int)arg2);
    case Syscall::PosixGetuid:
        return current->sys$getuid();
    default:
        kprintf("int0x80: Unknown function %x requested {%x, %x, %x}\n", function, arg1, arg2, arg3);
        break;
    }
    return 0;
}

}

void syscall_entry()
{
    auto& regs = *syscallRegDump;
    DWORD function = regs.eax;
    DWORD arg1 = regs.edx;
    DWORD arg2 = regs.ecx;
    DWORD arg3 = regs.ebx;
    regs.eax = Syscall::handle(function, arg1, arg2, arg3);
}
