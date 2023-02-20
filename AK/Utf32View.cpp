/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>

namespace AK {

ErrorOr<void> Formatter<Utf32View>::format(FormatBuilder& builder, Utf32View const& string)
{
    return builder.builder().try_append(string);
}

}
