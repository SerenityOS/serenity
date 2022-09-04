/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Path2D.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

JS::NonnullGCPtr<Path2D> Path2D::create_with_global_object(HTML::Window& window, Optional<Variant<JS::Handle<Path2D>, String>> const& path)
{
    return *window.heap().allocate<Path2D>(window.realm(), window, path);
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-path2d
Path2D::Path2D(HTML::Window& window, Optional<Variant<JS::Handle<Path2D>, String>> const& path)
    : PlatformObject(window.realm())
    , CanvasPath(static_cast<Bindings::PlatformObject&>(*this))
{
    set_prototype(&window.cached_web_prototype("Path2D"));

    // 1. Let output be a new Path2D object.
    // 2. If path is not given, then return output.
    if (!path.has_value())
        return;

    // 3. If path is a Path2D object, then add all subpaths of path to output and return output.
    //    (In other words, it returns a copy of the argument.)
    if (path->has<JS::Handle<Path2D>>()) {
        this->path() = path->get<JS::Handle<Path2D>>()->path();
        return;
    }

    dbgln("TODO: Implement constructing Path2D object with an SVG path string");

    // FIXME: 4. Let svgPath be the result of parsing and interpreting path according to SVG 2's rules for path data. [SVG]
    // FIXME: 5. Let (x, y) be the last point in svgPath.
    // FIXME: 6. Add all the subpaths, if any, from svgPath to output.
    // FIXME: 7. Create a new subpath in output with (x, y) as the only point in the subpath.
    // FIXME: 8. Return output.
}

Path2D::~Path2D() = default;

}
