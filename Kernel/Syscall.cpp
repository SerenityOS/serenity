#include "i386.h"
#include "Process.h"
#include "Syscall.h"
#include "Console.h"
#include "Scheduler.h"

extern "C" void syscall_trap_entry(RegisterDump&);
extern "C" void syscall_trap_handler();
extern volatile RegisterDump* syscallRegDump;

asm(
    ".globl syscall_trap_handler \n"
    "syscall_trap_handler:\n"
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
    "    call syscall_trap_entry\n"
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
    register_user_callable_interrupt_handler(0x82, syscall_trap_handler);
    kprintf("Syscall: int 0x82 handler installed\n");
}

int sync()
{
    VFS::the().sync();
    return 0;
}

static dword handle(RegisterDump& regs, dword function, dword arg1, dword arg2, dword arg3)
{
    ASSERT_INTERRUPTS_ENABLED();
    switch (function) {
    case Syscall::SC_yield:
        Scheduler::yield();
        break;
    case Syscall::SC_putch:
        Console::the().put_char(arg1 & 0xff);
        break;
    case Syscall::SC_sleep:
        return current->sys$sleep((unsigned)arg1);
    case Syscall::SC_usleep:
        return current->sys$usleep((unsigned)arg1);
    case Syscall::SC_gettimeofday:
        return current->sys$gettimeofday((timeval*)arg1);
    case Syscall::SC_get_dir_entries:
        return current->sys$get_dir_entries((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::SC_lstat:
        return current->sys$lstat((const char*)arg1, (stat*)arg2);
    case Syscall::SC_stat:
        return current->sys$stat((const char*)arg1, (stat*)arg2);
    case Syscall::SC_getcwd:
        return current->sys$getcwd((char*)arg1, (size_t)arg2);
    case Syscall::SC_open:
        return current->sys$open((const char*)arg1, (int)arg2, (mode_t)arg3);
    case Syscall::SC_write:
        return current->sys$write((int)arg1, (const byte*)arg2, (ssize_t)arg3);
    case Syscall::SC_close:
        return current->sys$close((int)arg1);
    case Syscall::SC_read:
        return current->sys$read((int)arg1, (byte*)arg2, (ssize_t)arg3);
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
    case Syscall::SC_select:
        return current->sys$select((const SC_select_params*)arg1);
    case Syscall::SC_poll:
        return current->sys$poll((pollfd*)arg1, (int)arg2, (int)arg3);
    case Syscall::SC_munmap:
        return current->sys$munmap((void*)arg1, (size_t)arg2);
    case Syscall::SC_gethostname:
        return current->sys$gethostname((char*)arg1, (size_t)arg2);
    case Syscall::SC_exit:
        cli();
        current->sys$exit((int)arg1);
        ASSERT_NOT_REACHED();
        return 0;
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
    case Syscall::SC_ptsname_r:
        return current->sys$ptsname_r((int)arg1, (char*)arg2, (size_t)arg3);
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
        return current->sys$sigaction((int)arg1, (const sigaction*)arg2, (sigaction*)arg3);
    case Syscall::SC_umask:
        return current->sys$umask((mode_t)arg1);
    case Syscall::SC_getgroups:
        return current->sys$getgroups((ssize_t)arg1, (gid_t*)arg2);
    case Syscall::SC_setgroups:
        return current->sys$setgroups((ssize_t)arg1, (const gid_t*)arg2);
    case Syscall::SC_sigreturn:
        current->sys$sigreturn();
        ASSERT_NOT_REACHED();
        return 0;
    case Syscall::SC_sigprocmask:
        return current->sys$sigprocmask((int)arg1, (const sigset_t*)arg2, (sigset_t*)arg3);
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
        return current->sys$fstat((int)arg1, (stat*)arg2);
    case Syscall::SC_mkdir:
        return current->sys$mkdir((const char*)arg1, (mode_t)arg2);
    case Syscall::SC_times:
        return current->sys$times((tms*)arg1);
    case Syscall::SC_utime:
        return current->sys$utime((const char*)arg1, (const utimbuf*)arg2);
    case Syscall::SC_sync:
        return sync();
    case Syscall::SC_link:
        return current->sys$link((const char*)arg1, (const char*)arg2);
    case Syscall::SC_unlink:
        return current->sys$unlink((const char*)arg1);
    case Syscall::SC_read_tsc:
        return current->sys$read_tsc((dword*)arg1, (dword*)arg2);
    case Syscall::SC_rmdir:
        return current->sys$rmdir((const char*)arg1);
    case Syscall::SC_chmod:
        return current->sys$chmod((const char*)arg1, (mode_t)arg2);
    case Syscall::SC_socket:
        return current->sys$socket((int)arg1, (int)arg2, (int)arg3);
    case Syscall::SC_bind:
        return current->sys$bind((int)arg1, (const sockaddr*)arg2, (socklen_t)arg3);
    case Syscall::SC_listen:
        return current->sys$listen((int)arg1, (int)arg2);
    case Syscall::SC_accept:
        return current->sys$accept((int)arg1, (sockaddr*)arg2, (socklen_t*)arg3);
    case Syscall::SC_connect:
        return current->sys$connect((int)arg1, (const sockaddr*)arg2, (socklen_t)arg3);
    case Syscall::SC_create_shared_buffer:
        return current->sys$create_shared_buffer((pid_t)arg1, (size_t)arg2, (void**)arg3);
    case Syscall::SC_get_shared_buffer:
        return (dword)current->sys$get_shared_buffer((int)arg1);
    case Syscall::SC_release_shared_buffer:
        return current->sys$release_shared_buffer((int)arg1);
    case Syscall::SC_chown:
        return current->sys$chown((const char*)arg1, (uid_t)arg2, (gid_t)arg3);
    default:
        kprintf("<%u> int0x80: Unknown function %u requested {%x, %x, %x}\n", current->pid(), function, arg1, arg2, arg3);
        break;
    }
    return 0;
}

}

void syscall_trap_entry(RegisterDump& regs)
{
    dword function = regs.eax;
    dword arg1 = regs.edx;
    dword arg2 = regs.ecx;
    dword arg3 = regs.ebx;
    regs.eax = Syscall::handle(regs, function, arg1, arg2, arg3);
    if (function == Syscall::SC_mkdir)
        dbgprintf("->%d\n", regs.eax);
}

