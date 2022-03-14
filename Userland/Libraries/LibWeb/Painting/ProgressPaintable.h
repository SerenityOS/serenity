/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/Progress.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

// FIXME: ProgressPaintable should inherit from LabelablePaintable, as it is a LabelableNode.
//        LabelablePaintable should be split into FormAssociatedLabelablePaintable once this
//        happens.
class ProgressPaintable final : public PaintableBox {
public:
    static NonnullRefPtr<ProgressPaintable> create(Layout::Progress const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::Progress const& layout_box() const;

private:
    ProgressPaintable(Layout::Progress const&);
};

}
