/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Forward.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class VideoPaintable final : public PaintableBox {
    JS_CELL(VideoPaintable, PaintableBox);

public:
    static JS::NonnullGCPtr<VideoPaintable> create(Layout::VideoBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::VideoBox const& layout_box() const;

private:
    VideoPaintable(Layout::VideoBox const&);
};

}
