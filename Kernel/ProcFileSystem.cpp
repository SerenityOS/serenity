#include "ProcFileSystem.h"
#include "Task.h"
#include <VirtualFileSystem/VirtualFileSystem.h>
#include "system.h"
#include "MemoryManager.h"

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

ByteBuffer procfs$pid_vm(const Task& task)
{
    InterruptDisabler disabler;
    char* buffer;
    auto stringImpl = StringImpl::createUninitialized(80 + task.regionCount() * 80 + 80 + task.subregionCount() * 80, buffer);
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
    if (task.subregionCount()) {
        ptr += ksprintf(ptr, "\nREGION    OFFSET    BEGIN       END         SIZE        NAME\n");
        for (auto& subregion : task.subregions()) {
            ptr += ksprintf(ptr, "%x  %x  %x -- %x    %x    %s\n",
                subregion->region->linearAddress.get(),
                subregion->offset,
                subregion->linearAddress.get(),
                subregion->linearAddress.offset(subregion->size - 1).get(),
                subregion->size,
                subregion->name.characters());
        }
    }
    *ptr = '\0';
    return ByteBuffer::copy((byte*)buffer, ptr - buffer);
}

ByteBuffer procfs$pid_stack(Task& task)
{
    InterruptDisabler disabler;
    if (current != &task) {
        MM.unmapRegionsForTask(*current);
        MM.mapRegionsForTask(task);
    }
    struct RecognizedSymbol {
        dword address;
        const KSym* ksym;
    };
    Vector<RecognizedSymbol> recognizedSymbols;
    if (auto* eipKsym = ksymbolicate(task.tss().eip))
        recognizedSymbols.append({ task.tss().eip, eipKsym });
    for (dword* stackPtr = (dword*)task.framePtr(); task.isValidAddressForKernel(LinearAddress((dword)stackPtr)); stackPtr = (dword*)*stackPtr) {
        dword retaddr = stackPtr[1];
        if (auto* ksym = ksymbolicate(retaddr))
            recognizedSymbols.append({ retaddr, ksym });
    }
    size_t bytesNeeded = 0;
    for (auto& symbol : recognizedSymbols) {
        bytesNeeded += symbol.ksym->name.length() + 8 + 16;
    }
    auto buffer = ByteBuffer::createUninitialized(bytesNeeded);
    char* bufptr = (char*)buffer.pointer();

    for (auto& symbol : recognizedSymbols) {
        // FIXME: This doesn't actually create a file!
        unsigned offset = symbol.address - symbol.ksym->address;
        bufptr += ksprintf(bufptr, "%p  %s +%u\n", symbol.address, symbol.ksym->name.characters(), offset);
    }
    buffer.trim(bufptr - (char*)buffer.pointer());
    if (current != &task) {
        MM.unmapRegionsForTask(task);
        MM.mapRegionsForTask(*current);
    }
    return buffer;
}

ByteBuffer procfs$pid_exe(Task& task)
{
    InodeIdentifier inode;
    {
        InterruptDisabler disabler;
        inode = task.executableInode();
    }
    return VirtualFileSystem::the().absolutePath(inode).toByteBuffer();
}

void ProcFileSystem::addProcess(Task& task)
{
    ASSERT_INTERRUPTS_DISABLED();
    char buf[16];
    ksprintf(buf, "%d", task.pid());
    auto dir = addFile(createDirectory(buf));
    m_pid2inode.set(task.pid(), dir.index());
    addFile(createGeneratedFile("vm", [&task] { return procfs$pid_vm(task); }), dir.index());
    addFile(createGeneratedFile("stack", [&task] { return procfs$pid_stack(task); }), dir.index());
    if (task.executableInode().isValid())
        addFile(createGeneratedFile("exe", [&task] { return procfs$pid_exe(task); }, 00120777), dir.index());
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

ByteBuffer procfs$mm()
{
    InterruptDisabler disabler;
    size_t zonePageCount = 0;
    for (auto* zone : MM.m_zones)
        zonePageCount += zone->m_pages.size();
    auto buffer = ByteBuffer::createUninitialized(1024 + 80 * MM.m_zones.size() + zonePageCount * 10);
    char* ptr = (char*)buffer.pointer();
    ptr += ksprintf(ptr, "Zone count: %u\n", MM.m_zones.size());
    ptr += ksprintf(ptr, "Free physical pages: %u\n", MM.m_freePages.size());
    for (auto* zone : MM.m_zones) {
        ptr += ksprintf(ptr, "Zone %p size: %u\n  ", zone, zone->size());
        for (auto page : zone->m_pages)
            ptr += ksprintf(ptr, "%x ", page);
        ptr += ksprintf(ptr, "\n");
    }
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}


ByteBuffer procfs$mounts()
{
    InterruptDisabler disabler;
    auto buffer = ByteBuffer::createUninitialized(VirtualFileSystem::the().mountCount() * 80);
    char* ptr = (char*)buffer.pointer();
    VirtualFileSystem::the().forEachMount([&ptr] (auto& mount) {
        auto& fs = mount.fileSystem();
        ptr += ksprintf(ptr, "%s @ ", fs.className());
        if (!mount.host().isValid())
            ptr += ksprintf(ptr, "/\n", fs.className());
        else
            ptr += ksprintf(ptr, "%u:%u\n", mount.host().fileSystemID(), mount.host().index());
    });
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

ByteBuffer procfs$kmalloc()
{
    InterruptDisabler disabler;
    auto buffer = ByteBuffer::createUninitialized(128);
    char* ptr = (char*)buffer.pointer();
    ptr += ksprintf(ptr, "alloc: %u\nfree:  %u\n", sum_alloc, sum_free);
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

static const char* toString(Task::State state)
{
    switch (state) {
    case Task::Invalid: return "Invalid";
    case Task::Runnable: return "Runnable";
    case Task::Running: return "Running";
    case Task::Terminated: return "Term";
    case Task::Crashing: return "Crash";
    case Task::Exiting: return "Exit";
    case Task::BlockedSleep: return "Sleep";
    case Task::BlockedWait: return "Wait";
    case Task::BlockedRead: return "Read";
    }
    ASSERT_NOT_REACHED();
    return nullptr;
}

ByteBuffer procfs$summary()
{
    InterruptDisabler disabler;
    auto tasks = Task::allTasks();
    auto buffer = ByteBuffer::createUninitialized(tasks.size() * 256);
    char* ptr = (char*)buffer.pointer();
    ptr += ksprintf(ptr, "PID    OWNER  STATE      PPID   NSCHED      FDS  NAME\n");
    for (auto* task : tasks) {
        ptr += ksprintf(ptr, "% 5u  % 4u   % 8s   % 5u  % 10u  % 3u  %s\n",
            task->pid(),
            task->uid(),
            toString(task->state()),
            task->parentPID(),
            task->timesScheduled(),
            task->fileHandleCount(),
            task->name().characters());
    }
    *ptr = '\0';
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

bool ProcFileSystem::initialize()
{
    SyntheticFileSystem::initialize();
    addFile(createGeneratedFile("mm", procfs$mm));
    addFile(createGeneratedFile("mounts", procfs$mounts));
    addFile(createGeneratedFile("kmalloc", procfs$kmalloc));
    addFile(createGeneratedFile("summary", procfs$summary));
    return true;
}

const char* ProcFileSystem::className() const
{
    return "procfs";
}
