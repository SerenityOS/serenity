/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/FUSE/Definitions.h>
#include <Kernel/FileSystem/FUSE/Inode.h>
#include <Kernel/FileSystem/RAMBackedFileType.h>

namespace Kernel {

FUSEInode::FUSEInode(FUSE& fs, InodeIndex index, InodeMetadata const& metadata)
    : Inode(fs, index)
    , m_metadata(metadata)
{
    m_metadata.inode = identifier();
}

FUSEInode::FUSEInode(FUSE& fs)
    : Inode(fs, 1)
{
    m_metadata.inode = identifier();
}

FUSEInode::~FUSEInode() = default;

ErrorOr<size_t> FUSEInode::read_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(!is_directory());
    if (offset >= m_metadata.size)
        return 0;

    constexpr size_t max_read_size = 0x21000 - sizeof(fuse_in_header) - sizeof(fuse_read_in);
    u64 id = TRY(try_open(false, O_RDONLY));
    u32 nodeid = identifier().index().value();

    size_t nread = 0;
    size_t target_size = min(m_metadata.size, size);
    while (target_size) {
        size_t chunk_size = min(size, max_read_size);
        fuse_read_in payload {};
        payload.fh = id;
        payload.offset = offset + nread;
        payload.size = chunk_size;
        auto response = TRY(fs().m_connection->send_request_and_wait_for_a_reply(FUSE_READ, nodeid, { &payload, sizeof(payload) }));

        fuse_out_header* header = bit_cast<fuse_out_header*>(response->data());
        if (header->error)
            return Error::from_errno(-header->error);

        u32 data_size = min(target_size, header->len - sizeof(fuse_out_header));
        if (offset >= data_size || data_size == 0)
            return 0;

        u8* data = bit_cast<u8*>(response->data() + sizeof(fuse_out_header));

        TRY(buffer.write(data, nread, data_size));
        nread += data_size;
        target_size -= data_size;
    }

    TRY(try_flush(id));
    TRY(try_release(id, false));

    return nread;
}

ErrorOr<size_t> FUSEInode::write_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer const& buffer, OpenFileDescription*)
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(!is_directory());
    VERIFY(offset >= 0);

    constexpr size_t max_write_size = 0x21000 - sizeof(fuse_in_header) - sizeof(fuse_write_in);
    u64 id = TRY(try_open(false, O_WRONLY));
    u32 nodeid = identifier().index().value();

    size_t nwritten = 0;
    while (size) {
        size_t chunk_size = min(size, max_write_size);
        auto request_buffer = TRY(KBuffer::try_create_with_size("FUSE: Write buffer"sv, sizeof(fuse_write_in) + chunk_size));
        fuse_write_in* write_header = bit_cast<fuse_write_in*>(request_buffer->data());
        write_header->fh = id;
        write_header->offset = offset + nwritten;
        write_header->size = chunk_size;
        TRY(buffer.read(request_buffer->data() + sizeof(fuse_write_in), nwritten, chunk_size));

        auto response = TRY(fs().m_connection->send_request_and_wait_for_a_reply(FUSE_WRITE, nodeid, request_buffer->bytes()));

        fuse_out_header* header = bit_cast<fuse_out_header*>(response->data());
        if (header->error)
            return Error::from_errno(-header->error);

        fuse_write_out* write_response = bit_cast<fuse_write_out*>(response->data() + sizeof(fuse_out_header));

        nwritten += write_response->size;
        size -= write_response->size;
    }

    TRY(try_flush(id));
    TRY(try_release(id, false));

    return nwritten;
}

InodeMetadata FUSEInode::metadata() const
{
    return m_metadata;
}

ErrorOr<u64> FUSEInode::try_open(bool directory, u32 flags) const
{
    u32 id = identifier().index().value();

    fuse_open_in payload {};
    payload.flags = flags;

    int opcode = directory ? FUSE_OPENDIR : FUSE_OPEN;
    auto response = TRY(fs().m_connection->send_request_and_wait_for_a_reply(opcode, id, { &payload, sizeof(payload) }));

    fuse_out_header* header = bit_cast<fuse_out_header*>(response->data());
    if (header->error)
        return Error::from_errno(-header->error);

    fuse_open_out* open_response = bit_cast<fuse_open_out*>(response->data() + sizeof(fuse_out_header));
    return open_response->fh;
}

ErrorOr<void> FUSEInode::try_flush(u64 id) const
{
    u32 nodeid = identifier().index().value();

    fuse_flush_in payload {};
    payload.fh = id;
    (void)TRY(fs().m_connection->send_request_and_wait_for_a_reply(FUSE_FLUSH, nodeid, { &payload, sizeof(payload) }));

    return {};
}

ErrorOr<void> FUSEInode::try_release(u64 id, bool directory) const
{
    u32 nodeid = identifier().index().value();

    fuse_release_in payload {};
    payload.fh = id;
    int opcode = directory ? FUSE_RELEASEDIR : FUSE_RELEASE;
    (void)TRY(fs().m_connection->send_request_and_wait_for_a_reply(opcode, nodeid, { &payload, sizeof(payload) }));

    return {};
}

static size_t get_dirent_entry_length(size_t name_length)
{
    return name_length + FUSE_NAME_OFFSET;
}

