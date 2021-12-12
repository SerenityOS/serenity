/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Emulator.h"
#include "MmapRegion.h"
#include "SimpleRegion.h"
#include <AK/Debug.h>
#include <AK/FileStream.h>
#include <AK/Format.h>
#include <alloca.h>
#include <fcntl.h>
#include <sched.h>
#include <serenity.h>
#include <strings.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/poll.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/uio.h>
#include <sys/utsname.h>
#include <syscall.h>
#include <termios.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

namespace UserspaceEmulator {

u32 Emulator::virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3)
{
    if constexpr (SPAM_DEBUG)
        reportln("Syscall: {} ({:x})", Syscall::to_string((Syscall::Function)function), function);
    switch (function) {
    case SC_accept4:
        return virt$accept4(arg1);
    case SC_access:
        return virt$access(arg1, arg2, arg3);
    case SC_allocate_tls:
        return virt$allocate_tls(arg1, arg2);
    case SC_anon_create:
        return virt$anon_create(arg1, arg2);
    case SC_beep:
        return virt$beep();
    case SC_bind:
        return virt$bind(arg1, arg2, arg3);
    case SC_chdir:
        return virt$chdir(arg1, arg2);
    case SC_chmod:
        return virt$chmod(arg1, arg2, arg3);
    case SC_chown:
        return virt$chown(arg1);
    case SC_clock_gettime:
        return virt$clock_gettime(arg1, arg2);
    case SC_clock_nanosleep:
        return virt$clock_nanosleep(arg1);
    case SC_clock_settime:
        return virt$clock_settime(arg1, arg2);
    case SC_close:
        return virt$close(arg1);
    case SC_connect:
        return virt$connect(arg1, arg2, arg3);
    case SC_create_inode_watcher:
        return virt$create_inode_watcher(arg1);
    case SC_dbgputstr:
        return virt$dbgputstr(arg1, arg2);
    case SC_disown:
        return virt$disown(arg1);
    case SC_dup2:
        return virt$dup2(arg1, arg2);
    case SC_emuctl:
        return virt$emuctl(arg1, arg2, arg3);
    case SC_execve:
        return virt$execve(arg1);
    case SC_exit:
        virt$exit((int)arg1);
        return 0;
    case SC_fchmod:
        return virt$fchmod(arg1, arg2);
    case SC_fchown:
        return virt$fchown(arg1, arg2, arg3);
    case SC_fcntl:
        return virt$fcntl(arg1, arg2, arg3);
    case SC_fork:
        return virt$fork();
    case SC_fstat:
        return virt$fstat(arg1, arg2);
    case SC_ftruncate:
        return virt$ftruncate(arg1, arg2);
    case SC_futex:
        return virt$futex(arg1);
    case SC_get_dir_entries:
        return virt$get_dir_entries(arg1, arg2, arg3);
    case SC_get_process_name:
        return virt$get_process_name(arg1, arg2);
    case SC_get_stack_bounds:
        return virt$get_stack_bounds(arg1, arg2);
    case SC_getcwd:
        return virt$getcwd(arg1, arg2);
    case SC_getegid:
        return virt$getegid();
    case SC_geteuid:
        return virt$geteuid();
    case SC_getgid:
        return virt$getgid();
    case SC_getgroups:
        return virt$getgroups(arg1, arg2);
    case SC_gethostname:
        return virt$gethostname(arg1, arg2);
    case SC_getpeername:
        return virt$getpeername(arg1);
    case SC_getpgid:
        return virt$getpgid(arg1);
    case SC_getpgrp:
        return virt$getpgrp();
    case SC_getpid:
        return virt$getpid();
    case SC_getrandom:
        return virt$getrandom(arg1, arg2, arg3);
    case SC_getsid:
        return virt$getsid(arg1);
    case SC_getsockname:
        return virt$getsockname(arg1);
    case SC_getsockopt:
        return virt$getsockopt(arg1);
    case SC_gettid:
        return virt$gettid();
    case SC_getuid:
        return virt$getuid();
    case SC_inode_watcher_add_watch:
        return virt$inode_watcher_add_watch(arg1);
    case SC_inode_watcher_remove_watch:
        return virt$inode_watcher_remove_watch(arg1, arg2);
    case SC_ioctl:
        return virt$ioctl(arg1, arg2, arg3);
    case SC_kill:
        return virt$kill(arg1, arg2);
    case SC_killpg:
        return virt$killpg(arg1, arg2);
    case SC_listen:
        return virt$listen(arg1, arg2);
    case SC_lseek:
        return virt$lseek(arg1, arg2, arg3);
    case SC_madvise:
        return virt$madvise(arg1, arg2, arg3);
    case SC_map_time_page:
        return -ENOSYS;
    case SC_mkdir:
        return virt$mkdir(arg1, arg2, arg3);
    case SC_mmap:
        return virt$mmap(arg1);
    case SC_mount:
        return virt$mount(arg1);
    case SC_mprotect:
        return virt$mprotect(arg1, arg2, arg3);
    case SC_mremap:
        return virt$mremap(arg1);
    case SC_msyscall:
        return virt$msyscall(arg1);
    case SC_munmap:
        return virt$munmap(arg1, arg2);
    case SC_open:
        return virt$open(arg1);
    case SC_perf_event:
        return virt$perf_event((int)arg1, arg2, arg3);
    case SC_perf_register_string:
        return virt$perf_register_string(arg1, arg2);
    case SC_pipe:
        return virt$pipe(arg1, arg2);
    case SC_pledge:
        return virt$pledge(arg1);
    case SC_poll:
        return virt$poll(arg1);
    case SC_profiling_disable:
        return virt$profiling_disable(arg1);
    case SC_profiling_enable:
        return virt$profiling_enable(arg1);
    case SC_ptsname:
        return virt$ptsname(arg1, arg2, arg3);
    case SC_purge:
        return virt$purge(arg1);
    case SC_read:
        return virt$read(arg1, arg2, arg3);
    case SC_readlink:
        return virt$readlink(arg1);
    case SC_realpath:
        return virt$realpath(arg1);
    case SC_recvfd:
        return virt$recvfd(arg1, arg2);
    case SC_recvmsg:
        return virt$recvmsg(arg1, arg2, arg3);
    case SC_rename:
        return virt$rename(arg1);
    case SC_rmdir:
        return virt$rmdir(arg1, arg2);
    case SC_sched_getparam:
        return virt$sched_getparam(arg1, arg2);
    case SC_sched_setparam:
        return virt$sched_setparam(arg1, arg2);
    case SC_sendfd:
        return virt$sendfd(arg1, arg2);
    case SC_sendmsg:
        return virt$sendmsg(arg1, arg2, arg3);
    case SC_set_coredump_metadata:
        return virt$set_coredump_metadata(arg1);
    case SC_set_mmap_name:
        return virt$set_mmap_name(arg1);
    case SC_set_process_name:
        return virt$set_process_name(arg1, arg2);
    case SC_set_thread_name:
        return virt$set_thread_name(arg1, arg2, arg3);
    case SC_setgid:
        return virt$setgid(arg2);
    case SC_setgroups:
        return virt$setgroups(arg1, arg2);
    case SC_setpgid:
        return virt$setpgid(arg1, arg2);
    case SC_setsid:
        return virt$setsid();
    case SC_setsockopt:
        return virt$setsockopt(arg1);
    case SC_setuid:
        return virt$setuid(arg1);
    case SC_shutdown:
        return virt$shutdown(arg1, arg2);
    case SC_sigaction:
        return virt$sigaction(arg1, arg2, arg3);
    case SC_sigreturn:
        return virt$sigreturn();
    case SC_socket:
        return virt$socket(arg1, arg2, arg3);
    case SC_stat:
        return virt$stat(arg1);
    case SC_symlink:
        return virt$symlink(arg1);
    case SC_sync:
        virt$sync();
        return 0;
    case SC_sysconf:
        return virt$sysconf(arg1);
    case SC_ttyname:
        return virt$ttyname(arg1, arg2, arg3);
    case SC_umask:
        return virt$umask(arg1);
    case SC_uname:
        return virt$uname(arg1);
    case SC_unlink:
        return virt$unlink(arg1, arg2);
    case SC_unveil:
        return virt$unveil(arg1);
    case SC_waitid:
        return virt$waitid(arg1);
    case SC_write:
        return virt$write(arg1, arg2, arg3);
    default:
        reportln("\n=={}==  \033[31;1mUnimplemented syscall: {}\033[0m, {:p}", getpid(), Syscall::to_string((Syscall::Function)function), function);
        dump_backtrace();
        TODO();
    }
}

