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

#include "FileUtils.h"
#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/MessageBox.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>

namespace FileUtils {

void delete_paths(const Vector<String>& paths, bool should_confirm, GUI::Window* parent_window)
{
    String message;
    if (paths.size() == 1) {
        message = String::formatted("Really delete {}?", LexicalPath(paths[0]).basename());
    } else {
        message = String::formatted("Really delete {} files?", paths.size());
    }

    if (should_confirm) {
        auto result = GUI::MessageBox::show(parent_window,
            message,
            "Confirm deletion",
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel);
        if (result == GUI::MessageBox::ExecCancel)
            return;
    }

    for (auto& path : paths) {
        struct stat st;
        if (lstat(path.characters(), &st)) {
            GUI::MessageBox::show(parent_window,
                String::formatted("lstat({}) failed: {}", path, strerror(errno)),
                "Delete failed",
                GUI::MessageBox::Type::Error);
            break;
        }

        if (S_ISDIR(st.st_mode)) {
            String error_path;
            int error = FileUtils::delete_directory(path, error_path);

            if (error) {
                GUI::MessageBox::show(parent_window,
                    String::formatted("Failed to delete directory \"{}\": {}", error_path, strerror(error)),
                    "Delete failed",
                    GUI::MessageBox::Type::Error);
                break;
            }
        } else if (unlink(path.characters()) < 0) {
            int saved_errno = errno;
            GUI::MessageBox::show(parent_window,
                String::formatted("unlink(\"{}\") failed: {}", path, strerror(saved_errno)),
                "Delete failed",
                GUI::MessageBox::Type::Error);
            break;
        }
    }
}

int delete_directory(String directory, String& file_that_caused_error)
{
    Core::DirIterator iterator(directory, Core::DirIterator::SkipDots);
    if (iterator.has_error()) {
        file_that_caused_error = directory;
        return -1;
    }

    while (iterator.has_next()) {
        auto file_to_delete = String::formatted("{}/{}", directory, iterator.next_path());
        struct stat st;

        if (lstat(file_to_delete.characters(), &st)) {
            file_that_caused_error = file_to_delete;
            return errno;
        }

        if (S_ISDIR(st.st_mode)) {
            if (delete_directory(file_to_delete, file_to_delete)) {
                file_that_caused_error = file_to_delete;
                return errno;
            }
        } else if (unlink(file_to_delete.characters())) {
            file_that_caused_error = file_to_delete;
            return errno;
        }
    }

    if (rmdir(directory.characters())) {
        file_that_caused_error = directory;
        return errno;
    }

    return 0;
}

bool copy_file_or_directory(const String& src_path, const String& dst_path)
{
    int duplicate_count = 0;
    while (access(get_duplicate_name(dst_path, duplicate_count).characters(), F_OK) == 0) {
        ++duplicate_count;
    }
    if (duplicate_count != 0) {
        return copy_file_or_directory(src_path, get_duplicate_name(dst_path, duplicate_count));
    }

    auto source_or_error = Core::File::open(src_path, Core::IODevice::ReadOnly);
    if (source_or_error.is_error())
        return false;

    auto& source = *source_or_error.value();

    struct stat src_stat;
    int rc = fstat(source.fd(), &src_stat);
    if (rc < 0)
        return false;

    if (source.is_directory())
        return copy_directory(src_path, dst_path, src_stat);

    return copy_file(dst_path, src_stat, source);
}

bool copy_directory(const String& src_path, const String& dst_path, const struct stat& src_stat)
{
    int rc = mkdir(dst_path.characters(), 0755);
    if (rc < 0) {
        return false;
    }
    Core::DirIterator di(src_path, Core::DirIterator::SkipDots);
    if (di.has_error()) {
        return false;
    }
    while (di.has_next()) {
        String filename = di.next_path();
        bool is_copied = copy_file_or_directory(
            String::formatted("{}/{}", src_path, filename),
            String::formatted("{}/{}", dst_path, filename));
        if (!is_copied) {
            return false;
        }
    }

    auto my_umask = umask(0);
    umask(my_umask);
    rc = chmod(dst_path.characters(), src_stat.st_mode & ~my_umask);
    if (rc < 0) {
        return false;
    }
    return true;
}

bool copy_file(const String& dst_path, const struct stat& src_stat, Core::File& source)
{
    int dst_fd = creat(dst_path.characters(), 0666);
    if (dst_fd < 0) {
        if (errno != EISDIR) {
            return false;
        }
        auto dst_dir_path = String::formatted("{}/{}", dst_path, LexicalPath(source.filename()).basename());
        dst_fd = creat(dst_dir_path.characters(), 0666);
        if (dst_fd < 0) {
            return false;
        }
    }

    ScopeGuard close_fd_guard([dst_fd]() { close(dst_fd); });

    if (src_stat.st_size > 0) {
        if (ftruncate(dst_fd, src_stat.st_size) < 0) {
            perror("cp: ftruncate");
            return false;
        }
    }

    for (;;) {
        char buffer[32768];
        ssize_t nread = read(source.fd(), buffer, sizeof(buffer));
        if (nread < 0) {
            return false;
        }
        if (nread == 0)
            break;
        ssize_t remaining_to_write = nread;
        char* bufptr = buffer;
        while (remaining_to_write) {
            ssize_t nwritten = write(dst_fd, bufptr, remaining_to_write);
            if (nwritten < 0) {
                return false;
            }
            assert(nwritten > 0);
            remaining_to_write -= nwritten;
            bufptr += nwritten;
        }
    }

    auto my_umask = umask(0);
    umask(my_umask);
    int rc = fchmod(dst_fd, src_stat.st_mode & ~my_umask);
    if (rc < 0) {
        return false;
    }

    return true;
}

String get_duplicate_name(const String& path, int duplicate_count)
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
}
