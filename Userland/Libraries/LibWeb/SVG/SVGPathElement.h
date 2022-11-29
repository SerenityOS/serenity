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
    WEB_PLATFORM_OBJECT(SVGPathElement, SVGGeometryElement);

public:
    virtual ~SVGPathElement() override = default;

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    virtual Gfx::Path& get_path() override;

private:
    SVGPathElement(DOM::Document&, DOM::QualifiedName);

    Vector<PathInstruction> m_instructions;
    Optional<Gfx::Path> m_path;
};

Gfx::Path path_from_path_instructions(Span<PathInstruction const>);

}
