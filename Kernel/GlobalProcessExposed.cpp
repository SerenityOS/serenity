/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <AK/UBSanitizer.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Arch/x86/ProcessorInfo.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/CommandLine.h>
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
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

class ProcFSAdapters final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSAdapters> must_create();

private:
    ProcFSAdapters();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(NetworkingManagement::the().try_for_each([&array](auto& adapter) -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            TRY(obj.add("name", adapter.name()));
            TRY(obj.add("class_name", adapter.class_name()));
            auto mac_address = adapter.mac_address().to_string().release_value_but_fixme_should_propagate_errors();
            TRY(obj.add("mac_address", mac_address->view()));
            if (!adapter.ipv4_address().is_zero()) {
                auto ipv4_address = adapter.ipv4_address().to_string().release_value_but_fixme_should_propagate_errors();
                TRY(obj.add("ipv4_address", ipv4_address->view()));
                auto ipv4_netmask = adapter.ipv4_netmask().to_string().release_value_but_fixme_should_propagate_errors();
                TRY(obj.add("ipv4_netmask", ipv4_netmask->view()));
            }
            if (!adapter.ipv4_gateway().is_zero()) {
                auto ipv4_gateway = adapter.ipv4_gateway().to_string().release_value_but_fixme_should_propagate_errors();
                TRY(obj.add("ipv4_gateway", ipv4_gateway->view()));
            }
            TRY(obj.add("packets_in", adapter.packets_in()));
            TRY(obj.add("bytes_in", adapter.bytes_in()));
            TRY(obj.add("packets_out", adapter.packets_out()));
            TRY(obj.add("bytes_out", adapter.bytes_out()));
            TRY(obj.add("link_up", adapter.link_up()));
            TRY(obj.add("link_speed", adapter.link_speed()));
            TRY(obj.add("link_full_duplex", adapter.link_full_duplex()));
            TRY(obj.add("mtu", adapter.mtu()));
            TRY(obj.finish());
            return {};
        }));
        TRY(array.finish());
        return {};
    }
};

class ProcFSARP final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSARP> must_create();

private:
    ProcFSARP();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(arp_table().with([&](const auto& table) -> ErrorOr<void> {
            for (auto& it : table) {
                auto obj = TRY(array.add_object());
                auto mac_address = it.value.to_string().release_value_but_fixme_should_propagate_errors();
                TRY(obj.add("mac_address", mac_address->view()));
                auto ip_address = it.key.to_string().release_value_but_fixme_should_propagate_errors();
                TRY(obj.add("ip_address", ip_address->view()));
                TRY(obj.finish());
            }
            return {};
        }));
        TRY(array.finish());
        return {};
    }
};

class ProcFSTCP final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSTCP> must_create();

private:
    ProcFSTCP();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(TCPSocket::try_for_each([&array](auto& socket) -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            auto local_address = socket.local_address().to_string().release_value_but_fixme_should_propagate_errors();
            TRY(obj.add("local_address", local_address->view()));
            TRY(obj.add("local_port", socket.local_port()));
            auto peer_address = socket.peer_address().to_string().release_value_but_fixme_should_propagate_errors();
            TRY(obj.add("peer_address", peer_address->view()));
            TRY(obj.add("peer_port", socket.peer_port()));
            TRY(obj.add("state", TCPSocket::to_string(socket.state())));
            TRY(obj.add("ack_number", socket.ack_number()));
            TRY(obj.add("sequence_number", socket.sequence_number()));
            TRY(obj.add("packets_in", socket.packets_in()));
            TRY(obj.add("bytes_in", socket.bytes_in()));
            TRY(obj.add("packets_out", socket.packets_out()));
            TRY(obj.add("bytes_out", socket.bytes_out()));
            if (Process::current().is_superuser() || Process::current().uid() == socket.origin_uid()) {
                TRY(obj.add("origin_pid", socket.origin_pid().value()));
                TRY(obj.add("origin_uid", socket.origin_uid().value()));
                TRY(obj.add("origin_gid", socket.origin_gid().value()));
            }
            TRY(obj.finish());
            return {};
        }));
        TRY(array.finish());
        return {};
    }
};

