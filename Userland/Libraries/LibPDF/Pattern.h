/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPDF/ColorSpace.h>

namespace PDF {

class Renderer;

class Pattern {
public:
    static PDFErrorOr<ColorOrStyle> style(Document*, NonnullRefPtr<Object>, Renderer&);
};

}
