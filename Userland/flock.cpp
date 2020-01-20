#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc < 3) {
        printf("usage: flock <path> <command...>\n");
        return 0;
    }

    if (!fork()) {
        if (execvp(argv[2], &argv[2]) < 0) {
            perror("execvp");
            exit(1);
        }
    }

    int status;
    if (waitpid(-1, &status, 0) < 0) {
        perror("waitpid");
        return 1;
    }
    return WEXITSTATUS(status);
}
