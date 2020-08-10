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
    };

    struct Move {
        Square from;
        Square to;
        Move(const StringView& algebraic);
        Move(const Square& from, const Square& to)
            : from(from)
            , to(to)
        {
        }
    };

    Chess();

    Piece get_piece(const Square&) const;
    Piece set_piece(const Square&, const Piece&);

    bool is_legal(const Move&) const;

    bool apply_move(const Move&);

    Colour turn() const { return m_turn; };

private:
    Piece m_board[8][8];
    Colour m_turn { Colour::White };
};

template<>
struct AK::Traits<Chess::Piece> : public GenericTraits<Chess::Piece> {
    static unsigned hash(Chess::Piece piece)
    {
        return pair_int_hash(static_cast<u32>(piece.colour), static_cast<u32>(piece.type));
    }
};
