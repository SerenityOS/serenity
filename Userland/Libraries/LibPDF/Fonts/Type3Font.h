/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPDF/Fonts/SimpleFont.h>

namespace PDF {

class Type3Font : public SimpleFont {
public:
    Optional<float> get_glyph_width(u8 char_code) const override;
    void set_font_size(float font_size) override;
    PDFErrorOr<void> draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Renderer const&) override;

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float font_size) override;

private:
    HashMap<DeprecatedFlyString, NonnullRefPtr<StreamObject>> m_char_procs;
    Gfx::AffineTransform m_font_matrix;
    Optional<NonnullRefPtr<DictObject>> m_resources;
};

}
