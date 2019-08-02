#include <LibCore/CArgsParser.h>
#include <stdio.h>
#include <unistd.h>

// This version of 'mount' must have the following arguments
// 'mount <device_path> <mount_point>
// It can currently only mount _physical_ devices found in /dev
//
// Currently, it is only possible to mount ext2 devices. Sorry! :^)
int main(int argc, char** argv)
{
    CArgsParser args_parser("mount");
    args_parser.add_arg("devname", "device path");
    args_parser.add_arg("mountpoint", "mount point");
    CArgsParserResult args = args_parser.parse(argc, argv);

    if (argc == 3) {
        // Let's use lstat so we can convert devname into a major/minor device pair!
        if (mount(argv[1], argv[2]) < 0) {
            perror("mount");
            return 1;
        }
    } else {
        args_parser.print_usage();
        return 0;
    }

    return 0;
}
