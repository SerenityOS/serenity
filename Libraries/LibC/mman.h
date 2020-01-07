#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

#define MAP_FILE 0x00
#define MAP_SHARED 0x01
#define MAP_PRIVATE 0x02
#define MAP_FIXED 0x10
#define MAP_ANONYMOUS 0x20
#define MAP_ANON MAP_ANONYMOUS
#define MAP_STACK 0x40
#define MAP_PURGEABLE 0x80

#define PROT_READ 0x1
#define PROT_WRITE 0x2
#define PROT_EXEC 0x4
#define PROT_NONE 0x0

#define MAP_FAILED ((void*)-1)

#define MADV_SET_VOLATILE 0x100
#define MADV_SET_NONVOLATILE 0x200
#define MADV_GET_VOLATILE 0x400

__BEGIN_DECLS

void* mmap(void* addr, size_t, int prot, int flags, int fd, off_t);
void* mmap_with_name(void* addr, size_t, int prot, int flags, int fd, off_t, const char* name);
int munmap(void*, size_t);
int mprotect(void*, size_t, int prot);
int set_mmap_name(void*, size_t, const char*);
int madvise(void*, size_t, int advice);

__END_DECLS
