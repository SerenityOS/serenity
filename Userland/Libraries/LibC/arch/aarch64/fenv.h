/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

#ifndef __aarch64__
#    error "This file should not be included on architectures other than AArch64."
#endif

__BEGIN_DECLS

// TODO: Implement this.
typedef struct fenv_t {
    char __dummy; // NOTE: This silences -Wextern-c-compat.
} fenv_t;

__END_DECLS
