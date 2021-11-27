/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Audio {

struct LoaderError {

    enum Category : u32 {
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
    Category category { Unknown };
    // Binary index: where in the file the error occurred.
    size_t index { 0 };
    FlyString description { String::empty() };

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
};

}
