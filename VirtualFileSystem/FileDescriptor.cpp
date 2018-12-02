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
    descriptor->m_currentOffset = m_currentOffset;
#ifdef SERENITY
    descriptor->m_isBlocking = m_isBlocking;
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
        newOffset = m_currentOffset + offset;
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

    m_currentOffset = newOffset;
    return m_currentOffset;
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
    ssize_t nread = m_vnode->fileSystem()->read_inode_bytes(m_vnode->inode, m_currentOffset, count, buffer, this);
    m_currentOffset += nread;
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

bool FileDescriptor::hasDataAvailableForRead()
{
    if (is_fifo()) {
        ASSERT(fifo_direction() == FIFO::Reader);
        return m_fifo->can_read();
    }
    if (m_vnode->isCharacterDevice())
        return m_vnode->characterDevice()->hasDataAvailableForRead();
    return true;
}

ByteBuffer FileDescriptor::readEntireFile()
{
    ASSERT(!is_fifo());

    if (m_vnode->isCharacterDevice()) {
        auto buffer = ByteBuffer::createUninitialized(1024);
        ssize_t nread = m_vnode->characterDevice()->read(buffer.pointer(), buffer.size());
        buffer.trim(nread);
        return buffer;
    }

    if (m_vnode->core_inode())
        return m_vnode->core_inode()->read_entire(this);
    return m_vnode->fileSystem()->read_entire_inode(m_vnode->inode, this);
}

bool FileDescriptor::isDirectory() const
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
    auto tempBuffer = ByteBuffer::createUninitialized(2048);
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
bool FileDescriptor::isTTY() const
{
    if (is_fifo())
        return false;
    if (auto* device = m_vnode->characterDevice())
        return device->isTTY();
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
    if (isTTY())
        return tty()->ttyName();
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
    : m_isBlocking(true)
    , m_fifo(fifo)
    , m_fifo_direction(direction)
{
    m_fifo->open(direction);
}
