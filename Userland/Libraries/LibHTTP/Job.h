/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/Socket.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>

namespace HTTP {

class Job : public Core::NetworkJob {
    C_OBJECT(Job);

public:
    explicit Job(HttpRequest&&, Core::File&);
    virtual ~Job() override = default;

    virtual void start(Core::BufferedSocketBase&) override;
    virtual void shutdown(ShutdownMode) override;

    Core::Socket const* socket() const { return m_socket; }
    URL::URL url() const { return m_request.url(); }

    HttpResponse* response() { return static_cast<HttpResponse*>(Core::NetworkJob::response()); }
    HttpResponse const* response() const { return static_cast<HttpResponse const*>(Core::NetworkJob::response()); }

private:
    auto parse_status(auto& stream) -> Coroutine<ErrorOr<void>>;
    auto parse_headers(auto& stream, bool in_trailers) -> Coroutine<ErrorOr<void>>;
    auto parse_body(auto& stream) -> Coroutine<ErrorOr<bool>>;
    auto read_response() -> Coroutine<ErrorOr<void>>;

protected:
    Coroutine<void> finish_up();
    void on_socket_connected();
    Coroutine<void> flush_received_buffers();

    HttpRequest m_request;
    Core::BufferedSocketBase* m_socket { nullptr };
    bool m_legacy_connection { false };
    int m_code { -1 };
    HTTP::HeaderMap m_headers;

    struct ReceivedBuffer {
        ReceivedBuffer(ByteBuffer d)
            : data(move(d))
            , pending_flush(data.bytes())
        {
        }

        // The entire received buffer.
        ByteBuffer data;

        // The bytes we have yet to flush. (This is a slice of `data`)
        ReadonlyBytes pending_flush;
    };

    Vector<NonnullOwnPtr<ReceivedBuffer>> m_received_buffers;

    size_t m_buffered_size { 0 };
    size_t m_received_size { 0 };
    Optional<u64> m_content_length;
    Optional<ssize_t> m_current_chunk_remaining_size;
    Optional<size_t> m_current_chunk_total_size;
    bool m_can_stream_response { true };
    bool m_should_read_chunk_ending_line { false };
    bool m_has_scheduled_finish { false };
    bool m_has_scheduled_flush { false };
    bool m_request_done { false };
};

}
