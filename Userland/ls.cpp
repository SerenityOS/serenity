#include <stdio.h>
#include <unistd.h>
#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <getopt.h>
#include <time.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <AK/AKString.h>
#include <AK/Vector.h>

static int do_dir(const char* path);
static int do_dir_short(const char* path);

static bool flag_colorize = true;
static bool flag_long = false;
static bool flag_show_dotfiles = false;
static bool flag_show_inode = false;

int main(int argc, char** argv)
{
    static const char* valid_option_characters = "laiG";
    int opt;
    while ((opt = getopt(argc, argv, valid_option_characters)) != -1) {
        switch (opt) {
        case 'a':
            flag_show_dotfiles = true;
            break;
        case 'l':
            flag_long = true;
            break;
        case 'G':
            flag_colorize = false;
            break;
        case 'i':
            flag_show_inode = true;
            break;
        default:
            fprintf(stderr, "usage: ls [-%s] [path]\n", valid_option_characters);
            return 1;
        }
    }

    const char* path;

    if (optind >= argc)
        path = ".";
    else
        path = argv[optind];

    if (flag_long)
        return do_dir(path);
    return do_dir_short(path);
}

void get_geometry(int& rows, int& columns)
{
    struct winsize ws;
    ioctl(0, TIOCGWINSZ, &ws);
    rows = ws.ws_row;
    columns = ws.ws_col;
}

int print_name(struct stat& st, const char* name, const char* path_for_link_resolution = nullptr)
{
    int nprinted = strlen(name);
    if (!flag_colorize) {
        printf("%s", name);
    } else {
        const char* begin_color = "";
        const char* end_color = "\033[0m";

        if (S_ISLNK(st.st_mode))
            begin_color = "\033[36;1m";
        else if (S_ISDIR(st.st_mode))
            begin_color = "\033[34;1m";
        else if (st.st_mode & 0111)
            begin_color = "\033[32;1m";
        else if (S_ISSOCK(st.st_mode))
            begin_color = "\033[35;1m";
        else if (S_ISCHR(st.st_mode) || S_ISBLK(st.st_mode))
            begin_color = "\033[33;1m";
        printf("%s%s%s", begin_color, name, end_color);
    }
    if (S_ISLNK(st.st_mode)) {
        if (path_for_link_resolution) {
            char linkbuf[256];
            ssize_t nread = readlink(path_for_link_resolution, linkbuf, sizeof(linkbuf));
            if (nread < 0) {
                perror("readlink failed");
            } else {
                nprinted += printf(" -> %s", linkbuf);
            }
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

int do_dir(const char* path)
{
    DIR* dirp = opendir(path);
    if (!dirp) {
        perror("opendir");
        return 1;
    }
    char pathbuf[PATH_MAX];

    while (auto* de = readdir(dirp)) {
        if (de->d_name[0] == '.' && !flag_show_dotfiles)
            continue;
        sprintf(pathbuf, "%s/%s", path, de->d_name);

        struct stat st;
        int rc = lstat(pathbuf, &st);
        if (rc == -1) {
            printf("lstat(%s) failed: %s\n", pathbuf, strerror(errno));
            return 2;
        }

        if (flag_show_inode)
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
            st.st_mode & S_IWOTH ? 'w' : '-'
        );

        if (st.st_mode & S_ISVTX)
            printf("t");
        else
            printf("%c", st.st_mode & S_IXOTH ? 'x' : '-');

        printf(" %4u %4u", st.st_uid, st.st_gid);

        if (S_ISCHR(st.st_mode) || S_ISBLK(st.st_mode))
            printf("  %4u,%4u ", major(st.st_rdev), minor(st.st_rdev));
        else
            printf(" %10u ", st.st_size);

        auto* tm = localtime(&st.st_mtime);
        printf("  %4u-%02u-%02u %02u:%02u:%02u  ",
            tm->tm_year + 1900,
            tm->tm_mon + 1,
            tm->tm_mday,
            tm->tm_hour,
            tm->tm_min,
            tm->tm_sec);

        print_name(st, de->d_name, pathbuf);

        printf("\n");
    }
    closedir(dirp);
    return 0;
}


int print_filesystem_object_short(const char *path, const char *name, int *nprinted) {
    struct stat st;
    int rc = lstat(path, &st);
    if (rc == -1) {
        printf("lstat(%s) failed: %s\n", path, strerror(errno));
        return 2;
    }

    *nprinted = print_name(st, name);
    return 0;
}

int do_dir_short(const char* path)
{
    int rows;
    int columns;
    get_geometry(rows, columns);

    DIR* dirp = opendir(path);
    if (!dirp) {
        if (errno == ENOTDIR) {
          int nprinted;
          print_filesystem_object_short(path, path, &nprinted);
          printf("\n");
          return 0;
        } else {
          perror("opendir");
          return 1;
        }
    }

    Vector<String, 1024> names;
    int longest_name = 0;
    while (auto* de = readdir(dirp)) {
        if (de->d_name[0] == '.' && !flag_show_dotfiles)
            continue;
        names.append(de->d_name);
        if (names.last().length() > longest_name)
            longest_name = names.last().length();
    }
    closedir(dirp);

    int printed_on_row = 0;
    int nprinted;

    for (int i = 0; i < names.size(); ++i) {
        auto& name = names[i];
        char pathbuf[256];
        sprintf(pathbuf, "%s/%s", path, name.characters());

        if (print_filesystem_object_short(pathbuf, name.characters(), &nprinted) == 2) {
          return 2;
        }
        int column_width = 14;
        printed_on_row += column_width;

        for (int i = nprinted; i < column_width; ++i)
            printf(" ");
        if ((printed_on_row + column_width) >= columns) {
            if (i != names.size() - 1)
                printf("\n");
            printed_on_row = 0;
        }
    }
    printf("\n");

    return 0;
}
