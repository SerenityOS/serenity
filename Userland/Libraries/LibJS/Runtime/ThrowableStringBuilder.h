/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

class ThrowableStringBuilder : private AK::StringBuilder {
public:
    explicit ThrowableStringBuilder(VM&);

    ThrowCompletionOr<void> append(char);
    ThrowCompletionOr<void> append(StringView);
    ThrowCompletionOr<void> append(Utf16View const&);
    ThrowCompletionOr<void> append_code_point(u32 value);
    ThrowCompletionOr<String> to_string() const;

    template<typename... Parameters>
    ThrowCompletionOr<void> appendff(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::No, Parameters...> variadic_format_params { parameters... };

        if (vformat(*this, fmtstr.view(), variadic_format_params).is_error()) {
            // The size returned here is a bit of an estimate, as we don't know what the final formatted string length would be.
            return m_vm.throw_completion<InternalError>(ErrorType::NotEnoughMemoryToAllocate, length() + fmtstr.view().length());
        }

        return {};
    }

    using AK::StringBuilder::is_empty;
    using AK::StringBuilder::string_view;
    using AK::StringBuilder::trim;

private:
    VM& m_vm;
};

}
