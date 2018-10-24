#include <LibC/unistd.h>
#include <LibC/stdio.h>

int main(int c, char** v)
{
    char buffer[1024];
    char* ptr = getcwd(buffer, sizeof(buffer));
    if (!ptr) {
        printf("getcwd() failed\n");
        return 1;
    }
    printf("%s\n", ptr);
    return 0;
}

