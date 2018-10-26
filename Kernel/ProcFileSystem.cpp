#include "ProcFileSystem.h"
#include "Task.h"

static ProcFileSystem* s_the;

ProcFileSystem& ProcFileSystem::the()
{
    ASSERT(s_the);
    return *s_the;
}

RetainPtr<ProcFileSystem> ProcFileSystem::create()
{
    return adopt(*new ProcFileSystem);
}

ProcFileSystem::ProcFileSystem()
{
    s_the = this;
}

ProcFileSystem::~ProcFileSystem()
{
}

void ProcFileSystem::addProcess(Task& task)
{
    ASSERT_INTERRUPTS_DISABLED();
    char buf[16];
    ksprintf(buf, "%d", task.pid());
    auto dir = addFile(createDirectory(buf));
    m_pid2inode.set(task.pid(), dir.index());
    addFile(createGeneratedFile("vm", [&task] {
        InterruptDisabler disabler;
        char* buffer;
        auto stringImpl = StringImpl::createUninitialized(80 + task.regionCount() * 80, buffer);
        memset(buffer, 0, stringImpl->length());
        char* ptr = buffer;
        ptr += ksprintf(ptr, "BEGIN       END         SIZE        NAME\n");
        for (auto& region : task.regions()) {
            ptr += ksprintf(ptr, "%x -- %x    %x    %s\n",
                region->linearAddress.get(),
                region->linearAddress.offset(region->size - 1).get(),
                region->size,
                region->name.characters());
        }
        *ptr = '\0';
        return ByteBuffer::copy((byte*)buffer, ptr - buffer);
    }), dir.index());
}

void ProcFileSystem::removeProcess(Task& task)
{
    ASSERT_INTERRUPTS_DISABLED();
    auto pid = task.pid();
    auto it = m_pid2inode.find(pid);
    ASSERT(it != m_pid2inode.end());
    bool success = removeFile((*it).value);
    ASSERT(success);
    m_pid2inode.remove(pid);
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
        ptr += ksprintf(ptr, "PID    OWNER      STATE  PPID  NSCHED      FDS    NAME\n");
        for (auto* task : tasks) {
            ptr += ksprintf(ptr, "%w   %w:%w  %b     %w  %x    %w   %s\n",
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
