/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/ProcessorInfo.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Console.h>
#include <Kernel/DMI.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/KSyms.h>
#include <Kernel/Module.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>
#include <Kernel/StdLib.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

enum ProcParentDirectory {
    PDI_AbstractRoot = 0,
    PDI_Root,
    PDI_Root_sys,
    PDI_Root_net,
    PDI_PID,
    PDI_PID_fd,
    PDI_PID_stacks,
};
static_assert(PDI_PID_stacks < 16, "Too many directories for identifier scheme");

enum ProcFileType {
    FI_Invalid = 0,

    FI_Root = 1, // directory

    __FI_Root_Start,
    FI_Root_df,
    FI_Root_all,
    FI_Root_memstat,
    FI_Root_cpuinfo,
    FI_Root_dmesg,
    FI_Root_interrupts,
    FI_Root_dmi,
    FI_Root_smbios_entry_point,
    FI_Root_keymap,
    FI_Root_pci,
    FI_Root_devices,
    FI_Root_uptime,
    FI_Root_cmdline,
    FI_Root_modules,
    FI_Root_profile,
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
    FI_PID_perf_events,
    FI_PID_vm,
    FI_PID_stacks, // directory
    FI_PID_fds,
    FI_PID_unveil,
    FI_PID_exe,  // symlink
    FI_PID_cwd,  // symlink
    FI_PID_root, // symlink
    FI_PID_fd,   // directory
    __FI_PID_End,

    FI_MaxStaticFileIndex,
};

static inline ProcessID to_pid(const InodeIdentifier& identifier)
{
    return identifier.index().value() >> 16u;
}

static inline ThreadID to_tid(const InodeIdentifier& identifier)
{
    // Sneakily, use the exact same mechanism.
    return to_pid(identifier).value();
}

static inline ProcParentDirectory to_proc_parent_directory(const InodeIdentifier& identifier)
{
    return (ProcParentDirectory)((identifier.index().value() >> 12) & 0xf);
}

static inline ProcFileType to_proc_file_type(const InodeIdentifier& identifier)
{
    return (ProcFileType)(identifier.index().value() & 0xff);
}

static inline int to_fd(const InodeIdentifier& identifier)
{
    ASSERT(to_proc_parent_directory(identifier) == PDI_PID_fd);
    return (identifier.index().value() & 0xff) - FI_MaxStaticFileIndex;
}

static inline size_t to_sys_index(const InodeIdentifier& identifier)
{
    ASSERT(to_proc_parent_directory(identifier) == PDI_Root_sys);
    ASSERT(to_proc_file_type(identifier) == FI_Root_sys_variable);
    return identifier.index().value() >> 16u;
}

static inline InodeIdentifier to_identifier(unsigned fsid, ProcParentDirectory parent, ProcessID pid, ProcFileType proc_file_type)
{
    return { fsid, ((unsigned)parent << 12u) | ((unsigned)pid.value() << 16u) | (unsigned)proc_file_type };
}

static inline InodeIdentifier to_identifier_with_fd(unsigned fsid, ProcessID pid, int fd)
{
    return { fsid, (PDI_PID_fd << 12u) | ((unsigned)pid.value() << 16u) | (FI_MaxStaticFileIndex + fd) };
}

static inline InodeIdentifier to_identifier_with_stack(unsigned fsid, ThreadID tid)
{
    return { fsid, (PDI_PID_stacks << 12u) | ((unsigned)tid.value() << 16u) | FI_MaxStaticFileIndex };
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
    case PDI_PID_stacks:
        return to_identifier(identifier.fsid(), PDI_PID, to_pid(identifier), FI_PID_stacks);
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

static inline bool is_thread_related_file(const InodeIdentifier& identifier)
{
    auto proc_parent_directory = to_proc_parent_directory(identifier);
    return proc_parent_directory == PDI_PID_stacks;
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
    case FI_PID_stacks:
        return true;
    default:
        return false;
    }
}

static inline bool is_persistent_inode(const InodeIdentifier& identifier)
{
    return to_proc_parent_directory(identifier) == PDI_Root_sys;
}

struct ProcFSInodeData : public FileDescriptionData {
    RefPtr<KBufferImpl> buffer;
};

NonnullRefPtr<ProcFS> ProcFS::create()
{
    return adopt(*new ProcFS);
}

ProcFS::~ProcFS()
{
}

