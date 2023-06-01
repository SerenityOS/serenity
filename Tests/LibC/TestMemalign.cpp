/*
 * Copyright (c) 2022, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <mallocdefs.h>
#include <stdlib.h>

static constexpr size_t runs = 500;
static constexpr size_t ptrs_per_run = 20;

static size_t random_alignment()
{
    return 1 << (rand() % 16);
}

static size_t random_size()
{
    int r = rand() % num_size_classes;
    if (r == num_size_classes - 1) {
        return rand() % (1 << 17);
    }

    return size_classes[r] + (rand() % (size_classes[r + 1] - size_classes[r]));
}

static bool is_aligned(void* ptr, size_t align)
{
    return (reinterpret_cast<uintptr_t>(ptr) & (align - 1)) == 0;
}

TEST_CASE(posix_memalign_fuzz)
{
    EXPECT_NO_CRASH("posix_memalign should not crash under regular use", [] {
        for (size_t i = 0; i < runs; ++i) {
            size_t align = random_alignment();
            size_t size = random_size();

            for (size_t j = 0; j < 2; ++j) {
                void* ptrs[ptrs_per_run];

                for (size_t k = 0; k < ptrs_per_run; ++k) {
                    EXPECT_EQ(posix_memalign(&(ptrs[k]), align, size), 0);
                    EXPECT(is_aligned(ptrs[k], align));
                }
                for (size_t k = 0; k < ptrs_per_run; ++k) {
                    free(ptrs[k]);
                }
            }
        }

        return Test::Crash::Failure::DidNotCrash;
    });
}

TEST_CASE(posix_memalign_not_power2)
{
    char secret_ptr[0];
    void* memptr = secret_ptr;
    EXPECT_EQ(posix_memalign(&memptr, 7, 256), EINVAL);
    EXPECT_EQ(memptr, secret_ptr);
}

TEST_CASE(aligned_alloc_fuzz)
{
    EXPECT_NO_CRASH("aligned_alloc should not crash under regular use", [] {
        for (size_t i = 0; i < runs; ++i) {
            size_t align = random_alignment();
            size_t size = random_size();

            for (size_t j = 0; j < 2; ++j) {
                void* ptrs[ptrs_per_run];

                for (size_t k = 0; k < ptrs_per_run; ++k) {
                    ptrs[k] = aligned_alloc(align, size);
                    EXPECT(ptrs[k]);
                    EXPECT(is_aligned(ptrs[k], align));
                }
                for (size_t k = 0; k < ptrs_per_run; ++k) {
                    free(ptrs[k]);
                }
            }
        }

        return Test::Crash::Failure::DidNotCrash;
    });
}

TEST_CASE(aligned_alloc_not_power2)
{
#if defined(AK_COMPILER_CLANG)
#    pragma clang diagnostic push
#    pragma clang diagnostic ignored "-Wnon-power-of-two-alignment"
#endif
    EXPECT_EQ(aligned_alloc(7, 256), nullptr);
    EXPECT_EQ(errno, EINVAL);
#if defined(AK_COMPILER_CLANG)
#    pragma clang diagnostic pop
#endif
}
