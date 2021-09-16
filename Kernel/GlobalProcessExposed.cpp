/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <AK/UBSanitizer.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Arch/x86/ProcessorInfo.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/ConsoleDevice.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

class ProcFSAdapters final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSAdapters> must_create();

private:
    ProcFSAdapters();
    virtual KResult try_generate(KBufferBuilder& builder) override
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
            obj.add("link_speed", adapter.link_speed());
            obj.add("link_full_duplex", adapter.link_full_duplex());
            obj.add("mtu", adapter.mtu());
        });
        array.finish();
        return KSuccess;
    }
};

class ProcFSARP final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSARP> must_create();

private:
    ProcFSARP();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        arp_table().for_each_shared([&](const auto& it) {
            auto obj = array.add_object();
            obj.add("mac_address", it.value.to_string());
            obj.add("ip_address", it.key.to_string());
        });
        array.finish();
        return KSuccess;
    }
};

class ProcFSTCP final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSTCP> must_create();

private:
    ProcFSTCP();
    virtual KResult try_generate(KBufferBuilder& builder) override
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
            if (Process::current().is_superuser() || Process::current().uid() == socket.origin_uid()) {
                obj.add("origin_pid", socket.origin_pid().value());
                obj.add("origin_uid", socket.origin_uid().value());
                obj.add("origin_gid", socket.origin_gid().value());
            }
        });
        array.finish();
        return KSuccess;
    }
};

class ProcFSLocalNet final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSLocalNet> must_create();

private:
    ProcFSLocalNet();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        LocalSocket::for_each([&array](auto& socket) {
            auto obj = array.add_object();
            obj.add("path", String(socket.socket_path()));
            obj.add("origin_pid", socket.origin_pid().value());
            obj.add("origin_uid", socket.origin_uid().value());
            obj.add("origin_gid", socket.origin_gid().value());
            obj.add("acceptor_pid", socket.acceptor_pid().value());
            obj.add("acceptor_uid", socket.acceptor_uid().value());
            obj.add("acceptor_gid", socket.acceptor_gid().value());
        });
        array.finish();
        return KSuccess;
    }
};

class ProcFSUDP final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSUDP> must_create();

private:
    ProcFSUDP();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        UDPSocket::for_each([&array](auto& socket) {
            auto obj = array.add_object();
            obj.add("local_address", socket.local_address().to_string());
            obj.add("local_port", socket.local_port());
            obj.add("peer_address", socket.peer_address().to_string());
            obj.add("peer_port", socket.peer_port());
            if (Process::current().is_superuser() || Process::current().uid() == socket.origin_uid()) {
                obj.add("origin_pid", socket.origin_pid().value());
                obj.add("origin_uid", socket.origin_uid().value());
                obj.add("origin_gid", socket.origin_gid().value());
            }
        });
        array.finish();
        return KSuccess;
    }
};

class ProcFSNetworkDirectory : public ProcFSExposedDirectory {
public:
    static NonnullRefPtr<ProcFSNetworkDirectory> must_create(const ProcFSRootDirectory& parent_directory);

private:
    ProcFSNetworkDirectory(const ProcFSRootDirectory& parent_directory);
};

class ProcFSSystemDirectory : public ProcFSExposedDirectory {
public:
    static NonnullRefPtr<ProcFSSystemDirectory> must_create(const ProcFSRootDirectory& parent_directory);

private:
    ProcFSSystemDirectory(const ProcFSRootDirectory& parent_directory);
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

UNMAP_AFTER_INIT NonnullRefPtr<ProcFSNetworkDirectory> ProcFSNetworkDirectory::must_create(const ProcFSRootDirectory& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) ProcFSNetworkDirectory(parent_directory));
    directory->m_components.append(ProcFSAdapters::must_create());
    directory->m_components.append(ProcFSARP::must_create());
    directory->m_components.append(ProcFSTCP::must_create());
    directory->m_components.append(ProcFSLocalNet::must_create());
    directory->m_components.append(ProcFSUDP::must_create());
    return directory;
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
UNMAP_AFTER_INIT ProcFSNetworkDirectory::ProcFSNetworkDirectory(const ProcFSRootDirectory& parent_directory)
    : ProcFSExposedDirectory("net"sv, parent_directory)
{
}

class ProcFSDumpKmallocStacks : public ProcFSSystemBoolean {
public:
    static NonnullRefPtr<ProcFSDumpKmallocStacks> must_create(const ProcFSSystemDirectory&);
    virtual bool value() const override
    {
        MutexLocker locker(m_lock);
        return g_dump_kmalloc_stacks;
    }
    virtual void set_value(bool new_value) override
    {
        MutexLocker locker(m_lock);
        g_dump_kmalloc_stacks = new_value;
    }

private:
    ProcFSDumpKmallocStacks();
    mutable Mutex m_lock;
};

