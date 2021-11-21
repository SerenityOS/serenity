/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/ioctl_numbers.h>
#include <sys/kcov.h>
#include <sys/mman.h>
#include <unistd.h>

// Note: This program requires serenity to be built with the CMake build option
// ENABLE_KERNEL_COVERAGE_COLLECTION
int main(void)
{
    constexpr size_t num_entries = 1024 * 100;

    int fd = open("/dev/kcov0", O_RDWR);
    if (fd == -1) {
        perror("open");
        fprintf(stderr, "Could not open /dev/kcov0 - is ENABLE_KERNEL_COVERAGE_COLLECTION enabled?\n");
        return 1;
    }
    if (ioctl(fd, KCOV_SETBUFSIZE, num_entries) == -1) {
        perror("ioctl: KCOV_SETBUFSIZE");
        return 1;
    }
    kcov_pc_t* cover = (kcov_pc_t*)mmap(NULL, num_entries * KCOV_ENTRY_SIZE,
        PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (cover == MAP_FAILED) {
        perror("mmap");
        return 1;
    }
    if (ioctl(fd, KCOV_ENABLE) == -1) {
        perror("ioctl: KCOV_ENABLE");
        return 1;
    }
    cover[0] = 0;

    // Example syscall so we actually cover some kernel code.
    getppid();

    if (ioctl(fd, KCOV_DISABLE) == -1) {
        perror("ioctl: KCOV_DISABLE");
        return 1;
    }
    u64 cov_idx = cover[0];
    for (size_t idx = 1; idx <= cov_idx; idx++)
        printf("%p\n", (void*)cover[idx]);
    if (munmap(const_cast<u64*>(cover), num_entries * KCOV_ENTRY_SIZE) == -1) {
        perror("munmap");
        return 1;
    }
    close(fd);

    return 0;
}
