/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChessWidget.h"
#include "PromotionDialog.h"
#include <AK/Random.h>
#include <AK/String.h>
#include <LibCore/Account.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Path.h>
#include <unistd.h>

ErrorOr<NonnullRefPtr<ChessWidget>> ChessWidget::try_create()
{
    auto widget = TRY(AK::adopt_nonnull_ref_or_enomem(new (nothrow) ChessWidget));
    widget->set_piece_set("Classic"sv);

    return widget;
}

void ChessWidget::paint_event(GUI::PaintEvent& event)
{
    int const min_size = min(width(), height());
    int const widget_offset_x = (window()->width() - min_size) / 2;
    int const widget_offset_y = (window()->height() - min_size) / 2;

    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(frame_inner_rect(), Color::Black);

    painter.translate(frame_thickness() + widget_offset_x, frame_thickness() + widget_offset_y);

    auto square_width = min_size / 8;
    auto square_height = min_size / 8;
    auto square_margin = square_width / 10;
    int coord_rank_file = (side() == Chess::Color::White) ? 0 : 7;

    Chess::Board& active_board = (m_playback ? board_playback() : board());

    auto& coordinate_font = Gfx::FontDatabase::default_font().bold_variant();

    Chess::Square::for_each([&](Chess::Square sq) {
        Gfx::IntRect tile_rect;
        if (side() == Chess::Color::White) {
            tile_rect = { sq.file * square_width, (7 - sq.rank) * square_height, square_width, square_height };
        } else {
            tile_rect = { (7 - sq.file) * square_width, sq.rank * square_height, square_width, square_height };
        }

        painter.fill_rect(tile_rect, (sq.is_light()) ? board_theme().light_square_color : board_theme().dark_square_color);

        if (active_board.last_move().has_value()) {
            auto const last_move = active_board.last_move().value();
            if (last_move.to == sq || last_move.from == sq)
                painter.fill_rect(tile_rect, m_move_highlight_color);

            auto const piece = active_board.get_piece(sq);
            if (m_highlight_checks && last_move.is_check && piece.type == Chess::Type::King && piece.color == active_board.turn()) {
                Array<Gfx::ColorStop, 2> colors = {
                    Gfx::ColorStop { .color = Color::Red, .position = 0.16f },
                    Gfx::ColorStop { .color = Color::Transparent, .position = .66f }
                };

                painter.fill_rect_with_radial_gradient(tile_rect, colors, tile_rect.center() - tile_rect.top_left(), tile_rect.size());
            }
        }

        if (m_coordinates) {
            auto text_color = (sq.is_light()) ? board_theme().dark_square_color : board_theme().light_square_color;

            auto shrunken_rect = tile_rect;
            shrunken_rect.shrink(4, 4);

            if (sq.rank == coord_rank_file) {
                auto file_char = sq.file_char();
                painter.draw_text(shrunken_rect, { &file_char, 1 }, coordinate_font, Gfx::TextAlignment::BottomRight, text_color);
            }

            if (sq.file == coord_rank_file) {
                auto rank_char = sq.rank_char();
                painter.draw_text(shrunken_rect, { &rank_char, 1 }, coordinate_font, Gfx::TextAlignment::TopLeft, text_color);
            }
        }

        for (auto& m : m_board_markings) {
            if (m.type() == BoardMarking::Type::Square && m.from == sq) {
                Gfx::Color color = m.secondary_color ? m_marking_secondary_color : (m.alternate_color ? m_marking_alternate_color : m_marking_primary_color);
                painter.fill_rect(tile_rect, color);
            }
        }

        if (!(m_dragging_piece && sq == m_moving_square)) {
            auto bmp = m_pieces.get(active_board.get_piece(sq));
            if (bmp.has_value()) {
                painter.draw_scaled_bitmap(tile_rect.shrunken(square_margin, square_margin, square_margin, square_margin), *bmp.value(), bmp.value()->rect(), 1.0f, Gfx::Painter::ScalingMode::BilinearBlend);
            }
        }

        return IterationDecision::Continue;
    });

    auto draw_arrow = [&painter](Gfx::FloatPoint A, Gfx::FloatPoint B, float w1, float w2, float h, Gfx::Color color) {
        float dx = B.x() - A.x();
        float dy = A.y() - B.y();
        float phi = atan2f(dy, dx);
        float hdx = h * cosf(phi);
        float hdy = h * sinf(phi);

        const auto cos_pi_2_phi = cosf(float { M_PI_2 } - phi);
        const auto sin_pi_2_phi = sinf(float { M_PI_2 } - phi);

        Gfx::FloatPoint A1(A.x() - (w1 / 2) * cos_pi_2_phi, A.y() - (w1 / 2) * sin_pi_2_phi);
        Gfx::FloatPoint B3(A.x() + (w1 / 2) * cos_pi_2_phi, A.y() + (w1 / 2) * sin_pi_2_phi);
        Gfx::FloatPoint A2(A1.x() + (dx - hdx), A1.y() - (dy - hdy));
        Gfx::FloatPoint B2(B3.x() + (dx - hdx), B3.y() - (dy - hdy));
        Gfx::FloatPoint A3(A2.x() - w2 * cos_pi_2_phi, A2.y() - w2 * sin_pi_2_phi);
        Gfx::FloatPoint B1(B2.x() + w2 * cos_pi_2_phi, B2.y() + w2 * sin_pi_2_phi);

        auto path = Gfx::Path();
        path.move_to(A);
        path.line_to(A1);
        path.line_to(A2);
        path.line_to(A3);
        path.line_to(B);
        path.line_to(B1);
        path.line_to(B2);
        path.line_to(B3);
        path.line_to(A);
        path.close();

        painter.fill_path(path, color, Gfx::Painter::WindingRule::EvenOdd);
    };

    for (auto& m : m_board_markings) {
        if (m.type() == BoardMarking::Type::Arrow) {
            Gfx::FloatPoint arrow_start;
            Gfx::FloatPoint arrow_end;

            if (side() == Chess::Color::White) {
                arrow_start = { m.from.file * square_width + square_width / 2.0f, (7 - m.from.rank) * square_height + square_height / 2.0f };
                arrow_end = { m.to.file * square_width + square_width / 2.0f, (7 - m.to.rank) * square_height + square_height / 2.0f };
            } else {
                arrow_start = { (7 - m.from.file) * square_width + square_width / 2.0f, m.from.rank * square_height + square_height / 2.0f };
                arrow_end = { (7 - m.to.file) * square_width + square_width / 2.0f, m.to.rank * square_height + square_height / 2.0f };
            }

            Gfx::Color color = m.secondary_color ? m_marking_secondary_color : (m.alternate_color ? m_marking_primary_color : m_marking_alternate_color);
            draw_arrow(arrow_start, arrow_end, square_width / 8.0f, square_width / 10.0f, square_height / 2.5f, color);
        }
    }

    if (m_dragging_piece) {
        if (m_show_available_moves) {
            Gfx::IntPoint move_point;
            Gfx::IntPoint point_offset = { square_width / 3, square_height / 3 };
            Gfx::IntSize rect_size = { square_width / 3, square_height / 3 };
            for (auto const& square : m_available_moves) {
                if (side() == Chess::Color::White) {
                    move_point = { square.file * square_width, (7 - square.rank) * square_height };
                } else {
                    move_point = { (7 - square.file) * square_width, square.rank * square_height };
                }

                Gfx::AntiAliasingPainter aa_painter { painter };
                aa_painter.fill_ellipse({ move_point + point_offset, rect_size }, Gfx::Color::LightGray);
            }
        }

        Gfx::IntRect origin_square;
        if (side() == Chess::Color::White) {
            origin_square = { m_moving_square.file * square_width, (7 - m_moving_square.rank) * square_height, square_width, square_height };
        } else {
            origin_square = { (7 - m_moving_square.file) * square_width, m_moving_square.rank * square_height, square_width, square_height };
        }
        painter.fill_rect(origin_square, m_move_highlight_color);

        auto bmp = m_pieces.get(active_board.get_piece(m_moving_square));
        if (bmp.has_value()) {
            auto center = m_drag_point - Gfx::IntPoint(square_width / 2, square_height / 2);
            painter.draw_scaled_bitmap({ center, { square_width, square_height } }, *bmp.value(), bmp.value()->rect(), 1.0f, Gfx::Painter::ScalingMode::BilinearBlend);
        }
    }
}

