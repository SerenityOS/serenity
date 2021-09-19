/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/NetworkResponse.h>

namespace Core {

NetworkJob::NetworkJob(OutputStream& output_stream)
    : m_output_stream(output_stream)
{
}

NetworkJob::~NetworkJob()
{
}

void NetworkJob::start(NonnullRefPtr<Core::Socket>)
{
}

void NetworkJob::shutdown()
{
}

void NetworkJob::did_finish(NonnullRefPtr<NetworkResponse>&& response)
{
    // NOTE: We protect ourselves here, since the on_finish callback may otherwise
    //       trigger destruction of this job somehow.
    NonnullRefPtr<NetworkJob> protector(*this);

    m_response = move(response);
    dbgln_if(CNETWORKJOB_DEBUG, "{} job did_finish", *this);
    VERIFY(on_finish);
    on_finish(true);
    shutdown();
}

void NetworkJob::did_fail(Error error)
{
    // NOTE: We protect ourselves here, since the on_finish callback may otherwise
    //       trigger destruction of this job somehow.
    NonnullRefPtr<NetworkJob> protector(*this);

    m_error = error;
    dbgln_if(CNETWORKJOB_DEBUG, "{}{{{:p}}} job did_fail! error: {} ({})", class_name(), this, (unsigned)error, to_string(error));
    VERIFY(on_finish);
    on_finish(false);
    shutdown();
}

void NetworkJob::did_progress(Optional<u32> total_size, u32 downloaded)
{
    // NOTE: We protect ourselves here, since the callback may otherwise
    //       trigger destruction of this job somehow.
    NonnullRefPtr<NetworkJob> protector(*this);

    if (on_progress)
        on_progress(total_size, downloaded);
}

const char* to_string(NetworkJob::Error error)
{
    switch (error) {
    case NetworkJob::Error::ProtocolFailed:
        return "ProtocolFailed";
    case NetworkJob::Error::ConnectionFailed:
        return "ConnectionFailed";
    case NetworkJob::Error::TransmissionFailed:
        return "TransmissionFailed";
    case NetworkJob::Error::Cancelled:
        return "Cancelled";
    default:
        return "(Unknown error)";
    }
}

}
