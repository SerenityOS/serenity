/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

// Arbitrary pain threshold.
#define IOV_MAX 1024

struct iovec {
    void* iov_base;
    size_t iov_len;
};

#ifdef __cplusplus
}
#endif
