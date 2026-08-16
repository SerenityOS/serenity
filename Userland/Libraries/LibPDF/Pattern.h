/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibPDF/ColorSpace.h>

namespace PDF {

class Renderer;

class Pattern : public RefCounted<Pattern> {
public:
    static PDFErrorOr<NonnullRefPtr<Pattern>> create(Document*, NonnullRefPtr<Object>, Renderer&);

    virtual ~Pattern() = default;
    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) = 0;
};

}
