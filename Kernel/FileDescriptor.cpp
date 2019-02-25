#include "FileDescriptor.h"
#include "FileSystem.h"
#include "CharacterDevice.h"
#include <LibC/errno_numbers.h>
#include "UnixTypes.h"
#include <AK/BufferStream.h>
#include "FIFO.h"
#include "TTY.h"
#include "MasterPTY.h"
#include <Kernel/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/BlockDevice.h>
#include <Kernel/MemoryManager.h>

Retained<FileDescriptor> FileDescriptor::create(RetainPtr<Inode>&& inode)
{
    return adopt(*new FileDescriptor(move(inode)));
}

Retained<FileDescriptor> FileDescriptor::create(RetainPtr<Device>&& device)
{
    return adopt(*new FileDescriptor(move(device)));
}

Retained<FileDescriptor> FileDescriptor::create(RetainPtr<Socket>&& socket, SocketRole role)
{
    return adopt(*new FileDescriptor(move(socket), role));
}

Retained<FileDescriptor> FileDescriptor::create_pipe_writer(FIFO& fifo)
{
    return adopt(*new FileDescriptor(fifo, FIFO::Writer));
}

Retained<FileDescriptor> FileDescriptor::create_pipe_reader(FIFO& fifo)
{
    return adopt(*new FileDescriptor(fifo, FIFO::Reader));
}

FileDescriptor::FileDescriptor(RetainPtr<Inode>&& inode)
    : m_inode(move(inode))
{
}

FileDescriptor::FileDescriptor(RetainPtr<Device>&& device)
    : m_device(move(device))
{
}

FileDescriptor::FileDescriptor(RetainPtr<Socket>&& socket, SocketRole role)
    : m_socket(move(socket))
{
    set_socket_role(role);
}

FileDescriptor::~FileDescriptor()
{
    if (m_socket) {
        m_socket->detach_fd(m_socket_role);
        m_socket = nullptr;
    }
    if (m_device) {
        m_device->close();
        m_device = nullptr;
    }
    if (m_fifo) {
        m_fifo->close(fifo_direction());
        m_fifo = nullptr;
    }
    m_inode = nullptr;
}

void FileDescriptor::set_socket_role(SocketRole role)
{
    if (role == m_socket_role)
        return;

    ASSERT(m_socket);
    m_socket_role = role;
    m_socket->attach_fd(role);
}

Retained<FileDescriptor> FileDescriptor::clone()
{
    RetainPtr<FileDescriptor> descriptor;
    if (is_fifo()) {
        descriptor = fifo_direction() == FIFO::Reader
            ? FileDescriptor::create_pipe_reader(*m_fifo)
            : FileDescriptor::create_pipe_writer(*m_fifo);
    } else {
        if (m_device) {
            descriptor = FileDescriptor::create(m_device.copy_ref());
            descriptor->m_inode = m_inode.copy_ref();
        } else if (m_socket) {
            descriptor = FileDescriptor::create(m_socket.copy_ref(), m_socket_role);
            descriptor->m_inode = m_inode.copy_ref();
        } else {
            descriptor = FileDescriptor::create(m_inode.copy_ref());
        }
    }
    ASSERT(descriptor);
    descriptor->m_current_offset = m_current_offset;
    descriptor->m_is_blocking = m_is_blocking;
    descriptor->m_file_flags = m_file_flags;
    return *descriptor;
}

bool addition_would_overflow(off_t a, off_t b)
{
    ASSERT(a > 0);
    uint64_t ua = a;
    return (ua + b) > maxFileOffset;
}

int FileDescriptor::fstat(stat* buffer)
{
    ASSERT(!is_fifo());
    if (!m_inode && !m_device)
        return -EBADF;

    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return -EIO;

    buffer->st_rdev = encoded_device(metadata.major_device, metadata.minor_device);
    buffer->st_ino = metadata.inode.index();
    buffer->st_mode = metadata.mode;
    buffer->st_nlink = metadata.link_count;
    buffer->st_uid = metadata.uid;
    buffer->st_gid = metadata.gid;
    buffer->st_dev = 0; // FIXME
    buffer->st_size = metadata.size;
    buffer->st_blksize = metadata.block_size;
    buffer->st_blocks = metadata.block_count;
    buffer->st_atime = metadata.atime;
    buffer->st_mtime = metadata.mtime;
    buffer->st_ctime = metadata.ctime;
    return 0;
}

