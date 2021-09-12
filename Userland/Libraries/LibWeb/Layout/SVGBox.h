/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/SVG/SVGElement.h>

namespace Web::Layout {

class SVGBox : public ReplacedBox {
public:
    SVGBox(DOM::Document&, SVG::SVGElement&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~SVGBox() override = default;

    virtual void before_children_paint(PaintContext& context, PaintPhase phase) override;
    virtual void after_children_paint(PaintContext& context, PaintPhase phase) override;
};

}
