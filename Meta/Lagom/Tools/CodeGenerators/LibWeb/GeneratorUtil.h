/*
 * Copyright (c) 2019-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <ctype.h>

ErrorOr<String> title_casify(StringView dashy_name)
{
    auto parts = dashy_name.split_view('-');
    StringBuilder builder;
    for (auto& part : parts) {
        if (part.is_empty())
            continue;
        TRY(builder.try_append(toupper(part[0])));
        if (part.length() == 1)
            continue;
        TRY(builder.try_append(part.substring_view(1, part.length() - 1)));
    }
    return builder.to_string();
}

ErrorOr<String> camel_casify(StringView dashy_name)
{
    auto parts = dashy_name.split_view('-');
    StringBuilder builder;
    bool first = true;
    for (auto& part : parts) {
        if (part.is_empty())
            continue;
        char ch = part[0];
        if (!first)
            ch = toupper(ch);
        else
            first = false;
        TRY(builder.try_append(ch));
        if (part.length() == 1)
            continue;
        TRY(builder.try_append(part.substring_view(1, part.length() - 1)));
    }
    return builder.to_string();
}

ErrorOr<String> snake_casify(StringView dashy_name)
{
    // FIXME: We don't really need to convert dashy_name to a String first, but currently
    //        all the `replace` functions that take a StringView return DeprecatedString.
    return TRY(String::from_utf8(dashy_name)).replace("-"sv, "_"sv, ReplaceMode::All);
}

ErrorOr<JsonValue> read_entire_file_as_json(StringView filename)
{
    auto file = TRY(Core::File::open(filename, Core::File::OpenMode::Read));
    auto json_size = TRY(file->size());
    auto json_data = TRY(ByteBuffer::create_uninitialized(json_size));
    TRY(file->read_until_filled(json_data.bytes()));
    return JsonValue::from_string(json_data);
}
