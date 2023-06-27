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

class VirtualFileSystem;
class Mount {
    AK_MAKE_NONCOPYABLE(Mount);
    AK_MAKE_NONMOVABLE(Mount);
    friend class VirtualFileSystem;

public:
    Mount(NonnullRefPtr<FileSystem>, RefPtr<Custody> host_custody, int flags);
    Mount(NonnullRefPtr<Inode> source, NonnullRefPtr<Custody> host_custody, int flags);

    RefPtr<Inode const> host() const;
    RefPtr<Inode> host();

    RefPtr<Custody const> host_custody() const;
    RefPtr<Custody> host_custody();

    Inode const& guest() const { return *m_guest; }
    Inode& guest() { return *m_guest; }

    FileSystem const& guest_fs() const { return *m_guest_fs; }
    FileSystem& guest_fs() { return *m_guest_fs; }

    ErrorOr<NonnullOwnPtr<KString>> absolute_path() const;

    int flags() const { return m_flags; }
    void set_flags(int flags) { m_flags = flags; }

private:
    NonnullRefPtr<FileSystem> const m_guest_fs;
    NonnullRefPtr<Inode> const m_guest;
    RefPtr<Custody> const m_host_custody;
    int m_flags { 0 };

    IntrusiveListNode<Mount> m_vfs_list_node;
};

}
