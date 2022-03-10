/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class ImagePaintable final : public PaintableBox {
public:
    static NonnullRefPtr<ImagePaintable> create(Layout::ImageBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::ImageBox const& layout_box() const;

private:
    ImagePaintable(Layout::ImageBox const&);
};

}
