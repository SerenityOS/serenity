#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
    if (argc < 2)
        return -1;

    dbgprintf("Starting console server on %s\n", argv[1]);

    while (true) {
        dbgprintf("Running shell on %s\n", argv[1]);
        int rc = system("/bin/Shell");
        dbgprintf("Shell on %s exited with code %d\n", argv[1], rc);
    }
}
