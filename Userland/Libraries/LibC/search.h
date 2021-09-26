/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

__BEGIN_DECLS

void* tsearch(const void*, void**, int (*)(const void*, const void*));
void* tfind(const void*, void* const*, int (*)(const void*, const void*));

__END_DECLS
