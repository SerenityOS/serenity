#include "FileDescriptor.h"
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <LibC/errno_numbers.h>
#include "UnixTypes.h"
#include <AK/BufferStream.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/SharedMemory.h>

Retained<FileDescriptor> FileDescriptor::create(RetainPtr<Inode>&& inode)
{
    return adopt(*new FileDescriptor(move(inode)));
}

Retained<FileDescriptor> FileDescriptor::create(RetainPtr<File>&& file, SocketRole role)
{
    return adopt(*new FileDescriptor(move(file), role));
}

FileDescriptor::FileDescriptor(RetainPtr<Inode>&& inode)
    : m_inode(move(inode))
{
}

FileDescriptor::FileDescriptor(RetainPtr<File>&& file, SocketRole role)
    : m_file(move(file))
{
    set_socket_role(role);
}

FileDescriptor::~FileDescriptor()
{
    if (is_socket())
        socket()->detach(*this);
    if (is_fifo())
        static_cast<FIFO*>(m_file.ptr())->detach(m_fifo_direction);
    if (m_file) {
        m_file->close();
        m_file = nullptr;
    }
    m_inode = nullptr;
}

void FileDescriptor::set_socket_role(SocketRole role)
{
    if (role == m_socket_role)
        return;

    ASSERT(is_socket());
    if (m_socket_role != SocketRole::None)
        socket()->detach(*this);
    m_socket_role = role;
    socket()->attach(*this);
}

Retained<FileDescriptor> FileDescriptor::clone()
{
    RetainPtr<FileDescriptor> descriptor;
    if (is_fifo()) {
        descriptor = fifo()->open_direction(m_fifo_direction);
    } else {
        if (m_file) {
            descriptor = FileDescriptor::create(m_file.copy_ref(), m_socket_role);
            descriptor->m_inode = m_inode.copy_ref();
        } else {
            descriptor = FileDescriptor::create(m_inode.copy_ref());
        }
    }
    ASSERT(descriptor);
    descriptor->m_current_offset = m_current_offset;
    descriptor->m_is_blocking = m_is_blocking;
    descriptor->m_should_append = m_should_append;
    descriptor->m_file_flags = m_file_flags;
    return *descriptor;
}

bool addition_would_overflow(off_t a, off_t b)
{
    ASSERT(a > 0);
    uint64_t ua = a;
    return (ua + b) > OFF_T_MAX;
}

KResult FileDescriptor::fstat(stat& buffer)
{
    ASSERT(!is_fifo());
    if (!m_inode && !m_file)
        return KResult(-EBADF);

    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return KResult(-EIO);

    buffer.st_rdev = encoded_device(metadata.major_device, metadata.minor_device);
    buffer.st_ino = metadata.inode.index();
    buffer.st_mode = metadata.mode;
    buffer.st_nlink = metadata.link_count;
    buffer.st_uid = metadata.uid;
    buffer.st_gid = metadata.gid;
    buffer.st_dev = 0; // FIXME
    buffer.st_size = metadata.size;
    buffer.st_blksize = metadata.block_size;
    buffer.st_blocks = metadata.block_count;
    buffer.st_atime = metadata.atime;
    buffer.st_mtime = metadata.mtime;
    buffer.st_ctime = metadata.ctime;
    return KSuccess;
}

KResult FileDescriptor::fchmod(mode_t mode)
{
    if (!m_inode)
        return KResult(-EBADF);
    return VFS::the().chmod(*m_inode, mode);
}

off_t FileDescriptor::seek(off_t offset, int whence)
{
    ASSERT(!is_fifo());
    if (!m_inode && !m_file)
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
        break;
    case SEEK_END:
        newOffset = metadata.size;
        break;
    default:
        return -EINVAL;
    }

    if (newOffset < 0)
        return -EINVAL;
    // FIXME: Return -EINVAL if attempting to seek past the end of a seekable device.

    m_current_offset = newOffset;
    return m_current_offset;
}

ssize_t FileDescriptor::read(byte* buffer, ssize_t count)
{
    if (m_file) {
        int nread = m_file->read(*this, buffer, count);
        if (!m_file->is_seekable())
            m_current_offset += nread;
        return nread;
    }
    ASSERT(inode());
    ssize_t nread = inode()->read_bytes(m_current_offset, count, buffer, this);
    m_current_offset += nread;
    return nread;
}

ssize_t FileDescriptor::write(const byte* data, ssize_t size)
{
    if (m_file) {
        int nwritten = m_file->write(*this, data, size);
        if (m_file->is_seekable())
            m_current_offset += nwritten;
        return nwritten;
    }
    ASSERT(m_inode);
    ssize_t nwritten = m_inode->write_bytes(m_current_offset, size, data, this);
    m_current_offset += nwritten;
    return nwritten;
}

bool FileDescriptor::can_write()
{
    if (m_file)
        return m_file->can_write(*this);
    return true;
}

bool FileDescriptor::can_read()
{
    if (m_file)
        return m_file->can_read(*this);
    return true;
}

