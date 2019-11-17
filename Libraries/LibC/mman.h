#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

#define MAP_SHARED 0x01
#define MAP_PRIVATE 0x02
#define MAP_FIXED 0x10
#define MAP_ANONYMOUS 0x20
#define MAP_ANON MAP_ANONYMOUS
#define MAP_STACK 0x40

#define PROT_READ 0x1
#define PROT_WRITE 0x2
#define PROT_EXEC 0x4
#define PROT_NONE 0x0

#define MAP_FAILED ((void*)-1)

__BEGIN_DECLS

void* mmap(void* addr, size_t, int prot, int flags, int fd, off_t);
void* mmap_with_name(void* addr, size_t, int prot, int flags, int fd, off_t, const char* name);
int munmap(void*, size_t);
int mprotect(void*, size_t, int prot);
int set_mmap_name(void*, size_t, const char*);
int shm_open(const char* name, int flags, mode_t);
int shm_unlink(const char* name);

__END_DECLS
