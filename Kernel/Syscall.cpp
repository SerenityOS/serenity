#include "i386.h"
#include "Process.h"
#include "Syscall.h"
#include "Console.h"

extern "C" void syscall_entry(RegisterDump&);
extern "C" void syscall_ISR();
extern volatile RegisterDump* syscallRegDump;

asm(
    ".globl syscall_ISR \n"
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
    "    mov %esp, %eax\n"
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

void initialize()
{
    registerUserCallableInterruptHandler(0x80, syscall_ISR);
    kprintf("syscall: int 0x80 handler installed\n");
}

DWORD handle(DWORD function, DWORD arg1, DWORD arg2, DWORD arg3)
{
    ASSERT_INTERRUPTS_ENABLED();
    switch (function) {
    case Syscall::Yield:
        yield();
        break;
    case Syscall::PutCharacter:
        Console::the().putChar(arg1 & 0xff);
        break;
    case Syscall::Sleep:
        return current->sys$sleep(arg1);
    case Syscall::PosixGettimeofday:
        return current->sys$gettimeofday((timeval*)arg1);
    case Syscall::Spawn:
        return current->sys$spawn((const char*)arg1, (const char**)arg2);
    case Syscall::GetDirEntries:
        return current->sys$get_dir_entries((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::PosixLstat:
        return current->sys$lstat((const char*)arg1, (Unix::stat*)arg2);
    case Syscall::PosixStat:
        return current->sys$stat((const char*)arg1, (Unix::stat*)arg2);
    case Syscall::PosixGetcwd:
        return current->sys$getcwd((char*)arg1, (size_t)arg2);
    case Syscall::PosixOpen:
        return current->sys$open((const char*)arg1, (int)arg2);
    case Syscall::PosixWrite:
        return current->sys$write((int)arg1, (const void*)arg2, (size_t)arg3);
    case Syscall::PosixClose:
        //kprintf("syscall: close(%d)\n", arg1);
        return current->sys$close((int)arg1);
    case Syscall::PosixRead:
        //kprintf("syscall: read(%d, %p, %u)\n", arg1, arg2, arg3);
        return current->sys$read((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::PosixLseek:
        return current->sys$lseek((int)arg1, (off_t)arg2, (int)arg3);
    case Syscall::PosixKill:
        return current->sys$kill((pid_t)arg1, (int)arg2);
    case Syscall::PosixGetuid:
        return current->sys$getuid();
    case Syscall::PosixGetgid:
        return current->sys$getgid();
    case Syscall::PosixGetpid:
        return current->sys$getpid();
    case Syscall::PosixWaitpid:
        return current->sys$waitpid((pid_t)arg1, (int*)arg2, (int)arg3);
    case Syscall::PosixMmap:
        return (dword)current->sys$mmap((void*)arg1, (size_t)arg2);
    case Syscall::PosixMunmap:
        return current->sys$munmap((void*)arg1, (size_t)arg2);
    case Syscall::PosixGethostname:
        return current->sys$gethostname((char*)arg1, (size_t)arg2);
    case Syscall::PosixExit:
        cli();
        current->sys$exit((int)arg1);
        ASSERT_NOT_REACHED();
        return 0;
    case Syscall::GetArguments:
        return current->sys$get_arguments((int*)arg1, (char***)arg2);
    case Syscall::GetEnvironment:
        return current->sys$get_environment((char***)arg1);
    case Syscall::PosixChdir:
        return current->sys$chdir((const char*)arg1);
    case Syscall::PosixUname:
        return current->sys$uname((utsname*)arg1);
    case Syscall::SetMmapName:
        return current->sys$set_mmap_name((void*)arg1, (size_t)arg2, (const char*)arg3);
    case Syscall::PosixReadlink:
        return current->sys$readlink((const char*)arg1, (char*)arg2, (size_t)arg3);
    case Syscall::PosixTtynameR:
        return current->sys$ttyname_r((int)arg1, (char*)arg2, (size_t)arg3);
    default:
        kprintf("<%u> int0x80: Unknown function %x requested {%x, %x, %x}\n", current->pid(), function, arg1, arg2, arg3);
        break;
    }
    return 0;
}

}

void syscall_entry(RegisterDump& regs)
{
    DWORD function = regs.eax;
    DWORD arg1 = regs.edx;
    DWORD arg2 = regs.ecx;
    DWORD arg3 = regs.ebx;
    regs.eax = Syscall::handle(function, arg1, arg2, arg3);
}
