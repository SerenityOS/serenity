#pragma once

#include <AK/AKString.h>

class CProcess {
public:
    ~CProcess();

    enum Priority {
        Highest,
        Normal,
        Lowest,
    };

    static CProcess* start_detached(const StringView& program, const Vector<StringView>& arguments = Vector<StringView>(), Priority = Priority::Normal);
    void kill();
    void set_priority(Priority priority);

private:
    CProcess(int pid);
    int m_pid = 0;
};
