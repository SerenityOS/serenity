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

#include "Engine.h"
#include <LibCore/File.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>

Engine::~Engine()
{
    if (m_pid != -1)
        kill(m_pid, SIGINT);
}

Engine::Engine(const StringView& command)
{
    int wpipefds[2];
    int rpipefds[2];
    if (pipe2(wpipefds, O_CLOEXEC) < 0) {
        perror("pipe2");
        ASSERT_NOT_REACHED();
    }

    if (pipe2(rpipefds, O_CLOEXEC) < 0) {
        perror("pipe2");
        ASSERT_NOT_REACHED();
    }

    posix_spawn_file_actions_t file_actions;
    posix_spawn_file_actions_init(&file_actions);
    posix_spawn_file_actions_adddup2(&file_actions, wpipefds[0], STDIN_FILENO);
    posix_spawn_file_actions_adddup2(&file_actions, rpipefds[1], STDOUT_FILENO);

    String cstr(command);
    const char* argv[] = { cstr.characters(), nullptr };
    if (posix_spawnp(&m_pid, cstr.characters(), &file_actions, nullptr, const_cast<char**>(argv), environ) < 0) {
        perror("posix_spawnp");
        ASSERT_NOT_REACHED();
    }

    posix_spawn_file_actions_destroy(&file_actions);

    close(wpipefds[0]);
    close(rpipefds[1]);

    auto infile = Core::File::construct();
    infile->open(rpipefds[0], Core::IODevice::ReadOnly, Core::File::ShouldCloseFileDescription::Yes);
    set_in(infile);

    auto outfile = Core::File::construct();
    outfile->open(wpipefds[1], Core::IODevice::WriteOnly, Core::File::ShouldCloseFileDescription::Yes);
    set_out(outfile);

    send_command(Chess::UCI::UCICommand());
}

void Engine::handle_bestmove(const Chess::UCI::BestMoveCommand& command)
{
    if (m_bestmove_callback)
        m_bestmove_callback(command.move());

    m_bestmove_callback = nullptr;
}
