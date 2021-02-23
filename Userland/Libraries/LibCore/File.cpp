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
#    include <serenity.h>
#endif
#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <errno.h>
#include <fcntl.h>
#include <libgen.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

// On Linux distros that use glibc `basename` is defined as a macro that expands to `__xpg_basename`, so we undefine it
#if defined(__linux__) && defined(basename)
#    undef basename
#endif

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
    if (m_should_close_file_descriptor == ShouldCloseFileDescriptor::Yes && mode() != NotOpen)
        close();
}

bool File::open(int fd, IODevice::OpenMode mode, ShouldCloseFileDescriptor should_close)
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
    VERIFY(!m_filename.is_null());
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
    VERIFY(path.starts_with("/"));

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

    int rc = serenity_readlink(link_path.characters_without_null_termination(), link_path.length(), small_buffer, sizeof(small_buffer));
    if (rc < 0)
        return {};

    size_t size = rc;
    // If the call was successful, the syscall (unlike the LibC wrapper)
    // returns the full size of the link. Let's see if our small buffer
    // was enough to read the whole link.
    if (size <= sizeof(small_buffer))
        return { small_buffer, size };
    // Nope, but at least now we know the right size.
    char* large_buffer_ptr;
    auto large_buffer = StringImpl::create_uninitialized(size, large_buffer_ptr);

    rc = serenity_readlink(link_path.characters_without_null_termination(), link_path.length(), large_buffer_ptr, size);
    if (rc < 0)
        return {};

    size_t new_size = rc;
    if (new_size == size)
        return { *large_buffer };

    // If we're here, the symlink has changed while we were looking at it.
    // If it became shorter, our buffer is valid, we just have to trim it a bit.
    if (new_size < size)
        return { large_buffer_ptr, new_size };
    // Otherwise, here's not much we can do, unless we want to loop endlessly
    // in this case. Let's leave it up to the caller whether to loop.
    errno = EAGAIN;
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

NonnullRefPtr<File> File::standard_input()
{
    if (!stdin_file) {
        stdin_file = File::construct();
        stdin_file->open(STDIN_FILENO, IODevice::ReadOnly, ShouldCloseFileDescriptor::No);
    }
    return *stdin_file;
}

NonnullRefPtr<File> File::standard_output()
{
    if (!stdout_file) {
        stdout_file = File::construct();
        stdout_file->open(STDOUT_FILENO, IODevice::WriteOnly, ShouldCloseFileDescriptor::No);
    }
    return *stdout_file;
}

NonnullRefPtr<File> File::standard_error()
{
    if (!stderr_file) {
        stderr_file = File::construct();
        stderr_file->open(STDERR_FILENO, IODevice::WriteOnly, ShouldCloseFileDescriptor::No);
    }
    return *stderr_file;
}

static String get_duplicate_name(const String& path, int duplicate_count)
{
    if (duplicate_count == 0) {
        return path;
    }
    LexicalPath lexical_path(path);
    StringBuilder duplicated_name;
    duplicated_name.append('/');
    for (size_t i = 0; i < lexical_path.parts().size() - 1; ++i) {
        duplicated_name.appendff("{}/", lexical_path.parts()[i]);
    }
    auto prev_duplicate_tag = String::formatted("({})", duplicate_count);
    auto title = lexical_path.title();
    if (title.ends_with(prev_duplicate_tag)) {
        // remove the previous duplicate tag "(n)" so we can add a new tag.
        title = title.substring(0, title.length() - prev_duplicate_tag.length());
    }
    duplicated_name.appendff("{} ({})", lexical_path.title(), duplicate_count);
    if (!lexical_path.extension().is_empty()) {
        duplicated_name.appendff(".{}", lexical_path.extension());
    }
    return duplicated_name.build();
}

Result<void, File::CopyError> File::copy_file_or_directory(const String& dst_path, const String& src_path, RecursionMode recursion_mode, LinkMode link_mode, AddDuplicateFileMarker add_duplicate_file_marker)
{
    if (add_duplicate_file_marker == AddDuplicateFileMarker::Yes) {
        int duplicate_count = 0;
        while (access(get_duplicate_name(dst_path, duplicate_count).characters(), F_OK) == 0) {
            ++duplicate_count;
        }
        if (duplicate_count != 0) {
            return copy_file_or_directory(get_duplicate_name(dst_path, duplicate_count), src_path);
        }
    }

    auto source_or_error = File::open(src_path, IODevice::ReadOnly);
    if (source_or_error.is_error())
        return CopyError { OSError(errno), false };

    auto& source = *source_or_error.value();

    struct stat src_stat;
    if (fstat(source.fd(), &src_stat) < 0)
        return CopyError { OSError(errno), false };

    if (source.is_directory()) {
        if (recursion_mode == RecursionMode::Disallowed)
            return CopyError { OSError(errno), true };
        return copy_directory(dst_path, src_path, src_stat);
    }

    if (link_mode == LinkMode::Allowed) {
        if (link(src_path.characters(), dst_path.characters()) < 0)
            return CopyError { OSError(errno), false };

        return {};
    }

    return copy_file(dst_path, src_stat, source);
}

