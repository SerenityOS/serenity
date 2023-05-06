/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MCTSTree.h"
#include <LibChess/Chess.h>
#include <LibChess/UCIEndpoint.h>

class ChessEngine : public Chess::UCI::Endpoint {
    C_OBJECT_ABSTRACT(ChessEngine)
public:
    static ErrorOr<NonnullRefPtr<ChessEngine>> try_create(NonnullOwnPtr<Core::File> in, NonnullOwnPtr<Core::File> out)
    {
        auto engine = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ChessEngine()));
        TRY(engine->set_in(move(in)));
        engine->set_out(move(out));
        return engine;
    }
    virtual ~ChessEngine() override = default;

    virtual void handle_uci() override;
    virtual void handle_position(Chess::UCI::PositionCommand const&) override;
    virtual void handle_go(Chess::UCI::GoCommand const&) override;
    virtual void handle_quit() override;
    virtual void handle_ucinewgame() override;
    virtual void handle_unexpected_eof() override;

    Function<void(int)> on_quit;

private:
    ChessEngine()
        : Endpoint()
    {
        on_command_read_error = [](auto command, auto error) {
            outln("{}: '{}'", error, command);
        };
    }

    Chess::Board m_board;
    Optional<MCTSTree> m_last_tree;
};