ByteBuffer FileDescriptor::read_entire_file()
{
    // HACK ALERT: (This entire function)

    ASSERT(!is_fifo());

    if (m_file) {
        auto buffer = ByteBuffer::create_uninitialized(1024);
        ssize_t nread = m_file->read(*this, buffer.pointer(), buffer.size());
        ASSERT(nread >= 0);
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

ssize_t FileDescriptor::get_dir_entries(byte* buffer, ssize_t size)
{
    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return -EIO;
    if (!metadata.is_directory())
        return -ENOTDIR;

    int size_to_allocate = max(PAGE_SIZE, metadata.size);

    auto temp_buffer = ByteBuffer::create_uninitialized(size_to_allocate);
    BufferStream stream(temp_buffer);
    VFS::the().traverse_directory_inode(*m_inode, [&stream] (auto& entry) {
        stream << (dword)entry.inode.index();
        stream << (byte)entry.file_type;
        stream << (dword)entry.name_length;
        stream << entry.name;
        return true;
    });
    stream.snip();

    if (size < temp_buffer.size())
        return -1;

    memcpy(buffer, temp_buffer.pointer(), temp_buffer.size());
    return stream.offset();
}

bool FileDescriptor::is_device() const
{
    return m_file && m_file->is_device();
}

bool FileDescriptor::is_tty() const
{
    return m_file && m_file->is_tty();
}

const TTY* FileDescriptor::tty() const
{
    if (!is_tty())
        return nullptr;
    return static_cast<const TTY*>(m_file.ptr());
}

TTY* FileDescriptor::tty()
{
    if (!is_tty())
        return nullptr;
    return static_cast<TTY*>(m_file.ptr());
}

bool FileDescriptor::is_master_pty() const
{
    return m_file && m_file->is_master_pty();
}

const MasterPTY* FileDescriptor::master_pty() const
{
    if (!is_master_pty())
        return nullptr;
    return static_cast<const MasterPTY*>(m_file.ptr());
}

MasterPTY* FileDescriptor::master_pty()
{
    if (!is_master_pty())
        return nullptr;
    return static_cast<MasterPTY*>(m_file.ptr());
}

int FileDescriptor::close()
{
    return 0;
}

bool FileDescriptor::is_fsfile() const
{
    return !is_tty() && !is_fifo() && !is_device() && !is_socket() && !is_shared_memory();
}

KResultOr<String> FileDescriptor::absolute_path()
{
    if (m_file)
        return m_file->absolute_path(*this);
    ASSERT(m_inode);
    return VFS::the().absolute_path(*m_inode);
}

InodeMetadata FileDescriptor::metadata() const
{
    if (m_inode)
        return m_inode->metadata();
    return { };
}

KResultOr<Region*> FileDescriptor::mmap(Process& process, LinearAddress laddr, size_t offset, size_t size, int prot)
{
    if (m_file)
        return m_file->mmap(process, laddr, offset, size, prot);

    if (!is_fsfile())
        return KResult(-ENODEV);

    ASSERT(m_inode);
    // FIXME: If PROT_EXEC, check that the underlying file system isn't mounted noexec.
    String region_name;
#if 0
    // FIXME: I would like to do this, but it would instantiate all the damn inodes.
    region_name = absolute_path();
#else
    region_name = "Memory-mapped file";
#endif
    InterruptDisabler disabler;
    auto* region = process.allocate_file_backed_region(laddr, size, inode(), move(region_name), prot & PROT_READ, prot & PROT_WRITE);
    if (!region)
        return KResult(-ENOMEM);
    return region;
}

KResult FileDescriptor::truncate(off_t length)
{
    if (is_file()) {
        return m_inode->truncate(length);
    }
    ASSERT(is_shared_memory());
    return shared_memory()->truncate(length);
}

bool FileDescriptor::is_shared_memory() const
{
    return m_file && m_file->is_shared_memory();
}

SharedMemory* FileDescriptor::shared_memory()
{
    if (!is_shared_memory())
        return nullptr;
    return static_cast<SharedMemory*>(m_file.ptr());
}

const SharedMemory* FileDescriptor::shared_memory() const
{
    if (!is_shared_memory())
        return nullptr;
    return static_cast<const SharedMemory*>(m_file.ptr());
}

bool FileDescriptor::is_fifo() const
{
    return m_file && m_file->is_fifo();
}

FIFO* FileDescriptor::fifo()
{
    if (!is_fifo())
        return nullptr;
    return static_cast<FIFO*>(m_file.ptr());
}

bool FileDescriptor::is_socket() const
{
    return m_file && m_file->is_socket();
}

Socket* FileDescriptor::socket()
{
    if (!is_socket())
        return nullptr;
    return static_cast<Socket*>(m_file.ptr());
}

const Socket* FileDescriptor::socket() const
{
    if (!is_socket())
        return nullptr;
    return static_cast<const Socket*>(m_file.ptr());
}
