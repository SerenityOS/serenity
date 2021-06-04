/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

KResultOr<NonnullRefPtr<FileDescription>> FileDescription::create(Custody& custody)
{
    auto inode_file = InodeFile::create(custody.inode());
    if (inode_file.is_error())
        return inode_file.error();

    auto description = adopt_ref_if_nonnull(new FileDescription(*inode_file.release_value()));
    if (!description)
        return ENOMEM;

    description->m_custody = custody;
    auto result = description->attach();
    if (result.is_error()) {
        dbgln_if(FILEDESCRIPTION_DEBUG, "Failed to create file description for custody: {}", result);
        return result;
    }
    return description.release_nonnull();
}

KResultOr<NonnullRefPtr<FileDescription>> FileDescription::create(File& file)
{
    auto description = adopt_ref_if_nonnull(new FileDescription(file));
    if (!description)
        return ENOMEM;
    auto result = description->attach();
    if (result.is_error()) {
        dbgln_if(FILEDESCRIPTION_DEBUG, "Failed to create file description for file: {}", result);
        return result;
    }
    return description.release_nonnull();
}

FileDescription::FileDescription(File& file)
    : m_file(file)
{
    if (file.is_inode())
        m_inode = static_cast<InodeFile&>(file).inode();

    m_is_directory = metadata().is_directory();
}

FileDescription::~FileDescription()
{
    m_file->detach(*this);
    if (is_fifo())
        static_cast<FIFO*>(m_file.ptr())->detach(m_fifo_direction);
    // FIXME: Should this error path be observed somehow?
    (void)m_file->close();
    if (m_inode)
        m_inode->detach(*this);
}

KResult FileDescription::attach()
{
    if (m_inode) {
        auto result = m_inode->attach(*this);
        if (result.is_error())
            return result;
    }
    return m_file->attach(*this);
}

Thread::FileBlocker::BlockFlags FileDescription::should_unblock(Thread::FileBlocker::BlockFlags block_flags) const
{
    using BlockFlags = Thread::FileBlocker::BlockFlags;
    BlockFlags unblock_flags = BlockFlags::None;
    if (has_flag(block_flags, BlockFlags::Read) && can_read())
        unblock_flags |= BlockFlags::Read;
    if (has_flag(block_flags, BlockFlags::Write) && can_write())
        unblock_flags |= BlockFlags::Write;
    // TODO: Implement Thread::FileBlocker::BlockFlags::Exception

    if (has_flag(block_flags, BlockFlags::SocketFlags)) {
        auto* sock = socket();
        VERIFY(sock);
        if (has_flag(block_flags, BlockFlags::Accept) && sock->can_accept())
            unblock_flags |= BlockFlags::Accept;
        if (has_flag(block_flags, BlockFlags::Connect) && sock->setup_state() == Socket::SetupState::Completed)
            unblock_flags |= BlockFlags::Connect;
    }
    return unblock_flags;
}

KResult FileDescription::stat(::stat& buffer)
{
    Locker locker(m_lock);
    // FIXME: This is a little awkward, why can't we always forward to File::stat()?
    if (m_inode)
        return metadata().stat(buffer);
    return m_file->stat(buffer);
}

KResultOr<off_t> FileDescription::seek(off_t offset, int whence)
{
    Locker locker(m_lock);
    if (!m_file->is_seekable())
        return ESPIPE;

    off_t new_offset;

    switch (whence) {
    case SEEK_SET:
        new_offset = offset;
        break;
    case SEEK_CUR:
        if (Checked<off_t>::addition_would_overflow(m_current_offset, offset))
            return EOVERFLOW;
        new_offset = m_current_offset + offset;
        break;
    case SEEK_END:
        if (!metadata().is_valid())
            return EIO;
        if (Checked<off_t>::addition_would_overflow(metadata().size, offset))
            return EOVERFLOW;
        new_offset = metadata().size + offset;
        break;
    default:
        return EINVAL;
    }

    if (new_offset < 0)
        return EINVAL;
    // FIXME: Return EINVAL if attempting to seek past the end of a seekable device.

    m_current_offset = new_offset;

    m_file->did_seek(*this, new_offset);
    if (m_inode)
        m_inode->did_seek(*this, new_offset);
    evaluate_block_conditions();
    return m_current_offset;
}

KResultOr<size_t> FileDescription::read(UserOrKernelBuffer& buffer, size_t count)
{
    Locker locker(m_lock);
    if (Checked<off_t>::addition_would_overflow(m_current_offset, count))
        return EOVERFLOW;
    auto nread_or_error = m_file->read(*this, offset(), buffer, count);
    if (!nread_or_error.is_error()) {
        if (m_file->is_seekable())
            m_current_offset += nread_or_error.value();
        evaluate_block_conditions();
    }
    return nread_or_error;
}

KResultOr<size_t> FileDescription::write(const UserOrKernelBuffer& data, size_t size)
{
    Locker locker(m_lock);
    if (Checked<off_t>::addition_would_overflow(m_current_offset, size))
        return EOVERFLOW;
    auto nwritten_or_error = m_file->write(*this, offset(), data, size);
    if (!nwritten_or_error.is_error()) {
        if (m_file->is_seekable())
            m_current_offset += nwritten_or_error.value();
        evaluate_block_conditions();
    }
    return nwritten_or_error;
}

bool FileDescription::can_write() const
{
    return m_file->can_write(*this, offset());
}

bool FileDescription::can_read() const
{
    return m_file->can_read(*this, offset());
}

KResultOr<NonnullOwnPtr<KBuffer>> FileDescription::read_entire_file()
{
    // HACK ALERT: (This entire function)
    VERIFY(m_file->is_inode());
    VERIFY(m_inode);
    return m_inode->read_entire(this);
}

