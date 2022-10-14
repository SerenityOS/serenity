/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Diagnostics.h>

// NOTE: This macro works with any result type that has the expected APIs.
//       It's designed with AK::Result and AK::Error in mind.
//
//       It depends on a non-standard C++ extension, specifically
//       on statement expressions [1]. This is known to be implemented
//       by at least clang and gcc.
//       [1] https://gcc.gnu.org/onlinedocs/gcc/Statement-Exprs.html

#define TRY(expression)                                   \
    ({                                                    \
        /* Ignore -Wshadow to allow nesting the macro. */ \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                  \
            auto _temporary_result = (expression));       \
        if (_temporary_result.is_error()) [[unlikely]]    \
            return _temporary_result.release_error();     \
        _temporary_result.release_value();                \
    })

#define MUST(expression)                                  \
    ({                                                    \
        /* Ignore -Wshadow to allow nesting the macro. */ \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                  \
            auto _temporary_result = (expression));       \
        VERIFY(!_temporary_result.is_error());            \
        _temporary_result.release_value();                \
    })
