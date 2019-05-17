#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <dirent.h>
#include <AK/AKString.h>
#include <AK/ArgsParser.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <AK/FileSystemPath.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GApplication.h>

static int handle_show_all()
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

static int handle_show_current()
{
    printf("%s\n", GDesktop::the().wallpaper().characters());
    return 0;
}

static int handle_set_pape(const String& name)
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
};

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    AK::ArgsParser args_parser("pape");

    args_parser.add_arg("a", "show all wallpapers");
    args_parser.add_arg("c", "show current wallpaper");
    args_parser.add_single_value("name");

    AK::ArgsParserResult args = args_parser.parse(argc, (const char**)argv);

    if (args.is_present("a"))
        return handle_show_all();
    else if (args.is_present("c"))
        return handle_show_current();

    Vector<String> values = args.get_single_values();
    if (values.size() != 1) {
        args_parser.print_usage();
        return 0;
    }

    return handle_set_pape(values[0]);
}

