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

#include <LibCore/DateTime.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc == 1) {
        printf("stat <file>\n");
        return 1;
    }
    struct stat st;
    int rc = lstat(argv[1], &st);
    if (rc < 0) {
        perror("lstat");
        return 1;
    }
    printf("    File: %s\n", argv[1]);
    printf("   Inode: %u\n", st.st_ino);
    if (S_ISCHR(st.st_mode) || S_ISBLK(st.st_mode))
        printf("  Device: %u,%u\n", major(st.st_rdev), minor(st.st_rdev));
    else
        printf("    Size: %u\n", st.st_size);
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
