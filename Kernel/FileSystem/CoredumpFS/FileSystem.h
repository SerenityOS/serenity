/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveListRelaxedConst.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class CoredumpFile;
class CoredumpFSInode;
class Process;

class CoredumpFS final : public FileSystem {
    friend class CoredumpFSInode;

public:
    virtual ~CoredumpFS() override;
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "CoredumpFS"sv; }
    virtual bool supports_watchers_only_on_root_inode() const override { return true; }
    virtual bool supports_watchers() const override { return true; }

    ErrorOr<void> notify_on_new_coredump(Badge<Process>, ProcessID);

    static InodeIndex coredump_pid_index_to_inode_index(ProcessID process_index);

    virtual Inode& root_inode() override;

private:
    virtual ErrorOr<void> prepare_after_mount_first_time() override;
    virtual ErrorOr<void> prepare_to_clear_last_mount() override;

    IntrusiveListNode<CoredumpFS, RefPtr<CoredumpFS>> m_coredumpfs_list_node;

public:
    using List = IntrusiveListRelaxedConst<&CoredumpFS::m_coredumpfs_list_node>;
    // FIXME: This should be placed later on in the Jail class when we have
    // different mount table for each Jail.
    static ErrorOr<void> for_each(Function<ErrorOr<void>(CoredumpFS&)>);
    static MutexProtected<CoredumpFS::List>& all_instances();

private:
    CoredumpFS();
    ErrorOr<NonnullLockRefPtr<Inode>> get_inode(InodeIdentifier) const;

    LockRefPtr<CoredumpFSInode> m_root_inode;
};

}
