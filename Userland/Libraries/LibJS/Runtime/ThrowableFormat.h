/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CheckedFormatString.h>
#include <AK/DeprecatedString.h>
#include <AK/Format.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

template<typename... Args>
ThrowCompletionOr<DeprecatedString> deprecated_format(VM& vm, CheckedFormatString<Args...>&& fmtstr, Args const&... args)
{
    StringBuilder builder;
    AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::No, Args...> parameters { args... };

    TRY_OR_THROW_OOM(vm, vformat(builder, fmtstr.view(), parameters));
    return builder.to_deprecated_string();
}

}