class ProcFSLocalNet final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSLocalNet> must_create();

private:
    ProcFSLocalNet();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(LocalSocket::try_for_each([&array](auto& socket) -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            TRY(obj.add("path", socket.socket_path()));
            TRY(obj.add("origin_pid", socket.origin_pid().value()));
            TRY(obj.add("origin_uid", socket.origin_uid().value()));
            TRY(obj.add("origin_gid", socket.origin_gid().value()));
            TRY(obj.add("acceptor_pid", socket.acceptor_pid().value()));
            TRY(obj.add("acceptor_uid", socket.acceptor_uid().value()));
            TRY(obj.add("acceptor_gid", socket.acceptor_gid().value()));
            TRY(obj.finish());
            return {};
        }));
        TRY(array.finish());
        return {};
    }
};

class ProcFSUDP final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSUDP> must_create();

private:
    ProcFSUDP();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(UDPSocket::try_for_each([&array](auto& socket) -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            auto local_address = socket.local_address().to_string().release_value_but_fixme_should_propagate_errors();
            TRY(obj.add("local_address", local_address->view()));
            TRY(obj.add("local_port", socket.local_port()));
            auto peer_address = socket.peer_address().to_string().release_value_but_fixme_should_propagate_errors();
            TRY(obj.add("peer_address", peer_address->view()));
            TRY(obj.add("peer_port", socket.peer_port()));
            if (Process::current().is_superuser() || Process::current().uid() == socket.origin_uid()) {
                TRY(obj.add("origin_pid", socket.origin_pid().value()));
                TRY(obj.add("origin_uid", socket.origin_uid().value()));
                TRY(obj.add("origin_gid", socket.origin_gid().value()));
            }
            TRY(obj.finish());
            return {};
        }));
        TRY(array.finish());
        return {};
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

    virtual bool value() const override { return AK::UBSanitizer::g_ubsan_is_deadly; }
    virtual void set_value(bool new_value) override { AK::UBSanitizer::g_ubsan_is_deadly = new_value; }

private:
    ProcFSUBSanDeadly();
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
        return !builder.appendff("{}", Process::current().pid().value()).is_error();
    }
};

class ProcFSDiskUsage final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSDiskUsage> must_create();

private:
    ProcFSDiskUsage();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(VirtualFileSystem::the().for_each_mount([&array](auto& mount) -> ErrorOr<void> {
            auto& fs = mount.guest_fs();
            auto fs_object = TRY(array.add_object());
            TRY(fs_object.add("class_name", fs.class_name()));
            TRY(fs_object.add("total_block_count", fs.total_block_count()));
            TRY(fs_object.add("free_block_count", fs.free_block_count()));
            TRY(fs_object.add("total_inode_count", fs.total_inode_count()));
            TRY(fs_object.add("free_inode_count", fs.free_inode_count()));
            auto mount_point = TRY(mount.absolute_path());
            TRY(fs_object.add("mount_point", mount_point->view()));
            TRY(fs_object.add("block_size", static_cast<u64>(fs.block_size())));
            TRY(fs_object.add("readonly", fs.is_readonly()));
            TRY(fs_object.add("mount_flags", mount.flags()));

            if (fs.is_file_backed()) {
                auto pseudo_path = TRY(static_cast<const FileBackedFileSystem&>(fs).file_description().pseudo_path());
                TRY(fs_object.add("source", pseudo_path->view()));
            } else {
                TRY(fs_object.add("source", "none"));
            }

            TRY(fs_object.finish());
            return {};
        }));
        TRY(array.finish());
        return {};
    }
};

class ProcFSMemoryStatus final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSMemoryStatus> must_create();

