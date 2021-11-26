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

void* tsearch(const void*, void**, int (*)(const void*, const void*));
void* tfind(const void*, void* const*, int (*)(const void*, const void*));
void* tdelete(const void*, void**, int (*)(const void*, const void*));
void twalk(const void*, void (*)(const void*, VISIT, int));

__END_DECLS
