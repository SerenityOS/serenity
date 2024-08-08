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
#include <Kernel/Forward.h>
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

    bool mount_point_exists_at_custody(Custody& mount_point);

    enum class DoBindMount {
        Yes,
        No,
    };
    ErrorOr<void> add_new_mount(DoBindMount, Inode& source, Custody& mount_point, int flags);

    ErrorOr<void> do_full_teardown(Badge<PowerStateSwitchTask>);

    ErrorOr<void> unmount(FileBackedFileSystem::List& file_backed_file_systems_list, Inode& guest_inode, StringView custody_path);
    ErrorOr<void> pivot_root(FileBackedFileSystem::List& file_backed_file_systems_list, FileSystem& fs, NonnullOwnPtr<Mount> new_mount, NonnullRefPtr<Custody> root_mount_point, int root_mount_flags);

    ErrorOr<void> apply_to_mount_for_host_custody(Custody const& current_custody, Function<void(Mount&)>);

    struct CurrentMountState {
        Mount::Details details;
        int flags { 0 };
    };
    ErrorOr<CurrentMountState> current_mount_state_for_host_custody(Custody const& current_custody) const;

    IndexID id() const { return m_id; }

    void attach(Badge<Process>);
    void detach(Badge<Process>);

    ErrorOr<void> for_each_mount(Function<ErrorOr<void>(Mount const&)>) const;

private:
    VFSRootContext(NonnullRefPtr<Custody> custody);

    enum class ValidateImmutableFlag {
        Yes,
        No,
    };
    ErrorOr<void> do_on_mount_for_host_custody(ValidateImmutableFlag validate_immutable_flag, Custody const& current_custody, Function<void(Mount&)> callback) const;

    static void add_to_mounts_list_and_increment_fs_mounted_count(DoBindMount do_bind_mount, IntrusiveList<&Mount::m_vfs_list_node>&, NonnullOwnPtr<Mount>);

    struct Details {
        SetOnce attached_by_process;
        size_t attach_count { 0 };
        IntrusiveList<&Mount::m_vfs_list_node> mounts;
    };

    static inline ErrorOr<void> validate_mount_not_immutable_while_being_used(Details& details, Mount& mount)
    {
        if (mount.is_immutable() && details.attach_count > 0)
            return Error::from_errno(EPERM);
        return {};
    }

    mutable SpinlockProtected<Details, LockRank::None> m_details {};

    SpinlockProtected<NonnullRefPtr<Custody>, LockRank::None> m_root_custody;

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
