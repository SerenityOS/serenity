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
        auto stringImpl = StringImpl::createUninitialized(tasks.size() * 256, buffer);
        memset(buffer, 0, stringImpl->length());
        char* ptr = buffer;
        ptr += ksprintf(ptr, "PID    OWNER      STATE  PPID  NSCHED  FDS    NAME\n");
        for (auto* task : tasks) {
            ptr += ksprintf(ptr, "%w   %w:%w  %b     %w  %w    %w   %s\n",
                task->pid(),
                task->uid(),
                task->gid(),
                task->state(),
                task->parentPID(),
                task->timesScheduled(),
                task->fileHandleCount(),
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
