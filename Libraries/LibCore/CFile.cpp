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
