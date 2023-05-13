/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <AK/ScopeGuard.h>
#include <LibCore/DeprecatedFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <errno.h>
#include <fcntl.h>
#include <libgen.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <utime.h>

#ifdef AK_OS_SERENITY
#    include <serenity.h>
#endif

namespace Core {

ErrorOr<NonnullRefPtr<DeprecatedFile>> DeprecatedFile::open(DeprecatedString filename, OpenMode mode, mode_t permissions)
{
    auto file = DeprecatedFile::construct(move(filename));
    if (!file->open_impl(mode, permissions))
        return Error::from_errno(file->error());
    return file;
}

DeprecatedFile::DeprecatedFile(DeprecatedString filename, Object* parent)
    : IODevice(parent)
    , m_filename(move(filename))
{
}

DeprecatedFile::~DeprecatedFile()
{
    if (m_should_close_file_descriptor == ShouldCloseFileDescriptor::Yes && mode() != OpenMode::NotOpen)
        close();
}

bool DeprecatedFile::open(int fd, OpenMode mode, ShouldCloseFileDescriptor should_close)
{
    set_fd(fd);
    set_mode(mode);
    m_should_close_file_descriptor = should_close;
    return true;
}

bool DeprecatedFile::open(OpenMode mode)
{
    return open_impl(mode, 0666);
}

bool DeprecatedFile::open_impl(OpenMode mode, mode_t permissions)
{
    VERIFY(!m_filename.is_null());
    int flags = 0;
    if (has_flag(mode, OpenMode::ReadOnly) && has_flag(mode, OpenMode::WriteOnly)) {
        flags |= O_RDWR | O_CREAT;
    } else if (has_flag(mode, OpenMode::ReadOnly)) {
        flags |= O_RDONLY;
    } else if (has_flag(mode, OpenMode::WriteOnly)) {
        flags |= O_WRONLY | O_CREAT;
        bool should_truncate = !(has_flag(mode, OpenMode::Append) || has_flag(mode, OpenMode::MustBeNew));
        if (should_truncate)
            flags |= O_TRUNC;
    }
    if (has_flag(mode, OpenMode::Append))
        flags |= O_APPEND;
    if (has_flag(mode, OpenMode::Truncate))
        flags |= O_TRUNC;
    if (has_flag(mode, OpenMode::MustBeNew))
        flags |= O_EXCL;
    if (!has_flag(mode, OpenMode::KeepOnExec))
        flags |= O_CLOEXEC;
    int fd = ::open(m_filename.characters(), flags, permissions);
    if (fd < 0) {
        set_error(errno);
        return false;
    }

    set_fd(fd);
    set_mode(mode);
    return true;
}

int DeprecatedFile::leak_fd()
{
    m_should_close_file_descriptor = ShouldCloseFileDescriptor::No;
    return fd();
}

bool DeprecatedFile::is_device() const
{
    struct stat st;
    if (fstat(fd(), &st) < 0)
        return false;
    return S_ISBLK(st.st_mode) || S_ISCHR(st.st_mode);
}

bool DeprecatedFile::is_block_device() const
{
    struct stat stat;
    if (fstat(fd(), &stat) < 0)
        return false;
    return S_ISBLK(stat.st_mode);
}

bool DeprecatedFile::is_char_device() const
{
    struct stat stat;
    if (fstat(fd(), &stat) < 0)
        return false;
    return S_ISCHR(stat.st_mode);
}

bool DeprecatedFile::is_directory() const
{
    struct stat st;
    if (fstat(fd(), &st) < 0)
        return false;
    return S_ISDIR(st.st_mode);
}

bool DeprecatedFile::is_link() const
{
    struct stat stat;
    if (fstat(fd(), &stat) < 0)
        return false;
    return S_ISLNK(stat.st_mode);
}

DeprecatedString DeprecatedFile::real_path_for(DeprecatedString const& filename)
{
    if (filename.is_null())
        return {};
    auto* path = realpath(filename.characters(), nullptr);
    DeprecatedString real_path(path);
    free(path);
    return real_path;
}

DeprecatedString DeprecatedFile::current_working_directory()
{
    char* cwd = getcwd(nullptr, 0);
    if (!cwd) {
        perror("getcwd");
        return {};
    }

    auto cwd_as_string = DeprecatedString(cwd);
    free(cwd);

    return cwd_as_string;
}

DeprecatedString DeprecatedFile::absolute_path(DeprecatedString const& path)
{
    if (!Core::System::stat(path).is_error())
        return DeprecatedFile::real_path_for(path);

    if (path.starts_with("/"sv))
        return LexicalPath::canonicalized_path(path);

    auto working_directory = DeprecatedFile::current_working_directory();
    auto full_path = LexicalPath::join(working_directory, path);

    return LexicalPath::canonicalized_path(full_path.string());
}

Optional<DeprecatedString> DeprecatedFile::resolve_executable_from_environment(StringView filename)
{
    if (filename.is_empty())
        return {};

    // Paths that aren't just a file name generally count as already resolved.
    if (filename.contains('/')) {
        if (access(DeprecatedString { filename }.characters(), X_OK) != 0)
            return {};

        return filename;
    }

    auto const* path_str = getenv("PATH");
    StringView path;
    if (path_str)
        path = { path_str, strlen(path_str) };
    if (path.is_empty())
        path = DEFAULT_PATH_SV;

    auto directories = path.split_view(':');

    for (auto directory : directories) {
        auto file = DeprecatedString::formatted("{}/{}", directory, filename);

        if (access(file.characters(), X_OK) == 0)
            return file;
    }

    return {};
};

}
