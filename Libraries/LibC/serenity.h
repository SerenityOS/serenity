#pragma once

#include <stdio.h>
#include <unistd.h>

#ifdef __cplusplus

struct Stopwatch {
    union SplitQword {
        struct {
            uint32_t lsw;
            uint32_t msw;
        };
        uint64_t qw { 0 };
    };

public:
    Stopwatch(const char* name)
        : m_name(name)
    {
        read_tsc(&m_start.lsw, &m_start.msw);
    }

    ~Stopwatch()
    {
        SplitQword end;
        read_tsc(&end.lsw, &end.msw);
        uint64_t diff = end.qw - m_start.qw;
        dbgprintf("Stopwatch(%s): %Q ticks\n", m_name, diff);
    }

private:
    const char* m_name { nullptr };
    SplitQword m_start;
};

#endif // __cplusplus

__BEGIN_DECLS

int module_load(const char* path, size_t path_length);
int module_unload(const char* name, size_t name_length);

int profiling_enable(pid_t);
int profiling_disable(pid_t);

#define THREAD_PRIORITY_MIN 1
#define THREAD_PRIORITY_LOW 10
#define THREAD_PRIORITY_NORMAL 30
#define THREAD_PRIORITY_HIGH 50
#define THREAD_PRIORITY_MAX 99

#define FUTEX_WAIT 1
#define FUTEX_WAKE 2

int futex(int32_t* userspace_address, int futex_op, int32_t value, const struct timespec* timeout);

__END_DECLS
