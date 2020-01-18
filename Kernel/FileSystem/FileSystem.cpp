/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

static u32 s_lastFileSystemID;
static HashMap<u32, FS*>* s_fs_map;

static HashMap<u32, FS*>& all_fses()
{
    if (!s_fs_map)
        s_fs_map = new HashMap<u32, FS*>();
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

FS::DirectoryEntry::DirectoryEntry(const char* n, InodeIdentifier i, u8 ft)
    : name_length(strlen(n))
    , inode(i)
    , file_type(ft)
{
    ASSERT(name_length < (int)sizeof(name));
    memcpy(name, n, name_length);
    name[name_length] = '\0';
}

FS::DirectoryEntry::DirectoryEntry(const char* n, size_t nl, InodeIdentifier i, u8 ft)
    : name_length(nl)
    , inode(i)
    , file_type(ft)
{
    ASSERT(name_length < (int)sizeof(name));
    memcpy(name, n, nl);
    name[nl] = '\0';
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

void FS::set_block_size(int block_size)
{
    ASSERT(block_size > 0);
    if (block_size == m_block_size)
        return;
    m_block_size = block_size;
}
