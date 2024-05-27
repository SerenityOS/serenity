/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/Error.h>
#include <AK/FlyString.h>
#include <errno.h>

namespace Audio {

struct LoaderError {

    enum class Category : u32 {
        // The error category is unknown.
        Unknown = 0,
        IO,
        // The read file doesn't follow the file format.
        Format,
        // Equivalent to an ASSERT(), except non-crashing.
        Internal,
        // The loader encountered something in the format that is not yet implemented.
        Unimplemented,
    };
    Category category { Category::Unknown };
    // Binary index: where in the file the error occurred.
    size_t index { 0 };
    FlyString description { ""_fly_string };

    constexpr LoaderError() = default;
    LoaderError(Category category, size_t index, FlyString description)
        : category(category)
        , index(index)
        , description(move(description))
    {
    }
    LoaderError(FlyString description)
        : description(move(description))
    {
    }
    LoaderError(Category category, FlyString description)
        : category(category)
        , description(move(description))
    {
    }

    LoaderError(LoaderError&) = default;
    LoaderError(LoaderError&&) = default;

    LoaderError(Error&& error)
    {
        if (error.is_errno()) {
            auto code = error.code();
            description = String::formatted("{} ({})", strerror(code), code).release_value_but_fixme_should_propagate_errors();
            if (code == EBADF || code == EBUSY || code == EEXIST || code == EIO || code == EISDIR || code == ENOENT || code == ENOMEM || code == EPIPE)
                category = Category::IO;
        } else {
            description = FlyString::from_utf8(error.string_literal()).release_value_but_fixme_should_propagate_errors();
        }
    }
};

}

namespace AK {

template<>
struct Formatter<Audio::LoaderError> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Audio::LoaderError const& error)
    {
        StringView category;
        switch (error.category) {
        case Audio::LoaderError::Category::Unknown:
            category = "Unknown"sv;
            break;
        case Audio::LoaderError::Category::IO:
            category = "I/O"sv;
            break;
        case Audio::LoaderError::Category::Format:
            category = "Format"sv;
            break;
        case Audio::LoaderError::Category::Internal:
            category = "Internal"sv;
            break;
        case Audio::LoaderError::Category::Unimplemented:
            category = "Unimplemented"sv;
            break;
        }
        return Formatter<FormatString>::format(builder, "{} error: {} (at {})"sv, category, error.description, error.index);
    }
};

}
