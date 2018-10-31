#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

size_t strlen(const char*);
int strcmp(const char*, const char*);
int memcmp(const void*, const void*, size_t);
void memcpy(void*, const void*, size_t);
const char* strerror(int errnum);

__END_DECLS