private:
    ProcFSMemoryStatus();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        InterruptDisabler disabler;

        kmalloc_stats stats;
        get_kmalloc_stats(stats);

        auto system_memory = MM.get_system_memory_info();

        auto json = TRY(JsonObjectSerializer<>::try_create(builder));
        TRY(json.add("kmalloc_allocated", stats.bytes_allocated));
        TRY(json.add("kmalloc_available", stats.bytes_free));
        TRY(json.add("user_physical_allocated", system_memory.user_physical_pages_used));
        TRY(json.add("user_physical_available", system_memory.user_physical_pages - system_memory.user_physical_pages_used));
        TRY(json.add("user_physical_committed", system_memory.user_physical_pages_committed));
        TRY(json.add("user_physical_uncommitted", system_memory.user_physical_pages_uncommitted));
        TRY(json.add("super_physical_allocated", system_memory.super_physical_pages_used));
        TRY(json.add("super_physical_available", system_memory.super_physical_pages - system_memory.super_physical_pages_used));
        TRY(json.add("kmalloc_call_count", stats.kmalloc_call_count));
        TRY(json.add("kfree_call_count", stats.kfree_call_count));
        TRY(json.finish());
        return {};
    }
};

class ProcFSSystemStatistics final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSSystemStatistics> must_create();

private:
    ProcFSSystemStatistics();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto json = TRY(JsonObjectSerializer<>::try_create(builder));
        auto total_time_scheduled = Scheduler::get_total_time_scheduled();
        TRY(json.add("total_time", total_time_scheduled.total));
        TRY(json.add("kernel_time", total_time_scheduled.total_kernel));
        TRY(json.add("user_time", total_time_scheduled.total - total_time_scheduled.total_kernel));
        u64 idle_time = 0;
        Processor::for_each([&](Processor& processor) {
            idle_time += processor.time_spent_idle();
        });
        TRY(json.add("idle_time", idle_time));
        TRY(json.finish());
        return {};
    }
};

class ProcFSOverallProcesses final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSOverallProcesses> must_create();

