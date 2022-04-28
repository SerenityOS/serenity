/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Percentage.h>

namespace Web::CSS {

struct LengthBox {
    LengthPercentage top { Length::make_auto() };
    LengthPercentage right { Length::make_auto() };
    LengthPercentage bottom { Length::make_auto() };
    LengthPercentage left { Length::make_auto() };
};

}
