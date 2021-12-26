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

extern char** environ;

void dump_backtrace();
int fsync(int fd);
void sysbeep();
int systrace(pid_t);
int gettid();
int donate(int tid);
int create_thread(int (*)(void*), void*);
void exit_thread(int);
int create_shared_buffer(int, void** buffer);
int share_buffer_with(int, pid_t peer_pid);
void* get_shared_buffer(int shared_buffer_id);
int release_shared_buffer(int shared_buffer_id);
int seal_shared_buffer(int shared_buffer_id);
int get_shared_buffer_size(int shared_buffer_id);
int read_tsc(unsigned* lsw, unsigned* msw);
inline int getpagesize() { return 4096; }
pid_t fork();
int execv(const char* path, char* const argv[]);
int execve(const char* filename, char* const argv[], char* const envp[]);
int execvpe(const char* filename, char* const argv[], char* const envp[]);
int execvp(const char* filename, char* const argv[]);
int execl(const char* filename, const char* arg, ...);
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
int creat(const char* path, mode_t);
int open(const char* path, int options, ...);
int creat_with_path_length(const char* path, size_t path_length, mode_t);
int open_with_path_length(const char* path, size_t path_length, int options, mode_t);
ssize_t read(int fd, void* buf, size_t count);
ssize_t write(int fd, const void* buf, size_t count);
int close(int fd);
pid_t waitpid(pid_t, int* wstatus, int options);
int chdir(const char* path);
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

enum {
    _PC_NAME_MAX,
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

__END_DECLS
