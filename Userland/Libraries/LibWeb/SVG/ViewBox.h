/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::SVG {

struct ViewBox {
    float min_x { 0 };
    float min_y { 0 };
    float width { 0 };
    float height { 0 };
};

Optional<ViewBox> try_parse_view_box(StringView);

}
