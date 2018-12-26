#include "ProcFileSystem.h"
#include "Process.h"
#include <VirtualFileSystem/VirtualFileSystem.h>
#include "system.h"
#include "MemoryManager.h"
#include "StdLib.h"
#include "i386.h"
#include "KSyms.h"

static ProcFS* s_the;

ProcFS& ProcFS::the()
{
    ASSERT(s_the);
    return *s_the;
}

RetainPtr<ProcFS> ProcFS::create()
{
    return adopt(*new ProcFS);
}

ProcFS::ProcFS()
{
    s_the = this;
}

ProcFS::~ProcFS()
{
}

ByteBuffer procfs$pid_fds(Process& process)
{
    ProcessInspectionHandle handle(process);
    char* buffer;
    auto stringImpl = StringImpl::create_uninitialized(process.number_of_open_file_descriptors() * 80, buffer);
    memset(buffer, 0, stringImpl->length());
    char* ptr = buffer;
    for (size_t i = 0; i < process.max_open_file_descriptors(); ++i) {
        auto* descriptor = process.file_descriptor(i);
        if (!descriptor)
            continue;
        ptr += ksprintf(ptr, "% 3u %s\n", i, descriptor->absolute_path().characters());
    }
    *ptr = '\0';
    return ByteBuffer::copy((byte*)buffer, ptr - buffer);
}

ByteBuffer procfs$pid_vm(Process& process)
{
    ProcessInspectionHandle handle(process);
    char* buffer;
    auto stringImpl = StringImpl::create_uninitialized(80 + process.regionCount() * 160 + 4096, buffer);
    memset(buffer, 0, stringImpl->length());
    char* ptr = buffer;
    ptr += ksprintf(ptr, "BEGIN       END         SIZE      COMMIT     NAME\n");
    for (auto& region : process.regions()) {
        ptr += ksprintf(ptr, "%x -- %x    %x  %x   %s\n",
            region->linearAddress.get(),
            region->linearAddress.offset(region->size - 1).get(),
            region->size,
            region->committed(),
            region->name.characters());
    }
    *ptr = '\0';
    return ByteBuffer::copy((byte*)buffer, ptr - buffer);
}

ByteBuffer procfs$pid_vmo(Process& process)
{
    ProcessInspectionHandle handle(process);
    char* buffer;
    auto stringImpl = StringImpl::create_uninitialized(80 + process.regionCount() * 160 + 4096, buffer);
    memset(buffer, 0, stringImpl->length());
    char* ptr = buffer;
    ptr += ksprintf(ptr, "BEGIN       END         SIZE        NAME\n");
    for (auto& region : process.regions()) {
        ptr += ksprintf(ptr, "%x -- %x    %x    %s\n",
            region->linearAddress.get(),
            region->linearAddress.offset(region->size - 1).get(),
            region->size,
            region->name.characters());
        ptr += ksprintf(ptr, "VMO: %s \"%s\" @ %x(%u)\n",
            region->vmo().is_anonymous() ? "anonymous" : "file-backed",
            region->vmo().name().characters(),
            &region->vmo(),
            region->vmo().retain_count());
        for (size_t i = 0; i < region->vmo().page_count(); ++i) {
            auto& physical_page = region->vmo().physical_pages()[i];
            ptr += ksprintf(ptr, "P%x%s(%u) ",
                physical_page ? physical_page->paddr().get() : 0,
                region->cow_map.get(i) ? "!" : "",
                physical_page ? physical_page->retain_count() : 0
            );
        }
        ptr += ksprintf(ptr, "\n");
    }
    *ptr = '\0';
    return ByteBuffer::copy((byte*)buffer, ptr - buffer);
}

ByteBuffer procfs$pid_stack(Process& process)
{
    ProcessInspectionHandle handle(process);
    ProcessPagingScope pagingScope(process);
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
        bytesNeeded += strlen(symbol.ksym->name) + 8 + 16;
    }
    auto buffer = ByteBuffer::create_uninitialized(bytesNeeded);
    char* bufptr = (char*)buffer.pointer();

    for (auto& symbol : recognizedSymbols) {
        unsigned offset = symbol.address - symbol.ksym->address;
        bufptr += ksprintf(bufptr, "%p  %s +%u\n", symbol.address, symbol.ksym->name, offset);
    }
    buffer.trim(bufptr - (char*)buffer.pointer());
    return buffer;
}