static bool procfs$pid_fds(InodeIdentifier identifier, KBufferBuilder& builder)
{
    JsonArraySerializer array { builder };

    auto process = Process::from_pid(to_pid(identifier));
    if (!process) {
        array.finish();
        return true;
    }
    if (process->number_of_open_file_descriptors() == 0) {
        array.finish();
        return true;
    }

    for (int i = 0; i < process->max_open_file_descriptors(); ++i) {
        auto description = process->file_description(i);
        if (!description)
            continue;
        bool cloexec = process->fd_flags(i) & FD_CLOEXEC;

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
    return true;
}

static bool procfs$pid_fd_entry(InodeIdentifier identifier, KBufferBuilder& builder)
{
    auto process = Process::from_pid(to_pid(identifier));
    if (!process)
        return false;
    int fd = to_fd(identifier);
    auto description = process->file_description(fd);
    if (!description)
        return false;
    builder.append_bytes(description->absolute_path().bytes());
    return true;
}

static bool procfs$pid_vm(InodeIdentifier identifier, KBufferBuilder& builder)
{
    auto process = Process::from_pid(to_pid(identifier));
    if (!process)
        return false;
    JsonArraySerializer array { builder };
    {
        ScopedSpinLock lock(process->space().get_lock());
        for (auto& region : process->space().regions()) {
            if (!region.is_user() && !Process::current()->is_superuser())
                continue;
            auto region_object = array.add_object();
            region_object.add("readable", region.is_readable());
            region_object.add("writable", region.is_writable());
            region_object.add("executable", region.is_executable());
            region_object.add("stack", region.is_stack());
            region_object.add("shared", region.is_shared());
            region_object.add("syscall", region.is_syscall_region());
            region_object.add("purgeable", region.vmobject().is_anonymous());
            if (region.vmobject().is_anonymous()) {
                region_object.add("volatile", static_cast<const AnonymousVMObject&>(region.vmobject()).is_any_volatile());
            }
            region_object.add("cacheable", region.is_cacheable());
            region_object.add("address", region.vaddr().get());
            region_object.add("size", region.size());
            region_object.add("amount_resident", region.amount_resident());
            region_object.add("amount_dirty", region.amount_dirty());
            region_object.add("cow_pages", region.cow_pages());
            region_object.add("name", region.name());
            region_object.add("vmobject", region.vmobject().class_name());

            StringBuilder pagemap_builder;
            for (size_t i = 0; i < region.page_count(); ++i) {
                auto* page = region.physical_page(i);
                if (!page)
                    pagemap_builder.append('N');
                else if (page->is_shared_zero_page() || page->is_lazy_committed_page())
                    pagemap_builder.append('Z');
                else
                    pagemap_builder.append('P');
            }
            region_object.add("pagemap", pagemap_builder.to_string());
        }
    }
    array.finish();
    return true;
}

static bool procfs$pci(InodeIdentifier, KBufferBuilder& builder)
{
    JsonArraySerializer array { builder };
    PCI::enumerate([&array](PCI::Address address, PCI::ID id) {
        auto obj = array.add_object();
        obj.add("seg", address.seg());
        obj.add("bus", address.bus());
        obj.add("device", address.device());
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
    return true;
}

static bool procfs$dmi(InodeIdentifier, KBufferBuilder& builder)
{
    if (!DMIExpose::the().is_available())
        return false;
    auto structures_ptr = DMIExpose::the().structure_table();
    builder.append_bytes(ReadonlyBytes { structures_ptr->data(), structures_ptr->size() });
    return true;
}

static bool procfs$smbios_entry_point(InodeIdentifier, KBufferBuilder& builder)
{
    if (!DMIExpose::the().is_available())
        return false;
    auto structures_ptr = DMIExpose::the().entry_point();
    builder.append_bytes(ReadonlyBytes { structures_ptr->data(), structures_ptr->size() });
    return true;
}

static bool procfs$interrupts(InodeIdentifier, KBufferBuilder& builder)
{
    JsonArraySerializer array { builder };
    InterruptManagement::the().enumerate_interrupt_handlers([&array](GenericInterruptHandler& handler) {
        auto obj = array.add_object();
        obj.add("purpose", handler.purpose());
        obj.add("interrupt_line", handler.interrupt_number());
        obj.add("controller", handler.controller());
        obj.add("cpu_handler", 0); // FIXME: Determine the responsible CPU for each interrupt handler.
        obj.add("device_sharing", (unsigned)handler.sharing_devices_count());
        obj.add("call_count", (unsigned)handler.get_invoking_count());
    });
    array.finish();
    return true;
}

static bool procfs$keymap(InodeIdentifier, KBufferBuilder& builder)
{
    JsonObjectSerializer<KBufferBuilder> json { builder };
    json.add("keymap", KeyboardDevice::the().keymap_name());
    json.finish();
    return true;
}

static bool procfs$devices(InodeIdentifier, KBufferBuilder& builder)
{
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
    return true;
}

static bool procfs$uptime(InodeIdentifier, KBufferBuilder& builder)
{
    builder.appendff("{}\n", TimeManagement::the().uptime_ms() / 1000);
    return true;
}

static bool procfs$cmdline(InodeIdentifier, KBufferBuilder& builder)
{
    builder.append(kernel_command_line().string());
    builder.append('\n');
    return true;
}

static bool procfs$modules(InodeIdentifier, KBufferBuilder& builder)
{
    extern HashMap<String, OwnPtr<Module>>* g_modules;
    JsonArraySerializer array { builder };
    for (auto& it : *g_modules) {
        auto obj = array.add_object();
        obj.add("name", it.value->name);
        obj.add("module_init", it.value->module_init);
        obj.add("module_fini", it.value->module_fini);
        u32 size = 0;
        for (auto& section : it.value->sections) {
            size += section.capacity();
        }
        obj.add("size", size);
    }
    array.finish();
    return true;
}

static bool procfs$pid_perf_events(InodeIdentifier identifier, KBufferBuilder& builder)
{
    auto process = Process::from_pid(to_pid(identifier));
    if (!process)
        return false;

    InterruptDisabler disabler;

    if (!process->executable())
        return false;

    if (!process->perf_events())
        return false;

    return process->perf_events()->to_json(builder, process->pid(), process->executable()->absolute_path());
}

static bool procfs$net_adapters(InodeIdentifier, KBufferBuilder& builder)
{
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
    return true;
}

static bool procfs$net_arp(InodeIdentifier, KBufferBuilder& builder)
{
    JsonArraySerializer array { builder };
    LOCKER(arp_table().lock(), Lock::Mode::Shared);
    for (auto& it : arp_table().resource()) {
        auto obj = array.add_object();
        obj.add("mac_address", it.value.to_string());
        obj.add("ip_address", it.key.to_string());
    }
    array.finish();
    return true;
}

static bool procfs$net_tcp(InodeIdentifier, KBufferBuilder& builder)
{
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
    return true;
}

static bool procfs$net_udp(InodeIdentifier, KBufferBuilder& builder)
{
    JsonArraySerializer array { builder };
    UDPSocket::for_each([&array](auto& socket) {
        auto obj = array.add_object();
        obj.add("local_address", socket.local_address().to_string());
        obj.add("local_port", socket.local_port());
        obj.add("peer_address", socket.peer_address().to_string());
        obj.add("peer_port", socket.peer_port());
    });
    array.finish();
    return true;
}

static bool procfs$net_local(InodeIdentifier, KBufferBuilder& builder)
{
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
    return true;
}

static bool procfs$pid_unveil(InodeIdentifier identifier, KBufferBuilder& builder)
{
    auto process = Process::from_pid(to_pid(identifier));
    if (!process)
        return false;
    JsonArraySerializer array { builder };
    for (auto& unveiled_path : process->unveiled_paths()) {
        if (!unveiled_path.was_explicitly_unveiled())
            continue;
        auto obj = array.add_object();
        obj.add("path", unveiled_path.path());
        StringBuilder permissions_builder;
        if (unveiled_path.permissions() & UnveilAccess::Read)
            permissions_builder.append('r');
        if (unveiled_path.permissions() & UnveilAccess::Write)
            permissions_builder.append('w');
        if (unveiled_path.permissions() & UnveilAccess::Execute)
            permissions_builder.append('x');
        if (unveiled_path.permissions() & UnveilAccess::CreateOrRemove)
            permissions_builder.append('c');
        if (unveiled_path.permissions() & UnveilAccess::Browse)
            permissions_builder.append('b');
        obj.add("permissions", permissions_builder.to_string());
    }
    array.finish();
    return true;
}

static bool procfs$tid_stack(InodeIdentifier identifier, KBufferBuilder& builder)
{
    auto thread = Thread::from_tid(to_tid(identifier));
    if (!thread)
        return false;

    JsonArraySerializer array { builder };
    bool show_kernel_addresses = Process::current()->is_superuser();
    for (auto address : Processor::capture_stack_trace(*thread, 1024)) {
        if (!show_kernel_addresses && !is_user_address(VirtualAddress { address }))
            address = 0xdeadc0de;
        array.add(JsonValue(address));
    }

    array.finish();
    return true;
}

static bool procfs$pid_exe(InodeIdentifier identifier, KBufferBuilder& builder)
{
    auto process = Process::from_pid(to_pid(identifier));
    if (!process)
        return false;
    auto* custody = process->executable();
    ASSERT(custody);
    builder.append(custody->absolute_path().bytes());
    return true;
}

static bool procfs$pid_cwd(InodeIdentifier identifier, KBufferBuilder& builder)
{
    auto process = Process::from_pid(to_pid(identifier));
    if (!process)
        return false;
    builder.append_bytes(process->current_directory().absolute_path().bytes());
    return true;
}

static bool procfs$pid_root(InodeIdentifier identifier, KBufferBuilder& builder)
{
    auto process = Process::from_pid(to_pid(identifier));
    if (!process)
        return false;
    builder.append_bytes(process->root_directory_relative_to_global_root().absolute_path().to_byte_buffer());
    return true;
}

static bool procfs$self(InodeIdentifier, KBufferBuilder& builder)
{
    builder.appendff("{}", Process::current()->pid().value());
    return true;
}

static bool procfs$dmesg(InodeIdentifier, KBufferBuilder& builder)
{
    InterruptDisabler disabler;
    for (char ch : Console::the().logbuffer())
        builder.append(ch);
    return true;
}

static bool procfs$df(InodeIdentifier, KBufferBuilder& builder)
{
    // FIXME: This is obviously racy against the VFS mounts changing.
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
        fs_object.add("block_size", static_cast<u64>(fs.block_size()));
        fs_object.add("readonly", fs.is_readonly());
        fs_object.add("mount_flags", mount.flags());

        if (fs.is_file_backed())
            fs_object.add("source", static_cast<const FileBackedFS&>(fs).file_description().absolute_path());
        else
            fs_object.add("source", "none");
    });
    array.finish();
    return true;
}

static bool procfs$cpuinfo(InodeIdentifier, KBufferBuilder& builder)
{
    JsonArraySerializer array { builder };
    Processor::for_each(
        [&](Processor& proc) -> IterationDecision {
            auto& info = proc.info();
            auto obj = array.add_object();
            JsonArray features;
            for (auto& feature : info.features().split(' '))
                features.append(feature);
            obj.add("processor", proc.get_id());
            obj.add("cpuid", info.cpuid());
            obj.add("family", info.display_family());
            obj.add("features", features);
            obj.add("model", info.display_model());
            obj.add("stepping", info.stepping());
            obj.add("type", info.type());
            obj.add("brandstr", info.brandstr());
            return IterationDecision::Continue;
        });
    array.finish();
    return true;
}

static bool procfs$memstat(InodeIdentifier, KBufferBuilder& builder)
{
    InterruptDisabler disabler;

    kmalloc_stats stats;
    get_kmalloc_stats(stats);

    ScopedSpinLock mm_lock(s_mm_lock);
    auto user_physical_pages_total = MM.user_physical_pages();
    auto user_physical_pages_used = MM.user_physical_pages_used();
    auto user_physical_pages_committed = MM.user_physical_pages_committed();
    auto user_physical_pages_uncommitted = MM.user_physical_pages_uncommitted();

    auto super_physical_total = MM.super_physical_pages();
    auto super_physical_used = MM.super_physical_pages_used();
    mm_lock.unlock();

    JsonObjectSerializer<KBufferBuilder> json { builder };
    json.add("kmalloc_allocated", stats.bytes_allocated);
    json.add("kmalloc_available", stats.bytes_free);
    json.add("kmalloc_eternal_allocated", stats.bytes_eternal);
    json.add("user_physical_allocated", user_physical_pages_used);
    json.add("user_physical_available", user_physical_pages_total - user_physical_pages_used);
    json.add("user_physical_committed", user_physical_pages_committed);
    json.add("user_physical_uncommitted", user_physical_pages_uncommitted);
    json.add("super_physical_allocated", super_physical_used);
    json.add("super_physical_available", super_physical_total - super_physical_used);
    json.add("kmalloc_call_count", stats.kmalloc_call_count);
    json.add("kfree_call_count", stats.kfree_call_count);
    slab_alloc_stats([&json](size_t slab_size, size_t num_allocated, size_t num_free) {
        auto prefix = String::formatted("slab_{}", slab_size);
        json.add(String::formatted("{}_num_allocated", prefix), num_allocated);
        json.add(String::formatted("{}_num_free", prefix), num_free);
    });
    json.finish();
    return true;
}

static bool procfs$all(InodeIdentifier, KBufferBuilder& builder)
{
    JsonArraySerializer array { builder };

    // Keep this in sync with CProcessStatistics.
    auto build_process = [&](const Process& process) {
        auto process_object = array.add_object();

        if (process.is_user_process()) {
            StringBuilder pledge_builder;

#define __ENUMERATE_PLEDGE_PROMISE(promise)      \
    if (process.has_promised(Pledge::promise)) { \
        pledge_builder.append(#promise " ");     \
    }
            ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE

            process_object.add("pledge", pledge_builder.to_string());

            switch (process.veil_state()) {
            case VeilState::None:
                process_object.add("veil", "None");
                break;
            case VeilState::Dropped:
                process_object.add("veil", "Dropped");
                break;
            case VeilState::Locked:
                process_object.add("veil", "Locked");
                break;
            }
        } else {
            process_object.add("pledge", String());
            process_object.add("veil", String());
        }

        process_object.add("pid", process.pid().value());
        process_object.add("pgid", process.tty() ? process.tty()->pgid().value() : 0);
        process_object.add("pgp", process.pgid().value());
        process_object.add("sid", process.sid().value());
        process_object.add("uid", process.uid());
        process_object.add("gid", process.gid());
        process_object.add("ppid", process.ppid().value());
        process_object.add("nfds", process.number_of_open_file_descriptors());
        process_object.add("name", process.name());
        process_object.add("executable", process.executable() ? process.executable()->absolute_path() : "");
        process_object.add("tty", process.tty() ? process.tty()->tty_name() : "notty");
        process_object.add("amount_virtual", process.space().amount_virtual());
        process_object.add("amount_resident", process.space().amount_resident());
        process_object.add("amount_dirty_private", process.space().amount_dirty_private());
        process_object.add("amount_clean_inode", process.space().amount_clean_inode());
        process_object.add("amount_shared", process.space().amount_shared());
        process_object.add("amount_purgeable_volatile", process.space().amount_purgeable_volatile());
        process_object.add("amount_purgeable_nonvolatile", process.space().amount_purgeable_nonvolatile());
        process_object.add("dumpable", process.is_dumpable());
        auto thread_array = process_object.add_array("threads");
        process.for_each_thread([&](const Thread& thread) {
            auto thread_object = thread_array.add_object();
            thread_object.add("tid", thread.tid().value());
            thread_object.add("name", thread.name());
            thread_object.add("times_scheduled", thread.times_scheduled());
            thread_object.add("ticks_user", thread.ticks_in_user());
            thread_object.add("ticks_kernel", thread.ticks_in_kernel());
            thread_object.add("state", thread.state_string());
            thread_object.add("cpu", thread.cpu());
            thread_object.add("priority", thread.priority());
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

    ScopedSpinLock lock(g_scheduler_lock);
    auto processes = Process::all_processes();
    build_process(*Scheduler::colonel());
    for (auto& process : processes)
        build_process(process);
    array.finish();
    return true;
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

static bool read_sys_bool(InodeIdentifier inode_id, KBufferBuilder& builder)
{
    auto& variable = SysVariable::for_inode(inode_id);
    ASSERT(variable.type == SysVariable::Type::Boolean);

    u8 buffer[2];
    auto* lockable_bool = reinterpret_cast<Lockable<bool>*>(variable.address);
    {
        LOCKER(lockable_bool->lock(), Lock::Mode::Shared);
        buffer[0] = lockable_bool->resource() ? '1' : '0';
    }
    buffer[1] = '\n';
    builder.append_bytes(ReadonlyBytes { buffer, sizeof(buffer) });
    return true;
}

static ssize_t write_sys_bool(InodeIdentifier inode_id, const UserOrKernelBuffer& buffer, size_t size)
{
    auto& variable = SysVariable::for_inode(inode_id);
    ASSERT(variable.type == SysVariable::Type::Boolean);

    char value = 0;
    bool did_read = false;
    ssize_t nread = buffer.read_buffered<1>(1, [&](const u8* data, size_t) {
        if (did_read)
            return 0;
        value = (char)data[0];
        did_read = true;
        return 1;
    });
    if (nread < 0)
        return nread;
    ASSERT(nread == 0 || (nread == 1 && did_read));
    if (nread == 0 || !(value == '0' || value == '1'))
        return (ssize_t)size;

    auto* lockable_bool = reinterpret_cast<Lockable<bool>*>(variable.address);
    {
        LOCKER(lockable_bool->lock());
        lockable_bool->resource() = value == '1';
    }
    variable.notify();
    return (ssize_t)size;
}

static bool read_sys_string(InodeIdentifier inode_id, KBufferBuilder& builder)
{
    auto& variable = SysVariable::for_inode(inode_id);
    ASSERT(variable.type == SysVariable::Type::String);

    auto* lockable_string = reinterpret_cast<Lockable<String>*>(variable.address);
    LOCKER(lockable_string->lock(), Lock::Mode::Shared);
    builder.append_bytes(lockable_string->resource().bytes());
    return true;
}

static ssize_t write_sys_string(InodeIdentifier inode_id, const UserOrKernelBuffer& buffer, size_t size)
{
    auto& variable = SysVariable::for_inode(inode_id);
    ASSERT(variable.type == SysVariable::Type::String);

    auto string_copy = buffer.copy_into_string(size);
    if (string_copy.is_null())
        return -EFAULT;

    {
        auto* lockable_string = reinterpret_cast<Lockable<String>*>(variable.address);
        LOCKER(lockable_string->lock());
        lockable_string->resource() = move(string_copy);
    }
    variable.notify();
    return (ssize_t)size;
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

NonnullRefPtr<Inode> ProcFS::root_inode() const
{
    return *m_root_inode;
}

RefPtr<Inode> ProcFS::get_inode(InodeIdentifier inode_id) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFS::get_inode({})", inode_id.index());
    if (inode_id == root_inode()->identifier())
        return m_root_inode;

    LOCKER(m_inodes_lock);
    auto it = m_inodes.find(inode_id.index().value());
    if (it != m_inodes.end()) {
        // It's possible that the ProcFSInode ref count was dropped to 0 or
        // the ~ProcFSInode destructor is even running already, but blocked
        // from removing it from this map. So we need to *try* to ref it,
        // and if that fails we cannot return this instance anymore and just
        // create a new one.
        if (it->value->try_ref())
            return adopt(*it->value);
        // We couldn't ref it, so just create a new one and replace the entry
    }
    auto inode = adopt(*new ProcFSInode(const_cast<ProcFS&>(*this), inode_id.index()));
    auto result = m_inodes.set(inode_id.index().value(), inode.ptr());
    ASSERT(result == ((it == m_inodes.end()) ? AK::HashSetResult::InsertedNewEntry : AK::HashSetResult::ReplacedExistingEntry));
    return inode;
}

ProcFSInode::ProcFSInode(ProcFS& fs, InodeIndex index)
    : Inode(fs, index)
{
}

ProcFSInode::~ProcFSInode()
{
    LOCKER(fs().m_inodes_lock);
    auto it = fs().m_inodes.find(index().value());
    if (it != fs().m_inodes.end() && it->value == this)
        fs().m_inodes.remove(it);
}

KResult ProcFSInode::refresh_data(FileDescription& description) const
{
    if (Kernel::is_directory(identifier()))
        return KSuccess;

    auto& cached_data = description.data();
    auto* directory_entry = fs().get_directory_entry(identifier());

    bool (*read_callback)(InodeIdentifier, KBufferBuilder&) = nullptr;
    if (directory_entry) {
        read_callback = directory_entry->read_callback;
        ASSERT(read_callback);
    } else {
        switch (to_proc_parent_directory(identifier())) {
        case PDI_PID_fd:
            read_callback = procfs$pid_fd_entry;
            break;
        case PDI_PID_stacks:
            read_callback = procfs$tid_stack;
            break;
        case PDI_Root_sys:
            switch (SysVariable::for_inode(identifier()).type) {
            case SysVariable::Type::Invalid:
                ASSERT_NOT_REACHED();
            case SysVariable::Type::Boolean:
                read_callback = read_sys_bool;
                break;
            case SysVariable::Type::String:
                read_callback = read_sys_string;
                break;
            }
            break;
        default:
            ASSERT_NOT_REACHED();
        }

        ASSERT(read_callback);
    }

    if (!cached_data)
        cached_data = new ProcFSInodeData;
    auto& buffer = static_cast<ProcFSInodeData&>(*cached_data).buffer;
    if (buffer) {
        // If we're reusing the buffer, reset the size to 0 first. This
        // ensures we don't accidentally leak previously written data.
        buffer->set_size(0);
    }
    KBufferBuilder builder(buffer, true);
    if (!read_callback(identifier(), builder))
        return ENOENT;
    // We don't use builder.build() here, which would steal our buffer
    // and turn it into an OwnPtr. Instead, just flush to the buffer so
    // that we can read all the data that was written.
    if (!builder.flush())
        return ENOMEM;
    if (!buffer)
        return ENOMEM;
    return KSuccess;
}

KResult ProcFSInode::attach(FileDescription& description)
{
    return refresh_data(description);
}

void ProcFSInode::did_seek(FileDescription& description, off_t new_offset)
{
    if (new_offset != 0)
        return;
    auto result = refresh_data(description);
    if (result.is_error()) {
        // Subsequent calls to read will return EIO!
        dbgln("ProcFS: Could not refresh contents: {}", result.error());
    }
}

InodeMetadata ProcFSInode::metadata() const
{
    dbgln_if(PROCFS_DEBUG, "ProcFSInode::metadata({})", index());
    InodeMetadata metadata;
    metadata.inode = identifier();
    metadata.ctime = mepoch;
    metadata.atime = mepoch;
    metadata.mtime = mepoch;
    auto proc_parent_directory = to_proc_parent_directory(identifier());
    auto proc_file_type = to_proc_file_type(identifier());

    dbgln_if(PROCFS_DEBUG, "  -> pid={}, fi={}, pdi={}", to_pid(identifier()).value(), (int)proc_file_type, (int)proc_parent_directory);

    if (is_process_related_file(identifier())) {
        ProcessID pid = to_pid(identifier());
        auto process = Process::from_pid(pid);
        if (process && process->is_dumpable()) {
            metadata.uid = process->euid();
            metadata.gid = process->egid();
        } else {
            metadata.uid = 0;
            metadata.gid = 0;
        }
    } else if (is_thread_related_file(identifier())) {
        ThreadID tid = to_tid(identifier());
        auto thread = Thread::from_tid(tid);
        if (thread && thread->process().is_dumpable()) {
            metadata.uid = thread->process().euid();
            metadata.gid = thread->process().egid();
        } else {
            metadata.uid = 0;
            metadata.gid = 0;
        }
    }

    if (proc_parent_directory == PDI_PID_fd) {
        metadata.mode = S_IFLNK | S_IRUSR | S_IWUSR | S_IXUSR;
        return metadata;
    }

    switch (proc_file_type) {
    case FI_Root_self:
        metadata.mode = S_IFLNK | S_IRUSR | S_IRGRP | S_IROTH;
        break;
    case FI_PID_cwd:
    case FI_PID_exe:
    case FI_PID_root:
        metadata.mode = S_IFLNK | S_IRUSR;
        break;
    case FI_Root:
    case FI_Root_sys:
    case FI_Root_net:
        metadata.mode = S_IFDIR | S_IRUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH;
        break;
    case FI_PID:
    case FI_PID_fd:
    case FI_PID_stacks:
        metadata.mode = S_IFDIR | S_IRUSR | S_IXUSR;
        break;
    case FI_Root_smbios_entry_point:
        metadata.mode = S_IFREG | S_IRUSR | S_IRGRP | S_IROTH;
        metadata.size = DMIExpose::the().entry_point_length();
        break;
    case FI_Root_dmi:
        metadata.mode = S_IFREG | S_IRUSR | S_IRGRP | S_IROTH;
        metadata.size = DMIExpose::the().structure_table_length();
        break;
    default:
        metadata.mode = S_IFREG | S_IRUSR | S_IRGRP | S_IROTH;
        break;
    }

    if (proc_file_type > FI_Invalid && proc_file_type < FI_MaxStaticFileIndex) {
        if (fs().m_entries[proc_file_type].supervisor_only) {
            metadata.uid = 0;
            metadata.gid = 0;
            metadata.mode &= ~077;
        }
    }
    return metadata;
}

ssize_t ProcFSInode::read_bytes(off_t offset, ssize_t count, UserOrKernelBuffer& buffer, FileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFS: read_bytes offset: {} count: {}", offset, count);
    ASSERT(offset >= 0);
    ASSERT(buffer.user_or_kernel_ptr());

    if (!description)
        return -EIO;
    if (!description->data()) {
        dbgln_if(PROCFS_DEBUG, "ProcFS: Do not have cached data!");
        return -EIO;
    }

    // Be sure to keep a reference to data_buffer while we use it!
    RefPtr<KBufferImpl> data_buffer = static_cast<ProcFSInodeData&>(*description->data()).buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(data_buffer->data() + offset, nread))
        return -EFAULT;

    return nread;
}

InodeIdentifier ProcFS::ProcFSDirectoryEntry::identifier(unsigned fsid) const
{
    return to_identifier(fsid, PDI_Root, 0, (ProcFileType)proc_file_type);
}

KResult ProcFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFS: traverse_as_directory {}", index());

    if (!Kernel::is_directory(identifier()))
        return ENOTDIR;

    auto proc_file_type = to_proc_file_type(identifier());
    auto parent_id = to_parent_id(identifier());

    callback({ ".", identifier(), 2 });
    callback({ "..", parent_id, 2 });

    switch (proc_file_type) {
    case FI_Root:
        for (auto& entry : fs().m_entries) {
            // FIXME: strlen() here is sad.
            if (!entry.name)
                continue;
            if (entry.proc_file_type > __FI_Root_Start && entry.proc_file_type < __FI_Root_End)
                callback({ { entry.name, strlen(entry.name) }, to_identifier(fsid(), PDI_Root, 0, (ProcFileType)entry.proc_file_type), 0 });
        }
        for (auto pid_child : Process::all_pids()) {
            callback({ String::number(pid_child.value()), to_identifier(fsid(), PDI_Root, pid_child, FI_PID), 0 });
        }
        break;

    case FI_Root_sys:
        for (size_t i = 1; i < sys_variables().size(); ++i) {
            auto& variable = sys_variables()[i];
            callback({ variable.name, sys_var_to_identifier(fsid(), i), 0 });
        }
        break;

    case FI_Root_net:
        callback({ "adapters", to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_adapters), 0 });
        callback({ "arp", to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_arp), 0 });
        callback({ "tcp", to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_tcp), 0 });
        callback({ "udp", to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_udp), 0 });
        callback({ "local", to_identifier(fsid(), PDI_Root_net, 0, FI_Root_net_local), 0 });
        break;

    case FI_PID: {
        auto pid = to_pid(identifier());
        auto process = Process::from_pid(pid);
        if (!process)
            return ENOENT;
        for (auto& entry : fs().m_entries) {
            if (entry.proc_file_type > __FI_PID_Start && entry.proc_file_type < __FI_PID_End) {
                if (entry.proc_file_type == FI_PID_exe && !process->executable())
                    continue;
                // FIXME: strlen() here is sad.
                callback({ { entry.name, strlen(entry.name) }, to_identifier(fsid(), PDI_PID, pid, (ProcFileType)entry.proc_file_type), 0 });
            }
        }
    } break;

    case FI_PID_fd: {
        auto pid = to_pid(identifier());
        auto process = Process::from_pid(pid);
        if (!process)
            return ENOENT;
        for (int i = 0; i < process->max_open_file_descriptors(); ++i) {
            auto description = process->file_description(i);
            if (!description)
                continue;
            callback({ String::number(i), to_identifier_with_fd(fsid(), pid, i), 0 });
        }
    } break;

    case FI_PID_stacks: {
        auto pid = to_pid(identifier());
        auto process = Process::from_pid(pid);
        if (!process)
            return ENOENT;
        process->for_each_thread([&](Thread& thread) -> IterationDecision {
            int tid = thread.tid().value();
            callback({ String::number(tid), to_identifier_with_stack(fsid(), tid), 0 });
            return IterationDecision::Continue;
        });
    } break;

    default:
        return KSuccess;
    }

    return KSuccess;
}

