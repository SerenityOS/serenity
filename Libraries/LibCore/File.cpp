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

#ifdef __serenity__
#    include <Kernel/API/Syscall.h>
#endif
#include <AK/ScopeGuard.h>
#include <LibCore/File.h>
#include <errno.h>
#include <fcntl.h>
#include <libgen.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

namespace Core {

Result<NonnullRefPtr<File>, String> File::open(const String& filename, IODevice::OpenMode mode, mode_t permissions)
{
    auto file = File::construct(filename);
    if (!file->open_impl(mode, permissions))
        return String(file->error_string());
    return file;
}

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
    return open_impl(mode, 0666);
}

bool File::open_impl(IODevice::OpenMode mode, mode_t permissions)
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
    int fd = ::open(m_filename.characters(), flags, permissions);
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

bool File::exists(const String& filename)
{
    struct stat st;
    return stat(filename.characters(), &st) == 0;
}

String File::real_path_for(const String& filename)
{
    if (filename.is_null())
        return {};
    auto* path = realpath(filename.characters(), nullptr);
    String real_path(path);
    free(path);
    return real_path;
}

bool File::ensure_parent_directories(const String& path)
{
    ASSERT(path.starts_with("/"));

    int saved_errno = 0;
    ScopeGuard restore_errno = [&saved_errno] { errno = saved_errno; };

    char* parent_buffer = strdup(path.characters());
    ScopeGuard free_buffer = [parent_buffer] { free(parent_buffer); };

    const char* parent = dirname(parent_buffer);

    int rc = mkdir(parent, 0755);
    saved_errno = errno;

    if (rc == 0 || errno == EEXIST)
        return true;

    if (errno != ENOENT)
        return false;

    bool ok = ensure_parent_directories(parent);
    saved_errno = errno;
    if (!ok)
        return false;

    rc = mkdir(parent, 0755);
    saved_errno = errno;
    return rc == 0;
}

#ifdef __serenity__

String File::read_link(const StringView& link_path)
{
    // First, try using a 64-byte buffer, that ought to be enough for anybody.
    char small_buffer[64];
    Syscall::SC_readlink_params small_params {
        { link_path.characters_without_null_termination(), link_path.length() },
        { small_buffer, sizeof(small_buffer) }
    };
    int rc = syscall(SC_readlink, &small_params);
    if (rc < 0) {
        errno = -rc;
        return {};
    }
    size_t size = rc;
    // If the call was successful, the syscall (unlike the LibC wrapper)
    // returns the full size of the link. Let's see if our small buffer
    // was enough to read the whole link.
    if (size <= sizeof(small_buffer))
        return { small_buffer, size };
    // Nope, but at least now we know the right size.
    char* large_buffer_ptr;
    auto large_buffer = StringImpl::create_uninitialized(size, large_buffer_ptr);
    Syscall::SC_readlink_params large_params {
        { link_path.characters_without_null_termination(), link_path.length() },
        { large_buffer_ptr, (size_t)size }
    };
    rc = syscall(SC_readlink, &large_params);
    if (rc < 0) {
        errno = -rc;
        return {};
    }
    size_t new_size = rc;
    if (new_size == size)
        return { *large_buffer };

    // If we're here, the symlink has changed while we were looking at it.
    // If it became shorter, our buffer is valid, we just have to trim it a bit.
    if (new_size < size)
        return { large_buffer_ptr, new_size };
    // Otherwise, here's not much we can do, unless we want to loop endlessly
    // in this case. Let's leave it up to the caller whether to loop.
    errno = -EAGAIN;
    return {};
}

#else

// This is a sad version for other systems. It has to always make a copy of the
// link path, and to always make two syscalls to get the right size first.
String File::read_link(const StringView& link_path)
{
    String link_path_str = link_path;
    struct stat statbuf;
    int rc = lstat(link_path_str.characters(), &statbuf);
    if (rc < 0)
        return {};
    char* buffer_ptr;
    auto buffer = StringImpl::create_uninitialized(statbuf.st_size, buffer_ptr);
    rc = readlink(link_path_str.characters(), buffer_ptr, statbuf.st_size);
    if (rc < 0)
        return {};
    // (See above.)
    if (rc == statbuf.st_size)
        return { *buffer };
    return { buffer_ptr, (size_t)rc };
}

#endif

static RefPtr<File> stdin_file;
static RefPtr<File> stdout_file;
static RefPtr<File> stderr_file;

NonnullRefPtr<File> File::stdin()
{
    if (!stdin_file) {
        stdin_file = File::construct();
        stdin_file->open(STDIN_FILENO, IODevice::ReadOnly, ShouldCloseFileDescription::No);
    }
    return *stdin_file;
}

NonnullRefPtr<File> File::stdout()
{
    if (!stdout_file) {
        stdout_file = File::construct();
        stdout_file->open(STDOUT_FILENO, IODevice::WriteOnly, ShouldCloseFileDescription::No);
    }
    return *stdout_file;
}

NonnullRefPtr<File> File::stderr()
{
    if (!stderr_file) {
        stderr_file = File::construct();
        stderr_file->open(STDERR_FILENO, IODevice::WriteOnly, ShouldCloseFileDescription::No);
    }
    return *stderr_file;
}
}
