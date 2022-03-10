/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

class ButtonPaintable final : public PaintableBox {
public:
    static NonnullOwnPtr<ButtonPaintable> create(Layout::ButtonBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::ButtonBox const& layout_box() const;

private:
    ButtonPaintable(Layout::ButtonBox const&);
};

}