KResultOr<ssize_t> FileDescription::get_dir_entries(UserOrKernelBuffer& output_buffer, ssize_t size)
{
    Locker locker(m_lock, Lock::Mode::Shared);
    if (!is_directory())
        return ENOTDIR;

    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return EIO;

    if (size < 0)
        return EINVAL;

    size_t remaining = size;
    KResult error = KSuccess;
    u8 stack_buffer[PAGE_SIZE];
    Bytes temp_buffer(stack_buffer, sizeof(stack_buffer));
    OutputMemoryStream stream { temp_buffer };

    auto flush_stream_to_output_buffer = [&error, &stream, &remaining, &output_buffer]() -> bool {
        if (error != 0)
            return false;
        if (stream.size() == 0)
            return true;
        if (remaining < stream.size()) {
            error = EINVAL;
            return false;
        } else if (!output_buffer.write(stream.bytes())) {
            error = EFAULT;
            return false;
        }
        output_buffer = output_buffer.offset(stream.size());
        remaining -= stream.size();
        stream.reset();
        return true;
    };

    KResult result = VFS::the().traverse_directory_inode(*m_inode, [&flush_stream_to_output_buffer, &error, &remaining, &output_buffer, &stream, this](auto& entry) {
        size_t serialized_size = sizeof(ino_t) + sizeof(u8) + sizeof(size_t) + sizeof(char) * entry.name.length();
        if (serialized_size > stream.remaining()) {
            if (!flush_stream_to_output_buffer()) {
                return false;
            }
        }
        stream << (u32)entry.inode.index().value();
        stream << m_inode->fs().internal_file_type_to_directory_entry_type(entry);
        stream << (u32)entry.name.length();
        stream << entry.name.bytes();
        return true;
    });
    flush_stream_to_output_buffer();

    if (result.is_error()) {
        // We should only return -EFAULT when the userspace buffer is too small,
        // so that userspace can reliably use it as a signal to increase its
        // buffer size.
        VERIFY(result != -EFAULT);
        return result;
    }

    if (error) {
        return error;
    }
    return size - remaining;
}

bool FileDescription::is_device() const
{
    return m_file->is_device();
}

const Device* FileDescription::device() const
{
    if (!is_device())
        return nullptr;
    return static_cast<const Device*>(m_file.ptr());
}

Device* FileDescription::device()
{
    if (!is_device())
        return nullptr;
    return static_cast<Device*>(m_file.ptr());
}

bool FileDescription::is_tty() const
{
    return m_file->is_tty();
}

const TTY* FileDescription::tty() const
{
    if (!is_tty())
        return nullptr;
    return static_cast<const TTY*>(m_file.ptr());
}

TTY* FileDescription::tty()
{
    if (!is_tty())
        return nullptr;
    return static_cast<TTY*>(m_file.ptr());
}

bool FileDescription::is_inode_watcher() const
{
    return m_file->is_inode_watcher();
}

const InodeWatcher* FileDescription::inode_watcher() const
{
    if (!is_inode_watcher())
        return nullptr;
    return static_cast<const InodeWatcher*>(m_file.ptr());
}

InodeWatcher* FileDescription::inode_watcher()
{
    if (!is_inode_watcher())
        return nullptr;
    return static_cast<InodeWatcher*>(m_file.ptr());
}

bool FileDescription::is_master_pty() const
{
    return m_file->is_master_pty();
}

const MasterPTY* FileDescription::master_pty() const
{
    if (!is_master_pty())
        return nullptr;
    return static_cast<const MasterPTY*>(m_file.ptr());
}

MasterPTY* FileDescription::master_pty()
{
    if (!is_master_pty())
        return nullptr;
    return static_cast<MasterPTY*>(m_file.ptr());
}

KResult FileDescription::close()
{
    if (m_file->attach_count() > 0)
        return KSuccess;
    return m_file->close();
}

String FileDescription::absolute_path() const
{
    if (m_custody)
        return m_custody->absolute_path();
    return m_file->absolute_path(*this);
}

InodeMetadata FileDescription::metadata() const
{
    if (m_inode)
        return m_inode->metadata();
    return {};
}

KResultOr<Region*> FileDescription::mmap(Process& process, const Range& range, u64 offset, int prot, bool shared)
{
    Locker locker(m_lock);
    return m_file->mmap(process, *this, range, offset, prot, shared);
}

KResult FileDescription::truncate(u64 length)
{
    Locker locker(m_lock);
    return m_file->truncate(length);
}

bool FileDescription::is_fifo() const
{
    return m_file->is_fifo();
}

FIFO* FileDescription::fifo()
{
    if (!is_fifo())
        return nullptr;
    return static_cast<FIFO*>(m_file.ptr());
}

bool FileDescription::is_socket() const
{
    return m_file->is_socket();
}

Socket* FileDescription::socket()
{
    if (!is_socket())
        return nullptr;
    return static_cast<Socket*>(m_file.ptr());
}

const Socket* FileDescription::socket() const
{
    if (!is_socket())
        return nullptr;
    return static_cast<const Socket*>(m_file.ptr());
}

void FileDescription::set_file_flags(u32 flags)
{
    Locker locker(m_lock);
    m_is_blocking = !(flags & O_NONBLOCK);
    m_should_append = flags & O_APPEND;
    m_direct = flags & O_DIRECT;
    m_file_flags = flags;
}

KResult FileDescription::chmod(mode_t mode)
{
    Locker locker(m_lock);
    return m_file->chmod(*this, mode);
}

KResult FileDescription::chown(uid_t uid, gid_t gid)
{
    Locker locker(m_lock);
    return m_file->chown(*this, uid, gid);
}

FileBlockCondition& FileDescription::block_condition()
{
    return m_file->block_condition();
}

}
