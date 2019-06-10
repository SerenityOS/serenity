#include <AK/AKString.h>
#include <stdio.h>
#include <unistd.h>

void usage(void)
{
    printf("usage: allocate [number [unit (B/KB/MB)]]\n");
    exit(1);
}

enum Unit { Bytes,
    KiloBytes,
    MegaBytes };

int main(int argc, char** argv)
{
    unsigned count = 50;
    Unit unit = MegaBytes;

    if (argc >= 2) {
        bool ok;
        count = String(argv[1]).to_uint(ok);
        if (!ok) {
            usage();
        }
    }

    if (argc >= 3) {
        if (strcmp(argv[2], "B") == 0)
            unit = Bytes;
        else if (strcmp(argv[2], "KB") == 0)
            unit = KiloBytes;
        else if (strcmp(argv[2], "MB") == 0)
            unit = MegaBytes;
        else
            usage();
    }

    switch (unit) {
    case Bytes:
        break;
    case KiloBytes:
        count *= 1024;
        break;
    case MegaBytes:
        count *= 1024 * 1024;
        break;
    }

    printf("allocating memory (%d bytes)...\n", count);
    char* ptr = (char*)malloc(count);
    if (!ptr) {
        printf("failed.\n");
        return 1;
    }
    printf("done.\n");

    printf("writing to allocated memory...\n");
    for (int i = 0; i < count; i++) {
        ptr[i] = i % 255;
    }
    printf("done.\n");

    printf("sleeping for ten seconds...");
    for (int i = 0; i < 10; i++) {
        printf("%d\n", i);
        sleep(1);
    }
    printf("done.\n");

    printf("freeing memory...\n");
    free(ptr);
    printf("done.\n");

    return 0;
}
