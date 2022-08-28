/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGfx/Path.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/HTML/Canvas/CanvasPath.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#path2d
class Path2D
    : public RefCounted<Path2D>
    , public Bindings::Wrappable
    , public CanvasPath {

    AK_MAKE_NONCOPYABLE(Path2D);
    AK_MAKE_NONMOVABLE(Path2D);

public:
    using WrapperType = Bindings::Path2DWrapper;

    static NonnullRefPtr<Path2D> create_with_global_object(HTML::Window&, Optional<Variant<NonnullRefPtr<Path2D>, String>> const& path) { return adopt_ref(*new Path2D(path)); }
    static NonnullRefPtr<Path2D> create(Optional<Variant<NonnullRefPtr<Path2D>, String>> const& path) { return adopt_ref(*new Path2D(path)); }
    ~Path2D() = default;

private:
    Path2D(Optional<Variant<NonnullRefPtr<Path2D>, String>> const&);
};

}
