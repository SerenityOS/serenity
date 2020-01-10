#include <pthread.h>
#include <stdio.h>
#include <sys/select.h>
#include <unistd.h>

int main(int, char**)
{
    pthread_t tid;
    pthread_create(
        &tid, nullptr, [](void*) -> void* {
            sleep(1);
            asm volatile("ud2");
            return nullptr;
        },
        nullptr);

    pthread_join(tid, nullptr);

    printf("ok\n");
    return 0;
}