int Emulator::virt$anon_create(size_t size, int options)
{
    return syscall(SC_anon_create, size, options);
}

int Emulator::virt$sendfd(int socket, int fd)
{
    return syscall(SC_sendfd, socket, fd);
}

int Emulator::virt$recvfd(int socket, int options)
{
    return syscall(SC_recvfd, socket, options);
}

int Emulator::virt$profiling_enable(pid_t pid)
{
    return syscall(SC_profiling_enable, pid);
}

int Emulator::virt$profiling_disable(pid_t pid)
{
    return syscall(SC_profiling_disable, pid);
}

FlatPtr Emulator::virt$perf_event(int event, FlatPtr arg1, FlatPtr arg2)
{
    if (event == PERF_EVENT_SIGNPOST) {
        if (is_profiling()) {
            if (profiler_string_id_map().size() > arg1)
                emit_profile_event(profile_stream(), "signpost", String::formatted("\"arg1\": {}, \"arg2\": {}", arg1, arg2));
            syscall(SC_perf_event, PERF_EVENT_SIGNPOST, profiler_string_id_map().at(arg1), arg2);
        } else {
            syscall(SC_perf_event, PERF_EVENT_SIGNPOST, arg1, arg2);
        }
        return 0;
    }
    return -ENOSYS;
}

FlatPtr Emulator::virt$perf_register_string(FlatPtr string, size_t size)
{
    char* buffer = (char*)alloca(size + 4);
    // FIXME: not nice, but works
    __builtin_memcpy(buffer, "UE: ", 4);
    mmu().copy_from_vm((buffer + 4), string, size);
    auto ret = (int)syscall(SC_perf_register_string, buffer, size + 4);

    if (ret >= 0 && is_profiling()) {
        profiler_strings().append(make<String>(StringView { buffer + 4, size }));
        profiler_string_id_map().append(ret);
        ret = profiler_string_id_map().size() - 1;
    }
    return ret;
}

int Emulator::virt$disown(pid_t pid)
{
    return syscall(SC_disown, pid);
}

int Emulator::virt$purge(int mode)
{
    return syscall(SC_purge, mode);
}

int Emulator::virt$fstat(int fd, FlatPtr statbuf)
{
    struct stat local_statbuf;
    int rc = syscall(SC_fstat, fd, &local_statbuf);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(statbuf, &local_statbuf, sizeof(local_statbuf));
    return rc;
}

int Emulator::virt$close(int fd)
{
    return syscall(SC_close, fd);
}

int Emulator::virt$mkdir(FlatPtr path, size_t path_length, mode_t mode)
{
    auto buffer = mmu().copy_buffer_from_vm(path, path_length);
    return syscall(SC_mkdir, buffer.data(), buffer.size(), mode);
}

int Emulator::virt$rmdir(FlatPtr path, size_t path_length)
{
    auto buffer = mmu().copy_buffer_from_vm(path, path_length);
    return syscall(SC_rmdir, buffer.data(), buffer.size());
}

int Emulator::virt$unlink(FlatPtr path, size_t path_length)
{
    auto buffer = mmu().copy_buffer_from_vm(path, path_length);
    return syscall(SC_unlink, buffer.data(), buffer.size());
}

int Emulator::virt$symlink(FlatPtr params_addr)
{
    Syscall::SC_symlink_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto target = mmu().copy_buffer_from_vm((FlatPtr)params.target.characters, params.target.length);
    params.target.characters = (const char*)target.data();
    params.target.length = target.size();

    auto link = mmu().copy_buffer_from_vm((FlatPtr)params.linkpath.characters, params.linkpath.length);
    params.linkpath.characters = (const char*)link.data();
    params.linkpath.length = link.size();

    return syscall(SC_symlink, &params);
}

int Emulator::virt$rename(FlatPtr params_addr)
{
    Syscall::SC_rename_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto new_path = mmu().copy_buffer_from_vm((FlatPtr)params.new_path.characters, params.new_path.length);
    params.new_path.characters = (const char*)new_path.data();
    params.new_path.length = new_path.size();

    auto old_path = mmu().copy_buffer_from_vm((FlatPtr)params.old_path.characters, params.old_path.length);
    params.old_path.characters = (const char*)old_path.data();
    params.old_path.length = old_path.size();

    return syscall(SC_rename, &params);
}

int Emulator::virt$set_coredump_metadata(FlatPtr params_addr)
{
    Syscall::SC_set_coredump_metadata_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto key = mmu().copy_buffer_from_vm((FlatPtr)params.key.characters, params.key.length);
    params.key.characters = (const char*)key.data();
    params.key.length = key.size();

    auto value = mmu().copy_buffer_from_vm((FlatPtr)params.value.characters, params.value.length);
    params.value.characters = (const char*)value.data();
    params.value.length = value.size();

    return syscall(SC_set_coredump_metadata, &params);
}

int Emulator::virt$dbgputstr(FlatPtr characters, int length)
{
    auto buffer = mmu().copy_buffer_from_vm(characters, length);
    dbgputstr((const char*)buffer.data(), buffer.size());
    return 0;
}

