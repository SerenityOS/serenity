/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/CoredumpFS/FileSystem.h>
#include <Kernel/FileSystem/CoredumpFS/Inode.h>
#include <Kernel/TTY/SlavePTY.h>

namespace Kernel {

static Singleton<MutexProtected<CoredumpFS::List>> s_all_instances;

ErrorOr<NonnullLockRefPtr<FileSystem>> CoredumpFS::try_create()
{
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) CoredumpFS));
}

ErrorOr<void> CoredumpFS::prepare_after_mount_first_time()
{
    CoredumpFS::all_instances().with_exclusive([&](auto& list) {
        list.append(*this);
    });
    return {};
}

ErrorOr<void> CoredumpFS::for_each(Function<ErrorOr<void>(CoredumpFS&)> callback)
{
    ErrorOr<void> result {};
    CoredumpFS::all_instances().with_exclusive([&](auto const& list) {
        for (auto& fs : list) {
            result = callback(fs);
            if (result.is_error())
                break;
        }
    });
    return result;
}

MutexProtected<CoredumpFS::List>& CoredumpFS::all_instances()
{
    return *s_all_instances;
}

ErrorOr<void> CoredumpFS::prepare_to_clear_last_mount()
{
    CoredumpFS::all_instances().with_exclusive([&](auto&) {
        m_coredumpfs_list_node.remove();
    });
    return {};
}

CoredumpFS::CoredumpFS() = default;
CoredumpFS::~CoredumpFS() = default;

InodeIndex CoredumpFS::coredump_pid_index_to_inode_index(ProcessID process_index)
{
    return process_index.value() + 2;
}

ErrorOr<void> CoredumpFS::initialize()
{
    m_root_inode = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) CoredumpFSInode(*this, 1)));
    return {};
}

Inode& CoredumpFS::root_inode()
{
    return *m_root_inode;
}

ErrorOr<void> CoredumpFS::notify_on_new_coredump(Badge<Process>, ProcessID pid)
{
    auto coredump_name = TRY(KString::formatted("{}", pid.value()));
    m_root_inode->did_add_child({ fsid(), CoredumpFS::coredump_pid_index_to_inode_index(pid.value()) }, coredump_name->view());
    return {};
}

ErrorOr<NonnullLockRefPtr<Inode>> CoredumpFS::get_inode(InodeIdentifier inode_id) const
{
    if (inode_id.index() == 1)
        return *m_root_inode;

    auto inode = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) CoredumpFSInode(const_cast<CoredumpFS&>(*this), inode_id.index())));
    return inode;
}
}
