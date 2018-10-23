#pragma once

#include "types.h"

extern "C" {

uid_t getuid();
gid_t getgid();
pid_t getpid();
int open(const char* path);
ssize_t read(int fd, void* buf, size_t count);
int close(int fd);

}

