/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
