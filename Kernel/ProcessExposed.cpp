/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <AK/UBSanitizer.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Arch/x86/ProcessorInfo.h>
#include <Kernel/CommandLine.h>
#include <Kernel/ConsoleDevice.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Module.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/Sections.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

static SpinLock<u8> s_index_lock;
static InodeIndex s_next_inode_index = 0;

static size_t s_allocate_inode_index()
{
    ScopedSpinLock lock(s_index_lock);
    s_next_inode_index = s_next_inode_index.value() + 1;
    VERIFY(s_next_inode_index > 0);
    return s_next_inode_index.value();
}

InodeIndex ProcFSComponentsRegistrar::allocate_inode_index() const
{
    return s_allocate_inode_index();
}

ProcFSExposedComponent::ProcFSExposedComponent(StringView name)
    : m_component_index(s_allocate_inode_index())
{
    m_name = KString::try_create(name);
}

// Note: This constructor is intended to be used in /proc/pid/fd/* symlinks
// so we preallocated inode index for them so we just need to set it here.
ProcFSExposedComponent::ProcFSExposedComponent(StringView name, InodeIndex preallocated_index)
    : m_component_index(preallocated_index.value())
{
    VERIFY(preallocated_index.value() != 0);
    VERIFY(preallocated_index <= s_next_inode_index);
    m_name = KString::try_create(name);
}

ProcFSExposedFolder::ProcFSExposedFolder(StringView name)
    : ProcFSExposedComponent(name)
{
}

ProcFSExposedFolder::ProcFSExposedFolder(StringView name, const ProcFSExposedFolder& parent_folder)
    : ProcFSExposedComponent(name)
    , m_parent_folder(parent_folder)
{
}

ProcFSExposedLink::ProcFSExposedLink(StringView name)
    : ProcFSExposedComponent(name)
{
}

ProcFSExposedLink::ProcFSExposedLink(StringView name, InodeIndex preallocated_index)
    : ProcFSExposedComponent(name, preallocated_index)
{
}

struct ProcFSInodeData : public FileDescriptionData {
    RefPtr<KBufferImpl> buffer;
};

KResultOr<size_t> ProcFSGlobalInformation::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFSGlobalInformation @ {}: read_bytes offset: {} count: {}", name(), offset, count);

    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description)
        return KResult(EIO);
    if (!description->data()) {
        dbgln("ProcFSGlobalInformation: Do not have cached data!");
        return KResult(EIO);
    }

    // Be sure to keep a reference to data_buffer while we use it!
    RefPtr<KBufferImpl> data_buffer = static_cast<ProcFSInodeData&>(*description->data()).buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(data_buffer->data() + offset, nread))
        return KResult(EFAULT);

    return nread;
}

KResult ProcFSGlobalInformation::refresh_data(FileDescription& description) const
{
    ScopedSpinLock lock(m_refresh_lock);
    auto& cached_data = description.data();
    if (!cached_data)
        cached_data = adopt_own_if_nonnull(new (nothrow) ProcFSInodeData);
    VERIFY(description.data());
    auto& buffer = static_cast<ProcFSInodeData&>(*cached_data).buffer;
    if (buffer) {
        // If we're reusing the buffer, reset the size to 0 first. This
        // ensures we don't accidentally leak previously written data.
        buffer->set_size(0);
    }
    KBufferBuilder builder(buffer, true);
    if (!const_cast<ProcFSGlobalInformation&>(*this).output(builder))
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

RefPtr<ProcFSProcessFolder> ProcFSRootFolder::process_folder_for(Process& process)
{
    RefPtr<Process> checked_process = process;
    for (auto& folder : m_process_folders) {
        if (folder.associated_process().ptr() == checked_process.ptr())
            return folder;
    }
    return {};
}

KResultOr<size_t> ProcFSProcessInformation::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFSProcessInformation @ {}: read_bytes offset: {} count: {}", name(), offset, count);

    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description)
        return KResult(EIO);
    if (!description->data()) {
        dbgln("ProcFSGlobalInformation: Do not have cached data!");
        return KResult(EIO);
    }

    // Be sure to keep a reference to data_buffer while we use it!
    RefPtr<KBufferImpl> data_buffer = static_cast<ProcFSInodeData&>(*description->data()).buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(data_buffer->data() + offset, nread))
        return KResult(EFAULT);

    return nread;
}

