/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stddef.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

__attribute__((malloc)) __attribute__((alloc_size(2))) __attribute__((alloc_align(1))) void* memalign(size_t alignment, size_t size);

__END_DECLS
