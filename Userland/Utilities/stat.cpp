/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <grp.h>
#include <inttypes.h>
#include <pwd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

static int stat(const char* file, bool should_follow_links)
{
    struct stat st;
    int rc = should_follow_links ? stat(file, &st) : lstat(file, &st);
    if (rc < 0) {
        perror("lstat");
        return 1;
    }
    printf("    File: %s\n", file);
    printf("   Inode: %u\n", st.st_ino);
    if (S_ISCHR(st.st_mode) || S_ISBLK(st.st_mode))
        printf("  Device: %u,%u\n", major(st.st_rdev), minor(st.st_rdev));
    else
        printf("    Size: %" PRIi64 "\n", st.st_size);
    printf("   Links: %u\n", st.st_nlink);
    printf("  Blocks: %u\n", st.st_blocks);
    printf("     UID: %u", st.st_uid);
    if (auto* pwd = getpwuid(st.st_uid)) {
        printf(" (%s)", pwd->pw_name);
    }
    printf("\n");
    printf("     GID: %u", st.st_gid);
    if (auto* grp = getgrgid(st.st_gid)) {
        printf(" (%s)", grp->gr_name);
    }
    printf("\n");
    printf("    Mode: (%o/", st.st_mode);

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

    printf(")\n");

    auto print_time = [](time_t t) {
        printf("%s\n", Core::DateTime::from_timestamp(t).to_string().characters());
    };

    printf("Accessed: ");
    print_time(st.st_atime);
    printf("Modified: ");
    print_time(st.st_mtime);
    printf(" Changed: ");
    print_time(st.st_ctime);

    return 0;
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool should_follow_links = false;
    Vector<const char*> files;

    auto args_parser = Core::ArgsParser();
    args_parser.add_option(should_follow_links, "Follow links to files", nullptr, 'L');
    args_parser.add_positional_argument(files, "File(s) to stat", "file", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    int ret = 0;
    for (auto& file : files)
        ret |= stat(file, should_follow_links);

    return ret;
}
