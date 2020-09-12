/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/TTY/SlavePTY.h>

namespace Kernel {

NonnullRefPtr<DevPtsFS> DevPtsFS::create()
{
    return adopt(*new DevPtsFS);
}

DevPtsFS::DevPtsFS()
{
}

DevPtsFS::~DevPtsFS()
{
}

static AK::Singleton<HashTable<unsigned>> s_ptys;

bool DevPtsFS::initialize()
{
    m_root_inode = adopt(*new DevPtsFSInode(*this, 1, nullptr));
    m_root_inode->m_metadata.inode = { fsid(), 1 };
    m_root_inode->m_metadata.mode = 0040555;
    m_root_inode->m_metadata.uid = 0;
    m_root_inode->m_metadata.gid = 0;
    m_root_inode->m_metadata.size = 0;
    m_root_inode->m_metadata.mtime = mepoch;

    return true;
}

static unsigned inode_index_to_pty_index(unsigned inode_index)
{
    ASSERT(inode_index > 1);
    return inode_index - 2;
}

static unsigned pty_index_to_inode_index(unsigned pty_index)
{
    return pty_index + 2;
}

NonnullRefPtr<Inode> DevPtsFS::root_inode() const
{
    return *m_root_inode;
}

RefPtr<Inode> DevPtsFS::get_inode(InodeIdentifier inode_id) const
{
    if (inode_id.index() == 1)
        return m_root_inode;

    unsigned pty_index = inode_index_to_pty_index(inode_id.index());
    auto* device = Device::get_device(201, pty_index);
    ASSERT(device);

    auto inode = adopt(*new DevPtsFSInode(const_cast<DevPtsFS&>(*this), inode_id.index(), static_cast<SlavePTY*>(device)));
    inode->m_metadata.inode = inode_id;
    inode->m_metadata.size = 0;
    inode->m_metadata.uid = device->uid();
    inode->m_metadata.gid = device->gid();
    inode->m_metadata.mode = 0020600;
    inode->m_metadata.major_device = device->major();
    inode->m_metadata.minor_device = device->minor();
    inode->m_metadata.mtime = mepoch;

    return inode;
}

void DevPtsFS::register_slave_pty(SlavePTY& slave_pty)
{
    s_ptys->set(slave_pty.index());
}

void DevPtsFS::unregister_slave_pty(SlavePTY& slave_pty)
{
    s_ptys->remove(slave_pty.index());
}

DevPtsFSInode::DevPtsFSInode(DevPtsFS& fs, unsigned index, SlavePTY* pty)
    : Inode(fs, index)
{
    if (pty)
        m_pty = pty->make_weak_ptr();
}

DevPtsFSInode::~DevPtsFSInode()
{
}

ssize_t DevPtsFSInode::read_bytes(off_t, ssize_t, UserOrKernelBuffer&, FileDescription*) const
{
    ASSERT_NOT_REACHED();
}

ssize_t DevPtsFSInode::write_bytes(off_t, ssize_t, const UserOrKernelBuffer&, FileDescription*)
{
    ASSERT_NOT_REACHED();
}

InodeMetadata DevPtsFSInode::metadata() const
{
    if (m_pty) {
        auto metadata = m_metadata;
        metadata.mtime = m_pty->time_of_last_write();
        return metadata;
    }
    return m_metadata;
}

KResult DevPtsFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    if (identifier().index() > 1)
        return KResult(-ENOTDIR);

    callback({ ".", identifier(), 0 });
    callback({ "..", identifier(), 0 });

    for (unsigned pty_index : *s_ptys) {
        String name = String::number(pty_index);
        InodeIdentifier identifier = { fsid(), pty_index_to_inode_index(pty_index) };
        callback({ name, identifier, 0 });
    }

    return KSuccess;
}

KResultOr<size_t> DevPtsFSInode::directory_entry_count() const
{
    ASSERT(identifier().index() == 1);

    return 2 + s_ptys->size();
}

RefPtr<Inode> DevPtsFSInode::lookup(StringView name)
{
    ASSERT(identifier().index() == 1);

    if (name == "." || name == "..")
        return this;

    auto& fs = static_cast<DevPtsFS&>(this->fs());

    auto pty_index = name.to_uint();
    if (pty_index.has_value() && s_ptys->contains(pty_index.value())) {
        return fs.get_inode({ fsid(), pty_index_to_inode_index(pty_index.value()) });
    }

    return {};
}

void DevPtsFSInode::flush_metadata()
{
}

KResult DevPtsFSInode::add_child(Inode&, const StringView&, mode_t)
{
    return KResult(-EROFS);
}

KResultOr<NonnullRefPtr<Inode>> DevPtsFSInode::create_child(const String&, mode_t, dev_t, uid_t, gid_t)
{
    return KResult(-EROFS);
}

KResult DevPtsFSInode::remove_child(const StringView&)
{
    return KResult(-EROFS);
}

KResult DevPtsFSInode::chmod(mode_t)
{
    return KResult(-EPERM);
}

KResult DevPtsFSInode::chown(uid_t, gid_t)
{
    return KResult(-EPERM);
}

}
