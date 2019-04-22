#pragma once

#include <AK/Retainable.h>
#include <AK/Retained.h>
#include <AK/CircularQueue.h>
#include <Kernel/UnixTypes.h>

class ProcessTracer : public Retainable<ProcessTracer> {
public:
    static Retained<ProcessTracer> create(pid_t pid) { return adopt(*new ProcessTracer(pid)); }
    ~ProcessTracer();

    bool is_dead() const { return m_dead; }
    void set_dead() { m_dead = true; }

    bool can_read() const { return !m_calls.is_empty() || m_dead; }
    int read(byte*, int);

    void did_syscall(dword function, dword arg1, dword arg2, dword arg3, dword result);
    pid_t pid() const { return m_pid; }

private:
    explicit ProcessTracer(pid_t);

    struct CallData {
        dword function;
        dword arg1;
        dword arg2;
        dword arg3;
        dword result;
    };

    pid_t m_pid;
    bool m_dead { false };
    CircularQueue<CallData, 200> m_calls;
};
