/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/Socket.h>
#include <LibGemini/GeminiRequest.h>
#include <LibGemini/GeminiResponse.h>

namespace Gemini {

class Job : public Core::NetworkJob {
    C_OBJECT(Job);

public:
    explicit Job(GeminiRequest const&, Core::File&);
    virtual ~Job() override = default;

    virtual void start(Core::BufferedSocketBase&) override;
    virtual void shutdown(ShutdownMode) override;

    GeminiResponse* response() { return static_cast<GeminiResponse*>(Core::NetworkJob::response()); }
    GeminiResponse const* response() const { return static_cast<GeminiResponse const*>(Core::NetworkJob::response()); }

    const URL::URL& url() const { return m_request.url(); }
    Core::Socket const* socket() const { return m_socket; }

    ErrorOr<size_t> response_length() const;

protected:
    void finish_up();
    void on_socket_connected();
    void flush_received_buffers();
    void register_on_ready_to_read(Function<void()>);
    bool can_read_line() const;
    ErrorOr<String> read_line(size_t);
    bool can_read() const;
    ErrorOr<ByteBuffer> receive(size_t);
    bool write(ReadonlyBytes);

    enum class State {
        InStatus,
        InBody,
        Finished,
        Failed,
    };

    GeminiRequest m_request;
    State m_state { State::InStatus };
    int m_status { -1 };
    ByteString m_meta;
    Vector<ByteBuffer, 2> m_received_buffers;
    size_t m_received_size { 0 };
    size_t m_buffered_size { 0 };
    Core::BufferedSocketBase* m_socket { nullptr };
};

}
