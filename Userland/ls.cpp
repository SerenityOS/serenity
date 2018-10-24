#include <LibC/stdio.h>
#include <LibC/unistd.h>
#include <LibC/mman.h>

int main(int c, char** v)
{
    int fd = open("/");
    if (fd == -1) {
        printf("failed to open / :(\n");
        return 1;
    }

    byte* memory = (byte*)mmap(nullptr, 16384);
    printf("%p\n", memory);
    memory[0] = 'H';
    memory[1] = 'i';
    memory[2] = '!';
    memory[3] = '\0';
    printf("%p : %s\n", memory, memory);
    return 0;
}
