#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int pipefds[2];

int main(int, char**)
{
    pipe(pipefds);

    pthread_t tid;
    pthread_create(
        &tid, nullptr, [](void*) -> void* {
            sleep(1);
            printf("Second thread closing pipes!\n");
            close(pipefds[0]);
            close(pipefds[1]);
            pthread_exit(nullptr);
            return nullptr;
        },
        nullptr);

    printf("First thread doing a blocking read from pipe...\n");
    char buffer[16];
    int nread = read(pipefds[0], buffer, sizeof(buffer));
    printf("Ok, read %d bytes from pipe\n", nread);

    return 0;
}
