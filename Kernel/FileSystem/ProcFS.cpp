#include "ProcFS.h"
#include "Console.h"
#include "KSyms.h"
#include "Process.h"
#include "Scheduler.h"
#include "StdLib.h"
#include <AK/JsonArraySerializer.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/DiskBackedFileSystem.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/KParams.h>
#include <Kernel/Module.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/PCI.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PurgeableVMObject.h>
#include <LibC/errno_numbers.h>

enum ProcParentDirectory {
    PDI_AbstractRoot = 0,
    PDI_Root,
    PDI_Root_sys,
    PDI_Root_net,
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
    FI_Root_all,
    FI_Root_memstat,
    FI_Root_cpuinfo,
    FI_Root_inodes,
    FI_Root_dmesg,
    FI_Root_pci,
    FI_Root_devices,
    FI_Root_uptime,
    FI_Root_cmdline,
    FI_Root_modules,
    FI_Root_self, // symlink
    FI_Root_sys,  // directory
    FI_Root_net,  // directory
    __FI_Root_End,

    FI_Root_sys_variable,

    FI_Root_net_adapters,
    FI_Root_net_arp,
    FI_Root_net_tcp,
    FI_Root_net_udp,
    FI_Root_net_local,

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

static inline ProcFileType to_proc_file_type(const InodeIdentifier& identifier)
{
    return (ProcFileType)(identifier.index() & 0xff);
}

static inline int to_fd(const InodeIdentifier& identifier)
{
    ASSERT(to_proc_parent_directory(identifier) == PDI_PID_fd);
    return (identifier.index() & 0xff) - FI_MaxStaticFileIndex;
}

static inline int to_sys_index(const InodeIdentifier& identifier)
{
    ASSERT(to_proc_parent_directory(identifier) == PDI_Root_sys);
    ASSERT(to_proc_file_type(identifier) == FI_Root_sys_variable);
    return identifier.index() >> 16u;
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
    return { fsid, (PDI_Root_sys << 12u) | (index << 16u) | FI_Root_sys_variable };
}

static inline InodeIdentifier to_parent_id(const InodeIdentifier& identifier)
{
    switch (to_proc_parent_directory(identifier)) {
    case PDI_AbstractRoot:
    case PDI_Root:
        return { identifier.fsid(), FI_Root };
    case PDI_Root_sys:
        return { identifier.fsid(), FI_Root_sys };
    case PDI_Root_net:
        return { identifier.fsid(), FI_Root_net };
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
    case FI_Root_net:
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

NonnullRefPtr<ProcFS> ProcFS::create()
{
    return adopt(*new ProcFS);
}

ProcFS::~ProcFS()
{
}

Optional<KBuffer> procfs$pid_fds(InodeIdentifier identifier)
{
    KBufferBuilder builder;
    JsonArraySerializer array { builder };

    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle) {
        array.finish();
        return builder.build();
    }
    auto& process = handle->process();
    if (process.number_of_open_file_descriptors() == 0) {
        array.finish();
        return builder.build();
    }

    for (int i = 0; i < process.max_open_file_descriptors(); ++i) {
        auto* description = process.file_description(i);
        if (!description)
            continue;
        bool cloexec = process.fd_flags(i) & FD_CLOEXEC;

        auto description_object = array.add_object();
        description_object.add("fd", i);
        description_object.add("absolute_path", description->absolute_path());
        description_object.add("seekable", description->file().is_seekable());
        description_object.add("class", description->file().class_name());
        description_object.add("offset", description->offset());
        description_object.add("cloexec", cloexec);
        description_object.add("blocking", description->is_blocking());
        description_object.add("can_read", description->can_read());
        description_object.add("can_write", description->can_write());
    }
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$pid_fd_entry(InodeIdentifier identifier)
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

Optional<KBuffer> procfs$pid_vm(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    for (auto& region : process.regions()) {
        auto region_object = array.add_object();
        region_object.add("readable", region.is_readable());
        region_object.add("writable", region.is_writable());
        region_object.add("stack", region.is_stack());
        region_object.add("shared", region.is_shared());
        region_object.add("purgeable", region.vmobject().is_purgeable());
        if (region.vmobject().is_purgeable()) {
            region_object.add("volatile", static_cast<const PurgeableVMObject&>(region.vmobject()).is_volatile());
        }
        region_object.add("purgeable", region.vmobject().is_purgeable());
        region_object.add("address", region.vaddr().get());
        region_object.add("size", (u32)region.size());
        region_object.add("amount_resident", (u32)region.amount_resident());
        region_object.add("name", region.name());
    }
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$pci(InodeIdentifier)
{
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    PCI::enumerate_all([&array](PCI::Address address, PCI::ID id) {
        auto obj = array.add_object();
        obj.add("bus", address.bus());
        obj.add("slot", address.slot());
        obj.add("function", address.function());
        obj.add("vendor_id", id.vendor_id);
        obj.add("device_id", id.device_id);
        obj.add("revision_id", PCI::get_revision_id(address));
        obj.add("subclass", PCI::get_subclass(address));
        obj.add("class", PCI::get_class(address));
        obj.add("subsystem_id", PCI::get_subsystem_id(address));
        obj.add("subsystem_vendor_id", PCI::get_subsystem_vendor_id(address));
    });
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$devices(InodeIdentifier)
{
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    Device::for_each([&array](auto& device) {
        auto obj = array.add_object();
        obj.add("major", device.major());
        obj.add("minor", device.minor());
        obj.add("class_name", device.class_name());

        if (device.is_block_device())
            obj.add("type", "block");
        else if (device.is_character_device())
            obj.add("type", "character");
        else
            ASSERT_NOT_REACHED();
    });
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$uptime(InodeIdentifier)
{
    KBufferBuilder builder;
    builder.appendf("%u\n", (u32)(g_uptime / 1000));
    return builder.build();
}

Optional<KBuffer> procfs$cmdline(InodeIdentifier)
{
    KBufferBuilder builder;
    builder.appendf("%s\n", KParams::the().cmdline().characters());
    return builder.build();
}

Optional<KBuffer> procfs$modules(InodeIdentifier)
{
    extern HashMap<String, OwnPtr<Module>>* g_modules;
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    for (auto& it : *g_modules) {
        auto obj = array.add_object();
        obj.add("name", it.value->name);
        obj.add("module_init", (u32)it.value->module_init);
        obj.add("module_fini", (u32)it.value->module_fini);
        u32 size = 0;
        for (auto& section : it.value->sections) {
            size += section.capacity();
        }
        obj.add("size", size);
    }
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$net_adapters(InodeIdentifier)
{
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    NetworkAdapter::for_each([&array](auto& adapter) {
        auto obj = array.add_object();
        obj.add("name", adapter.name());
        obj.add("class_name", adapter.class_name());
        obj.add("mac_address", adapter.mac_address().to_string());
        if (!adapter.ipv4_address().is_zero()) {
            obj.add("ipv4_address", adapter.ipv4_address().to_string());
            obj.add("ipv4_netmask", adapter.ipv4_netmask().to_string());
        }
        if (!adapter.ipv4_gateway().is_zero())
            obj.add("ipv4_gateway", adapter.ipv4_gateway().to_string());
        obj.add("packets_in", adapter.packets_in());
        obj.add("bytes_in", adapter.bytes_in());
        obj.add("packets_out", adapter.packets_out());
        obj.add("bytes_out", adapter.bytes_out());
        obj.add("link_up", adapter.link_up());
        obj.add("mtu", adapter.mtu());
    });
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$net_arp(InodeIdentifier)
{
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    LOCKER(arp_table().lock());
    for (auto& it : arp_table().resource()) {
        auto obj = array.add_object();
        obj.add("mac_address", it.value.to_string());
        obj.add("ip_address", it.key.to_string());
    }
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$net_tcp(InodeIdentifier)
{
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    TCPSocket::for_each([&array](auto& socket) {
        auto obj = array.add_object();
        obj.add("local_address", socket.local_address().to_string());
        obj.add("local_port", socket.local_port());
        obj.add("peer_address", socket.peer_address().to_string());
        obj.add("peer_port", socket.peer_port());
        obj.add("state", TCPSocket::to_string(socket.state()));
        obj.add("ack_number", socket.ack_number());
        obj.add("sequence_number", socket.sequence_number());
        obj.add("packets_in", socket.packets_in());
        obj.add("bytes_in", socket.bytes_in());
        obj.add("packets_out", socket.packets_out());
        obj.add("bytes_out", socket.bytes_out());
    });
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$net_udp(InodeIdentifier)
{
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    UDPSocket::for_each([&array](auto& socket) {
        auto obj = array.add_object();
        obj.add("local_address", socket.local_address().to_string());
        obj.add("local_port", socket.local_port());
        obj.add("peer_address", socket.peer_address().to_string());
        obj.add("peer_port", socket.peer_port());
    });
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$net_local(InodeIdentifier)
{
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    LocalSocket::for_each([&array](auto& socket) {
        auto obj = array.add_object();
        obj.add("path", String(socket.socket_path()));
        obj.add("origin_pid", socket.origin_pid());
        obj.add("origin_uid", socket.origin_uid());
        obj.add("origin_gid", socket.origin_gid());
        obj.add("acceptor_pid", socket.acceptor_pid());
        obj.add("acceptor_uid", socket.acceptor_uid());
        obj.add("acceptor_gid", socket.acceptor_gid());
    });
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$pid_vmo(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    KBufferBuilder builder;
    builder.appendf("BEGIN       END         SIZE        NAME\n");
    for (auto& region : process.regions()) {
        builder.appendf("%x -- %x    %x    %s\n",
            region.vaddr().get(),
            region.vaddr().offset(region.size() - 1).get(),
            region.size(),
            region.name().characters());
        builder.appendf("VMO: %s @ %x(%u)\n",
            region.vmobject().is_anonymous() ? "anonymous" : "file-backed",
            &region.vmobject(),
            region.vmobject().ref_count());
        for (size_t i = 0; i < region.vmobject().page_count(); ++i) {
            auto& physical_page = region.vmobject().physical_pages()[i];
            builder.appendf("P%x%s(%u) ",
                physical_page ? physical_page->paddr().get() : 0,
                region.should_cow(i) ? "!" : "",
                physical_page ? physical_page->ref_count() : 0);
        }
        builder.appendf("\n");
    }
    return builder.build();
}

Optional<KBuffer> procfs$pid_stack(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    return process.backtrace(*handle);
}

Optional<KBuffer> procfs$pid_regs(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    KBufferBuilder builder;
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
    return builder.build();
}

Optional<KBuffer> procfs$pid_exe(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    auto& process = handle->process();
    auto* custody = process.executable();
    ASSERT(custody);
    return custody->absolute_path().to_byte_buffer();
}

Optional<KBuffer> procfs$pid_cwd(InodeIdentifier identifier)
{
    auto handle = ProcessInspectionHandle::from_pid(to_pid(identifier));
    if (!handle)
        return {};
    return handle->process().current_directory().absolute_path().to_byte_buffer();
}

Optional<KBuffer> procfs$self(InodeIdentifier)
{
    char buffer[16];
    sprintf(buffer, "%u", current->pid());
    return KBuffer::copy((const u8*)buffer, strlen(buffer));
}

Optional<KBuffer> procfs$mm(InodeIdentifier)
{
    InterruptDisabler disabler;
    KBufferBuilder builder;
    u32 vmobject_count = 0;
    MemoryManager::for_each_vmobject([&](auto& vmobject) {
        ++vmobject_count;
        builder.appendf("VMObject: %p %s(%u): p:%4u\n",
            &vmobject,
            vmobject.is_anonymous() ? "anon" : "file",
            vmobject.ref_count(),
            vmobject.page_count());
        return IterationDecision::Continue;
    });
    builder.appendf("VMO count: %u\n", vmobject_count);
    builder.appendf("Free physical pages: %u\n", MM.user_physical_pages() - MM.user_physical_pages_used());
    builder.appendf("Free supervisor physical pages: %u\n", MM.super_physical_pages() - MM.super_physical_pages_used());
    return builder.build();
}

Optional<KBuffer> procfs$dmesg(InodeIdentifier)
{
    InterruptDisabler disabler;
    KBufferBuilder builder;
    for (char ch : Console::the().logbuffer())
        builder.append(ch);
    return builder.build();
}

Optional<KBuffer> procfs$mounts(InodeIdentifier)
{
    // FIXME: This is obviously racy against the VFS mounts changing.
    KBufferBuilder builder;
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
    return builder.build();
}

Optional<KBuffer> procfs$df(InodeIdentifier)
{
    // FIXME: This is obviously racy against the VFS mounts changing.
    KBufferBuilder builder;
    JsonArraySerializer array { builder };
    VFS::the().for_each_mount([&array](auto& mount) {
        auto& fs = mount.guest_fs();
        auto fs_object = array.add_object();
        fs_object.add("class_name", fs.class_name());
        fs_object.add("total_block_count", fs.total_block_count());
        fs_object.add("free_block_count", fs.free_block_count());
        fs_object.add("total_inode_count", fs.total_inode_count());
        fs_object.add("free_inode_count", fs.free_inode_count());
        fs_object.add("mount_point", mount.absolute_path());
        fs_object.add("block_size", fs.block_size());
        fs_object.add("readonly", fs.is_readonly());

        if (fs.is_disk_backed())
            fs_object.add("device", static_cast<const DiskBackedFS&>(fs).device().absolute_path());
        else
            fs_object.add("device", fs.class_name());
    });
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$cpuinfo(InodeIdentifier)
{
    KBufferBuilder builder;
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
    return builder.build();
}

Optional<KBuffer> procfs$memstat(InodeIdentifier)
{
    InterruptDisabler disabler;
    KBufferBuilder builder;
    JsonObjectSerializer<KBufferBuilder> json { builder };
    json.add("kmalloc_allocated", (u32)sum_alloc);
    json.add("kmalloc_available", (u32)sum_free);
    json.add("kmalloc_eternal_allocated", (u32)kmalloc_sum_eternal);
    json.add("user_physical_allocated", MM.user_physical_pages_used());
    json.add("user_physical_available", MM.user_physical_pages());
    json.add("super_physical_allocated", MM.super_physical_pages_used());
    json.add("super_physical_available", MM.super_physical_pages());
    json.add("kmalloc_call_count", g_kmalloc_call_count);
    json.add("kfree_call_count", g_kfree_call_count);
    slab_alloc_stats([&json](size_t slab_size, size_t num_allocated, size_t num_free) {
        auto prefix = String::format("slab_%zu", slab_size);
        json.add(String::format("%s_num_allocated", prefix.characters()), (u32)num_allocated);
        json.add(String::format("%s_num_free", prefix.characters()), (u32)num_free);
    });
    json.finish();
    return builder.build();
}

Optional<KBuffer> procfs$all(InodeIdentifier)
{
    InterruptDisabler disabler;
    auto processes = Process::all_processes();
    KBufferBuilder builder;
    JsonArraySerializer array { builder };

    // Keep this in sync with CProcessStatistics.
    auto build_process = [&](const Process& process) {
        auto process_object = array.add_object();
        process_object.add("pid", process.pid());
        process_object.add("pgid", process.tty() ? process.tty()->pgid() : 0);
        process_object.add("pgp", process.pgid());
        process_object.add("sid", process.sid());
        process_object.add("uid", process.uid());
        process_object.add("gid", process.gid());
        process_object.add("ppid", process.ppid());
        process_object.add("nfds", process.number_of_open_file_descriptors());
        process_object.add("name", process.name());
        process_object.add("tty", process.tty() ? process.tty()->tty_name() : "notty");
        process_object.add("amount_virtual", (u32)process.amount_virtual());
        process_object.add("amount_resident", (u32)process.amount_resident());
        process_object.add("amount_shared", (u32)process.amount_shared());
        process_object.add("amount_purgeable_volatile", (u32)process.amount_purgeable_volatile());
        process_object.add("amount_purgeable_nonvolatile", (u32)process.amount_purgeable_nonvolatile());
        process_object.add("icon_id", process.icon_id());
        auto thread_array = process_object.add_array("threads");
        process.for_each_thread([&](const Thread& thread) {
            auto thread_object = thread_array.add_object();
            thread_object.add("tid", thread.tid());
            thread_object.add("name", thread.name());
            thread_object.add("times_scheduled", thread.times_scheduled());
            thread_object.add("ticks", thread.ticks());
            thread_object.add("state", thread.state_string());
            thread_object.add("priority", to_string(thread.priority()));
            thread_object.add("syscall_count", thread.syscall_count());
            thread_object.add("inode_faults", thread.inode_faults());
            thread_object.add("zero_faults", thread.zero_faults());
            thread_object.add("cow_faults", thread.cow_faults());
            thread_object.add("file_read_bytes", thread.file_read_bytes());
            thread_object.add("file_write_bytes", thread.file_write_bytes());
            thread_object.add("unix_socket_read_bytes", thread.unix_socket_read_bytes());
            thread_object.add("unix_socket_write_bytes", thread.unix_socket_write_bytes());
            thread_object.add("ipv4_socket_read_bytes", thread.ipv4_socket_read_bytes());
            thread_object.add("ipv4_socket_write_bytes", thread.ipv4_socket_write_bytes());
            return IterationDecision::Continue;
        });
    };
    build_process(*Scheduler::colonel());
    for (auto* process : processes)
        build_process(*process);
    array.finish();
    return builder.build();
}

Optional<KBuffer> procfs$inodes(InodeIdentifier)
{
    extern InlineLinkedList<Inode>& all_inodes();
    KBufferBuilder builder;
    InterruptDisabler disabler;
    for (auto& inode : all_inodes()) {
        builder.appendf("Inode{K%x} %02u:%08u (%u)\n", &inode, inode.fsid(), inode.index(), inode.ref_count());
    }
    return builder.build();
}

struct SysVariable {
    String name;
    enum class Type : u8 {
        Invalid,
        Boolean,
        String,
    };
    Type type { Type::Invalid };
    Function<void()> notify_callback;
    void* address { nullptr };

    static SysVariable& for_inode(InodeIdentifier);

    void notify()
    {
        if (notify_callback)
            notify_callback();
    }
};

static Vector<SysVariable, 16>* s_sys_variables;

static inline Vector<SysVariable, 16>& sys_variables()
{
    if (s_sys_variables == nullptr) {
        s_sys_variables = new Vector<SysVariable, 16>;
        s_sys_variables->append({ "", SysVariable::Type::Invalid, nullptr, nullptr });
    }
    return *s_sys_variables;
}

SysVariable& SysVariable::for_inode(InodeIdentifier id)
{
    auto index = to_sys_index(id);
    if (index >= sys_variables().size())
        return sys_variables()[0];
    auto& variable = sys_variables()[index];
    ASSERT(variable.address);
    return variable;
}

static ByteBuffer read_sys_bool(InodeIdentifier inode_id)
{
    auto& variable = SysVariable::for_inode(inode_id);
    ASSERT(variable.type == SysVariable::Type::Boolean);

    auto buffer = ByteBuffer::create_uninitialized(2);
    auto* lockable_bool = reinterpret_cast<Lockable<bool>*>(variable.address);
    {
        LOCKER(lockable_bool->lock());
        buffer[0] = lockable_bool->resource() ? '1' : '0';
    }
    buffer[1] = '\n';
    return buffer;
}

static ssize_t write_sys_bool(InodeIdentifier inode_id, const ByteBuffer& data)
{
    auto& variable = SysVariable::for_inode(inode_id);
    ASSERT(variable.type == SysVariable::Type::Boolean);

    if (data.is_empty() || !(data[0] == '0' || data[0] == '1'))
        return data.size();

    auto* lockable_bool = reinterpret_cast<Lockable<bool>*>(variable.address);
    {
        LOCKER(lockable_bool->lock());
        lockable_bool->resource() = data[0] == '1';
    }
    variable.notify();
    return data.size();
}

static ByteBuffer read_sys_string(InodeIdentifier inode_id)
{
    auto& variable = SysVariable::for_inode(inode_id);
    ASSERT(variable.type == SysVariable::Type::String);

    auto* lockable_string = reinterpret_cast<Lockable<String>*>(variable.address);
    LOCKER(lockable_string->lock());
    return lockable_string->resource().to_byte_buffer();
}

static ssize_t write_sys_string(InodeIdentifier inode_id, const ByteBuffer& data)
{
    auto& variable = SysVariable::for_inode(inode_id);
    ASSERT(variable.type == SysVariable::Type::String);

    {
        auto* lockable_string = reinterpret_cast<Lockable<String>*>(variable.address);
        LOCKER(lockable_string->lock());
        lockable_string->resource() = String((const char*)data.data(), data.size());
    }
    variable.notify();
    return data.size();
}

void ProcFS::add_sys_bool(String&& name, Lockable<bool>& var, Function<void()>&& notify_callback)
{
    InterruptDisabler disabler;

    SysVariable variable;
    variable.name = move(name);
    variable.type = SysVariable::Type::Boolean;
    variable.notify_callback = move(notify_callback);
    variable.address = &var;

    sys_variables().append(move(variable));
}

void ProcFS::add_sys_string(String&& name, Lockable<String>& var, Function<void()>&& notify_callback)
{
    InterruptDisabler disabler;

    SysVariable variable;
    variable.name = move(name);
    variable.type = SysVariable::Type::String;
    variable.notify_callback = move(notify_callback);
    variable.address = &var;

    sys_variables().append(move(variable));
}

bool ProcFS::initialize()
{
    static Lockable<bool>* kmalloc_stack_helper;

    if (kmalloc_stack_helper == nullptr) {
        kmalloc_stack_helper = new Lockable<bool>();
        kmalloc_stack_helper->resource() = g_dump_kmalloc_stacks;
        ProcFS::add_sys_bool("kmalloc_stacks", *kmalloc_stack_helper, [] {
            g_dump_kmalloc_stacks = kmalloc_stack_helper->resource();
        });
    }
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

    switch (proc_file_type) {
    case FI_Root_self:
    case FI_PID_cwd:
    case FI_PID_exe:
        metadata.mode = 0120777;
        break;
    case FI_Root:
    case FI_Root_sys:
    case FI_Root_net:
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

    Function<Optional<KBuffer>(InodeIdentifier)> callback_tmp;
    Function<Optional<KBuffer>(InodeIdentifier)>* read_callback { nullptr };
    if (directory_entry)
        read_callback = &directory_entry->read_callback;
    else
        switch (to_proc_parent_directory(identifier())) {
        case PDI_PID_fd:
            callback_tmp = procfs$pid_fd_entry;
            read_callback = &callback_tmp;
            break;
        case PDI_Root_sys:
            switch (SysVariable::for_inode(identifier()).type) {
            case SysVariable::Type::Invalid:
                ASSERT_NOT_REACHED();
            case SysVariable::Type::Boolean:
                callback_tmp = read_sys_bool;
                break;
            case SysVariable::Type::String:
                callback_tmp = read_sys_string;
                break;
            }
            read_callback = &callback_tmp;
            break;
        default:
            ASSERT_NOT_REACHED();
        }

    ASSERT(read_callback);

    Optional<KBuffer> generated_data;
    if (!description) {
        generated_data = (*read_callback)(identifier());
    } else {
        if (!description->generator_cache())
            description->generator_cache() = (*read_callback)(identifier());
        generated_data = description->generator_cache();
    }

    auto& data = generated_data;
    ssize_t nread = 0;
    if (data.has_value()) {
        nread = min(static_cast<off_t>(data.value().size() - offset), static_cast<off_t>(count));
        memcpy(buffer, data.value().data() + offset, nread);
        if (nread == 0 && description && description->generator_cache())
            description->generator_cache().clear();
    }

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
                callback({ entry.name, strlen(entry.name), to_identifier(fsid(), PDI_Root, 0, (ProcFileType)entry.proc_file_type), 0 });
        }
        for (auto pid_child : Process::all_pids()) {
            char name[16];
            size_t name_length = (size_t)sprintf(name, "%u", pid_child);
            callback({ name, name_length, to_identifier(fsid(), PDI_Root, pid_child, FI_PID), 0 });
        }
        break;

    case FI_Root_sys:
        for (int i = 1; i < sys_variables().size(); ++i) {
            auto& variable = sys_variables()[i];
            callback({ variable.name.characters(), variable.name.length(), sys_var_to_identifier(fsid(), i), 0 });
        }
        break;

    case FI_Root_net:
        callback({ "adapters", 8, to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_adapters), 0 });
        callback({ "arp", 3, to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_arp), 0 });
        callback({ "tcp", 3, to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_tcp), 0 });
        callback({ "udp", 3, to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_udp), 0 });
        callback({ "local", 5, to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_local), 0 });
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
                callback({ entry.name, strlen(entry.name), to_identifier(fsid(), PDI_PID, pid, (ProcFileType)entry.proc_file_type), 0 });
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
            size_t name_length = (size_t)sprintf(name, "%u", i);
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
        for (int i = 1; i < sys_variables().size(); ++i) {
            auto& variable = sys_variables()[i];
            if (name == variable.name)
                return sys_var_to_identifier(fsid(), i);
        }
        return {};
    }

    if (proc_file_type == FI_Root_net) {
        if (name == "adapters")
            return to_identifier(fsid(), PDI_Root, 0, FI_Root_net_adapters);
        if (name == "arp")
            return to_identifier(fsid(), PDI_Root, 0, FI_Root_net_arp);
        if (name == "tcp")
            return to_identifier(fsid(), PDI_Root, 0, FI_Root_net_tcp);
        if (name == "udp")
            return to_identifier(fsid(), PDI_Root, 0, FI_Root_net_udp);
        if (name == "local")
            return to_identifier(fsid(), PDI_Root, 0, FI_Root_net_local);
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

    Function<ssize_t(InodeIdentifier, const ByteBuffer&)> callback_tmp;
    Function<ssize_t(InodeIdentifier, const ByteBuffer&)>* write_callback { nullptr };

    if (directory_entry == nullptr) {
        if (to_proc_parent_directory(identifier()) == PDI_Root_sys) {
            switch (SysVariable::for_inode(identifier()).type) {
            case SysVariable::Type::Invalid:
                ASSERT_NOT_REACHED();
            case SysVariable::Type::Boolean:
                callback_tmp = write_sys_bool;
                break;
            case SysVariable::Type::String:
                callback_tmp = write_sys_string;
                break;
            }
            write_callback = &callback_tmp;
        } else
            return -EPERM;
    } else {
        if (!directory_entry->write_callback)
            return -EPERM;
        write_callback = &directory_entry->write_callback;
    }

    ASSERT(is_persistent_inode(identifier()));
    // FIXME: Being able to write into ProcFS at a non-zero offset seems like something we should maybe support..
    ASSERT(offset == 0);
    bool success = (*write_callback)(identifier(), ByteBuffer::wrap(buffer, size));
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
    m_root_inode = adopt(*new ProcFSInode(*this, 1));
    m_entries.resize(FI_MaxStaticFileIndex);
    m_entries[FI_Root_mm] = { "mm", FI_Root_mm, procfs$mm };
    m_entries[FI_Root_mounts] = { "mounts", FI_Root_mounts, procfs$mounts };
    m_entries[FI_Root_df] = { "df", FI_Root_df, procfs$df };
    m_entries[FI_Root_all] = { "all", FI_Root_all, procfs$all };
    m_entries[FI_Root_memstat] = { "memstat", FI_Root_memstat, procfs$memstat };
    m_entries[FI_Root_cpuinfo] = { "cpuinfo", FI_Root_cpuinfo, procfs$cpuinfo };
    m_entries[FI_Root_inodes] = { "inodes", FI_Root_inodes, procfs$inodes };
    m_entries[FI_Root_dmesg] = { "dmesg", FI_Root_dmesg, procfs$dmesg };
    m_entries[FI_Root_self] = { "self", FI_Root_self, procfs$self };
    m_entries[FI_Root_pci] = { "pci", FI_Root_pci, procfs$pci };
    m_entries[FI_Root_devices] = { "devices", FI_Root_devices, procfs$devices };
    m_entries[FI_Root_uptime] = { "uptime", FI_Root_uptime, procfs$uptime };
    m_entries[FI_Root_cmdline] = { "cmdline", FI_Root_cmdline, procfs$cmdline };
    m_entries[FI_Root_modules] = { "modules", FI_Root_modules, procfs$modules };
    m_entries[FI_Root_sys] = { "sys", FI_Root_sys };
    m_entries[FI_Root_net] = { "net", FI_Root_net };

    m_entries[FI_Root_net_adapters] = { "adapters", FI_Root_net_adapters, procfs$net_adapters };
    m_entries[FI_Root_net_arp] = { "arp", FI_Root_net_arp, procfs$net_arp };
    m_entries[FI_Root_net_tcp] = { "tcp", FI_Root_net_tcp, procfs$net_tcp };
    m_entries[FI_Root_net_udp] = { "udp", FI_Root_net_udp, procfs$net_udp };
    m_entries[FI_Root_net_local] = { "local", FI_Root_net_local, procfs$net_local };

    m_entries[FI_PID_vm] = { "vm", FI_PID_vm, procfs$pid_vm };
    m_entries[FI_PID_vmo] = { "vmo", FI_PID_vmo, procfs$pid_vmo };
    m_entries[FI_PID_stack] = { "stack", FI_PID_stack, procfs$pid_stack };
    m_entries[FI_PID_regs] = { "regs", FI_PID_regs, procfs$pid_regs };
    m_entries[FI_PID_fds] = { "fds", FI_PID_fds, procfs$pid_fds };
    m_entries[FI_PID_exe] = { "exe", FI_PID_exe, procfs$pid_exe };
    m_entries[FI_PID_cwd] = { "cwd", FI_PID_cwd, procfs$pid_cwd };
    m_entries[FI_PID_fd] = { "fd", FI_PID_fd };
}

ProcFS::ProcFSDirectoryEntry* ProcFS::get_directory_entry(InodeIdentifier identifier) const
{
    auto proc_file_type = to_proc_file_type(identifier);
    if (proc_file_type != FI_Invalid && proc_file_type != FI_Root_sys_variable && proc_file_type < FI_MaxStaticFileIndex)
        return const_cast<ProcFSDirectoryEntry*>(&m_entries[proc_file_type]);
    return nullptr;
}

KResult ProcFSInode::chown(uid_t, gid_t)
{
    return KResult(-EPERM);
}
