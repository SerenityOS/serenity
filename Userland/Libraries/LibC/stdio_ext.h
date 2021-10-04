/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdio.h>

__BEGIN_DECLS

int __freading(FILE*);
int __fwriting(FILE*);
void __fpurge(FILE*);

__END_DECLS
