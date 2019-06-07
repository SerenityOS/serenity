#include <LibCore/CArgsParser.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    CArgsParser args_parser("ln");

    args_parser.add_arg("s", "create a symlink");
    args_parser.add_required_single_value("target");
    args_parser.add_required_single_value("link-path");

    CArgsParserResult args = args_parser.parse(argc, (const char**)argv);
    Vector<String> values = args.get_single_values();
    if (values.size() == 0) {
        args_parser.print_usage();
        return 0;
    }

    if (args.is_present("s")) {
        int rc = symlink(values[0].characters(), values[1].characters());
        if (rc < 0) {
            perror("symlink");
            return 1;
        }
        return 0;
    }

    int rc = link(values[0].characters(), values[1].characters());
    if (rc < 0) {
        perror("link");
        return 1;
    }
    return 0;
}
