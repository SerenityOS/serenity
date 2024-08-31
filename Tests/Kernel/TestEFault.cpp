/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/Types.h>
#include <LibTest/TestCase.h>
#include <errno.h>
#include <fcntl.h>
#include <serenity.h>
#include <stdio.h>
#include <sys/mman.h>
#include <syscall.h>
#include <unistd.h>

#define EXPECT_OK(syscall, address, size)                                                                                         \
    do {                                                                                                                          \
        errno = 0;                                                                                                                \
        rc = syscall(fd, (void*)(address), (size_t)(size));                                                                       \
        EXPECT(rc >= 0);                                                                                                          \
        if (rc < 0) {                                                                                                             \
            warnln("Expected success: " #syscall "({:p}, {}), got rc={}, errno={}", (void*)(address), (size_t)(size), rc, errno); \
        }                                                                                                                         \
    } while (0)

#define EXPECT_EFAULT(syscall, address, size)                                                                                    \
    do {                                                                                                                         \
        errno = 0;                                                                                                               \
        rc = syscall(fd, (void*)(address), (size_t)(size));                                                                      \
        EXPECT(rc < 0);                                                                                                          \
        EXPECT_EQ(errno, EFAULT);                                                                                                \
        if (rc >= 0 || errno != EFAULT) {                                                                                        \
            warnln("Expected EFAULT: " #syscall "({:p}, {}), got rc={}, errno={}", (void*)(address), (size_t)(size), rc, errno); \
        }                                                                                                                        \
    } while (0)

TEST_CASE(test_efault)
{
    int fd = open("/dev/zero", O_RDONLY);
    int rc = -1;

    // Make an inaccessible hole before the next mapping.
    (void)mmap(nullptr, 4096, PROT_NONE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);

    // Test a one-page mapping (4KB)
    u8* one_page = (u8*)mmap(nullptr, 4096, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    VERIFY(one_page);
    EXPECT_OK(read, one_page, 4096);
    EXPECT_EFAULT(read, one_page, 4097);
    EXPECT_EFAULT(read, one_page - 1, 4096);

    // Make an unused hole mapping to create some inaccessible distance between our one and two-page mappings.
    (void)mmap(nullptr, 16384, PROT_NONE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);

    // Test a two-page mapping (8KB)
    u8* two_page = (u8*)mmap(nullptr, 8192, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    VERIFY(two_page);

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

    constexpr auto user_range_ceiling = (sizeof(void*) == 4 ? 0xbe000000u : 0x1ffe000000);
    u8* jerk_page = nullptr;

    // Test every kernel page just because.
    constexpr auto kernel_range_ceiling = (sizeof(void*) == 4 ? 0xffffffffu : 0x203fffffff);
    for (u64 kernel_address = user_range_ceiling; kernel_address <= kernel_range_ceiling; kernel_address += PAGE_SIZE) {
        jerk_page = (u8*)mmap((void*)kernel_address, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED, 0, 0);
        EXPECT_EQ(jerk_page, MAP_FAILED);
        EXPECT_EQ(errno, EFAULT);
    }

    // Test the page just below where the user VM ends.
    jerk_page = (u8*)mmap((void*)(user_range_ceiling - PAGE_SIZE), PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED, 0, 0);
    EXPECT_EQ(jerk_page, (u8*)(user_range_ceiling - PAGE_SIZE));

    EXPECT_OK(read, jerk_page, PAGE_SIZE);
    EXPECT_EFAULT(read, jerk_page, PAGE_SIZE + 1);

    // Test something that would wrap around the 2^32 mark.
    EXPECT_EFAULT(read, jerk_page, 0x50000000);
}

TEST_CASE(test_dbgputstr_efault)
{
    EXPECT_EQ(-syscall(SC_dbgputstr, nullptr, 3), EFAULT);
    EXPECT_EQ(-syscall(SC_dbgputstr, nullptr, 4096), EFAULT);
}

TEST_CASE(test_futex_wake_op_efault)
{
    EXPECT(futex(nullptr, FUTEX_WAKE_OP, 0, nullptr, nullptr, FUTEX_OP(FUTEX_OP_SET, 0, FUTEX_OP_CMP_EQ, 0)) < 0);
    EXPECT_EQ(errno, EFAULT);

    EXPECT(futex(nullptr, FUTEX_WAKE_OP, 0, nullptr, nullptr, FUTEX_OP(FUTEX_OP_ADD, 0, FUTEX_OP_CMP_EQ, 0)) < 0);
    EXPECT_EQ(errno, EFAULT);

    EXPECT(futex(nullptr, FUTEX_WAKE_OP, 0, nullptr, nullptr, FUTEX_OP(FUTEX_OP_OR, 0, FUTEX_OP_CMP_EQ, 0)) < 0);
    EXPECT_EQ(errno, EFAULT);

    EXPECT(futex(nullptr, FUTEX_WAKE_OP, 0, nullptr, nullptr, FUTEX_OP(FUTEX_OP_ANDN, 0, FUTEX_OP_CMP_EQ, 0)) < 0);
    EXPECT_EQ(errno, EFAULT);

    EXPECT(futex(nullptr, FUTEX_WAKE_OP, 0, nullptr, nullptr, FUTEX_OP(FUTEX_OP_XOR, 0, FUTEX_OP_CMP_EQ, 0)) < 0);
    EXPECT_EQ(errno, EFAULT);

    u32 test = 0;

    EXPECT(futex(&test, FUTEX_WAKE_OP, 0, nullptr, &test, FUTEX_OP(FUTEX_OP_SET, 0, FUTEX_OP_CMP_EQ, 0)) >= 0);
    EXPECT(futex(&test, FUTEX_WAKE_OP, 0, nullptr, &test, FUTEX_OP(FUTEX_OP_ADD, 0, FUTEX_OP_CMP_EQ, 0)) >= 0);
    EXPECT(futex(&test, FUTEX_WAKE_OP, 0, nullptr, &test, FUTEX_OP(FUTEX_OP_OR, 0, FUTEX_OP_CMP_EQ, 0)) >= 0);
    EXPECT(futex(&test, FUTEX_WAKE_OP, 0, nullptr, &test, FUTEX_OP(FUTEX_OP_ANDN, 0, FUTEX_OP_CMP_EQ, 0)) >= 0);
    EXPECT(futex(&test, FUTEX_WAKE_OP, 0, nullptr, &test, FUTEX_OP(FUTEX_OP_XOR, 0, FUTEX_OP_CMP_EQ, 0)) >= 0);
}
