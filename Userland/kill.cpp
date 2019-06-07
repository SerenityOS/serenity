#include <AK/AKString.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static void print_usage_and_exit()
{
    printf("usage: kill [-signal] <PID>\n");
    exit(1);
}

int main(int argc, char** argv)
{
    if (argc != 2 && argc != 3)
        print_usage_and_exit();
    bool ok;
    unsigned signum = SIGTERM;
    int pid_argi = 1;
    if (argc == 3) {
        pid_argi = 2;
        if (argv[1][0] != '-')
            print_usage_and_exit();
        signum = String(&argv[1][1]).to_uint(ok);
        if (!ok) {
            printf("'%s' is not a valid signal number\n", &argv[1][1]);
            return 2;
        }
    }
    unsigned pid = String(argv[pid_argi]).to_uint(ok);
    if (!ok) {
        printf("'%s' is not a valid PID\n", argv[pid_argi]);
        return 3;
    }

    int rc = kill((pid_t)pid, signum);
    if (rc < 0)
        perror("kill");
    return 0;
}