class ProcFSUBSanDeadly : public ProcFSSystemBoolean {
public:
    static NonnullRefPtr<ProcFSUBSanDeadly> must_create(const ProcFSSystemDirectory&);
    virtual bool value() const override
    {
        MutexLocker locker(m_lock);
        return AK::UBSanitizer::g_ubsan_is_deadly;
    }
    virtual void set_value(bool new_value) override
    {
        MutexLocker locker(m_lock);
        AK::UBSanitizer::g_ubsan_is_deadly = new_value;
    }

private:
    ProcFSUBSanDeadly();
    mutable Mutex m_lock;
};

class ProcFSCapsLockRemap : public ProcFSSystemBoolean {
public:
    static NonnullRefPtr<ProcFSCapsLockRemap> must_create(const ProcFSSystemDirectory&);
    virtual bool value() const override
    {
        MutexLocker locker(m_lock);
        return g_caps_lock_remapped_to_ctrl.load();
    }
    virtual void set_value(bool new_value) override
    {
        MutexLocker locker(m_lock);
        g_caps_lock_remapped_to_ctrl.exchange(new_value);
    }

private:
    ProcFSCapsLockRemap();
    mutable Mutex m_lock;
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

class ProcFSSelfProcessDirectory final : public ProcFSExposedLink {
public:
    static NonnullRefPtr<ProcFSSelfProcessDirectory> must_create();

private:
    ProcFSSelfProcessDirectory();
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        if (builder.appendff("{}", Process::current().pid().value()).is_error())
            return false;
        return true;
    }
};

class ProcFSDiskUsage final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSDiskUsage> must_create();

private:
    ProcFSDiskUsage();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        VirtualFileSystem::the().for_each_mount([&array](auto& mount) {
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
                fs_object.add("source", static_cast<const FileBackedFileSystem&>(fs).file_description().absolute_path());
            else
                fs_object.add("source", "none");
        });
        array.finish();
        return KSuccess;
    }
};

class ProcFSMemoryStatus final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSMemoryStatus> must_create();

private:
    ProcFSMemoryStatus();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        InterruptDisabler disabler;

        kmalloc_stats stats;
        get_kmalloc_stats(stats);

        auto system_memory = MM.get_system_memory_info();

        JsonObjectSerializer<KBufferBuilder> json { builder };
        json.add("kmalloc_allocated", stats.bytes_allocated);
        json.add("kmalloc_available", stats.bytes_free);
        json.add("kmalloc_eternal_allocated", stats.bytes_eternal);
        json.add("user_physical_allocated", system_memory.user_physical_pages_used);
        json.add("user_physical_available", system_memory.user_physical_pages - system_memory.user_physical_pages_used);
        json.add("user_physical_committed", system_memory.user_physical_pages_committed);
        json.add("user_physical_uncommitted", system_memory.user_physical_pages_uncommitted);
        json.add("super_physical_allocated", system_memory.super_physical_pages_used);
        json.add("super_physical_available", system_memory.super_physical_pages - system_memory.super_physical_pages_used);
        json.add("kmalloc_call_count", stats.kmalloc_call_count);
        json.add("kfree_call_count", stats.kfree_call_count);
        slab_alloc_stats([&json](size_t slab_size, size_t num_allocated, size_t num_free) {
            auto prefix = String::formatted("slab_{}", slab_size);
            json.add(String::formatted("{}_num_allocated", prefix), num_allocated);
            json.add(String::formatted("{}_num_free", prefix), num_free);
        });
        json.finish();
        return KSuccess;
    }
};

class ProcFSOverallProcesses final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSOverallProcesses> must_create();

