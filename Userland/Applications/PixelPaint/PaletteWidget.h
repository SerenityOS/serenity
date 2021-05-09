/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace PixelPaint {

class ImageEditor;

class PaletteWidget final : public GUI::Frame {
    C_OBJECT(PaletteWidget);

public:
    virtual ~PaletteWidget() override;

    void set_primary_color(Color);
    void set_secondary_color(Color);

private:
    explicit PaletteWidget(ImageEditor&);

    ImageEditor& m_editor;
    RefPtr<GUI::Frame> m_primary_color_widget;
    RefPtr<GUI::Frame> m_secondary_color_widget;
};

}
