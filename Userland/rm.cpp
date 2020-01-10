#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CDirIterator.h>
#include <dirent.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

int remove(bool recursive, const char* path)
{
    struct stat path_stat;
    if (lstat(path, &path_stat) < 0) {
        perror("lstat");
        return 1;
    }

    if (S_ISDIR(path_stat.st_mode) && recursive) {
        DIR* derp = opendir(path);
        if (!derp) {
            return 1;
        }

        while (auto* de = readdir(derp)) {
            if (strcmp(de->d_name, ".") != 0 && strcmp(de->d_name, "..") != 0) {
                StringBuilder builder;
                builder.append(path);
                builder.append('/');
                builder.append(de->d_name);
                int s = remove(true, builder.to_string().characters());
                if (s < 0)
                    return s;
            }
        }
        int s = rmdir(path);
        if (s < 0) {
            perror("rmdir");
            return 1;
        }
    } else {
        int rc = unlink(path);
        if (rc < 0) {
            perror("unlink");
            return 1;
        }
    }
    return 0;
}

int main(int argc, char** argv)
{
    CArgsParser args_parser("rm");
    args_parser.add_arg("r", "Delete directory recursively.");
    args_parser.add_required_single_value("path");

    CArgsParserResult args = args_parser.parse(argc, argv);
    Vector<String> values = args.get_single_values();
    if (values.size() == 0) {
        args_parser.print_usage();
        return 1;
    }

    return remove(args.is_present("r"), values[0].characters());
}
