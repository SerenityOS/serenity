/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/ListItemMarkerBox.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class MarkerPaintable final : public PaintableBox {
public:
    static NonnullRefPtr<MarkerPaintable> create(Layout::ListItemMarkerBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::ListItemMarkerBox const& layout_box() const;

private:
    MarkerPaintable(Layout::ListItemMarkerBox const&);
};

}
