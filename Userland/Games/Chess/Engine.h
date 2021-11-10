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

    Engine(StringView command);

    Engine(const Engine&) = delete;
    Engine& operator=(const Engine&) = delete;

    virtual void handle_bestmove(const Chess::UCI::BestMoveCommand&) override;

    template<typename Callback>
    void get_best_move(const Chess::Board& board, int time_limit, Callback&& callback)
    {
        send_command(Chess::UCI::PositionCommand({}, board.moves()));
        Chess::UCI::GoCommand go_command;
        go_command.movetime = time_limit;
        send_command(go_command);
        m_bestmove_callback = move(callback);
    }

private:
    Function<void(Chess::Move)> m_bestmove_callback;
    pid_t m_pid { -1 };
};