RefPtr<Inode> ProcFSInode::lookup(StringView name)
{
    ASSERT(is_directory());
    if (name == ".")
        return this;
    if (name == "..")
        return fs().get_inode(to_parent_id(identifier()));

    auto proc_file_type = to_proc_file_type(identifier());

    if (proc_file_type == FI_Root) {
        for (auto& entry : fs().m_entries) {
            if (entry.name == nullptr)
                continue;
            if (entry.proc_file_type > __FI_Root_Start && entry.proc_file_type < __FI_Root_End) {
                if (name == entry.name) {
                    return fs().get_inode(to_identifier(fsid(), PDI_Root, 0, (ProcFileType)entry.proc_file_type));
                }
            }
        }
        auto name_as_number = name.to_uint();
        if (!name_as_number.has_value())
            return {};
        bool process_exists = false;
        {
            InterruptDisabler disabler;
            process_exists = Process::from_pid(name_as_number.value());
        }
        if (process_exists)
            return fs().get_inode(to_identifier(fsid(), PDI_Root, name_as_number.value(), FI_PID));
        return {};
    }

    if (proc_file_type == FI_Root_sys) {
        for (size_t i = 1; i < sys_variables().size(); ++i) {
            auto& variable = sys_variables()[i];
            if (name == variable.name)
                return fs().get_inode(sys_var_to_identifier(fsid(), i));
        }
        return {};
    }

    if (proc_file_type == FI_Root_net) {
        if (name == "adapters")
            return fs().get_inode(to_identifier(fsid(), PDI_Root, 0, FI_Root_net_adapters));
        if (name == "arp")
            return fs().get_inode(to_identifier(fsid(), PDI_Root, 0, FI_Root_net_arp));
        if (name == "tcp")
            return fs().get_inode(to_identifier(fsid(), PDI_Root, 0, FI_Root_net_tcp));
        if (name == "udp")
            return fs().get_inode(to_identifier(fsid(), PDI_Root, 0, FI_Root_net_udp));
        if (name == "local")
            return fs().get_inode(to_identifier(fsid(), PDI_Root, 0, FI_Root_net_local));
        return {};
    }

    if (proc_file_type == FI_PID) {
        auto process = Process::from_pid(to_pid(identifier()));
        if (!process)
            return {};
        for (auto& entry : fs().m_entries) {
            if (entry.proc_file_type > __FI_PID_Start && entry.proc_file_type < __FI_PID_End) {
                if (entry.proc_file_type == FI_PID_exe && !process->executable())
                    continue;
                if (entry.name == nullptr)
                    continue;
                if (name == entry.name) {
                    return fs().get_inode(to_identifier(fsid(), PDI_PID, to_pid(identifier()), (ProcFileType)entry.proc_file_type));
                }
            }
        }
        return {};
    }

    if (proc_file_type == FI_PID_fd) {
        auto name_as_number = name.to_uint();
        if (!name_as_number.has_value())
            return {};
        bool fd_exists = false;
        {
            if (auto process = Process::from_pid(to_pid(identifier())))
                fd_exists = process->file_description(name_as_number.value());
        }
        if (fd_exists)
            return fs().get_inode(to_identifier_with_fd(fsid(), to_pid(identifier()), name_as_number.value()));
    }

    if (proc_file_type == FI_PID_stacks) {
        auto name_as_number = name.to_int();
        if (!name_as_number.has_value())
            return {};
        int tid = name_as_number.value();
        if (tid <= 0) {
            return {};
        }
        bool thread_exists = false;
        {
            auto process = Process::from_pid(to_pid(identifier()));
            auto thread = Thread::from_tid(tid);
            thread_exists = process && thread && process->pid() == thread->pid();
        }
        if (thread_exists)
            return fs().get_inode(to_identifier_with_stack(fsid(), tid));
    }

    return {};
}

