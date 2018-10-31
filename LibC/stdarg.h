#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

typedef char* va_list;

#define va_start(ap, v) ap = (va_list)&v + sizeof(v)
#define va_arg(ap, t) ((t*)(ap += sizeof(t)))[-1]
#define va_end(ap) ap = nullptr

__END_DECLS

