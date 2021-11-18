/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Net/LocalSocket.h>

namespace Kernel {

static u32 s_lastFileSystemID;
static Singleton<HashMap<FileSystemID, FileSystem*>> s_file_system_map;

static HashMap<FileSystemID, FileSystem*>& all_file_systems()
{
    return *s_file_system_map;
}

FileSystem::FileSystem()
    : m_fsid(++s_lastFileSystemID)
{
    s_file_system_map->set(m_fsid, this);
}

FileSystem::~FileSystem()
{
    s_file_system_map->remove(m_fsid);
}

FileSystem* FileSystem::from_fsid(FileSystemID id)
{
    auto it = all_file_systems().find(id);
    if (it != all_file_systems().end())
        return (*it).value;
    return nullptr;
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

    NonnullRefPtrVector<FileSystem, 32> file_systems;
    {
        InterruptDisabler disabler;
        for (auto& it : all_file_systems())
            file_systems.append(*it.value);
    }

    for (auto& fs : file_systems)
        fs.flush_writes();
}

void FileSystem::lock_all()
{
    for (auto& it : all_file_systems()) {
        it.value->m_lock.lock();
    }
}

}
