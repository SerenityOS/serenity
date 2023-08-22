/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChessSettingsWidget.h"
#include <Applications/GamesSettings/ChessSettingsWidgetGML.h>
#include <LibChess/Chess.h>
#include <LibConfig/Client.h>
#include <LibCore/Directory.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Frame.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Painter.h>

namespace GamesSettings {

struct BoardTheme {
    StringView name;
    Color dark_square_color;
    Color light_square_color;
};

// The following colors have been taken from lichess.org, but I'm pretty sure they took them from chess.com.
Array<BoardTheme, 3> s_board_themes {
    BoardTheme { "Beige"sv, Color::from_rgb(0xb58863), Color::from_rgb(0xf0d9b5) },
    BoardTheme { "Blue"sv, Color::from_rgb(0x8ca2ad), Color::from_rgb(0xdee3e6) },
    BoardTheme { "Green"sv, Color::from_rgb(0x86a666), Color::from_rgb(0xffffdd) },
};

static BoardTheme& get_board_theme(StringView name)
{
    for (size_t i = 0; i < s_board_themes.size(); ++i) {
        if (s_board_themes[i].name == name)
            return s_board_themes[i];
    }
    return s_board_themes[0];
}

class BoardThemeModel final : public GUI::Model {
public:
    static ErrorOr<NonnullRefPtr<BoardThemeModel>> create()
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) BoardThemeModel());
    }

    ~BoardThemeModel() = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override
    {
        return s_board_themes.size();
    }

    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override
    {
        return 1;
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role = GUI::ModelRole::Display) const override
    {
        if (role != GUI::ModelRole::Display)
            return {};
        if (!is_within_range(index))
            return {};

        return s_board_themes.at(index.row()).name;
    }

private:
    BoardThemeModel()
    {
    }
};

static ErrorOr<RefPtr<Gfx::Bitmap>> load_piece_image(StringView set, StringView image)
{
    auto path = TRY(String::formatted("/res/graphics/chess/sets/{}/{}", set, image));
    return Gfx::Bitmap::load_from_file(path.bytes_as_string_view());
}

class ChessGamePreview final : public GUI::Frame {
    C_OBJECT_ABSTRACT(ChessGamePreview)

public:
    static ErrorOr<NonnullRefPtr<ChessGamePreview>> try_create()
    {
        auto preview = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ChessGamePreview()));
        return preview;
    }

    virtual ~ChessGamePreview() = default;

    ErrorOr<void> set_piece_set_name(DeprecatedString const& piece_set_name)
    {
        if (m_piece_set_name == piece_set_name.view())
            return {};

        m_piece_set_name = TRY(String::from_deprecated_string(piece_set_name));
        m_piece_images.clear();

        m_piece_images.set({ Chess::Color::White, Chess::Type::Pawn }, TRY(load_piece_image(m_piece_set_name, "white-pawn.png"sv)));
        m_piece_images.set({ Chess::Color::Black, Chess::Type::Pawn }, TRY(load_piece_image(m_piece_set_name, "black-pawn.png"sv)));
        m_piece_images.set({ Chess::Color::White, Chess::Type::Knight }, TRY(load_piece_image(m_piece_set_name, "white-knight.png"sv)));
        m_piece_images.set({ Chess::Color::Black, Chess::Type::Knight }, TRY(load_piece_image(m_piece_set_name, "black-knight.png"sv)));
        m_piece_images.set({ Chess::Color::White, Chess::Type::Bishop }, TRY(load_piece_image(m_piece_set_name, "white-bishop.png"sv)));
        m_piece_images.set({ Chess::Color::Black, Chess::Type::Bishop }, TRY(load_piece_image(m_piece_set_name, "black-bishop.png"sv)));
        m_piece_images.set({ Chess::Color::White, Chess::Type::Rook }, TRY(load_piece_image(m_piece_set_name, "white-rook.png"sv)));
        m_piece_images.set({ Chess::Color::Black, Chess::Type::Rook }, TRY(load_piece_image(m_piece_set_name, "black-rook.png"sv)));
        m_piece_images.set({ Chess::Color::White, Chess::Type::Queen }, TRY(load_piece_image(m_piece_set_name, "white-queen.png"sv)));
        m_piece_images.set({ Chess::Color::Black, Chess::Type::Queen }, TRY(load_piece_image(m_piece_set_name, "black-queen.png"sv)));
        m_piece_images.set({ Chess::Color::White, Chess::Type::King }, TRY(load_piece_image(m_piece_set_name, "white-king.png"sv)));
        m_piece_images.set({ Chess::Color::Black, Chess::Type::King }, TRY(load_piece_image(m_piece_set_name, "black-king.png"sv)));

        update();

        return {};
    }

    void set_dark_square_color(Gfx::Color dark_square_color)
    {
        if (m_dark_square_color == dark_square_color)
            return;

        m_dark_square_color = dark_square_color;
        update();
    }

    void set_light_square_color(Gfx::Color light_square_color)
    {
        if (m_light_square_color == light_square_color)
            return;

        m_light_square_color = light_square_color;
        update();
    }

    void set_show_coordinates(bool show_coordinates)
    {
        if (m_show_coordinates == show_coordinates)
            return;

        m_show_coordinates = show_coordinates;
        update();
    }

