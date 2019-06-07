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
        auto* tm = localtime(&t);
        printf("%4u-%02u-%02u %02u:%02u:%02u\n",
            tm->tm_year + 1900,
            tm->tm_mon + 1,
            tm->tm_mday,
            tm->tm_hour,
            tm->tm_min,
            tm->tm_sec);
    };

    printf("Accessed: ");
    print_time(st.st_atime);
    printf("Modified: ");
    print_time(st.st_mtime);
    printf(" Changed: ");
    print_time(st.st_ctime);

    return 0;
}
