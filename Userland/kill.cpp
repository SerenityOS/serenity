#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <AK/String.h>

static unsigned parseUInt(const String& str, bool& ok)
{
    unsigned value = 0;
    for (size_t i = 0; i < str.length(); ++i) {
        if (str[i] < '0' || str[i] > '9') {
            ok = false;
            return 0;
        }
        value = value * 10;
        value += str[i] - '0';
    }
    ok = true;
    return value;
}

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
        signum = parseUInt(&argv[1][1], ok);
        if (!ok) {
            printf("%s is not a valid signal number\n", &argv[1][1]);
            return 2;
        }
    }
    unsigned pid = parseUInt(argv[pid_argi], ok);
    if (!ok) {
        printf("%s is not a valid PID\n", argv[pid_argi]);
        return 3;
    }

    kill((pid_t)pid, signum);
    return 0;
}

