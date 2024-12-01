/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Plan9FS/Inode.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

Plan9FSInode::Plan9FSInode(Plan9FS& fs, u32 fid)
    : Inode(fs, fid)
{
}

ErrorOr<NonnullRefPtr<Plan9FSInode>> Plan9FSInode::try_create(Plan9FS& fs, u32 fid)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) Plan9FSInode(fs, fid));
}

Plan9FSInode::~Plan9FSInode()
{
    Plan9FSMessage clunk_request { fs(), Plan9FSMessage::Type::Tclunk };
    clunk_request << fid();
    // FIXME: Should we observe this  error somehow?
    [[maybe_unused]] auto rc = fs().post_message_and_explicitly_ignore_reply(clunk_request);
}

ErrorOr<void> Plan9FSInode::ensure_open_for_mode(int mode)
{
    bool use_lopen = fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L;
    u32 l_mode = 0;
    u8 p9_mode = 0;

    {
        MutexLocker locker(m_inode_lock);

        // If it's already open in this mode, we're done.
        if ((m_open_mode & mode) == mode)
            return {};

        m_open_mode |= mode;

        if ((m_open_mode & O_RDWR) == O_RDWR) {
            l_mode |= 2;
            p9_mode |= 2;
        } else if (m_open_mode & O_WRONLY) {
            l_mode |= 1;
            p9_mode |= 1;
        } else if (m_open_mode & O_RDONLY) {
            // Leave the values at 0.
        }
    }

    if (use_lopen) {
        Plan9FSMessage message { fs(), Plan9FSMessage::Type::Tlopen };
        message << fid() << l_mode;
        return fs().post_message_and_wait_for_a_reply(message);
    }

    Plan9FSMessage message { fs(), Plan9FSMessage::Type::Topen };
    message << fid() << p9_mode;
    return fs().post_message_and_wait_for_a_reply(message);
}

ErrorOr<size_t> Plan9FSInode::read_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    TRY(const_cast<Plan9FSInode&>(*this).ensure_open_for_mode(O_RDONLY));

    size = fs().adjust_buffer_size(size);

    Plan9FSMessage message { fs(), Plan9FSMessage::Type::Treadlink };
    StringView data;

    // Try readlink first.
    bool readlink_succeeded = false;
    if (fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L && offset == 0) {
        message << fid();
        if (auto result = fs().post_message_and_wait_for_a_reply(message); !result.is_error()) {
            readlink_succeeded = true;
            message >> data;
        }
    }

    if (!readlink_succeeded) {
        message = Plan9FSMessage { fs(), Plan9FSMessage::Type::Tread };
        message << fid() << (u64)offset << (u32)size;
        TRY(fs().post_message_and_wait_for_a_reply(message));
        data = message.read_data();
    }

    // Guard against the server returning more data than requested.
    size_t nread = min(data.length(), size);
    TRY(buffer.write(data.characters_without_null_termination(), nread));
    return nread;
}

ErrorOr<size_t> Plan9FSInode::write_bytes_locked(off_t offset, size_t size, UserOrKernelBuffer const& data, OpenFileDescription*)
{
    TRY(ensure_open_for_mode(O_WRONLY));
    size = fs().adjust_buffer_size(size);

    auto data_copy = TRY(data.try_copy_into_kstring(size)); // FIXME: this seems ugly

    Plan9FSMessage message { fs(), Plan9FSMessage::Type::Twrite };
    message << fid() << (u64)offset;
    TRY(message.append_data(data_copy->view()));
    TRY(fs().post_message_and_wait_for_a_reply(message));

    u32 nwritten;
    message >> nwritten;
    return nwritten;
}

InodeMetadata Plan9FSInode::metadata() const
{
    InodeMetadata metadata;
    metadata.inode = identifier();

    // 9P2000.L; TODO: 9P2000 & 9P2000.u
    Plan9FSMessage message { fs(), Plan9FSMessage::Type::Tgetattr };
    message << fid() << (u64)GetAttrMask::Basic;
    auto result = fs().post_message_and_wait_for_a_reply(message);
    if (result.is_error()) {
        // Just return blank metadata; hopefully that's enough to result in an
        // error at some upper layer. Ideally, there would be a way for
        // Inode::metadata() to return failure.
        return metadata;
    }

    u64 valid;
    Plan9FSQIdentifier qid;
    u32 mode;
    u32 uid;
    u32 gid;
    u64 nlink;
    u64 rdev;
    u64 size;
    u64 blksize;
    u64 blocks;
    message >> valid >> qid >> mode >> uid >> gid >> nlink >> rdev >> size >> blksize >> blocks;
    // TODO: times...

    if (valid & (u64)GetAttrMask::Mode)
        metadata.mode = mode;
    if (valid & (u64)GetAttrMask::NLink)
        metadata.link_count = nlink;

#if 0
    // FIXME: Map UID/GID somehow? Or what do we do?
    if (valid & (u64)GetAttrMask::UID)
        metadata.uid = uid;
    if (valid & (u64)GetAttrMask::GID)
        metadata.uid = gid;
    // FIXME: What about device nodes?
    if (valid & (u64)GetAttrMask::RDev)
        metadata.encoded_device = 0; // TODO
#endif

    if (valid & (u64)GetAttrMask::Size)
        metadata.size = size;
    if (valid & (u64)GetAttrMask::Blocks) {
        metadata.block_size = blksize;
        metadata.block_count = blocks;
    }

    return metadata;
}

