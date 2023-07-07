/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Engine.h"
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

Engine::~Engine()
{
    quit();
}

Engine::Engine(String command)
    : m_command(move(command))
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

    auto command_length = m_command.code_points().length();
    auto command_name = new char[command_length + 1];
    memcpy(command_name, m_command.bytes_as_string_view().characters_without_null_termination(), command_length);
    command_name[command_length] = '\0';

    char const* argv[] = { command_name, nullptr };

    pid_t pid = -1;
    if (posix_spawnp(&pid, command_name, &file_actions, nullptr, const_cast<char**>(argv), environ) < 0) {
        perror("posix_spawnp");
        VERIFY_NOT_REACHED();
    }

    delete[] command_name;

    posix_spawn_file_actions_destroy(&file_actions);

    close(wpipefds[0]);
    close(rpipefds[1]);

    auto infile = Core::File::adopt_fd(rpipefds[0], Core::File::OpenMode::Read).release_value_but_fixme_should_propagate_errors();
    infile->set_blocking(false).release_value_but_fixme_should_propagate_errors();
    set_in(move(infile)).release_value_but_fixme_should_propagate_errors();

    auto outfile = Core::File::adopt_fd(wpipefds[1], Core::File::OpenMode::Write).release_value_but_fixme_should_propagate_errors();
    outfile->set_blocking(false).release_value_but_fixme_should_propagate_errors();
    set_out(move(outfile));

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
