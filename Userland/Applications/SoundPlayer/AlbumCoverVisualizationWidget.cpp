/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Nícolas F. R. A. Prado <n@nfraprado.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AlbumCoverVisualizationWidget.h"
#include <AK/LexicalPath.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Rect.h>

AlbumCoverVisualizationWidget::AlbumCoverVisualizationWidget(Function<RefPtr<Gfx::Bitmap>()> get_file_cover_from_player)
    : m_get_file_cover_from_player(move(get_file_cover_from_player))
{
}

void AlbumCoverVisualizationWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);
    GUI::Painter painter(*this);

    auto const& cover = m_file_cover ? m_file_cover : m_album_cover;
    if (cover) {
        auto album_cover_rect = cover->rect();

        auto height_ratio = frame_inner_rect().height() / (float)album_cover_rect.height();
        auto width_ratio = frame_inner_rect().width() / (float)album_cover_rect.width();
        auto scale = min(height_ratio, width_ratio);

        Gfx::IntRect fitted_rect = { 0, 0, (int)(album_cover_rect.width() * scale), (int)(album_cover_rect.height() * scale) };
        fitted_rect.center_within(frame_inner_rect());

        painter.draw_scaled_bitmap(fitted_rect, *cover, cover->rect(), 1.0f);
    } else {
        if (!m_serenity_bg)
            m_serenity_bg = Gfx::Bitmap::load_from_file("/res/wallpapers/sunset-retro.png"sv).release_value_but_fixme_should_propagate_errors();
        painter.draw_scaled_bitmap(frame_inner_rect(), *m_serenity_bg, m_serenity_bg->rect(), 1.0f);
    }
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> AlbumCoverVisualizationWidget::get_album_cover(StringView const filename)
{
    auto directory = LexicalPath::dirname(filename);

    static constexpr auto possible_cover_filenames = Array { "cover.png"sv, "cover.jpg"sv };
    for (auto& it : possible_cover_filenames) {
        LexicalPath cover_path = LexicalPath::join(directory, it);
        if (FileSystem::exists(cover_path.string()))
            return Gfx::Bitmap::load_from_file(cover_path.string());
    }

    return Error::from_string_literal("No cover file found");
}

void AlbumCoverVisualizationWidget::start_new_file(StringView filename)
{
    if (m_get_file_cover_from_player)
        m_file_cover = m_get_file_cover_from_player();

    if (m_file_cover)
        return;

    auto album_cover_or_error = get_album_cover(filename);
    if (album_cover_or_error.is_error())
        m_album_cover = nullptr;
    else
        m_album_cover = album_cover_or_error.value();
}
