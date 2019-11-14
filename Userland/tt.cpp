#include <pthread.h>
#include <stdio.h>

int main(int, char**)
{
    printf("Hello from the first thread!\n");
    pthread_t thread_id;
    int rc = pthread_create(&thread_id, nullptr, [](void*) -> void* {
        printf("Hi there, from the second thread!\n");
        pthread_exit((void*)0xDEADBEEF);
        return nullptr;
    }, nullptr);
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
