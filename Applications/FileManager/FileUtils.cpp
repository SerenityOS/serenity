#include "FileUtils.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/CDirIterator.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>

namespace FileUtils {

int delete_directory(String directory, String& file_that_caused_error)
{
    CDirIterator iterator(directory, CDirIterator::SkipDots);
    if (iterator.has_error()) {
        file_that_caused_error = directory;
        return -1;
    }

    while (iterator.has_next()) {
        auto file_to_delete = String::format("%s/%s", directory.characters(), iterator.next_path().characters());
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

    int src_fd = open(src_path.characters(), O_RDONLY);
    if (src_fd < 0) {
        return false;
    }

    struct stat src_stat;
    int rc = fstat(src_fd, &src_stat);
    if (rc < 0) {
        return false;
    }

    if (S_ISDIR(src_stat.st_mode)) {
        return copy_directory(src_path, dst_path);
    }
    return copy_file(src_path, dst_path, src_stat, src_fd);
}

bool copy_directory(const String& src_path, const String& dst_path)
{
    int rc = mkdir(dst_path.characters(), 0755);
    if (rc < 0) {
        return false;
    }
    CDirIterator di(src_path, CDirIterator::SkipDots);
    if (di.has_error()) {
        return false;
    }
    while (di.has_next()) {
        String filename = di.next_path();
        bool is_copied = copy_file_or_directory(
            String::format("%s/%s", src_path.characters(), filename.characters()),
            String::format("%s/%s", dst_path.characters(), filename.characters()));
        if (!is_copied) {
            return false;
        }
    }
    return true;
}

bool copy_file(const String& src_path, const String& dst_path, const struct stat& src_stat, int src_fd)
{
    int dst_fd = creat(dst_path.characters(), 0666);
    if (dst_fd < 0) {
        if (errno != EISDIR) {
            return false;
        }
        StringBuilder builder;
        builder.appendf("%s/%s", dst_path, FileSystemPath(src_path).basename());
        String dst_path = builder.to_string();
        dst_fd = creat(dst_path.characters(), 0666);
        if (dst_fd < 0) {
            return false;
        }
    }

    if (src_stat.st_size > 0) {
        if (ftruncate(dst_fd, src_stat.st_size) < 0) {
            perror("cp: ftruncate");
            return false;
        }
    }

    for (;;) {
        char buffer[32768];
        ssize_t nread = read(src_fd, buffer, sizeof(buffer));
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

    close(src_fd);
    close(dst_fd);
    return true;
}

String get_duplicate_name(const String& path, int duplicate_count)
{
    if (duplicate_count == 0) {
        return path;
    }
    FileSystemPath fsp(path);
    StringBuilder duplicated_name;
    duplicated_name.append('/');
    for (int i = 0; i < fsp.parts().size() - 1; ++i) {
        duplicated_name.appendf("%s/", fsp.parts()[i].characters());
    }
    auto prev_duplicate_tag = String::format("(%d)", duplicate_count);
    auto title = fsp.title();
    if (title.ends_with(prev_duplicate_tag)) {
        // remove the previous duplicate tag "(n)" so we can add a new tag.
        title = title.substring(0, title.length() - prev_duplicate_tag.length());
    }
    duplicated_name.appendf("%s (%d)", fsp.title().characters(), duplicate_count);
    if (!fsp.extension().is_empty()) {
        duplicated_name.appendf(".%s", fsp.extension().characters());
    }
    return duplicated_name.build();
}
}
