/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<OpenFileDescription>> OpenFileDescription::try_create(Custody& custody)
{
    auto inode_file = TRY(InodeFile::create(custody.inode()));
    auto description = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) OpenFileDescription(move(inode_file))));

    description->m_custody = custody;
    TRY(description->attach());
    return description;
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> OpenFileDescription::try_create(File& file)
{
    auto description = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) OpenFileDescription(file)));
    TRY(description->attach());
    return description;
}

OpenFileDescription::OpenFileDescription(File& file)
    : m_file(file)
{
    if (file.is_inode())
        m_inode = static_cast<InodeFile&>(file).inode();

    auto metadata = this->metadata();
    m_state.with([&](auto& state) { state.is_directory = metadata.is_directory(); });
}

OpenFileDescription::~OpenFileDescription()
{
    m_file->detach(*this);
    if (is_fifo())
        static_cast<FIFO*>(m_file.ptr())->detach(fifo_direction());
    // FIXME: Should this error path be observed somehow?
    (void)m_file->close();
    if (m_inode)
        m_inode->detach(*this);

    if (m_inode)
        m_inode->remove_flocks_for_description(*this);
}

ErrorOr<void> OpenFileDescription::attach()
{
    if (m_inode)
        TRY(m_inode->attach(*this));
    return m_file->attach(*this);
}

void OpenFileDescription::set_original_custody(Badge<VirtualFileSystem>, Custody& custody)
{
    m_custody = custody;
}

Thread::FileBlocker::BlockFlags OpenFileDescription::should_unblock(Thread::FileBlocker::BlockFlags block_flags) const
{
    using BlockFlags = Thread::FileBlocker::BlockFlags;
    BlockFlags unblock_flags = BlockFlags::None;
    if (has_flag(block_flags, BlockFlags::Read) && can_read())
        unblock_flags |= BlockFlags::Read;
    if (has_flag(block_flags, BlockFlags::Write) && can_write())
        unblock_flags |= BlockFlags::Write;
    // TODO: Implement Thread::FileBlocker::BlockFlags::Exception

    if (has_any_flag(block_flags, BlockFlags::SocketFlags)) {
        auto const* sock = socket();
        VERIFY(sock);
        if (has_flag(block_flags, BlockFlags::Accept) && sock->can_accept())
            unblock_flags |= BlockFlags::Accept;
        if (has_flag(block_flags, BlockFlags::Connect) && sock->setup_state() == Socket::SetupState::Completed)
            unblock_flags |= BlockFlags::Connect;
    }
    return unblock_flags;
}

ErrorOr<struct stat> OpenFileDescription::stat()
{
    // FIXME: This is due to the Device class not overriding File::stat().
    if (m_inode)
        return m_inode->metadata().stat();
    return m_file->stat();
}

ErrorOr<off_t> OpenFileDescription::seek(off_t offset, int whence)
{
    if (!m_file->is_seekable())
        return ESPIPE;

    auto metadata = this->metadata();

    auto new_offset = TRY(m_state.with([&](auto& state) -> ErrorOr<off_t> {
        off_t new_offset;
        switch (whence) {
        case SEEK_SET:
            new_offset = offset;
            break;
        case SEEK_CUR:
            if (Checked<off_t>::addition_would_overflow(state.current_offset, offset))
                return EOVERFLOW;
            new_offset = state.current_offset + offset;
            break;
        case SEEK_END:
            if (!metadata.is_valid())
                return EIO;
            if (Checked<off_t>::addition_would_overflow(metadata.size, offset))
                return EOVERFLOW;
            new_offset = metadata.size + offset;
            break;
        default:
            return EINVAL;
        }
        if (new_offset < 0)
            return EINVAL;
        state.current_offset = new_offset;
        return new_offset;
    }));

    // FIXME: Return EINVAL if attempting to seek past the end of a seekable device.

    m_file->did_seek(*this, new_offset);
    if (m_inode)
        m_inode->did_seek(*this, new_offset);
    evaluate_block_conditions();
    return new_offset;
}

ErrorOr<size_t> OpenFileDescription::read(UserOrKernelBuffer& buffer, u64 offset, size_t count)
{
    if (Checked<u64>::addition_would_overflow(offset, count))
        return EOVERFLOW;
    return m_file->read(*this, offset, buffer, count);
}

ErrorOr<size_t> OpenFileDescription::write(u64 offset, UserOrKernelBuffer const& data, size_t data_size)
{
    if (Checked<u64>::addition_would_overflow(offset, data_size))
        return EOVERFLOW;
    return m_file->write(*this, offset, data, data_size);
}

ErrorOr<size_t> OpenFileDescription::read(UserOrKernelBuffer& buffer, size_t count)
{
    auto offset = TRY(m_state.with([&](auto& state) -> ErrorOr<off_t> {
        if (Checked<off_t>::addition_would_overflow(state.current_offset, count))
            return EOVERFLOW;
        return state.current_offset;
    }));
    auto nread = TRY(m_file->read(*this, offset, buffer, count));
    if (m_file->is_seekable())
        m_state.with([&](auto& state) { state.current_offset = offset + nread; });
    evaluate_block_conditions();
    return nread;
}