private:
    ProcFSOverallProcesses();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto json = TRY(JsonObjectSerializer<>::try_create(builder));

        // Keep this in sync with CProcessStatistics.
        auto build_process = [&](JsonArraySerializer<KBufferBuilder>& array, const Process& process) -> ErrorOr<void> {
            auto process_object = TRY(array.add_object());

            if (process.is_user_process()) {
                StringBuilder pledge_builder;

#define __ENUMERATE_PLEDGE_PROMISE(promise)    \
    if (process.has_promised(Pledge::promise)) \
        TRY(pledge_builder.try_append(#promise " "));
                ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE

                TRY(process_object.add("pledge", pledge_builder.string_view()));

                switch (process.veil_state()) {
                case VeilState::None:
                    TRY(process_object.add("veil", "None"));
                    break;
                case VeilState::Dropped:
                    TRY(process_object.add("veil", "Dropped"));
                    break;
                case VeilState::Locked:
                    TRY(process_object.add("veil", "Locked"));
                    break;
                }
            } else {
                TRY(process_object.add("pledge", ""sv));
                TRY(process_object.add("veil", ""sv));
            }

            TRY(process_object.add("pid", process.pid().value()));
            TRY(process_object.add("pgid", process.tty() ? process.tty()->pgid().value() : 0));
            TRY(process_object.add("pgp", process.pgid().value()));
            TRY(process_object.add("sid", process.sid().value()));
            TRY(process_object.add("uid", process.uid().value()));
            TRY(process_object.add("gid", process.gid().value()));
            TRY(process_object.add("ppid", process.ppid().value()));
            TRY(process_object.add("nfds", process.fds().with_shared([](auto& fds) { return fds.open_count(); })));
            TRY(process_object.add("name", process.name()));
            TRY(process_object.add("executable", process.executable() ? TRY(process.executable()->try_serialize_absolute_path())->view() : ""sv));
            TRY(process_object.add("tty", process.tty() ? process.tty()->tty_name().view() : "notty"sv));
            TRY(process_object.add("amount_virtual", process.address_space().amount_virtual()));
            TRY(process_object.add("amount_resident", process.address_space().amount_resident()));
            TRY(process_object.add("amount_dirty_private", process.address_space().amount_dirty_private()));
            TRY(process_object.add("amount_clean_inode", TRY(process.address_space().amount_clean_inode())));
            TRY(process_object.add("amount_shared", process.address_space().amount_shared()));
            TRY(process_object.add("amount_purgeable_volatile", process.address_space().amount_purgeable_volatile()));
            TRY(process_object.add("amount_purgeable_nonvolatile", process.address_space().amount_purgeable_nonvolatile()));
            TRY(process_object.add("dumpable", process.is_dumpable()));
            TRY(process_object.add("kernel", process.is_kernel_process()));
            auto thread_array = TRY(process_object.add_array("threads"));
            TRY(process.try_for_each_thread([&](const Thread& thread) -> ErrorOr<void> {
                SpinlockLocker locker(thread.get_lock());
                auto thread_object = TRY(thread_array.add_object());
#if LOCK_DEBUG
                TRY(thread_object.add("lock_count", thread.lock_count()));
#endif
                TRY(thread_object.add("tid", thread.tid().value()));
                TRY(thread_object.add("name", thread.name()));
                TRY(thread_object.add("times_scheduled", thread.times_scheduled()));
                TRY(thread_object.add("time_user", thread.time_in_user()));
                TRY(thread_object.add("time_kernel", thread.time_in_kernel()));
                TRY(thread_object.add("state", thread.state_string()));
                TRY(thread_object.add("cpu", thread.cpu()));
                TRY(thread_object.add("priority", thread.priority()));
                TRY(thread_object.add("syscall_count", thread.syscall_count()));
                TRY(thread_object.add("inode_faults", thread.inode_faults()));
                TRY(thread_object.add("zero_faults", thread.zero_faults()));
                TRY(thread_object.add("cow_faults", thread.cow_faults()));
                TRY(thread_object.add("file_read_bytes", thread.file_read_bytes()));
                TRY(thread_object.add("file_write_bytes", thread.file_write_bytes()));
                TRY(thread_object.add("unix_socket_read_bytes", thread.unix_socket_read_bytes()));
                TRY(thread_object.add("unix_socket_write_bytes", thread.unix_socket_write_bytes()));
                TRY(thread_object.add("ipv4_socket_read_bytes", thread.ipv4_socket_read_bytes()));
                TRY(thread_object.add("ipv4_socket_write_bytes", thread.ipv4_socket_write_bytes()));

                TRY(thread_object.finish());
                return {};
            }));
            TRY(thread_array.finish());
            TRY(process_object.finish());
            return {};
        };

        SpinlockLocker lock(g_scheduler_lock);
        {
            {
                auto array = TRY(json.add_array("processes"));
                TRY(build_process(array, *Scheduler::colonel()));
                TRY(Process::all_instances().with([&](auto& processes) -> ErrorOr<void> {
                    for (auto& process : processes)
                        TRY(build_process(array, process));
                    return {};
                }));
                TRY(array.finish());
            }

            auto total_time_scheduled = Scheduler::get_total_time_scheduled();
            TRY(json.add("total_time", total_time_scheduled.total));
            TRY(json.add("total_time_kernel", total_time_scheduled.total_kernel));
        }
        TRY(json.finish());
        return {};
    }
};
class ProcFSCPUInformation final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSCPUInformation> must_create();

private:
    ProcFSCPUInformation();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(Processor::try_for_each(
            [&](Processor& proc) -> ErrorOr<void> {
                auto& info = proc.info();
                auto obj = TRY(array.add_object());
                TRY(obj.add("processor", proc.id()));
                TRY(obj.add("cpuid", info.cpuid()));
                TRY(obj.add("family", info.display_family()));

                auto features_array = TRY(obj.add_array("features"));
                auto keep_empty = false;

                ErrorOr<void> result; // FIXME: Make this nicer
                info.features().for_each_split_view(' ', keep_empty, [&](StringView feature) {
                    if (result.is_error())
                        return;
                    result = features_array.add(feature);
                });
                TRY(result);

                TRY(features_array.finish());

                TRY(obj.add("model", info.display_model()));
                TRY(obj.add("stepping", info.stepping()));
                TRY(obj.add("type", info.type()));
                TRY(obj.add("brand", info.brand()));

                TRY(obj.finish());
                return {};
            }));
        TRY(array.finish());
        return {};
    }
};
class ProcFSDmesg final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSDmesg> must_create();

    virtual mode_t required_mode() const override { return 0400; }

