/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Session.h"

namespace SSH {

Coroutine<ErrorOr<void>> ExecData::handle_channel_data(Session& session)
{
    CO_TRY(co_await stdin_->wait_for_state(Core::Notifier::Type::Write));
    auto& stream = session.channel_data;
    auto written = CO_TRY(stdin_->write_some(stream.data()));
    stream.dequeue(written);
    co_return {};
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
