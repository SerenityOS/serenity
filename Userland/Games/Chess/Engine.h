/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibChess/UCIEndpoint.h>
#include <sys/types.h>

class Engine : public Chess::UCI::Endpoint {
    C_OBJECT(Engine)
public:
    virtual ~Engine() override;

    Engine(String command);

    Engine(Engine const&) = delete;
    Engine& operator=(Engine const&) = delete;

    Function<void()> on_connection_lost;

    virtual void handle_bestmove(Chess::UCI::BestMoveCommand const&) override;
    virtual void handle_unexpected_eof() override;

    template<typename Callback>
    void get_best_move(Chess::Board const& board, int time_limit, Callback&& callback)
    {
        if (!m_connected)
            connect_to_engine_service();

        send_command(Chess::UCI::PositionCommand({}, board.moves()));
        Chess::UCI::GoCommand go_command;
        go_command.movetime = time_limit;
        send_command(go_command);
        m_bestmove_callback = move(callback);
    }

    void start_new_game()
    {
        if (!m_connected)
            return;

        Chess::UCI::UCINewGameCommand ucinewgame_command;
        send_command(ucinewgame_command);
    }

private:
    void quit();
    void connect_to_engine_service();

    String m_command;
    Function<void(ErrorOr<Chess::Move>)> m_bestmove_callback;
    bool m_connected { false };
};
