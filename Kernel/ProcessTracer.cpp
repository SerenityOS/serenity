#include <AK/kstdio.h>
#include <Kernel/ProcessTracer.h>

ProcessTracer::ProcessTracer(pid_t pid)
    : m_pid(pid)
{
}

ProcessTracer::~ProcessTracer()
{
}

void ProcessTracer::did_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3, u32 result)
{
    CallData data = { function, arg1, arg2, arg3, result };
    m_calls.enqueue(data);
}

int ProcessTracer::read(FileDescription&, u8* buffer, int buffer_size)
{
    if (m_calls.is_empty())
        return 0;
    auto data = m_calls.dequeue();
    // FIXME: This should not be an assertion.
    ASSERT(buffer_size == sizeof(data));
    memcpy(buffer, &data, sizeof(data));
    return sizeof(data);
}

String ProcessTracer::absolute_path(const FileDescription&) const
{
    return String::format("tracer:%d", m_pid);
}