private:
    ProcFSDmesg();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        VERIFY(DeviceManagement::the().is_console_device_attached());
        InterruptDisabler disabler;
        for (char ch : DeviceManagement::the().console_device().logbuffer()) {
            TRY(builder.append(ch));
        }
        return {};
    }
};
class ProcFSInterrupts final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSInterrupts> must_create();

private:
    ProcFSInterrupts();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        ErrorOr<void> result; // FIXME: Make this nicer
        InterruptManagement::the().enumerate_interrupt_handlers([&array, &result](GenericInterruptHandler& handler) {
            if (result.is_error())
                return;
            result = ([&]() -> ErrorOr<void> {
                auto obj = TRY(array.add_object());
                TRY(obj.add("purpose", handler.purpose()));
                TRY(obj.add("interrupt_line", handler.interrupt_number()));
                TRY(obj.add("controller", handler.controller()));
                TRY(obj.add("cpu_handler", 0)); // FIXME: Determine the responsible CPU for each interrupt handler.
                TRY(obj.add("device_sharing", (unsigned)handler.sharing_devices_count()));
                TRY(obj.add("call_count", (unsigned)handler.get_invoking_count()));
                TRY(obj.finish());
                return {};
            })();
        });
        TRY(result);
        TRY(array.finish());
        return {};
    }
};
class ProcFSKeymap final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSKeymap> must_create();

private:
    ProcFSKeymap();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto json = TRY(JsonObjectSerializer<>::try_create(builder));
        TRY(json.add("keymap", HIDManagement::the().keymap_name()));
        TRY(json.finish());
        return {};
    }
};

// FIXME: Remove this after we enumerate the SysFS from lspci and SystemMonitor
class ProcFSPCI final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSPCI> must_create();

private:
    ProcFSPCI();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        ErrorOr<void> result; // FIXME: Make this nicer
        PCI::enumerate([&array, &result](PCI::DeviceIdentifier const& device_identifier) {
            if (result.is_error())
                return;
            result = ([&]() -> ErrorOr<void> {
                auto obj = TRY(array.add_object());
                TRY(obj.add("domain", device_identifier.address().domain()));
                TRY(obj.add("bus", device_identifier.address().bus()));
                TRY(obj.add("device", device_identifier.address().device()));
                TRY(obj.add("function", device_identifier.address().function()));
                TRY(obj.add("vendor_id", device_identifier.hardware_id().vendor_id));
                TRY(obj.add("device_id", device_identifier.hardware_id().device_id));
                TRY(obj.add("revision_id", device_identifier.revision_id().value()));
                TRY(obj.add("subclass", device_identifier.subclass_code().value()));
                TRY(obj.add("class", device_identifier.class_code().value()));
                TRY(obj.add("subsystem_id", device_identifier.subsystem_id().value()));
                TRY(obj.add("subsystem_vendor_id", device_identifier.subsystem_vendor_id().value()));
                TRY(obj.finish());
                return {};
            })();
        });
        TRY(result);
        TRY(array.finish());
        return {};
    }
};

class ProcFSDevices final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSDevices> must_create();

private:
    ProcFSDevices();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(DeviceManagement::the().try_for_each([&array](auto& device) -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            TRY(obj.add("major", device.major().value()));
            TRY(obj.add("minor", device.minor().value()));
            TRY(obj.add("class_name", device.class_name()));

            if (device.is_block_device())
                TRY(obj.add("type", "block"));
            else if (device.is_character_device())
                TRY(obj.add("type", "character"));
            else
                VERIFY_NOT_REACHED();
            TRY(obj.finish());
            return {};
        }));
        TRY(array.finish());
        return {};
    }
};
class ProcFSUptime final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSUptime> must_create();

private:
    ProcFSUptime();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        return builder.appendff("{}\n", TimeManagement::the().uptime_ms() / 1000);
    }
};
class ProcFSCommandLine final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSCommandLine> must_create();