int Emulator::virt$chmod(FlatPtr path_addr, size_t path_length, mode_t mode)
{
    auto path = mmu().copy_buffer_from_vm(path_addr, path_length);
    return syscall(SC_chmod, path.data(), path.size(), mode);
}

int Emulator::virt$chown(FlatPtr params_addr)
{
    Syscall::SC_chown_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length);
    params.path.characters = (const char*)path.data();
    params.path.length = path.size();

    return syscall(SC_chown, &params);
}

int Emulator::virt$fchmod(int fd, mode_t mode)
{
    return syscall(SC_fchmod, fd, mode);
}

int Emulator::virt$fchown(int fd, uid_t uid, gid_t gid)
{
    return syscall(SC_fchown, fd, uid, gid);
}

int Emulator::virt$setsockopt(FlatPtr params_addr)
{
    Syscall::SC_setsockopt_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    if (params.option == SO_RCVTIMEO || params.option == SO_TIMESTAMP) {
        auto host_value_buffer_result = ByteBuffer::create_zeroed(params.value_size);
        if (!host_value_buffer_result.has_value())
            return -ENOMEM;
        auto& host_value_buffer = host_value_buffer_result.value();
        mmu().copy_from_vm(host_value_buffer.data(), (FlatPtr)params.value, params.value_size);
        int rc = setsockopt(params.sockfd, params.level, params.option, host_value_buffer.data(), host_value_buffer.size());
        if (rc < 0)
            return -errno;
        return rc;
    }

    if (params.option == SO_BINDTODEVICE) {
        auto ifname = mmu().copy_buffer_from_vm((FlatPtr)params.value, params.value_size);
        params.value = ifname.data();
        params.value_size = ifname.size();
        return syscall(SC_setsockopt, &params);
    }

    TODO();
}

int Emulator::virt$get_stack_bounds(FlatPtr base, FlatPtr size)
{
    auto* region = mmu().find_region({ m_cpu.ss(), m_cpu.esp().value() });
    FlatPtr b = region->base();
    size_t s = region->size();
    mmu().copy_to_vm(base, &b, sizeof(b));
    mmu().copy_to_vm(size, &s, sizeof(s));
    return 0;
}

int Emulator::virt$ftruncate(int fd, FlatPtr length_addr)
{
    off_t length;
    mmu().copy_from_vm(&length, length_addr, sizeof(off_t));
    return syscall(SC_ftruncate, fd, &length);
}

int Emulator::virt$uname(FlatPtr params_addr)
{
    struct utsname local_uname;
    auto rc = syscall(SC_uname, &local_uname);
    mmu().copy_to_vm(params_addr, &local_uname, sizeof(local_uname));
    return rc;
}

mode_t Emulator::virt$umask(mode_t mask)
{
    return syscall(SC_umask, mask);
}

int Emulator::virt$accept4(FlatPtr params_addr)
{
    Syscall::SC_accept4_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));
    sockaddr_storage addr = {};
    socklen_t addrlen;
    mmu().copy_from_vm(&addrlen, (FlatPtr)params.addrlen, sizeof(socklen_t));
    VERIFY(addrlen <= sizeof(addr));
    int rc = accept4(params.sockfd, (sockaddr*)&addr, &addrlen, params.flags);
    if (rc == 0) {
        mmu().copy_to_vm((FlatPtr)params.addr, &addr, addrlen);
        mmu().copy_to_vm((FlatPtr)params.addrlen, &addrlen, sizeof(socklen_t));
    }
    return rc < 0 ? -errno : rc;
}

int Emulator::virt$bind(int sockfd, FlatPtr address, socklen_t address_length)
{
    auto buffer = mmu().copy_buffer_from_vm(address, address_length);
    return syscall(SC_bind, sockfd, buffer.data(), buffer.size());
}

int Emulator::virt$connect(int sockfd, FlatPtr address, socklen_t address_size)
{
    auto buffer = mmu().copy_buffer_from_vm(address, address_size);
    return syscall(SC_connect, sockfd, buffer.data(), buffer.size());
}

int Emulator::virt$shutdown(int sockfd, int how)
{
    return syscall(SC_shutdown, sockfd, how);
}

int Emulator::virt$listen(int fd, int backlog)
{
    return syscall(SC_listen, fd, backlog);
}

int Emulator::virt$kill(pid_t pid, int signal)
{
    return syscall(SC_kill, pid, signal);
}

int Emulator::virt$killpg(int pgrp, int sig)
{
    return syscall(SC_killpg, pgrp, sig);
}

int Emulator::virt$clock_gettime(int clockid, FlatPtr timespec)
{
    struct timespec host_timespec;
    int rc = syscall(SC_clock_gettime, clockid, &host_timespec);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(timespec, &host_timespec, sizeof(host_timespec));
    return rc;
}

int Emulator::virt$clock_settime(uint32_t clock_id, FlatPtr user_ts)
{
    struct timespec user_timespec;
    mmu().copy_from_vm(&user_timespec, user_ts, sizeof(user_timespec));
    int rc = syscall(SC_clock_settime, clock_id, &user_timespec);
    return rc;
}

int Emulator::virt$set_mmap_name(FlatPtr params_addr)
{
    Syscall::SC_set_mmap_name_params params {};
    mmu().copy_from_vm(&params, params_addr, sizeof(params));
    auto name = mmu().copy_buffer_from_vm((FlatPtr)params.name.characters, params.name.length);

    auto* region = mmu().find_region({ 0x23, (FlatPtr)params.addr });
    if (!region || !is<MmapRegion>(*region))
        return -EINVAL;
    static_cast<MmapRegion&>(*region).set_name(String::copy(name));
    return 0;
}

