/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/TCPSocket.h>
#include <LibGemini/GeminiRequest.h>
#include <LibGemini/GeminiResponse.h>

namespace Gemini {

class Job : public Core::NetworkJob {
public:
    explicit Job(const GeminiRequest&, OutputStream&);
    virtual ~Job() override;

    virtual void start(NonnullRefPtr<Core::Socket>) override = 0;
    virtual void shutdown(ShutdownMode) override = 0;

    GeminiResponse* response() { return static_cast<GeminiResponse*>(Core::NetworkJob::response()); }
    const GeminiResponse* response() const { return static_cast<const GeminiResponse*>(Core::NetworkJob::response()); }

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
    virtual bool should_fail_on_empty_payload() const { return false; }
    virtual void read_while_data_available(Function<IterationDecision()> read) { read(); };

    enum class State {
        InStatus,
        InBody,
        Finished,
    };

    GeminiRequest m_request;
    State m_state { State::InStatus };
    int m_status { -1 };
    String m_meta;
    Vector<ByteBuffer, 2> m_received_buffers;
    size_t m_received_size { 0 };
    bool m_sent_data { false };
    bool m_should_have_payload { false };
};

}