void ChessWidget::mousedown_event(GUI::MouseEvent& event)
{
    int const min_size = min(width(), height());
    int const widget_offset_x = (window()->width() - min_size) / 2;
    int const widget_offset_y = (window()->height() - min_size) / 2;

    if (!frame_inner_rect().contains(event.position()))
        return;

    auto square = mouse_to_square(event);
    if (event.button() == GUI::MouseButton::Secondary) {
        if (m_dragging_piece) {
            m_dragging_piece = false;
            set_override_cursor(Gfx::StandardCursor::None);
            m_available_moves.clear();
        } else if (square.has_value()) {
            m_current_marking.from = square.release_value();
        }
        return;
    }
    m_board_markings.clear();

    if (!square.has_value())
        return;

    auto piece = board().get_piece(square.value());
    if (drag_enabled() && piece.color == board().turn() && !m_playback) {
        m_dragging_piece = true;
        set_override_cursor(Gfx::StandardCursor::Drag);
        m_drag_point = { event.position().x() - widget_offset_x, event.position().y() - widget_offset_y };
        m_moving_square = square.value();

        m_board.generate_moves([&](Chess::Move move) {
            if (move.from == m_moving_square) {
                m_available_moves.append(move.to);
            }
            return IterationDecision::Continue;
        });
    }

    update();
}

void ChessWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (!frame_inner_rect().contains(event.position()))
        return;

    auto target_square = mouse_to_square(event);
    if (event.button() == GUI::MouseButton::Secondary) {
        if (!target_square.has_value())
            return;

        m_current_marking.secondary_color = event.shift();
        m_current_marking.alternate_color = event.ctrl();
        m_current_marking.to = target_square.release_value();
        auto match_index = m_board_markings.find_first_index(m_current_marking);
        if (match_index.has_value()) {
            m_board_markings.remove(match_index.value());
            update();
            return;
        }
        m_board_markings.append(m_current_marking);
        update();
        return;
    }

    if (!m_dragging_piece)
        return;

    m_dragging_piece = false;
    set_override_cursor(Gfx::StandardCursor::Hand);
    m_available_moves.clear();

    if (!target_square.has_value()) {
        update();
        return;
    }

    Chess::Move move = { m_moving_square, target_square.release_value() };
    if (board().is_promotion_move(move)) {
        auto promotion_dialog = PromotionDialog::construct(*this);
        if (promotion_dialog->exec() == PromotionDialog::ExecResult::OK)
            move.promote_to = promotion_dialog->selected_piece();
    }

    if (board().apply_move(move)) {
        m_playback_move_number = board().moves().size();
        m_playback = false;
        m_board_playback = m_board;
        // If two humans are playing, ask whether they wish to accept a draw.
        auto claim_draw_behavior = m_engine.is_null() ? ClaimDrawBehavior::Prompt : ClaimDrawBehavior::Always;
        if (!check_game_over(claim_draw_behavior))
            input_engine_move();
    }

    update();
}

void ChessWidget::mousemove_event(GUI::MouseEvent& event)
{
    int const min_size = min(width(), height());
    int const widget_offset_x = (window()->width() - min_size) / 2;
    int const widget_offset_y = (window()->height() - min_size) / 2;

    if (!frame_inner_rect().contains(event.position()))
        return;

    if (m_engine && board().turn() != side())
        return;

    if (!m_dragging_piece) {
        auto square = mouse_to_square(event);
        if (!square.has_value()) {
            set_override_cursor(Gfx::StandardCursor::None);
            return;
        }

        auto piece = board().get_piece(square.release_value());
        if (piece.color == board().turn())
            set_override_cursor(Gfx::StandardCursor::Hand);
        else
            set_override_cursor(Gfx::StandardCursor::None);
        return;
    }

    m_drag_point = { event.position().x() - widget_offset_x, event.position().y() - widget_offset_y };
    update();
}

