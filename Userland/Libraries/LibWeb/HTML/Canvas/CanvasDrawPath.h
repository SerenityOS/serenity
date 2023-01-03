/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <LibWeb/HTML/Path2D.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvasdrawpath
class CanvasDrawPath {
public:
    virtual ~CanvasDrawPath() = default;

    virtual void begin_path() = 0;

    virtual void fill(DeprecatedString const& fill_rule) = 0;
    virtual void fill(Path2D& path, DeprecatedString const& fill_rule) = 0;

    virtual void stroke() = 0;
    virtual void stroke(Path2D const& path) = 0;

    virtual void clip() = 0;

protected:
    CanvasDrawPath() = default;
};

}
