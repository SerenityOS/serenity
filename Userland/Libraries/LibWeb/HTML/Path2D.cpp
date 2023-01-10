/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/Path2D.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGPathElement.h>

namespace Web::HTML {

JS::NonnullGCPtr<Path2D> Path2D::construct_impl(JS::Realm& realm, Optional<Variant<JS::Handle<Path2D>, DeprecatedString>> const& path)
{
    return realm.heap().allocate<Path2D>(realm, realm, path);
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-path2d
Path2D::Path2D(JS::Realm& realm, Optional<Variant<JS::Handle<Path2D>, DeprecatedString>> const& path)
    : PlatformObject(realm)
    , CanvasPath(static_cast<Bindings::PlatformObject&>(*this))
{
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

    // 4. Let svgPath be the result of parsing and interpreting path according to SVG 2's rules for path data. [SVG]
    auto path_instructions = SVG::AttributeParser::parse_path_data(path->get<DeprecatedString>());
    auto svg_path = SVG::path_from_path_instructions(path_instructions);

    if (!svg_path.segments().is_empty()) {
        // 5. Let (x, y) be the last point in svgPath.
        auto xy = svg_path.segments().last().point();

        // 6. Add all the subpaths, if any, from svgPath to output.
        this->path() = move(svg_path);

        // 7. Create a new subpath in output with (x, y) as the only point in the subpath.
        this->move_to(xy.x(), xy.y());
    }

    // 8. Return output.
}

Path2D::~Path2D() = default;

void Path2D::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::Path2DPrototype>(realm, "Path2D"));
}

}
