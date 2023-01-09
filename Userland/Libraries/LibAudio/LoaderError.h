/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/Error.h>
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
    DeprecatedFlyString description { DeprecatedString::empty() };

    constexpr LoaderError() = default;
    LoaderError(Category category, size_t index, DeprecatedFlyString description)
        : category(category)
        , index(index)
        , description(move(description))
    {
    }
    LoaderError(DeprecatedFlyString description)
        : description(move(description))
    {
    }
    LoaderError(Category category, DeprecatedFlyString description)
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
            description = DeprecatedString::formatted("{} ({})", strerror(code), code);
            if (code == EBADF || code == EBUSY || code == EEXIST || code == EIO || code == EISDIR || code == ENOENT || code == ENOMEM || code == EPIPE)
                category = Category::IO;
        } else {
            description = error.string_literal();
        }
    }
};

}

// Convenience TRY-like macro to convert an Error to a LoaderError
#define LOADER_TRY(expression)                                     \
    ({                                                             \
        auto _temporary_result = (expression);                     \
        if (_temporary_result.is_error())                          \
            return LoaderError(_temporary_result.release_error()); \
        _temporary_result.release_value();                         \
    })
