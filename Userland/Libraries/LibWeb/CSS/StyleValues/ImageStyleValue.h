/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/StyleValues/AbstractImageStyleValue.h>

namespace Web::CSS {

class ImageStyleValue final
    : public AbstractImageStyleValue
    , public Weakable<ImageStyleValue> {
public:
    static ValueComparingNonnullRefPtr<ImageStyleValue> create(AK::URL const& url)
    {
        return adopt_ref(*new (nothrow) ImageStyleValue(url));
    }
    virtual ~ImageStyleValue() override = default;

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

    virtual void load_any_resources(DOM::Document&) override;

    Optional<CSSPixels> natural_width() const override;
    Optional<CSSPixels> natural_height() const override;

    virtual bool is_paintable() const override;
    void paint(PaintContext& context, DevicePixelRect const& dest_rect, CSS::ImageRendering image_rendering) const override;

    Function<void()> on_animate;

    RefPtr<HTML::DecodedImageData const> image_data() const;

private:
    ImageStyleValue(AK::URL const&);

    JS::Handle<HTML::SharedImageRequest> m_image_request;

    void animate();
    Gfx::Bitmap const* bitmap(size_t frame_index, Gfx::IntSize = {}) const;

    AK::URL m_url;
    WeakPtr<DOM::Document> m_document;

    size_t m_current_frame_index { 0 };
    size_t m_loops_completed { 0 };
    RefPtr<Platform::Timer> m_timer;
};

}
