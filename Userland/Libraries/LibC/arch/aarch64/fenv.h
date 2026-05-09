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

typedef struct fenv_t {
    uint64_t fpcr;
    uint64_t fpsr;
} fenv_t;

__END_DECLS
