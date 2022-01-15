/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// NOTE: This macro works with any result type that has the expected APIs.
//       It's designed with AK::Result and AK::Error in mind.

#define TRY(expression)                                \
    ({                                                 \
        auto _temporary_result = (expression);         \
        if (_temporary_result.is_error()) [[unlikely]] \
            return _temporary_result.release_error();  \
        _temporary_result.release_value();             \
    })

#define MUST(expression)                       \
    ({                                         \
        auto _temporary_result = (expression); \
        VERIFY(!_temporary_result.is_error()); \
        _temporary_result.release_value();     \
    })
