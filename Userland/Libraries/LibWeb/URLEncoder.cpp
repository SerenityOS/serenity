/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/URLParser.h>
#include <LibWeb/URLEncoder.h>

namespace Web {

String urlencode(const Vector<URLQueryParam>& pairs)
{
    StringBuilder builder;
    for (size_t i = 0; i < pairs.size(); ++i) {
        builder.append(urlencode(pairs[i].name));
        builder.append('=');
        builder.append(urlencode(pairs[i].value));
        if (i != pairs.size() - 1)
            builder.append('&');
    }
    return builder.to_string();
}

}
