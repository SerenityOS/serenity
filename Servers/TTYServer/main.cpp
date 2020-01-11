#include <AK/Assertions.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio tty proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (argc < 2)
        return -1;

    dbgprintf("Starting console server on %s\n", argv[1]);

    while (true) {
        dbgprintf("Running shell on %s\n", argv[1]);

        auto child = fork();
        if (!child) {
            int rc = execl("/bin/Shell", "Shell", nullptr);
            ASSERT(rc < 0);
            perror("execl");
            exit(127);
        } else {
            int wstatus;
            waitpid(child, &wstatus, 0);
            dbgprintf("Shell on %s exited with code %d\n", argv[1], WEXITSTATUS(wstatus));
        }
    }
}
