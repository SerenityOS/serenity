/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/Process.h>
#include <LibC/limits.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<FileSystem>> TmpFS::try_create()
{
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) TmpFS));
}

TmpFS::TmpFS() = default;
TmpFS::~TmpFS() = default;

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

TmpFSInode::TmpFSInode(TmpFS& fs, InodeMetadata const& metadata, LockWeakPtr<TmpFSInode> parent)
    : Inode(fs, fs.next_inode_index())
    , m_metadata(metadata)
    , m_parent(move(parent))
{
    m_metadata.inode = identifier();
}

TmpFSInode::~TmpFSInode() = default;

ErrorOr<NonnullLockRefPtr<TmpFSInode>> TmpFSInode::try_create(TmpFS& fs, InodeMetadata const& metadata, LockWeakPtr<TmpFSInode> parent)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) TmpFSInode(fs, metadata, move(parent)));
}

ErrorOr<NonnullLockRefPtr<TmpFSInode>> TmpFSInode::try_create_root(TmpFS& fs)
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

    TRY(callback({ "."sv, identifier(), 0 }));
    if (auto parent = m_parent.strong_ref())
        TRY(callback({ ".."sv, parent->identifier(), 0 }));

    for (auto& child : m_children) {
        TRY(callback({ child.name->view(), child.inode->identifier(), 0 }));
    }
    return {};
}

ErrorOr<NonnullOwnPtr<TmpFSInode::DataBlock>> TmpFSInode::DataBlock::create()
{
    auto data_block_buffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_with_size(DataBlock::block_size, AllocationStrategy::AllocateNow));
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) DataBlock(move(data_block_buffer_vmobject))));
}

ErrorOr<void> TmpFSInode::ensure_allocated_blocks(size_t offset, size_t io_size)
{
    VERIFY(m_inode_lock.is_locked());
    size_t block_start_index = offset / DataBlock::block_size;
    size_t block_last_index = ((offset + io_size) / DataBlock::block_size) + (((offset + io_size) % DataBlock::block_size) == 0 ? 0 : 1);
    VERIFY(block_start_index <= block_last_index);

    size_t original_size = m_blocks.size();
    Vector<size_t> allocated_block_indices;
    ArmedScopeGuard clean_allocated_blocks_on_failure([&] {
        for (auto index : allocated_block_indices)
            m_blocks[index].clear();
        MUST(m_blocks.try_resize(original_size));
    });

    if (m_blocks.size() < (block_last_index))
        TRY(m_blocks.try_resize(block_last_index));

    for (size_t block_index = block_start_index; block_index < block_last_index; block_index++) {
        if (!m_blocks[block_index]) {
            TRY(allocated_block_indices.try_append(block_index));
            m_blocks[block_index] = TRY(DataBlock::create());
        }
    }
    clean_allocated_blocks_on_failure.disarm();
    return {};
}

ErrorOr<size_t> TmpFSInode::read_bytes_from_content_space(size_t offset, size_t io_size, UserOrKernelBuffer& buffer) const
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(m_metadata.size >= 0);
    if (static_cast<size_t>(m_metadata.size) < offset)
        return 0;
    auto mapping_region = TRY(MM.allocate_kernel_region(DataBlock::block_size, "TmpFSInode Mapping Region"sv, Memory::Region::Access::Read, AllocationStrategy::Reserve));
    return const_cast<TmpFSInode&>(*this).do_io_on_content_space(*mapping_region, offset, io_size, buffer, false);
}

ErrorOr<size_t> TmpFSInode::read_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(!is_directory());
    return read_bytes_from_content_space(offset, size, buffer);
}

ErrorOr<size_t> TmpFSInode::write_bytes_to_content_space(size_t offset, size_t io_size, UserOrKernelBuffer const& buffer)
{
    VERIFY(m_inode_lock.is_locked());
    auto mapping_region = TRY(MM.allocate_kernel_region(DataBlock::block_size, "TmpFSInode Mapping Region"sv, Memory::Region::Access::Write, AllocationStrategy::Reserve));
    return do_io_on_content_space(*mapping_region, offset, io_size, const_cast<UserOrKernelBuffer&>(buffer), true);
}

ErrorOr<size_t> TmpFSInode::write_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer const& buffer, OpenFileDescription*)
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(!is_directory());
    VERIFY(offset >= 0);

    TRY(ensure_allocated_blocks(offset, size));
    auto nwritten = TRY(write_bytes_to_content_space(offset, size, buffer));

    off_t old_size = m_metadata.size;
    off_t new_size = m_metadata.size;
    if (static_cast<off_t>(offset + size) > new_size)
        new_size = offset + size;

    if (new_size > old_size) {
        m_metadata.size = new_size;
        set_metadata_dirty(true);
    }
    did_modify_contents();
    return nwritten;
}

