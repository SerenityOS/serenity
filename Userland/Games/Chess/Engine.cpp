/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Engine.h"
#include <LibCore/DeprecatedFile.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

Engine::~Engine()
{
    quit();
}

Engine::Engine(StringView command)
    : m_command(command)
{
    connect_to_engine_service();
}

void Engine::connect_to_engine_service()
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

    char const* argv[] = { m_command.characters(), nullptr };
    pid_t pid = -1;
    if (posix_spawnp(&pid, m_command.characters(), &file_actions, nullptr, const_cast<char**>(argv), environ) < 0) {
        perror("posix_spawnp");
        VERIFY_NOT_REACHED();
    }

    posix_spawn_file_actions_destroy(&file_actions);

    close(wpipefds[0]);
    close(rpipefds[1]);

    auto infile = Core::DeprecatedFile::construct();
    infile->open(rpipefds[0], Core::OpenMode::ReadOnly, Core::DeprecatedFile::ShouldCloseFileDescriptor::Yes);
    set_in(infile);

    auto outfile = Core::DeprecatedFile::construct();
    outfile->open(wpipefds[1], Core::OpenMode::WriteOnly, Core::DeprecatedFile::ShouldCloseFileDescriptor::Yes);
    set_out(outfile);

    send_command(Chess::UCI::UCICommand());
    m_connected = true;
}

void Engine::handle_bestmove(Chess::UCI::BestMoveCommand const& command)
{
    if (m_bestmove_callback)
        m_bestmove_callback(command.move());

    m_bestmove_callback = nullptr;
}

void Engine::quit()
{
    if (!m_connected)
        return;

    send_command(Chess::UCI::QuitCommand());
    m_connected = false;
}

void Engine::handle_unexpected_eof()
{
    m_connected = false;
    if (m_bestmove_callback)
        m_bestmove_callback(Error::from_errno(EPIPE));

    m_bestmove_callback = nullptr;

    if (on_connection_lost)
        on_connection_lost();
}
