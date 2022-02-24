/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

class SVGPathElement final : public SVGGeometryElement {
public:
    using WrapperType = Bindings::SVGPathElementWrapper;

    SVGPathElement(DOM::Document&, DOM::QualifiedName);
    virtual ~SVGPathElement() override = default;

    virtual void parse_attribute(const FlyString& name, const String& value) override;

    virtual Gfx::Path& get_path() override;

private:
    Vector<PathInstruction> m_instructions;
    Gfx::FloatPoint m_previous_control_point = {};
    Optional<Gfx::Path> m_path;
};

}
