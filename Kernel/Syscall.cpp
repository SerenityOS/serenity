#include "i386.h"
#include "Task.h"
#include "Syscall.h"
#include "Console.h"
#include <AK/Lock.h>

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
    "    pushw %ss\n"
    "    popw %ds\n"
    "    popw %es\n"
    "    popw %fs\n"
    "    popw %gs\n"
    "    mov %esp, syscallRegDump\n"
    "    call syscall_entry\n"
    "    popw %gs\n"
    "    popw %gs\n"
    "    popw %fs\n"
    "    popw %es\n"
    "    popw %ds\n"
    "    popa\n"
    "    iret\n"
);

namespace Syscall {

static SpinLock* s_lock;

void initialize()
{
    s_lock = new SpinLock;
    registerUserCallableInterruptHandler(0x80, syscall_ISR);
    kprintf("syscall: int 0x80 handler installed\n");
}

DWORD handle(DWORD function, DWORD arg1, DWORD arg2, DWORD arg3)
{
    Locker locker(*s_lock);
    switch (function) {
    case Syscall::Yield:
        yield();
        break;
    case Syscall::PutCharacter:
        Console::the().putChar(arg1 & 0xff);
        break;
    case Syscall::Sleep:
        //kprintf("syscall: sleep(%d)\n", arg1);
        current->sys$sleep(arg1);
        break;
    case Syscall::Spawn:
        return current->sys$spawn((const char*)arg1);
    case Syscall::GetDirEntries:
        return current->sys$get_dir_entries((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::PosixLstat:
        return current->sys$lstat((const char*)arg1, (void*)arg2);
    case Syscall::PosixOpen:
        //kprintf("syscall: open('%s', %u)\n", arg1, arg2);
        return current->sys$open((const char*)arg1, (size_t)arg2);
    case Syscall::PosixClose:
        //kprintf("syscall: close(%d)\n", arg1);
        return current->sys$close((int)arg1);
    case Syscall::PosixRead:
        //kprintf("syscall: read(%d, %p, %u)\n", arg1, arg2, arg3);
        return current->sys$read((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::PosixSeek:
        // FIXME: This has the wrong signature, should be like lseek()
        kprintf("syscall: seek(%d, %d)\n", arg1, arg2);
        return current->sys$seek((int)arg1, (int)arg2);
    case Syscall::PosixKill:
        kprintf("syscall: kill(%d, %d)\n", arg1, arg2);
        return current->sys$kill((pid_t)arg1, (int)arg2);
    case Syscall::PosixGetuid:
        return current->sys$getuid();
    case Syscall::PosixGetgid:
        return current->sys$getgid();
    case Syscall::PosixGetpid:
        return current->sys$getpid();
    case Syscall::PosixWaitpid:
        return current->sys$waitpid((pid_t)arg1);
    case Syscall::PosixMmap:
        return (dword)current->sys$mmap((void*)arg1, (size_t)arg2);
    case Syscall::PosixMunmap:
        return current->sys$munmap((void*)arg1, (size_t)arg2);
    case Syscall::PosixExit:
        cli();
        locker.unlock();
        current->sys$exit((int)arg1);
        ASSERT_NOT_REACHED();
        return 0;
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
