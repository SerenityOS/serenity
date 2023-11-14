/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Tuple.h>
#include <LibGfx/Forward.h>
#include <LibPDF/Document.h>
#include <LibPDF/Encoding.h>

namespace PDF {

class Renderer;

class PDFFont : public RefCounted<PDFFont> {
public:
    static PDFErrorOr<NonnullRefPtr<PDFFont>> create(Document*, NonnullRefPtr<DictObject> const&, float font_size);

    virtual ~PDFFont() = default;

    virtual void set_font_size(float font_size) = 0;
    virtual PDFErrorOr<Gfx::FloatPoint> draw_string(Gfx::Painter&, Gfx::FloatPoint, DeprecatedString const&, Renderer const&) = 0;

protected:
    virtual PDFErrorOr<void> initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size);
    static PDFErrorOr<NonnullRefPtr<Gfx::Font>> replacement_for(StringView name, float font_size);
};

}
