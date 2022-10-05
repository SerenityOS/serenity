/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <AK/Try.h>
#include <AK/UBSanitizer.h>
#include <Kernel/Arch/InterruptManagement.h>
#include <Kernel/Arch/x86/ProcessorInfo.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/InterruptDisabler.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
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
    static NonnullLockRefPtr<ProcFSAdapters> must_create();

private:
    ProcFSAdapters();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(NetworkingManagement::the().try_for_each([&array](auto& adapter) -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            TRY(obj.add("name"sv, adapter.name()));
            TRY(obj.add("class_name"sv, adapter.class_name()));
            auto mac_address = TRY(adapter.mac_address().to_string());
            TRY(obj.add("mac_address"sv, mac_address->view()));
            if (!adapter.ipv4_address().is_zero()) {
                auto ipv4_address = TRY(adapter.ipv4_address().to_string());
                TRY(obj.add("ipv4_address"sv, ipv4_address->view()));
                auto ipv4_netmask = TRY(adapter.ipv4_netmask().to_string());
                TRY(obj.add("ipv4_netmask"sv, ipv4_netmask->view()));
            }
            TRY(obj.add("packets_in"sv, adapter.packets_in()));
            TRY(obj.add("bytes_in"sv, adapter.bytes_in()));
            TRY(obj.add("packets_out"sv, adapter.packets_out()));
            TRY(obj.add("bytes_out"sv, adapter.bytes_out()));
            TRY(obj.add("link_up"sv, adapter.link_up()));
            TRY(obj.add("link_speed"sv, adapter.link_speed()));
            TRY(obj.add("link_full_duplex"sv, adapter.link_full_duplex()));
            TRY(obj.add("mtu"sv, adapter.mtu()));
            TRY(obj.finish());
            return {};
        }));
        TRY(array.finish());
        return {};
    }
};

class ProcFSARP final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSARP> must_create();

private:
    ProcFSARP();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(arp_table().with([&](auto const& table) -> ErrorOr<void> {
            for (auto& it : table) {
                auto obj = TRY(array.add_object());
                auto mac_address = TRY(it.value.to_string());
                TRY(obj.add("mac_address"sv, mac_address->view()));
                auto ip_address = TRY(it.key.to_string());
                TRY(obj.add("ip_address"sv, ip_address->view()));
                TRY(obj.finish());
            }
            return {};
        }));
        TRY(array.finish());
        return {};
    }
};

class ProcFSRoute final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSRoute> must_create();

