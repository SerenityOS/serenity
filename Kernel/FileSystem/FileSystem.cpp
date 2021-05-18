/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

static u32 s_lastFileSystemID;
static AK::Singleton<HashMap<u32, FS*>> s_fs_map;

static HashMap<u32, FS*>& all_fses()
{
    return *s_fs_map;
}

FS::FS()
    : m_fsid(++s_lastFileSystemID)
{
    all_fses().set(m_fsid, this);
}

FS::~FS()
{
    all_fses().remove(m_fsid);
}

FS* FS::from_fsid(u32 id)
{
    auto it = all_fses().find(id);
    if (it != all_fses().end())
        return (*it).value;
    return nullptr;
}

FS::DirectoryEntryView::DirectoryEntryView(const StringView& n, InodeIdentifier i, u8 ft)
    : name(n)
    , inode(i)
    , file_type(ft)
{
}

void FS::sync()
{
    Inode::sync();

    NonnullRefPtrVector<FS, 32> fses;
    {
        InterruptDisabler disabler;
        for (auto& it : all_fses())
            fses.append(*it.value);
    }

    for (auto& fs : fses)
        fs.flush_writes();
}

void FS::lock_all()
{
    for (auto& it : all_fses()) {
        it.value->m_lock.lock();
    }
}

void FS::set_block_size(size_t block_size)
{
    VERIFY(block_size > 0);
    if (block_size == m_block_size)
        return;
    m_block_size = block_size;
}

void FS::set_fragment_size(size_t fragment_size)
{
    VERIFY(fragment_size > 0);
    if (fragment_size == m_fragment_size)
        return;
    m_fragment_size = fragment_size;
}

}
