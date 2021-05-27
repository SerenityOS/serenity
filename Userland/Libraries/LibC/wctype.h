/*
 * Copyright (c) 2021, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <wchar.h>

__BEGIN_DECLS

wctype_t wctype(const char* name);
int iswctype(wint_t wc, wctype_t desc);

__END_DECLS
