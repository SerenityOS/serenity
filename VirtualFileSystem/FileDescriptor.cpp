#include "FileDescriptor.h"
#include "FileSystem.h"
#include "CharacterDevice.h"
#include <LibC/errno_numbers.h>
#include "UnixTypes.h"
#include <AK/BufferStream.h>
#include "FIFO.h"

#ifdef SERENITY
#include "TTY.h"
#endif

RetainPtr<FileDescriptor> FileDescriptor::create(RetainPtr<Vnode>&& vnode)
{
    return adopt(*new FileDescriptor(move(vnode)));
}

RetainPtr<FileDescriptor> FileDescriptor::create_pipe_writer(FIFO& fifo)
{
    return adopt(*new FileDescriptor(fifo, FIFO::Writer));
}

RetainPtr<FileDescriptor> FileDescriptor::create_pipe_reader(FIFO& fifo)
{
    return adopt(*new FileDescriptor(fifo, FIFO::Reader));
}

FileDescriptor::FileDescriptor(RetainPtr<Vnode>&& vnode)
    : m_vnode(move(vnode))
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
        descriptor = FileDescriptor::create(m_vnode.copyRef());
    }
    if (!descriptor)
        return nullptr;
    descriptor->m_current_offset = m_current_offset;
#ifdef SERENITY
    descriptor->m_is_blocking = m_is_blocking;
    descriptor->m_file_flags = m_file_flags;
#endif
    return descriptor;
}

#ifndef SERENITY
bool additionWouldOverflow(Unix::off_t a, Unix::off_t b)
{
    ASSERT(a > 0);
    uint64_t ua = a;
    return (ua + b) > maxFileOffset;
}
#endif

int FileDescriptor::stat(Unix::stat* buffer)
{
    ASSERT(!is_fifo());
    if (!m_vnode)
        return -EBADF;

    auto metadata = m_vnode->metadata();
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
    if (!m_vnode)
        return -EBADF;

    // FIXME: The file type should be cached on the vnode.
    //        It's silly that we have to do a full metadata lookup here.
    auto metadata = m_vnode->metadata();
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

ssize_t FileDescriptor::read(byte* buffer, size_t count)
{
    if (is_fifo()) {
        ASSERT(fifo_direction() == FIFO::Reader);
        return m_fifo->read(buffer, count);
    }
    if (m_vnode->isCharacterDevice()) {
        // FIXME: What should happen to m_currentOffset?
        return m_vnode->characterDevice()->read(buffer, count);
    }
    ASSERT(inode());
    ssize_t nread = inode()->read_bytes(m_current_offset, count, buffer, this);
    m_current_offset += nread;
    return nread;
}

ssize_t FileDescriptor::write(const byte* data, size_t size)
{
    if (is_fifo()) {
        ASSERT(fifo_direction() == FIFO::Writer);
        return m_fifo->write(data, size);
    }
    if (m_vnode->isCharacterDevice()) {
        // FIXME: What should happen to m_currentOffset?
        return m_vnode->characterDevice()->write(data, size);
    }
    // FIXME: Implement non-device writes.
    ASSERT_NOT_REACHED();
    return -1;
}

bool FileDescriptor::can_write()
{
    if (is_fifo()) {
        ASSERT(fifo_direction() == FIFO::Writer);
        return m_fifo->can_write();
    }
    return true;
}

bool FileDescriptor::has_data_available_for_reading()
{
    if (is_fifo()) {
        ASSERT(fifo_direction() == FIFO::Reader);
        return m_fifo->can_read();
    }
    if (m_vnode->isCharacterDevice())
        return m_vnode->characterDevice()->has_data_available_for_reading();
    return true;
}

ByteBuffer FileDescriptor::read_entire_file()
{
    ASSERT(!is_fifo());

    if (m_vnode->isCharacterDevice()) {
        auto buffer = ByteBuffer::create_uninitialized(1024);
        ssize_t nread = m_vnode->characterDevice()->read(buffer.pointer(), buffer.size());
        buffer.trim(nread);
        return buffer;
    }

    ASSERT(inode());
    return inode()->read_entire(this);
}

bool FileDescriptor::is_directory() const
{
    ASSERT(!is_fifo());
    return m_vnode->metadata().isDirectory();
}

ssize_t FileDescriptor::get_dir_entries(byte* buffer, size_t size)
{
    auto metadata = m_vnode->metadata();
    if (!metadata.isValid())
        return -EIO;
    if (!metadata.isDirectory())
        return -ENOTDIR;

    // FIXME: Compute the actual size needed.
    auto tempBuffer = ByteBuffer::create_uninitialized(2048);
    BufferStream stream(tempBuffer);
    m_vnode->vfs()->traverse_directory_inode(*m_vnode->core_inode(), [&stream] (auto& entry) {
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
\
#ifdef SERENITY
bool FileDescriptor::is_tty() const
{
    if (is_fifo())
        return false;
    if (auto* device = m_vnode->characterDevice())
        return device->is_tty();
    return false;
}

const TTY* FileDescriptor::tty() const
{
    if (is_fifo())
        return nullptr;
    if (auto* device = m_vnode->characterDevice())
        return static_cast<const TTY*>(device);
    return nullptr;
}

TTY* FileDescriptor::tty()
{
    if (is_fifo())
        return nullptr;
    if (auto* device = m_vnode->characterDevice())
        return static_cast<TTY*>(device);
    return nullptr;
}
#endif

int FileDescriptor::close()
{
    return 0;
}

String FileDescriptor::absolute_path()
{
    Stopwatch sw("absolute_path");
#ifdef SERENITY
    if (is_tty())
        return tty()->tty_name();
#endif
    if (is_fifo()) {
        char buf[32];
        ksprintf(buf, "fifo:%x", m_fifo.ptr());
        return buf;
    }
    ASSERT(m_vnode->core_inode());
    return VFS::the().absolute_path(*m_vnode->core_inode());
}

FileDescriptor::FileDescriptor(FIFO& fifo, FIFO::Direction direction)
    : m_is_blocking(true)
    , m_fifo(fifo)
    , m_fifo_direction(direction)
{
    m_fifo->open(direction);
}
