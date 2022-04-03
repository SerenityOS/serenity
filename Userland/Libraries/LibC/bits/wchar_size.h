/*
 * Copyright (c) 2022, Tim Schumacher <timschumi@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define WCHAR_MAX __WCHAR_MAX__
#ifdef __WCHAR_MIN__
#    define WCHAR_MIN __WCHAR_MIN__
#else
// Note: This assumes that wchar_t is a signed type.
#    define WCHAR_MIN (-WCHAR_MAX - 1)
#endif
