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

#include <fd_set.h>
#include <limits.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define HZ 1000
#define STDIN_FILENO 0
#define STDOUT_FILENO 1
#define STDERR_FILENO 2

/* lseek whence values */
#ifndef _STDIO_H       /* also defined in stdio.h */
#    define SEEK_SET 0 /* from beginning of file.  */
#    define SEEK_CUR 1 /* from current position in file.  */
#    define SEEK_END 2 /* from the end of the file.  */
#endif

extern char** environ;

int get_process_name(char* buffer, int buffer_size);
int set_process_name(const char* name, size_t name_length);
void dump_backtrace();
int fsync(int fd);
void sysbeep();
int gettid();
int donate(int tid);
int getpagesize();
pid_t fork();
pid_t vfork();
int execv(const char* path, char* const argv[]);
int execve(const char* filename, char* const argv[], char* const envp[]);
int execvpe(const char* filename, char* const argv[], char* const envp[]);
int execvp(const char* filename, char* const argv[]);
int execl(const char* filename, const char* arg, ...);
int execle(const char* filename, const char* arg, ...);
int execlp(const char* filename, const char* arg, ...);
int chroot(const char* path);
int chroot_with_mount_flags(const char* path, int mount_flags);
void sync();
__attribute__((noreturn)) void _exit(int status);
pid_t getsid(pid_t);
pid_t setsid();
int setpgid(pid_t pid, pid_t pgid);
pid_t getpgid(pid_t);
pid_t getpgrp();
uid_t geteuid();
gid_t getegid();
uid_t getuid();
gid_t getgid();
pid_t getpid();
pid_t getppid();
int getresuid(uid_t*, uid_t*, uid_t*);
int getresgid(gid_t*, gid_t*, gid_t*);
int getgroups(int size, gid_t list[]);
int setgroups(size_t, const gid_t*);
int seteuid(uid_t);
int setegid(gid_t);
int setuid(uid_t);
int setgid(gid_t);
int setreuid(uid_t, uid_t);
int setresuid(uid_t, uid_t, uid_t);
int setresgid(gid_t, gid_t, gid_t);
pid_t tcgetpgrp(int fd);
int tcsetpgrp(int fd, pid_t pgid);
ssize_t read(int fd, void* buf, size_t count);
ssize_t pread(int fd, void* buf, size_t count, off_t);
ssize_t write(int fd, const void* buf, size_t count);
ssize_t pwrite(int fd, const void* buf, size_t count, off_t);
int close(int fd);
int chdir(const char* path);
int fchdir(int fd);
char* getcwd(char* buffer, size_t size);
char* getwd(char* buffer);
unsigned int sleep(unsigned int seconds);
int usleep(useconds_t);
int gethostname(char*, size_t);
int sethostname(const char*, ssize_t);
ssize_t readlink(const char* path, char* buffer, size_t);
char* ttyname(int fd);
int ttyname_r(int fd, char* buffer, size_t);
off_t lseek(int fd, off_t, int whence);
int link(const char* oldpath, const char* newpath);
int unlink(const char* pathname);
int symlink(const char* target, const char* linkpath);
int rmdir(const char* pathname);
int dup(int old_fd);
int dup2(int old_fd, int new_fd);
int pipe(int pipefd[2]);
int pipe2(int pipefd[2], int flags);
unsigned int alarm(unsigned int seconds);
int access(const char* pathname, int mode);
int isatty(int fd);
int mknod(const char* pathname, mode_t, dev_t);
long fpathconf(int fd, int name);
long pathconf(const char* path, int name);
char* getlogin();
int chown(const char* pathname, uid_t, gid_t);
int fchown(int fd, uid_t, gid_t);
int ftruncate(int fd, off_t length);
int truncate(const char* path, off_t length);
int halt();
int reboot();
int mount(int source_fd, const char* target, const char* fs_type, int flags);
int umount(const char* mountpoint);
int pledge(const char* promises, const char* execpromises);
int unveil(const char* path, const char* permissions);
char* getpass(const char* prompt);

enum {
    _PC_NAME_MAX,
    _PC_PATH_MAX,
    _PC_PIPE_BUF,
    _PC_VDISABLE
};

#define R_OK 4
#define W_OK 2
#define X_OK 1
#define F_OK 0

#define MS_NODEV (1 << 0)
#define MS_NOEXEC (1 << 1)
#define MS_NOSUID (1 << 2)
#define MS_BIND (1 << 3)
#define MS_RDONLY (1 << 4)
#define MS_REMOUNT (1 << 5)

#define _POSIX_MONOTONIC_CLOCK 200112L
#define _POSIX_SAVED_IDS
#define _POSIX_TIMERS 200809L

/*
 * We aren't fully compliant (don't support policies, and don't have a wide
 * range of values), but we do have process priorities.
 */
#define _POSIX_PRIORITY_SCHEDULING
#define _POSIX_VDISABLE '\0'

enum {
    _SC_MONOTONIC_CLOCK,
    _SC_NPROCESSORS_CONF,
    _SC_NPROCESSORS_ONLN,
    _SC_OPEN_MAX,
    _SC_TTY_NAME_MAX,
    _SC_PAGESIZE,
    _SC_GETPW_R_SIZE_MAX,
    _SC_CLK_TCK,
};

#define _SC_MONOTONIC_CLOCK _SC_MONOTONIC_CLOCK
#define _SC_NPROCESSORS_CONF _SC_NPROCESSORS_CONF
#define _SC_NPROCESSORS_ONLN _SC_NPROCESSORS_ONLN
#define _SC_OPEN_MAX _SC_OPEN_MAX
#define _SC_PAGESIZE _SC_PAGESIZE
#define _SC_TTY_NAME_MAX _SC_TTY_NAME_MAX
#define _SC_GETPW_R_SIZE_MAX _SC_GETPW_R_SIZE_MAX
#define _SC_CLK_TCK _SC_CLK_TCK

long sysconf(int name);

// If opterr is set (the default), print error messages to stderr.
extern int opterr;
// On errors, optopt is set to the erroneous *character*.
extern int optopt;
// Index of the next argument to process upon a getopt*() call.
extern int optind;
// If set, reset the internal state kept by getopt*(). You may also want to set
// optind to 1 in that case. Alternatively, setting optind to 0 is treated like
// doing both of the above.
extern int optreset;
// After parsing an option that accept an argument, set to point to the argument
// value.
extern char* optarg;

int getopt(int argc, char* const* argv, const char* short_options);

__END_DECLS
