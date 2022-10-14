/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/URL.h>
#include <LibWeb/CSS/UnicodeRange.h>

namespace Web::CSS {

class FontFace {
public:
    struct Source {
        AK::URL url;
        // FIXME: Do we need to keep this around, or is it only needed to discard unwanted formats during parsing?
        Optional<FlyString> format;
    };

    FontFace(FlyString font_family, Vector<Source> sources, Vector<UnicodeRange> unicode_ranges);
    ~FontFace() = default;

    FlyString font_family() const { return m_font_family; }
    Vector<Source> const& sources() const { return m_sources; }
    Vector<UnicodeRange> const& unicode_ranges() const { return m_unicode_ranges; }
    // FIXME: font-style, font-weight, font-stretch, font-feature-settings

private:
    FlyString m_font_family;
    Vector<Source> m_sources;
    Vector<UnicodeRange> m_unicode_ranges;
};

}
