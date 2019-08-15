#include <LibCore/CArgsParser.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    CArgsParser args_parser("mount");
    args_parser.add_arg("devname", "device path");
    args_parser.add_arg("mountpoint", "mount point");
    args_parser.add_arg("fstype", "file system type");
    CArgsParserResult args = args_parser.parse(argc, argv);

    if (argc < 3 || argc > 4) {
        args_parser.print_usage();
        return 0;
    }

    const char* devname = argv[1];
    const char* mountpoint = argv[2];
    const char* fstype = argc == 4 ? argv[3] : "ext2";

    if (mount(devname, mountpoint, fstype) < 0) {
        perror("mount");
        return 1;
    }

    return 0;
}
