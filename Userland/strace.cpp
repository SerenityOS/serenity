#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Syscall.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int usage()
{
    printf("usage: strace [-p PID] [command...]\n");
    return 0;
}

int main(int argc, char** argv)
{
    if (argc == 1)
        return usage();

    pid_t pid = -1;
    bool pid_is_child = false;

    if (!strcmp(argv[1], "-p")) {
        if (argc != 3)
            return usage();
        pid = atoi(argv[2]);
    } else {
        pid_is_child = true;
        pid = fork();
        if (!pid) {
            kill(getpid(), SIGSTOP);
            int rc = execvp(argv[1], &argv[1]);
            if (rc < 0) {
                perror("execvp");
                exit(1);
            }
            ASSERT_NOT_REACHED();
        }
    }

    int fd = systrace(pid);
    if (fd < 0) {
        perror("systrace");
        return 1;
    }

    if (pid_is_child) {
        int rc = kill(pid, SIGCONT);
        if (rc < 0) {
            perror("kill(pid, SIGCONT)");
            return 1;
        }
    }

    for (;;) {
        u32 call[5];
        int nread = read(fd, &call, sizeof(call));
        if (nread == 0)
            break;
        if (nread < 0) {
            perror("read");
            return 1;
        }
        ASSERT(nread == sizeof(call));
        fprintf(stderr, "%s(%#x, %#x, %#x) = %#x\n", Syscall::to_string((Syscall::Function)call[0]), call[1], call[2], call[3], call[4]);
    }

    int rc = close(fd);
    if (rc < 0) {
        perror("close");
        return 1;
    }

    return 0;
}