private:
    ProcFSRoute();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(routing_table().with([&](auto const& table) -> ErrorOr<void> {
            for (auto& it : table) {
                auto obj = TRY(array.add_object());
                auto destination = TRY(it.destination.to_string());
                TRY(obj.add("destination"sv, destination->view()));
                auto gateway = TRY(it.gateway.to_string());
                TRY(obj.add("gateway"sv, gateway->view()));
                auto netmask = TRY(it.netmask.to_string());
                TRY(obj.add("genmask"sv, netmask->view()));
                TRY(obj.add("flags"sv, it.flags));
                TRY(obj.add("interface"sv, it.adapter->name()));
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
    static NonnullLockRefPtr<ProcFSTCP> must_create();

private:
    ProcFSTCP();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(TCPSocket::try_for_each([&array](auto& socket) -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            auto local_address = TRY(socket.local_address().to_string());
            TRY(obj.add("local_address"sv, local_address->view()));
            TRY(obj.add("local_port"sv, socket.local_port()));
            auto peer_address = TRY(socket.peer_address().to_string());
            TRY(obj.add("peer_address"sv, peer_address->view()));
            TRY(obj.add("peer_port"sv, socket.peer_port()));
            TRY(obj.add("state"sv, TCPSocket::to_string(socket.state())));
            TRY(obj.add("ack_number"sv, socket.ack_number()));
            TRY(obj.add("sequence_number"sv, socket.sequence_number()));
            TRY(obj.add("packets_in"sv, socket.packets_in()));
            TRY(obj.add("bytes_in"sv, socket.bytes_in()));
            TRY(obj.add("packets_out"sv, socket.packets_out()));
            TRY(obj.add("bytes_out"sv, socket.bytes_out()));
            auto current_process_credentials = Process::current().credentials();
            if (current_process_credentials->is_superuser() || current_process_credentials->uid() == socket.origin_uid()) {
                TRY(obj.add("origin_pid"sv, socket.origin_pid().value()));
                TRY(obj.add("origin_uid"sv, socket.origin_uid().value()));
                TRY(obj.add("origin_gid"sv, socket.origin_gid().value()));
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
    static NonnullLockRefPtr<ProcFSLocalNet> must_create();

private:
    ProcFSLocalNet();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(LocalSocket::try_for_each([&array](auto& socket) -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            TRY(obj.add("path"sv, socket.socket_path()));
            TRY(obj.add("origin_pid"sv, socket.origin_pid().value()));
            TRY(obj.add("origin_uid"sv, socket.origin_uid().value()));
            TRY(obj.add("origin_gid"sv, socket.origin_gid().value()));
            TRY(obj.add("acceptor_pid"sv, socket.acceptor_pid().value()));
            TRY(obj.add("acceptor_uid"sv, socket.acceptor_uid().value()));
            TRY(obj.add("acceptor_gid"sv, socket.acceptor_gid().value()));
            TRY(obj.finish());
            return {};
        }));
        TRY(array.finish());
        return {};
    }
};

class ProcFSUDP final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSUDP> must_create();

private:
    ProcFSUDP();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(UDPSocket::try_for_each([&array](auto& socket) -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            auto local_address = TRY(socket.local_address().to_string());
            TRY(obj.add("local_address"sv, local_address->view()));
            TRY(obj.add("local_port"sv, socket.local_port()));
            auto peer_address = TRY(socket.peer_address().to_string());
            TRY(obj.add("peer_address"sv, peer_address->view()));
            TRY(obj.add("peer_port"sv, socket.peer_port()));
            auto current_process_credentials = Process::current().credentials();
            if (current_process_credentials->is_superuser() || current_process_credentials->uid() == socket.origin_uid()) {
                TRY(obj.add("origin_pid"sv, socket.origin_pid().value()));
                TRY(obj.add("origin_uid"sv, socket.origin_uid().value()));
                TRY(obj.add("origin_gid"sv, socket.origin_gid().value()));
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
    static NonnullLockRefPtr<ProcFSNetworkDirectory> must_create(ProcFSRootDirectory const& parent_directory);

private:
    ProcFSNetworkDirectory(ProcFSRootDirectory const& parent_directory);
};

class ProcFSSystemDirectory : public ProcFSExposedDirectory {
public:
    static NonnullLockRefPtr<ProcFSSystemDirectory> must_create(ProcFSRootDirectory const& parent_directory);

private:
    ProcFSSystemDirectory(ProcFSRootDirectory const& parent_directory);
};

UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSAdapters> ProcFSAdapters::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSAdapters).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSARP> ProcFSARP::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSARP).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSRoute> ProcFSRoute::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSRoute).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSTCP> ProcFSTCP::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSTCP).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSLocalNet> ProcFSLocalNet::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSLocalNet).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSUDP> ProcFSUDP::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSUDP).release_nonnull();
}

UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSNetworkDirectory> ProcFSNetworkDirectory::must_create(ProcFSRootDirectory const& parent_directory)
{
    auto directory = adopt_lock_ref(*new (nothrow) ProcFSNetworkDirectory(parent_directory));
    directory->m_components.append(ProcFSAdapters::must_create());
    directory->m_components.append(ProcFSARP::must_create());
    directory->m_components.append(ProcFSRoute::must_create());
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
UNMAP_AFTER_INIT ProcFSRoute::ProcFSRoute()
    : ProcFSGlobalInformation("route"sv)
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
UNMAP_AFTER_INIT ProcFSNetworkDirectory::ProcFSNetworkDirectory(ProcFSRootDirectory const& parent_directory)
    : ProcFSExposedDirectory("net"sv, parent_directory)
{
}

class ProcFSDumpKmallocStacks : public ProcFSSystemBoolean {
public:
    static NonnullLockRefPtr<ProcFSDumpKmallocStacks> must_create(ProcFSSystemDirectory const&);
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
    static NonnullLockRefPtr<ProcFSUBSanDeadly> must_create(ProcFSSystemDirectory const&);

    virtual bool value() const override { return AK::UBSanitizer::g_ubsan_is_deadly; }
    virtual void set_value(bool new_value) override { AK::UBSanitizer::g_ubsan_is_deadly = new_value; }

private:
    ProcFSUBSanDeadly();
};

class ProcFSCapsLockRemap : public ProcFSSystemBoolean {
public:
    static NonnullLockRefPtr<ProcFSCapsLockRemap> must_create(ProcFSSystemDirectory const&);
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

UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSDumpKmallocStacks> ProcFSDumpKmallocStacks::must_create(ProcFSSystemDirectory const&)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSDumpKmallocStacks).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSUBSanDeadly> ProcFSUBSanDeadly::must_create(ProcFSSystemDirectory const&)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSUBSanDeadly).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSCapsLockRemap> ProcFSCapsLockRemap::must_create(ProcFSSystemDirectory const&)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSCapsLockRemap).release_nonnull();
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
    static NonnullLockRefPtr<ProcFSSelfProcessDirectory> must_create();

