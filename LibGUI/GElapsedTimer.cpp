#include <LibGUI/GElapsedTimer.h>
#include <sys/time.h>

void GElapsedTimer::start()
{
    gettimeofday(&m_start_time, nullptr);
}

inline void timersub(const struct timeval* a, const struct timeval* b, struct timeval* result)
{
    result->tv_sec = a->tv_sec - b->tv_sec;
    result->tv_usec = a->tv_usec - b->tv_usec;
    if (result->tv_usec < 0) {
        --result->tv_sec;
        result->tv_usec += 1000000;
    }
}

int GElapsedTimer::elapsed() const
{
    struct timeval now;
    gettimeofday(&now, nullptr);
    struct timeval diff;
    timersub(&now, &m_start_time, &diff);
    return diff.tv_sec * 1000 + diff.tv_usec / 1000;
}
