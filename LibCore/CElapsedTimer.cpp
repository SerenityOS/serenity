#include <AK/Assertions.h>
#include <AK/Time.h>
#include <LibCore/CElapsedTimer.h>
#include <sys/time.h>

void CElapsedTimer::start()
{
    m_valid = true;
    gettimeofday(&m_start_time, nullptr);
}

int CElapsedTimer::elapsed() const
{
    ASSERT(is_valid());
    struct timeval now;
    gettimeofday(&now, nullptr);
    struct timeval diff;
    timeval_sub(now, m_start_time, diff);
    return diff.tv_sec * 1000 + diff.tv_usec / 1000;
}
