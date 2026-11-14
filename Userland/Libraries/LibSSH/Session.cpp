/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Session.h"

namespace SSH {

Coroutine<ErrorOr<BehaviorControl>> ChildManagingSystem::handle_channel_data(Session& session)
{
    CO_TRY(co_await stdin_->wait_for_state(Core::Notifier::Type::Write));
    auto& stream = session.channel_data;
    auto written = CO_TRY(stdin_->write_some(stream.data()));
    stream.dequeue(written);

    close_stdin_if_required(session);

    co_return BehaviorControl::ContinueExecution;
}

void ChildManagingSystem::handle_channel_eof(Session const& session) const
{
    close_stdin_if_required(session);
}

void ChildManagingSystem::close_stdin_if_required(Session const& session) const
{
    if (session.channel_data.is_empty() && session.has_received_eof)
        stdin_->close();
}

ErrorOr<PipeChild> PipeChild::create(Core::Process&& process, int fd_stdin, int fd_stdout, int fd_stderr)
{
    PipeChild child = {
        ChildManagingSystem {
            .stdin_ = TRY(Core::File::adopt_fd(fd_stdin, Core::File::OpenMode::Write)),
            .stdout_ = TRY(Core::File::adopt_fd(fd_stdout, Core::File::OpenMode::Read)),
            .child_pid = process.pid(),
        },
        move(process),
        TRY(Core::File::adopt_fd(fd_stderr, Core::File::OpenMode::Read)),
    };

    TRY(child.stdin_->set_blocking(false));
    TRY(child.stdout_->set_blocking(false));
    TRY(child.stderr_->set_blocking(false));

    return child;
}

bool PipeChild::is_ready_to_be_closed() const
{
    return exit_status.has_value() && stdout_->is_eof() && stderr_->is_eof();
}

ErrorOr<PtyChild> PtyChild::create(pid_t child_pid, int fd_pty)
{
    PtyChild child = {
        ChildManagingSystem {
            .stdin_ = TRY(Core::File::adopt_fd(fd_pty, Core::File::OpenMode::Write)),
            .stdout_ = TRY(Core::File::adopt_fd(fd_pty, Core::File::OpenMode::Read, Core::File::ShouldCloseFileDescriptor::No)),
            .child_pid = child_pid,
        },
    };

    TRY(child.stdin_->set_blocking(false));

    return child;
}

bool PtyChild::is_ready_to_be_closed() const
{
    return exit_status.has_value() && (stdout_->is_eof() || received_io_error);
}

ErrorOr<NonnullRefPtr<Session>> Session::create(u32 sender_channel_id, u32 window_size, u32 maximum_packet_size)
{
    auto session = make_ref_counted<Session>();
    session->local_channel_id = sender_channel_id;
    session->sender_channel_id = sender_channel_id;
    session->maximum_packet_size = maximum_packet_size;
    session->initial_window_size = window_size;
    session->local_window_size = window_size;
    return session;
}

bool Session::is_ready_to_be_closed() const
{
    return system.visit(
        [](Empty) { return true; },
        [](auto& system) { return system.is_ready_to_be_closed(); });
}

int Session::exit_status() const
{
    return system.visit(
        [](auto const&) { return 0; },
        []<DerivedFrom<ChildManagingSystem> T>(T const& child) { return *child.exit_status; });
}

}
