#include <AK/BufferStream.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/SharedMemory.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

Retained<FileDescription> FileDescription::create(RetainPtr<Custody>&& custody)
{
    auto descriptor = adopt(*new FileDescription(InodeFile::create(custody->inode())));
    descriptor->m_custody = move(custody);
    return descriptor;
}

Retained<FileDescription> FileDescription::create(RetainPtr<File>&& file, SocketRole role)
{
    return adopt(*new FileDescription(move(file), role));
}

FileDescription::FileDescription(RetainPtr<File>&& file, SocketRole role)
    : m_file(move(file))
{
    if (m_file->is_inode())
        m_inode = static_cast<InodeFile&>(*m_file).inode();
    set_socket_role(role);
}

FileDescription::~FileDescription()
{
    if (is_socket())
        socket()->detach(*this);
    if (is_fifo())
        static_cast<FIFO*>(m_file.ptr())->detach(m_fifo_direction);
    m_file->close();
    m_file = nullptr;
    m_inode = nullptr;
}

void FileDescription::set_socket_role(SocketRole role)
{
    if (role == m_socket_role)
        return;

    ASSERT(is_socket());
    if (m_socket_role != SocketRole::None)
        socket()->detach(*this);
    m_socket_role = role;
    socket()->attach(*this);
}

Retained<FileDescription> FileDescription::clone()
{
    RetainPtr<FileDescription> descriptor;
    if (is_fifo()) {
        descriptor = fifo()->open_direction(m_fifo_direction);
    } else {
        descriptor = FileDescription::create(m_file.copy_ref(), m_socket_role);
        descriptor->m_custody = m_custody.copy_ref();
        descriptor->m_inode = m_inode.copy_ref();
    }
    ASSERT(descriptor);
    descriptor->m_current_offset = m_current_offset;
    descriptor->m_is_blocking = m_is_blocking;
    descriptor->m_should_append = m_should_append;
    descriptor->m_file_flags = m_file_flags;
    return *descriptor;
}

KResult FileDescription::fstat(stat& buffer)
{
    ASSERT(!is_fifo());
    if (!m_inode)
        return KResult(-EBADF);
    return metadata().stat(buffer);
}

KResult FileDescription::fchmod(mode_t mode)
{
    if (!m_inode)
        return KResult(-EBADF);
    return VFS::the().chmod(*m_inode, mode);
}

off_t FileDescription::seek(off_t offset, int whence)
{
    if (!m_file->is_seekable())
        return -EINVAL;

    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return -EIO;

    if (metadata.is_socket() || metadata.is_fifo())
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
        new_offset = metadata.size;
        break;
    default:
        return -EINVAL;
    }

    if (new_offset < 0)
        return -EINVAL;
    // FIXME: Return -EINVAL if attempting to seek past the end of a seekable device.

    m_current_offset = new_offset;
    return m_current_offset;
}

ssize_t FileDescription::read(byte* buffer, ssize_t count)
{
    int nread = m_file->read(*this, buffer, count);
    if (m_file->is_seekable())
        m_current_offset += nread;
    return nread;
}

ssize_t FileDescription::write(const byte* data, ssize_t size)
{
    int nwritten = m_file->write(*this, data, size);
    if (m_file->is_seekable())
        m_current_offset += nwritten;
    return nwritten;
}

bool FileDescription::can_write()
{
    return m_file->can_write(*this);
}

bool FileDescription::can_read()
{
    return m_file->can_read(*this);
}

ByteBuffer FileDescription::read_entire_file()
{
    // HACK ALERT: (This entire function)
    ASSERT(m_file->is_inode());
    ASSERT(m_inode);
    return m_inode->read_entire(this);
}

bool FileDescription::is_directory() const
{
    ASSERT(!is_fifo());
    return metadata().is_directory();
}

ssize_t FileDescription::get_dir_entries(byte* buffer, ssize_t size)
{
    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return -EIO;
    if (!metadata.is_directory())
        return -ENOTDIR;

    int size_to_allocate = max(PAGE_SIZE, metadata.size);

    auto temp_buffer = ByteBuffer::create_uninitialized(size_to_allocate);
    BufferStream stream(temp_buffer);
    VFS::the().traverse_directory_inode(*m_inode, [&stream](auto& entry) {
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

bool FileDescription::is_device() const
{
    return m_file->is_device();
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

int FileDescription::close()
{
    return 0;
}

String FileDescription::absolute_path() const
{
    if (m_custody)
        return m_custody->absolute_path();
    dbgprintf("FileDescription::absolute_path() for FD without custody, File type: %s\n", m_file->class_name());
    return m_file->absolute_path(*this);
}

InodeMetadata FileDescription::metadata() const
{
    if (m_inode)
        return m_inode->metadata();
    return {};
}

KResultOr<Region*> FileDescription::mmap(Process& process, LinearAddress laddr, size_t offset, size_t size, int prot)
{
    return m_file->mmap(process, *this, laddr, offset, size, prot);
}

KResult FileDescription::truncate(off_t length)
{
    return m_file->truncate(length);
}

bool FileDescription::is_shared_memory() const
{
    return m_file->is_shared_memory();
}

SharedMemory* FileDescription::shared_memory()
{
    if (!is_shared_memory())
        return nullptr;
    return static_cast<SharedMemory*>(m_file.ptr());
}

const SharedMemory* FileDescription::shared_memory() const
{
    if (!is_shared_memory())
        return nullptr;
    return static_cast<const SharedMemory*>(m_file.ptr());
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

void FileDescription::set_file_flags(dword flags)
{
    m_is_blocking = !(flags & O_NONBLOCK);
    m_should_append = flags & O_APPEND;
    m_file_flags = flags;
}

KResult FileDescription::chown(uid_t uid, gid_t gid)
{
    if (!m_inode)
        return KResult(-EINVAL);
    return VFS::the().chown(*m_inode, uid, gid);
}
