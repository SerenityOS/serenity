/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/SourceLocation.h>
#include <AK/String.h>
#include <errno.h>

namespace Video {

struct DecoderError;

template<typename T>
using DecoderErrorOr = ErrorOr<T, DecoderError>;

enum class DecoderErrorCategory : u32 {
    Unknown,
    IO,
    Memory,
    // The input is corrupted.
    Corrupted,
    // The input uses features that are not yet implemented.
    NotImplemented,
};

struct DecoderError {
public:
    static DecoderError with_description(DecoderErrorCategory category, StringView description)
    {
        return DecoderError(category, description);
    }

    template<typename... Parameters>
    static DecoderError format(DecoderErrorCategory category, CheckedFormatString<Parameters...>&& format_string, Parameters const&... parameters)
    {
        AK::VariadicFormatParams variadic_format_params { parameters... };
        return DecoderError::with_description(category, String::vformatted(format_string.view(), variadic_format_params));
    }

    static DecoderError corrupted(StringView description, SourceLocation location = SourceLocation::current())
    {
        return DecoderError::format(DecoderErrorCategory::Corrupted, "{}: {}", location, description);
    }

    static DecoderError not_implemented(SourceLocation location = SourceLocation::current())
    {
        return DecoderError::format(DecoderErrorCategory::NotImplemented, "{} is not implemented", location.function_name());
    }

    DecoderErrorCategory category() { return m_category; }
    StringView description() { return m_description; }
    StringView string_literal() { return m_description; }

private:
    DecoderError(DecoderErrorCategory category, String description)
        : m_category(category)
        , m_description(move(description))
    {
    }

    DecoderErrorCategory m_category { DecoderErrorCategory::Unknown };
    String m_description;
};

#define DECODER_TRY(category, expression)                                  \
    ({                                                                     \
        auto _result = ((expression));                                     \
        if (_result.is_error()) [[unlikely]] {                             \
            auto _error_string = _result.release_error().string_literal(); \
            return DecoderError::format(                                   \
                ((category)), "{}: {}",                                    \
                SourceLocation::current(), _error_string);                 \
        }                                                                  \
        _result.release_value();                                           \
    })

#define DECODER_TRY_ALLOC(expression) DECODER_TRY(DecoderErrorCategory::Memory, expression)

}
