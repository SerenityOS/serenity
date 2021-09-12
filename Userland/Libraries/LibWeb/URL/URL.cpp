/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibWeb/URL/URL.h>

namespace Web::URL {

String url_encode(const Vector<QueryParam>& pairs, AK::URL::PercentEncodeSet percent_encode_set)
{
    StringBuilder builder;
    for (size_t i = 0; i < pairs.size(); ++i) {
        builder.append(AK::URL::percent_encode(pairs[i].name, percent_encode_set));
        builder.append('=');
        builder.append(AK::URL::percent_encode(pairs[i].value, percent_encode_set));
        if (i != pairs.size() - 1)
            builder.append('&');
    }
    return builder.to_string();
}

Vector<QueryParam> url_decode(StringView const& input)
{
    // 1. Let sequences be the result of splitting input on 0x26 (&).
    auto sequences = input.split_view('&');

    // 2. Let output be an initially empty list of name-value tuples where both name and value hold a string.
    Vector<QueryParam> output;

    // 3. For each byte sequence bytes in sequences:
    for (auto bytes : sequences) {
        // 1. If bytes is the empty byte sequence, then continue.
        if (bytes.is_empty())
            continue;

        StringView name;
        StringView value;

        // 2. If bytes contains a 0x3D (=), then let name be the bytes from the start of bytes up to but excluding its first 0x3D (=), and let value be the bytes, if any, after the first 0x3D (=) up to the end of bytes. If 0x3D (=) is the first byte, then name will be the empty byte sequence. If it is the last, then value will be the empty byte sequence.
        if (auto index = bytes.find('='); index.has_value()) {
            name = bytes.substring_view(0, *index);
            value = bytes.substring_view(*index + 1);
        }
        // 3. Otherwise, let name have the value of bytes and let value be the empty byte sequence.
        else {
            name = bytes;
            value = ""sv;
        }

        // 4. Replace any 0x2B (+) in name and value with 0x20 (SP).
        auto space_decoded_name = name.replace("+"sv, " "sv, true);

        // 5. Let nameString and valueString be the result of running UTF-8 decode without BOM on the percent-decoding of name and value, respectively.
        auto name_string = AK::URL::percent_decode(space_decoded_name);
        auto value_string = AK::URL::percent_decode(value);

        output.empend(move(name_string), move(value_string));
    }

    return output;
}

}
