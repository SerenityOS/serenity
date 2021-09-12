/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/URL.h>
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

}
