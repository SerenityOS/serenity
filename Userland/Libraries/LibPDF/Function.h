/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibPDF/Value.h>

namespace PDF {

class Function : public RefCounted<Function> {
public:
    static PDFErrorOr<NonnullRefPtr<Function>> create(Document*, NonnullRefPtr<Object>);
    virtual ~Function() = default;
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const = 0;
};

}
