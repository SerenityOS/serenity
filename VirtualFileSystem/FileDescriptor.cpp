#include "FileDescriptor.h"
#include "FileSystem.h"
#include "CharacterDevice.h"
#include <LibC/errno_numbers.h>
#include "UnixTypes.h"
#include <AK/BufferStream.h>
#include "FIFO.h"
#include "TTY.h"
#include "MasterPTY.h"

RetainPtr<FileDescriptor> FileDescriptor::create(RetainPtr<Inode>&& inode)
{
    return adopt(*new FileDescriptor(move(inode)));
}

RetainPtr<FileDescriptor> FileDescriptor::create(RetainPtr<CharacterDevice>&& device)
{
    return adopt(*new FileDescriptor(move(device)));
}

RetainPtr<FileDescriptor> FileDescriptor::create_pipe_writer(FIFO& fifo)
{
    return adopt(*new FileDescriptor(fifo, FIFO::Writer));
}

RetainPtr<FileDescriptor> FileDescriptor::create_pipe_reader(FIFO& fifo)
{
    return adopt(*new FileDescriptor(fifo, FIFO::Reader));
}

FileDescriptor::FileDescriptor(RetainPtr<Inode>&& inode)
    : m_inode(move(inode))
{
}

FileDescriptor::FileDescriptor(RetainPtr<CharacterDevice>&& device)
    : m_device(move(device))
{
}

FileDescriptor::~FileDescriptor()
{
    if (m_fifo)
        m_fifo->close(fifo_direction());
}

RetainPtr<FileDescriptor> FileDescriptor::clone()
{
    RetainPtr<FileDescriptor> descriptor;
    if (is_fifo()) {
        descriptor = fifo_direction() == FIFO::Reader
            ? FileDescriptor::create_pipe_reader(*m_fifo)
            : FileDescriptor::create_pipe_writer(*m_fifo);
    } else {
        if (m_inode)
            descriptor = FileDescriptor::create(m_inode.copyRef());
        else {
            descriptor = FileDescriptor::create(m_device.copyRef());
        }
    }
    if (!descriptor)
        return nullptr;
    descriptor->m_current_offset = m_current_offset;
    descriptor->m_is_blocking = m_is_blocking;
    descriptor->m_file_flags = m_file_flags;
    return descriptor;
}

bool additionWouldOverflow(Unix::off_t a, Unix::off_t b)
{
    ASSERT(a > 0);
    uint64_t ua = a;
    return (ua + b) > maxFileOffset;
}

int FileDescriptor::stat(Unix::stat* buffer)
{
    ASSERT(!is_fifo());
    if (!m_inode && !m_device)
        return -EBADF;

    auto metadata = this->metadata();
    if (!metadata.isValid())
        return -EIO;

    buffer->st_dev = 0; // FIXME
    buffer->st_ino = metadata.inode.index();
    buffer->st_mode = metadata.mode;
    buffer->st_nlink = metadata.linkCount;
    buffer->st_uid = metadata.uid;
    buffer->st_gid = metadata.gid;
    buffer->st_rdev = 0; // FIXME
    buffer->st_size = metadata.size;
    buffer->st_blksize = metadata.blockSize;
    buffer->st_blocks = metadata.blockCount;
    buffer->st_atime = metadata.atime;
    buffer->st_mtime = metadata.mtime;
    buffer->st_ctime = metadata.ctime;
    return 0;
}

Unix::off_t FileDescriptor::seek(Unix::off_t offset, int whence)
{
    ASSERT(!is_fifo());
    if (!m_inode && !m_device)
        return -EBADF;

    // FIXME: The file type should be cached on the vnode.
    //        It's silly that we have to do a full metadata lookup here.
    auto metadata = this->metadata();
    if (!metadata.isValid())
        return -EIO;

    if (metadata.isSocket() || metadata.isFIFO())
        return -ESPIPE;

    Unix::off_t newOffset;

    switch (whence) {
    case SEEK_SET:
        newOffset = offset;
        break;
    case SEEK_CUR:
        newOffset = m_current_offset + offset;
#ifndef SERENITY
        if (additionWouldOverflow(m_currentOffset, offset))
            return -EOVERFLOW;
#endif
        if (newOffset < 0)
            return -EINVAL;
        break;
    case SEEK_END:
        ASSERT(metadata.size); // FIXME: What do I do?
        newOffset = metadata.size;
        break;
    default:
        return -EINVAL;
    }

    m_current_offset = newOffset;
    return m_current_offset;
}