ErrorOr<size_t> OpenFileDescription::write(UserOrKernelBuffer const& data, size_t size)
{
    auto offset = TRY(m_state.with([&](auto& state) -> ErrorOr<off_t> {
        if (Checked<off_t>::addition_would_overflow(state.current_offset, size))
            return EOVERFLOW;
        return state.current_offset;
    }));
    auto nwritten = TRY(m_file->write(*this, offset, data, size));

    if (m_file->is_seekable())
        m_state.with([&](auto& state) { state.current_offset = offset + nwritten; });

    evaluate_block_conditions();
    return nwritten;
}

bool OpenFileDescription::can_write() const
{
    return m_file->can_write(*this, offset());
}

bool OpenFileDescription::can_read() const
{
    return m_file->can_read(*this, offset());
}

ErrorOr<NonnullOwnPtr<KBuffer>> OpenFileDescription::read_entire_file()
{
    // HACK ALERT: (This entire function)
    VERIFY(m_file->is_inode());
    VERIFY(m_inode);
    return m_inode->read_entire(this);
}

ErrorOr<size_t> OpenFileDescription::get_dir_entries(UserOrKernelBuffer& output_buffer, size_t size)
{
    if (!is_directory())
        return ENOTDIR;

    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return EIO;

    size_t remaining = size;
    ErrorOr<void> error;
    u8 stack_buffer[PAGE_SIZE];
    Bytes temp_buffer(stack_buffer, sizeof(stack_buffer));
    OutputMemoryStream stream { temp_buffer };

    auto flush_stream_to_output_buffer = [&error, &stream, &remaining, &output_buffer]() -> bool {
        if (error.is_error())
            return false;
        if (stream.size() == 0)
            return true;
        if (remaining < stream.size()) {
            error = EINVAL;
            return false;
        }
        if (auto result = output_buffer.write(stream.bytes()); result.is_error()) {
            error = result.release_error();
            return false;
        }
        output_buffer = output_buffer.offset(stream.size());
        remaining -= stream.size();
        stream.reset();
        return true;
    };

    ErrorOr<void> result = VirtualFileSystem::the().traverse_directory_inode(*m_inode, [&flush_stream_to_output_buffer, &error, &stream, this](auto& entry) -> ErrorOr<void> {
        size_t serialized_size = sizeof(ino_t) + sizeof(u8) + sizeof(size_t) + sizeof(char) * entry.name.length();
        if (serialized_size > stream.remaining()) {
            if (!flush_stream_to_output_buffer())
                return error;
        }
        stream << (u64)entry.inode.index().value();
        stream << m_inode->fs().internal_file_type_to_directory_entry_type(entry);
        stream << (u32)entry.name.length();
        stream << entry.name.bytes();
        return {};
    });
    flush_stream_to_output_buffer();

    if (result.is_error()) {
        // We should only return EFAULT when the userspace buffer is too small,
        // so that userspace can reliably use it as a signal to increase its
        // buffer size.
        VERIFY(result.error().code() != EFAULT);
        return result.release_error();
    }

    if (error.is_error())
        return error.release_error();
    return size - remaining;
}

bool OpenFileDescription::is_device() const
{
    return m_file->is_device();
}

Device const* OpenFileDescription::device() const
{
    if (!is_device())
        return nullptr;
    return static_cast<Device const*>(m_file.ptr());
}

Device* OpenFileDescription::device()
{
    if (!is_device())
        return nullptr;
    return static_cast<Device*>(m_file.ptr());
}

bool OpenFileDescription::is_tty() const
{
    return m_file->is_tty();
}

const TTY* OpenFileDescription::tty() const
{
    if (!is_tty())
        return nullptr;
    return static_cast<const TTY*>(m_file.ptr());
}

TTY* OpenFileDescription::tty()
{
    if (!is_tty())
        return nullptr;
    return static_cast<TTY*>(m_file.ptr());
}

bool OpenFileDescription::is_inode_watcher() const
{
    return m_file->is_inode_watcher();
}

InodeWatcher const* OpenFileDescription::inode_watcher() const
{
    if (!is_inode_watcher())
        return nullptr;
    return static_cast<InodeWatcher const*>(m_file.ptr());
}

InodeWatcher* OpenFileDescription::inode_watcher()
{
    if (!is_inode_watcher())
        return nullptr;
    return static_cast<InodeWatcher*>(m_file.ptr());
}

bool OpenFileDescription::is_master_pty() const
{
    return m_file->is_master_pty();
}

MasterPTY const* OpenFileDescription::master_pty() const
{
    if (!is_master_pty())
        return nullptr;
    return static_cast<MasterPTY const*>(m_file.ptr());
}

MasterPTY* OpenFileDescription::master_pty()
{
    if (!is_master_pty())
        return nullptr;
    return static_cast<MasterPTY*>(m_file.ptr());
}

