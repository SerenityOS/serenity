#include "i386.h"
#include "Process.h"
#include "Syscall.h"
#include "Console.h"
#include "Scheduler.h"

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
    register_user_callable_interrupt_handler(0x80, syscall_ISR);
    kprintf("syscall: int 0x80 handler installed\n");
}

static dword handle(RegisterDump& regs, dword function, dword arg1, dword arg2, dword arg3)
{
    ASSERT_INTERRUPTS_ENABLED();
    switch (function) {
    case Syscall::SC_yield:
        Scheduler::yield();
        break;
    case Syscall::SC_putch:
        Console::the().putChar(arg1 & 0xff);
        break;
    case Syscall::SC_sleep:
        return current->sys$sleep((unsigned)arg1);
    case Syscall::SC_gettimeofday:
        return current->sys$gettimeofday((timeval*)arg1);
    case Syscall::SC_get_dir_entries:
        return current->sys$get_dir_entries((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::SC_lstat:
        return current->sys$lstat((const char*)arg1, (Unix::stat*)arg2);
    case Syscall::SC_stat:
        return current->sys$stat((const char*)arg1, (Unix::stat*)arg2);
    case Syscall::SC_getcwd:
        return current->sys$getcwd((char*)arg1, (size_t)arg2);
    case Syscall::SC_open:
        return current->sys$open((const char*)arg1, (int)arg2);
    case Syscall::SC_write:
        return current->sys$write((int)arg1, (const void*)arg2, (size_t)arg3);
    case Syscall::SC_close:
        //kprintf("syscall: close(%d)\n", arg1);
        return current->sys$close((int)arg1);
    case Syscall::SC_read:
        //kprintf("syscall: read(%d, %p, %u)\n", arg1, arg2, arg3);
        return current->sys$read((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::SC_lseek:
        return current->sys$lseek((int)arg1, (off_t)arg2, (int)arg3);
    case Syscall::SC_kill:
        return current->sys$kill((pid_t)arg1, (int)arg2);
    case Syscall::SC_getuid:
        return current->sys$getuid();
    case Syscall::SC_getgid:
        return current->sys$getgid();
    case Syscall::SC_getpid:
        return current->sys$getpid();
    case Syscall::SC_getppid:
        return current->sys$getppid();
    case Syscall::SC_waitpid:
        return current->sys$waitpid((pid_t)arg1, (int*)arg2, (int)arg3);
    case Syscall::SC_mmap:
        return (dword)current->sys$mmap((const SC_mmap_params*)arg1);
    case Syscall::SC_munmap:
        return current->sys$munmap((void*)arg1, (size_t)arg2);
    case Syscall::SC_gethostname:
        return current->sys$gethostname((char*)arg1, (size_t)arg2);
    case Syscall::SC_exit:
        cli();
        current->sys$exit((int)arg1);
        ASSERT_NOT_REACHED();
        return 0;
    case Syscall::SC_get_arguments:
        return current->sys$get_arguments((int*)arg1, (char***)arg2);
    case Syscall::SC_get_environment:
        return current->sys$get_environment((char***)arg1);
    case Syscall::SC_chdir:
        return current->sys$chdir((const char*)arg1);
    case Syscall::SC_uname:
        return current->sys$uname((utsname*)arg1);
    case Syscall::SC_set_mmap_name:
        return current->sys$set_mmap_name((void*)arg1, (size_t)arg2, (const char*)arg3);
    case Syscall::SC_readlink:
        return current->sys$readlink((const char*)arg1, (char*)arg2, (size_t)arg3);
    case Syscall::SC_ttyname_r:
        return current->sys$ttyname_r((int)arg1, (char*)arg2, (size_t)arg3);
    case Syscall::SC_setsid:
        return current->sys$setsid();
    case Syscall::SC_getsid:
        return current->sys$getsid((pid_t)arg1);
    case Syscall::SC_setpgid:
        return current->sys$setpgid((pid_t)arg1, (pid_t)arg2);
    case Syscall::SC_getpgid:
        return current->sys$getpgid((pid_t)arg1);
    case Syscall::SC_getpgrp:
        return current->sys$getpgrp();
    case Syscall::SC_fork:
        return current->sys$fork(regs);
    case Syscall::SC_execve:
        return current->sys$execve((const char*)arg1, (const char**)arg2, (const char**)arg3);
    case Syscall::SC_geteuid:
        return current->sys$geteuid();
    case Syscall::SC_getegid:
        return current->sys$getegid();
    case Syscall::SC_isatty:
        return current->sys$isatty((int)arg1);
    case Syscall::SC_getdtablesize:
        return current->sys$getdtablesize();
    case Syscall::SC_dup:
        return current->sys$dup((int)arg1);
    case Syscall::SC_dup2:
        return current->sys$dup2((int)arg1, (int)arg2);
    case Syscall::SC_sigaction:
        return current->sys$sigaction((int)arg1, (const Unix::sigaction*)arg2, (Unix::sigaction*)arg3);
    case Syscall::SC_umask:
        return current->sys$umask((mode_t)arg1);
    case Syscall::SC_getgroups:
        return current->sys$getgroups((int)arg1, (gid_t*)arg2);
    case Syscall::SC_setgroups:
        return current->sys$setgroups((size_t)arg1, (const gid_t*)arg2);
    case Syscall::SC_sigreturn:
        current->sys$sigreturn();
        ASSERT_NOT_REACHED();
        return 0;
    case Syscall::SC_sigprocmask:
        return current->sys$sigprocmask((int)arg1, (const Unix::sigset_t*)arg2, (Unix::sigset_t*)arg3);
    case Syscall::SC_pipe:
        return current->sys$pipe((int*)arg1);
    case Syscall::SC_killpg:
        return current->sys$killpg((int)arg1, (int)arg2);
    case Syscall::SC_setuid:
        return current->sys$setuid((uid_t)arg1);
    case Syscall::SC_setgid:
        return current->sys$setgid((gid_t)arg1);
    case Syscall::SC_alarm:
        return current->sys$alarm((unsigned)arg1);
    case Syscall::SC_access:
        return current->sys$access((const char*)arg1, (int)arg2);
    case Syscall::SC_fcntl:
        return current->sys$fcntl((int)arg1, (int)arg2, (dword)arg3);
    case Syscall::SC_ioctl:
        return current->sys$ioctl((int)arg1, (unsigned)arg2, (unsigned)arg3);
    case Syscall::SC_fstat:
        return current->sys$fstat((int)arg1, (Unix::stat*)arg2);
    case Syscall::SC_mkdir:
        return current->sys$mkdir((const char*)arg1, (mode_t)arg2);
    default:
        kprintf("<%u> int0x80: Unknown function %u requested {%x, %x, %x}\n", current->pid(), function, arg1, arg2, arg3);
        break;
    }
    return 0;
}

}

void syscall_entry(RegisterDump& regs)
{
    dword function = regs.eax;
    dword arg1 = regs.edx;
    dword arg2 = regs.ecx;
    dword arg3 = regs.ebx;
    regs.eax = Syscall::handle(regs, function, arg1, arg2, arg3);
}

