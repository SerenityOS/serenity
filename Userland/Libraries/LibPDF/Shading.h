/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGfx/Forward.h>
#include <LibPDF/Value.h>

namespace PDF {

class Renderer;

class Shading : public RefCounted<Shading> {
public:
    static PDFErrorOr<NonnullRefPtr<Shading>> create(Document*, NonnullRefPtr<Object>, Renderer&);

    virtual ~Shading() = default;

    virtual PDFErrorOr<void> draw(Gfx::Painter&, Gfx::AffineTransform const&) = 0;
};

}
