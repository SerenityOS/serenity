/*
 * Copyright (c) 2020, the SerenityOS developers.
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

enum class Type {
    Pawn,
    Knight,
    Bishop,
    Rook,
    Queen,
    King,
    None,
};

String char_for_piece(Type type);
Chess::Type piece_for_char_promotion(const StringView& str);

enum class Color {
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
    bool operator==(const Piece& other) const { return color == other.color && type == other.type; }
};

constexpr Piece EmptyPiece = { Color::None, Type::None };

struct Square {
    int rank; // zero indexed;
    int file;
    Square(const StringView& name);
    Square(const int& rank, const int& file)
        : rank(rank)
        , file(file)
    {
    }
    bool operator==(const Square& other) const { return rank == other.rank && file == other.file; }

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
    String to_algebraic() const;
};

class Board;

struct Move {
    Square from;
    Square to;
    Type promote_to;
    Piece piece;
    bool is_check = false;
    bool is_mate = false;
    bool is_capture = false;
    bool is_ambiguous = false;
    Square ambiguous { 50, 50 };
    Move(const StringView& long_algebraic);
    Move(const Square& from, const Square& to, const Type& promote_to = Type::None)
        : from(from)
        , to(to)
        , promote_to(promote_to)
    {
    }
    bool operator==(const Move& other) const { return from == other.from && to == other.to && promote_to == other.promote_to; }

    static Move from_algebraic(const StringView& algebraic, const Color turn, const Board& board);
    String to_long_algebraic() const;
    String to_algebraic() const;
};

class Board {
public:
    Board();

    Piece get_piece(const Square&) const;
    Piece set_piece(const Square&, const Piece&);

    bool is_legal(const Move&, Color color = Color::None) const;
    bool in_check(Color color) const;

    bool is_promotion_move(const Move&, Color color = Color::None) const;

    bool apply_move(const Move&, Color color = Color::None);
    const Optional<Move>& last_move() const { return m_last_move; }

    String to_fen() const;

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

    static String result_to_string(Result, Color turn);
    static String result_to_points(Result, Color turn);

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
    const Vector<Move>& moves() const { return m_moves; }

    bool operator==(const Board& other) const;

private:
    bool is_legal_no_check(const Move&, Color color) const;
    bool is_legal_promotion(const Move&, Color color) const;
    bool apply_illegal_move(const Move&, Color color);

    Piece m_board[8][8];
    Color m_turn { Color::White };
    Color m_resigned { Color::None };
    Optional<Move> m_last_move;
    int m_moves_since_capture { 0 };
    int m_moves_since_pawn_advance { 0 };

    bool m_white_can_castle_kingside { true };
    bool m_white_can_castle_queenside { true };
    bool m_black_can_castle_kingside { true };
    bool m_black_can_castle_queenside { true };

    HashMap<Board, int> m_previous_states;
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
struct AK::Traits<Chess::Piece> : public GenericTraits<Chess::Piece> {
    static unsigned hash(Chess::Piece piece)
    {
        return pair_int_hash(static_cast<u32>(piece.color), static_cast<u32>(piece.type));
    }
};

template<>
struct AK::Traits<Chess::Board> : public GenericTraits<Chess::Board> {
    static unsigned hash(Chess::Board chess)
    {
        unsigned hash = 0;
        hash = pair_int_hash(hash, static_cast<u32>(chess.m_white_can_castle_queenside));
        hash = pair_int_hash(hash, static_cast<u32>(chess.m_white_can_castle_kingside));
        hash = pair_int_hash(hash, static_cast<u32>(chess.m_black_can_castle_queenside));
        hash = pair_int_hash(hash, static_cast<u32>(chess.m_black_can_castle_kingside));

        hash = pair_int_hash(hash, static_cast<u32>(chess.m_black_can_castle_kingside));

        Chess::Square::for_each([&](Chess::Square sq) {
            hash = pair_int_hash(hash, Traits<Chess::Piece>::hash(chess.get_piece(sq)));
            return IterationDecision::Continue;
        });

        return hash;
    }
};
