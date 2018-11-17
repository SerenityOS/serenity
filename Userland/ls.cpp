#include <LibC/stdio.h>
#include <LibC/unistd.h>
#include <LibC/dirent.h>
#include <LibC/errno.h>
#include <LibC/string.h>

static int do_dir(const char* path);

int main(int argc, char** argv)
{
    if (argc == 2) {
        return do_dir(argv[1]);
    }
    return do_dir(".");
}

int do_dir(const char* path)
{
    DIR* dirp = opendir(path);
    if (!dirp) {
        perror("opendir");
        return 1;
    }
    bool colorize = true;
    char pathbuf[256];
    while (auto* de = readdir(dirp)) {
        sprintf(pathbuf, "%s/%s", path, de->d_name);

        struct stat st;
        int rc = lstat(pathbuf, &st);
        if (rc == -1) {
            printf("lstat(%s) failed: %s\n", pathbuf, strerror(errno));
            return 2;
        }

        printf("%08u ", de->d_ino);

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
        else if (S_ISREG(st.st_mode))
            printf("-");
        else
            printf("?");

        printf("%c%c%c%c%c%c%c%c",
            st.st_mode & S_IRUSR ? 'r' : '-',
            st.st_mode & S_IWUSR ? 'w' : '-',
            st.st_mode & S_IXUSR ? 'x' : '-',
            st.st_mode & S_IRGRP ? 'r' : '-',
            st.st_mode & S_IWGRP ? 'w' : '-',
            st.st_mode & S_IXGRP ? 'x' : '-',
            st.st_mode & S_IROTH ? 'r' : '-',
            st.st_mode & S_IWOTH ? 'w' : '-'
        );

        if (st.st_mode & S_ISVTX)
            printf("t");
        else
            printf("%c", st.st_mode & S_IXOTH ? 'x' : '-');

        printf(" %4u %4u", st.st_uid, st.st_gid);

        printf(" %10u  ", st.st_size);

        const char* beginColor = "";
        const char* endColor = "";

        if (colorize) {
            if (S_ISLNK(st.st_mode))
                beginColor = "\033[36;1m";
            else if (S_ISDIR(st.st_mode))
                beginColor = "\033[34;1m";
            else if (st.st_mode & 0111)
                beginColor = "\033[32;1m";
            else if (S_ISCHR(st.st_mode) || S_ISBLK(st.st_mode))
                beginColor = "\033[33;1m";
            endColor = "\033[0m";
        }

        printf("%s%s%s", beginColor, de->d_name, endColor);

        if (S_ISLNK(st.st_mode)) {
            char linkbuf[256];
            ssize_t nread = readlink(pathbuf, linkbuf, sizeof(linkbuf));
            if (nread < 0) {
                perror("readlink failed");
            } else {
                printf(" -> %s", linkbuf);
            }
        } else if (S_ISDIR(st.st_mode)) {
            printf("/");
        } else if (st.st_mode & 0111) {
            printf("*");
        }
        printf("\n");
    }
    return 0;
}
