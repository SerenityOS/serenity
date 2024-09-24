/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Net/LocalSocket.h>

namespace Kernel {

static u32 s_lastFileSystemID;

FileSystem::FileSystem()
    : m_fsid(++s_lastFileSystemID)
{
}

FileSystem::~FileSystem()
{
}

ErrorOr<void> FileSystem::prepare_to_unmount(Inode& mount_guest_inode)
{
    return m_attach_count.with([&](auto& attach_count) -> ErrorOr<void> {
        dbgln_if(VFS_DEBUG, "VFS: File system {} (id {}) is attached {} time(s)", class_name(), m_fsid.value(), attach_count);
        if (attach_count == 1)
            return prepare_to_clear_last_mount(mount_guest_inode);
        return {};
    });
}

FileSystem::DirectoryEntryView::DirectoryEntryView(StringView n, InodeIdentifier i, u8 ft)
    : name(n)
    , inode(i)
    , file_type(ft)
{
}

void FileSystem::sync()
{
    Inode::sync_all();
    VirtualFileSystem::sync_filesystems();
}

}
