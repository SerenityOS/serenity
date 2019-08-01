#include "ProcFS.h"
#include "Console.h"
#include "KSyms.h"
#include "Process.h"
#include "Scheduler.h"
#include "StdLib.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KParams.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/PCI.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/kmalloc.h>
#include <LibC/errno_numbers.h>

enum ProcParentDirectory {
    PDI_AbstractRoot = 0,
    PDI_Root,
    PDI_Root_sys,
    PDI_PID,
    PDI_PID_fd,
};

enum ProcFileType {
    FI_Invalid = 0,

    FI_Root = 1, // directory

    __FI_Root_Start,
    FI_Root_mm,
    FI_Root_mounts,
    FI_Root_df,
    FI_Root_kmalloc,
    FI_Root_all,
    FI_Root_memstat,
    FI_Root_cpuinfo,
    FI_Root_inodes,
    FI_Root_dmesg,
    FI_Root_pci,
    FI_Root_uptime,
    FI_Root_cmdline,
    FI_Root_netadapters,
    FI_Root_self, // symlink
    FI_Root_sys,  // directory
    __FI_Root_End,

    FI_PID,

    __FI_PID_Start,
    FI_PID_vm,
    FI_PID_vmo,
    FI_PID_stack,
    FI_PID_regs,
    FI_PID_fds,
    FI_PID_exe, // symlink
    FI_PID_cwd, // symlink
    FI_PID_fd,  // directory
    __FI_PID_End,

    FI_MaxStaticFileIndex,
};

static inline pid_t to_pid(const InodeIdentifier& identifier)
{
#ifdef PROCFS_DEBUG
    dbgprintf("to_pid, index=%08x -> %u\n", identifier.index(), identifier.index() >> 16);
#endif
    return identifier.index() >> 16u;
}

static inline ProcParentDirectory to_proc_parent_directory(const InodeIdentifier& identifier)
{
    return (ProcParentDirectory)((identifier.index() >> 12) & 0xf);
}

static inline int to_fd(const InodeIdentifier& identifier)
{
    ASSERT(to_proc_parent_directory(identifier) == PDI_PID_fd);
    return (identifier.index() & 0xff) - FI_MaxStaticFileIndex;
}

static inline int to_sys_index(const InodeIdentifier& identifier)
{
    ASSERT(to_proc_parent_directory(identifier) == PDI_Root_sys);
    return identifier.index() & 0xff;
}

static inline InodeIdentifier to_identifier(unsigned fsid, ProcParentDirectory parent, pid_t pid, ProcFileType proc_file_type)
{
    return { fsid, ((unsigned)parent << 12u) | ((unsigned)pid << 16u) | (unsigned)proc_file_type };
}

static inline InodeIdentifier to_identifier_with_fd(unsigned fsid, pid_t pid, int fd)
{
    return { fsid, (PDI_PID_fd << 12u) | ((unsigned)pid << 16u) | (FI_MaxStaticFileIndex + fd) };
}

static inline InodeIdentifier sys_var_to_identifier(unsigned fsid, unsigned index)
{
    ASSERT(index < 256);
    return { fsid, (PDI_Root_sys << 12u) | index };
}

static inline InodeIdentifier to_parent_id(const InodeIdentifier& identifier)
{
    switch (to_proc_parent_directory(identifier)) {
    case PDI_AbstractRoot:
    case PDI_Root:
        return { identifier.fsid(), FI_Root };
    case PDI_Root_sys:
        return { identifier.fsid(), FI_Root_sys };
    case PDI_PID:
        return to_identifier(identifier.fsid(), PDI_Root, to_pid(identifier), FI_PID);
    case PDI_PID_fd:
        return to_identifier(identifier.fsid(), PDI_PID, to_pid(identifier), FI_PID_fd);
    }
    ASSERT_NOT_REACHED();
}

#if 0
static inline u8 to_unused_metadata(const InodeIdentifier& identifier)
{
    return (identifier.index() >> 8) & 0xf;
}
#endif

static inline ProcFileType to_proc_file_type(const InodeIdentifier& identifier)
{
    return (ProcFileType)(identifier.index() & 0xff);
}

static inline bool is_process_related_file(const InodeIdentifier& identifier)
{
    if (to_proc_file_type(identifier) == FI_PID)
        return true;
    auto proc_parent_directory = to_proc_parent_directory(identifier);
    switch (proc_parent_directory) {
    case PDI_PID:
    case PDI_PID_fd:
        return true;
    default:
        return false;
    }
}

