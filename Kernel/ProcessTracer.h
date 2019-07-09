#pragma once

#include <AK/CircularQueue.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/UnixTypes.h>

class ProcessTracer : public File {
public:
    static NonnullRefPtr<ProcessTracer> create(pid_t pid) { return adopt(*new ProcessTracer(pid)); }
    virtual ~ProcessTracer() override;

    bool is_dead() const { return m_dead; }
    void set_dead() { m_dead = true; }

    virtual bool can_read(FileDescription&) const override { return !m_calls.is_empty() || m_dead; }
    virtual int read(FileDescription&, u8*, int) override;

    virtual bool can_write(FileDescription&) const override { return true; }
    virtual int write(FileDescription&, const u8*, int) override { return -EIO; }

    virtual String absolute_path(const FileDescription&) const override;

    void did_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3, u32 result);
    pid_t pid() const { return m_pid; }

private:
    virtual const char* class_name() const override { return "ProcessTracer"; }
    explicit ProcessTracer(pid_t);

    struct CallData {
        u32 function;
        u32 arg1;
        u32 arg2;
        u32 arg3;
        u32 result;
    };

    pid_t m_pid;
    bool m_dead { false };
    CircularQueue<CallData, 200> m_calls;
};
