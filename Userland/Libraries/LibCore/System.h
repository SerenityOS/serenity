/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/StringView.h>
#include <dirent.h>
#include <fcntl.h>
#include <grp.h>
#include <pwd.h>
#include <signal.h>
#include <spawn.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/utsname.h>
#include <sys/wait.h>
#include <termios.h>
#include <time.h>
#include <utime.h>

#if !defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_ANDROID)
#    include <shadow.h>
#endif

namespace Core::System {

#ifdef __serenity__
ErrorOr<void> beep();
ErrorOr<void> pledge(StringView promises, StringView execpromises = {});
ErrorOr<void> unveil(StringView path, StringView permissions);
ErrorOr<void> sendfd(int sockfd, int fd);
ErrorOr<int> recvfd(int sockfd, int options);
ErrorOr<void> ptrace_peekbuf(pid_t tid, void const* tracee_addr, Bytes destination_buf);
ErrorOr<void> setgroups(Span<gid_t const>);
ErrorOr<void> mount(int source_fd, StringView target, StringView fs_type, int flags);
ErrorOr<void> umount(StringView mount_point);
ErrorOr<long> ptrace(int request, pid_t tid, void* address, void* data);
ErrorOr<void> disown(pid_t pid);
ErrorOr<void> profiling_enable(pid_t, u64 event_mask);
ErrorOr<void> profiling_disable(pid_t);
ErrorOr<void> profiling_free_buffer(pid_t);
#else
inline ErrorOr<void> unveil(StringView, StringView)
{
    return {};
}
inline ErrorOr<void> pledge(StringView, StringView = {}) { return {}; }
#endif

template<size_t N>
ALWAYS_INLINE ErrorOr<void> pledge(char const (&promises)[N])
{
    return pledge(StringView { promises, N - 1 });
}

template<size_t NPromises, size_t NExecPromises>
ALWAYS_INLINE ErrorOr<void> pledge(char const (&promises)[NPromises], char const (&execpromises)[NExecPromises])
{
    return pledge(StringView { promises, NPromises - 1 }, StringView { execpromises, NExecPromises - 1 });
}

template<size_t NPath, size_t NPermissions>
ALWAYS_INLINE ErrorOr<void> unveil(char const (&path)[NPath], char const (&permissions)[NPermissions])
{
    return unveil(StringView { path, NPath - 1 }, StringView { permissions, NPermissions - 1 });
}

ALWAYS_INLINE ErrorOr<void> unveil(std::nullptr_t, std::nullptr_t)
{
    return unveil(StringView {}, StringView {});
}

#if !defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_ANDROID)
ErrorOr<Optional<struct spwd>> getspent();
ErrorOr<Optional<struct spwd>> getspnam(StringView name);
#endif

#ifndef AK_OS_MACOS
ErrorOr<int> accept4(int sockfd, struct sockaddr*, socklen_t*, int flags);
#endif

ErrorOr<void> sigaction(int signal, struct sigaction const* action, struct sigaction* old_action);
#if defined(__APPLE__) || defined(__OpenBSD__) || defined(__FreeBSD__)
ErrorOr<sig_t> signal(int signal, sig_t handler);
#else
ErrorOr<sighandler_t> signal(int signal, sighandler_t handler);
#endif
ErrorOr<struct stat> fstat(int fd);
ErrorOr<int> fcntl(int fd, int command, ...);
ErrorOr<void*> mmap(void* address, size_t, int protection, int flags, int fd, off_t, size_t alignment = 0, StringView name = {});
ErrorOr<void> munmap(void* address, size_t);
ErrorOr<int> anon_create(size_t size, int options);
ErrorOr<int> open(StringView path, int options, mode_t mode = 0);
ErrorOr<int> openat(int fd, StringView path, int options, mode_t mode = 0);
ErrorOr<void> close(int fd);
ErrorOr<void> ftruncate(int fd, off_t length);
ErrorOr<struct stat> stat(StringView path);
ErrorOr<struct stat> lstat(StringView path);
ErrorOr<ssize_t> read(int fd, Bytes buffer);
ErrorOr<ssize_t> write(int fd, ReadonlyBytes buffer);
ErrorOr<void> kill(pid_t, int signal);
ErrorOr<void> killpg(int pgrp, int signal);
ErrorOr<int> dup(int source_fd);
ErrorOr<int> dup2(int source_fd, int destination_fd);
ErrorOr<String> ptsname(int fd);
ErrorOr<String> gethostname();
ErrorOr<void> sethostname(StringView);
ErrorOr<String> getcwd();
ErrorOr<void> ioctl(int fd, unsigned request, ...);
ErrorOr<struct termios> tcgetattr(int fd);
ErrorOr<void> tcsetattr(int fd, int optional_actions, struct termios const&);
ErrorOr<int> tcsetpgrp(int fd, pid_t pgrp);
ErrorOr<void> chmod(StringView pathname, mode_t mode);
ErrorOr<void> lchown(StringView pathname, uid_t uid, gid_t gid);
ErrorOr<void> chown(StringView pathname, uid_t uid, gid_t gid);
ErrorOr<Optional<struct passwd>> getpwnam(StringView name);
ErrorOr<Optional<struct group>> getgrnam(StringView name);
ErrorOr<Optional<struct passwd>> getpwuid(uid_t);
ErrorOr<Optional<struct group>> getgrgid(gid_t);
ErrorOr<void> clock_settime(clockid_t clock_id, struct timespec* ts);
ErrorOr<pid_t> posix_spawn(StringView path, posix_spawn_file_actions_t const* file_actions, posix_spawnattr_t const* attr, char* const arguments[], char* const envp[]);
ErrorOr<pid_t> posix_spawnp(StringView path, posix_spawn_file_actions_t* const file_actions, posix_spawnattr_t* const attr, char* const arguments[], char* const envp[]);
ErrorOr<off_t> lseek(int fd, off_t, int whence);
ErrorOr<void> endgrent();

struct WaitPidResult {
    pid_t pid;
    int status;
};
ErrorOr<WaitPidResult> waitpid(pid_t waitee, int options = 0);
ErrorOr<void> setuid(uid_t);
ErrorOr<void> seteuid(uid_t);
ErrorOr<void> setgid(gid_t);
ErrorOr<void> setegid(gid_t);
ErrorOr<void> setpgid(pid_t pid, pid_t pgid);
ErrorOr<pid_t> setsid();
ErrorOr<void> drop_privileges();
ErrorOr<bool> isatty(int fd);
ErrorOr<void> link(StringView old_path, StringView new_path);
ErrorOr<void> symlink(StringView target, StringView link_path);
ErrorOr<void> mkdir(StringView path, mode_t);
ErrorOr<void> chdir(StringView path);
ErrorOr<void> rmdir(StringView path);
ErrorOr<pid_t> fork();
ErrorOr<int> mkstemp(Span<char> pattern);
ErrorOr<void> fchmod(int fd, mode_t mode);
ErrorOr<void> fchown(int fd, uid_t, gid_t);
ErrorOr<void> rename(StringView old_path, StringView new_path);
ErrorOr<void> unlink(StringView path);
ErrorOr<void> utime(StringView path, Optional<struct utimbuf>);
ErrorOr<struct utsname> uname();
ErrorOr<Array<int, 2>> pipe2(int flags);
#ifndef AK_OS_ANDROID
ErrorOr<void> adjtime(const struct timeval* delta, struct timeval* old_delta);
#endif
enum class SearchInPath {
    No,
    Yes,
};
ErrorOr<void> exec(StringView filename, Span<StringView> arguments, SearchInPath, Optional<Span<StringView>> environment = {});

ErrorOr<int> socket(int domain, int type, int protocol);
ErrorOr<void> bind(int sockfd, struct sockaddr const*, socklen_t);
ErrorOr<void> listen(int sockfd, int backlog);
ErrorOr<int> accept(int sockfd, struct sockaddr*, socklen_t*);
ErrorOr<void> connect(int sockfd, struct sockaddr const*, socklen_t);
ErrorOr<void> shutdown(int sockfd, int how);
ErrorOr<ssize_t> send(int sockfd, void const*, size_t, int flags);
ErrorOr<ssize_t> sendmsg(int sockfd, const struct msghdr*, int flags);
ErrorOr<ssize_t> sendto(int sockfd, void const*, size_t, int flags, struct sockaddr const*, socklen_t);
ErrorOr<ssize_t> recv(int sockfd, void*, size_t, int flags);
ErrorOr<ssize_t> recvmsg(int sockfd, struct msghdr*, int flags);
ErrorOr<ssize_t> recvfrom(int sockfd, void*, size_t, int flags, struct sockaddr*, socklen_t*);
ErrorOr<void> getsockopt(int sockfd, int level, int option, void* value, socklen_t* value_size);
ErrorOr<void> setsockopt(int sockfd, int level, int option, void const* value, socklen_t value_size);
ErrorOr<void> getsockname(int sockfd, struct sockaddr*, socklen_t*);
ErrorOr<void> getpeername(int sockfd, struct sockaddr*, socklen_t*);
ErrorOr<void> socketpair(int domain, int type, int protocol, int sv[2]);
ErrorOr<Vector<gid_t>> getgroups();
ErrorOr<void> mknod(StringView pathname, mode_t mode, dev_t dev);
ErrorOr<void> mkfifo(StringView pathname, mode_t mode);
ErrorOr<void> setenv(StringView, StringView, bool);
ErrorOr<int> posix_openpt(int flags);
ErrorOr<void> grantpt(int fildes);
ErrorOr<void> unlockpt(int fildes);
ErrorOr<void> access(StringView pathname, int mode);

}
