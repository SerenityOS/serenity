#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <LibCore/CFile.h>
#include <AK/AKString.h>
#include <AK/Vector.h>
#include <AK/ArgsParser.h>

static int pid_of(const String& process_name, bool single_shot, bool omit_pid, pid_t pid)
{
    bool displayed_at_least_one = false;

    CFile file("/proc/all");
    if (!file.open(CIODevice::ReadOnly)) {
	fprintf(stderr, "pidof failed to open /proc/all\n");
	return 2;
    }

    for (;;) {
	auto line = file.read_line(1024);
		
	if (line.is_empty())
	    break;

	auto chomped = String((const char*)line.pointer(), line.size() - 1, Chomp);
	auto parts = chomped.split_view(',');

	if (parts.size() < 18)
	    break;

	bool ok = false;
	pid_t current_pid = parts[0].to_uint(ok);
	String name = parts[11];

	if (!ok) {
	    fprintf(stderr, "pidof failed : couldn't convert %s to a valid pid\n", parts[0].characters());
	    return 3;
	}
		
	if (name == process_name) {	
	    if (!omit_pid || (omit_pid && current_pid != pid)) {
		printf("%d ", current_pid);
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
    
    args_parser.add_arg("s", "Single shot - this instructs the program to only return one pid", false);
    args_parser.add_arg("o", "pid", "Tells pidof to omit processes with that pid. The special pid %PPID can be used to name the parent process of the pidof program.", false);
    
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