int Emulator::virt$get_process_name(FlatPtr buffer, int size)
{
    if (size < 0)
        return -EINVAL;
    auto host_buffer_result = ByteBuffer::create_zeroed((size_t)size);
    if (!host_buffer_result.has_value())
        return -ENOMEM;
    auto& host_buffer = host_buffer_result.value();
    int rc = syscall(SC_get_process_name, host_buffer.data(), host_buffer.size());
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$set_process_name(FlatPtr user_buffer, int size)
{
    if (size < 0)
        return -EINVAL;
    auto host_buffer = mmu().copy_buffer_from_vm(user_buffer, size);
    auto name = String::formatted("(UE) {}", StringView { host_buffer.data(), host_buffer.size() });
    return syscall(SC_set_process_name, name.characters(), name.length());
}

int Emulator::virt$lseek(int fd, FlatPtr offset_addr, int whence)
{
    off_t offset;
    mmu().copy_from_vm(&offset, offset_addr, sizeof(off_t));
    auto rc = syscall(SC_lseek, fd, &offset, whence);
    mmu().copy_to_vm(offset_addr, &offset, sizeof(off_t));
    return rc;
}

int Emulator::virt$socket(int domain, int type, int protocol)
{
    return syscall(SC_socket, domain, type, protocol);
}

int Emulator::virt$recvmsg(int sockfd, FlatPtr msg_addr, int flags)
{
    msghdr mmu_msg;
    mmu().copy_from_vm(&mmu_msg, msg_addr, sizeof(mmu_msg));

    Vector<iovec, 1> mmu_iovs;
    mmu_iovs.resize(mmu_msg.msg_iovlen);
    mmu().copy_from_vm(mmu_iovs.data(), (FlatPtr)mmu_msg.msg_iov, mmu_msg.msg_iovlen * sizeof(iovec));
    Vector<ByteBuffer, 1> buffers;
    Vector<iovec, 1> iovs;
    for (const auto& iov : mmu_iovs) {
        auto buffer_result = ByteBuffer::create_uninitialized(iov.iov_len);
        if (!buffer_result.has_value())
            return -ENOMEM;
        buffers.append(buffer_result.release_value());
        iovs.append({ buffers.last().data(), buffers.last().size() });
    }

    ByteBuffer control_buffer;
    if (mmu_msg.msg_control) {
        auto buffer_result = ByteBuffer::create_uninitialized(mmu_msg.msg_controllen);
        if (!buffer_result.has_value())
            return -ENOMEM;
        control_buffer = buffer_result.release_value();
    }

    sockaddr_storage addr;
    msghdr msg = { &addr, sizeof(addr), iovs.data(), (int)iovs.size(), mmu_msg.msg_control ? control_buffer.data() : nullptr, mmu_msg.msg_controllen, mmu_msg.msg_flags };
    int rc = recvmsg(sockfd, &msg, flags);
    if (rc < 0)
        return -errno;

    for (size_t i = 0; i < buffers.size(); ++i)
        mmu().copy_to_vm((FlatPtr)mmu_iovs[i].iov_base, buffers[i].data(), mmu_iovs[i].iov_len);

    if (mmu_msg.msg_name)
        mmu().copy_to_vm((FlatPtr)mmu_msg.msg_name, &addr, min(sizeof(addr), (size_t)mmu_msg.msg_namelen));
    if (mmu_msg.msg_control)
        mmu().copy_to_vm((FlatPtr)mmu_msg.msg_control, control_buffer.data(), min(mmu_msg.msg_controllen, msg.msg_controllen));
    mmu_msg.msg_namelen = msg.msg_namelen;
    mmu_msg.msg_controllen = msg.msg_controllen;
    mmu_msg.msg_flags = msg.msg_flags;
    mmu().copy_to_vm(msg_addr, &mmu_msg, sizeof(mmu_msg));
    return rc;
}

int Emulator::virt$sendmsg(int sockfd, FlatPtr msg_addr, int flags)
{
    msghdr mmu_msg;
    mmu().copy_from_vm(&mmu_msg, msg_addr, sizeof(mmu_msg));

    Vector<iovec, 1> iovs;
    iovs.resize(mmu_msg.msg_iovlen);
    mmu().copy_from_vm(iovs.data(), (FlatPtr)mmu_msg.msg_iov, mmu_msg.msg_iovlen * sizeof(iovec));
    Vector<ByteBuffer, 1> buffers;
    for (auto& iov : iovs) {
        buffers.append(mmu().copy_buffer_from_vm((FlatPtr)iov.iov_base, iov.iov_len));
        iov = { buffers.last().data(), buffers.last().size() };
    }

    ByteBuffer control_buffer;
    if (mmu_msg.msg_control) {
        auto buffer_result = ByteBuffer::create_uninitialized(mmu_msg.msg_controllen);
        if (!buffer_result.has_value())
            return -ENOMEM;
        control_buffer = buffer_result.release_value();
    }

    sockaddr_storage address;
    socklen_t address_length = 0;
    if (mmu_msg.msg_name) {
        address_length = min(sizeof(address), (size_t)mmu_msg.msg_namelen);
        mmu().copy_from_vm(&address, (FlatPtr)mmu_msg.msg_name, address_length);
    }

    msghdr msg = { mmu_msg.msg_name ? &address : nullptr, address_length, iovs.data(), (int)iovs.size(), mmu_msg.msg_control ? control_buffer.data() : nullptr, mmu_msg.msg_controllen, mmu_msg.msg_flags };
    return sendmsg(sockfd, &msg, flags);
}

int Emulator::virt$getsockopt(FlatPtr params_addr)
{
    Syscall::SC_getsockopt_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    if (params.option == SO_PEERCRED) {
        struct ucred creds = {};
        socklen_t creds_size = sizeof(creds);
        int rc = getsockopt(params.sockfd, params.level, SO_PEERCRED, &creds, &creds_size);
        if (rc < 0)
            return -errno;
        // FIXME: Check params.value_size
        mmu().copy_to_vm((FlatPtr)params.value, &creds, sizeof(creds));
        return rc;
    }
    if (params.option == SO_ERROR) {
        int so_error;
        socklen_t so_error_len = sizeof(so_error);
        int rc = getsockopt(params.sockfd, params.level, SO_ERROR, &so_error, &so_error_len);
        if (rc < 0)
            return -errno;
        // FIXME: Check params.value_size
        mmu().copy_to_vm((FlatPtr)params.value, &so_error, sizeof(so_error));
        return rc;
    }

    dbgln("Not implemented socket param: {}", params.option);
    TODO();
}

int Emulator::virt$getsockname(FlatPtr params_addr)
{
    Syscall::SC_getsockname_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));
    sockaddr_storage addr = {};
    socklen_t addrlen;
    mmu().copy_from_vm(&addrlen, (FlatPtr)params.addrlen, sizeof(socklen_t));
    VERIFY(addrlen <= sizeof(addr));
    auto rc = getsockname(params.sockfd, (sockaddr*)&addr, &addrlen);
    if (rc == 0) {
        mmu().copy_to_vm((FlatPtr)params.addr, &addr, sizeof(addr));
        mmu().copy_to_vm((FlatPtr)params.addrlen, &addrlen, sizeof(addrlen));
    }
    return rc < 0 ? -errno : rc;
}

int Emulator::virt$getpeername(FlatPtr params_addr)
{
    Syscall::SC_getpeername_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));
    sockaddr_storage addr = {};
    socklen_t addrlen;
    mmu().copy_from_vm(&addrlen, (FlatPtr)params.addrlen, sizeof(socklen_t));
    VERIFY(addrlen <= sizeof(addr));
    auto rc = getpeername(params.sockfd, (sockaddr*)&addr, &addrlen);
    if (rc == 0) {
        mmu().copy_to_vm((FlatPtr)params.addr, &addr, sizeof(addr));
        mmu().copy_to_vm((FlatPtr)params.addrlen, &addrlen, sizeof(addrlen));
    }
    return rc < 0 ? -errno : rc;
}