void ProcFSInode::flush_metadata()
{
}

ssize_t ProcFSInode::write_bytes(off_t offset, ssize_t size, const UserOrKernelBuffer& buffer, FileDescription*)
{
    auto result = prepare_to_write_data();
    if (result.is_error())
        return result;

    auto* directory_entry = fs().get_directory_entry(identifier());

    ssize_t (*write_callback)(InodeIdentifier, const UserOrKernelBuffer&, size_t) = nullptr;

    if (directory_entry == nullptr) {
        if (to_proc_parent_directory(identifier()) == PDI_Root_sys) {
            switch (SysVariable::for_inode(identifier()).type) {
            case SysVariable::Type::Invalid:
                ASSERT_NOT_REACHED();
            case SysVariable::Type::Boolean:
                write_callback = write_sys_bool;
                break;
            case SysVariable::Type::String:
                write_callback = write_sys_string;
                break;
            }
        } else
            return -EPERM;
    } else {
        if (!directory_entry->write_callback)
            return -EPERM;
        write_callback = directory_entry->write_callback;
    }

    ASSERT(is_persistent_inode(identifier()));
    // FIXME: Being able to write into ProcFS at a non-zero offset seems like something we should maybe support..
    ASSERT(offset == 0);
    ssize_t nwritten = write_callback(identifier(), buffer, (size_t)size);
    if (nwritten < 0)
        klog() << "ProcFS: Writing " << size << " bytes failed: " << nwritten;
    return nwritten;
}

