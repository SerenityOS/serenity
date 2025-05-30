/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/NonnullLockRefPtr.h>

namespace Kernel {

class Mount {
    AK_MAKE_NONCOPYABLE(Mount);
    AK_MAKE_NONMOVABLE(Mount);
    friend class VFSRootContext;

public:
    struct Details {
        NonnullRefPtr<FileSystem> guest_fs;
        NonnullRefPtr<Inode> guest;
    };

    // NOTE: This constructor is valid for VFSRootContext root inodes (as for the "/" directory)
    Mount(NonnullRefPtr<Inode> source, int flags);

    Mount(NonnullRefPtr<Inode> source, NonnullRefPtr<Custody> host_custody, int flags);

    RefPtr<Inode const> host() const;
    RefPtr<Inode> host();

    RefPtr<Custody const> host_custody() const;
    RefPtr<Custody> host_custody();

    Inode const& guest() const { return *m_details.guest; }
    Inode& guest() { return *m_details.guest; }

    FileSystem const& guest_fs() const { return *m_details.guest_fs; }
    FileSystem& guest_fs() { return *m_details.guest_fs; }

    ErrorOr<NonnullOwnPtr<KString>> absolute_path() const;

    int flags() const
    {
        return m_flags.with([](auto const& current_flags) -> int { return current_flags; });
    }
    void set_flags(int flags);

    static void delete_mount_from_list(Mount&);

    bool is_immutable() const { return m_immutable.was_set(); }

    Details const& details() const { return m_details; }

private:
    Details const m_details;

    RefPtr<Custody> const m_host_custody;
    SpinlockProtected<int, LockRank::None> m_flags { 0 };

    SetOnce m_immutable;

    IntrusiveListNode<Mount> m_vfs_list_node;
};

}
