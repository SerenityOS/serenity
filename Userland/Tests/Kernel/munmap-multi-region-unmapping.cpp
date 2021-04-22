/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

int main()
{
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

        ((u32*)map1)[0] = 0x41414141;
        ((u32*)map1)[PAGE_SIZE / sizeof(u32)] = 0x42424242;

        ((u32*)map2)[0] = 0xbeefbeef;
        ((u32*)map2)[PAGE_SIZE / sizeof(u32)] = 0xc0dec0de;

        if (((u32*)map1)[0] != 0x41414141 || ((u32*)map1)[PAGE_SIZE / sizeof(u32)] != 0x42424242
            || ((u32*)map2)[0] != 0xbeefbeef || ((u32*)map2)[PAGE_SIZE / sizeof(u32)] != 0xc0dec0de) {
            perror("write");
            return 1;
        }

        int res = munmap(map1, 4 * PAGE_SIZE);
        if (res < 0) {
            perror("unmap");
            return 1;
        }
    }
    {
        printf("Testing partial unmapping\n");
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

        ((u32*)map1)[0] = 0x41414141;
        ((u32*)map1)[PAGE_SIZE / sizeof(u32)] = 0x42424242;

        ((u32*)map2)[0] = 0xbeefbeef;
        ((u32*)map2)[PAGE_SIZE / sizeof(u32)] = 0xc0dec0de;

        if (((u32*)map1)[0] != 0x41414141 || ((u32*)map1)[PAGE_SIZE / sizeof(u32)] != 0x42424242
            || ((u32*)map2)[0] != 0xbeefbeef || ((u32*)map2)[PAGE_SIZE / sizeof(u32)] != 0xc0dec0de) {
            perror("write");
            return 1;
        }

        int res = munmap((void*)((FlatPtr)map1 + PAGE_SIZE), 2 * PAGE_SIZE);
        if (res < 0) {
            perror("unmap");
            return 1;
        }

        auto* map3 = mmap((void*)((FlatPtr)map1 + PAGE_SIZE), PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED, 0, 0);
        if (map3 == MAP_FAILED) {
            perror("remap 1");
            return 1;
        }
        auto* map4 = mmap(map2, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED, 0, 0);
        if (map4 == MAP_FAILED) {
            perror("remap 2");
            return 1;
        }
        ((u32*)map3)[0] = 0x13371337;
        ((u32*)map4)[0] = 0x1b1b1b1b;
        if (((u32*)map1)[0] != 0x41414141 || ((u32*)map2)[PAGE_SIZE / sizeof(u32)] != 0xc0dec0de
            || ((u32*)map3)[0] != 0x13371337 || ((u32*)map4)[0] != 0x1b1b1b1b
            || ((u32*)map1)[PAGE_SIZE / sizeof(int)] != ((u32*)map3)[0] || ((u32*)map2)[0] != ((u32*)map4)[0]) {
            perror("read at old map and write at remap");
            return 1;
        }

        res = munmap(map1, PAGE_SIZE * 4);
        if (res < 0) {
            perror("cleanup");
            return 1;
        }
    }

    printf("PASS\n");
    return 0;
}
