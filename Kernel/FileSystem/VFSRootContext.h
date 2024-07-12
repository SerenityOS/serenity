/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Noncopyable.h>
#include <AK/RefPtr.h>
#include <AK/SetOnce.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Mount.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

class VFSRootContext : public AtomicRefCounted<VFSRootContext> {
    AK_MAKE_NONCOPYABLE(VFSRootContext);
    AK_MAKE_NONMOVABLE(VFSRootContext);

public:
    AK_TYPEDEF_DISTINCT_ORDERED_ID(u64, IndexID);

    static VFSRootContext const& empty_context_for_kernel_processes();
    static Custody const& empty_context_custody_for_kernel_processes();
    static void initialize_empty_ramfs_root_context_for_kernel_processes();

    static ErrorOr<NonnullRefPtr<VFSRootContext>> create_with_empty_ramfs();
    static ErrorOr<NonnullRefPtr<VFSRootContext>> create_empty();

    SpinlockProtected<NonnullRefPtr<Custody>, LockRank::None>& root_custody() { return m_root_custody; }
    SpinlockProtected<NonnullRefPtr<Custody>, LockRank::None> const& root_custody() const { return m_root_custody; }

    SpinlockProtected<IntrusiveList<&Mount::m_vfs_list_node>, LockRank::None>& mounts() { return m_mounts; }
    SpinlockProtected<IntrusiveList<&Mount::m_vfs_list_node>, LockRank::None> const& mounts() const { return m_mounts; }

    struct Attributes {
        SetOnce attached_by_process;
        SetOnce immutable;
    };
    SpinlockProtected<Attributes, LockRank::None>& attributes() { return m_attributes; }

    bool mount_point_exists_at_custody(Custody& mount_point);

    enum class DoBindMount {
        Yes,
        No,
    };
    ErrorOr<void> add_new_mount(DoBindMount, Inode& source, Custody& mount_point, int flags);

    IndexID id() const { return m_id; }

    void set_attached(Badge<Process>);

    ErrorOr<void> for_each_mount(Function<ErrorOr<void>(Mount const&)>) const;

private:
    VFSRootContext(NonnullRefPtr<Custody> custody);

    static void add_to_mounts_list_and_increment_fs_mounted_count(DoBindMount do_bind_mount, IntrusiveList<&Mount::m_vfs_list_node>&, NonnullOwnPtr<Mount>);

    SpinlockProtected<Attributes, LockRank::None> m_attributes {};
    SpinlockProtected<NonnullRefPtr<Custody>, LockRank::None> m_root_custody;
    SpinlockProtected<IntrusiveList<&Mount::m_vfs_list_node>, LockRank::None> m_mounts {};

    IntrusiveListNode<VFSRootContext, NonnullRefPtr<VFSRootContext>> m_list_node;

    IndexID m_id;

    // NOTE: This method is implemented in Kernel/FileSystem/VirtualFileSystem.cpp
    static SpinlockProtected<IntrusiveList<&VFSRootContext::m_list_node>, LockRank::FileSystem>& all_root_contexts_list();

public:
    using List = IntrusiveList<&VFSRootContext::m_list_node>;

    // NOTE: These methods are implemented in Kernel/FileSystem/VirtualFileSystem.cpp
    static SpinlockProtected<VFSRootContext::List, LockRank::FileSystem>& all_root_contexts_list(Badge<PowerStateSwitchTask>);
    static SpinlockProtected<VFSRootContext::List, LockRank::FileSystem>& all_root_contexts_list(Badge<Process>);
};

}