private:
    ProcFSSelfProcessDirectory();
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        return !builder.appendff("{}", Process::current().pid().value()).is_error();
    }
};

class ProcFSDiskUsage final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSDiskUsage> must_create();

private:
    ProcFSDiskUsage();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(VirtualFileSystem::the().for_each_mount([&array](auto& mount) -> ErrorOr<void> {
            auto& fs = mount.guest_fs();
            auto fs_object = TRY(array.add_object());
            TRY(fs_object.add("class_name"sv, fs.class_name()));
            TRY(fs_object.add("total_block_count"sv, fs.total_block_count()));
            TRY(fs_object.add("free_block_count"sv, fs.free_block_count()));
            TRY(fs_object.add("total_inode_count"sv, fs.total_inode_count()));
            TRY(fs_object.add("free_inode_count"sv, fs.free_inode_count()));
            auto mount_point = TRY(mount.absolute_path());
            TRY(fs_object.add("mount_point"sv, mount_point->view()));
            TRY(fs_object.add("block_size"sv, static_cast<u64>(fs.block_size())));
            TRY(fs_object.add("readonly"sv, fs.is_readonly()));
            TRY(fs_object.add("mount_flags"sv, mount.flags()));

            if (fs.is_file_backed()) {
                auto pseudo_path = TRY(static_cast<const FileBackedFileSystem&>(fs).file_description().pseudo_path());
                TRY(fs_object.add("source"sv, pseudo_path->view()));
            } else {
                TRY(fs_object.add("source"sv, "none"));
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
    static NonnullLockRefPtr<ProcFSMemoryStatus> must_create();

private:
    ProcFSMemoryStatus();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        InterruptDisabler disabler;

        kmalloc_stats stats;
        get_kmalloc_stats(stats);

        auto system_memory = MM.get_system_memory_info();

        auto json = TRY(JsonObjectSerializer<>::try_create(builder));
        TRY(json.add("kmalloc_allocated"sv, stats.bytes_allocated));
        TRY(json.add("kmalloc_available"sv, stats.bytes_free));
        TRY(json.add("physical_allocated"sv, system_memory.physical_pages_used));
        TRY(json.add("physical_available"sv, system_memory.physical_pages - system_memory.physical_pages_used));
        TRY(json.add("physical_committed"sv, system_memory.physical_pages_committed));
        TRY(json.add("physical_uncommitted"sv, system_memory.physical_pages_uncommitted));
        TRY(json.add("kmalloc_call_count"sv, stats.kmalloc_call_count));
        TRY(json.add("kfree_call_count"sv, stats.kfree_call_count));
        TRY(json.finish());
        return {};
    }
};

class ProcFSSystemStatistics final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSSystemStatistics> must_create();

private:
    ProcFSSystemStatistics();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto json = TRY(JsonObjectSerializer<>::try_create(builder));
        auto total_time_scheduled = Scheduler::get_total_time_scheduled();
        TRY(json.add("total_time"sv, total_time_scheduled.total));
        TRY(json.add("kernel_time"sv, total_time_scheduled.total_kernel));
        TRY(json.add("user_time"sv, total_time_scheduled.total - total_time_scheduled.total_kernel));
        u64 idle_time = 0;
        Processor::for_each([&](Processor& processor) {
            idle_time += processor.time_spent_idle();
        });
        TRY(json.add("idle_time"sv, idle_time));
        TRY(json.finish());
        return {};
    }
};

class ProcFSOverallProcesses final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSOverallProcesses> must_create();

