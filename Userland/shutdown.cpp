#include <LibCore/CArgsParser.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    CArgsParser args_parser("shutdown");
    args_parser.add_arg("n", "shut down now");
    CArgsParserResult args = args_parser.parse(argc, argv);

    if (args.is_present("n")) {
        if (halt() < 0) {
            perror("shutdown");
            return 1;
        }
    } else {
        args_parser.print_usage();
        return 0;
    }
}