KResultOr<NonnullRefPtr<Custody>> ProcFSInode::resolve_as_link(Custody& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level) const
{
    if (FI_Root_self == to_proc_file_type(identifier())) {
        return VFS::the().resolve_path(String::number(Process::current()->pid().value()), base, out_parent, options, symlink_recursion_level);
    }

    // The only other links are in pid directories, so it's safe to ignore
    // unrelated files and the thread-specific stacks/ directory.
    if (!is_process_related_file(identifier()))
        return Inode::resolve_as_link(base, out_parent, options, symlink_recursion_level);

    // FIXME: We should return a custody for FI_PID or FI_PID_fd here
    //        for correctness. It's impossible to create files in ProcFS,
    //        so returning null shouldn't break much.
    if (out_parent)
        *out_parent = nullptr;

    auto pid = to_pid(identifier());
    auto proc_file_type = to_proc_file_type(identifier());
    auto process = Process::from_pid(pid);
    if (!process)
        return ENOENT;

    if (to_proc_parent_directory(identifier()) == PDI_PID_fd) {
        if (out_parent)
            *out_parent = base;
        int fd = to_fd(identifier());
        auto description = process->file_description(fd);
        if (!description)
            return ENOENT;
        auto proxy_inode = ProcFSProxyInode::create(const_cast<ProcFS&>(fs()), *description);
        return Custody::create(&base, "", proxy_inode, base.mount_flags());
    }

    Custody* res = nullptr;

    switch (proc_file_type) {
    case FI_PID_cwd:
        res = &process->current_directory();
        break;
    case FI_PID_exe:
        res = process->executable();
        break;
    case FI_PID_root:
        // Note: we open root_directory() here, not
        // root_directory_relative_to_global_root().
        // This seems more useful.
        res = &process->root_directory();
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    if (!res)
        return ENOENT;

    return *res;
}

ProcFSProxyInode::ProcFSProxyInode(ProcFS& fs, FileDescription& fd)
    : Inode(fs, 0)
    , m_fd(fd)
{
}

ProcFSProxyInode::~ProcFSProxyInode()
{
}

KResult ProcFSProxyInode::attach(FileDescription& fd)
{
    return m_fd->inode()->attach(fd);
}

void ProcFSProxyInode::did_seek(FileDescription& fd, off_t new_offset)
{
    return m_fd->inode()->did_seek(fd, new_offset);
}

InodeMetadata ProcFSProxyInode::metadata() const
{
    InodeMetadata metadata = m_fd->metadata();

    if (m_fd->is_readable())
        metadata.mode |= 0444;
    else
        metadata.mode &= ~0444;

    if (m_fd->is_writable())
        metadata.mode |= 0222;
    else
        metadata.mode &= ~0222;

    if (!metadata.is_directory())
        metadata.mode &= ~0111;

    return metadata;
}

KResultOr<NonnullRefPtr<Inode>> ProcFSProxyInode::create_child(const String& name, mode_t mode, dev_t dev, uid_t uid, gid_t gid)
{
    if (!m_fd->inode())
        return EINVAL;
    return m_fd->inode()->create_child(name, mode, dev, uid, gid);
}

KResult ProcFSProxyInode::add_child(Inode& child, const StringView& name, mode_t mode)
{
    if (!m_fd->inode())
        return EINVAL;
    return m_fd->inode()->add_child(child, name, mode);
}

KResult ProcFSProxyInode::remove_child(const StringView& name)
{
    if (!m_fd->inode())
        return EINVAL;
    return m_fd->inode()->remove_child(name);
}

RefPtr<Inode> ProcFSProxyInode::lookup(StringView name)
{
    if (!m_fd->inode())
        return {};
    return m_fd->inode()->lookup(name);
}

KResultOr<size_t> ProcFSProxyInode::directory_entry_count() const
{
    if (!m_fd->inode())
        return EINVAL;
    return m_fd->inode()->directory_entry_count();
}

KResultOr<NonnullRefPtr<Inode>> ProcFSInode::create_child(const String&, mode_t, dev_t, uid_t, gid_t)
{
    return EPERM;
}

KResult ProcFSInode::add_child(Inode&, const StringView&, mode_t)
{
    return EPERM;
}

KResult ProcFSInode::remove_child([[maybe_unused]] const StringView& name)
{
    return EPERM;
}

KResultOr<size_t> ProcFSInode::directory_entry_count() const
{
    ASSERT(is_directory());
    size_t count = 0;
    KResult result = traverse_as_directory([&count](auto&) {
        ++count;
        return true;
    });

    if (result.is_error())
        return result;

    return count;
}

KResult ProcFSInode::chmod(mode_t)
{
    return EPERM;
}

ProcFS::ProcFS()
{
    m_root_inode = adopt(*new ProcFSInode(*this, 1));
    m_entries.resize(FI_MaxStaticFileIndex);
    m_entries[FI_Root_df] = { "df", FI_Root_df, false, procfs$df };
    m_entries[FI_Root_all] = { "all", FI_Root_all, false, procfs$all };
    m_entries[FI_Root_memstat] = { "memstat", FI_Root_memstat, false, procfs$memstat };
    m_entries[FI_Root_cpuinfo] = { "cpuinfo", FI_Root_cpuinfo, false, procfs$cpuinfo };
    m_entries[FI_Root_dmesg] = { "dmesg", FI_Root_dmesg, true, procfs$dmesg };
    m_entries[FI_Root_self] = { "self", FI_Root_self, false, procfs$self };
    m_entries[FI_Root_pci] = { "pci", FI_Root_pci, false, procfs$pci };
    m_entries[FI_Root_interrupts] = { "interrupts", FI_Root_interrupts, false, procfs$interrupts };
    m_entries[FI_Root_dmi] = { "DMI", FI_Root_dmi, false, procfs$dmi };
    m_entries[FI_Root_smbios_entry_point] = { "smbios_entry_point", FI_Root_smbios_entry_point, false, procfs$smbios_entry_point };
    m_entries[FI_Root_keymap] = { "keymap", FI_Root_keymap, false, procfs$keymap };
    m_entries[FI_Root_devices] = { "devices", FI_Root_devices, false, procfs$devices };
    m_entries[FI_Root_uptime] = { "uptime", FI_Root_uptime, false, procfs$uptime };
    m_entries[FI_Root_cmdline] = { "cmdline", FI_Root_cmdline, true, procfs$cmdline };
    m_entries[FI_Root_modules] = { "modules", FI_Root_modules, true, procfs$modules };
    m_entries[FI_Root_sys] = { "sys", FI_Root_sys, true };
    m_entries[FI_Root_net] = { "net", FI_Root_net, false };

    m_entries[FI_Root_net_adapters] = { "adapters", FI_Root_net_adapters, false, procfs$net_adapters };
    m_entries[FI_Root_net_arp] = { "arp", FI_Root_net_arp, true, procfs$net_arp };
    m_entries[FI_Root_net_tcp] = { "tcp", FI_Root_net_tcp, false, procfs$net_tcp };
    m_entries[FI_Root_net_udp] = { "udp", FI_Root_net_udp, false, procfs$net_udp };
    m_entries[FI_Root_net_local] = { "local", FI_Root_net_local, false, procfs$net_local };

    m_entries[FI_PID_vm] = { "vm", FI_PID_vm, false, procfs$pid_vm };
    m_entries[FI_PID_stacks] = { "stacks", FI_PID_stacks, false };
    m_entries[FI_PID_fds] = { "fds", FI_PID_fds, false, procfs$pid_fds };
    m_entries[FI_PID_exe] = { "exe", FI_PID_exe, false, procfs$pid_exe };
    m_entries[FI_PID_cwd] = { "cwd", FI_PID_cwd, false, procfs$pid_cwd };
    m_entries[FI_PID_unveil] = { "unveil", FI_PID_unveil, false, procfs$pid_unveil };
    m_entries[FI_PID_root] = { "root", FI_PID_root, false, procfs$pid_root };
    m_entries[FI_PID_perf_events] = { "perf_events", FI_PID_perf_events, false, procfs$pid_perf_events };
    m_entries[FI_PID_fd] = { "fd", FI_PID_fd, false };
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
    return EPERM;
}
}