private:
    ProcFSOverallProcesses();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto json = TRY(JsonObjectSerializer<>::try_create(builder));

        // Keep this in sync with CProcessStatistics.
        auto build_process = [&](JsonArraySerializer<KBufferBuilder>& array, Process const& process) -> ErrorOr<void> {
            auto process_object = TRY(array.add_object());

            if (process.is_user_process()) {
                StringBuilder pledge_builder;

#define __ENUMERATE_PLEDGE_PROMISE(promise)    \
    if (process.has_promised(Pledge::promise)) \
        TRY(pledge_builder.try_append(#promise " "sv));
                ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE

                TRY(process_object.add("pledge"sv, pledge_builder.string_view()));

                switch (process.veil_state()) {
                case VeilState::None:
                    TRY(process_object.add("veil"sv, "None"));
                    break;
                case VeilState::Dropped:
                    TRY(process_object.add("veil"sv, "Dropped"));
                    break;
                case VeilState::Locked:
                    TRY(process_object.add("veil"sv, "Locked"));
                    break;
                }
            } else {
                TRY(process_object.add("pledge"sv, ""sv));
                TRY(process_object.add("veil"sv, ""sv));
            }

            TRY(process_object.add("pid"sv, process.pid().value()));
            TRY(process_object.add("pgid"sv, process.tty() ? process.tty()->pgid().value() : 0));
            TRY(process_object.add("pgp"sv, process.pgid().value()));
            TRY(process_object.add("sid"sv, process.sid().value()));
            auto credentials = process.credentials();
            TRY(process_object.add("uid"sv, credentials->uid().value()));
            TRY(process_object.add("gid"sv, credentials->gid().value()));
            TRY(process_object.add("ppid"sv, process.ppid().value()));
            if (process.tty()) {
                auto tty_pseudo_name = TRY(process.tty()->pseudo_name());
                TRY(process_object.add("tty"sv, tty_pseudo_name->view()));
            } else {
                TRY(process_object.add("tty"sv, ""));
            }
            TRY(process_object.add("nfds"sv, process.fds().with_shared([](auto& fds) { return fds.open_count(); })));
            TRY(process_object.add("name"sv, process.name()));
            TRY(process_object.add("executable"sv, process.executable() ? TRY(process.executable()->try_serialize_absolute_path())->view() : ""sv));

            size_t amount_virtual = 0;
            size_t amount_resident = 0;
            size_t amount_dirty_private = 0;
            size_t amount_clean_inode = 0;
            size_t amount_shared = 0;
            size_t amount_purgeable_volatile = 0;
            size_t amount_purgeable_nonvolatile = 0;

            TRY(process.address_space().with([&](auto& space) -> ErrorOr<void> {
                amount_virtual = space->amount_virtual();
                amount_resident = space->amount_resident();
                amount_dirty_private = space->amount_dirty_private();
                amount_clean_inode = TRY(space->amount_clean_inode());
                amount_shared = space->amount_shared();
                amount_purgeable_volatile = space->amount_purgeable_volatile();
                amount_purgeable_nonvolatile = space->amount_purgeable_nonvolatile();
                return {};
            }));

            TRY(process_object.add("amount_virtual"sv, amount_virtual));
            TRY(process_object.add("amount_resident"sv, amount_resident));
            TRY(process_object.add("amount_dirty_private"sv, amount_dirty_private));
            TRY(process_object.add("amount_clean_inode"sv, amount_clean_inode));
            TRY(process_object.add("amount_shared"sv, amount_shared));
            TRY(process_object.add("amount_purgeable_volatile"sv, amount_purgeable_volatile));
            TRY(process_object.add("amount_purgeable_nonvolatile"sv, amount_purgeable_nonvolatile));
            TRY(process_object.add("dumpable"sv, process.is_dumpable()));
            TRY(process_object.add("kernel"sv, process.is_kernel_process()));
            auto thread_array = TRY(process_object.add_array("threads"sv));
            TRY(process.try_for_each_thread([&](const Thread& thread) -> ErrorOr<void> {
                SpinlockLocker locker(thread.get_lock());
                auto thread_object = TRY(thread_array.add_object());
#if LOCK_DEBUG
                TRY(thread_object.add("lock_count"sv, thread.lock_count()));
#endif
                TRY(thread_object.add("tid"sv, thread.tid().value()));
                TRY(thread_object.add("name"sv, thread.name()));
                TRY(thread_object.add("times_scheduled"sv, thread.times_scheduled()));
                TRY(thread_object.add("time_user"sv, thread.time_in_user()));
                TRY(thread_object.add("time_kernel"sv, thread.time_in_kernel()));
                TRY(thread_object.add("state"sv, thread.state_string()));
                TRY(thread_object.add("cpu"sv, thread.cpu()));
                TRY(thread_object.add("priority"sv, thread.priority()));
                TRY(thread_object.add("syscall_count"sv, thread.syscall_count()));
                TRY(thread_object.add("inode_faults"sv, thread.inode_faults()));
                TRY(thread_object.add("zero_faults"sv, thread.zero_faults()));
                TRY(thread_object.add("cow_faults"sv, thread.cow_faults()));
                TRY(thread_object.add("file_read_bytes"sv, thread.file_read_bytes()));
                TRY(thread_object.add("file_write_bytes"sv, thread.file_write_bytes()));
                TRY(thread_object.add("unix_socket_read_bytes"sv, thread.unix_socket_read_bytes()));
                TRY(thread_object.add("unix_socket_write_bytes"sv, thread.unix_socket_write_bytes()));
                TRY(thread_object.add("ipv4_socket_read_bytes"sv, thread.ipv4_socket_read_bytes()));
                TRY(thread_object.add("ipv4_socket_write_bytes"sv, thread.ipv4_socket_write_bytes()));

                TRY(thread_object.finish());
                return {};
            }));
            TRY(thread_array.finish());
            TRY(process_object.finish());
            return {};
        };

        {
            auto array = TRY(json.add_array("processes"sv));
            TRY(build_process(array, *Scheduler::colonel()));
            TRY(Process::all_instances().with([&](auto& processes) -> ErrorOr<void> {
                for (auto& process : processes)
                    TRY(build_process(array, process));
                return {};
            }));
            TRY(array.finish());
        }

        auto total_time_scheduled = Scheduler::get_total_time_scheduled();
        TRY(json.add("total_time"sv, total_time_scheduled.total));
        TRY(json.add("total_time_kernel"sv, total_time_scheduled.total_kernel));
        TRY(json.finish());
        return {};
    }
};
class ProcFSCPUInformation final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSCPUInformation> must_create();

