#pragma once

#include <sys/time.h>

class CElapsedTimer {
public:
    CElapsedTimer() { }

    bool is_valid() const { return m_valid; }
    void start();
    int elapsed() const;

private:
    bool m_valid { false };
    struct timeval m_start_time { 0, 0 };
};