off_t FileDescriptor::seek(off_t offset, int whence)
{
    ASSERT(!is_fifo());
    if (!m_inode && !m_device)
        return -EBADF;

    // FIXME: The file type should be cached on the vnode.
    //        It's silly that we have to do a full metadata lookup here.
    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return -EIO;

    if (metadata.is_socket() || metadata.is_fifo())
        return -ESPIPE;

    off_t newOffset;

    switch (whence) {
    case SEEK_SET:
        newOffset = offset;
        break;
    case SEEK_CUR:
        newOffset = m_current_offset + offset;
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
    if (m_socket)
        return m_socket->read(m_socket_role, buffer, count);
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
    if (m_socket)
        return m_socket->write(m_socket_role, data, size);
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
    if (m_socket)
        return m_socket->can_write(m_socket_role);
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
    if (m_socket)
        return m_socket->can_read(m_socket_role);
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
    return metadata().is_directory();
}

ssize_t FileDescriptor::get_dir_entries(byte* buffer, size_t size)
{
    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return -EIO;
    if (!metadata.is_directory())
        return -ENOTDIR;

    // FIXME: Compute the actual size needed.
    auto temp_buffer = ByteBuffer::create_uninitialized(2048);
    BufferStream stream(temp_buffer);
    VFS::the().traverse_directory_inode(*m_inode, [&stream] (auto& entry) {
        stream << (dword)entry.inode.index();
        stream << (byte)entry.file_type;
        stream << (dword)entry.name_length;
        stream << entry.name;
        return true;
    });

    if (size < stream.offset())
        return -1;

    memcpy(buffer, temp_buffer.pointer(), stream.offset());
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

const char* to_string(SocketRole role)
{
    switch (role) {
    case SocketRole::Listener:
        return "Listener";
    case SocketRole::Accepted:
        return "Accepted";
    case SocketRole::Connected:
        return "Connected";
    default:
        return "None";
    }
}

String FileDescriptor::absolute_path()
{
    Stopwatch sw("absolute_path");
    if (is_tty())
        return tty()->tty_name();
    if (is_fifo())
        return String::format("fifo:%x", m_fifo.ptr());
    if (is_device())
        return String::format("device:%u,%u (%s)", m_device->major(), m_device->minor(), m_device->class_name());
    if (is_socket())
        return String::format("socket:%x (role: %s)", m_socket.ptr(), to_string(m_socket_role));
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

bool FileDescriptor::supports_mmap() const
{
    if (m_inode)
        return true;
    if (m_device)
        return m_device->is_block_device();
    return false;
}

Region* FileDescriptor::mmap(Process& process, LinearAddress laddr, size_t offset, size_t size, int prot)
{
    ASSERT(supports_mmap());

    if (is_block_device())
        return static_cast<BlockDevice&>(*m_device).mmap(process, laddr, offset, size);

    ASSERT(m_inode);
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    auto region_name = absolute_path();
    InterruptDisabler disabler;
    // FIXME: Implement mapping at a client-specified address. Most of the support is already in plcae.
    ASSERT(laddr.as_ptr() == nullptr);
    auto* region = process.allocate_file_backed_region(LinearAddress(), size, inode(), move(region_name), prot & PROT_READ, prot & PROT_WRITE);
    region->page_in();
    return region;
}

bool FileDescriptor::is_block_device() const
{
    return m_device && m_device->is_block_device();
}

bool FileDescriptor::is_character_device() const
{
    return m_device && m_device->is_character_device();
}

CharacterDevice* FileDescriptor::character_device()
{
    return is_character_device() ? static_cast<CharacterDevice*>(device()) : nullptr;
}

const CharacterDevice* FileDescriptor::character_device() const
{
    return is_character_device() ? static_cast<const CharacterDevice*>(device()) : nullptr;
}
