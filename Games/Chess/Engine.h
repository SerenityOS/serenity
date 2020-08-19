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

#include <AK/Function.h>
#include <LibChess/UCIEndpoint.h>
#include <sys/types.h>

class Engine : public Chess::UCI::Endpoint {
    C_OBJECT(Engine)
public:
    virtual ~Engine() override;

    Engine(const StringView& command);

    Engine(const Engine&) = delete;
    Engine& operator=(const Engine&) = delete;

    virtual void handle_bestmove(const Chess::UCI::BestMoveCommand&);

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
