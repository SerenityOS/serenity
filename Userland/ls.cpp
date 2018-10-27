#include <LibC/stdio.h>
#include <LibC/unistd.h>
#include <LibC/dirent.h>
#include <LibC/errno.h>
#include <LibC/string.h>

int main(int c, char** v)
{
    DIR* dirp = opendir(".");
    if (!dirp) {
        perror("opendir failed");
        return 1;
    }
    char pathbuf[256];
    while (auto* de = readdir(dirp)) {
        sprintf(pathbuf, "%s", de->d_name);

        stat st;
        int rc = lstat(pathbuf, &st);
        if (rc == -1) {
            printf("lstat(%s) failed: %s\n", pathbuf, strerror(errno));
            return 2;
        }

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

        printf(" i:%x ", de->d_ino);
        printf(" %x  ", st.st_size);
        printf("%s%c", de->d_name, S_ISDIR(st.st_mode) ? '/' : ' ');
        printf("\n");
    }
    return 0;
}
