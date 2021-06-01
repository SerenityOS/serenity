/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

namespace Web {

struct URLQueryParam {
    String name;
    String value;
};

String urlencode(const Vector<URLQueryParam>&, URL::PercentEncodeSet);

}
