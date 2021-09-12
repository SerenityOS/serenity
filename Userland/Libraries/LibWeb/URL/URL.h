/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

namespace Web::URL {

struct QueryParam {
    String name;
    String value;
};
String url_encode(const Vector<QueryParam>&, AK::URL::PercentEncodeSet);

}
