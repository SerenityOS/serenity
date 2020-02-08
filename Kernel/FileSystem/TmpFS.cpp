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

#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

NonnullRefPtr<TmpFS> TmpFS::create()
{
    return adopt(*new TmpFS);
}

TmpFS::TmpFS()
{
}

TmpFS::~TmpFS()
{
}

bool TmpFS::initialize()
{
    m_root_inode = TmpFSInode::create_root(*this);
    return true;
}

InodeIdentifier TmpFS::root_inode() const
{
    ASSERT(!m_root_inode.is_null());
    return m_root_inode->identifier();
}

void TmpFS::register_inode(TmpFSInode& inode)
{
    LOCKER(m_lock);
    ASSERT(inode.identifier().fsid() == fsid());

    unsigned index = inode.identifier().index();
    m_inodes.set(index, inode);
}

void TmpFS::unregister_inode(InodeIdentifier identifier)
{
    LOCKER(m_lock);
    ASSERT(identifier.fsid() == fsid());

    m_inodes.remove(identifier.index());
}

unsigned TmpFS::next_inode_index()
{
    LOCKER(m_lock);

    return m_next_inode_index++;
}

RefPtr<Inode> TmpFS::get_inode(InodeIdentifier identifier) const
{
    LOCKER(m_lock);
    ASSERT(identifier.fsid() == fsid());

    auto it = m_inodes.find(identifier.index());
    if (it == m_inodes.end())
        return nullptr;
    return it->value;
}

RefPtr<Inode> TmpFS::create_inode(InodeIdentifier parent_id, const String& name, mode_t mode, off_t size, dev_t dev, uid_t uid, gid_t gid, int& error)
{
    LOCKER(m_lock);
    ASSERT(parent_id.fsid() == fsid());

    ASSERT(size == 0);
    ASSERT(dev == 0);

    struct timeval now;
    kgettimeofday(now);

    InodeMetadata metadata;
    metadata.mode = mode;
    metadata.uid = uid;
    metadata.gid = gid;
    metadata.atime = now.tv_sec;
    metadata.ctime = now.tv_sec;
    metadata.mtime = now.tv_sec;

    auto inode = TmpFSInode::create(*this, metadata, parent_id);

    auto it = m_inodes.find(parent_id.index());
    ASSERT(it != m_inodes.end());
    auto parent_inode = it->value;
    error = parent_inode->add_child(inode->identifier(), name, mode);

    return inode;
}

KResult TmpFS::create_directory(InodeIdentifier parent_id, const String& name, mode_t mode, uid_t uid, gid_t gid)
{
    // Ensure it's a directory.
    mode &= ~0170000;
    mode |= 0040000;
    int error;
    if (!create_inode(parent_id, name, mode, 0, 0, uid, gid, error))
        return KResult(error);
    return KSuccess;
}

TmpFSInode::TmpFSInode(TmpFS& fs, InodeMetadata metadata, InodeIdentifier parent)
    : Inode(fs, fs.next_inode_index())
    , m_metadata(metadata)
    , m_parent(parent)
{
    m_metadata.inode = identifier();
}

TmpFSInode::~TmpFSInode()
{
}

NonnullRefPtr<TmpFSInode> TmpFSInode::create(TmpFS& fs, InodeMetadata metadata, InodeIdentifier parent)
{
    auto inode = adopt(*new TmpFSInode(fs, metadata, parent));
    fs.register_inode(inode);
    return inode;
}

NonnullRefPtr<TmpFSInode> TmpFSInode::create_root(TmpFS& fs)
{
    InodeMetadata metadata;
    metadata.mode = 0041777;
    return create(fs, metadata, { fs.fsid(), 1 });
}

InodeMetadata TmpFSInode::metadata() const
{
    LOCKER(m_lock);

    return m_metadata;
}

bool TmpFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntry&)> callback) const
{
    LOCKER(m_lock);

    if (!is_directory())
        return false;

    callback({ ".", identifier(), 0 });
    callback({ "..", m_parent, 0 });

    for (auto& it : m_children)
        callback(it.value.entry);
    return true;
}

ssize_t TmpFSInode::read_bytes(off_t offset, ssize_t size, u8* buffer, FileDescription*) const
{
    LOCKER(m_lock);
    ASSERT(!is_directory());
    ASSERT(size >= 0);
    ASSERT(offset >= 0);

    if (!m_content.has_value())
        return 0;

    if (offset >= m_metadata.size)
        return 0;

    if (static_cast<off_t>(size) > m_metadata.size - offset)
        size = m_metadata.size - offset;

    memcpy(buffer, m_content.value().data() + offset, size);
    return size;
}

