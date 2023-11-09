/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibPDF/Value.h>

namespace PDF {

class Shading : public RefCounted<Shading> {
public:
    static PDFErrorOr<NonnullRefPtr<Shading>> create(Document*, NonnullRefPtr<Object>);

    virtual ~Shading() = default;
};

}
