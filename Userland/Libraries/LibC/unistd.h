/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

/* standard symbolic constants and types
 *
 * values from POSIX standard unix specification
 *
 * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/unistd.h.html
 */

#pragma once

#include <Kernel/API/POSIX/unistd.h>
#include <bits/getopt.h>
#include <fd_set.h>
#include <limits.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#define HZ 1000

/* lseek whence values */
#ifndef _STDIO_H       /* also defined in stdio.h */
#    define SEEK_SET 0 /* from beginning of file.  */
#    define SEEK_CUR 1 /* from current position in file.  */
#    define SEEK_END 2 /* from the end of the file.  */
#endif

extern char** environ;

int get_process_name(char* buffer, int buffer_size);
int set_process_name(char const* name, size_t name_length);
void dump_backtrace(void);
int fsync(int fd);
int gettid(void);
int getpagesize(void);
pid_t fork(void);
pid_t vfork(void);
int daemon(int nochdir, int noclose);
int execv(char const* path, char* const argv[]);
int execve(char const* filename, char* const argv[], char* const envp[]);
int execvpe(char const* filename, char* const argv[], char* const envp[]);
int execvp(char const* filename, char* const argv[]);
int execl(char const* filename, char const* arg, ...);
int execle(char const* filename, char const* arg, ...);
int execlp(char const* filename, char const* arg, ...);
void sync(void);
__attribute__((noreturn)) void _exit(int status);
pid_t getsid(pid_t);
pid_t setsid(void);
int setpgid(pid_t pid, pid_t pgid);
pid_t getpgid(pid_t);
pid_t getpgrp(void);
uid_t geteuid(void);
gid_t getegid(void);
uid_t getuid(void);
gid_t getgid(void);
pid_t getpid(void);
pid_t getppid(void);
int getresuid(uid_t*, uid_t*, uid_t*);
int getresgid(gid_t*, gid_t*, gid_t*);
int getgroups(int size, gid_t list[]);
int setgroups(size_t, gid_t const*);
int seteuid(uid_t);
int setegid(gid_t);
int setuid(uid_t);
int setgid(gid_t);
int setreuid(uid_t, uid_t);
int setresuid(uid_t, uid_t, uid_t);
int setregid(gid_t, gid_t);
int setresgid(gid_t, gid_t, gid_t);
pid_t tcgetpgrp(int fd);
int tcsetpgrp(int fd, pid_t pgid);
ssize_t read(int fd, void* buf, size_t count);
ssize_t pread(int fd, void* buf, size_t count, off_t);
ssize_t write(int fd, void const* buf, size_t count);
ssize_t pwrite(int fd, void const* buf, size_t count, off_t);
int close(int fd);
int chdir(char const* path);
int fchdir(int fd);
char* getcwd(char* buffer, size_t size);
char* getwd(char* buffer);
unsigned int sleep(unsigned int seconds);
int usleep(useconds_t);
int gethostname(char*, size_t);
int sethostname(char const*, ssize_t);
ssize_t readlink(char const* path, char* buffer, size_t);
ssize_t readlinkat(int dirfd, char const* path, char* buffer, size_t);
char* ttyname(int fd);
int ttyname_r(int fd, char* buffer, size_t);
off_t lseek(int fd, off_t, int whence);
int link(char const* oldpath, char const* newpath);
int unlink(char const* pathname);
int unlinkat(int dirfd, char const* pathname, int flags);
int symlink(char const* target, char const* linkpath);
int symlinkat(char const* target, int newdirfd, char const* linkpath);
int rmdir(char const* pathname);
int dup(int old_fd);
int dup2(int old_fd, int new_fd);
int pipe(int pipefd[2]);
int pipe2(int pipefd[2], int flags);
unsigned int alarm(unsigned int seconds);
int access(char const* pathname, int mode);
int faccessat(int dirfd, char const* pathname, int mode, int flags);
int isatty(int fd);
int mknod(char const* pathname, mode_t, dev_t);
int mknodat(int dirfd, char const* pathname, mode_t, dev_t);
long fpathconf(int fd, int name);
long pathconf(char const* path, int name);
char* getlogin(void);
int lchown(char const* pathname, uid_t uid, gid_t gid);
int chown(char const* pathname, uid_t, gid_t);
int fchown(int fd, uid_t, gid_t);
int fchownat(int fd, char const* pathname, uid_t uid, gid_t gid, int flags);
int ftruncate(int fd, off_t length);
int truncate(char const* path, off_t length);
int fsopen(char const* fs_type, int flags);
int fsmount(int vfs_context_id, int mount_fd, int source_fd, char const* target);
int bindmount(int vfs_context_id, int source_fd, char const* target, int flags);
int mount(int source_fd, char const* target, char const* fs_type, int flags);
int umount(char const* mountpoint);
int pledge(char const* promises, char const* execpromises);
int unveil(char const* path, char const* permissions);
char* getpass(char const* prompt);
int pause(void);
int chroot(char const*);
int getdtablesize(void);
int nice(int incr);
int brk(void* addr);
void* sbrk(intptr_t incr);

enum {
    _PC_NAME_MAX,
    _PC_PATH_MAX,
    _PC_PIPE_BUF,
    _PC_VDISABLE,
    _PC_LINK_MAX
};

#define _POSIX_FSYNC 200112L
#define _POSIX_MAPPED_FILES 200112L
#define _POSIX_MEMORY_PROTECTION 200112L
#define _POSIX_MONOTONIC_CLOCK 200112L
#define _POSIX_RAW_SOCKETS 200112L
#define _POSIX_REGEXP 1
#define _POSIX_SAVED_IDS 1
#define _POSIX_SPAWN 200112L
#define _POSIX_THREADS 200112L
#define _POSIX_THREAD_ATTR_STACKADDR 200112L
#define _POSIX_THREAD_ATTR_STACKSIZE 200112L
#define _POSIX_TIMERS 200809L

/*
 * We aren't fully compliant (don't support policies, and don't have a wide
 * range of values), but we do have process priorities.
 */
#define _POSIX_PRIORITY_SCHEDULING
#define _POSIX_VDISABLE '\0'

long sysconf(int name);

__END_DECLS
