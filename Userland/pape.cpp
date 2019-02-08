#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <getopt.h>
#include <fcntl.h>
#include <dirent.h>
#include <AK/AKString.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <AK/FileSystemPath.h>

static bool flag_show_all = false;
static int show_all();
static int show_current();
static int set_pape(const char*);

static void usage()
{
    printf("usage: pape [-a] [name]\n");
}

int main(int argc, char** argv)
{
    int opt;
    while ((opt = getopt(argc, argv, "a")) != -1) {
        switch (opt) {
        case 'a':
            flag_show_all = true;
            break;
        default:
            usage();
            return 0;
        }
    }

    if (flag_show_all)
        return show_all();

    if (argc == 1) {
        show_current();
        return 0;
    }

    if (optind >= argc) {
        usage();
        return 0;
    }

    return set_pape(argv[optind]);
}

int show_all()
{
    DIR* dirp = opendir("/res/wallpapers");
    if (!dirp) {
        perror("opendir");
        return 1;
    }
    while (auto* de = readdir(dirp)) {
        if (de->d_name[0] == '.')
            continue;
        printf("%s\n", de->d_name);
    }
    closedir(dirp);
    return 0;
}

static String read_var(const String& name)
{
    StringBuilder builder;
    builder.append("/proc/sys/");
    builder.append(name);
    auto path = builder.to_string();
    int fd = open(path.characters(), O_RDONLY);
    if (fd < 0) {
        perror("open");
        exit(1);
    }
    char buffer[BUFSIZ];
    int nread = read(fd, buffer, sizeof(buffer));
    close(fd);
    if (nread < 0) {
        perror("read");
        exit(1);
    }
    return String(buffer, nread, Chomp);
}

static void write_var(const String& name, const String& value)
{
    StringBuilder builder;
    builder.append("/proc/sys/");
    builder.append(name);
    auto path = builder.to_string();
    int fd = open(path.characters(), O_WRONLY);
    if (fd < 0) {
        perror("open");
        exit(1);
    }
    int nwritten = write(fd, value.characters(), value.length());
    if (nwritten < 0) {
        perror("read");
        exit(1);
    }
    close(fd);
}

int show_current()
{
    FileSystemPath path(read_var("wm_wallpaper"));
    printf("%s\n", path.basename().characters());
    return 0;
}

int set_pape(const char* name)
{
    StringBuilder builder;
    builder.append("/res/wallpapers/");
    builder.append(name);

    write_var("wm_wallpaper", builder.to_string());
    return 0;
}
