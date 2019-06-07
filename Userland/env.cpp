#include <stdio.h>
#include <unistd.h>

int main(int, char**)
{
    for (size_t i = 0; environ[i]; ++i)
        printf("%s\n", environ[i]);
    return 0;
}
