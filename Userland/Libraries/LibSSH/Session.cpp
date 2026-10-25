/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Session.h"

#include <AK/Debug.h>
#include <AK/ScopeGuard.h>
#include <LibCore/System.h>
#include <errno.h>
#include <signal.h>

namespace SSH {

ExecData ExecData::create(
    Core::Process&& process,
    NonnullOwnPtr<Core::File>&& stdin_,
    NonnullOwnPtr<Core::File>&& stdout_,
    NonnullOwnPtr<Core::File>&& stderr_)
{
    return {
        move(process),
        move(stdin_),
        move(stdout_),
        move(stderr_),
    };
}

static ErrorOr<size_t> write_to_child_stdin(Core::File& stdin_file, ReadonlyBytes bytes)
{
    struct sigaction previous_action {};
    struct sigaction ignored_action {};
    ignored_action.sa_handler = SIG_IGN;
    sigemptyset(&ignored_action.sa_mask);
    TRY(Core::System::sigaction(SIGPIPE, &ignored_action, &previous_action));
    ScopeGuard restore_sigpipe_action { [&] { MUST(Core::System::sigaction(SIGPIPE, &previous_action, nullptr)); } };
    return stdin_file.write_some(bytes);
}

Coroutine<ErrorOr<BehaviorControl>> ExecData::handle_channel_data(Session& session)
{
    CO_TRY(co_await stdin_->wait_for_state(Core::Notifier::Type::Write | Core::Notifier::Type::Error | Core::Notifier::Type::HangUp));
    auto& stream = session.channel_data;
    auto maybe_written = write_to_child_stdin(*stdin_, stream.data());
    if (maybe_written.is_error()) {
        auto error = maybe_written.release_error();
        if (!error.is_errno() || error.code() != EPIPE)
            co_return move(error);

        session.dequeue_channel_data(stream.data().size());
        stdin_->close();
        close_stdin_if_required(session);
        co_return BehaviorControl::ContinueExecution;
    }

    session.dequeue_channel_data(maybe_written.release_value());
    close_stdin_if_required(session);

    co_return BehaviorControl::ContinueExecution;
}

void ExecData::handle_channel_eof(Session const& session)
{
    close_stdin_if_required(session);
}

void ExecData::terminate()
{
    if (!exit_status.has_value()) {
        auto maybe_error = Core::System::killpg(child.pid(), SIGKILL);
        if (maybe_error.is_error()) {
            auto error = maybe_error.release_error();
            if (!error.is_errno() || error.code() != ESRCH)
                dbgln("Unable to terminate SSH child process: {}", error);
        }
    }

    // Keep the pipe descriptors open until the async coroutines observe the
    // child's exit; closing them first would leave their poll waiters behind.
}

void ExecData::close_stdin_if_required(Session const& session) const
{
    if (session.channel_data.is_empty() && session.has_received_eof)
        stdin_->close();
}

bool ExecData::is_ready_to_be_closed() const
{
    return exit_status.has_value() && stdout_->is_eof() && stderr_->is_eof();
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
        [](ExecData const& exec_data) { return *exec_data.exit_status; });
}

}