ByteBuffer procfs$pid_regs(Process& process)
{
    ProcessInspectionHandle handle(process);
    auto& tss = process.tss();
    auto buffer = ByteBuffer::create_uninitialized(1024);
    char* ptr = (char*)buffer.pointer();
    ptr += ksprintf(ptr, "eax: %x\n", tss.eax);
    ptr += ksprintf(ptr, "ebx: %x\n", tss.ebx);
    ptr += ksprintf(ptr, "ecx: %x\n", tss.ecx);
    ptr += ksprintf(ptr, "edx: %x\n", tss.edx);
    ptr += ksprintf(ptr, "esi: %x\n", tss.esi);
    ptr += ksprintf(ptr, "edi: %x\n", tss.edi);
    ptr += ksprintf(ptr, "ebp: %x\n", tss.ebp);
    ptr += ksprintf(ptr, "cr3: %x\n", tss.cr3);
    ptr += ksprintf(ptr, "flg: %x\n", tss.eflags);
    ptr += ksprintf(ptr, "sp:  %w:%x\n", tss.ss, tss.esp);
    ptr += ksprintf(ptr, "pc:  %w:%x\n", tss.cs, tss.eip);
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

ByteBuffer procfs$pid_exe(Process& process)
{
    ProcessInspectionHandle handle(process);
    auto inode = process.executable_inode();
    ASSERT(inode);
    return VFS::the().absolute_path(*inode).to_byte_buffer();
}

ByteBuffer procfs$pid_cwd(Process& process)
{
    ProcessInspectionHandle handle(process);
    auto inode = process.cwd_inode();
    ASSERT(inode);
    return VFS::the().absolute_path(*inode).to_byte_buffer();
}

void ProcFS::add_process(Process& process)
{
    InterruptDisabler disabler;
    char buf[16];
    ksprintf(buf, "%d", process.pid());
    auto dir = add_file(create_directory(buf));
    m_pid2inode.set(process.pid(), dir.index());
    add_file(create_generated_file("vm", [&process] { return procfs$pid_vm(process); }), dir.index());
    add_file(create_generated_file("vmo", [&process] { return procfs$pid_vmo(process); }), dir.index());
    add_file(create_generated_file("stack", [&process] { return procfs$pid_stack(process); }), dir.index());
    add_file(create_generated_file("regs", [&process] { return procfs$pid_regs(process); }), dir.index());
    add_file(create_generated_file("fds", [&process] { return procfs$pid_fds(process); }), dir.index());
    if (process.executable_inode())
        add_file(create_generated_file("exe", [&process] { return procfs$pid_exe(process); }, 00120777), dir.index());
    add_file(create_generated_file("cwd", [&process] { return procfs$pid_cwd(process); }, 00120777), dir.index());
}

void ProcFS::remove_process(Process& process)
{
    InterruptDisabler disabler;
    auto pid = process.pid();
    auto it = m_pid2inode.find(pid);
    if (it == m_pid2inode.end())
        return;
    bool success = remove_file((*it).value);
    ASSERT(success);
    m_pid2inode.remove(pid);
}

ByteBuffer procfs$mm()
{
    // FIXME: Implement
    InterruptDisabler disabler;
    auto buffer = ByteBuffer::create_uninitialized(1024 + 80 * MM.m_vmos.size());
    char* ptr = (char*)buffer.pointer();
    for (auto* vmo : MM.m_vmos) {
        ptr += ksprintf(ptr, "VMO: %p %s(%u): p:%4u %s\n",
            vmo,
            vmo->is_anonymous() ? "anon" : "file",
            vmo->retain_count(),
            vmo->page_count(),
            vmo->name().characters());
    }
    ptr += ksprintf(ptr, "VMO count: %u\n", MM.m_vmos.size());
    ptr += ksprintf(ptr, "Free physical pages: %u\n", MM.m_free_physical_pages.size());
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

ByteBuffer procfs$regions()
{
    // FIXME: Implement
    InterruptDisabler disabler;
    auto buffer = ByteBuffer::create_uninitialized(1024 + 80 * MM.m_regions.size());
    char* ptr = (char*)buffer.pointer();
    for (auto* region : MM.m_regions) {
        ptr += ksprintf(ptr, "Region: %p VMO=%p %s\n",
            region,
            &region->vmo(),
            region->name.characters());
    }
    ptr += ksprintf(ptr, "Region count: %u\n", MM.m_regions.size());
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

ByteBuffer procfs$mounts()
{
    InterruptDisabler disabler;
    auto buffer = ByteBuffer::create_uninitialized(VFS::the().mount_count() * 80);
    char* ptr = (char*)buffer.pointer();
    VFS::the().for_each_mount([&ptr] (auto& mount) {
        auto& fs = mount.guest_fs();
        ptr += ksprintf(ptr, "%s @ ", fs.class_name());
        if (!mount.host().is_valid())
            ptr += ksprintf(ptr, "/\n", fs.class_name());
        else
            ptr += ksprintf(ptr, "%u:%u\n", mount.host().fsid(), mount.host().index());
    });
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

ByteBuffer procfs$cpuinfo()
{
    auto buffer = ByteBuffer::create_uninitialized(256);
    char* ptr = (char*)buffer.pointer();
    {
        CPUID cpuid(0);
        ptr += ksprintf(ptr, "cpuid:     ");
        auto emit_dword = [&] (dword value) {
            ptr += ksprintf(ptr, "%c%c%c%c",
                value & 0xff,
                (value >> 8) & 0xff,
                (value >> 16) & 0xff,
                (value >> 24) & 0xff);
        };
        emit_dword(cpuid.ebx());
        emit_dword(cpuid.edx());
        emit_dword(cpuid.ecx());
        ptr += ksprintf(ptr, "\n");
    }
    {
        CPUID cpuid(1);
        dword stepping = cpuid.eax() & 0xf;
        dword model = (cpuid.eax() >> 4) & 0xf;
        dword family = (cpuid.eax() >> 8) & 0xf;
        dword type = (cpuid.eax() >> 12) & 0x3;
        dword extended_model = (cpuid.eax() >> 16) & 0xf;
        dword extended_family = (cpuid.eax() >> 20) & 0xff;
        dword display_model;
        dword display_family;
        if (family == 15) {
            display_family = family + extended_family;
            display_model = model + (extended_model << 4);
        } else if (family == 6) {
            display_family = family;
            display_model = model + (extended_model << 4);
        } else {
            display_family = family;
            display_model = model;
        }
        ptr += ksprintf(ptr, "family:    %u\n", display_family);
        ptr += ksprintf(ptr, "model:     %u\n", display_model);
        ptr += ksprintf(ptr, "stepping:  %u\n", stepping);
        ptr += ksprintf(ptr, "type:      %u\n", type);
    }
    {
        // FIXME: Check first that this is supported by calling CPUID with eax=0x80000000
        //        and verifying that the returned eax>=0x80000004.
        char buffer[48];
        dword* bufptr = reinterpret_cast<dword*>(buffer);
        auto copy_brand_string_part_to_buffer = [&] (dword i) {
            CPUID cpuid(0x80000002 + i);
            *bufptr++ = cpuid.eax();
            *bufptr++ = cpuid.ebx();
            *bufptr++ = cpuid.ecx();
            *bufptr++ = cpuid.edx();
        };
        copy_brand_string_part_to_buffer(0);
        copy_brand_string_part_to_buffer(1);
        copy_brand_string_part_to_buffer(2);
        ptr += ksprintf(ptr, "brandstr:  \"%s\"\n", buffer);
    }
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

ByteBuffer procfs$kmalloc()
{
    auto buffer = ByteBuffer::create_uninitialized(256);
    char* ptr = (char*)buffer.pointer();
    ptr += ksprintf(ptr, "eternal:      %u\nallocated:    %u\nfree:         %u\n", kmalloc_sum_eternal, sum_alloc, sum_free);
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

ByteBuffer procfs$summary()
{
    InterruptDisabler disabler;
    auto processes = Process::allProcesses();
    auto buffer = ByteBuffer::create_uninitialized(processes.size() * 256);
    char* ptr = (char*)buffer.pointer();
    ptr += ksprintf(ptr, "PID TPG PGP SID  OWNER  STATE      PPID NSCHED     FDS  TTY  NAME\n");
    for (auto* process : processes) {
        ptr += ksprintf(ptr, "% 3u % 3u % 3u % 3u  % 4u   % 8s   % 3u  % 9u  % 3u  % 4s  %s\n",
            process->pid(),
            process->tty() ? process->tty()->pgid() : 0,
            process->pgid(),
            process->sid(),
            process->uid(),
            toString(process->state()),
            process->ppid(),
            process->timesScheduled(),
            process->number_of_open_file_descriptors(),
            process->tty() ? strrchr(process->tty()->tty_name().characters(), '/') + 1 : "n/a",
            process->name().characters());
    }
    *ptr = '\0';
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

ByteBuffer procfs$vnodes()
{
    auto& vfs = VFS::the();
    auto buffer = ByteBuffer::create_uninitialized(vfs.m_max_vnode_count * 256);
    char* ptr = (char*)buffer.pointer();
    for (size_t i = 0; i < vfs.m_max_vnode_count; ++i) {
        auto& vnode = vfs.m_nodes[i];
        // FIXME: Retain the vnode while inspecting it.
        if (!vnode.inUse())
            continue;
        String path;
        if (vnode.core_inode())
            path = vfs.absolute_path(*vnode.core_inode());
        if (path.is_empty()) {
            if (auto* dev = vnode.characterDevice()) {
                if (dev->is_tty())
                    path = static_cast<const TTY*>(dev)->tty_name();
            }
        }
        ptr += ksprintf(ptr, "vnode %03u: %02u:%08u (%u) %s", i, vnode.inode.fsid(), vnode.inode.index(), vnode.retain_count(), path.characters());
        if (vnode.characterDevice())
            ptr += ksprintf(ptr, " (chardev: %p)", vnode.characterDevice());
        ptr += ksprintf(ptr, "\n");
    }
    *ptr = '\0';
    buffer.trim(ptr - (char*)buffer.pointer());
    return buffer;
}

bool ProcFS::initialize()
{
    SynthFS::initialize();
    add_file(create_generated_file("mm", procfs$mm));
    add_file(create_generated_file("regions", procfs$regions));
    add_file(create_generated_file("mounts", procfs$mounts));
    add_file(create_generated_file("kmalloc", procfs$kmalloc));
    add_file(create_generated_file("summary", procfs$summary));
    add_file(create_generated_file("cpuinfo", procfs$cpuinfo));
    add_file(create_generated_file("vnodes", procfs$vnodes));
    return true;
}

const char* ProcFS::class_name() const
{
    return "procfs";
}
