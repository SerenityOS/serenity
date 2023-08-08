/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/PaintStyle.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/HTML/Canvas/CanvasDrawImage.h>

namespace Web::HTML {

class CanvasPatternPaintStyle final : public Gfx::PaintStyle {
public:
    enum class Repetition {
        Repeat,
        RepeatX,
        RepeatY,
        NoRepeat
    };

    static ErrorOr<NonnullRefPtr<CanvasPatternPaintStyle>> create(Gfx::Bitmap const& bitmap, Repetition repetition)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) CanvasPatternPaintStyle(bitmap, repetition));
    }

    virtual void paint(Gfx::IntRect physical_bounding_box, PaintFunction paint) const override;

private:
    CanvasPatternPaintStyle(Gfx::Bitmap const& bitmap, Repetition repetition)
        : m_bitmap(bitmap)
        , m_repetition(repetition)
    {
    }

    NonnullRefPtr<Gfx::Bitmap const> m_bitmap;
    Repetition m_repetition { Repetition::Repeat };
};

class CanvasPattern final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(CanvasPattern, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::GCPtr<CanvasPattern>> create(JS::Realm&, CanvasImageSource const& image, StringView repetition);

    ~CanvasPattern();

    NonnullRefPtr<Gfx::PaintStyle> to_gfx_paint_style() { return m_pattern; }

private:
    CanvasPattern(JS::Realm&, CanvasPatternPaintStyle&);

    virtual void initialize(JS::Realm&) override;

    NonnullRefPtr<CanvasPatternPaintStyle> m_pattern;
};

}
