#pragma once

#include <time.h>

class GElapsedTimer {
public:
    GElapsedTimer() { }

    void start();
    int elapsed() const;

private:
    struct timeval m_start_time { 0, 0 };
};
