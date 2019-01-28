#pragma once

#include <stdio.h>
#include <unistd.h>

#ifdef __cplusplus

struct Stopwatch {
public:
    Stopwatch(const char* name)
        : m_name(name)
    {
        read_tsc(&m_start_lsw, &m_start_msw);
    }

    ~Stopwatch()
    {
        unsigned end_lsw;
        unsigned end_msw;
        read_tsc(&end_lsw, &end_msw);
        if (m_start_msw != end_msw) {
            dbgprintf("Stopwatch: differing msw, no result for %s\n", m_name);
        }
        unsigned diff = end_lsw - m_start_lsw;
        dbgprintf("Stopwatch(%s): %u ticks\n", m_name, diff);
    }

private:
    const char* m_name { nullptr };
    unsigned m_start_lsw { 0 };
    unsigned m_start_msw { 0 };
};

#endif // __cplusplus
