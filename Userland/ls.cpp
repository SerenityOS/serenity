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
#include <AK/NumberFormat.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
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

static bool flag_colorize = false;
static bool flag_long = false;
static bool flag_show_dotfiles = false;
static bool flag_show_inode = false;
static bool flag_print_numeric = false;
static bool flag_human_readable = false;
static bool flag_sort_by_timestamp = false;
static bool flag_reverse_sort = false;
static bool flag_disable_hyperlinks = false;

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
    if (!isatty(STDOUT_FILENO)) {
        flag_disable_hyperlinks = true;
    } else {
        flag_colorize = true;
    }

    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(flag_show_dotfiles, "Show dotfiles", "all", 'a');
    args_parser.add_option(flag_long, "Display long info", "long", 'l');
    args_parser.add_option(flag_sort_by_timestamp, "Sort files by timestamp", nullptr, 't');
    args_parser.add_option(flag_reverse_sort, "Reverse sort order", "reverse", 'r');
    args_parser.add_option(flag_colorize, "Use pretty colors", nullptr, 'G');
    args_parser.add_option(flag_show_inode, "Show inode ids", "inode", 'i');
    args_parser.add_option(flag_print_numeric, "In long format, display numeric UID/GID", "numeric-uid-gid", 'n');
    args_parser.add_option(flag_human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.add_option(flag_disable_hyperlinks, "Disable hyperlinks", "no-hyperlinks", 'K');
    args_parser.add_positional_argument(paths, "Directory to list", "path", Core::ArgsParser::Required::No);
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
            status = do_file_system_object(path);
        }
    }
    return status;
}

static int print_escaped(const char* name)
{
    int printed = 0;

    Utf8View utf8_name(name);
    if (utf8_name.validate()) {
        printf("%s", name);
        return utf8_name.length_in_code_points();
    }

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

static String& hostname()
{
    static String s_hostname;
    if (s_hostname.is_null()) {
        char buffer[HOST_NAME_MAX];
        if (gethostname(buffer, sizeof(buffer)) == 0)
            s_hostname = buffer;
        else
            s_hostname = "localhost";
    }
    return s_hostname;
}

static size_t print_name(const struct stat& st, const String& name, const char* path_for_link_resolution, const char* path_for_hyperlink)
{
    if (!flag_disable_hyperlinks) {
        if (auto* full_path = realpath(path_for_hyperlink, nullptr)) {
            printf("\033]8;;file://%s%s\033\\", hostname().characters(), full_path);
            free(full_path);
        }
    }

    size_t nprinted = 0;

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
            auto link_destination = Core::File::read_link(path_for_link_resolution);
            if (link_destination.is_null()) {
                perror("readlink");
            } else {
                nprinted += printf(" -> ") + print_escaped(link_destination.characters());
            }
        } else {
            nprinted += printf("@");
        }
    } else if (S_ISDIR(st.st_mode)) {
        nprinted += printf("/");
    } else if (st.st_mode & 0111) {
        nprinted += printf("*");
    }

    if (!flag_disable_hyperlinks) {
        printf("\033]8;;\033\\");
    }

    return nprinted;
}

static bool print_filesystem_object(const String& path, const String& name, const struct stat& st)
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
            printf(" %10s ", human_readable_size((size_t)st.st_size).characters());
        } else {
            printf(" %10zd ", st.st_size);
        }
    }

    printf("  %s  ", Core::DateTime::from_timestamp(st.st_mtime).to_string().characters());

    print_name(st, name, path.characters(), path.characters());

    printf("\n");
    return true;
}

static int do_file_system_object_long(const char* path)
{
    Core::DirIterator di(path, !flag_show_dotfiles ? Core::DirIterator::SkipDots : Core::DirIterator::Flags::NoFlags);
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

    quick_sort(files, [](auto& a, auto& b) {
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

static bool print_filesystem_object_short(const char* path, const char* name, size_t* nprinted)
{
    struct stat st;
    int rc = lstat(path, &st);
    if (rc == -1) {
        printf("lstat(%s) failed: %s\n", path, strerror(errno));
        return false;
    }

    *nprinted = print_name(st, name, nullptr, path);
    return true;
}

int do_file_system_object_short(const char* path)
{
    Core::DirIterator di(path, !flag_show_dotfiles ? Core::DirIterator::SkipDots : Core::DirIterator::Flags::NoFlags);
    if (di.has_error()) {
        if (di.error() == ENOTDIR) {
            size_t nprinted = 0;
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
    quick_sort(names);

    size_t printed_on_row = 0;
    size_t nprinted = 0;
    for (size_t i = 0; i < names.size(); ++i) {
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
        size_t column_width = longest_name + max(offset, 2);
        printed_on_row += column_width;

        for (size_t j = nprinted; i != (names.size() - 1) && j < column_width; ++j)
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
