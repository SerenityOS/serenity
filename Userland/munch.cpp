#include <AK/Types.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    size_t limit = 0;
    size_t bite_size = 0;
    int interval = 0;
    if (argc == 1) {
        limit = 42 * MB;
        bite_size = 1 * MB;
        interval = 200000;
    } else if (argc == 4) {
        bite_size = atoi(argv[1]);
        limit = atoi(argv[2]);
        interval = atoi(argv[3]);
    } else {
        printf("usage: munch [bite_size limit interval]\n");
        return 1;
    }

    size_t munched = 0;
    printf("Munching %zu bytes every %d ms, stopping at %zu\n", bite_size, interval / 1000, limit);
    for (;;) {
        auto* ptr = mmap(nullptr, bite_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
        if (ptr == MAP_FAILED) {
            perror("mmap");
            return 1;
        }
        memset(ptr, 0, bite_size);
        munched += bite_size;
        printf("Allocated: %zu\n", munched);
        if (limit && munched >= limit) {
            printf("All done!\n");
            break;
        }
        usleep(interval);
    }
    return 0;
}
