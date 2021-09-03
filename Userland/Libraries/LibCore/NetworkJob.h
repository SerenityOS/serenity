/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Function.h>
#include <YAK/Stream.h>
#include <LibCore/Object.h>

namespace Core {

class NetworkJob : public Object {
    C_OBJECT_ABSTRACT(NetworkJob)
public:
    enum class Error {
        None,
        ConnectionFailed,
        TransmissionFailed,
        ProtocolFailed,
        Cancelled,
    };
    virtual ~NetworkJob() override;

    // Could fire twice, after Headers and after Trailers!
    Function<void(const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> response_code)> on_headers_received;
    Function<void(bool success)> on_finish;
    Function<void(Optional<u32>, u32)> on_progress;

    bool is_cancelled() const { return m_error == Error::Cancelled; }
    bool has_error() const { return m_error != Error::None; }
    Error error() const { return m_error; }
    NetworkResponse* response() { return m_response.ptr(); }
    const NetworkResponse* response() const { return m_response.ptr(); }

    virtual void start() = 0;
    virtual void shutdown() = 0;

    void cancel()
    {
        shutdown();
        m_error = Error::Cancelled;
    }

protected:
    NetworkJob(OutputStream&);
    void did_finish(NonnullRefPtr<NetworkResponse>&&);
    void did_fail(Error);
    void did_progress(Optional<u32> total_size, u32 downloaded);

    size_t do_write(ReadonlyBytes bytes) { return m_output_stream.write(bytes); }

private:
    RefPtr<NetworkResponse> m_response;
    OutputStream& m_output_stream;
    Error m_error { Error::None };
};

const char* to_string(NetworkJob::Error);

}

template<>
struct YAK::Formatter<Core::NetworkJob> : Formatter<Core::Object> {
};
