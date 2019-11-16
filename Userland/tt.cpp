#include <pthread.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>

static int mutex_test();

int main(int argc, char** argv)
{
    if (argc == 2 && *argv[1] == 'm')
        return mutex_test();

    printf("Hello from the first thread!\n");
    pthread_t thread_id;
    int rc = pthread_create(
        &thread_id, nullptr, [](void*) -> void* {
            printf("Hi there, from the second thread!\n");
            pthread_exit((void*)0xDEADBEEF);
            return nullptr;
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 1;
    }
    void* retval;
    rc = pthread_join(thread_id, &retval);
    if (rc < 0) {
        perror("pthread_join");
        return 1;
    }
    printf("Okay, joined and got retval=%p\n", retval);
    return 0;
}

static pthread_mutex_t mutex;

int mutex_test()
{
    int rc = pthread_mutex_init(&mutex, nullptr);
    if (rc < 0) {
        perror("pthread_mutex_init");
        return 1;
    }
    pthread_t thread_id;
    rc = pthread_create(
        &thread_id, nullptr, [](void*) -> void* {
            printf("I'm the secondary thread :^)\n");
            for (;;) {
                pthread_mutex_lock(&mutex);
                printf("Second thread stole mutex\n");
                sleep(1);
                printf("Second thread giving back mutex\n");
                pthread_mutex_unlock(&mutex);
                sleep(1);
            }
            pthread_exit((void*)0xDEADBEEF);
            return nullptr;
        },
        nullptr);
    if (rc < 0) {
        perror("pthread_create");
        return 1;
    }
    for (;;) {
        pthread_mutex_lock(&mutex);
        printf("Obnoxious spam!\n");
        pthread_mutex_unlock(&mutex);
        usleep(10000);
    }
    return 0;
}
