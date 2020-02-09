/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibCore/File.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

namespace Core {

File::File(const StringView& filename, Object* parent)
    : IODevice(parent)
    , m_filename(filename)
{
}

File::~File()
{
    if (m_should_close_file_descriptor == ShouldCloseFileDescription::Yes && mode() != NotOpen)
        close();
}

bool File::open(int fd, IODevice::OpenMode mode, ShouldCloseFileDescription should_close)
{
    set_fd(fd);
    set_mode(mode);
    m_should_close_file_descriptor = should_close;
    return true;
}

bool File::open(IODevice::OpenMode mode)
{
    ASSERT(!m_filename.is_null());
    int flags = 0;
    if ((mode & IODevice::ReadWrite) == IODevice::ReadWrite) {
        flags |= O_RDWR | O_CREAT;
    } else if (mode & IODevice::ReadOnly) {
        flags |= O_RDONLY;
    } else if (mode & IODevice::WriteOnly) {
        flags |= O_WRONLY | O_CREAT;
        bool should_truncate = !((mode & IODevice::Append) || (mode & IODevice::MustBeNew));
        if (should_truncate)
            flags |= O_TRUNC;
    }
    if (mode & IODevice::Append)
        flags |= O_APPEND;
    if (mode & IODevice::Truncate)
        flags |= O_TRUNC;
    if (mode & IODevice::MustBeNew)
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

bool File::is_directory() const
{
    struct stat stat;
    if (fstat(fd(), &stat) < 0)
        return false;
    return S_ISDIR(stat.st_mode);
}

bool File::is_directory(const String& filename)
{
    struct stat st;
    if (stat(filename.characters(), &st) < 0)
        return false;
    return S_ISDIR(st.st_mode);
}

}
