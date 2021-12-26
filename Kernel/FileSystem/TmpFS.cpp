/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/Process.h>
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

KResult TmpFS::initialize()
{
    m_root_inode = TmpFSInode::create_root(*this);
    if (!m_root_inode)
        return ENOMEM;
    return KSuccess;
}

Inode& TmpFS::root_inode()
{
    VERIFY(!m_root_inode.is_null());
    return *m_root_inode;
}

void TmpFS::register_inode(TmpFSInode& inode)
{
    MutexLocker locker(m_lock);
    VERIFY(inode.identifier().fsid() == fsid());

    auto index = inode.identifier().index();
    m_inodes.set(index, inode);
}

void TmpFS::unregister_inode(InodeIdentifier identifier)
{
    MutexLocker locker(m_lock);
    VERIFY(identifier.fsid() == fsid());

    m_inodes.remove(identifier.index());
}

unsigned TmpFS::next_inode_index()
{
    MutexLocker locker(m_lock);

    return m_next_inode_index++;
}

KResultOr<NonnullRefPtr<Inode>> TmpFS::get_inode(InodeIdentifier identifier) const
{
    MutexLocker locker(m_lock, Mutex::Mode::Shared);
    VERIFY(identifier.fsid() == fsid());

    auto it = m_inodes.find(identifier.index());
    if (it == m_inodes.end())
        return ENOENT;
    return it->value;
}

TmpFSInode::TmpFSInode(TmpFS& fs, const InodeMetadata& metadata, InodeIdentifier parent)
    : Inode(fs, fs.next_inode_index())
    , m_metadata(metadata)
    , m_parent(parent)
{
    m_metadata.inode = identifier();
}

TmpFSInode::~TmpFSInode()
{
}

RefPtr<TmpFSInode> TmpFSInode::create(TmpFS& fs, const InodeMetadata& metadata, InodeIdentifier parent)
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
    MutexLocker locker(m_inode_lock, Mutex::Mode::Shared);

    return m_metadata;
}

KResult TmpFSInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(m_inode_lock, Mutex::Mode::Shared);

    if (!is_directory())
        return ENOTDIR;

    callback({ ".", identifier(), 0 });
    callback({ "..", m_parent, 0 });

    for (auto& child : m_children) {
        callback({ child.name->view(), child.inode->identifier(), 0 });
    }
    return KSuccess;
}

KResultOr<size_t> TmpFSInode::read_bytes(off_t offset, size_t size, UserOrKernelBuffer& buffer, FileDescription*) const
{
    MutexLocker locker(m_inode_lock, Mutex::Mode::Shared);
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
    MutexLocker locker(m_inode_lock);
    VERIFY(!is_directory());
    VERIFY(offset >= 0);

    auto result = prepare_to_write_data();
    if (result.is_error())
        return result;

    off_t old_size = m_metadata.size;
    off_t new_size = m_metadata.size;
    if (static_cast<off_t>(offset + size) > new_size)
        new_size = offset + size;

    if (static_cast<u64>(new_size) > (NumericLimits<size_t>::max() / 2)) // on 32-bit, size_t might be 32 bits while off_t is 64 bits
        return ENOMEM;                                                   // we won't be able to resize to this capacity

    if (new_size > old_size) {
        if (m_content && static_cast<off_t>(m_content->capacity()) >= new_size) {
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
        notify_watchers();
    }

    if (!buffer.read(m_content->data() + offset, size)) // TODO: partial reads?
        return EFAULT;

    did_modify_contents();
    return size;
}

KResultOr<NonnullRefPtr<Inode>> TmpFSInode::lookup(StringView name)
{
    MutexLocker locker(m_inode_lock, Mutex::Mode::Shared);
    VERIFY(is_directory());

    if (name == ".")
        return *this;
    if (name == "..")
        return fs().get_inode(m_parent);

    auto* child = find_child_by_name(name);
    if (!child)
        return ENOENT;
    return child->inode;
}

TmpFSInode::Child* TmpFSInode::find_child_by_name(StringView name)
{
    for (auto& child : m_children) {
        if (child.name->view() == name)
            return &child;
    }
    return nullptr;
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
    MutexLocker locker(m_inode_lock);

    m_metadata.mode = mode;
    notify_watchers();
    return KSuccess;
}

KResult TmpFSInode::chown(UserID uid, GroupID gid)
{
    MutexLocker locker(m_inode_lock);

    m_metadata.uid = uid;
    m_metadata.gid = gid;
    notify_watchers();
    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> TmpFSInode::create_child(StringView name, mode_t mode, dev_t dev, UserID uid, GroupID gid)
{
    MutexLocker locker(m_inode_lock);

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

KResult TmpFSInode::add_child(Inode& child, StringView const& name, mode_t)
{
    VERIFY(is_directory());
    VERIFY(child.fsid() == fsid());

    if (name.length() > NAME_MAX)
        return ENAMETOOLONG;

    auto name_kstring = KString::try_create(name);
    if (!name_kstring)
        return ENOMEM;

    auto* child_entry = new (nothrow) Child { name_kstring.release_nonnull(), static_cast<TmpFSInode&>(child) };
    if (!child_entry)
        return ENOMEM;

    MutexLocker locker(m_inode_lock);
    m_children.append(*child_entry);
    did_add_child(child.identifier(), name);
    return KSuccess;
}

KResult TmpFSInode::remove_child(StringView const& name)
{
    MutexLocker locker(m_inode_lock);
    VERIFY(is_directory());

    if (name == "." || name == "..")
        return KSuccess;

    auto* child = find_child_by_name(name);
    if (!child)
        return ENOENT;

    auto child_id = child->inode->identifier();
    child->inode->did_delete_self();
    m_children.remove(*child);
    did_remove_child(child_id, name);
    return KSuccess;
}

KResult TmpFSInode::truncate(u64 size)
{
    MutexLocker locker(m_inode_lock);
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
    MutexLocker locker(m_inode_lock);

    m_metadata.atime = time;
    notify_watchers();
    return KSuccess;
}

KResult TmpFSInode::set_ctime(time_t time)
{
    MutexLocker locker(m_inode_lock);

    m_metadata.ctime = time;
    notify_watchers();
    return KSuccess;
}

KResult TmpFSInode::set_mtime(time_t t)
{
    MutexLocker locker(m_inode_lock);

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