private:
    ChessGamePreview() = default;

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Frame::paint_event(event);

        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.add_clip_rect(frame_inner_rect());

        auto& coordinate_font = Gfx::FontDatabase::default_font().bold_variant();

        // To show all the piece graphics, we need at least 12 squares visible.
        // With the same preview size as we use for card games, a nice fit is 2 ranks of 6.
        // There are definitely better ways of doing this, but it'll do. ;^)
        auto square_size = 61;
        auto square_margin = square_size / 10;

        auto rect_for_square = [&](Chess::Square const& square) {
            return Gfx::IntRect {
                frame_inner_rect().left() + square.file * square_size,
                frame_inner_rect().bottom() - (square.rank + 1) * square_size,
                square_size,
                square_size
            };
        };

        for (int rank = 0; rank < 3; ++rank) {
            for (int file = 0; file < 8; ++file) {
                Chess::Square square { rank, file };
                auto square_rect = rect_for_square(square);
                painter.fill_rect(square_rect, square.is_light() ? m_light_square_color : m_dark_square_color);

                if (m_show_coordinates) {
                    auto text_color = square.is_light() ? m_dark_square_color : m_light_square_color;
                    auto shrunken_rect = square_rect.shrunken(4, 4);

                    if (square.rank == 0) {
                        auto file_char = square.file_char();
                        painter.draw_text(shrunken_rect, { &file_char, 1 }, coordinate_font, Gfx::TextAlignment::BottomRight, text_color);
                    }

                    if (square.file == 0) {
                        auto rank_char = square.rank_char();
                        painter.draw_text(shrunken_rect, { &rank_char, 1 }, coordinate_font, Gfx::TextAlignment::TopLeft, text_color);
                    }
                }
            }
        }

        auto draw_piece = [&](Chess::Piece const& piece, Chess::Square const& square) {
            auto& bitmap = *m_piece_images.get(piece).value();
            painter.draw_scaled_bitmap(
                rect_for_square(square).shrunken(square_margin, square_margin, square_margin, square_margin),
                bitmap,
                bitmap.rect(),
                1.0f,
                Gfx::Painter::ScalingMode::BilinearBlend);
        };

        draw_piece({ Chess::Color::White, Chess::Type::King }, { 0, 0 });
        draw_piece({ Chess::Color::Black, Chess::Type::King }, { 1, 0 });
        draw_piece({ Chess::Color::White, Chess::Type::Queen }, { 0, 1 });
        draw_piece({ Chess::Color::Black, Chess::Type::Queen }, { 1, 1 });
        draw_piece({ Chess::Color::White, Chess::Type::Rook }, { 0, 2 });
        draw_piece({ Chess::Color::Black, Chess::Type::Rook }, { 1, 2 });
        draw_piece({ Chess::Color::White, Chess::Type::Bishop }, { 0, 3 });
        draw_piece({ Chess::Color::Black, Chess::Type::Bishop }, { 1, 3 });
        draw_piece({ Chess::Color::White, Chess::Type::Knight }, { 0, 4 });
        draw_piece({ Chess::Color::Black, Chess::Type::Knight }, { 1, 4 });
        draw_piece({ Chess::Color::White, Chess::Type::Pawn }, { 0, 5 });
        draw_piece({ Chess::Color::Black, Chess::Type::Pawn }, { 1, 5 });
    }

    HashMap<Chess::Piece, RefPtr<Gfx::Bitmap>> m_piece_images;

    Gfx::Color m_dark_square_color { s_board_themes[0].dark_square_color };
    Gfx::Color m_light_square_color { s_board_themes[0].light_square_color };
    bool m_show_coordinates { true };
    String m_piece_set_name;
};