ErrorOr<size_t> TmpFSInode::do_io_on_content_space(Memory::Region& mapping_region, size_t offset, size_t io_size, UserOrKernelBuffer& buffer, bool write)
{
    VERIFY(m_inode_lock.is_locked());
    size_t remaining_bytes = 0;
    if (!write) {
        // Note: For read operations, only perform read until the last byte.
        // If we are beyond the last byte, return 0 to indicate EOF.
        remaining_bytes = min(io_size, m_metadata.size - offset);
        if (remaining_bytes == 0)
            return 0;
    } else {
        remaining_bytes = io_size;
    }
    VERIFY(remaining_bytes != 0);

    UserOrKernelBuffer current_buffer = buffer.offset(0);
    auto block_start_index = offset / DataBlock::block_size;
    auto offset_in_block = offset % DataBlock::block_size;
    u64 block_index = block_start_index;
    size_t nio = 0;
    while (remaining_bytes > 0) {
        size_t current_io_size = min(DataBlock::block_size - offset_in_block, remaining_bytes);
        auto& block = m_blocks[block_index];
        if (!block && !write) {
            // Note: If the block does not exist then it's just a gap in the file,
            // so the buffer should be placed with zeroes in that section.
            TRY(current_buffer.memset(0, 0, current_io_size));
            remaining_bytes -= current_io_size;
            current_buffer = current_buffer.offset(current_io_size);
            nio += current_io_size;
            block_index++;
            // Note: Clear offset_in_block to zero to ensure that if we started from a middle of
            // a block, then next writes are just going to happen from the start of each block until the end.
            offset_in_block = 0;
            continue;
        } else if (!block) {
            return Error::from_errno(EIO);
        }

        NonnullLockRefPtr<Memory::AnonymousVMObject> block_vmobject = block->vmobject();
        mapping_region.set_vmobject(block_vmobject);
        mapping_region.remap();
        if (write)
            TRY(current_buffer.read(mapping_region.vaddr().offset(offset_in_block).as_ptr(), 0, current_io_size));
        else
            TRY(current_buffer.write(mapping_region.vaddr().offset(offset_in_block).as_ptr(), 0, current_io_size));
        current_buffer = current_buffer.offset(current_io_size);
        nio += current_io_size;
        remaining_bytes -= current_io_size;
        block_index++;
        // Note: Clear offset_in_block to zero to ensure that if we started from a middle of
        // a block, then next writes are just going to happen from the start of each block until the end.
        offset_in_block = 0;
    }
    VERIFY(nio <= io_size);
    return nio;
}

ErrorOr<void> TmpFSInode::truncate_to_block_index(size_t block_index)
{
    VERIFY(m_inode_lock.is_locked());
    TRY(m_blocks.try_resize(block_index));
    return {};
}

ErrorOr<NonnullLockRefPtr<Inode>> TmpFSInode::lookup(StringView name)
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

ErrorOr<NonnullLockRefPtr<Inode>> TmpFSInode::create_child(StringView name, mode_t mode, dev_t dev, UserID uid, GroupID gid)
{
    MutexLocker locker(m_inode_lock);
    time_t now = kgettimeofday().to_truncated_seconds();

    InodeMetadata metadata;
    metadata.mode = mode;
    metadata.uid = uid;
    metadata.gid = gid;
    metadata.atime = now;
    metadata.ctime = now;
    metadata.mtime = now;
    metadata.major_device = major_from_encoded_device(dev);
    metadata.minor_device = minor_from_encoded_device(dev);

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

    u64 block_index = size / DataBlock::block_size + ((size % DataBlock::block_size == 0) ? 0 : 1);
    TRY(truncate_to_block_index(block_index));

    u64 last_possible_block_index = size / DataBlock::block_size;
    if ((size % DataBlock::block_size != 0) && m_blocks[last_possible_block_index]) {
        auto mapping_region = TRY(MM.allocate_kernel_region(DataBlock::block_size, "TmpFSInode Mapping Region"sv, Memory::Region::Access::Write, AllocationStrategy::Reserve));
        VERIFY(m_blocks[last_possible_block_index]);
        NonnullLockRefPtr<Memory::AnonymousVMObject> block_vmobject = m_blocks[last_possible_block_index]->vmobject();
        mapping_region->set_vmobject(block_vmobject);
        mapping_region->remap();
        memset(mapping_region->vaddr().offset(size % DataBlock::block_size).as_ptr(), 0, DataBlock::block_size - (size % DataBlock::block_size));
    }
    m_metadata.size = size;
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> TmpFSInode::update_timestamps(Optional<time_t> atime, Optional<time_t> ctime, Optional<time_t> mtime)
{
    MutexLocker locker(m_inode_lock);

    if (atime.has_value())
        m_metadata.atime = atime.value();
    if (ctime.has_value())
        m_metadata.ctime = ctime.value();
    if (mtime.has_value())
        m_metadata.ctime = mtime.value();
    set_metadata_dirty(true);
    return {};
}

}