KResult ProcFSProcessInformation::refresh_data(FileDescription& description) const
{
    // For process-specific inodes, hold the process's ptrace lock across refresh
    // and refuse to load data if the process is not dumpable.
    // Without this, files opened before a process went non-dumpable could still be used for dumping.
    auto process = const_cast<ProcFSProcessInformation&>(*this).m_parent_folder->m_associated_process;
    process->ptrace_lock().lock();
    if (!process->is_dumpable()) {
        process->ptrace_lock().unlock();
        return EPERM;
    }
    ScopeGuard guard = [&] {
        process->ptrace_lock().unlock();
    };
    ScopedSpinLock lock(m_refresh_lock);
    auto& cached_data = description.data();
    if (!cached_data)
        cached_data = adopt_own_if_nonnull(new (nothrow) ProcFSInodeData);
    VERIFY(description.data());
    auto& buffer = static_cast<ProcFSInodeData&>(*cached_data).buffer;
    if (buffer) {
        // If we're reusing the buffer, reset the size to 0 first. This
        // ensures we don't accidentally leak previously written data.
        buffer->set_size(0);
    }
    KBufferBuilder builder(buffer, true);
    if (!const_cast<ProcFSProcessInformation&>(*this).output(builder))
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

class ProcFSSystemDirectory;

class ProcFSDumpKmallocStacks : public ProcFSSystemBoolean {
public:
    static NonnullRefPtr<ProcFSDumpKmallocStacks> must_create(const ProcFSSystemDirectory&);
    virtual bool value() const override
    {
        Locker locker(m_lock);
        return g_dump_kmalloc_stacks;
    }
    virtual void set_value(bool new_value) override
    {
        Locker locker(m_lock);
        g_dump_kmalloc_stacks = new_value;
    }

private:
    ProcFSDumpKmallocStacks();
    mutable Lock m_lock;
};

class ProcFSUBSanDeadly : public ProcFSSystemBoolean {
public:
    static NonnullRefPtr<ProcFSUBSanDeadly> must_create(const ProcFSSystemDirectory&);
    virtual bool value() const override
    {
        Locker locker(m_lock);
        return AK::UBSanitizer::g_ubsan_is_deadly;
    }
    virtual void set_value(bool new_value) override
    {
        Locker locker(m_lock);
        AK::UBSanitizer::g_ubsan_is_deadly = new_value;
    }

private:
    ProcFSUBSanDeadly();
    mutable Lock m_lock;
};

class ProcFSCapsLockRemap : public ProcFSSystemBoolean {
public:
    static NonnullRefPtr<ProcFSCapsLockRemap> must_create(const ProcFSSystemDirectory&);
    virtual bool value() const override
    {
        Locker locker(m_lock);
        return g_caps_lock_remapped_to_ctrl.load();
    }
    virtual void set_value(bool new_value) override
    {
        Locker locker(m_lock);
        g_caps_lock_remapped_to_ctrl.exchange(new_value);
    }

private:
    ProcFSCapsLockRemap();
    mutable Lock m_lock;
};

UNMAP_AFTER_INIT NonnullRefPtr<ProcFSDumpKmallocStacks> ProcFSDumpKmallocStacks::must_create(const ProcFSSystemDirectory&)
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSDumpKmallocStacks).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSUBSanDeadly> ProcFSUBSanDeadly::must_create(const ProcFSSystemDirectory&)
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSUBSanDeadly).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSCapsLockRemap> ProcFSCapsLockRemap::must_create(const ProcFSSystemDirectory&)
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSCapsLockRemap).release_nonnull();
}

UNMAP_AFTER_INIT ProcFSDumpKmallocStacks::ProcFSDumpKmallocStacks()
    : ProcFSSystemBoolean("kmalloc_stacks"sv)
{
}

UNMAP_AFTER_INIT ProcFSUBSanDeadly::ProcFSUBSanDeadly()
    : ProcFSSystemBoolean("ubsan_is_deadly"sv)
{
}

UNMAP_AFTER_INIT ProcFSCapsLockRemap::ProcFSCapsLockRemap()
    : ProcFSSystemBoolean("caps_lock_to_ctrl"sv)
{
}

class ProcFSSelfProcessFolder final : public ProcFSExposedLink {
public:
    static NonnullRefPtr<ProcFSSelfProcessFolder> must_create();

private:
    ProcFSSelfProcessFolder();
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        builder.appendff("{}", Process::current()->pid().value());
        return true;
    }
};

class ProcFSDiskUsage final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSDiskUsage> must_create();

private:
    ProcFSDiskUsage();
    virtual bool output(KBufferBuilder& builder) override
    {
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
};

class ProcFSMemoryStatus final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSMemoryStatus> must_create();

private:
    ProcFSMemoryStatus();
    virtual bool output(KBufferBuilder& builder) override
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
};

class ProcFSOverallProcesses final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSOverallProcesses> must_create();

private:
    ProcFSOverallProcesses();
    virtual bool output(KBufferBuilder& builder) override
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
            process_object.add("kernel", process.is_kernel_process());
            auto thread_array = process_object.add_array("threads");
            process.for_each_thread([&](const Thread& thread) {
                auto thread_object = thread_array.add_object();
#if LOCK_DEBUG
                thread_object.add("lock_count", thread.lock_count());
#endif
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
};
class ProcFSCPUInformation final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSCPUInformation> must_create();

private:
    ProcFSCPUInformation();
    virtual bool output(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        Processor::for_each(
            [&](Processor& proc) {
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
            });
        array.finish();
        return true;
    }
};
class ProcFSDmesg final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSDmesg> must_create();

