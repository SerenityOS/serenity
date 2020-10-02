/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/LogStream.h>
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

Chess::Type piece_for_char_promotion(const StringView& str)
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

Colour opposing_colour(Colour colour)
{
    return (colour == Colour::White) ? Colour::Black : Colour::White;
}

Square::Square(const StringView& name)
{
    ASSERT(name.length() == 2);
    char filec = name[0];
    char rankc = name[1];

    if (filec >= 'a' && filec <= 'h') {
        file = filec - 'a';
    } else if (filec >= 'A' && filec <= 'H') {
        file = filec - 'A';
    } else {
        ASSERT_NOT_REACHED();
    }

    if (rankc >= '1' && rankc <= '8') {
        rank = rankc - '1';
    } else {
        ASSERT_NOT_REACHED();
    }
}

String Square::to_algebraic() const
{
    StringBuilder builder;
    builder.append(file + 'a');
    builder.append(rank + '1');
    return builder.build();
}

Move::Move(const StringView& algebraic)
    : from(algebraic.substring_view(0, 2))
    , to(algebraic.substring_view(2, 2))
    , promote_to(piece_for_char_promotion((algebraic.length() >= 5) ? algebraic.substring_view(4, 1) : ""))
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

Board::Board()
{
    // Fill empty spaces.
    for (unsigned rank = 2; rank < 6; ++rank) {
        for (unsigned file = 0; file < 8; ++file) {
            set_piece({ rank, file }, EmptyPiece);
        }
    }

    // Fill white pawns.
    for (unsigned file = 0; file < 8; ++file) {
        set_piece({ 1, file }, { Colour::White, Type::Pawn });
    }

    // Fill black pawns.
    for (unsigned file = 0; file < 8; ++file) {
        set_piece({ 6, file }, { Colour::Black, Type::Pawn });
    }

    // Fill while pieces.
    set_piece(Square("a1"), { Colour::White, Type::Rook });
    set_piece(Square("b1"), { Colour::White, Type::Knight });
    set_piece(Square("c1"), { Colour::White, Type::Bishop });
    set_piece(Square("d1"), { Colour::White, Type::Queen });
    set_piece(Square("e1"), { Colour::White, Type::King });
    set_piece(Square("f1"), { Colour::White, Type::Bishop });
    set_piece(Square("g1"), { Colour::White, Type::Knight });
    set_piece(Square("h1"), { Colour::White, Type::Rook });

    // Fill black pieces.
    set_piece(Square("a8"), { Colour::Black, Type::Rook });
    set_piece(Square("b8"), { Colour::Black, Type::Knight });
    set_piece(Square("c8"), { Colour::Black, Type::Bishop });
    set_piece(Square("d8"), { Colour::Black, Type::Queen });
    set_piece(Square("e8"), { Colour::Black, Type::King });
    set_piece(Square("f8"), { Colour::Black, Type::Bishop });
    set_piece(Square("g8"), { Colour::Black, Type::Knight });
    set_piece(Square("h8"), { Colour::Black, Type::Rook });
}

Piece Board::get_piece(const Square& square) const
{
    ASSERT(square.rank < 8);
    ASSERT(square.file < 8);
    return m_board[square.rank][square.file];
}

Piece Board::set_piece(const Square& square, const Piece& piece)
{
    ASSERT(square.rank < 8);
    ASSERT(square.file < 8);
    return m_board[square.rank][square.file] = piece;
}

bool Board::is_legal(const Move& move, Colour colour) const
{
    if (colour == Colour::None)
        colour = turn();

    if (!is_legal_no_check(move, colour))
        return false;

    Board clone = *this;
    clone.apply_illegal_move(move, colour);
    if (clone.in_check(colour))
        return false;

    // Don't allow castling through check or out of check.
    Vector<Square> check_squares;
    if (colour == Colour::White && move.from == Square("e1") && get_piece(Square("e1")) == Piece(Colour::White, Type::King)) {
        if (move.to == Square("a1") || move.to == Square("c1")) {
            check_squares = { Square("e1"), Square("d1"), Square("c1") };
        } else if (move.to == Square("h1") || move.to == Square("g1")) {
            check_squares = { Square("e1"), Square("f1"), Square("g1") };
        }
    } else if (colour == Colour::Black && move.from == Square("e8") && get_piece(Square("e8")) == Piece(Colour::Black, Type::King)) {
        if (move.to == Square("a8") || move.to == Square("c8")) {
            check_squares = { Square("e8"), Square("d8"), Square("c8") };
        } else if (move.to == Square("h8") || move.to == Square("g8")) {
            check_squares = { Square("e8"), Square("f8"), Square("g8") };
        }
    }
    for (auto& square : check_squares) {
        Board clone = *this;
        clone.set_piece(move.from, EmptyPiece);
        clone.set_piece(square, { colour, Type::King });
        if (clone.in_check(colour))
            return false;
    }

    return true;
}

