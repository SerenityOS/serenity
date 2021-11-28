/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FileStream.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/TCPSocket.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>

namespace HTTP {

class Job : public Core::NetworkJob {
public:
    explicit Job(HttpRequest&&, OutputStream&);
    virtual ~Job() override;

    virtual void start(NonnullRefPtr<Core::Socket>) override = 0;
    virtual void shutdown(ShutdownMode) override = 0;

    HttpResponse* response() { return static_cast<HttpResponse*>(Core::NetworkJob::response()); }
    const HttpResponse* response() const { return static_cast<const HttpResponse*>(Core::NetworkJob::response()); }

protected:
    void finish_up();
    void on_socket_connected();
    void flush_received_buffers();
    virtual void register_on_ready_to_read(Function<void()>) = 0;
    virtual void register_on_ready_to_write(Function<void()>) = 0;
    virtual bool can_read_line() const = 0;
    virtual String read_line(size_t) = 0;
    virtual bool can_read() const = 0;
    virtual ByteBuffer receive(size_t) = 0;
    virtual bool eof() const = 0;
    virtual bool write(ReadonlyBytes) = 0;
    virtual bool is_established() const = 0;
    virtual bool should_fail_on_empty_payload() const { return true; }
    virtual void read_while_data_available(Function<IterationDecision()> read) { read(); };
    virtual void timer_event(Core::TimerEvent&) override;

    enum class State {
        InStatus,
        InHeaders,
        InBody,
        Trailers,
        Finished,
    };

    HttpRequest m_request;
    State m_state { State::InStatus };
    int m_code { -1 };
    HashMap<String, String, CaseInsensitiveStringTraits> m_headers;
    Vector<String> m_set_cookie_headers;
    Vector<ByteBuffer, 2> m_received_buffers;
    size_t m_buffered_size { 0 };
    size_t m_received_size { 0 };
    bool m_sent_data { 0 };
    Optional<u32> m_content_length;
    Optional<ssize_t> m_current_chunk_remaining_size;
    Optional<size_t> m_current_chunk_total_size;
    bool m_can_stream_response { true };
    bool m_should_read_chunk_ending_line { false };
    bool m_has_scheduled_finish { false };
};

}