static size_t get_dirent_entry_length_padded(size_t name_length)
{
    return FUSE_DIRENT_ALIGN(get_dirent_entry_length(name_length));
}

ErrorOr<void> FUSEInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    u64 id = TRY(try_open(true, 0));
    u32 nodeid = identifier().index().value();

    fuse_read_in payload {};
    payload.fh = id;
    payload.size = 4096;
    while (true) {
        auto response = TRY(fs().m_connection->send_request_and_wait_for_a_reply(FUSE_READDIR, nodeid, { &payload, sizeof(payload) }));
        fuse_out_header* header = bit_cast<fuse_out_header*>(response->data());
        if (header->len == sizeof(fuse_out_header))
            break;
        char* dirents = bit_cast<char*>(response->data() + sizeof(fuse_out_header));
        u32 total_size = header->len - sizeof(fuse_out_header);
        u32 offset = 0;
        while (offset < total_size) {
            fuse_dirent* dirent = bit_cast<fuse_dirent*>(dirents + offset);
            if (dirent->ino == 0)
                break;

            if (dirent->namelen > NAME_MAX)
                return Error::from_errno(EIO);

            TRY(callback({ { dirent->name, dirent->namelen }, { fsid(), dirent->ino }, to_underlying(ram_backed_file_type_from_mode(dirent->type << 12)) }));
            offset += get_dirent_entry_length_padded(dirent->namelen);
        }
        payload.offset = offset;
    }

    TRY(try_release(id, true));
    return {};
}

ErrorOr<NonnullRefPtr<Inode>> FUSEInode::lookup(StringView name)
{
    auto name_buffer = TRY(KBuffer::try_create_with_size("FUSE: Lookup name string"sv, name.length() + 1));
    memset(name_buffer->data(), 0, name_buffer->size());
    memcpy(name_buffer->data(), name.characters_without_null_termination(), name.length());

    auto response = TRY(fs().m_connection->send_request_and_wait_for_a_reply(FUSE_LOOKUP, identifier().index().value(), name_buffer->bytes()));

    fuse_out_header* header = bit_cast<fuse_out_header*>(response->data());
    if (header->error)
        return Error::from_errno(-header->error);

    fuse_entry_out* entry = bit_cast<fuse_entry_out*>(response->data() + sizeof(fuse_out_header));

    InodeMetadata metadata {};

    metadata.inode = { fsid(), entry->nodeid };
    metadata.mode = entry->attr.mode;
    metadata.link_count = entry->attr.nlink;
    metadata.size = entry->attr.size;

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) FUSEInode(fs(), entry->nodeid, metadata)));
}

ErrorOr<void> FUSEInode::flush_metadata()
{
    return {};
}

ErrorOr<void> FUSEInode::add_child(Inode&, StringView, mode_t)
{
    return ENOTIMPL;
}

ErrorOr<NonnullRefPtr<Inode>> FUSEInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return ENOTIMPL;
}

ErrorOr<void> FUSEInode::remove_child(StringView)
{
    return ENOTIMPL;
}

ErrorOr<void> FUSEInode::replace_child(StringView, Inode&)
{
    return ENOTIMPL;
}

ErrorOr<void> FUSEInode::chmod(mode_t)
{
    return ENOTIMPL;
}

ErrorOr<void> FUSEInode::chown(UserID, GroupID)
{
    return ENOTIMPL;
}

ErrorOr<void> FUSEInode::truncate_locked(u64 new_size)
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(!is_directory());

    fuse_setattr_in setattr {};
    setattr.valid = FATTR_SIZE;
    setattr.size = new_size;

    auto response = TRY(fs().m_connection->send_request_and_wait_for_a_reply(FUSE_SETATTR, identifier().index().value(), { &setattr, sizeof(setattr) }));

    fuse_out_header* header = bit_cast<fuse_out_header*>(response->data());
    if (header->error)
        return Error::from_errno(-header->error);

    fuse_attr_out* attr = bit_cast<fuse_attr_out*>(response->data() + sizeof(fuse_out_header));
    m_metadata.size = attr->attr.size;

    return {};
}

ErrorOr<void> FUSEInode::update_timestamps(Optional<UnixDateTime> atime, Optional<UnixDateTime> ctime, Optional<UnixDateTime> mtime)
{
    MutexLocker locker(m_inode_lock);

    fuse_setattr_in setattr {};
    if (atime.has_value()) {
        m_metadata.atime = atime.value();
        setattr.valid = setattr.valid | FATTR_ATIME;
        setattr.atime = atime.value().to_timespec().tv_sec;
    }
    if (ctime.has_value()) {
        m_metadata.ctime = ctime.value();
        setattr.valid = setattr.valid | FATTR_CTIME;
        setattr.ctime = ctime.value().to_timespec().tv_sec;
    }
    if (mtime.has_value()) {
        m_metadata.mtime = mtime.value();
        setattr.valid = setattr.valid | FATTR_MTIME;
        setattr.mtime = mtime.value().to_timespec().tv_sec;
    }

    auto response = TRY(fs().m_connection->send_request_and_wait_for_a_reply(FUSE_SETATTR, identifier().index().value(), { &setattr, sizeof(setattr) }));

    fuse_out_header* header = bit_cast<fuse_out_header*>(response->data());
    if (header->error)
        return Error::from_errno(-header->error);

    return {};
}

}
