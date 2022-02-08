/*
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>

namespace Web::HTML {

void report_exception(JS::Completion const&);

template<typename T>
inline void report_exception(JS::ThrowCompletionOr<T> const& result)
{
    VERIFY(result.is_throw_completion());
    report_exception(result.throw_completion());
}

}
