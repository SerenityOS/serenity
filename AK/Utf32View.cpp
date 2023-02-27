/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>

namespace AK {

Optional<u32> Utf32CodePointIterator::peek(size_t offset) const
{
    if (offset == 0) {
        if (this->done())
            return {};
        return this->operator*();
    }

    auto new_iterator = *this;
    for (size_t index = 0; index < offset; ++index) {
        ++new_iterator;
        if (new_iterator.done())
            return {};
    }

    return *new_iterator;
}

ErrorOr<void> Formatter<Utf32View>::format(FormatBuilder& builder, Utf32View const& string)
{
    return builder.builder().try_append(string);
}

}
