/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibRuntime/Serenity/PossiblyThrowingCallback.h>

namespace Runtime {

// NOTE: Be sure to only call noexcept functions in this file, otherwise compiler will insert calls
//       to std::terminate, which most likely be undefined.
void run_possibly_throwing_callback(void (*callback)()) noexcept
{
    try {
        callback();
    } catch (...) {
        VERIFY_NOT_REACHED();
    }
}

void run_possibly_throwing_callback(void (*callback)(void* argument), void* argument) noexcept
{
    try {
        callback(argument);
    } catch (...) {
        VERIFY_NOT_REACHED();
    }
}

}
