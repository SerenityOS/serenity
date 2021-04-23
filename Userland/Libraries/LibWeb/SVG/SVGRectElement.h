/*
 * Copyright (c) 2021, Simon Danner <danner.simon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

class SVGRectElement final : public SVGGeometryElement {
public:
    using WrapperType = Bindings::SVGPathElementWrapper;

    SVGRectElement(DOM::Document& doc, QualifiedName name)
        : SVGGeometryElement(doc, name)
    {
        dbgln("constructed rect");
    };
    virtual ~SVGRectElement() override = default;

    virtual RefPtr<Layout::Node> create_layout_node() override;

    virtual void parse_attribute(const FlyString& name, const String& value) override;

    int x;
    int y;

private:
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
