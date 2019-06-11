#include <AK/AKString.h>
#include <LibCore/CElapsedTimer.h>
#include <stdio.h>
#include <unistd.h>

void usage(void)
{
    printf("usage: allocate [number [unit (B/KB/MB)]]\n");
    exit(1);
}

enum Unit { Bytes, KiloBytes, MegaBytes };

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

    CElapsedTimer timer;

    printf("allocating memory (%d bytes)...\n", count);
    timer.start();
    char* ptr = (char*)malloc(count);
    if (!ptr) {
        printf("failed.\n");
        return 1;
    }
    printf("done in %dms\n", timer.elapsed());

    auto pages = count / 4096;
    auto step = pages / 10;

    CElapsedTimer timer2;

    printf("writing one byte to each page of allocated memory...\n");
    timer.start();
    timer2.start();
    for (int i = 0; i < pages; ++i) {
        ptr[i * 4096] = 1;

        if (i != 0 && (i % step) == 0) {
            auto ms = timer2.elapsed();
            if (ms == 0)
                ms = 1;

            auto bps = double(step * 4096) / (double(ms) / 1000);

            printf("step took %dms (%fMB/s)\n", ms, bps / 1024 / 1024);

            timer2.start();
        }
    }
    printf("done in %dms\n", timer.elapsed());

    printf("sleeping for ten seconds...\n");
    for (int i = 0; i < 10; i++) {
        printf("%d\n", i);
        sleep(1);
    }
    printf("done.\n");

    printf("freeing memory...\n");
    timer.start();
    free(ptr);
    printf("done in %dms\n", timer.elapsed());

    return 0;
}
