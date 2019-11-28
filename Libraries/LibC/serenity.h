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

__END_DECLS
