/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Serializer.h>

namespace SQL {

void Serializer::serialize(String const& text)
{
    serialize<u32>((u32)text.length());
    if (!text.is_empty())
        write((u8 const*)text.characters(), text.length());
}

void Serializer::deserialize_to(String& text)
{
    auto length = deserialize<u32>();
    if (length > 0) {
        text = String(reinterpret_cast<char const*>(read(length)), length);
    } else {
        text = "";
    }
}

}
