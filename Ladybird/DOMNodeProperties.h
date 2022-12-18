/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace Ladybird {

struct DOMNodeProperties {
    String computed_style_json;
    String resolved_style_json;
    String custom_properties_json;
};

}
