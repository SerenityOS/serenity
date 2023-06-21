/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Types.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

int main()
{
    printf("Testing full unnmap\n");
    auto* map1 = mmap(nullptr, 2 * PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED, 0, 0);
    if (map1 == MAP_FAILED) {
        perror("mmap 1");
        return 1;
    }
    auto* map2 = mmap((void*)((FlatPtr)map1 + 2 * PAGE_SIZE), 2 * PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED, 0, 0);
    if (map2 == MAP_FAILED) {
        perror("mmap 2");
        return 1;
    }
    auto* map3 = mmap((void*)((FlatPtr)map1 + 4 * PAGE_SIZE), 2 * PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED, 0, 0);
    if (map3 == MAP_FAILED) {
        perror("mmap 3");
        return 1;
    }

    // really allocating pages
    memset(map1, 0x01, 6 * PAGE_SIZE);

    int rc;

    outln("Mprotect 3 ranges [2, 2 ,2]");
    rc = mprotect(map1, 6 * PAGE_SIZE, PROT_READ);
    if (rc) {
        perror("mprotect full");
        return 1;
    }

    outln("Mprotect 3 ranges [-1, 2 ,1-]");
    rc = mprotect((void*)((FlatPtr)map1 + PAGE_SIZE), 4 * PAGE_SIZE, PROT_READ);
    if (rc) {
        perror("mprotect partial");
        return 1;
    }

    outln("unmapping");
    munmap(map2, 2 * PAGE_SIZE);

    outln("Mprotect 2 ranges [2, -- ,2] -> Error");
    rc = mprotect(map1, 6 * PAGE_SIZE, PROT_READ);
    if (!rc) {
        perror("mprotect full over missing succeeded");
        return 1;
    }

    outln("Mprotect 3 ranges [-1, -- ,1-] -> Error");
    rc = mprotect((void*)((FlatPtr)map1 + PAGE_SIZE), 4 * PAGE_SIZE, PROT_READ);
    if (!rc) {
        perror("mprotect partial over missing succeeded");
        return 1;
    }

    // cleanup
    munmap(map1, 6 * PAGE_SIZE);

    outln("PASS");
    return 0;
}
