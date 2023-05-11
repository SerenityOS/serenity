/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/Variant.h>
#include <LibWeb/CSS/Length.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/images.html#image-source
struct ImageSource {
    struct PixelDensityDescriptorValue {
        float value { 0 };
    };

    struct WidthDescriptorValue {
        CSSPixels value { 0 };
    };

    String url;
    Variant<Empty, PixelDensityDescriptorValue, WidthDescriptorValue> descriptor;
};

struct ImageSourceAndPixelDensity {
    ImageSource source;
    float pixel_density { 1.0f };
};

// https://html.spec.whatwg.org/multipage/images.html#source-set
struct SourceSet {
    static SourceSet create(DOM::Document const&, String default_source, String srcset, String sizes);

    [[nodiscard]] bool is_empty() const;

    // https://html.spec.whatwg.org/multipage/images.html#select-an-image-source-from-a-source-set
    [[nodiscard]] ImageSourceAndPixelDensity select_an_image_source();

    // https://html.spec.whatwg.org/multipage/images.html#normalise-the-source-densities
    void normalize_source_densities();

    SourceSet();

    Vector<ImageSource> m_sources;
    CSS::Length m_source_size;
};

SourceSet parse_a_srcset_attribute(StringView);
CSS::Length parse_a_sizes_attribute(DOM::Document const&, StringView);

}
