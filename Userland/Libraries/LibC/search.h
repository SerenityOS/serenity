/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

typedef enum {
    preorder,
    postorder,
    endorder,
    leaf,
} VISIT;

void* tsearch(void const*, void**, int (*)(void const*, void const*));
void* tfind(void const*, void* const*, int (*)(void const*, void const*));
void* tdelete(void const*, void**, int (*)(void const*, void const*));
void twalk(void const*, void (*)(void const*, VISIT, int));

__END_DECLS