ErrorOr<void> OpenFileDescription::close()
{
    if (m_file->attach_count() > 0)
        return {};
    return m_file->close();
}

ErrorOr<NonnullOwnPtr<KString>> OpenFileDescription::original_absolute_path() const
{
    if (!m_custody)
        return ENOENT;
    return m_custody->try_serialize_absolute_path();
}

ErrorOr<NonnullOwnPtr<KString>> OpenFileDescription::pseudo_path() const
{
    if (m_custody)
        return m_custody->try_serialize_absolute_path();
    return m_file->pseudo_path(*this);
}

InodeMetadata OpenFileDescription::metadata() const
{
    if (m_inode)
        return m_inode->metadata();
    return {};
}

ErrorOr<Memory::Region*> OpenFileDescription::mmap(Process& process, Memory::VirtualRange const& range, u64 offset, int prot, bool shared)
{
    return m_file->mmap(process, *this, range, offset, prot, shared);
}

ErrorOr<void> OpenFileDescription::truncate(u64 length)
{
    return m_file->truncate(length);
}

ErrorOr<void> OpenFileDescription::sync()
{
    return m_file->sync();
}

bool OpenFileDescription::is_fifo() const
{
    return m_file->is_fifo();
}

FIFO* OpenFileDescription::fifo()
{
    if (!is_fifo())
        return nullptr;
    return static_cast<FIFO*>(m_file.ptr());
}

bool OpenFileDescription::is_socket() const
{
    return m_file->is_socket();
}

Socket* OpenFileDescription::socket()
{
    if (!is_socket())
        return nullptr;
    return static_cast<Socket*>(m_file.ptr());
}

Socket const* OpenFileDescription::socket() const
{
    if (!is_socket())
        return nullptr;
    return static_cast<Socket const*>(m_file.ptr());
}

void OpenFileDescription::set_file_flags(u32 flags)
{
    m_state.with([&](auto& state) {
        state.is_blocking = !(flags & O_NONBLOCK);
        state.should_append = flags & O_APPEND;
        state.direct = flags & O_DIRECT;
        state.file_flags = flags;
    });
}

ErrorOr<void> OpenFileDescription::chmod(mode_t mode)
{
    return m_file->chmod(*this, mode);
}

ErrorOr<void> OpenFileDescription::chown(UserID uid, GroupID gid)
{
    return m_file->chown(*this, uid, gid);
}

FileBlockerSet& OpenFileDescription::blocker_set()
{
    return m_file->blocker_set();
}

ErrorOr<void> OpenFileDescription::apply_flock(Process const& process, Userspace<flock const*> lock)
{
    if (!m_inode)
        return EBADF;

    return m_inode->apply_flock(process, *this, lock);
}

ErrorOr<void> OpenFileDescription::get_flock(Userspace<flock*> lock) const
{
    if (!m_inode)
        return EBADF;

    return m_inode->get_flock(*this, lock);
}
bool OpenFileDescription::is_readable() const
{
    return m_state.with([](auto& state) { return state.readable; });
}

bool OpenFileDescription::is_writable() const
{
    return m_state.with([](auto& state) { return state.writable; });
}

void OpenFileDescription::set_readable(bool b)
{
    m_state.with([&](auto& state) { state.readable = b; });
}

void OpenFileDescription::set_writable(bool b)
{
    m_state.with([&](auto& state) { state.writable = b; });
}

void OpenFileDescription::set_rw_mode(int options)
{
    m_state.with([&](auto& state) {
        state.readable = (options & O_RDONLY) == O_RDONLY;
        state.writable = (options & O_WRONLY) == O_WRONLY;
    });
}

bool OpenFileDescription::is_direct() const
{
    return m_state.with([](auto& state) { return state.direct; });
}

bool OpenFileDescription::is_directory() const
{
    return m_state.with([](auto& state) { return state.is_directory; });
}

bool OpenFileDescription::is_blocking() const
{
    return m_state.with([](auto& state) { return state.is_blocking; });
}

void OpenFileDescription::set_blocking(bool b)
{
    m_state.with([&](auto& state) { state.is_blocking = b; });
}

bool OpenFileDescription::should_append() const
{
    return m_state.with([](auto& state) { return state.should_append; });
}

u32 OpenFileDescription::file_flags() const
{
    return m_state.with([](auto& state) { return state.file_flags; });
}

FIFO::Direction OpenFileDescription::fifo_direction() const
{
    return m_state.with([](auto& state) { return state.fifo_direction; });
}

void OpenFileDescription::set_fifo_direction(Badge<FIFO>, FIFO::Direction direction)
{
    m_state.with([&](auto& state) { state.fifo_direction = direction; });
}

OwnPtr<OpenFileDescriptionData>& OpenFileDescription::data()
{
    return m_state.with([](auto& state) -> OwnPtr<OpenFileDescriptionData>& { return state.data; });
}

off_t OpenFileDescription::offset() const
{
    return m_state.with([](auto& state) { return state.current_offset; });
}

}
