#include <stdio.h>
#include <unistd.h>


int main(int, char**)
{
    if (reboot() < 0){
        perror("reboot");
        return 1;
    }
    return 0;
}



