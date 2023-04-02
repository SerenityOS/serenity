/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/Mount.h>

namespace Kernel {

Mount::Mount(NonnullRefPtr<FileSystem> guest_fs, Custody* host_custody, int flags)
    : m_guest(guest_fs->root_inode())
    , m_guest_fs(guest_fs)
    , m_host_custody(host_custody)
    , m_flags(flags)
{
}

Mount::Mount(Inode& source, Custody& host_custody, int flags)
    : m_guest(source)
    , m_guest_fs(source.fs())
    , m_host_custody(host_custody)
    , m_flags(flags)
{
}

ErrorOr<NonnullOwnPtr<KString>> Mount::absolute_path() const
{
    return m_host_custody.with([&](auto& host_custody) -> ErrorOr<NonnullOwnPtr<KString>> {
        if (!host_custody)
            return KString::try_create("/"sv);
        return host_custody->try_serialize_absolute_path();
    });
}

RefPtr<Inode> Mount::host()
{
    return m_host_custody.with([](auto& host_custody) -> RefPtr<Inode> {
        if (!host_custody)
            return nullptr;
        return &host_custody->inode();
    });
}

RefPtr<Inode const> Mount::host() const
{
    return m_host_custody.with([](auto& host_custody) -> RefPtr<Inode const> {
        if (!host_custody)
            return nullptr;
        return &host_custody->inode();
    });
}

}
