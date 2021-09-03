/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "XSV.h"
#include <YAK/Forward.h>
#include <YAK/StringView.h>

namespace Reader {

class CSV : public XSV {
public:
    CSV(StringView source, ParserBehaviour behaviours = default_behaviours())
        : XSV(source, { ",", "\"", ParserTraits::Repeat }, behaviours)
    {
    }
};

}
