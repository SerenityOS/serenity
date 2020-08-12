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

#pragma once

#include <AK/HashMap.h>
#include <AK/IterationDecision.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Traits.h>

class Chess {
public:
    enum class Type {
        Pawn,
        Knight,
        Bishop,
        Rook,
        Queen,
        King,
        None,
    };

    enum class Colour {
        White,
        Black,
        None,
    };
    static Colour opposing_colour(Colour colour) { return (colour == Colour::White) ? Colour::Black : Colour::White; }

    struct Piece {
        Colour colour;
        Type type;
        bool operator==(const Piece& other) const { return colour == other.colour && type == other.type; }
    };
    static constexpr Piece EmptyPiece = { Colour::None, Type::None };

    struct Square {
        unsigned rank; // zero indexed;
        unsigned file;
        Square(const StringView& name);
        Square(const unsigned& rank, const unsigned& file)
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
                    if (callback(Square(rank, file)) == IterationDecision::Break) {
                        goto exit;
                    }
                }
            }
        exit:;
        }

        bool in_bounds() const { return rank < 8 && file < 8; }
        bool is_light() const { return (rank % 2) != (file % 2); }
    };

    struct Move {
        Square from;
        Square to;
        Type promote_to;
        Move(const StringView& algebraic);
        Move(const Square& from, const Square& to, const Type& promote_to = Type::Queen)
            : from(from)
            , to(to)
            , promote_to(promote_to)
        {
        }
        bool operator==(const Move& other) const { return from == other.from && to == other.to && promote_to == other.promote_to; }
    };

    Chess();

    Piece get_piece(const Square&) const;
    Piece set_piece(const Square&, const Piece&);

    bool is_legal(const Move&, Colour colour = Colour::None) const;
    bool in_check(Colour colour) const;

    bool is_promotion_move(const Move&, Colour colour = Colour::None) const;

    bool apply_move(const Move&, Colour colour = Colour::None);
    const Optional<Move>& last_move() const { return m_last_move; }

    enum class Result {
        CheckMate,
        StaleMate,
        FiftyMoveRule,
        SeventyFiveMoveRule,
        ThreeFoldRepitition,
        FiveFoldRepitition,
        InsufficientMaterial,
        NotFinished,
    };

    template<typename Callback>
    void generate_moves(Callback callback, Colour colour = Colour::None) const;
    Result game_result() const;

    Colour turn() const { return m_turn; };

    bool operator==(const Chess& other) const;

private:
    bool is_legal_no_check(const Move&, Colour colour) const;
    bool apply_illegal_move(const Move&, Colour colour);

    Piece m_board[8][8];
    Colour m_turn { Colour::White };
    Optional<Move> m_last_move;
    int m_moves_since_capture { 0 };

    bool m_white_can_castle_kingside { true };
    bool m_white_can_castle_queenside { true };
    bool m_black_can_castle_kingside { true };
    bool m_black_can_castle_queenside { true };

    HashMap<Chess, int> m_previous_states;
    friend struct AK::Traits<Chess>;
};

template<>
struct AK::Traits<Chess::Piece> : public GenericTraits<Chess::Piece> {
    static unsigned hash(Chess::Piece piece)
    {
        return pair_int_hash(static_cast<u32>(piece.colour), static_cast<u32>(piece.type));
    }
};

template<>
struct AK::Traits<Chess> : public GenericTraits<Chess> {
    static unsigned hash(Chess chess)
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

template<typename Callback>
void Chess::generate_moves(Callback callback, Colour colour) const
{
    if (colour == Colour::None)
        colour = turn();

    auto try_move = [&](Move m) {
        if (is_legal(m, colour)) {
            if (callback(m) == IterationDecision::Break)
                return false;
        }
        return true;
    };

    Square::for_each([&](Square sq) {
        auto piece = get_piece(sq);
        if (piece.colour != colour)
            return IterationDecision::Continue;

        bool keep_going = true;
        if (piece.type == Type::Pawn) {
            keep_going =
                try_move({sq, {sq.rank+1, sq.file}}) &&
                try_move({sq, {sq.rank+2, sq.file}}) &&
                try_move({sq, {sq.rank-1, sq.file}}) &&
                try_move({sq, {sq.rank-2, sq.file}}) &&
                try_move({sq, {sq.rank+1, sq.file+1}}) &&
                try_move({sq, {sq.rank+1, sq.file-1}}) &&
                try_move({sq, {sq.rank-1, sq.file+1}}) &&
                try_move({sq, {sq.rank-1, sq.file-1}});
        } else if (piece.type == Type::Knight) {
            keep_going =
                try_move({sq, {sq.rank+2, sq.file+1}}) &&
                try_move({sq, {sq.rank+2, sq.file-1}}) &&
                try_move({sq, {sq.rank+1, sq.file+2}}) &&
                try_move({sq, {sq.rank+1, sq.file-2}}) &&
                try_move({sq, {sq.rank-2, sq.file+1}}) &&
                try_move({sq, {sq.rank-2, sq.file-1}}) &&
                try_move({sq, {sq.rank-1, sq.file+2}}) &&
                try_move({sq, {sq.rank-1, sq.file-2}});
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
