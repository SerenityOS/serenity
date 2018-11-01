#include "ProcFileSystem.h"
#include "Process.h"
#include <VirtualFileSystem/VirtualFileSystem.h>
#include "system.h"
#include "MemoryManager.h"
#include "StdLib.h"

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

ByteBuffer procfs$pid_fds(Process& process)
{
    ProcessInspectionScope scope(process);
    char* buffer;
    auto stringImpl = StringImpl::createUninitialized(process.number_of_open_file_descriptors() * 80, buffer);
    memset(buffer, 0, stringImpl->length());
    char* ptr = buffer;
    for (size_t i = 0; i < process.max_open_file_descriptors(); ++i) {
        auto* handle = process.file_descriptor(i);
        if (!handle)
            continue;
        ptr += ksprintf(ptr, "% 3u %s\n", i, handle->absolute_path().characters());
    }
    *ptr = '\0';
    return ByteBuffer::copy((byte*)buffer, ptr - buffer);
}

ByteBuffer procfs$pid_vm(Process& process)
{
    ProcessInspectionScope scope(process);
    char* buffer;
    auto stringImpl = StringImpl::createUninitialized(80 + process.regionCount() * 80 + 80 + process.subregionCount() * 80, buffer);
    memset(buffer, 0, stringImpl->length());
    char* ptr = buffer;
    ptr += ksprintf(ptr, "BEGIN       END         SIZE        NAME\n");
    for (auto& region : process.regions()) {
        ptr += ksprintf(ptr, "%x -- %x    %x    %s\n",
            region->linearAddress.get(),
            region->linearAddress.offset(region->size - 1).get(),
            region->size,
            region->name.characters());
    }
    if (process.subregionCount()) {
        ptr += ksprintf(ptr, "\nREGION    OFFSET    BEGIN       END         SIZE        NAME\n");
        for (auto& subregion : process.subregions()) {
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

ByteBuffer procfs$pid_stack(Process& process)
{
    ProcessInspectionScope scope(process);
    OtherProcessPagingScope pagingScope(process);
    struct RecognizedSymbol {
        dword address;
        const KSym* ksym;
    };
    Vector<RecognizedSymbol> recognizedSymbols;
    if (auto* eipKsym = ksymbolicate(process.tss().eip))
        recognizedSymbols.append({ process.tss().eip, eipKsym });
    for (dword* stackPtr = (dword*)process.framePtr(); process.isValidAddressForKernel(LinearAddress((dword)stackPtr)); stackPtr = (dword*)*stackPtr) {
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
    return buffer;
}

ByteBuffer procfs$pid_exe(Process& process)
{
    ProcessInspectionScope scope(process);
    auto inode = process.executableInode();
    return VirtualFileSystem::the().absolutePath(inode).toByteBuffer();
}

void ProcFileSystem::addProcess(Process& process)
{
    InterruptDisabler disabler;
    char buf[16];
    ksprintf(buf, "%d", process.pid());
    auto dir = addFile(createDirectory(buf));
    m_pid2inode.set(process.pid(), dir.index());
    addFile(createGeneratedFile("vm", [&process] { return procfs$pid_vm(process); }), dir.index());
    addFile(createGeneratedFile("stack", [&process] { return procfs$pid_stack(process); }), dir.index());
    addFile(createGeneratedFile("fds", [&process] { return procfs$pid_fds(process); }), dir.index());
    if (process.executableInode().isValid())
        addFile(createGeneratedFile("exe", [&process] { return procfs$pid_exe(process); }, 00120777), dir.index());
}

void ProcFileSystem::removeProcess(Process& process)
{
    InterruptDisabler disabler;
    auto pid = process.pid();
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
    auto buffer = ByteBuffer::createUninitialized(256);
    char* ptr = (char*)buffer.pointer();
    ptr += ksprintf(ptr, "eternal:      %u\npage-aligned: %u\nallocated:    %u\nfree:         %u\n", kmalloc_sum_eternal, sum_alloc, sum_free);
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

static const char* toString(Process::State state)
{
    switch (state) {
    case Process::Invalid: return "Invalid";
    case Process::Runnable: return "Runnable";
    case Process::Running: return "Running";
    case Process::Terminated: return "Term";
    case Process::Crashing: return "Crash";
    case Process::Exiting: return "Exit";
    case Process::BlockedSleep: return "Sleep";
    case Process::BlockedWait: return "Wait";
    case Process::BlockedRead: return "Read";
    case Process::BeingInspected: return "Inspect";
    }
    ASSERT_NOT_REACHED();
    return nullptr;
}

ByteBuffer procfs$summary()
{
    InterruptDisabler disabler;
    auto processes = Process::allProcesses();
    auto buffer = ByteBuffer::createUninitialized(processes.size() * 256);
    char* ptr = (char*)buffer.pointer();
    ptr += ksprintf(ptr, "PID    OWNER  STATE      PPID   NSCHED      FDS  TTY  NAME\n");
    for (auto* process : processes) {
        ptr += ksprintf(ptr, "% 5u  % 4u   % 8s   % 5u  % 10u  % 3u  % 4s  %s\n",
            process->pid(),
            process->uid(),
            toString(process->state()),
            process->parentPID(),
            process->timesScheduled(),
            process->number_of_open_file_descriptors(),
            process->tty() ? strrchr(process->tty()->ttyName().characters(), '/') + 1 : "n/a",
            process->name().characters());
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
