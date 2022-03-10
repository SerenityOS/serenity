/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/RadioButton.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

class RadioButtonPaintable final : public Paintable {
public:
    static NonnullOwnPtr<RadioButtonPaintable> create(Layout::RadioButton const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::RadioButton const& layout_box() const;

private:
    RadioButtonPaintable(Layout::RadioButton const&);
};

}
