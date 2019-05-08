#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <LibCore/CFile.h>
#include <AK/AKString.h>

static void print_usage_and_exit()
{
    printf("usage: killall [-signal] process_name\n");
    exit(1);
}

static int kill_all(const String& process_name, const unsigned signum)
{
    CFile file("/proc/all");
    if (!file.open(CIODevice::ReadOnly)) {
	fprintf(stderr, "killall failed to open /proc/all\n");
	return 3;
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
	pid_t pid = parts[0].to_uint(ok);
	String name = parts[11];

	if (!ok) {
	    fprintf(stderr, "killall failed : couldn't convert %s to a valid pid\n", parts[0].characters());
	    return 4;
	}
      
	if (name == process_name) {	
	    int ret = kill(pid, signum);
	    if (ret < 0)
		perror("kill");
	}
    }

    return 0;
}

int main(int argc, char** argv)
{
    bool ok;
    unsigned signum = SIGTERM;
    int name_argi = 1;
    
    if (argc != 2 && argc != 3)
        print_usage_and_exit();
        
    if (argc == 3) {
	name_argi = 2;
      
        if (argv[1][0] != '-')
            print_usage_and_exit();
	
	signum = String(&argv[1][1]).to_uint(ok);
        if (!ok) {
            printf("'%s' is not a valid signal number\n", &argv[1][1]);
            return 2;
        }
    }

    return kill_all(argv[name_argi], signum);
}