private:
    ProcFSOverallProcesses();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        JsonObjectSerializer<KBufferBuilder> json { builder };

        // Keep this in sync with CProcessStatistics.
        auto build_process = [&](JsonArraySerializer<KBufferBuilder>& array, const Process& process) {
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
            process_object.add("uid", process.uid().value());
            process_object.add("gid", process.gid().value());
            process_object.add("ppid", process.ppid().value());
            process_object.add("nfds", process.fds().open_count());
            process_object.add("name", process.name());
            process_object.add("executable", process.executable() ? process.executable()->absolute_path() : "");
            process_object.add("tty", process.tty() ? process.tty()->tty_name() : "notty");
            process_object.add("amount_virtual", process.address_space().amount_virtual());
            process_object.add("amount_resident", process.address_space().amount_resident());
            process_object.add("amount_dirty_private", process.address_space().amount_dirty_private());
            process_object.add("amount_clean_inode", process.address_space().amount_clean_inode());
            process_object.add("amount_shared", process.address_space().amount_shared());
            process_object.add("amount_purgeable_volatile", process.address_space().amount_purgeable_volatile());
            process_object.add("amount_purgeable_nonvolatile", process.address_space().amount_purgeable_nonvolatile());
            process_object.add("dumpable", process.is_dumpable());
            process_object.add("kernel", process.is_kernel_process());
            auto thread_array = process_object.add_array("threads");
            process.for_each_thread([&](const Thread& thread) {
                SpinlockLocker locker(thread.get_lock());
                auto thread_object = thread_array.add_object();
#if LOCK_DEBUG
                thread_object.add("lock_count", thread.lock_count());
#endif
                thread_object.add("tid", thread.tid().value());
                thread_object.add("name", thread.name());
                thread_object.add("times_scheduled", thread.times_scheduled());
                thread_object.add("time_user", thread.time_in_user());
                thread_object.add("time_kernel", thread.time_in_kernel());
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

        SpinlockLocker lock(g_scheduler_lock);
        {
            {
                auto array = json.add_array("processes");
                auto processes = Process::all_processes();
                build_process(array, *Scheduler::colonel());
                for (auto& process : processes)
                    build_process(array, process);
            }

            auto total_time_scheduled = Scheduler::get_total_time_scheduled();
            json.add("total_time", total_time_scheduled.total);
            json.add("total_time_kernel", total_time_scheduled.total_kernel);
        }
        return KSuccess;
    }
};
class ProcFSCPUInformation final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSCPUInformation> must_create();

private:
    ProcFSCPUInformation();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        Processor::for_each(
            [&](Processor& proc) {
                auto& info = proc.info();
                auto obj = array.add_object();
                obj.add("processor", proc.id());
                obj.add("cpuid", info.cpuid());
                obj.add("family", info.display_family());

                auto features_array = obj.add_array("features");
                for (auto& feature : info.features().split(' '))
                    features_array.add(feature);
                features_array.finish();

                obj.add("model", info.display_model());
                obj.add("stepping", info.stepping());
                obj.add("type", info.type());
                obj.add("brandstr", info.brandstr());
            });
        array.finish();
        return KSuccess;
    }
};
class ProcFSDmesg final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSDmesg> must_create();

    virtual mode_t required_mode() const override { return 0400; }

private:
    ProcFSDmesg();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        VERIFY(DeviceManagement::the().is_console_device_attached());
        InterruptDisabler disabler;
        for (char ch : DeviceManagement::the().console_device().logbuffer()) {
            TRY(builder.append(ch));
        }
        return KSuccess;
    }
};
class ProcFSInterrupts final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSInterrupts> must_create();

private:
    ProcFSInterrupts();
    virtual KResult try_generate(KBufferBuilder& builder) override
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
        return KSuccess;
    }
};
class ProcFSKeymap final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSKeymap> must_create();

private:
    ProcFSKeymap();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        JsonObjectSerializer<KBufferBuilder> json { builder };
        json.add("keymap", HIDManagement::the().keymap_name());
        json.finish();
        return KSuccess;
    }
};

// FIXME: Remove this after we enumerate the SysFS from lspci and SystemMonitor
class ProcFSPCI final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSPCI> must_create();

private:
    ProcFSPCI();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        PCI::enumerate([&array](PCI::Address address, PCI::ID id) {
            auto obj = array.add_object();
            obj.add("domain", address.domain());
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
        return KSuccess;
    }
};

class ProcFSDevices final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSDevices> must_create();

private:
    ProcFSDevices();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        DeviceManagement::the().for_each([&array](auto& device) {
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
        return KSuccess;
    }
};
class ProcFSUptime final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSUptime> must_create();

private:
    ProcFSUptime();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        return builder.appendff("{}\n", TimeManagement::the().uptime_ms() / 1000);
    }
};
class ProcFSCommandLine final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSCommandLine> must_create();

private:
    ProcFSCommandLine();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        TRY(builder.append(kernel_command_line().string()));
        TRY(builder.append('\n'));
        return KSuccess;
    }
};

class ProcFSProfile final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSProfile> must_create();

    virtual mode_t required_mode() const override { return 0400; }

private:
    ProcFSProfile();
    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        if (!g_global_perf_events)
            return ENOENT;
        TRY(g_global_perf_events->to_json(builder));
        return KSuccess;
    }
};

class ProcFSKernelBase final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSKernelBase> must_create();

