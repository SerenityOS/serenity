/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Session.h"

namespace SSH {

ErrorOr<ExecData> ExecData::create(Core::Process&& process, int fd_stdin, int fd_stdout, int fd_stderr)
{
    ExecData exec_data = {
        move(process),
        TRY(Core::File::adopt_fd(fd_stdin, Core::File::OpenMode::Write)),
        TRY(Core::File::adopt_fd(fd_stdout, Core::File::OpenMode::Read)),
        TRY(Core::File::adopt_fd(fd_stderr, Core::File::OpenMode::Read)),
    };

    TRY(exec_data.stdin_->set_blocking(false));
    TRY(exec_data.stdout_->set_blocking(false));
    TRY(exec_data.stderr_->set_blocking(false));

    return exec_data;
}

Coroutine<ErrorOr<void>> ExecData::handle_channel_data(Session& session)
{
    CO_TRY(co_await stdin_->wait_for_state(Core::Notifier::Type::Write));
    auto& stream = session.channel_data;
    auto written = CO_TRY(stdin_->write_some(stream.data()));
    stream.dequeue(written);

    close_stdin_if_required(session);

    co_return {};
}

void ExecData::handle_channel_eof(Session const& session)
{
    close_stdin_if_required(session);
}

void ExecData::close_stdin_if_required(Session const& session) const
{
    if (session.channel_data.is_empty() && session.has_received_eof)
        stdin_->close();
}

ErrorOr<NonnullRefPtr<Session>> Session::create(u32 sender_channel_id, u32 window_size, u32 maximum_packet_size)
{
    auto session = make_ref_counted<Session>();
    session->local_channel_id = sender_channel_id;
    session->sender_channel_id = sender_channel_id;
    session->maximum_packet_size = maximum_packet_size;
    session->window_size = window_size;
    return session;
}

}