static inline bool is_directory(const InodeIdentifier& identifier)
{
    auto proc_file_type = to_proc_file_type(identifier);
    switch (proc_file_type) {
    case FI_Root:
    case FI_Root_sys:
    case FI_PID:
    case FI_PID_fd:
        return true;
    default:
        return false;
    }
}

static inline bool is_persistent_inode(const InodeIdentifier& identifier)
{
    return to_proc_parent_directory(identifier) == PDI_Root_sys;
}

static ProcFS* s_the;

ProcFS& ProcFS::the()
{
    ASSERT(s_the);
    return *s_the;
}

NonnullRefPtr<ProcFS> ProcFS::create()
{
    return adopt(*new ProcFS);
}

ProcFS::~ProcFS()
{
}

ByteBuffer procfs$pid_fds(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    if (process.number_of_open_file_descriptors() == 0)
        return {};
    JsonArray array;
    for (int i = 0; i < process.max_open_file_descriptors(); ++i) {
        auto* description = process.file_description(i);
        if (!description)
            continue;
        JsonObject description_object;
        description_object.set("fd", i);
        description_object.set("absolute_path", description->absolute_path());
        array.append(move(description_object));
    }
    return array.serialized().to_byte_buffer();
}

ByteBuffer procfs$pid_fd_entry(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    int fd = to_fd(identifier);
    auto* description = process.file_description(fd);
    if (!description)
        return {};
    return description->absolute_path().to_byte_buffer();
}

ByteBuffer procfs$pid_vm(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    JsonArray array;
    for (auto& region : process.regions()) {
        JsonObject region_object;
        region_object.set("readable", region.is_readable());
        region_object.set("writable", region.is_writable());
        region_object.set("address", region.vaddr().get());
        region_object.set("size", region.size());
        region_object.set("amount_resident", region.amount_resident());
        region_object.set("name", region.name());
        array.append(move(region_object));
    }
    return array.serialized().to_byte_buffer();
}

ByteBuffer procfs$pci(InodeIdentifier)
{
    StringBuilder builder;
    PCI::enumerate_all([&builder](PCI::Address address, PCI::ID id) {
        builder.appendf("%b:%b.%b %w:%w\n", address.bus(), address.slot(), address.function(), id.vendor_id, id.device_id);
    });
    return builder.to_byte_buffer();
}

ByteBuffer procfs$uptime(InodeIdentifier)
{
    StringBuilder builder;
    builder.appendf("%u\n", (u32)(g_uptime / 1000));
    return builder.to_byte_buffer();
}

ByteBuffer procfs$cmdline(InodeIdentifier)
{
    StringBuilder builder;
    builder.appendf("%s\n", KParams::the().cmdline().characters());
    return builder.to_byte_buffer();
}

ByteBuffer procfs$netadapters(InodeIdentifier)
{
    StringBuilder builder;
    NetworkAdapter::for_each([&builder](auto& adapter) {
        builder.appendf("%s,%s,%s,%s\n",
            adapter.name().characters(),
            adapter.class_name(),
            adapter.mac_address().to_string().characters(),
            adapter.ipv4_address().to_string().characters());
    });
    return builder.to_byte_buffer();
}

ByteBuffer procfs$pid_vmo(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    StringBuilder builder;
    builder.appendf("BEGIN       END         SIZE        NAME\n");
    for (auto& region : process.regions()) {
        builder.appendf("%x -- %x    %x    %s\n",
            region.vaddr().get(),
            region.vaddr().offset(region.size() - 1).get(),
            region.size(),
            region.name().characters());
        builder.appendf("VMO: %s \"%s\" @ %x(%u)\n",
            region.vmo().is_anonymous() ? "anonymous" : "file-backed",
            region.vmo().name().characters(),
            &region.vmo(),
            region.vmo().ref_count());
        for (int i = 0; i < region.vmo().page_count(); ++i) {
            auto& physical_page = region.vmo().physical_pages()[i];
            builder.appendf("P%x%s(%u) ",
                physical_page ? physical_page->paddr().get() : 0,
                region.should_cow(i) ? "!" : "",
                physical_page ? physical_page->ref_count() : 0);
        }
        builder.appendf("\n");
    }
    return builder.to_byte_buffer();
}

ByteBuffer procfs$pid_stack(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    return process.backtrace(*handle).to_byte_buffer();
}

ByteBuffer procfs$pid_regs(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    StringBuilder builder;
    process.for_each_thread([&](Thread& thread) {
        builder.appendf("Thread %d:\n", thread.tid());
        auto& tss = thread.tss();
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
        return IterationDecision::Continue;
    });
    return builder.to_byte_buffer();
}