ErrorOr<NonnullRefPtr<ChessSettingsWidget>> ChessSettingsWidget::try_create()
{
    auto chess_settings_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ChessSettingsWidget));
    TRY(chess_settings_widget->initialize());
    return chess_settings_widget;
}

ErrorOr<void> ChessSettingsWidget::initialize()
{
    TRY(load_from_gml(chess_settings_widget_gml));

    auto piece_set_name = Config::read_string("Games"sv, "Chess"sv, "PieceSet"sv, "Classic"sv);
    auto board_theme = get_board_theme(Config::read_string("Games"sv, "Chess"sv, "BoardTheme"sv, "Beige"sv));
    auto show_coordinates = Config::read_bool("Games"sv, "Chess"sv, "ShowCoordinates"sv, true);

    m_preview = find_descendant_of_type_named<ChessGamePreview>("chess_preview");

    m_piece_set_combobox = find_descendant_of_type_named<GUI::ComboBox>("piece_set");
    TRY(Core::Directory::for_each_entry("/res/graphics/chess/sets/"sv, Core::DirIterator::SkipParentAndBaseDir, [&](auto const& entry, auto&) -> ErrorOr<IterationDecision> {
        TRY(m_piece_sets.try_append(entry.name));
        return IterationDecision::Continue;
    }));
    auto piece_set_model = GUI::ItemListModel<DeprecatedString>::create(m_piece_sets);
    m_piece_set_combobox->set_model(piece_set_model);
    m_piece_set_combobox->set_text(piece_set_name, GUI::AllowCallback::No);
    m_piece_set_combobox->on_change = [&](auto& value, auto&) {
        set_modified(true);
        m_preview->set_piece_set_name(value).release_value_but_fixme_should_propagate_errors();
    };

    m_board_theme_combobox = find_descendant_of_type_named<GUI::ComboBox>("board_theme");
    m_board_theme_combobox->set_model(TRY(BoardThemeModel::create()));
    m_board_theme_combobox->set_text(board_theme.name, GUI::AllowCallback::No);
    m_board_theme_combobox->on_change = [&](auto&, auto& index) {
        set_modified(true);

        auto& theme = s_board_themes[index.row()];
        m_preview->set_dark_square_color(theme.dark_square_color);
        m_preview->set_light_square_color(theme.light_square_color);
    };

    m_show_coordinates_checkbox = find_descendant_of_type_named<GUI::CheckBox>("show_coordinates");
    m_show_coordinates_checkbox->set_checked(show_coordinates, GUI::AllowCallback::No);
    m_show_coordinates_checkbox->on_checked = [&](bool checked) {
        set_modified(true);
        m_preview->set_show_coordinates(checked);
    };

    m_highlight_checks_checkbox = find_descendant_of_type_named<GUI::CheckBox>("highlight_checks");
    m_highlight_checks_checkbox->set_checked(show_coordinates, GUI::AllowCallback::No);
    m_highlight_checks_checkbox->on_checked = [&](bool) {
        set_modified(true);
    };

    TRY(m_preview->set_piece_set_name(piece_set_name));
    m_preview->set_dark_square_color(board_theme.dark_square_color);
    m_preview->set_light_square_color(board_theme.light_square_color);
    m_preview->set_show_coordinates(show_coordinates);

    return {};
}

void ChessSettingsWidget::apply_settings()
{
    Config::write_string("Games"sv, "Chess"sv, "PieceSet"sv, m_piece_set_combobox->text());
    Config::write_string("Games"sv, "Chess"sv, "BoardTheme"sv, m_board_theme_combobox->text());
    Config::write_bool("Games"sv, "Chess"sv, "ShowCoordinates"sv, m_show_coordinates_checkbox->is_checked());
    Config::write_bool("Games"sv, "Chess"sv, "HighlightChecks"sv, m_highlight_checks_checkbox->is_checked());
}

void ChessSettingsWidget::reset_default_values()
{
    // FIXME: `set_text()` on a combobox doesn't trigger the `on_change` callback, but it probably should!
    //        Until then, we have to manually tell the preview to update.
    m_piece_set_combobox->set_text("Classic");
    (void)m_preview->set_piece_set_name("Classic");
    auto& board_theme = get_board_theme("Beige"sv);
    m_board_theme_combobox->set_text(board_theme.name);
    m_preview->set_dark_square_color(board_theme.dark_square_color);
    m_preview->set_light_square_color(board_theme.light_square_color);
    m_show_coordinates_checkbox->set_checked(true);
    m_highlight_checks_checkbox->set_checked(true);
}

}

REGISTER_WIDGET(GamesSettings, ChessGamePreview);
