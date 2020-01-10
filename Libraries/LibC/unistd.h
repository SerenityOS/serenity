/* standard symbolic constants and types
 *
 * values from POSIX standard unix specification
 *
 * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/unistd.h.html
 */

#pragma once

#include <errno.h>
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
void dump_backtrace();
int fsync(int fd);
void sysbeep();
int systrace(pid_t);
int gettid();
int donate(int tid);
int create_shared_buffer(int, void** buffer);
int share_buffer_with(int, pid_t peer_pid);
int share_buffer_globally(int);
void* get_shared_buffer(int shared_buffer_id);
int release_shared_buffer(int shared_buffer_id);
int seal_shared_buffer(int shared_buffer_id);
int get_shared_buffer_size(int shared_buffer_id);
int set_process_icon(int icon_id);
inline int getpagesize() { return 4096; }
pid_t fork();
int execv(const char* path, char* const argv[]);
int execve(const char* filename, char* const argv[], char* const envp[]);
int execvpe(const char* filename, char* const argv[], char* const envp[]);
int execvp(const char* filename, char* const argv[]);
int execl(const char* filename, const char* arg, ...);
int execlp(const char* filename, const char* arg, ...);
int chroot(const char* path);
void sync();
void _exit(int status);
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
int getgroups(int size, gid_t list[]);
int setgroups(size_t, const gid_t*);
int setuid(uid_t);
int setgid(gid_t);
pid_t tcgetpgrp(int fd);
int tcsetpgrp(int fd, pid_t pgid);
ssize_t read(int fd, void* buf, size_t count);
ssize_t write(int fd, const void* buf, size_t count);
int close(int fd);
pid_t waitpid(pid_t, int* wstatus, int options);
pid_t wait(int* wstatus);
int chdir(const char* path);
int fchdir(int fd);
char* getcwd(char* buffer, size_t size);
char* getwd(char* buffer);
int fstat(int fd, struct stat* statbuf);
int lstat(const char* path, struct stat* statbuf);
int stat(const char* path, struct stat* statbuf);
int sleep(unsigned seconds);
int usleep(useconds_t);
int gethostname(char*, size_t);
ssize_t readlink(const char* path, char* buffer, size_t);
char* ttyname(int fd);
int ttyname_r(int fd, char* buffer, size_t);
off_t lseek(int fd, off_t, int whence);
int link(const char* oldpath, const char* newpath);
int unlink(const char* pathname);
int symlink(const char* target, const char* linkpath);
int rmdir(const char* pathname);
int getdtablesize();
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
int halt();
int reboot();
int mount(const char* device, const char* mountpoint, const char* fstype);
int umount(const char* mountpoint);

enum {
    _PC_NAME_MAX,
    _PC_PATH_MAX,
    _PC_PIPE_BUF,
    _PC_VDISABLE
};

#define HOST_NAME_MAX 64

#define R_OK 4
#define W_OK 2
#define X_OK 1
#define F_OK 0

/*
 * We aren't fully compliant (don't support policies, and don't have a wide
 * range of values), but we do have process priorities.
 */
#define _POSIX_PRIORITY_SCHEDULING
#define _POSIX_VDISABLE '\0'

__END_DECLS
