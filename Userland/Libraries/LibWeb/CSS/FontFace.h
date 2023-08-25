/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
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
        Variant<String, AK::URL> local_or_url;
        // FIXME: Do we need to keep this around, or is it only needed to discard unwanted formats during parsing?
        Optional<FlyString> format;
    };

    FontFace(FlyString font_family, Optional<int> weight, Optional<int> slope, Vector<Source> sources, Vector<UnicodeRange> unicode_ranges);
    ~FontFace() = default;

    FlyString font_family() const { return m_font_family; }
    Optional<int> weight() const { return m_weight; }
    Optional<int> slope() const { return m_slope; }
    Vector<Source> const& sources() const { return m_sources; }
    Vector<UnicodeRange> const& unicode_ranges() const { return m_unicode_ranges; }
    // FIXME: font-stretch, font-feature-settings

private:
    FlyString m_font_family;
    Optional<int> m_weight { 0 };
    Optional<int> m_slope { 0 };
    Vector<Source> m_sources;
    Vector<UnicodeRange> m_unicode_ranges;
};

}
