#include <stdio.h>
#include <unistd.h>

int main(int, char**)
{
    puts(getlogin());
    return 0;
}
