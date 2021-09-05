/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// NOTE: This macro works with any result type that has the expected APIs.
//       It's designed with AK::Result and Kernel::KResult in mind.

#define TRY(expression)                    \
    ({                                     \
        auto result = (expression);        \
        if (result.is_error())             \
            return result.release_error(); \
        result.release_value();            \
    })
