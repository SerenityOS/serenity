/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/IterationDecision.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <AK/Vector.h>

namespace Chess {

enum class Type : u8 {
    Pawn,
    Knight,
    Bishop,
    Rook,
    Queen,
    King,
    None,
};

enum class Notation {
    Algebraic,
    FEN,
};
Optional<char> char_for_piece(Type, Notation);
Type piece_from_char(char);

enum class Color : u8 {
    White,
    Black,
    None,
};

Color opposing_color(Color color);

struct Piece {
    constexpr Piece()
        : color(Color::None)
        , type(Type::None)
    {
    }
    constexpr Piece(Color c, Type t)
        : color(c)
        , type(t)
    {
    }
    Color color : 4;
    Type type : 4;
    bool operator==(Piece const& other) const { return color == other.color && type == other.type; }
};

constexpr Piece EmptyPiece = { Color::None, Type::None };

struct Square {
    i8 rank; // zero indexed;
    i8 file;

    Square(StringView name);

    Square(char const name[3])
        : Square({ name, 2 })
    {
    }

    Square(int const& rank, int const& file)
        : rank(rank)
        , file(file)
    {
    }
    bool operator==(Square const& other) const { return rank == other.rank && file == other.file; }

    template<typename Callback>
    static void for_each(Callback callback)
    {
        for (int rank = 0; rank < 8; ++rank) {
            for (int file = 0; file < 8; ++file) {
                if (callback(Square(rank, file)) == IterationDecision::Break)
                    return;
            }
        }
    }

    bool in_bounds() const { return rank >= 0 && file >= 0 && rank < 8 && file < 8; }
    bool is_light() const { return (rank % 2) != (file % 2); }

    char file_char() const;
    char rank_char() const;
    ErrorOr<String> to_algebraic() const;
};

class Board;

struct Move {
    Square from;
    Square to;
    Type promote_to;
    Piece piece;
    bool is_check : 1 = false;
    bool is_mate : 1 = false;
    bool is_capture : 1 = false;
    bool is_ambiguous : 1 = false;
    Square ambiguous { 50, 50 };
    Move(StringView long_algebraic);
    Move(Square const& from, Square const& to, Type const& promote_to = Type::None)
        : from(from)
        , to(to)
        , promote_to(promote_to)
    {
    }
    bool operator==(Move const& other) const { return from == other.from && to == other.to && promote_to == other.promote_to; }

    static Move from_algebraic(StringView algebraic, Color const turn, Board const& board);
    ErrorOr<String> to_long_algebraic() const;
    ErrorOr<String> to_algebraic() const;
};

class Board {
public:
    Board();
    Board clone_without_history() const;

    Piece get_piece(Square const&) const;
    Piece set_piece(Square const&, Piece const&);

    bool is_legal(Move const&, Color color = Color::None) const;
    bool in_check(Color color) const;

    bool is_promotion_move(Move const&, Color color = Color::None) const;

    bool apply_move(Move const&, Color color = Color::None);
    Optional<Move> const& last_move() const { return m_last_move; }

    ErrorOr<String> to_fen() const;

    enum class Result {
        CheckMate,
        StaleMate,
        WhiteResign,
        BlackResign,
        FiftyMoveRule,
        SeventyFiveMoveRule,
        ThreeFoldRepetition,
        FiveFoldRepetition,
        InsufficientMaterial,
        NotFinished,
    };

    static StringView result_to_string(Result, Color turn);
    static StringView result_to_points_string(Result, Color turn);

    template<typename Callback>
    void generate_moves(Callback callback, Color color = Color::None) const;
    Move random_move(Color color = Color::None) const;
    Result game_result() const;
    Color game_winner() const;
    int game_score() const;
    bool game_finished() const;
    void set_resigned(Color);
    int material_imbalance() const;

    Color turn() const { return m_turn; }
    Vector<Move> const& moves() const { return m_moves; }

    bool operator==(Board const& other) const;

private:
    bool is_legal_no_check(Move const&, Color color) const;
    bool is_legal_promotion(Move const&, Color color) const;
    bool apply_illegal_move(Move const&, Color color);

    Piece m_board[8][8];
    Optional<Move> m_last_move;
    short m_moves_since_capture { 0 };
    short m_moves_since_pawn_advance { 0 };

    Color m_turn : 2 { Color::White };
    Color m_resigned : 2 { Color::None };

    bool m_white_can_castle_kingside : 1 { true };
    bool m_white_can_castle_queenside : 1 { true };
    bool m_black_can_castle_kingside : 1 { true };
    bool m_black_can_castle_queenside : 1 { true };

