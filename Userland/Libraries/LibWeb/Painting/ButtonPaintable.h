/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Painting/LabelablePaintable.h>

namespace Web::Painting {

class ButtonPaintable final : public LabelablePaintable {
    JS_CELL(ButtonPaintable, LabelablePaintable);
    JS_DECLARE_ALLOCATOR(ButtonPaintable);

public:
    static JS::NonnullGCPtr<ButtonPaintable> create(Layout::ButtonBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::ButtonBox const& layout_box() const;
    Layout::ButtonBox& layout_box();

private:
    ButtonPaintable(Layout::ButtonBox const&);
};

}