private:
    ProcFSDmesg();
    virtual bool output(KBufferBuilder& builder) override
    {
        InterruptDisabler disabler;
        for (char ch : ConsoleDevice::the().logbuffer())
            builder.append(ch);
        return true;
    }
};
class ProcFSInterrupts final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSInterrupts> must_create();

private:
    ProcFSInterrupts();
    virtual bool output(KBufferBuilder& builder) override
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
};
class ProcFSKeymap final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSKeymap> must_create();

private:
    ProcFSKeymap();
    virtual bool output(KBufferBuilder& builder) override
    {
        JsonObjectSerializer<KBufferBuilder> json { builder };
        json.add("keymap", HIDManagement::the().keymap_name());
        json.finish();
        return true;
    }
};

// FIXME: Remove this after we enumerate the SysFS from lspci and SystemMonitor
class ProcFSPCI final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSPCI> must_create();

private:
    ProcFSPCI();
    virtual bool output(KBufferBuilder& builder) override
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
};

class ProcFSDevices final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSDevices> must_create();

private:
    ProcFSDevices();
    virtual bool output(KBufferBuilder& builder) override
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
                VERIFY_NOT_REACHED();
        });
        array.finish();
        return true;
    }
};
class ProcFSUptime final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSUptime> must_create();

private:
    ProcFSUptime();
    virtual bool output(KBufferBuilder& builder) override
    {
        builder.appendff("{}\n", TimeManagement::the().uptime_ms() / 1000);
        return true;
    }
};
class ProcFSCommandLine final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSCommandLine> must_create();

private:
    ProcFSCommandLine();
    virtual bool output(KBufferBuilder& builder) override
    {
        builder.append(kernel_command_line().string());
        builder.append('\n');
        return true;
    }
};
class ProcFSModules final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSModules> must_create();

private:
    ProcFSModules();
    virtual bool output(KBufferBuilder& builder) override
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
};
class ProcFSProfile final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSProfile> must_create();

private:
    ProcFSProfile();
    virtual bool output(KBufferBuilder& builder) override
    {
        extern PerformanceEventBuffer* g_global_perf_events;
        if (!g_global_perf_events)
            return false;

        return g_global_perf_events->to_json(builder);
    }
};

UNMAP_AFTER_INIT NonnullRefPtr<ProcFSSelfProcessFolder> ProcFSSelfProcessFolder::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSSelfProcessFolder()).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSDiskUsage> ProcFSDiskUsage::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSDiskUsage).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSMemoryStatus> ProcFSMemoryStatus::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSMemoryStatus).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSOverallProcesses> ProcFSOverallProcesses::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSOverallProcesses).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSCPUInformation> ProcFSCPUInformation::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSCPUInformation).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSDmesg> ProcFSDmesg::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSDmesg).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSInterrupts> ProcFSInterrupts::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSInterrupts).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSKeymap> ProcFSKeymap::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSKeymap).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSPCI> ProcFSPCI::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSPCI).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSDevices> ProcFSDevices::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSDevices).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSUptime> ProcFSUptime::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSUptime).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSCommandLine> ProcFSCommandLine::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSCommandLine).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSModules> ProcFSModules::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSModules).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSProfile> ProcFSProfile::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSProfile).release_nonnull();
}

UNMAP_AFTER_INIT ProcFSSelfProcessFolder::ProcFSSelfProcessFolder()
    : ProcFSExposedLink("self"sv)
{
}
UNMAP_AFTER_INIT ProcFSDiskUsage::ProcFSDiskUsage()
    : ProcFSGlobalInformation("df"sv)
{
}
UNMAP_AFTER_INIT ProcFSMemoryStatus::ProcFSMemoryStatus()
    : ProcFSGlobalInformation("memstat"sv)
{
}
UNMAP_AFTER_INIT ProcFSOverallProcesses::ProcFSOverallProcesses()
    : ProcFSGlobalInformation("all"sv)
{
}
UNMAP_AFTER_INIT ProcFSCPUInformation::ProcFSCPUInformation()
    : ProcFSGlobalInformation("cpuinfo"sv)
{
}
UNMAP_AFTER_INIT ProcFSDmesg::ProcFSDmesg()
    : ProcFSGlobalInformation("dmesg"sv)
{
}
UNMAP_AFTER_INIT ProcFSInterrupts::ProcFSInterrupts()
    : ProcFSGlobalInformation("interrupts"sv)
{
}
UNMAP_AFTER_INIT ProcFSKeymap::ProcFSKeymap()
    : ProcFSGlobalInformation("keymap"sv)
{
}
UNMAP_AFTER_INIT ProcFSPCI::ProcFSPCI()
    : ProcFSGlobalInformation("pci"sv)
{
}
UNMAP_AFTER_INIT ProcFSDevices::ProcFSDevices()
    : ProcFSGlobalInformation("devices"sv)
{
}
UNMAP_AFTER_INIT ProcFSUptime::ProcFSUptime()
    : ProcFSGlobalInformation("uptime"sv)
{
}
UNMAP_AFTER_INIT ProcFSCommandLine::ProcFSCommandLine()
    : ProcFSGlobalInformation("cmdline"sv)
{
}
UNMAP_AFTER_INIT ProcFSModules::ProcFSModules()
    : ProcFSGlobalInformation("modules"sv)
{
}
UNMAP_AFTER_INIT ProcFSProfile::ProcFSProfile()
    : ProcFSGlobalInformation("profile"sv)
{
}