int Emulator::virt$getgroups(ssize_t count, FlatPtr groups)
{
    if (!count)
        return syscall(SC_getgroups, 0, nullptr);

    auto buffer_result = ByteBuffer::create_uninitialized(count * sizeof(gid_t));
    if (!buffer_result.has_value())
        return -ENOMEM;
    auto& buffer = buffer_result.value();
    int rc = syscall(SC_getgroups, count, buffer.data());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(groups, buffer.data(), buffer.size());
    return 0;
}

int Emulator::virt$setgroups(ssize_t count, FlatPtr groups)
{
    if (!count)
        return syscall(SC_setgroups, 0, nullptr);

    auto buffer = mmu().copy_buffer_from_vm(groups, count * sizeof(gid_t));
    return syscall(SC_setgroups, count, buffer.data());
}

u32 Emulator::virt$fcntl(int fd, int cmd, u32 arg)
{
    switch (cmd) {
    case F_DUPFD:
    case F_GETFD:
    case F_SETFD:
    case F_GETFL:
    case F_SETFL:
    case F_ISTTY:
        break;
    default:
        dbgln("Invalid fcntl cmd: {}", cmd);
    }

    return syscall(SC_fcntl, fd, cmd, arg);
}

u32 Emulator::virt$open(u32 params_addr)
{
    Syscall::SC_open_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length);

    Syscall::SC_open_params host_params {};
    host_params.dirfd = params.dirfd;
    host_params.mode = params.mode;
    host_params.options = params.options;
    host_params.path.characters = (const char*)path.data();
    host_params.path.length = path.size();

    return syscall(SC_open, &host_params);
}

int Emulator::virt$pipe(FlatPtr vm_pipefd, int flags)
{
    int pipefd[2];
    int rc = syscall(SC_pipe, pipefd, flags);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(vm_pipefd, pipefd, sizeof(pipefd));
    return rc;
}

static void round_to_page_size(FlatPtr& address, size_t& size)
{
    auto new_end = round_up_to_power_of_two(address + size, PAGE_SIZE);
    address &= ~(PAGE_SIZE - 1);
    size = new_end - address;
}

u32 Emulator::virt$munmap(FlatPtr address, size_t size)
{
    if (is_profiling())
        emit_profile_event(profile_stream(), "munmap", String::formatted("\"ptr\": {}, \"size\": {}", address, size));
    round_to_page_size(address, size);
    Vector<Region*, 4> marked_for_deletion;
    bool has_non_mmap_region = false;
    mmu().for_regions_in({ 0x23, address }, size, [&](Region* region) {
        if (region) {
            if (!is<MmapRegion>(*region)) {
                has_non_mmap_region = true;
                return IterationDecision::Break;
            }
            marked_for_deletion.append(region);
        }
        return IterationDecision::Continue;
    });
    if (has_non_mmap_region)
        return -EINVAL;

    for (Region* region : marked_for_deletion) {
        m_range_allocator.deallocate(region->range());
        mmu().remove_region(*region);
    }
    return 0;
}

u32 Emulator::virt$mmap(u32 params_addr)
{
    Syscall::SC_mmap_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    u32 requested_size = round_up_to_power_of_two(params.size, PAGE_SIZE);
    FlatPtr final_address;

    Optional<Range> result;
    if (params.flags & MAP_RANDOMIZED) {
        result = m_range_allocator.allocate_randomized(requested_size, params.alignment);
    } else if (params.flags & MAP_FIXED) {
        if (params.addr)
            result = m_range_allocator.allocate_specific(VirtualAddress { params.addr }, requested_size);
        else {
            // mmap(nullptr, …, MAP_FIXED) is technically okay, but tends to be a bug.
            // Therefore, refuse to be helpful.
            reportln("\n=={}==  \033[31;1mTried to mmap at nullptr with MAP_FIXED.\033[0m, {:#x} bytes.", getpid(), params.size);
            dump_backtrace();
        }
    } else {
        result = m_range_allocator.allocate_anywhere(requested_size, params.alignment);
    }
    if (!result.has_value())
        return -ENOMEM;
    final_address = result.value().base().get();
    auto final_size = result.value().size();

    String name_str;
    if (params.name.characters) {
        auto buffer_result = ByteBuffer::create_uninitialized(params.name.length);
        if (!buffer_result.has_value())
            return -ENOMEM;
        auto& name = buffer_result.value();
        mmu().copy_from_vm(name.data(), (FlatPtr)params.name.characters, params.name.length);
        name_str = { name.data(), name.size() };
    }

    if (is_profiling())
        emit_profile_event(profile_stream(), "mmap", String::formatted(R"("ptr": {}, "size": {}, "name": "{}")", final_address, final_size, name_str));

    if (params.flags & MAP_ANONYMOUS) {
        mmu().add_region(MmapRegion::create_anonymous(final_address, final_size, params.prot, move(name_str)));
    } else {
        auto region = MmapRegion::create_file_backed(final_address, final_size, params.prot, params.flags, params.fd, params.offset, move(name_str));
        if (region->name() == "libsystem.so: .text" && !m_libsystem_start) {
            m_libsystem_start = final_address;
            m_libsystem_end = final_address + final_size;
        }
        mmu().add_region(move(region));
    }

    return final_address;
}

FlatPtr Emulator::virt$mremap(FlatPtr params_addr)
{
    Syscall::SC_mremap_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    // FIXME: Support regions that have been split in the past (e.g. due to mprotect or munmap).
    if (auto* region = mmu().find_region({ m_cpu.ds(), params.old_address })) {
        if (!is<MmapRegion>(*region))
            return -EINVAL;
        VERIFY(region->size() == params.old_size);
        auto& mmap_region = *(MmapRegion*)region;
        auto* ptr = mremap(mmap_region.data(), mmap_region.size(), mmap_region.size(), params.flags);
        if (ptr == MAP_FAILED)
            return -errno;
        return (FlatPtr)ptr;
    }
    return -EINVAL;
}

u32 Emulator::virt$mount(u32 params_addr)
{
    Syscall::SC_mount_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));
    auto target = mmu().copy_buffer_from_vm((FlatPtr)params.target.characters, params.target.length);
    auto fs_path = mmu().copy_buffer_from_vm((FlatPtr)params.fs_type.characters, params.fs_type.length);
    params.fs_type.characters = (char*)fs_path.data();
    params.fs_type.length = fs_path.size();
    params.target.characters = (char*)target.data();
    params.target.length = target.size();

    return syscall(SC_mount, &params);
}

u32 Emulator::virt$gettid()
{
    return gettid();
}

u32 Emulator::virt$getpid()
{
    return getpid();
}

u32 Emulator::virt$pledge(u32)
{
    return 0;
}

u32 Emulator::virt$unveil(u32)
{
    return 0;
}

