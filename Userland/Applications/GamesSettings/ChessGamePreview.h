/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibChess/Chess.h>
#include <LibConfig/Client.h>
#include <LibCore/Directory.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Frame.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Painter.h>

namespace GamesSettings {

class ChessGamePreview final : public GUI::Frame {
    C_OBJECT_ABSTRACT(ChessGamePreview)

public:
    static ErrorOr<NonnullRefPtr<ChessGamePreview>> try_create();

    virtual ~ChessGamePreview() = default;

    void set_piece_set_name(String piece_set_name);
    void set_dark_square_color(Gfx::Color dark_square_color);
    void set_light_square_color(Gfx::Color light_square_color);
    void set_show_coordinates(bool show_coordinates);

private:
    ChessGamePreview();

    virtual void paint_event(GUI::PaintEvent& event) override;

    HashMap<Chess::Piece, RefPtr<Gfx::Bitmap>> m_piece_images;
    bool m_any_piece_images_are_missing { false };

    Gfx::Color m_dark_square_color;
    Gfx::Color m_light_square_color;
    bool m_show_coordinates { true };
    String m_piece_set_name;
};

}
