/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdint.h>
#include <sys/cdefs.h>

#ifndef __aarch64__
#    error "This file should not be included on architectures other than AArch64."
#endif

__BEGIN_DECLS

// we need to implement this for aarch64
typedef struct {
    uint32_t fpcr;
    uint32_t fpsr;
} fenv_t;

static_assert(sizeof(fenv_t) == 8);

__END_DECLS