private:
    ProcFSCommandLine();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        TRY(builder.append(kernel_command_line().string()));
        TRY(builder.append('\n'));
        return {};
    }
};
class ProcFSSystemMode final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSSystemMode> must_create();

private:
    ProcFSSystemMode();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        TRY(builder.append(kernel_command_line().system_mode()));
        TRY(builder.append('\n'));
        return {};
    }
};

class ProcFSProfile final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSProfile> must_create();

    virtual mode_t required_mode() const override { return 0400; }

private:
    ProcFSProfile();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        if (!g_global_perf_events)
            return ENOENT;
        TRY(g_global_perf_events->to_json(builder));
        return {};
    }
};

class ProcFSKernelBase final : public ProcFSGlobalInformation {
public:
    static NonnullRefPtr<ProcFSKernelBase> must_create();

private:
    ProcFSKernelBase();

    virtual mode_t required_mode() const override { return 0400; }

    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        if (!Process::current().is_superuser())
            return EPERM;
        return builder.appendff("{}", kernel_load_base);
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
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSSystemStatistics> ProcFSSystemStatistics::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSSystemStatistics).release_nonnull();
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
UNMAP_AFTER_INIT NonnullRefPtr<ProcFSSystemMode> ProcFSSystemMode::must_create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSSystemMode).release_nonnull();
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
UNMAP_AFTER_INIT ProcFSSystemStatistics::ProcFSSystemStatistics()
    : ProcFSGlobalInformation("stat"sv)
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
UNMAP_AFTER_INIT ProcFSSystemMode::ProcFSSystemMode()
    : ProcFSGlobalInformation("system_mode"sv)
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
    directory->m_components.append(ProcFSSystemStatistics::must_create());
    directory->m_components.append(ProcFSOverallProcesses::must_create());
    directory->m_components.append(ProcFSCPUInformation::must_create());
    directory->m_components.append(ProcFSDmesg::must_create());
    directory->m_components.append(ProcFSInterrupts::must_create());
    directory->m_components.append(ProcFSKeymap::must_create());
    directory->m_components.append(ProcFSPCI::must_create());
    directory->m_components.append(ProcFSDevices::must_create());
    directory->m_components.append(ProcFSUptime::must_create());
    directory->m_components.append(ProcFSCommandLine::must_create());
    directory->m_components.append(ProcFSSystemMode::must_create());
    directory->m_components.append(ProcFSProfile::must_create());
    directory->m_components.append(ProcFSKernelBase::must_create());

    directory->m_components.append(ProcFSNetworkDirectory::must_create(*directory));
    directory->m_components.append(ProcFSSystemDirectory::must_create(*directory));
    return directory;
}

ErrorOr<void> ProcFSRootDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(ProcFSComponentRegistry::the().get_lock());
    TRY(callback({ ".", { fsid, component_index() }, 0 }));
    TRY(callback({ "..", { fsid, 0 }, 0 }));

    for (auto const& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        TRY(callback({ component.name(), identifier, 0 }));
    }

    return Process::all_instances().with([&](auto& list) -> ErrorOr<void> {
        for (auto& process : list) {
            VERIFY(!(process.pid() < 0));
            u64 process_id = (u64)process.pid().value();
            InodeIdentifier identifier = { fsid, static_cast<InodeIndex>(process_id << 36) };
            auto process_id_string = TRY(KString::formatted("{:d}", process_id));
            TRY(callback({ process_id_string->view(), identifier, 0 }));
        }
        return {};
    });
}

ErrorOr<NonnullRefPtr<ProcFSExposedComponent>> ProcFSRootDirectory::lookup(StringView name)
{
    auto maybe_candidate = ProcFSExposedDirectory::lookup(name);
    if (maybe_candidate.is_error()) {
        if (maybe_candidate.error().code() != ENOENT) {
            return maybe_candidate.release_error();
        }
    } else {
        return maybe_candidate.release_value();
    }

    auto pid = name.to_uint<unsigned>();
    if (!pid.has_value())
        return ESRCH;
    auto actual_pid = pid.value();

    if (auto maybe_process = Process::from_pid(actual_pid))
        return maybe_process->procfs_traits();

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
