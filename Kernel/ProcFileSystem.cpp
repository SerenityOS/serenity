#include "ProcFileSystem.h"
#include "Process.h"
#include <Kernel/VirtualFileSystem.h>
#include "system.h"
#include "MemoryManager.h"
#include "StdLib.h"
#include "i386.h"
#include "KSyms.h"
#include <AK/StringBuilder.h>

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
    if (process.number_of_open_file_descriptors() == 0)
        return { };
    StringBuilder builder;
    for (size_t i = 0; i < process.max_open_file_descriptors(); ++i) {
        auto* descriptor = process.file_descriptor(i);
        if (!descriptor)
            continue;
        builder.appendf("% 3u %s\n", i, descriptor->absolute_path().characters());
    }
    return builder.to_byte_buffer();
}

ByteBuffer procfs$pid_vm(Process& process)
{
    ProcessInspectionHandle handle(process);
    StringBuilder builder;
    builder.appendf("BEGIN       END         SIZE      COMMIT     NAME\n");
    for (auto& region : process.regions()) {
        builder.appendf("%x -- %x    %x  %x   %s\n",
            region->laddr().get(),
            region->laddr().offset(region->size() - 1).get(),
            region->size(),
            region->committed(),
            region->name().characters());
    }
    return builder.to_byte_buffer();
}

ByteBuffer procfs$pid_vmo(Process& process)
{
    ProcessInspectionHandle handle(process);
    StringBuilder builder;
    builder.appendf("BEGIN       END         SIZE        NAME\n");
    for (auto& region : process.regions()) {
        builder.appendf("%x -- %x    %x    %s\n",
            region->laddr().get(),
            region->laddr().offset(region->size() - 1).get(),
            region->size(),
            region->name().characters());
        builder.appendf("VMO: %s \"%s\" @ %x(%u)\n",
            region->vmo().is_anonymous() ? "anonymous" : "file-backed",
            region->vmo().name().characters(),
            &region->vmo(),
            region->vmo().retain_count());
        for (size_t i = 0; i < region->vmo().page_count(); ++i) {
            auto& physical_page = region->vmo().physical_pages()[i];
            builder.appendf("P%x%s(%u) ",
                physical_page ? physical_page->paddr().get() : 0,
                region->cow_map().get(i) ? "!" : "",
                physical_page ? physical_page->retain_count() : 0
            );
        }
        builder.appendf("\n");
    }
    return builder.to_byte_buffer();
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
    for (dword* stackPtr = (dword*)process.framePtr(); process.validate_read_from_kernel(LinearAddress((dword)stackPtr)); stackPtr = (dword*)*stackPtr) {
        dword retaddr = stackPtr[1];
        if (auto* ksym = ksymbolicate(retaddr))
            recognizedSymbols.append({ retaddr, ksym });
    }
    StringBuilder builder;
    for (auto& symbol : recognizedSymbols) {
        unsigned offset = symbol.address - symbol.ksym->address;
        builder.appendf("%p  %s +%u\n", symbol.address, symbol.ksym->name, offset);
    }
    return builder.to_byte_buffer();
}

