#include <LibGUI/GFile.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>

GFile::GFile(const String& filename)
    : m_filename(filename)
{
}

GFile::~GFile()
{
    if (mode() != NotOpen)
        close();
}

bool GFile::open(GIODevice::OpenMode mode)
{
    int flags = 0;
    if ((mode & GIODevice::ReadWrite) == GIODevice::ReadWrite) {
        flags |= O_RDWR | O_CREAT;
    } else if (mode & GIODevice::ReadOnly) {
        flags |= O_RDONLY;
    } else if (mode & GIODevice::WriteOnly) {
        flags |= O_WRONLY | O_CREAT;
    }
    if (mode & GIODevice::Append)
        flags |= O_APPEND;
    if (mode & GIODevice::Truncate)
        flags |= O_TRUNC;
    if (mode & GIODevice::MustBeNew)
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
