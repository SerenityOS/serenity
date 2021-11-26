/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibChess/Chess.h>
#include <LibChess/UCIEndpoint.h>

class ChessEngine : public Chess::UCI::Endpoint {
    C_OBJECT(ChessEngine)
public:
    virtual ~ChessEngine() override { }

    virtual void handle_uci() override;
    virtual void handle_position(const Chess::UCI::PositionCommand&) override;
    virtual void handle_go(const Chess::UCI::GoCommand&) override;

private:
    ChessEngine() { }
    ChessEngine(NonnullRefPtr<Core::IODevice> in, NonnullRefPtr<Core::IODevice> out)
        : Endpoint(in, out)
    {
    }

    Chess::Board m_board;
};
