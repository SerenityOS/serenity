#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static int pid_of(const String& process_name, bool single_shot, bool omit_pid, pid_t pid)
{
    bool displayed_at_least_one = false;

    auto processes = CProcessStatisticsReader().get_all();

    for (auto& it : processes) {
        if (it.value.name == process_name) {
            if (!omit_pid || it.value.pid != pid) {
                printf("%d ", it.value.pid);
                displayed_at_least_one = true;

                if (single_shot)
                    break;
            }
        }
    }

    if (displayed_at_least_one)
        printf("\n");

    return 0;
}

int main(int argc, char** argv)
{
    CArgsParser args_parser("pidof");

    args_parser.add_arg("s", "Single shot - this instructs the program to only return one pid");
    args_parser.add_arg("o", "pid", "Tells pidof to omit processes with that pid. The special pid %PPID can be used to name the parent process of the pidof program.");

    CArgsParserResult args = args_parser.parse(argc, argv);

    bool s_arg = args.is_present("s");
    bool o_arg = args.is_present("o");
    pid_t pid = 0;

    if (o_arg) {
        bool ok = false;
        String pid_str = args.get("o");

        if (pid_str == "%PPID")
            pid = getppid();
        else
            pid = pid_str.to_uint(ok);
    }

    // We should have one single value : the process name
    Vector<String> values = args.get_single_values();
    if (values.size() == 0) {
        args_parser.print_usage();
        return 0;
    }

    return pid_of(values[0], s_arg, o_arg, pid);
}