ssize_t FileDescriptor::read(Process& process, byte* buffer, size_t count)
{
    if (is_fifo()) {
        ASSERT(fifo_direction() == FIFO::Reader);
        return m_fifo->read(buffer, count);
    }
    if (m_device) {
        // FIXME: What should happen to m_currentOffset?
        return m_device->read(process, buffer, count);
    }
    ASSERT(inode());
    ssize_t nread = inode()->read_bytes(m_current_offset, count, buffer, this);
    m_current_offset += nread;
    return nread;
}

ssize_t FileDescriptor::write(Process& process, const byte* data, size_t size)
{
    if (is_fifo()) {
        ASSERT(fifo_direction() == FIFO::Writer);
        return m_fifo->write(data, size);
    }
    if (m_device) {
        // FIXME: What should happen to m_currentOffset?
        return m_device->write(process, data, size);
    }
    ASSERT(m_inode);
    ssize_t nwritten = m_inode->write_bytes(m_current_offset, size, data, this);
    m_current_offset += nwritten;
    return nwritten;
}

bool FileDescriptor::can_write(Process& process)
{
    if (is_fifo()) {
        ASSERT(fifo_direction() == FIFO::Writer);
        return m_fifo->can_write();
    }
    if (m_device)
        return m_device->can_write(process);
    return true;
}

bool FileDescriptor::can_read(Process& process)
{
    if (is_fifo()) {
        ASSERT(fifo_direction() == FIFO::Reader);
        return m_fifo->can_read();
    }
    if (m_device)
        return m_device->can_read(process);
    return true;
}

ByteBuffer FileDescriptor::read_entire_file(Process& process)
{
    ASSERT(!is_fifo());

    if (m_device) {
        auto buffer = ByteBuffer::create_uninitialized(1024);
        ssize_t nread = m_device->read(process, buffer.pointer(), buffer.size());
        buffer.trim(nread);
        return buffer;
    }

    ASSERT(m_inode);
    return m_inode->read_entire(this);
}

bool FileDescriptor::is_directory() const
{
    ASSERT(!is_fifo());
    return metadata().isDirectory();
}

ssize_t FileDescriptor::get_dir_entries(byte* buffer, size_t size)
{
    auto metadata = this->metadata();
    if (!metadata.isValid())
        return -EIO;
    if (!metadata.isDirectory())
        return -ENOTDIR;

    // FIXME: Compute the actual size needed.
    auto tempBuffer = ByteBuffer::create_uninitialized(2048);
    BufferStream stream(tempBuffer);
    VFS::the().traverse_directory_inode(*m_inode, [&stream] (auto& entry) {
        stream << (dword)entry.inode.index();
        stream << (byte)entry.fileType;
        stream << (dword)entry.name_length;
        stream << entry.name;
        return true;
    });

    if (size < stream.offset())
        return -1;

    memcpy(buffer, tempBuffer.pointer(), stream.offset());
    return stream.offset();
}

bool FileDescriptor::is_tty() const
{
    return m_device && m_device->is_tty();
}

const TTY* FileDescriptor::tty() const
{
    if (!is_tty())
        return nullptr;
    return static_cast<const TTY*>(m_device.ptr());
}

TTY* FileDescriptor::tty()
{
    if (!is_tty())
        return nullptr;
    return static_cast<TTY*>(m_device.ptr());
}

bool FileDescriptor::is_master_pty() const
{
    if (m_device)
        return m_device->is_master_pty();
    return false;
}

const MasterPTY* FileDescriptor::master_pty() const
{
    if (!is_master_pty())
        return nullptr;
    return static_cast<const MasterPTY*>(m_device.ptr());
}

MasterPTY* FileDescriptor::master_pty()
{
    if (!is_master_pty())
        return nullptr;
    return static_cast<MasterPTY*>(m_device.ptr());
}

int FileDescriptor::close()
{
    return 0;
}

String FileDescriptor::absolute_path()
{
    Stopwatch sw("absolute_path");
    if (is_tty())
        return tty()->tty_name();
    if (is_fifo()) {
        char buf[32];
        ksprintf(buf, "fifo:%x", m_fifo.ptr());
        return buf;
    }
    if (is_character_device()) {
        char buf[128];
        ksprintf(buf, "device:%u,%u (%s)", m_device->major(), m_device->minor(), m_device->class_name());
        return buf;
    }
    ASSERT(m_inode);
    return VFS::the().absolute_path(*m_inode);
}

FileDescriptor::FileDescriptor(FIFO& fifo, FIFO::Direction direction)
    : m_is_blocking(true)
    , m_fifo(fifo)
    , m_fifo_direction(direction)
{
    m_fifo->open(direction);
}

InodeMetadata FileDescriptor::metadata() const
{
    if (m_inode)
        return m_inode->metadata();
    return { };
}
