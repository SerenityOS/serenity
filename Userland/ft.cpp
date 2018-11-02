#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    printf("Testing fork()...\n");
    pid_t pid = fork();
    if (!pid) {
        printf("child, pid=%d\n", getpid());
    } else {
        printf("parent, child pid=%d\n", pid);
    }
    return 0;
}
