/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibChess/Chess.h>
#include <stdlib.h>

namespace Chess {

String char_for_piece(Chess::Type type)
{
    switch (type) {
    case Type::Knight:
        return "N";
    case Type::Bishop:
        return "B";
    case Type::Rook:
        return "R";
    case Type::Queen:
        return "Q";
    case Type::King:
        return "K";
    case Type::Pawn:
    default:
        return "";
    }
}

Chess::Type piece_for_char_promotion(StringView str)
{
    String string = String(str).to_lowercase();
    if (string == "")
        return Type::None;
    if (string == "n")
        return Type::Knight;
    if (string == "b")
        return Type::Bishop;
    if (string == "r")
        return Type::Rook;
    if (string == "q")
        return Type::Queen;
    if (string == "k")
        return Type::King;

    return Type::None;
}

Color opposing_color(Color color)
{
    return (color == Color::White) ? Color::Black : Color::White;
}

Square::Square(StringView name)
{
    VERIFY(name.length() == 2);
    char filec = name[0];
    char rankc = name[1];

    if (filec >= 'a' && filec <= 'h') {
        file = filec - 'a';
    } else if (filec >= 'A' && filec <= 'H') {
        file = filec - 'A';
    } else {
        VERIFY_NOT_REACHED();
    }

    if (rankc >= '1' && rankc <= '8') {
        rank = rankc - '1';
    } else {
        VERIFY_NOT_REACHED();
    }
}

String Square::to_algebraic() const
{
    StringBuilder builder;
    builder.append(file + 'a');
    builder.append(rank + '1');
    return builder.build();
}

Move::Move(StringView long_algebraic)
    : from(long_algebraic.substring_view(0, 2))
    , to(long_algebraic.substring_view(2, 2))
    , promote_to(piece_for_char_promotion((long_algebraic.length() >= 5) ? long_algebraic.substring_view(4, 1) : ""))
{
}

String Move::to_long_algebraic() const
{
    StringBuilder builder;
    builder.append(from.to_algebraic());
    builder.append(to.to_algebraic());
    builder.append(char_for_piece(promote_to).to_lowercase());
    return builder.build();
}

Move Move::from_algebraic(StringView algebraic, const Color turn, const Board& board)
{
    String move_string = algebraic;
    Move move({ 50, 50 }, { 50, 50 });

    if (move_string.contains("-")) {
        move.from = Square(turn == Color::White ? 0 : 7, 4);
        move.to = Square(turn == Color::White ? 0 : 7, move_string == "O-O" ? 6 : 2);
        move.promote_to = Type::None;
        move.piece = { turn, Type::King };

        return move;
    }

    if (algebraic.contains("#")) {
        move.is_mate = true;
        move_string = move_string.substring(0, move_string.length() - 1);
    } else if (algebraic.contains("+")) {
        move.is_check = true;
        move_string = move_string.substring(0, move_string.length() - 1);
    }

    if (algebraic.contains("=")) {
        move.promote_to = piece_for_char_promotion(move_string.split('=').at(1).substring(0, 1));
        move_string = move_string.split('=').at(0);
    }

    move.to = Square(move_string.substring(move_string.length() - 2, 2));
    move_string = move_string.substring(0, move_string.length() - 2);

    if (move_string.contains("x")) {
        move.is_capture = true;
        move_string = move_string.substring(0, move_string.length() - 1);
    }

    if (move_string.is_empty() || move_string.characters()[0] >= 'a') {
        move.piece = Piece(turn, Type::Pawn);
    } else {
        move.piece = Piece(turn, piece_for_char_promotion(move_string.substring(0, 1)));
        move_string = move_string.substring(1, move_string.length() - 1);
    }

    Square::for_each([&](const Square& square) {
        if (!move_string.is_empty()) {
            if (board.get_piece(square).type == move.piece.type && board.is_legal(Move(square, move.to), turn)) {
                if (move_string.length() >= 2) {
                    if (square == Square(move_string.substring(0, 2))) {
                        move.from = square;
                        return IterationDecision::Break;
                    }
                } else if (move_string.characters()[0] <= 57) {
                    if (square.rank == (move_string.characters()[0] - '0')) {
                        move.from = square;
                        return IterationDecision::Break;
                    }
                } else {
                    if (square.file == (move_string.characters()[0] - 'a')) {
                        move.from = square;
                        return IterationDecision::Break;
                    }
                }
            }
            return IterationDecision::Continue;
        } else {
            if (board.get_piece(square).type == move.piece.type && board.is_legal(Move(square, move.to), turn)) {
                move.from = square;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        }
    });

    return move;
}

String Move::to_algebraic() const
{
    if (piece.type == Type::King && from.file == 4) {
        if (to.file == 2)
            return "O-O-O";
        if (to.file == 6)
            return "O-O";
    }

    StringBuilder builder;

    builder.append(char_for_piece(piece.type));

    if (is_ambiguous) {
        if (from.file != ambiguous.file)
            builder.append(from.to_algebraic().substring(0, 1));
        else if (from.rank != ambiguous.rank)
            builder.append(from.to_algebraic().substring(1, 1));
        else
            builder.append(from.to_algebraic());
    }

    if (is_capture) {
        if (piece.type == Type::Pawn && !is_ambiguous)
            builder.append(from.to_algebraic().substring(0, 1));
        builder.append("x");
    }

    builder.append(to.to_algebraic());

    if (promote_to != Type::None) {
        builder.append("=");
        builder.append(char_for_piece(promote_to));
    }

    if (is_mate)
        builder.append("#");
    else if (is_check)
        builder.append("+");

    return builder.build();
}

Board::Board()
{
    // Fill empty spaces.
    for (int rank = 2; rank < 6; ++rank) {
        for (int file = 0; file < 8; ++file) {
            set_piece({ rank, file }, EmptyPiece);
        }
    }

    // Fill white pawns.
    for (int file = 0; file < 8; ++file) {
        set_piece({ 1, file }, { Color::White, Type::Pawn });
    }

    // Fill black pawns.
    for (int file = 0; file < 8; ++file) {
        set_piece({ 6, file }, { Color::Black, Type::Pawn });
    }

    // Fill while pieces.
    set_piece(Square("a1"), { Color::White, Type::Rook });
    set_piece(Square("b1"), { Color::White, Type::Knight });
    set_piece(Square("c1"), { Color::White, Type::Bishop });
    set_piece(Square("d1"), { Color::White, Type::Queen });
    set_piece(Square("e1"), { Color::White, Type::King });
    set_piece(Square("f1"), { Color::White, Type::Bishop });
    set_piece(Square("g1"), { Color::White, Type::Knight });
    set_piece(Square("h1"), { Color::White, Type::Rook });

    // Fill black pieces.
    set_piece(Square("a8"), { Color::Black, Type::Rook });
    set_piece(Square("b8"), { Color::Black, Type::Knight });
    set_piece(Square("c8"), { Color::Black, Type::Bishop });
    set_piece(Square("d8"), { Color::Black, Type::Queen });
    set_piece(Square("e8"), { Color::Black, Type::King });
    set_piece(Square("f8"), { Color::Black, Type::Bishop });
    set_piece(Square("g8"), { Color::Black, Type::Knight });
    set_piece(Square("h8"), { Color::Black, Type::Rook });
}

String Board::to_fen() const
{
    StringBuilder builder;

    // 1. Piece placement
    int empty = 0;
    for (int rank = 0; rank < 8; rank++) {
        for (int file = 0; file < 8; file++) {
            const Piece p(get_piece({ 7 - rank, file }));
            if (p.type == Type::None) {
                empty++;
                continue;
            }
            if (empty > 0) {
                builder.append(String::number(empty));
                empty = 0;
            }
            String piece = char_for_piece(p.type);
            if (piece == "")
                piece = "P";

            builder.append(p.color == Color::Black ? piece.to_lowercase() : piece);
        }
        if (empty > 0) {
            builder.append(String::number(empty));
            empty = 0;
        }
        if (rank < 7)
            builder.append("/");
    }

    // 2. Active color
    VERIFY(m_turn != Color::None);
    builder.append(m_turn == Color::White ? " w " : " b ");

    // 3. Castling availability
    builder.append(m_white_can_castle_kingside ? "K" : "");
    builder.append(m_white_can_castle_queenside ? "Q" : "");
    builder.append(m_black_can_castle_kingside ? "k" : "");
    builder.append(m_black_can_castle_queenside ? "q" : "");
    builder.append(" ");

    // 4. En passant target square
    if (!m_last_move.has_value())
        builder.append("-");
    else if (m_last_move.value().piece.type == Type::Pawn) {
        if (m_last_move.value().from.rank == 1 && m_last_move.value().to.rank == 3)
            builder.append(Square(m_last_move.value().to.rank - 1, m_last_move.value().to.file).to_algebraic());
        else if (m_last_move.value().from.rank == 6 && m_last_move.value().to.rank == 4)
            builder.append(Square(m_last_move.value().to.rank + 1, m_last_move.value().to.file).to_algebraic());
        else
            builder.append("-");
    } else {
        builder.append("-");
    }
    builder.append(" ");

    // 5. Halfmove clock
    builder.append(String::number(min(m_moves_since_capture, m_moves_since_pawn_advance)));
    builder.append(" ");

    // 6. Fullmove number
    builder.append(String::number(1 + m_moves.size() / 2));

    return builder.to_string();
}

Piece Board::get_piece(const Square& square) const
{
    VERIFY(square.in_bounds());
    return m_board[square.rank][square.file];
}

Piece Board::set_piece(const Square& square, const Piece& piece)
{
    VERIFY(square.in_bounds());
    return m_board[square.rank][square.file] = piece;
}

bool Board::is_legal_promotion(const Move& move, Color color) const
{
    auto piece = get_piece(move.from);

    if (move.promote_to == Type::Pawn || move.promote_to == Type::King) {
        // attempted promotion to invalid piece
        return false;
    }

    if (piece.type != Type::Pawn && move.promote_to != Type::None) {
        // attempted promotion from invalid piece
        return false;
    }

    int promotion_rank = (color == Color::White) ? 7 : 0;

    if (move.to.rank != promotion_rank && move.promote_to != Type::None) {
        // attempted promotion from invalid rank
        return false;
    }

    if (piece.type == Type::Pawn && move.to.rank == promotion_rank && move.promote_to == Type::None) {
        // attempted move to promotion rank without promoting
        return false;
    }

    return true;
}

bool Board::is_legal(const Move& move, Color color) const
{
    if (color == Color::None)
        color = turn();

    if (!is_legal_no_check(move, color))
        return false;

    if (!is_legal_promotion(move, color))
        return false;

    Board clone = *this;
    clone.apply_illegal_move(move, color);
    if (clone.in_check(color))
        return false;

    // Don't allow castling through check or out of check.
    Vector<Square> check_squares;
    if (color == Color::White && move.from == Square("e1") && get_piece(Square("e1")) == Piece(Color::White, Type::King)) {
        if (move.to == Square("a1") || move.to == Square("c1")) {
            check_squares = { Square("e1"), Square("d1"), Square("c1") };
        } else if (move.to == Square("h1") || move.to == Square("g1")) {
            check_squares = { Square("e1"), Square("f1"), Square("g1") };
        }
    } else if (color == Color::Black && move.from == Square("e8") && get_piece(Square("e8")) == Piece(Color::Black, Type::King)) {
        if (move.to == Square("a8") || move.to == Square("c8")) {
            check_squares = { Square("e8"), Square("d8"), Square("c8") };
        } else if (move.to == Square("h8") || move.to == Square("g8")) {
            check_squares = { Square("e8"), Square("f8"), Square("g8") };
        }
    }
    for (auto& square : check_squares) {
        Board clone = *this;
        clone.set_piece(move.from, EmptyPiece);
        clone.set_piece(square, { color, Type::King });
        if (clone.in_check(color))
            return false;
    }

    return true;
}

bool Board::is_legal_no_check(const Move& move, Color color) const
{
    auto piece = get_piece(move.from);

    if (piece.color != color)
        // attempted move of opponent's piece
        return false;

    if (!move.to.in_bounds())
        // attempted move outside of board
        return false;

    // Check castling first to allow dragging king onto the rook.
    if (piece.type == Type::King) {
        if (color == Color::White) {
            if ((move.to == Square("a1") || move.to == Square("c1")) && m_white_can_castle_queenside && get_piece(Square("b1")).type == Type::None && get_piece(Square("c1")).type == Type::None && get_piece(Square("d1")).type == Type::None) {
                return true;
            } else if ((move.to == Square("h1") || move.to == Square("g1")) && m_white_can_castle_kingside && get_piece(Square("f1")).type == Type::None && get_piece(Square("g1")).type == Type::None) {
                return true;
            }
        } else {
            if ((move.to == Square("a8") || move.to == Square("c8")) && m_black_can_castle_queenside && get_piece(Square("b8")).type == Type::None && get_piece(Square("c8")).type == Type::None && get_piece(Square("d8")).type == Type::None) {
                return true;
            } else if ((move.to == Square("h8") || move.to == Square("g8")) && m_black_can_castle_kingside && get_piece(Square("f8")).type == Type::None && get_piece(Square("g8")).type == Type::None) {
                return true;
            }
        }
    }

    if (piece.color == get_piece(move.to).color)
        // Attempted move to a square occupied by a piece of the same color.
        return false;

    if (piece.type == Type::Pawn) {
        int dir = (color == Color::White) ? +1 : -1;
        int start_rank = (color == Color::White) ? 1 : 6;

        if (move.from.rank == start_rank && move.to.rank == move.from.rank + (2 * dir) && move.to.file == move.from.file
            && get_piece(move.to).type == Type::None && get_piece({ move.from.rank + dir, move.from.file }).type == Type::None) {
            // 2 square pawn move from initial position.
            return true;
        }

        if (move.to.rank != move.from.rank + dir)
            // attempted backwards or sideways move
            return false;

        if (move.to.file == move.from.file && get_piece(move.to).type == Type::None) {
            // Regular pawn move.
            return true;
        }

        if (move.to.file == move.from.file + 1 || move.to.file == move.from.file - 1) {
            int other_start_rank = (color == Color::White) ? 6 : 1;
            int en_passant_rank = (color == Color::White) ? 4 : 3;
            Move en_passant_last_move = { { other_start_rank, move.to.file }, { en_passant_rank, move.to.file } };
            if (get_piece(move.to).color == opposing_color(color)) {
                // Pawn capture.
                return true;
            }
            if (m_last_move.has_value() && move.from.rank == en_passant_rank && m_last_move.value() == en_passant_last_move
                && get_piece(en_passant_last_move.to) == Piece(opposing_color(color), Type::Pawn)) {
                // En passant.
                return true;
            }
        }

        return false;
    } else if (piece.type == Type::Knight) {
        int rank_delta = abs(move.to.rank - move.from.rank);
        int file_delta = abs(move.to.file - move.from.file);
        if (max(rank_delta, file_delta) == 2 && min(rank_delta, file_delta) == 1) {
            return true;
        }
    } else if (piece.type == Type::Bishop) {
        int rank_delta = move.to.rank - move.from.rank;
        int file_delta = move.to.file - move.from.file;
        if (abs(rank_delta) == abs(file_delta)) {
            int dr = rank_delta / abs(rank_delta);
            int df = file_delta / abs(file_delta);
            for (Square sq = move.from; sq != move.to; sq.rank += dr, sq.file += df) {
                if (get_piece(sq).type != Type::None && sq != move.from) {
                    return false;
                }
            }
            return true;
        }
    } else if (piece.type == Type::Rook) {
        int rank_delta = move.to.rank - move.from.rank;
        int file_delta = move.to.file - move.from.file;
        if (rank_delta == 0 || file_delta == 0) {
            int dr = (rank_delta) ? rank_delta / abs(rank_delta) : 0;
            int df = (file_delta) ? file_delta / abs(file_delta) : 0;
            for (Square sq = move.from; sq != move.to; sq.rank += dr, sq.file += df) {
                if (get_piece(sq).type != Type::None && sq != move.from) {
                    return false;
                }
            }
            return true;
        }
    } else if (piece.type == Type::Queen) {
        int rank_delta = move.to.rank - move.from.rank;
        int file_delta = move.to.file - move.from.file;
        if (abs(rank_delta) == abs(file_delta) || rank_delta == 0 || file_delta == 0) {
            int dr = (rank_delta) ? rank_delta / abs(rank_delta) : 0;
            int df = (file_delta) ? file_delta / abs(file_delta) : 0;
            for (Square sq = move.from; sq != move.to; sq.rank += dr, sq.file += df) {
                if (get_piece(sq).type != Type::None && sq != move.from) {
                    return false;
                }
            }
            return true;
        }
    } else if (piece.type == Type::King) {
        int rank_delta = move.to.rank - move.from.rank;
        int file_delta = move.to.file - move.from.file;
        if (abs(rank_delta) <= 1 && abs(file_delta) <= 1) {
            return true;
        }
    }

    return false;
}

bool Board::in_check(Color color) const
{
    Square king_square = { 50, 50 };
    Square::for_each([&](const Square& square) {
        if (get_piece(square) == Piece(color, Type::King)) {
            king_square = square;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    bool check = false;
    Square::for_each([&](const Square& square) {
        if (is_legal_no_check({ square, king_square }, opposing_color(color))) {
            check = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    return check;
}

bool Board::apply_move(const Move& move, Color color)
{
    if (color == Color::None)
        color = turn();

    if (!is_legal(move, color))
        return false;

    const_cast<Move&>(move).piece = get_piece(move.from);

    return apply_illegal_move(move, color);
}

bool Board::apply_illegal_move(const Move& move, Color color)
{
    auto state = Traits<Board>::hash(*this);
    auto state_count = 0;
    if (m_previous_states.contains(state))
        state_count = m_previous_states.get(state).value();

    m_previous_states.set(state, state_count + 1);
    m_moves.append(move);

    m_turn = opposing_color(color);

    m_last_move = move;
    m_moves_since_capture++;
    m_moves_since_pawn_advance++;

    if (move.from == Square("a1") || move.to == Square("a1") || move.from == Square("e1"))
        m_white_can_castle_queenside = false;
    if (move.from == Square("h1") || move.to == Square("h1") || move.from == Square("e1"))
        m_white_can_castle_kingside = false;
    if (move.from == Square("a8") || move.to == Square("a8") || move.from == Square("e8"))
        m_black_can_castle_queenside = false;
    if (move.from == Square("h8") || move.to == Square("h8") || move.from == Square("e8"))
        m_black_can_castle_kingside = false;

    if (color == Color::White && move.from == Square("e1") && get_piece(Square("e1")) == Piece(Color::White, Type::King)) {
        if (move.to == Square("a1") || move.to == Square("c1")) {
            set_piece(Square("e1"), EmptyPiece);
            set_piece(Square("a1"), EmptyPiece);
            set_piece(Square("c1"), { Color::White, Type::King });
            set_piece(Square("d1"), { Color::White, Type::Rook });
            return true;
        } else if (move.to == Square("h1") || move.to == Square("g1")) {
            set_piece(Square("e1"), EmptyPiece);
            set_piece(Square("h1"), EmptyPiece);
            set_piece(Square("g1"), { Color::White, Type::King });
            set_piece(Square("f1"), { Color::White, Type::Rook });
            return true;
        }
    } else if (color == Color::Black && move.from == Square("e8") && get_piece(Square("e8")) == Piece(Color::Black, Type::King)) {
        if (move.to == Square("a8") || move.to == Square("c8")) {
            set_piece(Square("e8"), EmptyPiece);
            set_piece(Square("a8"), EmptyPiece);
            set_piece(Square("c8"), { Color::Black, Type::King });
            set_piece(Square("d8"), { Color::Black, Type::Rook });
            return true;
        } else if (move.to == Square("h8") || move.to == Square("g8")) {
            set_piece(Square("e8"), EmptyPiece);
            set_piece(Square("h8"), EmptyPiece);
            set_piece(Square("g8"), { Color::Black, Type::King });
            set_piece(Square("f8"), { Color::Black, Type::Rook });
            return true;
        }
    }

    if (move.piece.type == Type::Pawn)
        m_moves_since_pawn_advance = 0;

    if (get_piece(move.to).color != Color::None) {
        const_cast<Move&>(move).is_capture = true;
        m_moves_since_capture = 0;
    }

    if (get_piece(move.from).type == Type::Pawn && ((color == Color::Black && move.to.rank == 0) || (color == Color::White && move.to.rank == 7))) {
        // Pawn Promotion
        set_piece(move.to, { color, move.promote_to });
        set_piece(move.from, EmptyPiece);

        if (in_check(m_turn))
            const_cast<Move&>(move).is_check = true;

        return true;
    }

    if (get_piece(move.from).type == Type::Pawn && move.from.file != move.to.file && get_piece(move.to).type == Type::None) {
        // En passant.
        if (color == Color::White) {
            set_piece({ move.to.rank - 1, move.to.file }, EmptyPiece);
        } else {
            set_piece({ move.to.rank + 1, move.to.file }, EmptyPiece);
        }
        const_cast<Move&>(move).is_capture = true;
        m_moves_since_capture = 0;
    }

    Square::for_each([&](Square sq) {
        // Ambiguous Move
        if (sq != move.from && get_piece(sq).type == move.piece.type && get_piece(sq).color == move.piece.color) {
            if (is_legal(Move(sq, move.to), get_piece(sq).color)) {
                m_moves.last().is_ambiguous = true;
                m_moves.last().ambiguous = sq;

                return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    });

    set_piece(move.to, get_piece(move.from));
    set_piece(move.from, EmptyPiece);

    if (in_check(m_turn))
        const_cast<Move&>(move).is_check = true;

    return true;
}

Move Board::random_move(Color color) const
{
    if (color == Color::None)
        color = turn();

    Move move = { { 50, 50 }, { 50, 50 } };
    int probability = 1;
    generate_moves([&](Move m) {
        if (rand() % probability == 0)
            move = m;
        ++probability;
        return IterationDecision::Continue;
    });

    return move;
}

Board::Result Board::game_result() const
{
    if (m_resigned != Color::None)
        return (m_resigned == Color::White) ? Result::WhiteResign : Result::BlackResign;

    bool sufficient_material = false;
    bool no_more_pieces_allowed = false;
    Optional<Square> bishop;
    Square::for_each([&](Square sq) {
        if (get_piece(sq).type == Type::Queen || get_piece(sq).type == Type::Rook || get_piece(sq).type == Type::Pawn) {
            sufficient_material = true;
            return IterationDecision::Break;
        }

        if (get_piece(sq).type != Type::None && get_piece(sq).type != Type::King && no_more_pieces_allowed) {
            sufficient_material = true;
            return IterationDecision::Break;
        }

        if (get_piece(sq).type == Type::Knight)
            no_more_pieces_allowed = true;

        if (get_piece(sq).type == Type::Bishop) {
            if (bishop.has_value()) {
                if (get_piece(sq).color == get_piece(bishop.value()).color) {
                    sufficient_material = true;
                    return IterationDecision::Break;
                } else if (sq.is_light() != bishop.value().is_light()) {
                    sufficient_material = true;
                    return IterationDecision::Break;
                }
                no_more_pieces_allowed = true;
            } else {
                bishop = sq;
            }
        }

        return IterationDecision::Continue;
    });

    if (!sufficient_material)
        return Result::InsufficientMaterial;

    bool are_legal_moves = false;
    generate_moves([&]([[maybe_unused]] Move m) {
        are_legal_moves = true;
        return IterationDecision::Break;
    });

    if (are_legal_moves) {
        if (m_moves_since_capture >= 75 * 2)
            return Result::SeventyFiveMoveRule;
        if (m_moves_since_capture == 50 * 2)
            return Result::FiftyMoveRule;

        auto repeats = m_previous_states.get(Traits<Board>::hash(*this));
        if (repeats.has_value()) {
            if (repeats.value() == 3)
                return Result::ThreeFoldRepetition;
            if (repeats.value() >= 5)
                return Result::FiveFoldRepetition;
        }

        return Result::NotFinished;
    }

    if (in_check(turn())) {
        const_cast<Vector<Move>&>(m_moves).last().is_mate = true;
        return Result::CheckMate;
    }

    return Result::StaleMate;
}

Color Board::game_winner() const
{
    if (game_result() == Result::CheckMate)
        return opposing_color(turn());

    return Color::None;
}

int Board::game_score() const
{
    switch (game_winner()) {
    case Color::White:
        return +1;
    case Color::Black:
        return -1;
    case Color::None:
        return 0;
    }
    return 0;
}

bool Board::game_finished() const
{
    return game_result() != Result::NotFinished;
}

int Board::material_imbalance() const
{
    int imbalance = 0;
    Square::for_each([&](Square square) {
        int value = 0;
        switch (get_piece(square).type) {
        case Type::Pawn:
            value = 1;
            break;
        case Type::Knight:
        case Type::Bishop:
            value = 3;
            break;
        case Type::Rook:
            value = 5;
            break;
        case Type::Queen:
            value = 9;
            break;
        default:
            break;
        }

        if (get_piece(square).color == Color::White) {
            imbalance += value;
        } else {
            imbalance -= value;
        }
        return IterationDecision::Continue;
    });
    return imbalance;
}

bool Board::is_promotion_move(const Move& move, Color color) const
{
    if (color == Color::None)
        color = turn();

    int promotion_rank = (color == Color::White) ? 7 : 0;
    if (move.to.rank != promotion_rank)
        return false;

    if (get_piece(move.from).type != Type::Pawn)
        return false;

    Move queen_move = move;
    queen_move.promote_to = Type::Queen;
    if (!is_legal(queen_move, color))
        return false;

    return true;
}

bool Board::operator==(const Board& other) const
{
    bool equal_squares = true;
    Square::for_each([&](Square sq) {
        if (get_piece(sq) != other.get_piece(sq)) {
            equal_squares = false;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    if (!equal_squares)
        return false;

    if (m_white_can_castle_queenside != other.m_white_can_castle_queenside)
        return false;
    if (m_white_can_castle_kingside != other.m_white_can_castle_kingside)
        return false;
    if (m_black_can_castle_queenside != other.m_black_can_castle_queenside)
        return false;
    if (m_black_can_castle_kingside != other.m_black_can_castle_kingside)
        return false;

    return turn() == other.turn();
}

void Board::set_resigned(Chess::Color c)
{
    m_resigned = c;
}

String Board::result_to_string(Result result, Color turn)
{
    switch (result) {
    case Result::CheckMate:
        VERIFY(turn != Chess::Color::None);
        return turn == Chess::Color::White ? "Black wins by Checkmate" : "White wins by Checkmate";
    case Result::WhiteResign:
        return "Black wins by Resignation";
    case Result::BlackResign:
        return "White wins by Resignation";
    case Result::StaleMate:
        return "Draw by Stalemate";
    case Chess::Board::Result::FiftyMoveRule:
        return "Draw by 50 move rule";
    case Chess::Board::Result::SeventyFiveMoveRule:
        return "Draw by 75 move rule";
    case Chess::Board::Result::ThreeFoldRepetition:
        return "Draw by threefold repetition";
    case Chess::Board::Result::FiveFoldRepetition:
        return "Draw by fivefold repetition";
    case Chess::Board::Result::InsufficientMaterial:
        return "Draw by insufficient material";
    case Chess::Board::Result::NotFinished:
        return "Game not finished";
    default:
        VERIFY_NOT_REACHED();
    }
}

String Board::result_to_points(Result result, Color turn)
{
    switch (result) {
    case Result::CheckMate:
        VERIFY(turn != Chess::Color::None);
        return turn == Chess::Color::White ? "0-1" : "1-0";
    case Result::WhiteResign:
        return "0-1";
    case Result::BlackResign:
        return "1-0";
    case Result::StaleMate:
        return "1/2-1/2";
    case Chess::Board::Result::FiftyMoveRule:
        return "1/2-1/2";
    case Chess::Board::Result::SeventyFiveMoveRule:
        return "1/2-1/2";
    case Chess::Board::Result::ThreeFoldRepetition:
        return "1/2-1/2";
    case Chess::Board::Result::FiveFoldRepetition:
        return "1/2-1/2";
    case Chess::Board::Result::InsufficientMaterial:
        return "1/2-1/2";
    case Chess::Board::Result::NotFinished:
        return "*";
    default:
        VERIFY_NOT_REACHED();
    }
}

}