ssize_t TmpFSInode::write_bytes(off_t offset, ssize_t size, const u8* buffer, FileDescription*)
{
    LOCKER(m_lock);
    ASSERT(!is_directory());
    ASSERT(offset >= 0);

    off_t old_size = m_metadata.size;
    off_t new_size = m_metadata.size;
    if ((offset + size) > new_size)
        new_size = offset + size;

    if (new_size > old_size) {
        if (m_content.has_value() && m_content.value().capacity() >= (size_t)new_size) {
            m_content.value().set_size(new_size);
        } else {
            // Grow the content buffer 2x the new sizeto accomodate repeating write() calls.
            // Note that we're not actually committing physical memory to the buffer
            // until it's needed. We only grow VM here.

            // FIXME: Fix this so that no memcpy() is necessary, and we can just grow the
            //        KBuffer and it will add physical pages as needed while keeping the
            //        existing ones.
            auto tmp = KBuffer::create_with_size(new_size * 2);
            tmp.set_size(new_size);
            if (m_content.has_value())
                memcpy(tmp.data(), m_content.value().data(), old_size);
            m_content = move(tmp);
        }
        m_metadata.size = new_size;
        set_metadata_dirty(true);
        set_metadata_dirty(false);
        inode_size_changed(old_size, new_size);
    }

    memcpy(m_content.value().data() + offset, buffer, size);
    inode_contents_changed(offset, size, buffer);

    return size;
}

RefPtr<Inode> TmpFSInode::lookup(StringView name)
{
    LOCKER(m_lock);
    ASSERT(is_directory());

    if (name == ".")
        return fs().get_inode(identifier());
    if (name == "..")
        return fs().get_inode(m_parent);

    auto it = m_children.find(name);
    if (it == m_children.end())
        return {};
    return fs().get_inode(it->value.entry.inode);
}

size_t TmpFSInode::directory_entry_count() const
{
    LOCKER(m_lock);
    ASSERT(is_directory());
    return 2 + m_children.size();
}

void TmpFSInode::flush_metadata()
{
    // We don't really have any metadata that could become dirty.
    // The only reason we even call set_metadata_dirty() is
    // to let the watchers know we have updates. Once that is
    // switched to a different mechanism, we can stop ever marking
    // our metadata as dirty at all.
    set_metadata_dirty(false);
}

KResult TmpFSInode::chmod(mode_t mode)
{
    LOCKER(m_lock);

    m_metadata.mode = mode;
    set_metadata_dirty(true);
    set_metadata_dirty(false);
    return KSuccess;
}

KResult TmpFSInode::chown(uid_t uid, gid_t gid)
{
    LOCKER(m_lock);

    m_metadata.uid = uid;
    m_metadata.gid = gid;
    set_metadata_dirty(true);
    set_metadata_dirty(false);
    return KSuccess;
}

KResult TmpFSInode::add_child(InodeIdentifier child_id, const StringView& name, mode_t)
{
    LOCKER(m_lock);
    ASSERT(is_directory());
    ASSERT(child_id.fsid() == fsid());

    String owned_name = name;
    FS::DirectoryEntry entry = { owned_name.characters(), owned_name.length(), child_id, 0 };
    RefPtr<Inode> child_tmp = fs().get_inode(child_id);
    NonnullRefPtr<TmpFSInode> child = static_cast<NonnullRefPtr<TmpFSInode>>(child_tmp.release_nonnull());

    m_children.set(owned_name, { entry, move(child) });
    set_metadata_dirty(true);
    set_metadata_dirty(false);
    return KSuccess;
}

KResult TmpFSInode::remove_child(const StringView& name)
{
    LOCKER(m_lock);
    ASSERT(is_directory());

    if (name == "." || name == "..")
        return KSuccess;

    auto it = m_children.find(name);
    if (it == m_children.end())
        return KResult(-ENOENT);
    m_children.remove(it);
    set_metadata_dirty(true);
    set_metadata_dirty(false);
    return KSuccess;
}

KResult TmpFSInode::truncate(off_t size)
{
    LOCKER(m_lock);
    ASSERT(!is_directory());

    if (size == 0)
        m_content.clear();
    else if (!m_content.has_value()) {
        m_content = KBuffer::create_with_size(size);
    } else if (static_cast<size_t>(size) < m_content.value().capacity()) {
        size_t prev_size = m_metadata.size;
        m_content.value().set_size(size);
        if (prev_size < static_cast<size_t>(size))
            memset(m_content.value().data() + prev_size, 0, size - prev_size);
    } else {
        size_t prev_size = m_metadata.size;
        KBuffer tmp = KBuffer::create_with_size(size);
        memcpy(tmp.data(), m_content.value().data(), prev_size);
        m_content = move(tmp);
    }

    size_t old_size = m_metadata.size;
    m_metadata.size = size;
    set_metadata_dirty(true);
    set_metadata_dirty(false);

    if (old_size != (size_t)size) {
        inode_size_changed(old_size, size);
        if (m_content.has_value())
            inode_contents_changed(0, size, m_content.value().data());
    }

    return KSuccess;
}

int TmpFSInode::set_atime(time_t time)
{
    LOCKER(m_lock);

    m_metadata.atime = time;
    set_metadata_dirty(true);
    set_metadata_dirty(false);
    return KSuccess;
}

int TmpFSInode::set_ctime(time_t time)
{
    LOCKER(m_lock);

    m_metadata.ctime = time;
    set_metadata_dirty(true);
    set_metadata_dirty(false);
    return KSuccess;
}

int TmpFSInode::set_mtime(time_t time)
{
    LOCKER(m_lock);

    m_metadata.mtime = time;
    set_metadata_dirty(true);
    set_metadata_dirty(false);
    return KSuccess;
}

void TmpFSInode::one_ref_left()
{
    // Destroy ourselves.
    fs().unregister_inode(identifier());
}