private:
    ProcFSCPUInformation();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto array = TRY(JsonArraySerializer<>::try_create(builder));
        TRY(Processor::try_for_each(
            [&](Processor& proc) -> ErrorOr<void> {
                auto& info = proc.info();
                auto obj = TRY(array.add_object());
                TRY(obj.add("processor"sv, proc.id()));
                TRY(obj.add("vendor_id"sv, info.vendor_id_string()));
                TRY(obj.add("family"sv, info.display_family()));
                if (!info.hypervisor_vendor_id_string().is_null())
                    TRY(obj.add("hypervisor_vendor_id"sv, info.hypervisor_vendor_id_string()));

                auto features_array = TRY(obj.add_array("features"sv));
                auto keep_empty = false;

                ErrorOr<void> result; // FIXME: Make this nicer
                info.features_string().for_each_split_view(' ', keep_empty, [&](StringView feature) {
                    if (result.is_error())
                        return;
                    result = features_array.add(feature);
                });
                TRY(result);

                TRY(features_array.finish());

                TRY(obj.add("model"sv, info.display_model()));
                TRY(obj.add("stepping"sv, info.stepping()));
                TRY(obj.add("type"sv, info.type()));
                TRY(obj.add("brand"sv, info.brand_string()));

                auto caches = TRY(obj.add_object("caches"sv));

                auto add_cache_info = [&](StringView name, ProcessorInfo::Cache const& cache) -> ErrorOr<void> {
                    auto cache_object = TRY(caches.add_object(name));
                    TRY(cache_object.add("size"sv, cache.size));
                    TRY(cache_object.add("line_size"sv, cache.line_size));
                    TRY(cache_object.finish());
                    return {};
                };

                if (info.l1_data_cache().has_value())
                    TRY(add_cache_info("l1_data"sv, *info.l1_data_cache()));
                if (info.l1_instruction_cache().has_value())
                    TRY(add_cache_info("l1_instruction"sv, *info.l1_instruction_cache()));
                if (info.l2_cache().has_value())
                    TRY(add_cache_info("l2"sv, *info.l2_cache()));
                if (info.l3_cache().has_value())
                    TRY(add_cache_info("l3"sv, *info.l3_cache()));

                TRY(caches.finish());

                TRY(obj.finish());
                return {};
            }));
        TRY(array.finish());
        return {};
    }
};
class ProcFSDmesg final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSDmesg> must_create();

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
    static NonnullLockRefPtr<ProcFSInterrupts> must_create();

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
                TRY(obj.add("purpose"sv, handler.purpose()));
                TRY(obj.add("interrupt_line"sv, handler.interrupt_number()));
                TRY(obj.add("controller"sv, handler.controller()));
                TRY(obj.add("cpu_handler"sv, 0)); // FIXME: Determine the responsible CPU for each interrupt handler.
                TRY(obj.add("device_sharing"sv, (unsigned)handler.sharing_devices_count()));
                TRY(obj.add("call_count"sv, (unsigned)handler.get_invoking_count()));
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
    static NonnullLockRefPtr<ProcFSKeymap> must_create();

