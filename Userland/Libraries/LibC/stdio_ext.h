/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdio.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

size_t __fpending(FILE*);
int __freading(FILE*);
int __fwriting(FILE*);
void __fpurge(FILE*);

size_t __freadahead(FILE*);
char const* __freadptr(FILE*, size_t*);
void __freadptrinc(FILE*, size_t);
void __fseterr(FILE*);

__END_DECLS
