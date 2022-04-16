/*
 * Copyright (c) 2019-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Samuel Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/Stream.h>
#include <ctype.h>

String title_casify(String const& dashy_name)
{
    auto parts = dashy_name.split('-');
    StringBuilder builder;
    for (auto& part : parts) {
        if (part.is_empty())
            continue;
        builder.append(toupper(part[0]));
        if (part.length() == 1)
            continue;
        builder.append(part.substring_view(1, part.length() - 1));
    }
    return builder.to_string();
}

String camel_casify(StringView dashy_name)
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
        builder.append(ch);
        if (part.length() == 1)
            continue;
        builder.append(part.substring_view(1, part.length() - 1));
    }
    return builder.to_string();
}

String snake_casify(String const& dashy_name)
{
    return dashy_name.replace("-", "_", true);
}

ErrorOr<JsonValue> read_entire_file_as_json(StringView filename)
{
    auto file = TRY(Core::Stream::File::open(filename, Core::Stream::OpenMode::Read));
    auto json_size = TRY(file->size());
    auto json_data = TRY(ByteBuffer::create_uninitialized(json_size));
    if (!file->read_or_error(json_data.bytes()))
        return Error::from_string_literal("Failed to read json file.");
    return JsonValue::from_string(json_data);
}