u32 Emulator::virt$mprotect(FlatPtr base, size_t size, int prot)
{
    round_to_page_size(base, size);
    bool has_non_mmapped_region = false;

    mmu().for_regions_in({ 0x23, base }, size, [&](Region* region) {
        if (region) {
            if (!is<MmapRegion>(*region)) {
                has_non_mmapped_region = true;
                return IterationDecision::Break;
            }
            auto& mmap_region = *(MmapRegion*)region;
            mmap_region.set_prot(prot);
        }
        return IterationDecision::Continue;
    });
    if (has_non_mmapped_region)
        return -EINVAL;

    return 0;
}

u32 Emulator::virt$madvise(FlatPtr, size_t, int)
{
    return 0;
}

uid_t Emulator::virt$getuid()
{
    return getuid();
}

uid_t Emulator::virt$geteuid()
{
    return geteuid();
}

gid_t Emulator::virt$getgid()
{
    return getgid();
}

gid_t Emulator::virt$getegid()
{
    return getegid();
}

int Emulator::virt$setuid(uid_t uid)
{
    return syscall(SC_setuid, uid);
}

int Emulator::virt$setgid(gid_t gid)
{
    return syscall(SC_setgid, gid);
}

u32 Emulator::virt$write(int fd, FlatPtr data, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    auto buffer = mmu().copy_buffer_from_vm(data, size);
    return syscall(SC_write, fd, buffer.data(), buffer.size());
}

u32 Emulator::virt$read(int fd, FlatPtr buffer, ssize_t size)
{
    if (size < 0)
        return -EINVAL;
    auto buffer_result = ByteBuffer::create_uninitialized(size);
    if (!buffer_result.has_value())
        return -ENOMEM;
    auto& local_buffer = buffer_result.value();
    int nread = syscall(SC_read, fd, local_buffer.data(), local_buffer.size());
    if (nread < 0) {
        if (nread == -EPERM) {
            dump_backtrace();
            TODO();
        }
        return nread;
    }
    mmu().copy_to_vm(buffer, local_buffer.data(), local_buffer.size());
    return nread;
}

void Emulator::virt$sync()
{
    syscall(SC_sync);
}

void Emulator::virt$exit(int status)
{
    reportln("\n=={}==  \033[33;1mSyscall: exit({})\033[0m, shutting down!", getpid(), status);
    m_exit_status = status;
    m_shutdown = true;
}

ssize_t Emulator::virt$getrandom(FlatPtr buffer, size_t buffer_size, unsigned int flags)
{
    auto buffer_result = ByteBuffer::create_uninitialized(buffer_size);
    if (!buffer_result.has_value())
        return -ENOMEM;
    auto& host_buffer = buffer_result.value();
    int rc = syscall(SC_getrandom, host_buffer.data(), host_buffer.size(), flags);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$get_dir_entries(int fd, FlatPtr buffer, ssize_t size)
{
    auto buffer_result = ByteBuffer::create_uninitialized(size);
    if (!buffer_result.has_value())
        return -ENOMEM;
    auto& host_buffer = buffer_result.value();
    int rc = syscall(SC_get_dir_entries, fd, host_buffer.data(), host_buffer.size());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$ioctl([[maybe_unused]] int fd, unsigned request, [[maybe_unused]] FlatPtr arg)
{
    if (request == TIOCGWINSZ) {
        struct winsize ws;
        int rc = syscall(SC_ioctl, fd, TIOCGWINSZ, &ws);
        if (rc < 0)
            return rc;
        mmu().copy_to_vm(arg, &ws, sizeof(winsize));
        return 0;
    }
    if (request == TIOCSPGRP) {
        return syscall(SC_ioctl, fd, request, arg);
    }
    if (request == TCGETS) {
        struct termios termios;
        int rc = syscall(SC_ioctl, fd, request, &termios);
        if (rc < 0)
            return rc;
        mmu().copy_to_vm(arg, &termios, sizeof(termios));
        return rc;
    }
    if (request == TCSETS) {
        struct termios termios;
        mmu().copy_from_vm(&termios, arg, sizeof(termios));
        return syscall(SC_ioctl, fd, request, &termios);
    }
    if (request == TIOCNOTTY || request == TIOCSCTTY) {
        return syscall(SC_ioctl, fd, request, 0);
    }
    if (request == FB_IOCTL_GET_PROPERTIES) {
        size_t size = 0;
        auto rc = syscall(SC_ioctl, fd, request, &size);
        mmu().copy_to_vm(arg, &size, sizeof(size));
        return rc;
    }
    if (request == FB_IOCTL_SET_HEAD_RESOLUTION) {
        FBHeadResolution user_resolution;
        mmu().copy_from_vm(&user_resolution, arg, sizeof(user_resolution));
        auto rc = syscall(SC_ioctl, fd, request, &user_resolution);
        mmu().copy_to_vm(arg, &user_resolution, sizeof(user_resolution));
        return rc;
    }
    if (request == FB_IOCTL_SET_HEAD_VERTICAL_OFFSET_BUFFER) {
        return syscall(SC_ioctl, fd, request, arg);
    }
    reportln("Unsupported ioctl: {}", request);
    dump_backtrace();
    TODO();
}

int Emulator::virt$emuctl(FlatPtr arg1, FlatPtr arg2, FlatPtr arg3)
{
    auto* tracer = malloc_tracer();
    if (arg1 <= 4 && !tracer)
        return 0;
    switch (arg1) {
    case 1:
        tracer->target_did_malloc({}, arg3, arg2);
        return 0;
    case 2:
        tracer->target_did_free({}, arg2);
        return 0;
    case 3:
        tracer->target_did_realloc({}, arg3, arg2);
        return 0;
    case 4:
        tracer->target_did_change_chunk_size({}, arg3, arg2);
        return 0;
    case 5: // mark ROI start
        if (is_in_region_of_interest())
            return -EINVAL;
        m_is_in_region_of_interest = true;
        return 0;
    case 6: // mark ROI end
        m_is_in_region_of_interest = false;
        return 0;
    case 7:
        m_is_memory_auditing_suppressed = true;
        return 0;
    case 8:
        m_is_memory_auditing_suppressed = false;
        return 0;
    default:
        return -EINVAL;
    }
}

int Emulator::virt$fork()
{
    int rc = fork();
    if (rc < 0)
        return -errno;
    return rc;
}

int Emulator::virt$execve(FlatPtr params_addr)
{
    Syscall::SC_execve_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = String::copy(mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length));
    Vector<String> arguments;
    Vector<String> environment;

    auto copy_string_list = [this](auto& output_vector, auto& string_list) {
        for (size_t i = 0; i < string_list.length; ++i) {
            Syscall::StringArgument string;
            mmu().copy_from_vm(&string, (FlatPtr)&string_list.strings[i], sizeof(string));
            output_vector.append(String::copy(mmu().copy_buffer_from_vm((FlatPtr)string.characters, string.length)));
        }
    };

    copy_string_list(arguments, params.arguments);
    copy_string_list(environment, params.environment);

    reportln("\n=={}==  \033[33;1mSyscall:\033[0m execve", getpid());
    reportln("=={}==  @ {}", getpid(), path);
    for (auto& argument : arguments)
        reportln("=={}==    - {}", getpid(), argument);

    Vector<char*> argv;
    Vector<char*> envp;

    argv.append(const_cast<char*>("/bin/UserspaceEmulator"));
    argv.append(const_cast<char*>(path.characters()));
    if (g_report_to_debug)
        argv.append(const_cast<char*>("--report-to-debug"));
    argv.append(const_cast<char*>("--"));

    auto create_string_vector = [](auto& output_vector, auto& input_vector) {
        for (auto& string : input_vector)
            output_vector.append(const_cast<char*>(string.characters()));
        output_vector.append(nullptr);
    };

    create_string_vector(argv, arguments);
    create_string_vector(envp, environment);

    // Yoink duplicated program name.
    argv.remove(3 + (g_report_to_debug ? 1 : 0));

    return execve(argv[0], (char* const*)argv.data(), (char* const*)envp.data());
}

int Emulator::virt$stat(FlatPtr params_addr)
{
    Syscall::SC_stat_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = String::copy(mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length));
    struct stat host_statbuf;
    int rc;
    if (params.follow_symlinks)
        rc = stat(path.characters(), &host_statbuf);
    else
        rc = lstat(path.characters(), &host_statbuf);
    if (rc < 0)
        return -errno;
    mmu().copy_to_vm((FlatPtr)params.statbuf, &host_statbuf, sizeof(host_statbuf));
    return rc;
}

