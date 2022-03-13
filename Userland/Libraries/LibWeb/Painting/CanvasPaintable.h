/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/CanvasBox.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class CanvasPaintable final : public PaintableBox {
public:
    static NonnullRefPtr<CanvasPaintable> create(Layout::CanvasBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::CanvasBox const& layout_box() const;

private:
    CanvasPaintable(Layout::CanvasBox const&);
};

}