ByteBuffer procfs$pid_exe(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    auto* custody = process.executable();
    ASSERT(custody);
    return custody->absolute_path().to_byte_buffer();
}

ByteBuffer procfs$pid_cwd(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    return handle->process().current_directory().absolute_path().to_byte_buffer();
}

ByteBuffer procfs$self(InodeIdentifier)
{
    char buffer[16];
    ksprintf(buffer, "%u", current->pid());
    return ByteBuffer::copy((const u8*)buffer, strlen(buffer));
}

ByteBuffer procfs$mm(InodeIdentifier)
{
    // FIXME: Implement
    InterruptDisabler disabler;
    StringBuilder builder;
    for (auto* vmo : MM.m_vmos) {
        builder.appendf("VMO: %p %s(%u): p:%4u %s\n",
            vmo,
            vmo->is_anonymous() ? "anon" : "file",
            vmo->ref_count(),
            vmo->page_count(),
            vmo->name().characters());
    }
    builder.appendf("VMO count: %u\n", MM.m_vmos.size());
    builder.appendf("Free physical pages: %u\n", MM.user_physical_pages() - MM.user_physical_pages_used());
    builder.appendf("Free supervisor physical pages: %u\n", MM.super_physical_pages() - MM.super_physical_pages_used());
    return builder.to_byte_buffer();
}

ByteBuffer procfs$dmesg(InodeIdentifier)
{
    InterruptDisabler disabler;
    StringBuilder builder;
    for (char ch : Console::the().logbuffer())
        builder.append(ch);
    return builder.to_byte_buffer();
}

ByteBuffer procfs$mounts(InodeIdentifier)
{
    // FIXME: This is obviously racy against the VFS mounts changing.
    StringBuilder builder;
    VFS::the().for_each_mount([&builder](auto& mount) {
        auto& fs = mount.guest_fs();
        builder.appendf("%s @ ", fs.class_name());
        if (!mount.host().is_valid())
            builder.appendf("/");
        else {
            builder.appendf("%u:%u", mount.host().fsid(), mount.host().index());
            builder.append(' ');
            builder.append(mount.absolute_path());
        }
        builder.append('\n');
    });
    return builder.to_byte_buffer();
}

ByteBuffer procfs$df(InodeIdentifier)
{
    // FIXME: This is obviously racy against the VFS mounts changing.
    JsonArray json;
    VFS::the().for_each_mount([&json](auto& mount) {
        auto& fs = mount.guest_fs();
        JsonObject fs_object;
        fs_object.set("class_name", fs.class_name());
        fs_object.set("total_block_count", fs.total_block_count());
        fs_object.set("free_block_count", fs.free_block_count());
        fs_object.set("total_inode_count", fs.total_inode_count());
        fs_object.set("free_inode_count", fs.free_inode_count());
        fs_object.set("mount_point", mount.absolute_path());
        json.append(fs_object);
    });
    return json.serialized().to_byte_buffer();
}

