/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace HTTP {

// https://fetch.spec.whatwg.org/#concept-header
struct Header {
    // FIXME: These should be byte sequences.
    String name;
    String value;
};

}
