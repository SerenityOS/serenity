#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Console.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessTracer.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Syscall.h>

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
    "    iret\n");

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

static u32 handle(RegisterDump& regs, u32 function, u32 arg1, u32 arg2, u32 arg3)
{
    current->process().did_syscall();

    ASSERT_INTERRUPTS_ENABLED();
    switch (function) {
    case Syscall::SC_yield:
        Scheduler::yield();
        break;
    case Syscall::SC_beep:
        Scheduler::beep();
        break;
    case Syscall::SC_donate:
        return current->process().sys$donate((int)arg1);
    case Syscall::SC_gettid:
        return current->process().sys$gettid();
    case Syscall::SC_putch:
        Console::the().put_char(arg1 & 0xff);
        break;
    case Syscall::SC_dbgputch:
        return current->process().sys$dbgputch((u8)arg1);
    case Syscall::SC_dbgputstr:
        return current->process().sys$dbgputstr((const u8*)arg1, (int)arg2);
    case Syscall::SC_sleep:
        return current->process().sys$sleep((unsigned)arg1);
    case Syscall::SC_usleep:
        return current->process().sys$usleep((unsigned)arg1);
    case Syscall::SC_gettimeofday:
        return current->process().sys$gettimeofday((timeval*)arg1);
    case Syscall::SC_get_dir_entries:
        return current->process().sys$get_dir_entries((int)arg1, (void*)arg2, (size_t)arg3);
    case Syscall::SC_lstat:
        return current->process().sys$lstat((const char*)arg1, (stat*)arg2);
    case Syscall::SC_stat:
        return current->process().sys$stat((const char*)arg1, (stat*)arg2);
    case Syscall::SC_getcwd:
        return current->process().sys$getcwd((char*)arg1, (size_t)arg2);
    case Syscall::SC_open:
        return current->process().sys$open((const SC_open_params*)arg1);
    case Syscall::SC_write:
        return current->process().sys$write((int)arg1, (const u8*)arg2, (ssize_t)arg3);
    case Syscall::SC_close:
        return current->process().sys$close((int)arg1);
    case Syscall::SC_read:
        return current->process().sys$read((int)arg1, (u8*)arg2, (ssize_t)arg3);
    case Syscall::SC_lseek:
        return current->process().sys$lseek((int)arg1, (off_t)arg2, (int)arg3);
    case Syscall::SC_kill:
        return current->process().sys$kill((pid_t)arg1, (int)arg2);
    case Syscall::SC_getuid:
        return current->process().sys$getuid();
    case Syscall::SC_getgid:
        return current->process().sys$getgid();
    case Syscall::SC_getpid:
        return current->process().sys$getpid();
    case Syscall::SC_getppid:
        return current->process().sys$getppid();
    case Syscall::SC_waitpid:
        return current->process().sys$waitpid((pid_t)arg1, (int*)arg2, (int)arg3);
    case Syscall::SC_mmap:
        return (u32)current->process().sys$mmap((const SC_mmap_params*)arg1);
    case Syscall::SC_select:
        return current->process().sys$select((const SC_select_params*)arg1);
    case Syscall::SC_poll:
        return current->process().sys$poll((pollfd*)arg1, (int)arg2, (int)arg3);
    case Syscall::SC_munmap:
        return current->process().sys$munmap((void*)arg1, (size_t)arg2);
    case Syscall::SC_gethostname:
        return current->process().sys$gethostname((char*)arg1, (size_t)arg2);
    case Syscall::SC_exit:
        cli();
        if (auto* tracer = current->process().tracer())
            tracer->did_syscall(function, arg1, arg2, arg3, 0);
        current->process().sys$exit((int)arg1);
        ASSERT_NOT_REACHED();
        return 0;
    case Syscall::SC_exit_thread:
        cli();
        if (auto* tracer = current->process().tracer())
            tracer->did_syscall(function, arg1, arg2, arg3, 0);
        current->process().sys$exit_thread((int)arg1);
        ASSERT_NOT_REACHED();
        break;
    case Syscall::SC_chdir:
        return current->process().sys$chdir((const char*)arg1);
    case Syscall::SC_uname:
        return current->process().sys$uname((utsname*)arg1);
    case Syscall::SC_set_mmap_name:
        return current->process().sys$set_mmap_name((void*)arg1, (size_t)arg2, (const char*)arg3);
    case Syscall::SC_readlink:
        return current->process().sys$readlink((const char*)arg1, (char*)arg2, (size_t)arg3);
    case Syscall::SC_ttyname_r:
        return current->process().sys$ttyname_r((int)arg1, (char*)arg2, (size_t)arg3);
    case Syscall::SC_ptsname_r:
        return current->process().sys$ptsname_r((int)arg1, (char*)arg2, (size_t)arg3);
    case Syscall::SC_setsid:
        return current->process().sys$setsid();
    case Syscall::SC_getsid:
        return current->process().sys$getsid((pid_t)arg1);
    case Syscall::SC_setpgid:
        return current->process().sys$setpgid((pid_t)arg1, (pid_t)arg2);
    case Syscall::SC_getpgid:
        return current->process().sys$getpgid((pid_t)arg1);
    case Syscall::SC_getpgrp:
        return current->process().sys$getpgrp();
    case Syscall::SC_fork:
        return current->process().sys$fork(regs);
    case Syscall::SC_execve:
        return current->process().sys$execve((const char*)arg1, (const char**)arg2, (const char**)arg3);
    case Syscall::SC_geteuid:
        return current->process().sys$geteuid();
    case Syscall::SC_getegid:
        return current->process().sys$getegid();
    case Syscall::SC_isatty:
        return current->process().sys$isatty((int)arg1);
    case Syscall::SC_getdtablesize:
        return current->process().sys$getdtablesize();
    case Syscall::SC_dup:
        return current->process().sys$dup((int)arg1);
    case Syscall::SC_dup2:
        return current->process().sys$dup2((int)arg1, (int)arg2);
    case Syscall::SC_sigaction:
        return current->process().sys$sigaction((int)arg1, (const sigaction*)arg2, (sigaction*)arg3);
    case Syscall::SC_umask:
        return current->process().sys$umask((mode_t)arg1);
    case Syscall::SC_getgroups:
        return current->process().sys$getgroups((ssize_t)arg1, (gid_t*)arg2);
    case Syscall::SC_setgroups:
        return current->process().sys$setgroups((ssize_t)arg1, (const gid_t*)arg2);
    case Syscall::SC_sigreturn:
        if (auto* tracer = current->process().tracer())
            tracer->did_syscall(function, arg1, arg2, arg3, 0);
        current->process().sys$sigreturn();
        ASSERT_NOT_REACHED();
        return 0;
    case Syscall::SC_sigprocmask:
        return current->process().sys$sigprocmask((int)arg1, (const sigset_t*)arg2, (sigset_t*)arg3);
    case Syscall::SC_pipe:
        return current->process().sys$pipe((int*)arg1);
    case Syscall::SC_killpg:
        return current->process().sys$killpg((int)arg1, (int)arg2);
    case Syscall::SC_setuid:
        return current->process().sys$setuid((uid_t)arg1);
    case Syscall::SC_setgid:
        return current->process().sys$setgid((gid_t)arg1);
    case Syscall::SC_alarm:
        return current->process().sys$alarm((unsigned)arg1);
    case Syscall::SC_access:
        return current->process().sys$access((const char*)arg1, (int)arg2);
    case Syscall::SC_fcntl:
        return current->process().sys$fcntl((int)arg1, (int)arg2, (u32)arg3);
    case Syscall::SC_ioctl:
        return current->process().sys$ioctl((int)arg1, (unsigned)arg2, (unsigned)arg3);
    case Syscall::SC_fstat:
        return current->process().sys$fstat((int)arg1, (stat*)arg2);
    case Syscall::SC_mkdir:
        return current->process().sys$mkdir((const char*)arg1, (mode_t)arg2);
    case Syscall::SC_times:
        return current->process().sys$times((tms*)arg1);
    case Syscall::SC_utime:
        return current->process().sys$utime((const char*)arg1, (const utimbuf*)arg2);
    case Syscall::SC_sync:
        return sync();
    case Syscall::SC_link:
        return current->process().sys$link((const char*)arg1, (const char*)arg2);
    case Syscall::SC_unlink:
        return current->process().sys$unlink((const char*)arg1);
    case Syscall::SC_symlink:
        return current->process().sys$symlink((const char*)arg1, (const char*)arg2);
    case Syscall::SC_read_tsc:
        return current->process().sys$read_tsc((u32*)arg1, (u32*)arg2);
    case Syscall::SC_rmdir:
        return current->process().sys$rmdir((const char*)arg1);
    case Syscall::SC_chmod:
        return current->process().sys$chmod((const char*)arg1, (mode_t)arg2);
    case Syscall::SC_fchmod:
        return current->process().sys$fchmod((int)arg1, (mode_t)arg2);
    case Syscall::SC_socket:
        return current->process().sys$socket((int)arg1, (int)arg2, (int)arg3);
    case Syscall::SC_bind:
        return current->process().sys$bind((int)arg1, (const sockaddr*)arg2, (socklen_t)arg3);
    case Syscall::SC_listen:
        return current->process().sys$listen((int)arg1, (int)arg2);
    case Syscall::SC_accept:
        return current->process().sys$accept((int)arg1, (sockaddr*)arg2, (socklen_t*)arg3);
    case Syscall::SC_connect:
        return current->process().sys$connect((int)arg1, (const sockaddr*)arg2, (socklen_t)arg3);
    case Syscall::SC_create_shared_buffer:
        return current->process().sys$create_shared_buffer((size_t)arg1, (void**)arg2);
    case Syscall::SC_share_buffer_with:
        return current->process().sys$share_buffer_with((int)arg1, (pid_t)arg2);
    case Syscall::SC_get_shared_buffer:
        return (u32)current->process().sys$get_shared_buffer((int)arg1);
    case Syscall::SC_release_shared_buffer:
        return current->process().sys$release_shared_buffer((int)arg1);
    case Syscall::SC_chown:
        return current->process().sys$chown((const char*)arg1, (uid_t)arg2, (gid_t)arg3);
    case Syscall::SC_fchown:
        return current->process().sys$fchown((int)arg1, (uid_t)arg2, (gid_t)arg3);
    case Syscall::SC_restore_signal_mask:
        return current->process().sys$restore_signal_mask((u32)arg1);
    case Syscall::SC_seal_shared_buffer:
        return current->process().sys$seal_shared_buffer((int)arg1);
    case Syscall::SC_get_shared_buffer_size:
        return current->process().sys$get_shared_buffer_size((int)arg1);
    case Syscall::SC_sendto:
        return current->process().sys$sendto((const SC_sendto_params*)arg1);
    case Syscall::SC_recvfrom:
        return current->process().sys$recvfrom((const SC_recvfrom_params*)arg1);
    case Syscall::SC_getsockopt:
        return current->process().sys$getsockopt((const SC_getsockopt_params*)arg1);
    case Syscall::SC_setsockopt:
        return current->process().sys$setsockopt((const SC_setsockopt_params*)arg1);
    case Syscall::SC_create_thread:
        return current->process().sys$create_thread((int (*)(void*))arg1, (void*)arg2);
    case Syscall::SC_rename:
        return current->process().sys$rename((const char*)arg1, (const char*)arg2);
    case Syscall::SC_shm_open:
        return current->process().sys$shm_open((const char*)arg1, (int)arg2, (mode_t)arg3);
    case Syscall::SC_shm_close:
        return current->process().sys$shm_unlink((const char*)arg1);
    case Syscall::SC_ftruncate:
        return current->process().sys$ftruncate((int)arg1, (off_t)arg2);
    case Syscall::SC_systrace:
        return current->process().sys$systrace((pid_t)arg1);
    case Syscall::SC_mknod:
        return current->process().sys$mknod((const char*)arg1, (mode_t)arg2, (dev_t)arg3);
    case Syscall::SC_writev:
        return current->process().sys$writev((int)arg1, (const struct iovec*)arg2, (int)arg3);
    case Syscall::SC_getsockname:
        return current->process().sys$getsockname((int)arg1, (sockaddr*)arg2, (socklen_t*)arg3);
    case Syscall::SC_getpeername:
        return current->process().sys$getpeername((int)arg1, (sockaddr*)arg2, (socklen_t*)arg3);
    case Syscall::SC_sched_setparam:
        return current->process().sys$sched_setparam((pid_t)arg1, (struct sched_param*)arg2);
    case Syscall::SC_sched_getparam:
        return current->process().sys$sched_setparam((pid_t)arg1, (struct sched_param*)arg2);
    case Syscall::SC_halt: {
        return current->process().sys$halt();
        break;
    }
    case Syscall::SC_mount:
        return current->process().sys$mount((const char*)arg1, (const char*)arg2);
    case Syscall::SC_reboot: {
        return current->process().sys$reboot();
    }
    case Syscall::SC_dump_backtrace:
        return current->process().sys$dump_backtrace();
    case Syscall::SC_watch_file:
        return current->process().sys$watch_file((const char*)arg1, (int)arg2);
    case Syscall::SC_share_buffer_globally:
        return current->process().sys$share_buffer_globally((int)arg1);
    case Syscall::SC_set_process_icon:
        return current->process().sys$set_process_icon((int)arg1);
    default:
        kprintf("<%u> int0x82: Unknown function %u requested {%x, %x, %x}\n", current->process().pid(), function, arg1, arg2, arg3);
        return -ENOSYS;
    }
    return 0;
}

}

void syscall_trap_entry(RegisterDump& regs)
{
    current->process().big_lock().lock();
    u32 function = regs.eax;
    u32 arg1 = regs.edx;
    u32 arg2 = regs.ecx;
    u32 arg3 = regs.ebx;
    regs.eax = Syscall::handle(regs, function, arg1, arg2, arg3);
    if (auto* tracer = current->process().tracer())
        tracer->did_syscall(function, arg1, arg2, arg3, regs.eax);
    current->process().big_lock().unlock();
}
