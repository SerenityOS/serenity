#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

typedef uint32_t jmp_buf[6];

int setjmp(jmp_buf);
void longjmp(jmp_buf, int val);

__END_DECLS
