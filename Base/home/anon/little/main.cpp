#include <stdio.h>
#include <sys/stat.h>

int main(int, char**)
{
    for (int i = 0; i < 3; ++i) {
        // This is a comment :^)
        printf("Hello friends!\n");
        mkdir("/tmp/xyz", 0755);
    }
    return 0;
}
