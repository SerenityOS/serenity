#pragma once

#include "types.h"

extern "C" {

void* mmap(void*, size_t);
int munmap(void*, size_t);
int set_mmap_name(void*, size_t, const char*);

}
