#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>
#include <limits.h>

__BEGIN_DECLS

extern char** environ;

inline int getpagesize() { return 4096; }
pid_t fork();
int execve(const char* filename, const char** argv, const char** envp);
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
int getgroups(int size, gid_t list[]);
int setgroups(size_t, const gid_t*);
pid_t tcgetpgrp(int fd);
int tcsetpgrp(int fd, pid_t pgid);
int open(const char* path, int options, ...);
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
int gethostname(char*, size_t);
ssize_t readlink(const char* path, char* buffer, size_t);
char* ttyname(int fd);
int ttyname_r(int fd, char* buffer, size_t);
off_t lseek(int fd, off_t, int whence);
int link(const char* oldpath, const char* newpath);
int unlink(const char* pathname);
int getdtablesize();
int dup(int old_fd);
int dup2(int old_fd, int new_fd);
int pipe(int pipefd[2]);
unsigned int alarm(unsigned int seconds);
int access(const char* pathname, int mode);
int isatty(int fd);
int mknod(const char* pathname, mode_t, dev_t);
long fpathconf(int fd, int name);
long pathconf(const char *path, int name);

enum {
    _PC_NAME_MAX,
};

#define WEXITSTATUS(status) (((status) & 0xff00) >> 8)
#define WTERMSIG(status) ((status) & 0x7f)
#define WIFEXITED(status) (WTERMSIG(status) == 0)
#define WIFSIGNALED(status) (((char) (((status) & 0x7f) + 1) >> 1) > 0)

#define HOST_NAME_MAX 64

#define	S_IFMT 0170000
#define	S_IFDIR 0040000
#define	S_IFCHR 0020000
#define	S_IFBLK 0060000
#define	S_IFREG 0100000
#define	S_IFIFO 0010000
#define	S_IFLNK 0120000
#define	S_IFSOCK 0140000

#define S_ISUID 04000
#define S_ISGID 02000
#define S_ISVTX 01000
#define S_IRUSR 0400
#define S_IWUSR 0200
#define S_IXUSR 0100
#define S_IRGRP 0040
#define S_IWGRP 0020
#define S_IXGRP 0010
#define S_IROTH 0004
#define S_IWOTH 0002
#define S_IXOTH 0001

#define S_IRWXG (S_IRWXU >> 3)
#define S_IRWXO (S_IRWXG >> 3)

#define S_ISDIR(m) (((m) & S_IFMT) == S_IFDIR)
#define S_ISCHR(m) (((m) & S_IFMT) == S_IFCHR)
#define S_ISBLK(m) (((m) & S_IFMT) == S_IFBLK)
#define S_ISREG(m) (((m) & S_IFMT) == S_IFREG)
#define S_ISFIFO(m) (((m) & S_IFMT) == S_IFIFO)
#define S_ISLNK(m) (((m) & S_IFMT) == S_IFLNK)

__END_DECLS

