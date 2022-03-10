/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

class CheckBoxPaintable final : public PaintableBox {
public:
    static NonnullOwnPtr<CheckBoxPaintable> create(Layout::CheckBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::CheckBox const& layout_box() const;

private:
    CheckBoxPaintable(Layout::CheckBox const&);
};

}
