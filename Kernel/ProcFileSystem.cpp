#include "ProcFileSystem.h"
#include "Task.h"

RetainPtr<ProcFileSystem> ProcFileSystem::create()
{
    return adopt(*new ProcFileSystem);
}

ProcFileSystem::ProcFileSystem()
{
}

ProcFileSystem::~ProcFileSystem()
{
}

bool ProcFileSystem::initialize()
{
    SyntheticFileSystem::initialize();
    addFile(createGeneratedFile("summary", [] {
        InterruptDisabler disabler;
        auto tasks = Task::allTasks();
        char* buffer;
        auto stringImpl = StringImpl::createUninitialized(tasks.size() * 128, buffer);
        memset(buffer, 0, stringImpl->length());
        char* ptr = buffer;
        ptr += ksprintf(ptr, "PID    OWNER      STATE  NSCHED  NAME\n");
        for (auto* task : tasks) {
            ptr += ksprintf(ptr, "%w   %w:%w  %b     %w    %s\n",
                task->pid(),
                task->uid(),
                task->gid(),
                task->state(),
                task->timesScheduled(),
                task->name().characters());
        }
        ptr += ksprintf(ptr, "kmalloc: alloc: %u / free: %u\n", sum_alloc, sum_free);
        *ptr = '\0';
        return ByteBuffer::copy((byte*)buffer, ptr - buffer);
    }));
    return true;
}

const char* ProcFileSystem::className() const
{
    return "procfs";
}
