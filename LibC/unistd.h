#pragma once

#include "types.h"

extern "C" {

uid_t getuid();
gid_t getgid();
pid_t getpid();
int open(const char* path);
ssize_t read(int fd, void* buf, size_t count);
int close(int fd);
pid_t waitpid(pid_t);
char* getcwd(char* buffer, size_t size);
int lstat(const char* path, stat* statbuf);
int sleep(unsigned seconds);

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

#define S_ISDIR(m) (((m) & S_IFMT) == S_IFDIR)
#define S_ISCHR(m) (((m) & S_IFMT) == S_IFCHR)
#define S_ISBLK(m) (((m) & S_IFMT) == S_IFBLK)
#define S_ISREG(m) (((m) & S_IFMT) == S_IFREG)
#define S_ISFIFO(m) (((m) & S_IFMT) == S_IFIFO)
#define S_ISLNK(m) (((m) & S_IFMT) == S_IFLNK)

}
