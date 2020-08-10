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

#include "Chess.h"
#include <AK/Assertions.h>

Chess::Square::Square(const StringView& name)
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

Chess::Chess()
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

Chess::Piece Chess::get_piece(const Square& square) const
{
    ASSERT(square.rank < 8);
    ASSERT(square.file < 8);
    return m_board[square.rank][square.file];
}

Chess::Piece Chess::set_piece(const Square& square, const Piece& piece)
{
    ASSERT(square.rank < 8);
    ASSERT(square.file < 8);
    return m_board[square.rank][square.file] = piece;
}

bool Chess::is_legal(const Move& move) const
{
    // FIXME: Impelement actual chess logic.
    return get_piece(move.from).colour == turn() && get_piece(move.to).colour != turn();
}

bool Chess::apply_move(const Move& move)
{
    if (!is_legal(move)) {
        return false;
    }

    set_piece(move.to, get_piece(move.from));
    set_piece(move.from, EmptyPiece);

    if (m_turn == Colour::White) {
        m_turn = Colour::Black;
    } else if (m_turn == Colour::Black) {
        m_turn = Colour::White;
    }

    return true;
}