ByteBuffer procfs$cpuinfo(InodeIdentifier)
{
    StringBuilder builder;
    {
        CPUID cpuid(0);
        builder.appendf("cpuid:     ");
        auto emit_u32 = [&](u32 value) {
            builder.appendf("%c%c%c%c",
                value & 0xff,
                (value >> 8) & 0xff,
                (value >> 16) & 0xff,
                (value >> 24) & 0xff);
        };
        emit_u32(cpuid.ebx());
        emit_u32(cpuid.edx());
        emit_u32(cpuid.ecx());
        builder.appendf("\n");
    }
    {
        CPUID cpuid(1);
        u32 stepping = cpuid.eax() & 0xf;
        u32 model = (cpuid.eax() >> 4) & 0xf;
        u32 family = (cpuid.eax() >> 8) & 0xf;
        u32 type = (cpuid.eax() >> 12) & 0x3;
        u32 extended_model = (cpuid.eax() >> 16) & 0xf;
        u32 extended_family = (cpuid.eax() >> 20) & 0xff;
        u32 display_model;
        u32 display_family;
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
        alignas(u32) char buffer[48];
        u32* bufptr = reinterpret_cast<u32*>(buffer);
        auto copy_brand_string_part_to_buffer = [&](u32 i) {
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

ByteBuffer procfs$kmalloc(InodeIdentifier)
{
    StringBuilder builder;
    builder.appendf(
        "eternal:      %u\n"
        "allocated:    %u\n"
        "free:         %u\n",
        kmalloc_sum_eternal,
        sum_alloc,
        sum_free);
    return builder.to_byte_buffer();
}

ByteBuffer procfs$memstat(InodeIdentifier)
{
    InterruptDisabler disabler;
    JsonObject json;
    json.set("kmalloc_allocated", sum_alloc);
    json.set("kmalloc_available", sum_free);
    json.set("kmalloc_eternal_allocated", kmalloc_sum_eternal);
    json.set("user_physical_allocated", MM.user_physical_pages_used());
    json.set("user_physical_available", MM.user_physical_pages());
    json.set("super_physical_allocated", MM.super_physical_pages_used());
    json.set("super_physical_available", MM.super_physical_pages());
    json.set("kmalloc_call_count", g_kmalloc_call_count);
    json.set("kfree_call_count", g_kfree_call_count);
    return json.serialized().to_byte_buffer();
}

ByteBuffer procfs$all(InodeIdentifier)
{
    InterruptDisabler disabler;
    auto processes = Process::all_processes();
    JsonArray array;

    // Keep this in sync with CProcessStatistics.
    auto build_process = [&](const Process& process) {
        JsonObject process_object;
        process_object.set("pid", process.pid());
        process_object.set("times_scheduled", process.main_thread().times_scheduled());
        process_object.set("pgid", process.tty() ? process.tty()->pgid() : 0);
        process_object.set("pgp", process.pgid());
        process_object.set("sid", process.sid());
        process_object.set("uid", process.uid());
        process_object.set("gid", process.gid());
        process_object.set("state", process.main_thread().state_string());
        process_object.set("ppid", process.ppid());
        process_object.set("nfds", process.number_of_open_file_descriptors());
        process_object.set("name", process.name());
        process_object.set("tty", process.tty() ? process.tty()->tty_name() : "notty");
        process_object.set("amount_virtual", process.amount_virtual());
        process_object.set("amount_resident", process.amount_resident());
        process_object.set("amount_shared", process.amount_shared());
        process_object.set("ticks", process.main_thread().ticks());
        process_object.set("priority", to_string(process.priority()));
        process_object.set("syscall_count", process.syscall_count());
        process_object.set("icon_id", process.icon_id());
        array.append(process_object);
    };
    build_process(*Scheduler::colonel());
    for (auto* process : processes)
        build_process(*process);
    return array.serialized().to_byte_buffer();
}

ByteBuffer procfs$inodes(InodeIdentifier)
{
    extern HashTable<Inode*>& all_inodes();
    StringBuilder builder;
    for (auto it : all_inodes()) {
        RefPtr<Inode> inode = *it;
        builder.appendf("Inode{K%x} %02u:%08u (%u)\n", inode.ptr(), inode->fsid(), inode->index(), inode->ref_count());
    }
    return builder.to_byte_buffer();
}

struct SysVariableData final : public ProcFSInodeCustomData {
    virtual ~SysVariableData() override {}

    enum Type {
        Invalid,
        Boolean,
        String,
    };
    Type type { Invalid };
    Function<void()> notify_callback;
    void* address;
};

static ByteBuffer read_sys_bool(InodeIdentifier inode_id)
{
    auto inode_ptr = ProcFS::the().get_inode(inode_id);
    if (!inode_ptr)
        return {};
    auto& inode = static_cast<ProcFSInode&>(*inode_ptr);
    ASSERT(inode.custom_data());
    auto buffer = ByteBuffer::create_uninitialized(2);
    auto& custom_data = *static_cast<const SysVariableData*>(inode.custom_data());
    ASSERT(custom_data.type == SysVariableData::Boolean);
    ASSERT(custom_data.address);
    auto* lockable_bool = reinterpret_cast<Lockable<bool>*>(custom_data.address);
    {
        LOCKER(lockable_bool->lock());
        buffer[0] = lockable_bool->resource() ? '1' : '0';
    }
    buffer[1] = '\n';
    return buffer;
}

static ssize_t write_sys_bool(InodeIdentifier inode_id, const ByteBuffer& data)
{
    auto inode_ptr = ProcFS::the().get_inode(inode_id);
    if (!inode_ptr)
        return {};
    auto& inode = static_cast<ProcFSInode&>(*inode_ptr);
    ASSERT(inode.custom_data());
    if (data.is_empty() || !(data[0] == '0' || data[0] == '1'))
        return data.size();

    auto& custom_data = *static_cast<const SysVariableData*>(inode.custom_data());
    auto* lockable_bool = reinterpret_cast<Lockable<bool>*>(custom_data.address);
    {
        LOCKER(lockable_bool->lock());
        lockable_bool->resource() = data[0] == '1';
    }
    if (custom_data.notify_callback)
        custom_data.notify_callback();
    return data.size();
}

static ByteBuffer read_sys_string(InodeIdentifier inode_id)
{
    auto inode_ptr = ProcFS::the().get_inode(inode_id);
    if (!inode_ptr)
        return {};
    auto& inode = static_cast<ProcFSInode&>(*inode_ptr);
    ASSERT(inode.custom_data());
    auto buffer = ByteBuffer::create_uninitialized(2);
    auto& custom_data = *static_cast<const SysVariableData*>(inode.custom_data());
    ASSERT(custom_data.type == SysVariableData::String);
    ASSERT(custom_data.address);
    auto* lockable_string = reinterpret_cast<Lockable<String>*>(custom_data.address);
    LOCKER(lockable_string->lock());
    return lockable_string->resource().to_byte_buffer();
}

static ssize_t write_sys_string(InodeIdentifier inode_id, const ByteBuffer& data)
{
    auto inode_ptr = ProcFS::the().get_inode(inode_id);
    if (!inode_ptr)
        return {};
    auto& inode = static_cast<ProcFSInode&>(*inode_ptr);
    ASSERT(inode.custom_data());
    auto& custom_data = *static_cast<const SysVariableData*>(inode.custom_data());
    ASSERT(custom_data.address);
    {
        auto* lockable_string = reinterpret_cast<Lockable<String>*>(custom_data.address);
        LOCKER(lockable_string->lock());
        lockable_string->resource() = String((const char*)data.pointer(), data.size());
    }
    if (custom_data.notify_callback)
        custom_data.notify_callback();
    return data.size();
}

void ProcFS::add_sys_bool(String&& name, Lockable<bool>& var, Function<void()>&& notify_callback)
{
    InterruptDisabler disabler;

    int index = m_sys_entries.size();
    auto inode = adopt(*new ProcFSInode(*this, sys_var_to_identifier(fsid(), index).index()));
    auto data = make<SysVariableData>();
    data->type = SysVariableData::Boolean;
    data->notify_callback = move(notify_callback);
    data->address = &var;
    inode->set_custom_data(move(data));
    m_sys_entries.empend(strdup(name.characters()), 0, read_sys_bool, write_sys_bool, move(inode));
}

void ProcFS::add_sys_string(String&& name, Lockable<String>& var, Function<void()>&& notify_callback)
{
    InterruptDisabler disabler;

    int index = m_sys_entries.size();
    auto inode = adopt(*new ProcFSInode(*this, sys_var_to_identifier(fsid(), index).index()));
    auto data = make<SysVariableData>();
    data->type = SysVariableData::String;
    data->notify_callback = move(notify_callback);
    data->address = &var;
    inode->set_custom_data(move(data));
    m_sys_entries.empend(strdup(name.characters()), 0, read_sys_string, write_sys_string, move(inode));
}

bool ProcFS::initialize()
{
    return true;
}

const char* ProcFS::class_name() const
{
    return "ProcFS";
}

RefPtr<Inode> ProcFS::create_inode(InodeIdentifier, const String&, mode_t, off_t, dev_t, int&)
{
    kprintf("FIXME: Implement ProcFS::create_inode()?\n");
    return {};
}

RefPtr<Inode> ProcFS::create_directory(InodeIdentifier, const String&, mode_t, int& error)
{
    error = -EROFS;
    return nullptr;
}

InodeIdentifier ProcFS::root_inode() const
{
    return { fsid(), FI_Root };
}

RefPtr<Inode> ProcFS::get_inode(InodeIdentifier inode_id) const
{
#ifdef PROCFS_DEBUG
    dbgprintf("ProcFS::get_inode(%u)\n", inode_id.index());
#endif
    if (inode_id == root_inode())
        return m_root_inode;

    if (to_proc_parent_directory(inode_id) == ProcParentDirectory::PDI_Root_sys) {
        auto sys_index = to_sys_index(inode_id);
        if (sys_index < m_sys_entries.size())
            return m_sys_entries[sys_index].inode;
    }

    LOCKER(m_inodes_lock);
    auto it = m_inodes.find(inode_id.index());
    if (it == m_inodes.end()) {
        auto inode = adopt(*new ProcFSInode(const_cast<ProcFS&>(*this), inode_id.index()));
        m_inodes.set(inode_id.index(), inode.ptr());
        return inode;
    }
    return (*it).value;
}

ProcFSInode::ProcFSInode(ProcFS& fs, unsigned index)
    : Inode(fs, index)
{
}

ProcFSInode::~ProcFSInode()
{
    LOCKER(fs().m_inodes_lock);
    fs().m_inodes.remove(index());
}

InodeMetadata ProcFSInode::metadata() const
{
#ifdef PROCFS_DEBUG
    dbgprintf("ProcFSInode::metadata(%u)\n", index());
#endif
    InodeMetadata metadata;
    metadata.inode = identifier();
    metadata.ctime = mepoch;
    metadata.atime = mepoch;
    metadata.mtime = mepoch;
    auto proc_parent_directory = to_proc_parent_directory(identifier());
    auto pid = to_pid(identifier());
    auto proc_file_type = to_proc_file_type(identifier());

#ifdef PROCFS_DEBUG
    dbgprintf("  -> pid: %d, fi: %u, pdi: %u\n", pid, proc_file_type, proc_parent_directory);
#endif

    if (is_process_related_file(identifier())) {
        auto handle = ProcessInspectionHandle::from_pid(pid);
        metadata.uid = handle->process().sys$getuid();
        metadata.gid = handle->process().sys$getgid();
    }

    if (proc_parent_directory == PDI_PID_fd) {
        metadata.mode = 00120777;
        return metadata;
    }

    if (proc_parent_directory == PDI_Root_sys) {
        metadata.mode = 00100644;
        return metadata;
    }

    switch (proc_file_type) {
    case FI_Root_self:
    case FI_PID_cwd:
    case FI_PID_exe:
        metadata.mode = 0120777;
        break;
    case FI_Root:
    case FI_Root_sys:
    case FI_PID:
    case FI_PID_fd:
        metadata.mode = 040777;
        break;
    default:
        metadata.mode = 0100644;
        break;
    }
#ifdef PROCFS_DEBUG
    dbgprintf("Returning mode %o\n", metadata.mode);
#endif
    return metadata;
}

ssize_t ProcFSInode::read_bytes(off_t offset, ssize_t count, u8* buffer, FileDescription* description) const
{
#ifdef PROCFS_DEBUG
    dbgprintf("ProcFS: read_bytes %u\n", index());
#endif
    ASSERT(offset >= 0);
    ASSERT(buffer);

    auto* directory_entry = fs().get_directory_entry(identifier());

    Function<ByteBuffer(InodeIdentifier)> callback_tmp;
    Function<ByteBuffer(InodeIdentifier)>* read_callback { nullptr };
    if (directory_entry) {
        read_callback = &directory_entry->read_callback;
    } else {
        if (to_proc_parent_directory(identifier()) == PDI_PID_fd) {
            callback_tmp = procfs$pid_fd_entry;
            read_callback = &callback_tmp;
        }
    }

    ASSERT(read_callback);

    ByteBuffer generated_data;
    if (!description) {
        generated_data = (*read_callback)(identifier());
    } else {
        if (!description->generator_cache())
            description->generator_cache() = (*read_callback)(identifier());
        generated_data = description->generator_cache();
    }

    auto& data = generated_data;
    ssize_t nread = min(static_cast<off_t>(data.size() - offset), static_cast<off_t>(count));
    memcpy(buffer, data.pointer() + offset, nread);
    if (nread == 0 && description && description->generator_cache())
        description->generator_cache().clear();
    return nread;
}

InodeIdentifier ProcFS::ProcFSDirectoryEntry::identifier(unsigned fsid) const
{
    return to_identifier(fsid, PDI_Root, 0, (ProcFileType)proc_file_type);
}

bool ProcFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntry&)> callback) const
{
#ifdef PROCFS_DEBUG
    dbgprintf("ProcFS: traverse_as_directory %u\n", index());
#endif

    if (!::is_directory(identifier()))
        return false;

    auto pid = to_pid(identifier());
    auto proc_file_type = to_proc_file_type(identifier());
    auto parent_id = to_parent_id(identifier());

    callback({ ".", 1, identifier(), 2 });
    callback({ "..", 2, parent_id, 2 });

    switch (proc_file_type) {
    case FI_Root:
        for (auto& entry : fs().m_entries) {
            // FIXME: strlen() here is sad.
            if (!entry.name)
                continue;
            if (entry.proc_file_type > __FI_Root_Start && entry.proc_file_type < __FI_Root_End)
                callback({ entry.name, (int)strlen(entry.name), to_identifier(fsid(), PDI_Root, 0, (ProcFileType)entry.proc_file_type), 0 });
        }
        for (auto pid_child : Process::all_pids()) {
            char name[16];
            int name_length = ksprintf(name, "%u", pid_child);
            callback({ name, name_length, to_identifier(fsid(), PDI_Root, pid_child, FI_PID), 0 });
        }
        break;

    case FI_Root_sys:
        for (int i = 0; i < fs().m_sys_entries.size(); ++i) {
            auto& entry = fs().m_sys_entries[i];
            callback({ entry.name, (int)strlen(entry.name), sys_var_to_identifier(fsid(), i), 0 });
        }
        break;

    case FI_PID: {
        auto handle = ProcessInspectionHandle::from_pid(pid);
        if (!handle)
            return false;
        auto& process = handle->process();
        for (auto& entry : fs().m_entries) {
            if (entry.proc_file_type > __FI_PID_Start && entry.proc_file_type < __FI_PID_End) {
                if (entry.proc_file_type == FI_PID_exe && !process.executable())
                    continue;
                // FIXME: strlen() here is sad.
                callback({ entry.name, (int)strlen(entry.name), to_identifier(fsid(), PDI_PID, pid, (ProcFileType)entry.proc_file_type), 0 });
            }
        }
    } break;

    case FI_PID_fd: {
        auto handle = ProcessInspectionHandle::from_pid(pid);
        if (!handle)
            return false;
        auto& process = handle->process();
        for (int i = 0; i < process.max_open_file_descriptors(); ++i) {
            auto* description = process.file_description(i);
            if (!description)
                continue;
            char name[16];
            int name_length = ksprintf(name, "%u", i);
            callback({ name, name_length, to_identifier_with_fd(fsid(), pid, i), 0 });
        }
    } break;
    default:
        return true;
    }

    return true;
}

InodeIdentifier ProcFSInode::lookup(StringView name)
{
    ASSERT(is_directory());
    if (name == ".")
        return identifier();
    if (name == "..")
        return to_parent_id(identifier());

    auto proc_file_type = to_proc_file_type(identifier());

    if (proc_file_type == FI_Root) {
        for (auto& entry : fs().m_entries) {
            if (entry.name == nullptr)
                continue;
            if (entry.proc_file_type > __FI_Root_Start && entry.proc_file_type < __FI_Root_End) {
                if (name == entry.name) {
                    return to_identifier(fsid(), PDI_Root, 0, (ProcFileType)entry.proc_file_type);
                }
            }
        }
        bool ok;
        unsigned name_as_number = name.to_uint(ok);
        if (ok) {
            bool process_exists = false;
            {
                InterruptDisabler disabler;
                process_exists = Process::from_pid(name_as_number);
            }
            if (process_exists)
                return to_identifier(fsid(), PDI_Root, name_as_number, FI_PID);
        }
        return {};
    }

    if (proc_file_type == FI_Root_sys) {
        for (int i = 0; i < fs().m_sys_entries.size(); ++i) {
            auto& entry = fs().m_sys_entries[i];
            if (name == entry.name)
                return sys_var_to_identifier(fsid(), i);
        }
        return {};
    }

    if (proc_file_type == FI_PID) {
        auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier()));
        if (!handle)
            return {};
        auto& process = handle->process();
        for (auto& entry : fs().m_entries) {
            if (entry.proc_file_type > __FI_PID_Start && entry.proc_file_type < __FI_PID_End) {
                if (entry.proc_file_type == FI_PID_exe && !process.executable())
                    continue;
                if (entry.name == nullptr)
                    continue;
                if (name == entry.name) {
                    return to_identifier(fsid(), PDI_PID, to_pid(identifier()), (ProcFileType)entry.proc_file_type);
                }
            }
        }
        return {};
    }

    if (proc_file_type == FI_PID_fd) {
        bool ok;
        unsigned name_as_number = name.to_uint(ok);
        if (ok) {
            bool fd_exists = false;
            {
                InterruptDisabler disabler;
                if (auto* process = Process::from_pid(to_pid(identifier())))
                    fd_exists = process->file_description(name_as_number);
            }
            if (fd_exists)
                return to_identifier_with_fd(fsid(), to_pid(identifier()), name_as_number);
        }
    }
    return {};
}