class ProcFSAdapters final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSAdapters> must_create();

private:
    ProcFSAdapters();
    virtual bool output(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        NetworkingManagement::the().for_each([&array](auto& adapter) {
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
};

class ProcFSARP final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSARP> must_create();

private:
    ProcFSARP();
    virtual bool output(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        Locker locker(arp_table().lock(), Lock::Mode::Shared);
        for (auto& it : arp_table().resource()) {
            auto obj = array.add_object();
            obj.add("mac_address", it.value.to_string());
            obj.add("ip_address", it.key.to_string());
        }
        array.finish();
        return true;
    }
};

class ProcFSTCP final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSTCP> must_create();

private:
    ProcFSTCP();
    virtual bool output(KBufferBuilder& builder) override
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
};

class ProcFSLocalNet final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSLocalNet> must_create();

private:
    ProcFSLocalNet();
    virtual bool output(KBufferBuilder& builder) override
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
};

class ProcFSUDP final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSUDP> must_create();

private:
    ProcFSUDP();
    virtual bool output(KBufferBuilder& builder) override
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
};

class ProcFSNetworkDirectory : public ProcFSExposedFolder {
public:
    static NonnullRefPtr<ProcFSNetworkDirectory> must_create(const ProcFSRootFolder& parent_folder);

private:
    ProcFSNetworkDirectory(const ProcFSRootFolder& parent_folder);
};

class ProcFSSystemDirectory : public ProcFSExposedFolder {
public:
    static NonnullRefPtr<ProcFSSystemDirectory> must_create(const ProcFSRootFolder& parent_folder);

private:
    ProcFSSystemDirectory(const ProcFSRootFolder& parent_folder);
};

