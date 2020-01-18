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

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>
#include <unistd.h>

#define EXPECT_OK(syscall, address, size) \
    do { \
        rc = syscall(fd, (void*)(address), (size_t)(size)); \
        if (rc < 0) { \
            fprintf(stderr, "Expected success: " #syscall "(%p, %zu), got rc=%d, errno=%d\n", (void*)(address), (size_t)(size), rc, errno); \
        } \
    } while(0)

#define EXPECT_EFAULT(syscall, address, size) \
    do { \
        rc = syscall(fd, (void*)(address), (size_t)(size)); \
        if (rc >= 0 || errno != EFAULT) { \
            fprintf(stderr, "Expected EFAULT: " #syscall "(%p, %zu), got rc=%d, errno=%d\n", (void*)(address), (size_t)(size), rc, errno); \
        } \
    } while(0)


int main(int, char**)
{
    int fd = open("/dev/zero", O_RDONLY);
    int rc;

    // Test a one-page mapping (4KB)
    u8* one_page = (u8*)mmap(nullptr, 4096, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    ASSERT(one_page);
    EXPECT_OK(read, one_page, 4096);
    EXPECT_EFAULT(read, one_page, 4097);
    EXPECT_EFAULT(read, one_page - 1, 4096);

    // Test a two-page mapping (8KB)
    u8* two_page = (u8*)mmap(nullptr, 8192, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    ASSERT(two_page);

    EXPECT_OK(read, two_page, 4096);
    EXPECT_OK(read, two_page + 4096, 4096);
    EXPECT_OK(read, two_page, 8192);
    EXPECT_OK(read, two_page + 4095, 4097);
    EXPECT_OK(read, two_page + 1, 8191);
    EXPECT_EFAULT(read, two_page, 8193);
    EXPECT_EFAULT(read, two_page - 1, 1);

    // Check validation of pages between the first and last address.
    ptrdiff_t distance = two_page - one_page;
    EXPECT_EFAULT(read, one_page, (u32)distance + 1024);

    // Test every kernel page just because.
    for (u64 kernel_address = 0xc0000000; kernel_address <= 0xffffffff; kernel_address += PAGE_SIZE) {
        EXPECT_EFAULT(read, (void*)kernel_address, 1);
    }

    // Test the page just below where the kernel VM begins.
    u8* jerk_page = (u8*)mmap((void*)(0xc0000000 - PAGE_SIZE), PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED, 0, 0);
    ASSERT(jerk_page == (void*)(0xc0000000 - PAGE_SIZE));

    EXPECT_OK(read, jerk_page, 4096);
    EXPECT_EFAULT(read, jerk_page, 4097);

    // Test something that would wrap around the 2^32 mark.
    EXPECT_EFAULT(read, jerk_page, 0x50000000);

    return 0;
}
