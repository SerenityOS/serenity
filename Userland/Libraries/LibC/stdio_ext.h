/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdio.h>

__BEGIN_DECLS

size_t __fpending(FILE*);
int __freading(FILE*);
int __fwriting(FILE*);
void __fpurge(FILE*);

const char* __freadptr(FILE*, size_t*);
void __fseterr(FILE*);

__END_DECLS
