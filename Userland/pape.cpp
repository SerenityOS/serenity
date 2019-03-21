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
#include <LibGUI/GDesktop.h>
#include <LibGUI/GApplication.h>

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
    GApplication app(argc, argv);

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

int show_current()
{
    printf("%s\n", GDesktop::the().wallpaper().characters());
    return 0;
}

int set_pape(const char* name)
{
    StringBuilder builder;
    builder.append("/res/wallpapers/");
    builder.append(name);
    String path = builder.to_string();
    if (!GDesktop::the().set_wallpaper(path)) {
        fprintf(stderr, "pape: Failed to set wallpaper %s\n", path.characters());
        return 1;
    }
    return 0;
}
