/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Engine.h"
#include <LibCore/File.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

Engine::~Engine()
{
    if (m_pid != -1)
        kill(m_pid, SIGINT);
}

Engine::Engine(StringView command)
{
    int wpipefds[2];
    int rpipefds[2];
    if (pipe2(wpipefds, O_CLOEXEC) < 0) {
        perror("pipe2");
        VERIFY_NOT_REACHED();
    }

    if (pipe2(rpipefds, O_CLOEXEC) < 0) {
        perror("pipe2");
        VERIFY_NOT_REACHED();
    }

    posix_spawn_file_actions_t file_actions;
    posix_spawn_file_actions_init(&file_actions);
    posix_spawn_file_actions_adddup2(&file_actions, wpipefds[0], STDIN_FILENO);
    posix_spawn_file_actions_adddup2(&file_actions, rpipefds[1], STDOUT_FILENO);

    String cstr(command);
    const char* argv[] = { cstr.characters(), nullptr };
    if (posix_spawnp(&m_pid, cstr.characters(), &file_actions, nullptr, const_cast<char**>(argv), environ) < 0) {
        perror("posix_spawnp");
        VERIFY_NOT_REACHED();
    }

    posix_spawn_file_actions_destroy(&file_actions);

    close(wpipefds[0]);
    close(rpipefds[1]);

    auto infile = Core::File::construct();
    infile->open(rpipefds[0], Core::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescriptor::Yes);
    set_in(infile);

    auto outfile = Core::File::construct();
    outfile->open(wpipefds[1], Core::OpenMode::WriteOnly, Core::File::ShouldCloseFileDescriptor::Yes);
    set_out(outfile);

    send_command(Chess::UCI::UCICommand());
}

void Engine::handle_bestmove(const Chess::UCI::BestMoveCommand& command)
{
    if (m_bestmove_callback)
        m_bestmove_callback(command.move());

    m_bestmove_callback = nullptr;
}