bool Board::is_legal_no_check(const Move& move, Colour colour) const
{
    auto piece = get_piece(move.from);
    if (piece.colour != colour)
        return false;

    if (move.to.rank > 7 || move.to.file > 7)
        return false;

    if (piece.type != Type::Pawn && move.promote_to != Type::None)
        return false;

    if (move.promote_to == Type::Pawn || move.promote_to == Type::King)
        return false;

    if (piece.type == Type::Pawn) {
        int dir = (colour == Colour::White) ? +1 : -1;
        unsigned start_rank = (colour == Colour::White) ? 1 : 6;
        unsigned other_start_rank = (colour == Colour::White) ? 6 : 1;
        unsigned en_passant_rank = (colour == Colour::White) ? 4 : 3;
        unsigned promotion_rank = (colour == Colour::White) ? 7 : 0;

        if (move.to.rank == promotion_rank) {
            if (move.promote_to == Type::Pawn || move.promote_to == Type::King || move.promote_to == Type::None)
                return false;
        } else if (move.promote_to != Type::None) {
            return false;
        }

        if (move.to.rank == move.from.rank + dir && move.to.file == move.from.file && get_piece(move.to).type == Type::None) {
            // Regular pawn move.
            return true;
        } else if (move.to.rank == move.from.rank + dir && (move.to.file == move.from.file + 1 || move.to.file == move.from.file - 1)) {
            Move en_passant_last_move = { { other_start_rank, move.to.file }, { en_passant_rank, move.to.file } };
            if (get_piece(move.to).colour == opposing_colour(colour)) {
                // Pawn capture.
                return true;
            } else if (m_last_move.has_value() && move.from.rank == en_passant_rank && m_last_move.value() == en_passant_last_move
                && get_piece(en_passant_last_move.to) == Piece(opposing_colour(colour), Type::Pawn)) {
                // En passant.
                return true;
            }
        } else if (move.from.rank == start_rank && move.to.rank == move.from.rank + (2 * dir) && move.to.file == move.from.file
            && get_piece(move.to).type == Type::None && get_piece({ move.from.rank + dir, move.from.file }).type == Type::None) {
            // 2 square pawn move from initial position.
            return true;
        }

        return false;
    } else if (piece.type == Type::Knight) {
        int rank_delta = abs(move.to.rank - move.from.rank);
        int file_delta = abs(move.to.file - move.from.file);
        if (get_piece(move.to).colour != colour && max(rank_delta, file_delta) == 2 && min(rank_delta, file_delta) == 1) {
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

            if (get_piece(move.to).colour != colour) {
                return true;
            }
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

            if (get_piece(move.to).colour != colour) {
                return true;
            }
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

            if (get_piece(move.to).colour != colour) {
                return true;
            }
        }
    } else if (piece.type == Type::King) {
        int rank_delta = move.to.rank - move.from.rank;
        int file_delta = move.to.file - move.from.file;
        if (abs(rank_delta) <= 1 && abs(file_delta) <= 1) {
            if (get_piece(move.to).colour != colour) {
                return true;
            }
        }

        if (colour == Colour::White) {
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

    return false;
}

bool Board::in_check(Colour colour) const
{
    Square king_square = { 50, 50 };
    Square::for_each([&](const Square& square) {
        if (get_piece(square) == Piece(colour, Type::King)) {
            king_square = square;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    bool check = false;
    Square::for_each([&](const Square& square) {
        if (is_legal_no_check({ square, king_square }, opposing_colour(colour))) {
            check = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    return check;
}

bool Board::apply_move(const Move& move, Colour colour)
{
    if (colour == Colour::None)
        colour = turn();

    if (!is_legal(move, colour))
        return false;

    return apply_illegal_move(move, colour);
}

bool Board::apply_illegal_move(const Move& move, Colour colour)
{
    Board clone = *this;
    clone.m_previous_states = {};
    clone.m_moves = {};
    auto state_count = 0;
    if (m_previous_states.contains(clone))
        state_count = m_previous_states.get(clone).value();

    m_previous_states.set(clone, state_count + 1);
    m_moves.append(move);

    m_turn = opposing_colour(colour);

    m_last_move = move;
    m_moves_since_capture++;

    if (move.from == Square("a1") || move.to == Square("a1") || move.from == Square("e1"))
        m_white_can_castle_queenside = false;
    if (move.from == Square("h1") || move.to == Square("h1") || move.from == Square("e1"))
        m_white_can_castle_kingside = false;
    if (move.from == Square("a8") || move.to == Square("a8") || move.from == Square("e8"))
        m_black_can_castle_queenside = false;
    if (move.from == Square("h8") || move.to == Square("h8") || move.from == Square("e8"))
        m_black_can_castle_kingside = false;

    if (colour == Colour::White && move.from == Square("e1") && get_piece(Square("e1")) == Piece(Colour::White, Type::King)) {
        if (move.to == Square("a1") || move.to == Square("c1")) {
            set_piece(Square("e1"), EmptyPiece);
            set_piece(Square("a1"), EmptyPiece);
            set_piece(Square("c1"), { Colour::White, Type::King });
            set_piece(Square("d1"), { Colour::White, Type::Rook });
            return true;
        } else if (move.to == Square("h1") || move.to == Square("g1")) {
            set_piece(Square("e1"), EmptyPiece);
            set_piece(Square("h1"), EmptyPiece);
            set_piece(Square("g1"), { Colour::White, Type::King });
            set_piece(Square("f1"), { Colour::White, Type::Rook });
            return true;
        }
    } else if (colour == Colour::Black && move.from == Square("e8") && get_piece(Square("e8")) == Piece(Colour::Black, Type::King)) {
        if (move.to == Square("a8") || move.to == Square("c8")) {
            set_piece(Square("e8"), EmptyPiece);
            set_piece(Square("a8"), EmptyPiece);
            set_piece(Square("c8"), { Colour::Black, Type::King });
            set_piece(Square("d8"), { Colour::Black, Type::Rook });
            return true;
        } else if (move.to == Square("h8") || move.to == Square("g8")) {
            set_piece(Square("e8"), EmptyPiece);
            set_piece(Square("h8"), EmptyPiece);
            set_piece(Square("g8"), { Colour::Black, Type::King });
            set_piece(Square("f8"), { Colour::Black, Type::Rook });
            return true;
        }
    }

    if (get_piece(move.from).type == Type::Pawn && ((colour == Colour::Black && move.to.rank == 0) || (colour == Colour::White && move.to.rank == 7))) {
        // Pawn Promotion
        set_piece(move.to, { colour, move.promote_to });
        set_piece(move.from, EmptyPiece);
        return true;
    }

    if (get_piece(move.from).type == Type::Pawn && move.from.file != move.to.file && get_piece(move.to).type == Type::None) {
        // En passant.
        if (colour == Colour::White) {
            set_piece({ move.to.rank - 1, move.to.file }, EmptyPiece);
        } else {
            set_piece({ move.to.rank + 1, move.to.file }, EmptyPiece);
        }
        m_moves_since_capture = 0;
    }

    if (get_piece(move.to).colour != Colour::None)
        m_moves_since_capture = 0;

    set_piece(move.to, get_piece(move.from));
    set_piece(move.from, EmptyPiece);

    return true;
}

Move Board::random_move(Colour colour) const
{
    if (colour == Colour::None)
        colour = turn();

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
                if (get_piece(sq).colour == get_piece(bishop.value()).colour) {
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
    generate_moves([&](Move m) {
        (void)m;
        are_legal_moves = true;
        return IterationDecision::Break;
    });

    if (are_legal_moves) {
        if (m_moves_since_capture >= 75 * 2)
            return Result::SeventyFiveMoveRule;
        if (m_moves_since_capture == 50 * 2)
            return Result::FiftyMoveRule;

        auto repeats = m_previous_states.get(*this);
        if (repeats.has_value()) {
            if (repeats.value() == 3)
                return Result::ThreeFoldRepetition;
            if (repeats.value() >= 5)
                return Result::FiveFoldRepetition;
        }

        return Result::NotFinished;
    }

    if (in_check(turn()))
        return Result::CheckMate;

    return Result::StaleMate;
}

Colour Board::game_winner() const
{
    if (game_result() == Result::CheckMate)
        return opposing_colour(turn());

    return Colour::None;
}

int Board::game_score() const
{
    switch (game_winner()) {
    case Colour::White:
        return +1;
    case Colour::Black:
        return -1;
    case Colour::None:
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

        if (get_piece(square).colour == Colour::White) {
            imbalance += value;
        } else {
            imbalance -= value;
        }
        return IterationDecision::Continue;
    });
    return imbalance;
}

bool Board::is_promotion_move(const Move& move, Colour colour) const
{
    if (colour == Colour::None)
        colour = turn();

    Move queen_move = move;
    queen_move.promote_to = Type::Queen;
    if (!is_legal(queen_move, colour))
        return false;

    if (get_piece(move.from).type == Type::Pawn && ((colour == Colour::Black && move.to.rank == 0) || (colour == Colour::White && move.to.rank == 7)))
        return true;

    return false;
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

}
