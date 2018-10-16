#pragma once

#include "types.h"

namespace Userspace {

int open(const char* path);
int close(int fd);
int read(int fd, void* outbuf, size_t nread);
int seek(int fd, int offset);
int kill(pid_t pid, int sig);
uid_t getuid();
void sleep(DWORD ticks);

}