ErrorOr<void> Plan9FSInode::flush_metadata()
{
    // Do nothing.
    return {};
}

ErrorOr<void> Plan9FSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    // TODO: Should we synthesize "." and ".." here?

    if (fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L) {
        // Start by cloning the fid and opening it.
        auto clone_fid = fs().allocate_fid();
        {
            Plan9FSMessage clone_message { fs(), Plan9FSMessage::Type::Twalk };
            clone_message << fid() << clone_fid << (u16)0;
            TRY(fs().post_message_and_wait_for_a_reply(clone_message));
            Plan9FSMessage open_message { fs(), Plan9FSMessage::Type::Tlopen };
            open_message << clone_fid << (u32)0;
            auto result = fs().post_message_and_wait_for_a_reply(open_message);
            if (result.is_error()) {
                Plan9FSMessage close_message { fs(), Plan9FSMessage::Type::Tclunk };
                close_message << clone_fid;
                // FIXME: Should we observe this error?
                [[maybe_unused]] auto rc = fs().post_message_and_explicitly_ignore_reply(close_message);
                return result;
            }
        }

        u64 offset = 0;
        u32 count = fs().adjust_buffer_size(8 * MiB);
        ErrorOr<void> result;

        while (true) {
            Plan9FSMessage message { fs(), Plan9FSMessage::Type::Treaddir };
            message << clone_fid << offset << count;
            result = fs().post_message_and_wait_for_a_reply(message);
            if (result.is_error())
                break;

            StringView data = message.read_data();
            if (data.is_empty()) {
                // We've reached the end.
                break;
            }

            for (Plan9FSMessage::Decoder decoder { data }; decoder.has_more_data();) {
                Plan9FSQIdentifier qid;
                u8 type;
                StringView name;
                decoder >> qid >> offset >> type >> name;
                result = callback({ name, { fsid(), fs().allocate_fid() }, 0 });
                if (result.is_error())
                    break;
            }

            if (result.is_error())
                break;
        }

        Plan9FSMessage close_message { fs(), Plan9FSMessage::Type::Tclunk };
        close_message << clone_fid;
        // FIXME: Should we observe this error?
        [[maybe_unused]] auto rc = fs().post_message_and_explicitly_ignore_reply(close_message);
        return result;
    }

    // TODO
    return ENOTIMPL;
}

ErrorOr<NonnullRefPtr<Inode>> Plan9FSInode::lookup(StringView name)
{
    u32 newfid = fs().allocate_fid();
    Plan9FSMessage message { fs(), Plan9FSMessage::Type::Twalk };
    message << fid() << newfid << (u16)1 << name;
    TRY(fs().post_message_and_wait_for_a_reply(message));
    return TRY(Plan9FSInode::try_create(fs(), newfid));
}

ErrorOr<NonnullRefPtr<Inode>> Plan9FSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::add_child(Inode&, StringView, mode_t)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::remove_child(StringView)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::chmod(mode_t)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::chown(UserID, GroupID)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::truncate_locked(u64 new_size)
{
    VERIFY(m_inode_lock.is_locked());
    if (fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L) {
        Plan9FSMessage message { fs(), Plan9FSMessage::Type::Tsetattr };
        SetAttrMask valid = SetAttrMask::Size;
        u32 mode = 0;
        u32 uid = 0;
        u32 gid = 0;
        u64 atime_sec = 0;
        u64 atime_nsec = 0;
        u64 mtime_sec = 0;
        u64 mtime_nsec = 0;
        message << fid() << (u64)valid << mode << uid << gid << new_size << atime_sec << atime_nsec << mtime_sec << mtime_nsec;
        return fs().post_message_and_wait_for_a_reply(message);
    }

    // TODO: wstat version
    return {};
}

}
