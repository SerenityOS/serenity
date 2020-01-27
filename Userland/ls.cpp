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

#include <AK/HashMap.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CDirIterator.h>
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

static int do_file_system_object_long(const char* path);
static int do_file_system_object_short(const char* path);

static bool flag_colorize = true;
static bool flag_long = false;
static bool flag_show_dotfiles = false;
static bool flag_show_inode = false;
static bool flag_print_numeric = false;
static bool flag_human_readable = false;
static bool flag_sort_by_timestamp = false;
static bool flag_reverse_sort = false;

static size_t terminal_rows = 0;
static size_t terminal_columns = 0;
static bool output_is_terminal = false;

static HashMap<uid_t, String> users;
static HashMap<gid_t, String> groups;

int main(int argc, char** argv)
{
    if (pledge("stdio rpath tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    struct winsize ws;
    int rc = ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws);
    if (rc == 0) {
        terminal_rows = ws.ws_row;
        terminal_columns = ws.ws_col;
        output_is_terminal = true;
    }

    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> paths;

    CArgsParser args_parser;
    args_parser.add_option(flag_show_dotfiles, "Show dotfiles", "all", 'a');
    args_parser.add_option(flag_long, "Display long info", "long", 'l');
    args_parser.add_option(flag_sort_by_timestamp, "Sort files by timestamp", nullptr, 't');
    args_parser.add_option(flag_reverse_sort, "Reverse sort order", "reverse", 'r');
    args_parser.add_option(flag_colorize, "Use pretty colors", nullptr, 'G');
    args_parser.add_option(flag_show_inode, "Show inode ids", "inode", 'i');
    args_parser.add_option(flag_print_numeric, "In long format, display numeric UID/GID", "numeric-uid-gid", 'n');
    args_parser.add_option(flag_human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.add_positional_argument(paths, "Directory to list", "path", CArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (flag_long) {
        setpwent();
        for (auto* pwd = getpwent(); pwd; pwd = getpwent())
            users.set(pwd->pw_uid, pwd->pw_name);
        endpwent();
        setgrent();
        for (auto* grp = getgrent(); grp; grp = getgrent())
            groups.set(grp->gr_gid, grp->gr_name);
        endgrent();
    }

    auto do_file_system_object = [&](const char* path) {
        if (flag_long)
            return do_file_system_object_long(path);
        return do_file_system_object_short(path);
    };

    int status = 0;
    if (paths.is_empty()) {
        status = do_file_system_object(".");
    } else if (paths.size() == 1) {
        status = do_file_system_object(paths[0]);
    } else {
        for (auto& path : paths) {
            printf("%s:\n", path);
            status = do_file_system_object(path);
        }
    }
    return status;
}

int print_escaped(const char* name)
{
    int printed = 0;

    for (int i = 0; name[i] != '\0'; i++) {
        if (isprint(name[i])) {
            putchar(name[i]);
            printed++;
        } else {
            printed += printf("\\%03d", name[i]);
        }
    }

    return printed;
}

int print_name(const struct stat& st, const String& name, const char* path_for_link_resolution = nullptr)
{
    int nprinted = 0;

    if (!flag_colorize || !output_is_terminal) {
        nprinted = printf("%s", name.characters());
    } else {
        const char* begin_color = "";
        const char* end_color = "\033[0m";

        if (st.st_mode & S_ISVTX)
            begin_color = "\033[42;30;1m";
        else if (st.st_mode & S_ISUID)
            begin_color = "\033[41;1m";
        else if (S_ISLNK(st.st_mode))
            begin_color = "\033[36;1m";
        else if (S_ISDIR(st.st_mode))
            begin_color = "\033[34;1m";
        else if (st.st_mode & 0111)
            begin_color = "\033[32;1m";
        else if (S_ISSOCK(st.st_mode))
            begin_color = "\033[35;1m";
        else if (S_ISCHR(st.st_mode) || S_ISBLK(st.st_mode))
            begin_color = "\033[33;1m";
        printf("%s", begin_color);
        nprinted = print_escaped(name.characters());
        printf("%s", end_color);
    }
    if (S_ISLNK(st.st_mode)) {
        if (path_for_link_resolution) {
            char linkbuf[PATH_MAX];
            ssize_t nread = readlink(path_for_link_resolution, linkbuf, sizeof(linkbuf));
            if (nread < 0)
                perror("readlink failed");
            else
                nprinted += printf(" -> ") + print_escaped(linkbuf);
        } else {
            nprinted += printf("@");
        }
    } else if (S_ISDIR(st.st_mode)) {
        nprinted += printf("/");
    } else if (st.st_mode & 0111) {
        nprinted += printf("*");
    }
    return nprinted;
}

// FIXME: Remove this hackery once printf() supports floats.
// FIXME: Also, we should probably round the sizes in ls -lh output.
static String number_string_with_one_decimal(float number, const char* suffix)
{
    float decimals = number - (int)number;
    return String::format("%d.%d%s", (int)number, (int)(decimals * 10), suffix);
}

static String human_readable_size(size_t size)
{
    if (size < 1 * KB)
        return String::number(size);
    if (size < 1 * MB)
        return number_string_with_one_decimal((float)size / (float)KB, "K");
    if (size < 1 * GB)
        return number_string_with_one_decimal((float)size / (float)MB, "M");
    return number_string_with_one_decimal((float)size / (float)GB, "G");
}

bool print_filesystem_object(const String& path, const String& name, const struct stat& st)
{
    if (flag_show_inode)
        printf("%08u ", st.st_ino);

    if (S_ISDIR(st.st_mode))
        printf("d");
    else if (S_ISLNK(st.st_mode))
        printf("l");
    else if (S_ISBLK(st.st_mode))
        printf("b");
    else if (S_ISCHR(st.st_mode))
        printf("c");
    else if (S_ISFIFO(st.st_mode))
        printf("f");
    else if (S_ISSOCK(st.st_mode))
        printf("s");
    else if (S_ISREG(st.st_mode))
        printf("-");
    else
        printf("?");

    printf("%c%c%c%c%c%c%c%c",
        st.st_mode & S_IRUSR ? 'r' : '-',
        st.st_mode & S_IWUSR ? 'w' : '-',
        st.st_mode & S_ISUID ? 's' : (st.st_mode & S_IXUSR ? 'x' : '-'),
        st.st_mode & S_IRGRP ? 'r' : '-',
        st.st_mode & S_IWGRP ? 'w' : '-',
        st.st_mode & S_ISGID ? 's' : (st.st_mode & S_IXGRP ? 'x' : '-'),
        st.st_mode & S_IROTH ? 'r' : '-',
        st.st_mode & S_IWOTH ? 'w' : '-');

    if (st.st_mode & S_ISVTX)
        printf("t");
    else
        printf("%c", st.st_mode & S_IXOTH ? 'x' : '-');

    auto username = users.get(st.st_uid);
    auto groupname = groups.get(st.st_gid);
    if (!flag_print_numeric && username.has_value()) {
        printf(" %7s", username.value().characters());
    } else {
        printf(" %7u", st.st_uid);
    }
    if (!flag_print_numeric && groupname.has_value()) {
        printf(" %7s", groupname.value().characters());
    } else {
        printf(" %7u", st.st_gid);
    }

    if (S_ISCHR(st.st_mode) || S_ISBLK(st.st_mode)) {
        printf("  %4u,%4u ", major(st.st_rdev), minor(st.st_rdev));
    } else {
        if (flag_human_readable) {
            ASSERT(st.st_size > 0);
            printf(" %10s ", human_readable_size((size_t)st.st_size).characters());
        } else {
            printf(" %10u ", st.st_size);
        }
    }

    auto* tm = localtime(&st.st_mtime);
    printf("  %4u-%02u-%02u %02u:%02u:%02u  ",
        tm->tm_year + 1900,
        tm->tm_mon + 1,
        tm->tm_mday,
        tm->tm_hour,
        tm->tm_min,
        tm->tm_sec);

    print_name(st, name, path.characters());

    printf("\n");
    return true;
}

int do_file_system_object_long(const char* path)
{
    CDirIterator di(path, !flag_show_dotfiles ? CDirIterator::SkipDots : CDirIterator::Flags::NoFlags);
    if (di.has_error()) {
        if (di.error() == ENOTDIR) {
            struct stat stat;
            int rc = lstat(path, &stat);
            if (rc < 0) {
                perror("lstat");
                memset(&stat, 0, sizeof(stat));
            }
            if (print_filesystem_object(path, path, stat))
                return 0;
            return 2;
        }
        fprintf(stderr, "%s: %s\n", path, di.error_string());
        return 1;
    }

    struct FileMetadata {
        String name;
        String path;
        struct stat stat;
    };

    Vector<FileMetadata> files;
    while (di.has_next()) {
        FileMetadata metadata;
        metadata.name = di.next_path();
        ASSERT(!metadata.name.is_empty());
        if (metadata.name[0] == '.' && !flag_show_dotfiles)
            continue;
        StringBuilder builder;
        builder.append(path);
        builder.append('/');
        builder.append(metadata.name);
        metadata.path = builder.to_string();
        ASSERT(!metadata.path.is_null());
        int rc = lstat(metadata.path.characters(), &metadata.stat);
        if (rc < 0) {
            perror("lstat");
            memset(&metadata.stat, 0, sizeof(metadata.stat));
        }
        files.append(move(metadata));
    }

    quick_sort(files.begin(), files.end(), [](auto& a, auto& b) {
        if (flag_sort_by_timestamp) {
            if (flag_reverse_sort)
                return a.stat.st_mtime > b.stat.st_mtime;
            return a.stat.st_mtime < b.stat.st_mtime;
        }
        // Fine, sort by name then!
        if (flag_reverse_sort)
            return a.name > b.name;
        return a.name < b.name;
    });

    for (auto& file : files) {
        if (!print_filesystem_object(file.path, file.name, file.stat))
            return 2;
    }
    return 0;
}

bool print_filesystem_object_short(const char* path, const char* name, int* nprinted)
{
    struct stat st;
    int rc = lstat(path, &st);
    if (rc == -1) {
        printf("lstat(%s) failed: %s\n", path, strerror(errno));
        return false;
    }

    *nprinted = print_name(st, name);
    return true;
}

int do_file_system_object_short(const char* path)
{
    CDirIterator di(path, !flag_show_dotfiles ? CDirIterator::SkipDots : CDirIterator::Flags::NoFlags);
    if (di.has_error()) {
        if (di.error() == ENOTDIR) {
            int nprinted;
            bool status = print_filesystem_object_short(path, path, &nprinted);
            printf("\n");
            if (status)
                return 0;
            return 2;
        }
        fprintf(stderr, "%s: %s\n", path, di.error_string());
        return 1;
    }

    Vector<String> names;
    size_t longest_name = 0;
    while (di.has_next()) {
        String name = di.next_path();
        names.append(name);
        if (names.last().length() > longest_name)
            longest_name = name.length();
    }
    quick_sort(names.begin(), names.end(), [](auto& a, auto& b) { return a < b; });

    size_t printed_on_row = 0;
    int nprinted;
    for (int i = 0; i < names.size(); ++i) {
        auto& name = names[i];
        StringBuilder builder;
        builder.append(path);
        builder.append('/');
        builder.append(name);
        if (!print_filesystem_object_short(builder.to_string().characters(), name.characters(), &nprinted))
            return 2;
        int offset = 0;
        if (terminal_columns > longest_name)
            offset = terminal_columns % longest_name / (terminal_columns / longest_name);

        // The offset must be at least 2 because:
        // - With each file an additional char is printed e.g. '@','*'.
        // - Each filename must be separated by a space.
        size_t column_width = longest_name + (offset > 0 ? offset : 2);
        printed_on_row += column_width;

        for (int j = nprinted; i != (names.size() - 1) && j < (int)column_width; ++j)
            printf(" ");
        if ((printed_on_row + column_width) >= terminal_columns) {
            printf("\n");
            printed_on_row = 0;
        }
    }
    if (printed_on_row)
        printf("\n");
    return 0;
}