void ChessWidget::keydown_event(GUI::KeyEvent& event)
{
    set_override_cursor(Gfx::StandardCursor::None);
    switch (event.key()) {
    case KeyCode::Key_Left:
        playback_move(PlaybackDirection::Backward);
        break;
    case KeyCode::Key_Right:
        playback_move(PlaybackDirection::Forward);
        break;
    case KeyCode::Key_Up:
        playback_move(PlaybackDirection::Last);
        break;
    case KeyCode::Key_Down:
        playback_move(PlaybackDirection::First);
        break;
    case KeyCode::Key_Home:
        playback_move(PlaybackDirection::First);
        break;
    case KeyCode::Key_End:
        playback_move(PlaybackDirection::Last);
        break;
    default:
        event.ignore();
        return;
    }
    update();
}

static constexpr StringView set_path = "/res/graphics/chess/sets/"sv;

static RefPtr<Gfx::Bitmap> get_piece(StringView set, StringView image)
{
    StringBuilder builder;
    builder.append(set_path);
    builder.append(set);
    builder.append('/');
    builder.append(image);
    return Gfx::Bitmap::load_from_file(builder.to_deprecated_string()).release_value_but_fixme_should_propagate_errors();
}

void ChessWidget::set_piece_set(StringView set)
{
    m_pieces.set({ Chess::Color::White, Chess::Type::Pawn }, get_piece(set, "white-pawn.png"sv));
    m_pieces.set({ Chess::Color::Black, Chess::Type::Pawn }, get_piece(set, "black-pawn.png"sv));
    m_pieces.set({ Chess::Color::White, Chess::Type::Knight }, get_piece(set, "white-knight.png"sv));
    m_pieces.set({ Chess::Color::Black, Chess::Type::Knight }, get_piece(set, "black-knight.png"sv));
    m_pieces.set({ Chess::Color::White, Chess::Type::Bishop }, get_piece(set, "white-bishop.png"sv));
    m_pieces.set({ Chess::Color::Black, Chess::Type::Bishop }, get_piece(set, "black-bishop.png"sv));
    m_pieces.set({ Chess::Color::White, Chess::Type::Rook }, get_piece(set, "white-rook.png"sv));
    m_pieces.set({ Chess::Color::Black, Chess::Type::Rook }, get_piece(set, "black-rook.png"sv));
    m_pieces.set({ Chess::Color::White, Chess::Type::Queen }, get_piece(set, "white-queen.png"sv));
    m_pieces.set({ Chess::Color::Black, Chess::Type::Queen }, get_piece(set, "black-queen.png"sv));
    m_pieces.set({ Chess::Color::White, Chess::Type::King }, get_piece(set, "white-king.png"sv));
    m_pieces.set({ Chess::Color::Black, Chess::Type::King }, get_piece(set, "black-king.png"sv));
}

Optional<Chess::Square> ChessWidget::mouse_to_square(GUI::MouseEvent& event) const
{
    int const min_size = min(width(), height());
    int const widget_offset_x = (window()->width() - min_size) / 2;
    int const widget_offset_y = (window()->height() - min_size) / 2;

    auto x = event.x() - widget_offset_x;
    auto y = event.y() - widget_offset_y;
    if (x < 0 || y < 0 || x > min_size || y > min_size)
        return {};

    int square_width = min_size / 8;
    int square_height = min_size / 8;

    auto rank = y / square_height;
    auto file = x / square_width;
    if (rank < 0 || file < 0 || rank > 7 || file > 7)
        return {};

    if (side() == Chess::Color::White)
        return Chess::Square { 7 - rank, file };

    return Chess::Square { rank, 7 - file };
}

RefPtr<Gfx::Bitmap const> ChessWidget::get_piece_graphic(Chess::Piece const& piece) const
{
    return m_pieces.get(piece).value();
}