ByteBuffer procfs$pid_regs(Process& process)
{
    ProcessInspectionHandle handle(process);
    auto& tss = process.tss();
    StringBuilder builder;
    builder.appendf("eax: %x\n", tss.eax);
    builder.appendf("ebx: %x\n", tss.ebx);
    builder.appendf("ecx: %x\n", tss.ecx);
    builder.appendf("edx: %x\n", tss.edx);
    builder.appendf("esi: %x\n", tss.esi);
    builder.appendf("edi: %x\n", tss.edi);
    builder.appendf("ebp: %x\n", tss.ebp);
    builder.appendf("cr3: %x\n", tss.cr3);
    builder.appendf("flg: %x\n", tss.eflags);
    builder.appendf("sp:  %w:%x\n", tss.ss, tss.esp);
    builder.appendf("pc:  %w:%x\n", tss.cs, tss.eip);
    return builder.to_byte_buffer();
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
    add_file(create_generated_file("vm", [&process] (SynthFSInode&) { return procfs$pid_vm(process); }), dir.index());
    add_file(create_generated_file("vmo", [&process] (SynthFSInode&) { return procfs$pid_vmo(process); }), dir.index());
    add_file(create_generated_file("stack", [&process] (SynthFSInode&) { return procfs$pid_stack(process); }), dir.index());
    add_file(create_generated_file("regs", [&process] (SynthFSInode&) { return procfs$pid_regs(process); }), dir.index());
    add_file(create_generated_file("fds", [&process] (SynthFSInode&) { return procfs$pid_fds(process); }), dir.index());
    if (process.executable_inode())
        add_file(create_generated_file("exe", [&process] (SynthFSInode&) { return procfs$pid_exe(process); }, 00120777), dir.index());
    if (process.cwd_inode())
        add_file(create_generated_file("cwd", [&process] (SynthFSInode&) { return procfs$pid_cwd(process); }, 00120777), dir.index());
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

ByteBuffer procfs$mm(SynthFSInode&)
{
    // FIXME: Implement
    InterruptDisabler disabler;
    StringBuilder builder;
    for (auto* vmo : MM.m_vmos) {
        builder.appendf("VMO: %p %s(%u): p:%4u %s\n",
            vmo,
            vmo->is_anonymous() ? "anon" : "file",
            vmo->retain_count(),
            vmo->page_count(),
            vmo->name().characters());
    }
    builder.appendf("VMO count: %u\n", MM.m_vmos.size());
    builder.appendf("Free physical pages: %u\n", MM.m_free_physical_pages.size());
    builder.appendf("Free supervisor physical pages: %u\n", MM.m_free_supervisor_physical_pages.size());
    return builder.to_byte_buffer();
}

ByteBuffer procfs$mounts(SynthFSInode&)
{
    InterruptDisabler disabler;
    StringBuilder builder;
    VFS::the().for_each_mount([&builder] (auto& mount) {
        auto& fs = mount.guest_fs();
        builder.appendf("%s @ ", fs.class_name());
        if (!mount.host().is_valid())
            builder.appendf("/\n", fs.class_name());
        else
            builder.appendf("%u:%u\n", mount.host().fsid(), mount.host().index());
    });
    return builder.to_byte_buffer();
}

ByteBuffer procfs$cpuinfo(SynthFSInode&)
{
    StringBuilder builder;
    {
        CPUID cpuid(0);
        builder.appendf("cpuid:     ");
        auto emit_dword = [&] (dword value) {
            builder.appendf("%c%c%c%c",
                value & 0xff,
                (value >> 8) & 0xff,
                (value >> 16) & 0xff,
                (value >> 24) & 0xff);
        };
        emit_dword(cpuid.ebx());
        emit_dword(cpuid.edx());
        emit_dword(cpuid.ecx());
        builder.appendf("\n");
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
        builder.appendf("family:    %u\n", display_family);
        builder.appendf("model:     %u\n", display_model);
        builder.appendf("stepping:  %u\n", stepping);
        builder.appendf("type:      %u\n", type);
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
        builder.appendf("brandstr:  \"%s\"\n", buffer);
    }
    return builder.to_byte_buffer();
}

ByteBuffer procfs$kmalloc(SynthFSInode&)
{
    StringBuilder builder;
    builder.appendf(
        "eternal:      %u\n"
        "allocated:    %u\n"
        "free:         %u\n",
        kmalloc_sum_eternal,
        sum_alloc,
        sum_free
    );
    return builder.to_byte_buffer();
}

ByteBuffer procfs$summary(SynthFSInode&)
{
    InterruptDisabler disabler;
    auto processes = Process::allProcesses();
    StringBuilder builder;
    builder.appendf("PID TPG PGP SID  OWNER  STATE      PPID NSCHED     FDS  TTY  NAME\n");
    for (auto* process : processes) {
        builder.appendf("% 3u % 3u % 3u % 3u  % 4u   % 8s   % 3u  % 9u  % 3u  % 4s  %s\n",
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
    return builder.to_byte_buffer();
}

ByteBuffer procfs$inodes(SynthFSInode&)
{
    extern HashTable<Inode*>& all_inodes();
    auto& vfs = VFS::the();
    StringBuilder builder;
    for (auto it : all_inodes()) {
        RetainPtr<Inode> inode = *it;
        String path = vfs.absolute_path(*inode);
        builder.appendf("Inode{K%x} %02u:%08u (%u) %s\n", inode.ptr(), inode->fsid(), inode->index(), inode->retain_count(), path.characters());
    }
    return builder.to_byte_buffer();
}

struct SysVariableData final : public SynthFSInodeCustomData {
    virtual ~SysVariableData() override { }

    enum Type {
        Invalid,
        Boolean,
    };
    Type type { Invalid };
    Function<void()> change_callback;
    void* address;
};

static ByteBuffer read_sys_bool(SynthFSInode& inode)
{
    ASSERT(inode.custom_data());
    auto buffer = ByteBuffer::create_uninitialized(2);
    auto& custom_data = *static_cast<const SysVariableData*>(inode.custom_data());
    ASSERT(custom_data.type == SysVariableData::Boolean);
    ASSERT(custom_data.address);
    buffer[0] = *reinterpret_cast<bool*>(custom_data.address) ? '1' : '0';
    buffer[1] = '\n';
    return buffer;
}

static ssize_t write_sys_bool(SynthFSInode& inode, const ByteBuffer& data)
{
    ASSERT(inode.custom_data());
    if (data.size() >= 1 && (data[0] == '0' || data[0] == '1')) {
        auto& custom_data = *static_cast<const SysVariableData*>(inode.custom_data());
        ASSERT(custom_data.address);
        bool old_value = *reinterpret_cast<bool*>(custom_data.address);
        bool new_value = data[0] == '1';
        *reinterpret_cast<bool*>(custom_data.address) = new_value;
        if (old_value != new_value && custom_data.change_callback)
            custom_data.change_callback();
    }
    return data.size();
}

void ProcFS::add_sys_bool(String&& name, bool* var, Function<void()>&& change_callback)
{
    auto file = create_generated_file(move(name), move(read_sys_bool), move(write_sys_bool));
    auto data = make<SysVariableData>();
    data->type = SysVariableData::Boolean;
    data->change_callback = move(change_callback);
    data->address = var;
    file->set_custom_data(move(data));
    InterruptDisabler disabler;
    add_file(move(file), m_sys_dir.index());
}

bool ProcFS::initialize()
{
    SynthFS::initialize();
    add_file(create_generated_file("mm", procfs$mm));
    add_file(create_generated_file("mounts", procfs$mounts));
    add_file(create_generated_file("kmalloc", procfs$kmalloc));
    add_file(create_generated_file("summary", procfs$summary));
    add_file(create_generated_file("cpuinfo", procfs$cpuinfo));
    add_file(create_generated_file("inodes", procfs$inodes));
    m_sys_dir = add_file(create_directory("sys"));
    return true;
}

const char* ProcFS::class_name() const
{
    return "procfs";
}