UNMAP_AFTER_INIT NonnullRefPtr<ProcFSAdapters> ProcFSAdapters::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSAdapters).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSARP> ProcFSARP::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSARP).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSTCP> ProcFSTCP::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSTCP).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSLocalNet> ProcFSLocalNet::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSLocalNet).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSUDP> ProcFSUDP::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSUDP).release_nonnull();
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSNetworkDirectory> ProcFSNetworkDirectory::must_create(const ProcFSRootFolder& parent_folder)
{
    auto folder = adopt_ref(*new (nothrow) ProcFSNetworkDirectory(parent_folder));
    folder->m_components.append(ProcFSAdapters::must_create());
    folder->m_components.append(ProcFSARP::must_create());
    folder->m_components.append(ProcFSTCP::must_create());
    folder->m_components.append(ProcFSLocalNet::must_create());
    folder->m_components.append(ProcFSUDP::must_create());
    return folder;
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSBusDirectory> ProcFSBusDirectory::must_create(const ProcFSRootFolder& parent_folder)
{
    auto folder = adopt_ref(*new (nothrow) ProcFSBusDirectory(parent_folder));
    return folder;
}
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSSystemDirectory> ProcFSSystemDirectory::must_create(const ProcFSRootFolder& parent_folder)
{
    auto folder = adopt_ref(*new (nothrow) ProcFSSystemDirectory(parent_folder));
    folder->m_components.append(ProcFSDumpKmallocStacks::must_create(folder));
    folder->m_components.append(ProcFSUBSanDeadly::must_create(folder));
    folder->m_components.append(ProcFSCapsLockRemap::must_create(folder));
    return folder;
}

UNMAP_AFTER_INIT ProcFSAdapters::ProcFSAdapters()
    : ProcFSGlobalInformation("adapters"sv)
{
}
UNMAP_AFTER_INIT ProcFSARP::ProcFSARP()
    : ProcFSGlobalInformation("arp"sv)
{
}
UNMAP_AFTER_INIT ProcFSTCP::ProcFSTCP()
    : ProcFSGlobalInformation("tcp"sv)
{
}
UNMAP_AFTER_INIT ProcFSLocalNet::ProcFSLocalNet()
    : ProcFSGlobalInformation("local"sv)
{
}
UNMAP_AFTER_INIT ProcFSUDP::ProcFSUDP()
    : ProcFSGlobalInformation("udp"sv)
{
}
UNMAP_AFTER_INIT ProcFSNetworkDirectory::ProcFSNetworkDirectory(const ProcFSRootFolder& parent_folder)
    : ProcFSExposedFolder("net"sv, parent_folder)
{
}
UNMAP_AFTER_INIT ProcFSBusDirectory::ProcFSBusDirectory(const ProcFSRootFolder& parent_folder)
    : ProcFSExposedFolder("bus"sv, parent_folder)
{
}
UNMAP_AFTER_INIT ProcFSSystemDirectory::ProcFSSystemDirectory(const ProcFSRootFolder& parent_folder)
    : ProcFSExposedFolder("sys"sv, parent_folder)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<ProcFSRootFolder> ProcFSRootFolder::must_create()
{
    auto folder = adopt_ref(*new (nothrow) ProcFSRootFolder);
    folder->m_components.append(ProcFSSelfProcessFolder::must_create());
    folder->m_components.append(ProcFSDiskUsage::must_create());
    folder->m_components.append(ProcFSMemoryStatus::must_create());
    folder->m_components.append(ProcFSOverallProcesses::must_create());
    folder->m_components.append(ProcFSCPUInformation::must_create());
    folder->m_components.append(ProcFSDmesg::must_create());
    folder->m_components.append(ProcFSInterrupts::must_create());
    folder->m_components.append(ProcFSKeymap::must_create());
    folder->m_components.append(ProcFSPCI::must_create());
    folder->m_components.append(ProcFSDevices::must_create());
    folder->m_components.append(ProcFSUptime::must_create());
    folder->m_components.append(ProcFSCommandLine::must_create());
    folder->m_components.append(ProcFSModules::must_create());
    folder->m_components.append(ProcFSProfile::must_create());

    folder->m_components.append(ProcFSNetworkDirectory::must_create(*folder));
    auto buses_folder = ProcFSBusDirectory::must_create(*folder);
    folder->m_components.append(buses_folder);
    folder->m_buses_folder = buses_folder;
    folder->m_components.append(ProcFSSystemDirectory::must_create(*folder));
    return folder;
}

UNMAP_AFTER_INIT ProcFSRootFolder::ProcFSRootFolder()
    : ProcFSExposedFolder("."sv)
{
}

UNMAP_AFTER_INIT ProcFSRootFolder::~ProcFSRootFolder()
{
}

class ProcFSProcessStacks;
class ProcFSThreadStack final : public ProcFSProcessInformation {
public:
    // Note: We pass const ProcFSProcessStacks& to enforce creation with this type of folder
    static NonnullRefPtr<ProcFSThreadStack> create(const ProcFSProcessFolder& process_folder, const ProcFSProcessStacks&, const Thread& thread)
    {
        return adopt_ref(*new (nothrow) ProcFSThreadStack(process_folder, thread));
    }

private:
    explicit ProcFSThreadStack(const ProcFSProcessFolder& process_folder, const Thread& thread)
        : ProcFSProcessInformation(String::formatted("{}", thread.tid()), process_folder)
        , m_associated_thread(thread)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        bool show_kernel_addresses = Process::current()->is_superuser();
        bool kernel_address_added = false;
        for (auto address : Processor::capture_stack_trace(*m_associated_thread, 1024)) {
            if (!show_kernel_addresses && !is_user_address(VirtualAddress { address })) {
                if (kernel_address_added)
                    continue;
                address = 0xdeadc0de;
                kernel_address_added = true;
            }
            array.add(JsonValue(address));
        }

        array.finish();
        return true;
    }

    NonnullRefPtr<Thread> m_associated_thread;
};

class ProcFSProcessStacks final : public ProcFSExposedFolder {
    // Note: This folder is special, because everything that is created here is dynamic!
    // This means we don't register anything in the m_components Vector, and every inode
    // is created in runtime when called to get it
    // Every ProcFSThreadStack (that represents a thread stack) is created only as a temporary object
    // therefore, we don't use m_components so when we are done with the ProcFSThreadStack object,
    // It should be deleted (as soon as possible)
public:
    virtual KResultOr<size_t> entries_count() const override;
    virtual KResult traverse_as_directory(unsigned, Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView name) override;

    static NonnullRefPtr<ProcFSProcessStacks> create(const ProcFSProcessFolder& parent_folder)
    {
        auto folder = adopt_ref(*new (nothrow) ProcFSProcessStacks(parent_folder));
        return folder;
    }

    virtual void prepare_for_deletion() override
    {
        ProcFSExposedFolder::prepare_for_deletion();
        m_process_folder.clear();
    }

private:
    ProcFSProcessStacks(const ProcFSProcessFolder& parent_folder)
        : ProcFSExposedFolder("stacks"sv, parent_folder)
        , m_process_folder(parent_folder)
    {
    }
    RefPtr<ProcFSProcessFolder> m_process_folder;
    mutable Lock m_lock;
};

KResultOr<size_t> ProcFSProcessStacks::entries_count() const
{
    Locker locker(m_lock);
    auto process = m_process_folder->m_associated_process;
    return process->thread_count();
}

