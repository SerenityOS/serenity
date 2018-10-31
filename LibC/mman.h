#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

void* mmap(void*, size_t);
int munmap(void*, size_t);
int set_mmap_name(void*, size_t, const char*);

__END_DECLS

