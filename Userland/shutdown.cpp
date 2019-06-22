#include <Kernel/Syscall.h>
#include <LibCore/CArgsParser.h>

int main(int argc, char** argv)
{
    CArgsParser args_parser("shutdown");
    args_parser.add_arg("n", "shut down now");
    CArgsParserResult args = args_parser.parse(argc, argv);

    if (args.is_present("n")) {
        syscall(SC_halt);
        return 0;
    } else {
        args_parser.print_usage();
        return 0;
    }
}
