#include <LibCore/CArgsParser.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    CArgsParser args_parser("umount");
    args_parser.add_arg("mountpoint", "mount point");
    CArgsParserResult args = args_parser.parse(argc, argv);

    if (argc == 2) {
        if (umount(argv[1]) < 0) {
            perror("umount");
            return 1;
        }
    } else {
        args_parser.print_usage();
        return 0;
    }

    return 0;
}
