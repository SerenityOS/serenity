#pragma once

#include <signal.h>
#include <stdbool.h>
#include <bits/stdint.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

struct __jmp_buf {
    uint32_t regs[6];
    bool did_save_signal_mask;
    sigset_t saved_signal_mask;
};

typedef struct __jmp_buf jmp_buf[1];
typedef struct __jmp_buf sigjmp_buf[1];

int setjmp(jmp_buf);
void longjmp(jmp_buf, int val);

int sigsetjmp(sigjmp_buf, int savesigs);
void siglongjmp(sigjmp_buf, int val);

__END_DECLS