    // We trust that hash collisions will not happen to save lots of memory and time.
    HashMap<unsigned, int> m_previous_states;
    Vector<Move> m_moves;
    friend struct Traits<Board>;
};

template<typename Callback>
void Board::generate_moves(Callback callback, Color color) const
{
    if (color == Color::None)
        color = turn();

    auto try_move = [&](Move m) {
        if (is_legal(m, color)) {
            if (callback(m) == IterationDecision::Break)
                return false;
        }
        return true;
    };

    Square::for_each([&](Square sq) {
        auto piece = get_piece(sq);
        if (piece.color != color)
            return IterationDecision::Continue;

        bool keep_going = true;
        if (piece.type == Type::Pawn) {
            for (auto& piece : Vector({ Type::None, Type::Knight, Type::Bishop, Type::Rook, Type::Queen })) {
                keep_going = try_move({ sq, { sq.rank + 1, sq.file }, piece })
                    && try_move({ sq, { sq.rank + 2, sq.file }, piece })
                    && try_move({ sq, { sq.rank - 1, sq.file }, piece })
                    && try_move({ sq, { sq.rank - 2, sq.file }, piece })
                    && try_move({ sq, { sq.rank + 1, sq.file + 1 }, piece })
                    && try_move({ sq, { sq.rank + 1, sq.file - 1 }, piece })
                    && try_move({ sq, { sq.rank - 1, sq.file + 1 }, piece })
                    && try_move({ sq, { sq.rank - 1, sq.file - 1 }, piece });
            }
        } else if (piece.type == Type::Knight) {
            keep_going = try_move({ sq, { sq.rank + 2, sq.file + 1 } })
                && try_move({ sq, { sq.rank + 2, sq.file - 1 } })
                && try_move({ sq, { sq.rank + 1, sq.file + 2 } })
                && try_move({ sq, { sq.rank + 1, sq.file - 2 } })
                && try_move({ sq, { sq.rank - 2, sq.file + 1 } })
                && try_move({ sq, { sq.rank - 2, sq.file - 1 } })
                && try_move({ sq, { sq.rank - 1, sq.file + 2 } })
                && try_move({ sq, { sq.rank - 1, sq.file - 2 } });
        } else if (piece.type == Type::Bishop) {
            for (int dr = -1; dr <= 1; dr += 2) {
                for (int df = -1; df <= 1; df += 2) {
                    for (Square to = sq; to.in_bounds(); to = { to.rank + dr, to.file + df }) {
                        if (!try_move({ sq, to }))
                            return IterationDecision::Break;
                    }
                }
            }
        } else if (piece.type == Type::Rook) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int df = -1; df <= 1; df++) {
                    if ((dr == 0) != (df == 0)) {
                        for (Square to = sq; to.in_bounds(); to = { to.rank + dr, to.file + df }) {
                            if (!try_move({ sq, to }))
                                return IterationDecision::Break;
                        }
                    }
                }
            }
        } else if (piece.type == Type::Queen) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int df = -1; df <= 1; df++) {
                    if (dr != 0 || df != 0) {
                        for (Square to = sq; to.in_bounds(); to = { to.rank + dr, to.file + df }) {
                            if (!try_move({ sq, to }))
                                return IterationDecision::Break;
                        }
                    }
                }
            }
        } else if (piece.type == Type::King) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int df = -1; df <= 1; df++) {
                    if (!try_move({ sq, { sq.rank + dr, sq.file + df } }))
                        return IterationDecision::Break;
                }
            }

            // Castling moves.
            if (sq == Square("e1")) {
                keep_going = try_move({ sq, Square("c1") }) && try_move({ sq, Square("g1") });
            } else if (sq == Square("e8")) {
                keep_going = try_move({ sq, Square("c8") }) && try_move({ sq, Square("g8") });
            }
        }

        if (keep_going) {
            return IterationDecision::Continue;
        } else {
            return IterationDecision::Break;
        }
    });
}

}

template<>
struct AK::Traits<Chess::Piece> : public DefaultTraits<Chess::Piece> {
    static unsigned hash(Chess::Piece const& piece)
    {
        return pair_int_hash(static_cast<u32>(piece.color), static_cast<u32>(piece.type));
    }
};

template<>
struct AK::Traits<Chess::Board> : public DefaultTraits<Chess::Board> {
    static unsigned hash(Chess::Board const& chess)
    {
        unsigned hash = 0;
        hash = pair_int_hash(hash, static_cast<u32>(chess.m_white_can_castle_queenside));
        hash = pair_int_hash(hash, static_cast<u32>(chess.m_white_can_castle_kingside));
        hash = pair_int_hash(hash, static_cast<u32>(chess.m_black_can_castle_queenside));
        hash = pair_int_hash(hash, static_cast<u32>(chess.m_black_can_castle_kingside));

        Chess::Square::for_each([&](Chess::Square sq) {
            hash = pair_int_hash(hash, Traits<Chess::Piece>::hash(chess.get_piece(sq)));
            return IterationDecision::Continue;
        });

        return hash;
    }
};
