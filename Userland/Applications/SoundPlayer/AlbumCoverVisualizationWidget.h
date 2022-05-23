/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "VisualizationWidget.h"
#include <LibGUI/Frame.h>

class AlbumCoverVisualizationWidget final : public VisualizationWidget {
    C_OBJECT(AlbumCoverVisualizationWidget)

public:
    ~AlbumCoverVisualizationWidget() override = default;
    void start_new_file(StringView) override;

private:
    void render(GUI::PaintEvent&, FixedArray<float> const&) override { }
    void paint_event(GUI::PaintEvent&) override;
    AlbumCoverVisualizationWidget() = default;
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> get_album_cover(StringView const filename);

    RefPtr<Gfx::Bitmap> m_serenity_bg;
    RefPtr<Gfx::Bitmap> m_album_cover;
};
