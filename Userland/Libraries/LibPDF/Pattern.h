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
    static PDFErrorOr<bool> is_type2(Document*, NonnullRefPtr<Object>);
    static PDFErrorOr<NonnullRefPtr<Pattern>> create(Document*, NonnullRefPtr<Object>, Renderer&);

    static PDFErrorOr<ColorOrStyle> style(Document*, NonnullRefPtr<Object>, Renderer&);
};

}
