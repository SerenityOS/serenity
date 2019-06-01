#pragma once

#include <AK/CircularQueue.h>
#include <Kernel/File.h>
#include <Kernel/UnixTypes.h>

class ProcessTracer : public File {
public:
    static Retained<ProcessTracer> create(pid_t pid) { return adopt(*new ProcessTracer(pid)); }
    virtual ~ProcessTracer() override;

    bool is_dead() const { return m_dead; }
    void set_dead() { m_dead = true; }

    virtual bool can_read(FileDescriptor&) const override { return !m_calls.is_empty() || m_dead; }
    virtual int read(FileDescriptor&, byte*, int) override;

    virtual bool can_write(FileDescriptor&) const override { return true; }
    virtual int write(FileDescriptor&, const byte*, int) override { return -EIO; }

    virtual String absolute_path(const FileDescriptor&) const override;

    void did_syscall(dword function, dword arg1, dword arg2, dword arg3, dword result);
    pid_t pid() const { return m_pid; }

private:
    virtual const char* class_name() const override { return "ProcessTracer"; }
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
