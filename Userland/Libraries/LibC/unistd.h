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
#include <fd_set.h>
#include <limits.h>

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
int set_process_name(const char* name, size_t name_length);
void dump_backtrace(void);
int fsync(int fd);
int sysbeep(void);
int gettid(void);
int getpagesize(void);
pid_t fork(void);
pid_t vfork(void);
int daemon(int nochdir, int noclose);
int execv(const char* path, char* const argv[]);
int execve(const char* filename, char* const argv[], char* const envp[]);
int execvpe(const char* filename, char* const argv[], char* const envp[]);
int execvp(const char* filename, char* const argv[]);
int execl(const char* filename, const char* arg, ...);
int execle(const char* filename, const char* arg, ...);
int execlp(const char* filename, const char* arg, ...);
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
char* getlogin(void);
int lchown(const char* pathname, uid_t uid, gid_t gid);
int chown(const char* pathname, uid_t, gid_t);
int fchown(int fd, uid_t, gid_t);
int fchownat(int fd, const char* pathname, uid_t uid, gid_t gid, int flags);
int ftruncate(int fd, off_t length);
int truncate(const char* path, off_t length);
int mount(int source_fd, const char* target, const char* fs_type, int flags);
int umount(const char* mountpoint);
int pledge(const char* promises, const char* execpromises);
int unveil(const char* path, const char* permissions);
char* getpass(const char* prompt);
int pause(void);
int chroot(const char*);

enum {
    _PC_NAME_MAX,
    _PC_PATH_MAX,
    _PC_PIPE_BUF,
    _PC_VDISABLE,
    _PC_LINK_MAX
};

#define _POSIX_MONOTONIC_CLOCK 200112L
#define _POSIX_SAVED_IDS
#define _POSIX_TIMERS 200809L

/*
 * We aren't fully compliant (don't support policies, and don't have a wide
 * range of values), but we do have process priorities.
 */
#define _POSIX_PRIORITY_SCHEDULING
#define _POSIX_VDISABLE '\0'

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
