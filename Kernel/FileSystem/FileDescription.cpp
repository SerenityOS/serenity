/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Debug.h>
#include <AK/MemoryStream.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeFile.h>
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
    auto description = adopt(*new FileDescription(InodeFile::create(custody.inode())));
    description->m_custody = custody;
    auto result = description->attach();
    if (result.is_error()) {
        dbgln<FILEDESCRIPTION_DEBUG>("Failed to create file description for custody: {}", result);
        return result;
    }
    return description;
}

KResultOr<NonnullRefPtr<FileDescription>> FileDescription::create(File& file)
{
    auto description = adopt(*new FileDescription(file));
    auto result = description->attach();
    if (result.is_error()) {
        dbgln<FILEDESCRIPTION_DEBUG>("Failed to create file description for file: {}", result);
        return result;
    }
    return description;
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
    u32 unblock_flags = (u32)Thread::FileBlocker::BlockFlags::None;
    if (((u32)block_flags & (u32)Thread::FileBlocker::BlockFlags::Read) && can_read())
        unblock_flags |= (u32)Thread::FileBlocker::BlockFlags::Read;
    if (((u32)block_flags & (u32)Thread::FileBlocker::BlockFlags::Write) && can_write())
        unblock_flags |= (u32)Thread::FileBlocker::BlockFlags::Write;
    // TODO: Implement Thread::FileBlocker::BlockFlags::Exception

    if ((u32)block_flags & (u32)Thread::FileBlocker::BlockFlags::SocketFlags) {
        auto* sock = socket();
        ASSERT(sock);
        if (((u32)block_flags & (u32)Thread::FileBlocker::BlockFlags::Accept) && sock->can_accept())
            unblock_flags |= (u32)Thread::FileBlocker::BlockFlags::Accept;
        if (((u32)block_flags & (u32)Thread::FileBlocker::BlockFlags::Connect) && sock->setup_state() == Socket::SetupState::Completed)
            unblock_flags |= (u32)Thread::FileBlocker::BlockFlags::Connect;
    }
    return (Thread::FileBlocker::BlockFlags)unblock_flags;
}

KResult FileDescription::stat(::stat& buffer)
{
    LOCKER(m_lock);
    // FIXME: This is a little awkward, why can't we always forward to File::stat()?
    if (m_inode)
        return metadata().stat(buffer);
    return m_file->stat(buffer);
}

off_t FileDescription::seek(off_t offset, int whence)
{
    LOCKER(m_lock);
    if (!m_file->is_seekable())
        return -ESPIPE;

    off_t new_offset;

    switch (whence) {
    case SEEK_SET:
        new_offset = offset;
        break;
    case SEEK_CUR:
        new_offset = m_current_offset + offset;
        break;
    case SEEK_END:
        if (!metadata().is_valid())
            return -EIO;
        new_offset = metadata().size;
        break;
    default:
        return -EINVAL;
    }

    if (new_offset < 0)
        return -EINVAL;
    // FIXME: Return -EINVAL if attempting to seek past the end of a seekable device.

    m_current_offset = new_offset;

    m_file->did_seek(*this, new_offset);
    if (m_inode)
        m_inode->did_seek(*this, new_offset);
    evaluate_block_conditions();
    return m_current_offset;
}

KResultOr<size_t> FileDescription::read(UserOrKernelBuffer& buffer, size_t count)
{
    LOCKER(m_lock);
    Checked<size_t> new_offset = m_current_offset;
    new_offset += count;
    if (new_offset.has_overflow())
        return -EOVERFLOW;
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
    LOCKER(m_lock);
    Checked<size_t> new_offset = m_current_offset;
    new_offset += size;
    if (new_offset.has_overflow())
        return -EOVERFLOW;
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
    ASSERT(m_file->is_inode());
    ASSERT(m_inode);
    return m_inode->read_entire(this);
}

ssize_t FileDescription::get_dir_entries(UserOrKernelBuffer& buffer, ssize_t size)
{
    LOCKER(m_lock, Lock::Mode::Shared);
    if (!is_directory())
        return -ENOTDIR;

    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return -EIO;

    if (size < 0)
        return -EINVAL;

    size_t size_to_allocate = max(static_cast<size_t>(PAGE_SIZE), static_cast<size_t>(metadata.size));

    auto temp_buffer = ByteBuffer::create_uninitialized(size_to_allocate);
    OutputMemoryStream stream { temp_buffer };

    KResult result = VFS::the().traverse_directory_inode(*m_inode, [&stream, this](auto& entry) {
        stream << (u32)entry.inode.index();
        stream << m_inode->fs().internal_file_type_to_directory_entry_type(entry);
        stream << (u32)entry.name.length();
        stream << entry.name.bytes();
        return true;
    });

    if (result.is_error())
        return result;

    if (stream.handle_recoverable_error())
        return -EINVAL;

    if (!buffer.write(stream.bytes()))
        return -EFAULT;

    return stream.size();
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
    if (m_file->ref_count() > 1)
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

KResultOr<Region*> FileDescription::mmap(Process& process, const Range& range, size_t offset, int prot, bool shared)
{
    LOCKER(m_lock);
    return m_file->mmap(process, *this, range, offset, prot, shared);
}

KResult FileDescription::truncate(u64 length)
{
    LOCKER(m_lock);
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
    LOCKER(m_lock);
    m_is_blocking = !(flags & O_NONBLOCK);
    m_should_append = flags & O_APPEND;
    m_direct = flags & O_DIRECT;
    m_file_flags = flags;
}

KResult FileDescription::chmod(mode_t mode)
{
    LOCKER(m_lock);
    return m_file->chmod(*this, mode);
}

KResult FileDescription::chown(uid_t uid, gid_t gid)
{
    LOCKER(m_lock);
    return m_file->chown(*this, uid, gid);
}

FileBlockCondition& FileDescription::block_condition()
{
    return m_file->block_condition();
}

}
