/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/URL.h>

namespace Web::CSS {

class FontFace {
public:
    struct Source {
        AK::URL url;
    };

    FontFace(FlyString font_family, Vector<Source> sources);
    ~FontFace() = default;

    FlyString font_family() const { return m_font_family; }
    Vector<Source> const& sources() const { return m_sources; }
    // FIXME: font-style, font-weight, font-stretch, unicode-range, font-feature-settings

private:
    FlyString m_font_family;
    Vector<Source> m_sources;
};

}