void ProcFSInode::flush_metadata()
{
}

ssize_t ProcFSInode::write_bytes(off_t offset, ssize_t size, const u8* buffer, FileDescription*)
{
    auto* directory_entry = fs().get_directory_entry(identifier());
    if (!directory_entry || !directory_entry->write_callback)
        return -EPERM;
    ASSERT(is_persistent_inode(identifier()));
    // FIXME: Being able to write into ProcFS at a non-zero offset seems like something we should maybe support..
    ASSERT(offset == 0);
    bool success = directory_entry->write_callback(identifier(), ByteBuffer::wrap(buffer, size));
    ASSERT(success);
    return 0;
}

KResult ProcFSInode::add_child(InodeIdentifier child_id, const StringView& name, mode_t)
{
    (void)child_id;
    (void)name;
    return KResult(-EPERM);
}

KResult ProcFSInode::remove_child(const StringView& name)
{
    (void)name;
    return KResult(-EPERM);
}

ProcFSInodeCustomData::~ProcFSInodeCustomData()
{
}

size_t ProcFSInode::directory_entry_count() const
{
    ASSERT(is_directory());
    size_t count = 0;
    traverse_as_directory([&count](const FS::DirectoryEntry&) {
        ++count;
        return true;
    });
    return count;
}

KResult ProcFSInode::chmod(mode_t)
{
    return KResult(-EPERM);
}