KResult ProcFSProcessStacks::traverse_as_directory(unsigned fsid, Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    Locker locker(m_lock);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, m_parent_folder->component_index() }, 0 });

    auto process = m_process_folder->m_associated_process;
    process->for_each_thread([&](const Thread& thread) {
        int tid = thread.tid().value();
        InodeIdentifier identifier = { fsid, thread.global_procfs_inode_index() };
        callback({ String::number(tid), identifier, 0 });
    });
    return KSuccess;
}

RefPtr<ProcFSExposedComponent> ProcFSProcessStacks::lookup(StringView name)
{
    Locker locker(m_lock);
    auto process = m_process_folder->m_associated_process;
    RefPtr<ProcFSThreadStack> procfd_stack;
    // FIXME: Try to exit the loop earlier
    process->for_each_thread([&](const Thread& thread) {
        int tid = thread.tid().value();
        if (name == String::number(tid)) {
            procfd_stack = ProcFSThreadStack::create(*m_process_folder, *this, thread);
        }
    });
    return procfd_stack;
}

class ProcFSProcessFileDescriptions;
class ProcFSProcessFileDescription final : public ProcFSExposedLink {
public:
    // Note: we pass const ProcFSProcessFileDescriptions& just to enforce creation of this in the correct folder.
    static NonnullRefPtr<ProcFSProcessFileDescription> create(unsigned fd_number, const FileDescription& fd, InodeIndex preallocated_index, const ProcFSProcessFileDescriptions&)
    {
        return adopt_ref(*new (nothrow) ProcFSProcessFileDescription(fd_number, fd, preallocated_index));
    }

private:
    explicit ProcFSProcessFileDescription(unsigned fd_number, const FileDescription& fd, InodeIndex preallocated_index)
        : ProcFSExposedLink(String::formatted("{}", fd_number), preallocated_index)
        , m_associated_file_description(fd)
    {
    }
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        builder.append_bytes(m_associated_file_description->absolute_path().bytes());
        return true;
    }

    NonnullRefPtr<FileDescription> m_associated_file_description;
};

class ProcFSProcessFileDescriptions final : public ProcFSExposedFolder {
    // Note: This folder is special, because everything that is created here is dynamic!
    // This means we don't register anything in the m_components Vector, and every inode
    // is created in runtime when called to get it
    // Every ProcFSProcessFileDescription (that represents a file descriptor) is created only as a temporary object
    // therefore, we don't use m_components so when we are done with the ProcFSProcessFileDescription object,
    // It should be deleted (as soon as possible)
public:
    virtual KResultOr<size_t> entries_count() const override;
    virtual KResult traverse_as_directory(unsigned, Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView name) override;

    static NonnullRefPtr<ProcFSProcessFileDescriptions> create(const ProcFSProcessFolder& parent_folder)
    {
        return adopt_ref(*new (nothrow) ProcFSProcessFileDescriptions(parent_folder));
    }

    virtual void prepare_for_deletion() override
    {
        ProcFSExposedFolder::prepare_for_deletion();
        m_process_folder.clear();
    }

private:
    explicit ProcFSProcessFileDescriptions(const ProcFSProcessFolder& parent_folder)
        : ProcFSExposedFolder("fd"sv, parent_folder)
        , m_process_folder(parent_folder)
    {
    }
    RefPtr<ProcFSProcessFolder> m_process_folder;
    mutable Lock m_lock;
};

KResultOr<size_t> ProcFSProcessFileDescriptions::entries_count() const
{
    Locker locker(m_lock);
    return m_process_folder->m_associated_process->number_of_open_file_descriptors();
}
KResult ProcFSProcessFileDescriptions::traverse_as_directory(unsigned fsid, Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    Locker locker(m_lock);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, m_parent_folder->component_index() }, 0 });

    auto process = m_process_folder->m_associated_process;
    for (int i = 0; i < process->max_open_file_descriptors(); ++i) {
        auto description_metadata = process->fds()[i];
        if (!description_metadata.is_valid())
            continue;
        InodeIdentifier identifier = { fsid, description_metadata.global_procfs_inode_index() };
        callback({ String::number(i), identifier, 0 });
    }
    return KSuccess;
}
RefPtr<ProcFSExposedComponent> ProcFSProcessFileDescriptions::lookup(StringView name)
{
    Locker locker(m_lock);
    auto process = m_process_folder->m_associated_process;
    ScopedSpinLock lock(process->m_fds_lock);
    for (int i = 0; i < process->max_open_file_descriptors(); ++i) {
        auto description_metadata = process->fds()[i];
        if (!description_metadata.is_valid())
            continue;
        if (name == String::number(i)) {
            return ProcFSProcessFileDescription::create(i, *description_metadata.description(), description_metadata.global_procfs_inode_index(), *this);
        }
    }
    return nullptr;
}