private:
    ProcFSKeymap();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto json = TRY(JsonObjectSerializer<>::try_create(builder));
        TRY(HIDManagement::the().keymap_data().with([&](auto const& keymap_data) {
            return json.add("keymap"sv, keymap_data.character_map_name->view());
        }));
        TRY(json.finish());
        return {};
    }
};

class ProcFSUptime final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSUptime> must_create();

private:
    ProcFSUptime();
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        return builder.appendff("{}\n", TimeManagement::the().uptime_ms() / 1000);
    }
};
class ProcFSCommandLine final : public ProcFSGlobalInformation {
public:
    static NonnullLockRefPtr<ProcFSCommandLine> must_create();

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
    static NonnullLockRefPtr<ProcFSSystemMode> must_create();

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
    static NonnullLockRefPtr<ProcFSProfile> must_create();

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
    static NonnullLockRefPtr<ProcFSKernelBase> must_create();

private:
    ProcFSKernelBase();

    virtual mode_t required_mode() const override { return 0400; }

    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override
    {
        auto current_process_credentials = Process::current().credentials();
        if (!current_process_credentials->is_superuser())
            return EPERM;
        return builder.appendff("{}", kernel_load_base);
    }
};

UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSSelfProcessDirectory> ProcFSSelfProcessDirectory::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSSelfProcessDirectory()).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSDiskUsage> ProcFSDiskUsage::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSDiskUsage).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSMemoryStatus> ProcFSMemoryStatus::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSMemoryStatus).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSSystemStatistics> ProcFSSystemStatistics::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSSystemStatistics).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSOverallProcesses> ProcFSOverallProcesses::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSOverallProcesses).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSCPUInformation> ProcFSCPUInformation::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSCPUInformation).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSDmesg> ProcFSDmesg::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSDmesg).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSInterrupts> ProcFSInterrupts::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSInterrupts).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSKeymap> ProcFSKeymap::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSKeymap).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSUptime> ProcFSUptime::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSUptime).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSCommandLine> ProcFSCommandLine::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSCommandLine).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSSystemMode> ProcFSSystemMode::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSSystemMode).release_nonnull();
}
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSProfile> ProcFSProfile::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSProfile).release_nonnull();
}

UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSKernelBase> ProcFSKernelBase::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSKernelBase).release_nonnull();
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

UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSSystemDirectory> ProcFSSystemDirectory::must_create(ProcFSRootDirectory const& parent_directory)
{
    auto directory = adopt_lock_ref(*new (nothrow) ProcFSSystemDirectory(parent_directory));
    directory->m_components.append(ProcFSDumpKmallocStacks::must_create(directory));
    directory->m_components.append(ProcFSUBSanDeadly::must_create(directory));
    directory->m_components.append(ProcFSCapsLockRemap::must_create(directory));
    return directory;
}

UNMAP_AFTER_INIT ProcFSSystemDirectory::ProcFSSystemDirectory(ProcFSRootDirectory const& parent_directory)
    : ProcFSExposedDirectory("sys"sv, parent_directory)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSRootDirectory> ProcFSRootDirectory::must_create()
{
    auto directory = adopt_lock_ref(*new (nothrow) ProcFSRootDirectory);
    directory->m_components.append(ProcFSSelfProcessDirectory::must_create());
    directory->m_components.append(ProcFSDiskUsage::must_create());
    directory->m_components.append(ProcFSMemoryStatus::must_create());
    directory->m_components.append(ProcFSSystemStatistics::must_create());
    directory->m_components.append(ProcFSOverallProcesses::must_create());
    directory->m_components.append(ProcFSCPUInformation::must_create());
    directory->m_components.append(ProcFSDmesg::must_create());
    directory->m_components.append(ProcFSInterrupts::must_create());
    directory->m_components.append(ProcFSKeymap::must_create());
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
    TRY(callback({ "."sv, { fsid, component_index() }, 0 }));
    TRY(callback({ ".."sv, { fsid, 0 }, 0 }));

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

ErrorOr<NonnullLockRefPtr<ProcFSExposedComponent>> ProcFSRootDirectory::lookup(StringView name)
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

UNMAP_AFTER_INIT ProcFSRootDirectory::~ProcFSRootDirectory() = default;

}
