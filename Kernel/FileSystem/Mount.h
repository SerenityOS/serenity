/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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
    friend class VirtualFileSystem;

public:
    Mount(FileSystem&, Custody* host_custody, int flags);
    Mount(Inode& source, Custody& host_custody, int flags);

    LockRefPtr<Inode const> host() const;
    LockRefPtr<Inode> host();

    Inode const& guest() const { return *m_guest; }
    Inode& guest() { return *m_guest; }

    FileSystem const& guest_fs() const { return *m_guest_fs; }
    FileSystem& guest_fs() { return *m_guest_fs; }

    ErrorOr<NonnullOwnPtr<KString>> absolute_path() const;

    int flags() const { return m_flags; }
    void set_flags(int flags) { m_flags = flags; }

private:
    NonnullLockRefPtr<Inode> m_guest;
    NonnullLockRefPtr<FileSystem> m_guest_fs;
    SpinlockProtected<RefPtr<Custody>> m_host_custody;
    int m_flags;

    IntrusiveListNode<Mount> m_vfs_list_node;
};

}
