/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/Process.h>
#include <LibC/limits.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<TmpFS>> TmpFS::try_create()
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) TmpFS);
}

TmpFS::TmpFS()
{
}

TmpFS::~TmpFS()
{
}

ErrorOr<void> TmpFS::initialize()
{
    m_root_inode = TRY(TmpFSInode::try_create_root(*this));
    return {};
}

Inode& TmpFS::root_inode()
{
    VERIFY(!m_root_inode.is_null());
    return *m_root_inode;
}

unsigned TmpFS::next_inode_index()
{
    MutexLocker locker(m_lock);

    return m_next_inode_index++;
}

TmpFSInode::TmpFSInode(TmpFS& fs, const InodeMetadata& metadata, WeakPtr<TmpFSInode> parent)
    : Inode(fs, fs.next_inode_index())
    , m_metadata(metadata)
    , m_parent(move(parent))
{
    m_metadata.inode = identifier();
}

TmpFSInode::~TmpFSInode()
{
}

ErrorOr<NonnullRefPtr<TmpFSInode>> TmpFSInode::try_create(TmpFS& fs, InodeMetadata const& metadata, WeakPtr<TmpFSInode> parent)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) TmpFSInode(fs, metadata, move(parent)));
}

ErrorOr<NonnullRefPtr<TmpFSInode>> TmpFSInode::try_create_root(TmpFS& fs)
{
    InodeMetadata metadata;
    auto now = kgettimeofday().to_truncated_seconds();
    metadata.atime = now;
    metadata.ctime = now;
    metadata.mtime = now;
    metadata.mode = S_IFDIR | S_ISVTX | 0777;
    return try_create(fs, metadata, {});
}

InodeMetadata TmpFSInode::metadata() const
{
    MutexLocker locker(m_inode_lock, Mutex::Mode::Shared);

    return m_metadata;
}

ErrorOr<void> TmpFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(m_inode_lock, Mutex::Mode::Shared);

    if (!is_directory())
        return ENOTDIR;

    TRY(callback({ ".", identifier(), 0 }));
    if (auto parent = m_parent.strong_ref())
        TRY(callback({ "..", parent->identifier(), 0 }));

    for (auto& child : m_children) {
        TRY(callback({ child.name->view(), child.inode->identifier(), 0 }));
    }
    return {};
}

ErrorOr<size_t> TmpFSInode::read_bytes(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
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

    TRY(buffer.write(m_content->data() + offset, size));
    return size;
}

ErrorOr<size_t> TmpFSInode::write_bytes(off_t offset, size_t size, const UserOrKernelBuffer& buffer, OpenFileDescription*)
{
    MutexLocker locker(m_inode_lock);
    VERIFY(!is_directory());
    VERIFY(offset >= 0);

    TRY(prepare_to_write_data());

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
            // Grow the content buffer 2x the new size to accommodate repeating write() calls.
            // Note that we're not actually committing physical memory to the buffer
            // until it's needed. We only grow VM here.

            // FIXME: Fix this so that no memcpy() is necessary, and we can just grow the
            //        KBuffer and it will add physical pages as needed while keeping the
            //        existing ones.
            auto tmp = TRY(KBuffer::try_create_with_size(new_size * 2));
            tmp->set_size(new_size);
            if (m_content)
                memcpy(tmp->data(), m_content->data(), old_size);
            m_content = move(tmp);
        }
        m_metadata.size = new_size;
        set_metadata_dirty(true);
    }

    TRY(buffer.read(m_content->data() + offset, size)); // TODO: partial reads?

    did_modify_contents();
    return size;
}

ErrorOr<NonnullRefPtr<Inode>> TmpFSInode::lookup(StringView name)
{
    MutexLocker locker(m_inode_lock, Mutex::Mode::Shared);
    VERIFY(is_directory());

    if (name == ".")
        return *this;
    if (name == "..") {
        if (auto parent = m_parent.strong_ref())
            return parent.release_nonnull();
        return ENOENT;
    }

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

ErrorOr<void> TmpFSInode::flush_metadata()
{
    // We don't really have any metadata that could become dirty.
    // The only reason we even call set_metadata_dirty() is
    // to let the watchers know we have updates. Once that is
    // switched to a different mechanism, we can stop ever marking
    // our metadata as dirty at all.
    set_metadata_dirty(false);
    return {};
}

ErrorOr<void> TmpFSInode::chmod(mode_t mode)
{
    MutexLocker locker(m_inode_lock);

    m_metadata.mode = mode;
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> TmpFSInode::chown(UserID uid, GroupID gid)
{
    MutexLocker locker(m_inode_lock);

    m_metadata.uid = uid;
    m_metadata.gid = gid;
    set_metadata_dirty(true);
    return {};
}

ErrorOr<NonnullRefPtr<Inode>> TmpFSInode::create_child(StringView name, mode_t mode, dev_t dev, UserID uid, GroupID gid)
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

    auto child = TRY(TmpFSInode::try_create(fs(), metadata, *this));
    TRY(add_child(*child, name, mode));
    return child;
}

ErrorOr<void> TmpFSInode::add_child(Inode& child, StringView name, mode_t)
{
    VERIFY(is_directory());
    VERIFY(child.fsid() == fsid());

    if (name.length() > NAME_MAX)
        return ENAMETOOLONG;

    MutexLocker locker(m_inode_lock);
    for (auto const& existing_child : m_children) {
        if (existing_child.name->view() == name)
            return EEXIST;
    }

    auto name_kstring = TRY(KString::try_create(name));
    // Balanced by `delete` in remove_child()

    auto* child_entry = new (nothrow) Child { move(name_kstring), static_cast<TmpFSInode&>(child) };
    if (!child_entry)
        return ENOMEM;

    m_children.append(*child_entry);
    did_add_child(child.identifier(), name);
    return {};
}

ErrorOr<void> TmpFSInode::remove_child(StringView name)
{
    MutexLocker locker(m_inode_lock);
    VERIFY(is_directory());

    if (name == "." || name == "..")
        return {};

    auto* child = find_child_by_name(name);
    if (!child)
        return ENOENT;

    auto child_id = child->inode->identifier();
    child->inode->did_delete_self();
    m_children.remove(*child);
    did_remove_child(child_id, name);
    // Balanced by `new` in add_child()
    delete child;
    return {};
}

ErrorOr<void> TmpFSInode::truncate(u64 size)
{
    MutexLocker locker(m_inode_lock);
    VERIFY(!is_directory());

    if (size == 0)
        m_content.clear();
    else if (!m_content) {
        m_content = TRY(KBuffer::try_create_with_size(size));
    } else if (static_cast<size_t>(size) < m_content->capacity()) {
        size_t prev_size = m_metadata.size;
        m_content->set_size(size);
        if (prev_size < static_cast<size_t>(size))
            memset(m_content->data() + prev_size, 0, size - prev_size);
    } else {
        size_t prev_size = m_metadata.size;
        auto tmp = TRY(KBuffer::try_create_with_size(size));
        memcpy(tmp->data(), m_content->data(), prev_size);
        m_content = move(tmp);
    }

    m_metadata.size = size;
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> TmpFSInode::set_atime(time_t time)
{
    MutexLocker locker(m_inode_lock);

    m_metadata.atime = time;
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> TmpFSInode::set_ctime(time_t time)
{
    MutexLocker locker(m_inode_lock);

    m_metadata.ctime = time;
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> TmpFSInode::set_mtime(time_t t)
{
    MutexLocker locker(m_inode_lock);

    m_metadata.mtime = t;
    set_metadata_dirty(true);
    return {};
}

}
