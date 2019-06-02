#include <LibCore/CFile.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

CFile::CFile(const StringView& filename)
    : m_filename(filename)
{
}

CFile::~CFile()
{
    if (m_should_close_file_descriptor == ShouldCloseFileDescriptor::Yes && mode() != NotOpen)
        close();
}

bool CFile::open(int fd, CIODevice::OpenMode mode, ShouldCloseFileDescriptor should_close)
{
    set_fd(fd);
    set_mode(mode);
    m_should_close_file_descriptor = should_close;
    return true;
}

bool CFile::open(CIODevice::OpenMode mode)
{
    int flags = 0;
    if ((mode & CIODevice::ReadWrite) == CIODevice::ReadWrite) {
        flags |= O_RDWR | O_CREAT;
    } else if (mode & CIODevice::ReadOnly) {
        flags |= O_RDONLY;
    } else if (mode & CIODevice::WriteOnly) {
        flags |= O_WRONLY | O_CREAT;
    }
    if (mode & CIODevice::Append)
        flags |= O_APPEND;
    if (mode & CIODevice::Truncate)
        flags |= O_TRUNC;
    if (mode & CIODevice::MustBeNew)
        flags |= O_EXCL;
    int fd = ::open(m_filename.characters(), flags, 0666);
    if (fd < 0) {
        set_error(errno);
        return false;
    }

    set_fd(fd);
    set_mode(mode);
    return true;
}
