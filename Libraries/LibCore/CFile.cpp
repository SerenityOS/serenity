#include <LibCore/CFile.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

CFile::CFile(const StringView& filename, CObject* parent)
    : CIODevice(parent)
    , m_filename(filename)
{
}

CFile::~CFile()
{
    if (m_should_close_file_descriptor == ShouldCloseFileDescription::Yes && mode() != NotOpen)
        close();
}

bool CFile::open(int fd, CIODevice::OpenMode mode, ShouldCloseFileDescription should_close)
{
    set_fd(fd);
    set_mode(mode);
    m_should_close_file_descriptor = should_close;
    return true;
}

bool CFile::open(CIODevice::OpenMode mode)
{
    ASSERT(!m_filename.is_null());
    int flags = 0;
    if ((mode & CIODevice::ReadWrite) == CIODevice::ReadWrite) {
        flags |= O_RDWR | O_CREAT;
    } else if (mode & CIODevice::ReadOnly) {
        flags |= O_RDONLY;
    } else if (mode & CIODevice::WriteOnly) {
        flags |= O_WRONLY | O_CREAT;
        bool should_truncate = !((mode & CIODevice::Append) || (mode & CIODevice::MustBeNew));
        if (should_truncate)
            flags |= O_TRUNC;
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
