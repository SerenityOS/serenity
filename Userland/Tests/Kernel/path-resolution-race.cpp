#include <unistd.h>
#include <sys/stat.h>

int main()
{
    if (!fork()) {
        for (;;) {
            mkdir("/tmp/x", 0666);
            rmdir("/tmp/x");
        }
    }
    for (;;) {
        chdir("/tmp/x");
    }
    return 0;
}
