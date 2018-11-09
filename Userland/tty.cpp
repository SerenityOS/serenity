#include <stdio.h>
#include <unistd.h>

int main(int, char**)
{
    char* tty = ttyname(0);
    if (!tty) {
        perror("Error");
        return 1;
    }
    printf("%s\n", tty);
    return 0;
}