class ProcFSProcessUnveil final : public ProcFSProcessInformation {
public:
    static NonnullRefPtr<ProcFSProcessUnveil> create(const ProcFSProcessFolder& parent_folder)
    {
        return adopt_ref(*new (nothrow) ProcFSProcessUnveil(parent_folder));
    }

private:
    explicit ProcFSProcessUnveil(const ProcFSProcessFolder& parent_folder)
        : ProcFSProcessInformation("unveil"sv, parent_folder)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        for (auto& unveiled_path : m_parent_folder->m_associated_process->unveiled_paths()) {
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
};

class ProcFSProcessPerformanceEvents final : public ProcFSProcessInformation {
public:
    static NonnullRefPtr<ProcFSProcessPerformanceEvents> create(const ProcFSProcessFolder& parent_folder)
    {
        return adopt_ref(*new (nothrow) ProcFSProcessPerformanceEvents(parent_folder));
    }

private:
    explicit ProcFSProcessPerformanceEvents(const ProcFSProcessFolder& parent_folder)
        : ProcFSProcessInformation("perf_events"sv, parent_folder)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        InterruptDisabler disabler;
        if (!m_parent_folder->m_associated_process->perf_events()) {
            dbgln("ProcFS: No perf events for {}", m_parent_folder->m_associated_process->pid());
            return false;
        }
        return m_parent_folder->m_associated_process->perf_events()->to_json(builder);
    }
};

class ProcFSProcessOverallFileDescriptions final : public ProcFSProcessInformation {
public:
    static NonnullRefPtr<ProcFSProcessOverallFileDescriptions> create(const ProcFSProcessFolder& parent_folder)
    {
        return adopt_ref(*new (nothrow) ProcFSProcessOverallFileDescriptions(parent_folder));
    }

private:
    explicit ProcFSProcessOverallFileDescriptions(const ProcFSProcessFolder& parent_folder)
        : ProcFSProcessInformation("fds"sv, parent_folder)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        auto process = m_parent_folder->m_associated_process;
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
};

class ProcFSProcessRoot final : public ProcFSExposedLink {
public:
    static NonnullRefPtr<ProcFSProcessRoot> create(const ProcFSProcessFolder& parent_folder)
    {
        return adopt_ref(*new (nothrow) ProcFSProcessRoot(parent_folder));
    }

private:
    explicit ProcFSProcessRoot(const ProcFSProcessFolder& parent_folder)
        : ProcFSExposedLink("root"sv)
        , m_parent_process_directory(parent_folder)
    {
    }
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        builder.append_bytes(m_parent_process_directory->m_associated_process->root_directory_relative_to_global_root().absolute_path().to_byte_buffer());
        return true;
    }
    NonnullRefPtr<ProcFSProcessFolder> m_parent_process_directory;
};

