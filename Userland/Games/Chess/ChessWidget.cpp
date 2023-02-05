/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChessWidget.h"
#include "PromotionDialog.h"
#include <AK/DeprecatedString.h>
#include <AK/Random.h>
#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <LibCore/Stream.h>
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
    widget->set_piece_set("stelar7"sv);

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

        if (active_board.last_move().has_value() && (active_board.last_move().value().to == sq || active_board.last_move().value().from == sq)) {
            painter.fill_rect(tile_rect, m_move_highlight_color);
        }

        if (m_coordinates) {
            auto coord = sq.to_algebraic();
            auto text_color = (sq.is_light()) ? board_theme().dark_square_color : board_theme().light_square_color;

            auto shrunken_rect = tile_rect;
            shrunken_rect.shrink(4, 4);
            if (sq.rank == coord_rank_file)
                painter.draw_text(shrunken_rect, coord.substring_view(0, 1), coordinate_font, Gfx::TextAlignment::BottomRight, text_color);

            if (sq.file == coord_rank_file)
                painter.draw_text(shrunken_rect, coord.substring_view(1, 1), coordinate_font, Gfx::TextAlignment::TopLeft, text_color);
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

    if (event.button() == GUI::MouseButton::Secondary) {
        if (m_dragging_piece) {
            m_dragging_piece = false;
            set_override_cursor(Gfx::StandardCursor::None);
            m_available_moves.clear();
        } else {
            m_current_marking.from = mouse_to_square(event);
        }
        return;
    }
    m_board_markings.clear();

    auto square = mouse_to_square(event);
    auto piece = board().get_piece(square);
    if (drag_enabled() && piece.color == board().turn() && !m_playback) {
        m_dragging_piece = true;
        set_override_cursor(Gfx::StandardCursor::Drag);
        m_drag_point = { event.position().x() - widget_offset_x, event.position().y() - widget_offset_y };
        m_moving_square = square;

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

    if (event.button() == GUI::MouseButton::Secondary) {
        m_current_marking.secondary_color = event.shift();
        m_current_marking.alternate_color = event.ctrl();
        m_current_marking.to = mouse_to_square(event);
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

    auto target_square = mouse_to_square(event);

    Chess::Move move = { m_moving_square, target_square };
    if (board().is_promotion_move(move)) {
        auto promotion_dialog = PromotionDialog::construct(*this);
        if (promotion_dialog->exec() == PromotionDialog::ExecResult::OK)
            move.promote_to = promotion_dialog->selected_piece();
    }

    if (board().apply_move(move)) {
        m_playback_move_number = board().moves().size();
        m_playback = false;
        m_board_playback = m_board;

        if (board().game_result() != Chess::Board::Result::NotFinished) {
            bool over = true;
            StringView msg;
            switch (board().game_result()) {
            case Chess::Board::Result::CheckMate:
                if (board().turn() == Chess::Color::White) {
                    msg = "Black wins by Checkmate."sv;
                } else {
                    msg = "White wins by Checkmate."sv;
                }
                break;
            case Chess::Board::Result::StaleMate:
                msg = "Draw by Stalemate."sv;
                break;
            case Chess::Board::Result::FiftyMoveRule:
                update();
                if (GUI::MessageBox::show(window(), "50 moves have elapsed without a capture. Claim Draw?"sv, "Claim Draw?"sv,
                        GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::YesNo)
                    == GUI::Dialog::ExecResult::Yes) {
                    msg = "Draw by 50 move rule."sv;
                } else {
                    over = false;
                }
                break;
            case Chess::Board::Result::SeventyFiveMoveRule:
                msg = "Draw by 75 move rule."sv;
                break;
            case Chess::Board::Result::ThreeFoldRepetition:
                update();
                if (GUI::MessageBox::show(window(), "The same board state has repeated three times. Claim Draw?"sv, "Claim Draw?"sv,
                        GUI::MessageBox::Type::Information, GUI::MessageBox::InputType::YesNo)
                    == GUI::Dialog::ExecResult::Yes) {
                    msg = "Draw by threefold repetition."sv;
                } else {
                    over = false;
                }
                break;
            case Chess::Board::Result::FiveFoldRepetition:
                msg = "Draw by fivefold repetition."sv;
                break;
            case Chess::Board::Result::InsufficientMaterial:
                msg = "Draw by insufficient material."sv;
                break;
            default:
                VERIFY_NOT_REACHED();
            }
            if (over) {
                set_override_cursor(Gfx::StandardCursor::None);
                set_drag_enabled(false);
                update();
                GUI::MessageBox::show(window(), msg, "Game Over"sv, GUI::MessageBox::Type::Information);
            }
        } else {
            input_engine_move();
        }
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
        if (!square.in_bounds())
            return;
        auto piece = board().get_piece(square);
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

static DeprecatedString set_path = DeprecatedString("/res/icons/chess/sets/");

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
    m_piece_set = set;
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

Chess::Square ChessWidget::mouse_to_square(GUI::MouseEvent& event) const
{
    int const min_size = min(width(), height());
    int const widget_offset_x = (window()->width() - min_size) / 2;
    int const widget_offset_y = (window()->height() - min_size) / 2;

    int square_width = min_size / 8;
    int square_height = min_size / 8;

    if (side() == Chess::Color::White) {
        return { 7 - ((event.y() - widget_offset_y) / square_height), (event.x() - widget_offset_x) / square_width };
    } else {
        return { (event.y() - widget_offset_y) / square_height, 7 - ((event.x() - widget_offset_x) / square_width) };
    }
}

RefPtr<Gfx::Bitmap> ChessWidget::get_piece_graphic(Chess::Piece const& piece) const
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
    input_engine_move();
    update();
}

void ChessWidget::set_board_theme(StringView name)
{
    // FIXME: Add some kind of themes.json
    // The following Colors have been taken from lichess.org, but i'm pretty sure they took them from chess.com.
    if (name == "Beige") {
        m_board_theme = { "Beige", Color::from_rgb(0xb58863), Color::from_rgb(0xf0d9b5) };
    } else if (name == "Green") {
        m_board_theme = { "Green", Color::from_rgb(0x86a666), Color::from_rgb(0xffffdd) };
    } else if (name == "Blue") {
        m_board_theme = { "Blue", Color::from_rgb(0x8ca2ad), Color::from_rgb(0xdee3e6) };
    } else {
        set_board_theme("Beige"sv);
    }
}

bool ChessWidget::want_engine_move()
{
    if (!m_engine)
        return false;
    if (board().turn() == side())
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
    m_engine->get_best_move(board(), 4000, [this, drag_was_enabled](Chess::Move move) {
        set_override_cursor(Gfx::StandardCursor::None);
        if (!want_engine_move())
            return;
        set_drag_enabled(drag_was_enabled);
        VERIFY(board().apply_move(move));
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

DeprecatedString ChessWidget::get_fen() const
{
    return m_playback ? m_board_playback.to_fen() : m_board.to_fen();
}

ErrorOr<void> ChessWidget::import_pgn(Core::Stream::File& file)
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
    DeprecatedString movetext;

    for (size_t j = i; j < lines.size(); j++)
        movetext = DeprecatedString::formatted("{}{}", movetext, lines.at(i).to_deprecated_string());

    for (auto token : movetext.split(' ')) {
        token = token.trim_whitespace();

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

ErrorOr<void> ChessWidget::export_pgn(Core::Stream::File& file) const
{
    // Tag Pair Section
    TRY(file.write("[Event \"Casual Game\"]\n"sv.bytes()));
    TRY(file.write("[Site \"SerenityOS Chess\"]\n"sv.bytes()));
    TRY(file.write(DeprecatedString::formatted("[Date \"{}\"]\n", Core::DateTime::now().to_deprecated_string("%Y.%m.%d"sv)).bytes()));
    TRY(file.write("[Round \"1\"]\n"sv.bytes()));

    DeprecatedString username(getlogin());
    auto const player1 = (!username.is_empty() ? username.view() : "?"sv.bytes());
    auto const player2 = (!m_engine.is_null() ? "SerenityOS ChessEngine"sv.bytes() : "?"sv.bytes());
    TRY(file.write(DeprecatedString::formatted("[White \"{}\"]\n", m_side == Chess::Color::White ? player1 : player2).bytes()));
    TRY(file.write(DeprecatedString::formatted("[Black \"{}\"]\n", m_side == Chess::Color::Black ? player1 : player2).bytes()));

    TRY(file.write(DeprecatedString::formatted("[Result \"{}\"]\n", Chess::Board::result_to_points_deprecated_string(m_board.game_result(), m_board.turn())).bytes()));
    TRY(file.write("[WhiteElo \"?\"]\n"sv.bytes()));
    TRY(file.write("[BlackElo \"?\"]\n"sv.bytes()));
    TRY(file.write("[Variant \"Standard\"]\n"sv.bytes()));
    TRY(file.write("[TimeControl \"-\"]\n"sv.bytes()));
    TRY(file.write("[Annotator \"SerenityOS Chess\"]\n"sv.bytes()));
    TRY(file.write("\n"sv.bytes()));

    // Movetext Section
    for (size_t i = 0, move_no = 1; i < m_board.moves().size(); i += 2, move_no++) {
        const DeprecatedString white = m_board.moves().at(i).to_algebraic();

        if (i + 1 < m_board.moves().size()) {
            const DeprecatedString black = m_board.moves().at(i + 1).to_algebraic();
            TRY(file.write(DeprecatedString::formatted("{}. {} {} ", move_no, white, black).bytes()));
        } else {
            TRY(file.write(DeprecatedString::formatted("{}. {} ", move_no, white).bytes()));
        }
    }

    TRY(file.write("{ "sv.bytes()));
    TRY(file.write(Chess::Board::result_to_deprecated_string(m_board.game_result(), m_board.turn()).bytes()));
    TRY(file.write(" } "sv.bytes()));
    TRY(file.write(Chess::Board::result_to_points_deprecated_string(m_board.game_result(), m_board.turn()).bytes()));
    TRY(file.write("\n"sv.bytes()));

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
    const DeprecatedString msg = Chess::Board::result_to_deprecated_string(m_board.game_result(), m_board.turn());
    GUI::MessageBox::show(window(), msg, "Game Over"sv, GUI::MessageBox::Type::Information);

    return 0;
}

void ChessWidget::config_string_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& value)
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

void ChessWidget::config_bool_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, bool value)
{
    if (domain != "Games"sv && group != "Chess"sv)
        return;

    if (key == "ShowCoordinates"sv) {
        set_coordinates(value);
        update();
    }
}