private:
    ProcFSKernelBase();

    virtual mode_t required_mode() const override { return 0400; }

    virtual KResult try_generate(KBufferBuilder& builder) override
    {
        if (!Process::current().is_superuser())
            return EPERM;
        return builder.append(String::number(kernel_load_base));
    }
};

UNMAP_AFTER_INIT NonnullRefPtr<ProcFSSelfProcessDirectory> ProcFSSelfProcessDirectory::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSSelfProcessDirectory()).release_nonnull();
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
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSProfile> ProcFSProfile::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSProfile).release_nonnull();
}

UNMAP_AFTER_INIT NonnullRefPtr<ProcFSKernelBase> ProcFSKernelBase::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSKernelBase).release_nonnull();
}

UNMAP_AFTER_INIT ProcFSSelfProcessDirectory::ProcFSSelfProcessDirectory()
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
UNMAP_AFTER_INIT ProcFSProfile::ProcFSProfile()
    : ProcFSGlobalInformation("profile"sv)
{
}

UNMAP_AFTER_INIT ProcFSKernelBase::ProcFSKernelBase()
    : ProcFSGlobalInformation("kernel_base"sv)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<ProcFSSystemDirectory> ProcFSSystemDirectory::must_create(const ProcFSRootDirectory& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) ProcFSSystemDirectory(parent_directory));
    directory->m_components.append(ProcFSDumpKmallocStacks::must_create(directory));
    directory->m_components.append(ProcFSUBSanDeadly::must_create(directory));
    directory->m_components.append(ProcFSCapsLockRemap::must_create(directory));
    return directory;
}

UNMAP_AFTER_INIT ProcFSSystemDirectory::ProcFSSystemDirectory(const ProcFSRootDirectory& parent_directory)
    : ProcFSExposedDirectory("sys"sv, parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<ProcFSRootDirectory> ProcFSRootDirectory::must_create()
{
    auto directory = adopt_ref(*new (nothrow) ProcFSRootDirectory);
    directory->m_components.append(ProcFSSelfProcessDirectory::must_create());
    directory->m_components.append(ProcFSDiskUsage::must_create());
    directory->m_components.append(ProcFSMemoryStatus::must_create());
    directory->m_components.append(ProcFSOverallProcesses::must_create());
    directory->m_components.append(ProcFSCPUInformation::must_create());
    directory->m_components.append(ProcFSDmesg::must_create());
    directory->m_components.append(ProcFSInterrupts::must_create());
    directory->m_components.append(ProcFSKeymap::must_create());
    directory->m_components.append(ProcFSPCI::must_create());
    directory->m_components.append(ProcFSDevices::must_create());
    directory->m_components.append(ProcFSUptime::must_create());
    directory->m_components.append(ProcFSCommandLine::must_create());
    directory->m_components.append(ProcFSProfile::must_create());
    directory->m_components.append(ProcFSKernelBase::must_create());

    directory->m_components.append(ProcFSNetworkDirectory::must_create(*directory));
    directory->m_components.append(ProcFSSystemDirectory::must_create(*directory));
    return directory;
}

KResult ProcFSRootDirectory::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(ProcFSComponentRegistry::the().get_lock());
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, 0 }, 0 });

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    processes().for_each([&](Process& process) {
        VERIFY(!(process.pid() < 0));
        u64 process_id = (u64)process.pid().value();
        InodeIdentifier identifier = { fsid, static_cast<InodeIndex>(process_id << 36) };
        callback({ String::formatted("{:d}", process.pid().value()), identifier, 0 });
        return IterationDecision::Continue;
    });
    return KSuccess;
}

KResultOr<NonnullRefPtr<ProcFSExposedComponent>> ProcFSRootDirectory::lookup(StringView name)
{
    auto maybe_candidate = ProcFSExposedDirectory::lookup(name);
    if (maybe_candidate.is_error()) {
        if (maybe_candidate.error() != ENOENT) {
            return maybe_candidate.error();
        }
    } else {
        return maybe_candidate.release_value();
    }

    String process_directory_name = name;
    auto pid = process_directory_name.to_uint<unsigned>();
    if (!pid.has_value())
        return ESRCH;
    auto actual_pid = pid.value();

    auto maybe_process = Process::from_pid(actual_pid);
    if (maybe_process) {
        return maybe_process->procfs_traits();
    }
    return ENOENT;
}

UNMAP_AFTER_INIT ProcFSRootDirectory::ProcFSRootDirectory()
    : ProcFSExposedDirectory("."sv)
{
}

UNMAP_AFTER_INIT ProcFSRootDirectory::~ProcFSRootDirectory()
{
}

}