class ProcFSProcessVirtualMemory final : public ProcFSProcessInformation {
public:
    static NonnullRefPtr<ProcFSProcessRoot> create(const ProcFSProcessFolder& parent_folder)
    {
        return adopt_ref(*new (nothrow) ProcFSProcessVirtualMemory(parent_folder));
    }

private:
    explicit ProcFSProcessVirtualMemory(const ProcFSProcessFolder& parent_folder)
        : ProcFSProcessInformation("vm"sv, parent_folder)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        auto process = m_parent_folder->m_associated_process;
        JsonArraySerializer array { builder };
        {
            ScopedSpinLock lock(process->space().get_lock());
            for (auto& region : process->space().regions()) {
                if (!region->is_user() && !Process::current()->is_superuser())
                    continue;
                auto region_object = array.add_object();
                region_object.add("readable", region->is_readable());
                region_object.add("writable", region->is_writable());
                region_object.add("executable", region->is_executable());
                region_object.add("stack", region->is_stack());
                region_object.add("shared", region->is_shared());
                region_object.add("syscall", region->is_syscall_region());
                region_object.add("purgeable", region->vmobject().is_anonymous());
                if (region->vmobject().is_anonymous()) {
                    region_object.add("volatile", static_cast<const AnonymousVMObject&>(region->vmobject()).is_any_volatile());
                }
                region_object.add("cacheable", region->is_cacheable());
                region_object.add("address", region->vaddr().get());
                region_object.add("size", region->size());
                region_object.add("amount_resident", region->amount_resident());
                region_object.add("amount_dirty", region->amount_dirty());
                region_object.add("cow_pages", region->cow_pages());
                region_object.add("name", region->name());
                region_object.add("vmobject", region->vmobject().class_name());

                StringBuilder pagemap_builder;
                for (size_t i = 0; i < region->page_count(); ++i) {
                    auto* page = region->physical_page(i);
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
};

class ProcFSProcessCurrentWorkDirectory final : public ProcFSExposedLink {
public:
    static NonnullRefPtr<ProcFSProcessCurrentWorkDirectory> create(const ProcFSProcessFolder& parent_folder)
    {
        return adopt_ref(*new (nothrow) ProcFSProcessCurrentWorkDirectory(parent_folder));
    }

private:
    explicit ProcFSProcessCurrentWorkDirectory(const ProcFSProcessFolder& parent_folder)
        : ProcFSExposedLink("cwd"sv)
        , m_parent_process_directory(parent_folder)
    {
    }
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        builder.append_bytes(m_parent_process_directory->m_associated_process->current_directory().absolute_path().bytes());
        return true;
    }

    NonnullRefPtr<ProcFSProcessFolder> m_parent_process_directory;
};

class ProcFSProcessBinary final : public ProcFSExposedLink {
public:
    static NonnullRefPtr<ProcFSProcessBinary> create(const ProcFSProcessFolder& parent_folder)
    {
        return adopt_ref(*new (nothrow) ProcFSProcessBinary(parent_folder));
    }

    virtual mode_t required_mode() const override
    {
        if (!m_parent_process_directory->m_associated_process->executable())
            return 0;
        return ProcFSExposedComponent::required_mode();
    }

private:
    explicit ProcFSProcessBinary(const ProcFSProcessFolder& parent_folder)
        : ProcFSExposedLink("exe"sv)
        , m_parent_process_directory(parent_folder)
    {
    }
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        auto* custody = m_parent_process_directory->m_associated_process->executable();
        if (!custody)
            return false;
        builder.append(custody->absolute_path().bytes());
        return true;
    }

    NonnullRefPtr<ProcFSProcessFolder> m_parent_process_directory;
};

NonnullRefPtr<ProcFSProcessFolder> ProcFSProcessFolder::create(const Process& process)
{
    auto folder = adopt_ref_if_nonnull(new (nothrow) ProcFSProcessFolder(process)).release_nonnull();
    folder->m_components.append(ProcFSProcessUnveil::create(folder));
    folder->m_components.append(ProcFSProcessPerformanceEvents::create(folder));
    folder->m_components.append(ProcFSProcessFileDescriptions::create(folder));
    folder->m_components.append(ProcFSProcessOverallFileDescriptions::create(folder));
    folder->m_components.append(ProcFSProcessRoot::create(folder));
    folder->m_components.append(ProcFSProcessVirtualMemory::create(folder));
    folder->m_components.append(ProcFSProcessCurrentWorkDirectory::create(folder));
    folder->m_components.append(ProcFSProcessBinary::create(folder));
    folder->m_components.append(ProcFSProcessStacks::create(folder));
    return folder;
}

ProcFSProcessFolder::ProcFSProcessFolder(const Process& process)
    : ProcFSExposedFolder(String::formatted("{:d}", process.pid().value()), ProcFSComponentsRegistrar::the().root_folder())
    , m_associated_process(process)
{
}

KResultOr<size_t> ProcFSExposedLink::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription*) const
{
    VERIFY(offset == 0);
    Locker locker(m_lock);
    KBufferBuilder builder;
    if (!const_cast<ProcFSExposedLink&>(*this).acquire_link(builder))
        return KResult(EFAULT);
    auto blob = builder.build();
    if (!blob)
        return KResult(EFAULT);

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(blob->data() + offset, nread))
        return KResult(EFAULT);
    return nread;
}

NonnullRefPtr<Inode> ProcFSExposedLink::to_inode(const ProcFS& procfs_instance) const
{
    return ProcFSLinkInode::create(procfs_instance, *this);
}

NonnullRefPtr<Inode> ProcFSExposedComponent::to_inode(const ProcFS& procfs_instance) const
{
    return ProcFSInode::create(procfs_instance, *this);
}

NonnullRefPtr<Inode> ProcFSExposedFolder::to_inode(const ProcFS& procfs_instance) const
{
    return ProcFSDirectoryInode::create(procfs_instance, *this);
}

void ProcFSExposedFolder::add_component(const ProcFSExposedComponent&)
{
    TODO();
}

RefPtr<ProcFSExposedComponent> ProcFSExposedFolder::lookup(StringView name)
{
    for (auto& component : m_components) {
        if (component.name() == name) {
            return component;
        }
    }
    return {};
}

KResult ProcFSExposedFolder::traverse_as_directory(unsigned fsid, Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    Locker locker(ProcFSComponentsRegistrar::the().m_lock);
    VERIFY(m_parent_folder);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, m_parent_folder->component_index() }, 0 });

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    return KSuccess;
}

RefPtr<ProcFSExposedComponent> ProcFSRootFolder::lookup(StringView name)
{
    if (auto candidate = ProcFSExposedFolder::lookup(name); !candidate.is_null())
        return candidate;

    for (auto& component : m_process_folders) {
        if (component.name() == name) {
            return component;
        }
    }
    return {};
}

KResult ProcFSRootFolder::traverse_as_directory(unsigned fsid, Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    Locker locker(ProcFSComponentsRegistrar::the().m_lock);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, 0 }, 0 });

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    for (auto& component : m_process_folders) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    return KSuccess;
}

}