void ChessWidget::reset()
{
    m_board_markings.clear();
    m_playback = false;
    m_playback_move_number = 0;
    m_board_playback = Chess::Board();
    m_board = Chess::Board();
    m_side = (get_random<u32>() % 2) ? Chess::Color::White : Chess::Color::Black;
    m_drag_enabled = true;
    if (m_engine)
        m_engine->start_new_game();

    input_engine_move();
    update();
}

void ChessWidget::set_board_theme(StringView name)
{
    // FIXME: Add some kind of themes.json
    // The following Colors have been taken from lichess.org, but i'm pretty sure they took them from chess.com.
    if (name == "Beige") {
        m_board_theme = { "Beige"sv, Color::from_rgb(0xb58863), Color::from_rgb(0xf0d9b5) };
    } else if (name == "Green") {
        m_board_theme = { "Green"sv, Color::from_rgb(0x86a666), Color::from_rgb(0xffffdd) };
    } else if (name == "Blue") {
        m_board_theme = { "Blue"sv, Color::from_rgb(0x8ca2ad), Color::from_rgb(0xdee3e6) };
    } else {
        set_board_theme("Beige"sv);
    }
}

bool ChessWidget::want_engine_move()
{
    if (!m_engine)
        return false;
    if (board().turn() == side() || board().game_finished())
        return false;
    return true;
}

void ChessWidget::input_engine_move()
{
    if (!want_engine_move())
        return;

    bool drag_was_enabled = drag_enabled();
    if (drag_was_enabled)
        set_drag_enabled(false);

    set_override_cursor(Gfx::StandardCursor::Wait);
    m_engine->get_best_move(board(), 4000, [this, drag_was_enabled](ErrorOr<Chess::Move> move) {
        set_override_cursor(Gfx::StandardCursor::None);
        if (!want_engine_move())
            return;
        set_drag_enabled(drag_was_enabled);
        if (!move.is_error()) {
            VERIFY(board().apply_move(move.release_value()));
            if (check_game_over(ClaimDrawBehavior::Prompt))
                return;
        }

        m_playback_move_number = m_board.moves().size();
        m_playback = false;
        m_board_markings.clear();
        update();
    });
}