ProcFS::ProcFS()
{
    s_the = this;
    m_root_inode = adopt(*new ProcFSInode(*this, 1));
    m_entries.resize(FI_MaxStaticFileIndex);
    m_entries[FI_Root_mm] = { "mm", FI_Root_mm, procfs$mm };
    m_entries[FI_Root_mounts] = { "mounts", FI_Root_mounts, procfs$mounts };
    m_entries[FI_Root_df] = { "df", FI_Root_df, procfs$df };
    m_entries[FI_Root_kmalloc] = { "kmalloc", FI_Root_kmalloc, procfs$kmalloc };
    m_entries[FI_Root_all] = { "all", FI_Root_all, procfs$all };
    m_entries[FI_Root_memstat] = { "memstat", FI_Root_memstat, procfs$memstat };
    m_entries[FI_Root_cpuinfo] = { "cpuinfo", FI_Root_cpuinfo, procfs$cpuinfo };
    m_entries[FI_Root_inodes] = { "inodes", FI_Root_inodes, procfs$inodes };
    m_entries[FI_Root_dmesg] = { "dmesg", FI_Root_dmesg, procfs$dmesg };
    m_entries[FI_Root_self] = { "self", FI_Root_self, procfs$self };
    m_entries[FI_Root_pci] = { "pci", FI_Root_pci, procfs$pci };
    m_entries[FI_Root_uptime] = { "uptime", FI_Root_uptime, procfs$uptime };
    m_entries[FI_Root_cmdline] = { "cmdline", FI_Root_cmdline, procfs$cmdline };
    m_entries[FI_Root_netadapters] = { "netadapters", FI_Root_netadapters, procfs$netadapters };
    m_entries[FI_Root_sys] = { "sys", FI_Root_sys };

    m_entries[FI_PID_vm] = { "vm", FI_PID_vm, procfs$pid_vm };
    m_entries[FI_PID_vmo] = { "vmo", FI_PID_vmo, procfs$pid_vmo };
    m_entries[FI_PID_stack] = { "stack", FI_PID_stack, procfs$pid_stack };
    m_entries[FI_PID_regs] = { "regs", FI_PID_regs, procfs$pid_regs };
    m_entries[FI_PID_fds] = { "fds", FI_PID_fds, procfs$pid_fds };
    m_entries[FI_PID_exe] = { "exe", FI_PID_exe, procfs$pid_exe };
    m_entries[FI_PID_cwd] = { "cwd", FI_PID_cwd, procfs$pid_cwd };
    m_entries[FI_PID_fd] = { "fd", FI_PID_fd };

    m_kmalloc_stack_helper.resource() = g_dump_kmalloc_stacks;
    add_sys_bool("kmalloc_stacks", m_kmalloc_stack_helper, [this] {
        g_dump_kmalloc_stacks = m_kmalloc_stack_helper.resource();
    });
}

ProcFS::ProcFSDirectoryEntry* ProcFS::get_directory_entry(InodeIdentifier identifier) const
{
    if (to_proc_parent_directory(identifier) == PDI_Root_sys) {
        auto sys_index = to_sys_index(identifier);
        if (sys_index < m_sys_entries.size())
            return const_cast<ProcFSDirectoryEntry*>(&m_sys_entries[sys_index]);
        return nullptr;
    }
    auto proc_file_type = to_proc_file_type(identifier);
    if (proc_file_type != FI_Invalid && proc_file_type < FI_MaxStaticFileIndex)
        return const_cast<ProcFSDirectoryEntry*>(&m_entries[proc_file_type]);
    return nullptr;
}

KResult ProcFSInode::chown(uid_t, gid_t)
{
    return KResult(-EPERM);
}
