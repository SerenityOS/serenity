#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <AK/AKString.h>
#include <AK/Vector.h>
#include <AK/ArgsParser.h>
#include <AK/HashMap.h>

static int pid_of(const String& process_name, bool single_shot, bool omit_pid, pid_t pid)
{
    bool displayed_at_least_one = false;

    HashMap<pid_t, CProcessStatistics> processes = CProcessStatisticsReader().get_map();
    
    for (auto& it : processes) {
        if (it.value.name == process_name) {    
            if (!omit_pid || (omit_pid && it.value.pid != pid)) {
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
    AK::ArgsParser args_parser("pidof", "-");

    args_parser.add_arg("s", "Single shot - this instructs the program to only return one pid");
    args_parser.add_arg("o", "pid", "Tells pidof to omit processes with that pid. The special pid %PPID can be used to name the parent process of the pidof program.");

    AK::ArgsParserResult args = args_parser.parse(argc, (const char**)argv);
    
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