void ChessWidget::playback_move(PlaybackDirection direction)
{
    if (m_board.moves().is_empty())
        return;

    m_playback = true;
    m_board_markings.clear();

    switch (direction) {
    case PlaybackDirection::Backward:
        if (m_playback_move_number == 0)
            return;
        m_board_playback = Chess::Board();
        for (size_t i = 0; i < m_playback_move_number - 1; i++)
            m_board_playback.apply_move(m_board.moves().at(i));
        m_playback_move_number--;
        break;
    case PlaybackDirection::Forward:
        if (m_playback_move_number + 1 > m_board.moves().size()) {
            m_playback = false;
            return;
        }
        m_board_playback.apply_move(m_board.moves().at(m_playback_move_number++));
        if (m_playback_move_number == m_board.moves().size())
            m_playback = false;
        break;
    case PlaybackDirection::First:
        m_board_playback = Chess::Board();
        m_playback_move_number = 0;
        break;
    case PlaybackDirection::Last:
        while (m_playback) {
            playback_move(PlaybackDirection::Forward);
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    update();
}

ErrorOr<String> ChessWidget::get_fen() const
{
    return TRY(m_playback ? m_board_playback.to_fen() : m_board.to_fen());
}

ErrorOr<void> ChessWidget::import_pgn(Core::File& file)
{
    m_board = Chess::Board();

    ByteBuffer bytes = TRY(file.read_until_eof());
    StringView content = bytes;
    auto lines = content.lines();
    StringView line;
    size_t i = 0;

    // Tag Pair Section
    // FIXME: Parse these tags when they become relevant
    do {
        line = lines.at(i++);
    } while (!line.is_empty() || i >= lines.size());

    // Movetext Section
    bool skip = false;
    bool recursive_annotation = false;
    bool future_expansion = false;
    Chess::Color turn = Chess::Color::White;
    String movetext;

    for (size_t j = i; j < lines.size(); j++)
        movetext = TRY(String::formatted("{}{}", movetext, lines.at(i)));

    for (auto token : TRY(movetext.split(' '))) {
        token = TRY(token.trim_ascii_whitespace());

        // FIXME: Parse all of these tokens when we start caring about them
        if (token.ends_with('}')) {
            skip = false;
            continue;
        }
        if (skip)
            continue;
        if (token.starts_with('{')) {
            if (token.ends_with('}'))
                continue;
            skip = true;
            continue;
        }
        if (token.ends_with(')')) {
            recursive_annotation = false;
            continue;
        }
        if (recursive_annotation)
            continue;
        if (token.starts_with('(')) {
            if (token.ends_with(')'))
                continue;
            recursive_annotation = true;
            continue;
        }
        if (token.ends_with('>')) {
            future_expansion = false;
            continue;
        }
        if (future_expansion)
            continue;
        if (token.starts_with('<')) {
            if (token.ends_with('>'))
                continue;
            future_expansion = true;
            continue;
        }
        if (token.starts_with('$'))
            continue;
        if (token.contains('*'))
            break;
        // FIXME: When we become able to set more of the game state, fix these end results
        if (token.contains("1-0"sv)) {
            m_board.set_resigned(Chess::Color::Black);
            break;
        }
        if (token.contains("0-1"sv)) {
            m_board.set_resigned(Chess::Color::White);
            break;
        }
        if (token.contains("1/2-1/2"sv)) {
            break;
        }
        if (!token.ends_with('.')) {
            m_board.apply_move(Chess::Move::from_algebraic(token, turn, m_board));
            turn = Chess::opposing_color(turn);
        }
    }

    m_board_markings.clear();
    m_board_playback = m_board;
    m_playback_move_number = m_board_playback.moves().size();
    m_playback = true;
    update();

    return {};
}

ErrorOr<void> ChessWidget::export_pgn(Core::File& file) const
{
    // Tag Pair Section
    TRY(file.write_until_depleted("[Event \"Casual Game\"]\n"sv.bytes()));
    TRY(file.write_until_depleted("[Site \"SerenityOS Chess\"]\n"sv.bytes()));
    TRY(file.write_formatted("[Date \"{}\"]\n", Core::DateTime::now().to_deprecated_string("%Y.%m.%d"sv)));
    TRY(file.write_until_depleted("[Round \"1\"]\n"sv.bytes()));

    auto current_user = TRY(Core::Account::self(Core::Account::Read::PasswdOnly));
    auto const username = TRY(String::from_deprecated_string(current_user.username()));

    auto const player1 = (!username.is_empty() ? username : "?"sv);
    auto const player2 = (!m_engine.is_null() ? "SerenityOS ChessEngine"sv : "?"sv);

    TRY(file.write_formatted("[White \"{}\"]\n", m_side == Chess::Color::White ? player1 : player2));
    TRY(file.write_formatted("[Black \"{}\"]\n", m_side == Chess::Color::Black ? player1 : player2));

    TRY(file.write_formatted("[Result \"{}\"]\n", Chess::Board::result_to_points_string(m_board.game_result(), m_board.turn())));
    TRY(file.write_until_depleted("[WhiteElo \"?\"]\n"sv.bytes()));
    TRY(file.write_until_depleted("[BlackElo \"?\"]\n"sv.bytes()));
    TRY(file.write_until_depleted("[Variant \"Standard\"]\n"sv.bytes()));
    TRY(file.write_until_depleted("[TimeControl \"-\"]\n"sv.bytes()));
    TRY(file.write_until_depleted("[Annotator \"SerenityOS Chess\"]\n"sv.bytes()));
    TRY(file.write_until_depleted("\n"sv.bytes()));

    // Movetext Section
    for (size_t i = 0, move_no = 1; i < m_board.moves().size(); i += 2, move_no++) {
        auto const white = TRY(m_board.moves().at(i).to_algebraic());

        if (i + 1 < m_board.moves().size()) {
            auto const black = TRY(m_board.moves().at(i + 1).to_algebraic());
            TRY(file.write_until_depleted(TRY(String::formatted("{}. {} {} ", move_no, white, black)).bytes()));
        } else {
            TRY(file.write_until_depleted(TRY(String::formatted("{}. {} ", move_no, white)).bytes()));
        }
    }

    TRY(file.write_until_depleted("{ "sv.bytes()));
    TRY(file.write_until_depleted(Chess::Board::result_to_string(m_board.game_result(), m_board.turn()).bytes()));
    TRY(file.write_until_depleted(" } "sv.bytes()));
    TRY(file.write_until_depleted(Chess::Board::result_to_points_string(m_board.game_result(), m_board.turn()).bytes()));
    TRY(file.write_until_depleted("\n"sv.bytes()));

    return {};
}

void ChessWidget::flip_board()
{
    if (want_engine_move()) {
        GUI::MessageBox::show(window(), "You can only flip the board on your turn."sv, "Flip Board"sv, GUI::MessageBox::Type::Information);
        return;
    }
    m_side = Chess::opposing_color(m_side);
    input_engine_move();
    update();
}

int ChessWidget::resign()
{
    // FIXME: Disable the resign checkbox if the game is finished
    if (board().game_finished())
        return -1;

    if (want_engine_move()) {
        GUI::MessageBox::show(window(), "You can only resign on your turn."sv, "Resign"sv, GUI::MessageBox::Type::Information);
        return -1;
    }

    auto result = GUI::MessageBox::show(window(), "Are you sure you wish to resign?"sv, "Resign"sv, GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNo);
    if (result != GUI::MessageBox::ExecResult::Yes)
        return -1;

    board().set_resigned(m_board.turn());

    set_drag_enabled(false);
    update();
    auto const msg = Chess::Board::result_to_string(m_board.game_result(), m_board.turn());
    GUI::MessageBox::show(window(), msg, "Game Over"sv, GUI::MessageBox::Type::Information);

    return 0;
}

bool ChessWidget::check_game_over(ClaimDrawBehavior claim_draw_behavior)
{
    if (board().game_result() == Chess::Board::Result::NotFinished)
        return false;

    auto over = true;
    switch (board().game_result()) {
    case Chess::Board::Result::FiftyMoveRule:
        if (claim_draw_behavior == ClaimDrawBehavior::Prompt) {
            update();
            auto dialog_result = GUI::MessageBox::show(window(), "50 moves have elapsed without a capture or pawn advance. Claim Draw?"sv, "Claim Draw?"sv,
                GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::YesNo);

            if (dialog_result != GUI::Dialog::ExecResult::Yes)
                over = false;
        }
        break;
    case Chess::Board::Result::ThreeFoldRepetition:
        if (claim_draw_behavior == ClaimDrawBehavior::Prompt) {
            update();
            auto dialog_result = GUI::MessageBox::show(window(), "The same board state has repeated three times. Claim Draw?"sv, "Claim Draw?"sv,
                GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::YesNo);
            if (dialog_result != GUI::Dialog::ExecResult::Yes)
                over = false;
        }
        break;
    default:
        break;
    }

    if (!over)
        return false;

    set_override_cursor(Gfx::StandardCursor::None);
    set_drag_enabled(false);
    update();
    auto msg = Chess::Board::result_to_string(board().game_result(), board().turn());
    GUI::MessageBox::show(window(), msg, "Game Over"sv, GUI::MessageBox::Type::Information);
    return true;
}

void ChessWidget::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
{
    if (domain != "Games"sv && group != "Chess"sv)
        return;

    if (key == "PieceSet"sv) {
        set_piece_set(value);
        update();
    } else if (key == "BoardTheme"sv) {
        set_board_theme(value);
        update();
    }
}

void ChessWidget::config_bool_did_change(StringView domain, StringView group, StringView key, bool value)
{
    if (domain != "Games"sv && group != "Chess"sv)
        return;

    if (key == "ShowCoordinates"sv) {
        set_coordinates(value);
        update();
    } else if (key == "HighlightChecks"sv) {
        set_highlight_checks(value);
        update();
    }
}