Result<void, File::CopyError> File::copy_file(const String& dst_path, const struct stat& src_stat, File& source)
{

    int dst_fd = creat(dst_path.characters(), 0666);
    if (dst_fd < 0) {
        if (errno != EISDIR)
            return CopyError { OSError(errno), false };

        auto dst_dir_path = String::formatted("{}/{}", dst_path, LexicalPath(source.filename()).basename());
        dst_fd = creat(dst_dir_path.characters(), 0666);
        if (dst_fd < 0)
            return CopyError { OSError(errno), false };
    }

    ScopeGuard close_fd_guard([dst_fd]() { ::close(dst_fd); });

    if (src_stat.st_size > 0) {
        if (ftruncate(dst_fd, src_stat.st_size) < 0)
            return CopyError { OSError(errno), false };
    }

    for (;;) {
        char buffer[32768];
        ssize_t nread = ::read(source.fd(), buffer, sizeof(buffer));
        if (nread < 0) {
            return CopyError { OSError(errno), false };
        }
        if (nread == 0)
            break;
        ssize_t remaining_to_write = nread;
        char* bufptr = buffer;
        while (remaining_to_write) {
            ssize_t nwritten = ::write(dst_fd, bufptr, remaining_to_write);
            if (nwritten < 0)
                return CopyError { OSError(errno), false };

            VERIFY(nwritten > 0);
            remaining_to_write -= nwritten;
            bufptr += nwritten;
        }
    }

    // NOTE: We don't copy the set-uid and set-gid bits.
    auto my_umask = umask(0);
    umask(my_umask);
    if (fchmod(dst_fd, (src_stat.st_mode & ~my_umask) & ~06000) < 0)
        return CopyError { OSError(errno), false };

    return {};
}

Result<void, File::CopyError> File::copy_directory(const String& dst_path, const String& src_path, const struct stat& src_stat, LinkMode link)
{
    if (mkdir(dst_path.characters(), 0755) < 0)
        return CopyError { OSError(errno), false };

    String src_rp = File::real_path_for(src_path);
    src_rp = String::formatted("{}/", src_rp);
    String dst_rp = File::real_path_for(dst_path);
    dst_rp = String::formatted("{}/", dst_rp);

    if (!dst_rp.is_empty() && dst_rp.starts_with(src_rp))
        return CopyError { OSError(errno), false };

    DirIterator di(src_path, DirIterator::SkipDots);
    if (di.has_error())
        return CopyError { OSError(errno), false };

    while (di.has_next()) {
        String filename = di.next_path();
        auto result = copy_file_or_directory(
            String::formatted("{}/{}", dst_path, filename),
            String::formatted("{}/{}", src_path, filename),
            RecursionMode::Allowed, link);
        if (result.is_error())
            return result.error();
    }

    auto my_umask = umask(0);
    umask(my_umask);
    if (chmod(dst_path.characters(), src_stat.st_mode & ~my_umask) < 0)
        return CopyError { OSError(errno), false };

    return {};
}

Result<void, OSError> File::link_file(const String& dst_path, const String& src_path)
{
    int duplicate_count = 0;
    while (access(get_duplicate_name(dst_path, duplicate_count).characters(), F_OK) == 0) {
        ++duplicate_count;
    }
    if (duplicate_count != 0) {
        return link_file(src_path, get_duplicate_name(dst_path, duplicate_count));
    }
    int rc = symlink(src_path.characters(), dst_path.characters());
    if (rc < 0) {
        return OSError(errno);
    }

    return {};
}

Result<void, File::RemoveError> File::remove(const String& path, RecursionMode mode, bool force)
{
    struct stat path_stat;
    if (lstat(path.characters(), &path_stat) < 0) {
        if (!force)
            return RemoveError { path, OSError(errno) };
        return {};
    }

    if (S_ISDIR(path_stat.st_mode) && mode == RecursionMode::Allowed) {
        auto di = DirIterator(path, DirIterator::SkipParentAndBaseDir);
        if (di.has_error())
            return RemoveError { path, OSError(di.error()) };

        while (di.has_next()) {
            auto result = remove(di.next_full_path(), RecursionMode::Allowed, true);
            if (result.is_error())
                return result.error();
        }

        if (rmdir(path.characters()) < 0 && !force)
            return RemoveError { path, OSError(errno) };
    } else {
        if (unlink(path.characters()) < 0 && !force)
            return RemoveError { path, OSError(errno) };
    }

    return {};
}

}
