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
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

NonnullRefPtr<FileDescription> FileDescription::create(Custody& custody)
{
    auto description = adopt(*new FileDescription(InodeFile::create(custody.inode())));
    description->m_custody = custody;
    return description;
}

NonnullRefPtr<FileDescription> FileDescription::create(File& file)
{
    return adopt(*new FileDescription(file));
}

FileDescription::FileDescription(File& file)
    : m_file(file)
{
    if (file.is_inode())
        m_inode = static_cast<InodeFile&>(file).inode();
    if (is_socket())
        socket()->attach(*this);
    m_is_directory = metadata().is_directory();
}

FileDescription::~FileDescription()
{
    if (is_socket())
        socket()->detach(*this);
    if (is_fifo())
        static_cast<FIFO*>(m_file.ptr())->detach(m_fifo_direction);
    m_file->close();
    m_inode = nullptr;
}

KResult FileDescription::fstat(stat& buffer)
{
    SmapDisabler disabler;
    if (is_fifo()) {
        memset(&buffer, 0, sizeof(buffer));
        buffer.st_mode = 001000;
        return KSuccess;
    }
    if (is_socket()) {
        memset(&buffer, 0, sizeof(buffer));
        buffer.st_mode = 0140000;
        return KSuccess;
    }

    if (!m_inode)
        return KResult(-EBADF);
    return metadata().stat(buffer);
}

off_t FileDescription::seek(off_t offset, int whence)
{
    LOCKER(m_lock);
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

ssize_t FileDescription::read(u8* buffer, ssize_t count)
{
    LOCKER(m_lock);
    if ((m_current_offset + count) < 0)
        return -EOVERFLOW;
    SmapDisabler disabler;
    int nread = m_file->read(*this, buffer, count);
    if (nread > 0 && m_file->is_seekable())
        m_current_offset += nread;
    return nread;
}

ssize_t FileDescription::write(const u8* data, ssize_t size)
{
    LOCKER(m_lock);
    if ((m_current_offset + size) < 0)
        return -EOVERFLOW;
    SmapDisabler disabler;
    int nwritten = m_file->write(*this, data, size);
    if (nwritten > 0 && m_file->is_seekable())
        m_current_offset += nwritten;
    return nwritten;
}

bool FileDescription::can_write() const
{
    return m_file->can_write(*this);
}

bool FileDescription::can_read() const
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


ssize_t FileDescription::get_dir_entries(u8* buffer, ssize_t size)
{
    LOCKER(m_lock);
    if (!is_directory())
        return -ENOTDIR;

    auto metadata = this->metadata();
    if (!metadata.is_valid())
        return -EIO;

    int size_to_allocate = max(PAGE_SIZE, metadata.size);

    auto temp_buffer = ByteBuffer::create_uninitialized(size_to_allocate);
    BufferStream stream(temp_buffer);
    VFS::the().traverse_directory_inode(*m_inode, [&stream](auto& entry) {
        stream << (u32)entry.inode.index();
        stream << (u8)entry.file_type;
        stream << (u32)entry.name_length;
        stream << entry.name;
        return true;
    });
    stream.snip();

    if (size < temp_buffer.size())
        return -1;

    copy_to_user(buffer, temp_buffer.data(), temp_buffer.size());
    return stream.offset();
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

int FileDescription::close()
{
    return 0;
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

KResultOr<Region*> FileDescription::mmap(Process& process, VirtualAddress vaddr, size_t offset, size_t size, int prot)
{
    LOCKER(m_lock);
    return m_file->mmap(process, *this, vaddr, offset, size, prot);
}

KResult FileDescription::truncate(off_t length)
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
    return m_file->chmod(mode);
}

KResult FileDescription::chown(uid_t uid, gid_t gid)
{
    LOCKER(m_lock);
    return m_file->chown(uid, gid);
}
