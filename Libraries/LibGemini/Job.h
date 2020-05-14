/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    explicit Job(const GeminiRequest&);
    virtual ~Job() override;

    virtual void start() override = 0;
    virtual void shutdown() override = 0;

    GeminiResponse* response() { return static_cast<GeminiResponse*>(Core::NetworkJob::response()); }
    const GeminiResponse* response() const { return static_cast<const GeminiResponse*>(Core::NetworkJob::response()); }

protected:
    void finish_up();
    void on_socket_connected();
    virtual void register_on_ready_to_read(Function<void()>) = 0;
    virtual void register_on_ready_to_write(Function<void()>) = 0;
    virtual bool can_read_line() const = 0;
    virtual ByteBuffer read_line(size_t) = 0;
    virtual bool can_read() const = 0;
    virtual ByteBuffer receive(size_t) = 0;
    virtual bool eof() const = 0;
    virtual bool write(const ByteBuffer&) = 0;
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
    Vector<ByteBuffer> m_received_buffers;
    size_t m_received_size { 0 };
    bool m_sent_data { false };
    bool m_should_have_payload { false };
};

}