int Emulator::virt$realpath(FlatPtr params_addr)
{
    Syscall::SC_realpath_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length);
    auto buffer_result = ByteBuffer::create_zeroed(params.buffer.size);
    if (!buffer_result.has_value())
        return -ENOMEM;
    auto& host_buffer = buffer_result.value();

    Syscall::SC_realpath_params host_params;
    host_params.path = { (const char*)path.data(), path.size() };
    host_params.buffer = { (char*)host_buffer.data(), host_buffer.size() };
    int rc = syscall(SC_realpath, &host_params);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm((FlatPtr)params.buffer.data, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$gethostname(FlatPtr buffer, ssize_t buffer_size)
{
    if (buffer_size < 0)
        return -EINVAL;
    auto buffer_result = ByteBuffer::create_zeroed(buffer_size);
    if (!buffer_result.has_value())
        return -ENOMEM;
    auto& host_buffer = buffer_result.value();
    int rc = syscall(SC_gethostname, host_buffer.data(), host_buffer.size());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$sigaction(int signum, FlatPtr act, FlatPtr oldact)
{
    if (signum == SIGKILL) {
        reportln("Attempted to sigaction() with SIGKILL");
        return -EINVAL;
    }

    if (signum <= 0 || signum >= NSIG)
        return -EINVAL;

    struct sigaction host_act;
    mmu().copy_from_vm(&host_act, act, sizeof(host_act));

    auto& handler = m_signal_handler[signum];
    handler.handler = (FlatPtr)host_act.sa_handler;
    handler.mask = host_act.sa_mask;
    handler.flags = host_act.sa_flags;

    if (oldact) {
        struct sigaction host_oldact;
        auto& old_handler = m_signal_handler[signum];
        host_oldact.sa_handler = (void (*)(int))(old_handler.handler);
        host_oldact.sa_mask = old_handler.mask;
        host_oldact.sa_flags = old_handler.flags;
        mmu().copy_to_vm(oldact, &host_oldact, sizeof(host_oldact));
    }
    return 0;
}

int Emulator::virt$sigreturn()
{
    u32 stack_ptr = m_cpu.esp().value();
    auto local_pop = [&]() -> ValueWithShadow<u32> {
        auto value = m_cpu.read_memory32({ m_cpu.ss(), stack_ptr });
        stack_ptr += sizeof(u32);
        return value;
    };

    auto smuggled_eax = local_pop();

    stack_ptr += 4 * sizeof(u32);

    m_signal_mask = local_pop().value();

    m_cpu.set_edi(local_pop());
    m_cpu.set_esi(local_pop());
    m_cpu.set_ebp(local_pop());
    m_cpu.set_esp(local_pop());
    m_cpu.set_ebx(local_pop());
    m_cpu.set_edx(local_pop());
    m_cpu.set_ecx(local_pop());
    m_cpu.set_eax(local_pop());

    m_cpu.set_eip(local_pop().value());
    m_cpu.set_eflags(local_pop());

    // FIXME: We're losing shadow bits here.
    return smuggled_eax.value();
}

int Emulator::virt$getpgrp()
{
    return syscall(SC_getpgrp);
}

int Emulator::virt$getpgid(pid_t pid)
{
    return syscall(SC_getpgid, pid);
}

int Emulator::virt$setpgid(pid_t pid, pid_t pgid)
{
    return syscall(SC_setpgid, pid, pgid);
}

int Emulator::virt$ttyname(int fd, FlatPtr buffer, size_t buffer_size)
{
    auto buffer_result = ByteBuffer::create_zeroed(buffer_size);
    if (!buffer_result.has_value())
        return -ENOMEM;
    auto& host_buffer = buffer_result.value();
    int rc = syscall(SC_ttyname, fd, host_buffer.data(), host_buffer.size());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$getcwd(FlatPtr buffer, size_t buffer_size)
{
    auto buffer_result = ByteBuffer::create_zeroed(buffer_size);
    if (!buffer_result.has_value())
        return -ENOMEM;
    auto& host_buffer = buffer_result.value();
    int rc = syscall(SC_getcwd, host_buffer.data(), host_buffer.size());
    if (rc < 0)
        return rc;
    mmu().copy_to_vm(buffer, host_buffer.data(), host_buffer.size());
    return rc;
}

int Emulator::virt$getsid(pid_t pid)
{
    return syscall(SC_getsid, pid);
}

int Emulator::virt$access(FlatPtr path, size_t path_length, int type)
{
    auto host_path = mmu().copy_buffer_from_vm(path, path_length);
    return syscall(SC_access, host_path.data(), host_path.size(), type);
}

int Emulator::virt$waitid(FlatPtr params_addr)
{
    Syscall::SC_waitid_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    Syscall::SC_waitid_params host_params = params;
    siginfo info {};
    host_params.infop = &info;

    int rc = syscall(SC_waitid, &host_params);
    if (rc < 0)
        return rc;

    if (info.si_addr) {
        // FIXME: Translate this somehow once we actually start setting it in the kernel.
        dbgln("si_addr is set to {:p}, I did not expect this!", info.si_addr);
        TODO();
    }

    if (params.infop)
        mmu().copy_to_vm((FlatPtr)params.infop, &info, sizeof(info));

    return rc;
}

int Emulator::virt$chdir(FlatPtr path, size_t path_length)
{
    auto host_path = mmu().copy_buffer_from_vm(path, path_length);
    return syscall(SC_chdir, host_path.data(), host_path.size());
}

int Emulator::virt$dup2(int old_fd, int new_fd)
{
    return syscall(SC_dup2, old_fd, new_fd);
}

int Emulator::virt$sched_getparam(pid_t pid, FlatPtr user_addr)
{
    sched_param user_param;
    mmu().copy_from_vm(&user_param, user_addr, sizeof(user_param));
    auto rc = syscall(SC_sched_getparam, pid, &user_param);
    mmu().copy_to_vm(user_addr, &user_param, sizeof(user_param));
    return rc;
}

int Emulator::virt$sched_setparam(int pid, FlatPtr user_addr)
{
    sched_param user_param;
    mmu().copy_from_vm(&user_param, user_addr, sizeof(user_param));
    return syscall(SC_sched_setparam, pid, &user_param);
}

int Emulator::virt$set_thread_name(pid_t pid, FlatPtr name_addr, size_t name_length)
{
    auto user_name = mmu().copy_buffer_from_vm(name_addr, name_length);
    auto name = String::formatted("(UE) {}", StringView { user_name.data(), user_name.size() });
    return syscall(SC_set_thread_name, pid, name.characters(), name.length());
}

pid_t Emulator::virt$setsid()
{
    return syscall(SC_setsid);
}

int Emulator::virt$create_inode_watcher(unsigned flags)
{
    return syscall(SC_create_inode_watcher, flags);
}

int Emulator::virt$inode_watcher_add_watch(FlatPtr params_addr)
{
    Syscall::SC_inode_watcher_add_watch_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));
    return syscall(SC_inode_watcher_add_watch, &params);
}

int Emulator::virt$inode_watcher_remove_watch(int fd, int wd)
{
    return syscall(SC_inode_watcher_add_watch, fd, wd);
}

int Emulator::virt$clock_nanosleep(FlatPtr params_addr)
{
    Syscall::SC_clock_nanosleep_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    timespec requested_sleep;
    mmu().copy_from_vm(&requested_sleep, (FlatPtr)params.requested_sleep, sizeof(timespec));
    params.requested_sleep = &requested_sleep;

    auto remaining_vm_addr = params.remaining_sleep;
    timespec remaining { 0, 0 };
    params.remaining_sleep = &remaining;

    int rc = syscall(SC_clock_nanosleep, &params);
    if (remaining_vm_addr)
        mmu().copy_to_vm((FlatPtr)remaining_vm_addr, &remaining, sizeof(timespec));

    return rc;
}

int Emulator::virt$readlink(FlatPtr params_addr)
{
    Syscall::SC_readlink_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    auto path = mmu().copy_buffer_from_vm((FlatPtr)params.path.characters, params.path.length);
    auto buffer_result = ByteBuffer::create_zeroed(params.buffer.size);
    if (!buffer_result.has_value())
        return -ENOMEM;
    auto& host_buffer = buffer_result.value();

    Syscall::SC_readlink_params host_params;
    host_params.path = { (const char*)path.data(), path.size() };
    host_params.buffer = { (char*)host_buffer.data(), host_buffer.size() };
    int rc = syscall(SC_readlink, &host_params);
    if (rc < 0)
        return rc;
    mmu().copy_to_vm((FlatPtr)params.buffer.data, host_buffer.data(), host_buffer.size());
    return rc;
}

u32 Emulator::virt$allocate_tls(FlatPtr initial_data, size_t size)
{
    // TODO: This matches what Thread::make_thread_specific_region does. The kernel
    // ends up allocating one more page. Figure out if this is intentional.
    auto region_size = align_up_to(size, PAGE_SIZE) + PAGE_SIZE;
    auto tcb_region = make<SimpleRegion>(0x20000000, region_size);

    size_t offset = 0;
    while (size - offset > 0) {
        u8 buffer[512];
        size_t read_bytes = min(sizeof(buffer), size - offset);
        mmu().copy_from_vm(buffer, initial_data + offset, read_bytes);
        memcpy(tcb_region->data() + offset, buffer, read_bytes);
        offset += read_bytes;
    }
    memset(tcb_region->shadow_data(), 0x01, size);

    auto tls_region = make<SimpleRegion>(0, 4);
    tls_region->write32(0, shadow_wrap_as_initialized(tcb_region->base() + (u32)size));
    memset(tls_region->shadow_data(), 0x01, 4);

    u32 tls_base = tcb_region->base();
    mmu().add_region(move(tcb_region));
    mmu().set_tls_region(move(tls_region));
    return tls_base;
}

int Emulator::virt$ptsname(int fd, FlatPtr buffer, size_t buffer_size)
{
    auto pts = mmu().copy_buffer_from_vm(buffer, buffer_size);
    return syscall(SC_ptsname, fd, pts.data(), pts.size());
}

int Emulator::virt$beep()
{
    return syscall(SC_beep);
}

u32 Emulator::virt$sysconf(u32 name)
{
    return syscall(SC_sysconf, name);
}

int Emulator::virt$msyscall(FlatPtr)
{
    // FIXME: Implement this.
    return 0;
}

int Emulator::virt$futex(FlatPtr params_addr)
{
    Syscall::SC_futex_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    // FIXME: Implement this.
    return 0;
}

int Emulator::virt$poll(FlatPtr params_addr)
{
    Syscall::SC_poll_params params;
    mmu().copy_from_vm(&params, params_addr, sizeof(params));

    if (params.nfds >= FD_SETSIZE)
        return EINVAL;

    Vector<pollfd, FD_SETSIZE> fds;
    struct timespec timeout;
    u32 sigmask;

    if (params.fds)
        mmu().copy_from_vm(fds.data(), (FlatPtr)params.fds, sizeof(pollfd) * params.nfds);
    if (params.timeout)
        mmu().copy_from_vm(&timeout, (FlatPtr)params.timeout, sizeof(timeout));
    if (params.sigmask)
        mmu().copy_from_vm(&sigmask, (FlatPtr)params.sigmask, sizeof(sigmask));

    int rc = ppoll(params.fds ? fds.data() : nullptr, params.nfds, params.timeout ? &timeout : nullptr, params.sigmask ? &sigmask : nullptr);
    if (rc < 0)
        return -errno;

    if (params.fds)
        mmu().copy_to_vm((FlatPtr)params.fds, fds.data(), sizeof(pollfd) * params.nfds);
    if (params.timeout)
        mmu().copy_to_vm((FlatPtr)params.timeout, &timeout, sizeof(timeout));

    return rc;
}
}
