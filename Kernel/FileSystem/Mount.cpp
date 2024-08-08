/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/Mount.h>

namespace Kernel {

Mount::Mount(NonnullRefPtr<Inode> source, int flags)
    : m_details(Mount::Details(source->fs(), move(source)))
{
    set_flags(flags);
}

Mount::Mount(NonnullRefPtr<Inode> source, NonnullRefPtr<Custody> host_custody, int flags)
    : m_details(Mount::Details(source->fs(), move(source)))
    , m_host_custody(move(host_custody))
{
    set_flags(flags);
}

void Mount::set_flags(int flags)
{
    // NOTE: We use a spinlock to serialize access, to protect against
    // a case which the user requested to set the immutable flag, and
    // there's another ongoing call to set the flags without it.
    m_flags.with([this, flags](auto& current_flags) {
        if (flags & MS_IMMUTABLE)
            m_immutable.set();

        current_flags = flags;
        if (m_immutable.was_set())
            current_flags |= MS_IMMUTABLE;
    });
}

void Mount::delete_mount_from_list(Mount& mount)
{
    dbgln("VirtualFileSystem: Unmounting file system {}...", mount.guest_fs().fsid());
    VERIFY(mount.m_vfs_list_node.is_in_list());
    mount.m_vfs_list_node.remove();
    delete &mount;
}

ErrorOr<NonnullOwnPtr<KString>> Mount::absolute_path() const
{
    if (!m_host_custody)
        return KString::try_create("/"sv);
    return m_host_custody->try_serialize_absolute_path();
}

RefPtr<Inode> Mount::host()
{
    if (!m_host_custody)
        return nullptr;
    return m_host_custody->inode();
}

RefPtr<Inode const> Mount::host() const
{
    if (!m_host_custody)
        return nullptr;
    return m_host_custody->inode();
}

RefPtr<Custody const> Mount::host_custody() const
{
    return m_host_custody;
}

RefPtr<Custody> Mount::host_custody()
{
    return m_host_custody;
}

}
