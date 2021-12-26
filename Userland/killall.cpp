#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <AK/AKString.h>

static void print_usage_and_exit()
{
    printf("usage: killall [-signal] process_name\n");
    exit(1);
}

static int kill_all(const String& process_name, const unsigned signum)
{
    HashMap<pid_t, CProcessStatistics> processes = CProcessStatisticsReader().get_map();
    
    for (auto& it : processes) {
        if (it.value.name == process_name) {    
            int ret = kill(it.value.pid, signum);
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
