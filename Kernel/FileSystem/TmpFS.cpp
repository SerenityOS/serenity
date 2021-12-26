/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <LibC/limits.h>

namespace Kernel {

RefPtr<TmpFS> TmpFS::create()
{
    return adopt_ref_if_nonnull(new (nothrow) TmpFS);
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
    return !m_root_inode.is_null();
}

NonnullRefPtr<Inode> TmpFS::root_inode() const
{
    VERIFY(!m_root_inode.is_null());

    return *m_root_inode;
}

void TmpFS::register_inode(TmpFSInode& inode)
{
    Locker locker(m_lock);
    VERIFY(inode.identifier().fsid() == fsid());

    auto index = inode.identifier().index();
    m_inodes.set(index, inode);
}

void TmpFS::unregister_inode(InodeIdentifier identifier)
{
    Locker locker(m_lock);
    VERIFY(identifier.fsid() == fsid());

    m_inodes.remove(identifier.index());
}

unsigned TmpFS::next_inode_index()
{
    Locker locker(m_lock);

    return m_next_inode_index++;
}

RefPtr<Inode> TmpFS::get_inode(InodeIdentifier identifier) const
{
    Locker locker(m_lock, Lock::Mode::Shared);
    VERIFY(identifier.fsid() == fsid());

    auto it = m_inodes.find(identifier.index());
    if (it == m_inodes.end())
        return nullptr;
    return it->value;
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

RefPtr<TmpFSInode> TmpFSInode::create(TmpFS& fs, InodeMetadata metadata, InodeIdentifier parent)
{
    auto inode = adopt_ref_if_nonnull(new (nothrow) TmpFSInode(fs, metadata, parent));
    if (inode)
        fs.register_inode(*inode);
    return inode;
}

RefPtr<TmpFSInode> TmpFSInode::create_root(TmpFS& fs)
{
    InodeMetadata metadata;
    auto now = kgettimeofday().to_truncated_seconds();
    metadata.atime = now;
    metadata.ctime = now;
    metadata.mtime = now;
    metadata.mode = S_IFDIR | S_ISVTX | 0777;
    return create(fs, metadata, { fs.fsid(), 1 });
}

InodeMetadata TmpFSInode::metadata() const
{
    Locker locker(m_lock, Lock::Mode::Shared);

    return m_metadata;
}

KResult TmpFSInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    Locker locker(m_lock, Lock::Mode::Shared);

    if (!is_directory())
        return ENOTDIR;

    callback({ ".", identifier(), 0 });
    callback({ "..", m_parent, 0 });

    for (auto& it : m_children) {
        auto& entry = it.value;
        callback({ entry.name, entry.inode->identifier(), 0 });
    }
    return KSuccess;
}

KResultOr<size_t> TmpFSInode::read_bytes(off_t offset, size_t size, UserOrKernelBuffer& buffer, FileDescription*) const
{
    Locker locker(m_lock, Lock::Mode::Shared);
    VERIFY(!is_directory());
    VERIFY(offset >= 0);

    if (!m_content)
        return 0;

    if (offset >= m_metadata.size)
        return 0;

    if (static_cast<off_t>(size) > m_metadata.size - offset)
        size = m_metadata.size - offset;

    if (!buffer.write(m_content->data() + offset, size))
        return EFAULT;
    return size;
}

KResultOr<size_t> TmpFSInode::write_bytes(off_t offset, size_t size, const UserOrKernelBuffer& buffer, FileDescription*)
{
    Locker locker(m_lock);
    VERIFY(!is_directory());
    VERIFY(offset >= 0);

    auto result = prepare_to_write_data();
    if (result.is_error())
        return result;

    off_t old_size = m_metadata.size;
    off_t new_size = m_metadata.size;
    if (offset + size > (size_t)new_size)
        new_size = offset + size;

    if (new_size > old_size) {
        if (m_content && m_content->capacity() >= (size_t)new_size) {
            m_content->set_size(new_size);
        } else {
            // Grow the content buffer 2x the new sizeto accommodate repeating write() calls.
            // Note that we're not actually committing physical memory to the buffer
            // until it's needed. We only grow VM here.

            // FIXME: Fix this so that no memcpy() is necessary, and we can just grow the
            //        KBuffer and it will add physical pages as needed while keeping the
            //        existing ones.
            auto tmp = KBuffer::try_create_with_size(new_size * 2);
            if (!tmp)
                return ENOMEM;
            tmp->set_size(new_size);
            if (m_content)
                memcpy(tmp->data(), m_content->data(), old_size);
            m_content = move(tmp);
        }
        m_metadata.size = new_size;
        set_metadata_dirty(true);
        set_metadata_dirty(false);
    }

    if (!buffer.read(m_content->data() + offset, size)) // TODO: partial reads?
        return EFAULT;

    did_modify_contents();
    return size;
}

RefPtr<Inode> TmpFSInode::lookup(StringView name)
{
    Locker locker(m_lock, Lock::Mode::Shared);
    VERIFY(is_directory());

    if (name == ".")
        return this;
    if (name == "..")
        return fs().get_inode(m_parent);

    auto it = m_children.find(name);
    if (it == m_children.end())
        return {};
    return fs().get_inode(it->value.inode->identifier());
}

KResultOr<size_t> TmpFSInode::directory_entry_count() const
{
    Locker locker(m_lock, Lock::Mode::Shared);
    VERIFY(is_directory());
    return 2 + m_children.size();
}

void TmpFSInode::notify_watchers()
{
    set_metadata_dirty(true);
    set_metadata_dirty(false);
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
    Locker locker(m_lock);

    m_metadata.mode = mode;
    notify_watchers();
    return KSuccess;
}

KResult TmpFSInode::chown(uid_t uid, gid_t gid)
{
    Locker locker(m_lock);

    m_metadata.uid = uid;
    m_metadata.gid = gid;
    notify_watchers();
    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> TmpFSInode::create_child(const String& name, mode_t mode, dev_t dev, uid_t uid, gid_t gid)
{
    Locker locker(m_lock);

    // TODO: Support creating devices on TmpFS.
    if (dev != 0)
        return ENOTSUP;

    time_t now = kgettimeofday().to_truncated_seconds();

    InodeMetadata metadata;
    metadata.mode = mode;
    metadata.uid = uid;
    metadata.gid = gid;
    metadata.atime = now;
    metadata.ctime = now;
    metadata.mtime = now;

    auto child = TmpFSInode::create(fs(), metadata, identifier());
    if (!child)
        return ENOMEM;
    auto result = add_child(*child, name, mode);
    if (result.is_error())
        return result;
    return child.release_nonnull();
}

KResult TmpFSInode::add_child(Inode& child, const StringView& name, mode_t)
{
    Locker locker(m_lock);
    VERIFY(is_directory());
    VERIFY(child.fsid() == fsid());

    if (name.length() > NAME_MAX)
        return ENAMETOOLONG;

    m_children.set(name, { name, static_cast<TmpFSInode&>(child) });
    did_add_child(child.identifier(), name);
    return KSuccess;
}

KResult TmpFSInode::remove_child(const StringView& name)
{
    Locker locker(m_lock);
    VERIFY(is_directory());

    if (name == "." || name == "..")
        return KSuccess;

    auto it = m_children.find(name);
    if (it == m_children.end())
        return ENOENT;
    auto child_id = it->value.inode->identifier();
    it->value.inode->did_delete_self();
    m_children.remove(it);
    did_remove_child(child_id, name);
    return KSuccess;
}

KResult TmpFSInode::truncate(u64 size)
{
    Locker locker(m_lock);
    VERIFY(!is_directory());

    if (size == 0)
        m_content.clear();
    else if (!m_content) {
        m_content = KBuffer::try_create_with_size(size);
        if (!m_content)
            return ENOMEM;
    } else if (static_cast<size_t>(size) < m_content->capacity()) {
        size_t prev_size = m_metadata.size;
        m_content->set_size(size);
        if (prev_size < static_cast<size_t>(size))
            memset(m_content->data() + prev_size, 0, size - prev_size);
    } else {
        size_t prev_size = m_metadata.size;
        auto tmp = KBuffer::try_create_with_size(size);
        if (!tmp)
            return ENOMEM;
        memcpy(tmp->data(), m_content->data(), prev_size);
        m_content = move(tmp);
    }

    m_metadata.size = size;
    notify_watchers();
    return KSuccess;
}

KResult TmpFSInode::set_atime(time_t time)
{
    Locker locker(m_lock);

    m_metadata.atime = time;
    set_metadata_dirty(true);
    set_metadata_dirty(false);
    return KSuccess;
}

KResult TmpFSInode::set_ctime(time_t time)
{
    Locker locker(m_lock);

    m_metadata.ctime = time;
    notify_watchers();
    return KSuccess;
}

KResult TmpFSInode::set_mtime(time_t t)
{
    Locker locker(m_lock);

    m_metadata.mtime = t;
    notify_watchers();
    return KSuccess;
}

void TmpFSInode::one_ref_left()
{
    // Destroy ourselves.
    fs().unregister_inode(identifier());
}

}
