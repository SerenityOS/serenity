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

#include <AK/HashMap.h>
#include <LibCore/NetworkJob.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <LibTLS/TLSv12.h>

namespace HTTP {

class HttpsJob final : public Core::NetworkJob {
    C_OBJECT(HttpsJob)
public:
    explicit HttpsJob(const HttpRequest&);
    virtual ~HttpsJob() override;

    virtual void start() override;
    virtual void shutdown() override;

    HttpResponse* response() { return static_cast<HttpResponse*>(Core::NetworkJob::response()); }
    const HttpResponse* response() const { return static_cast<const HttpResponse*>(Core::NetworkJob::response()); }

private:
    RefPtr<TLS::TLSv12> construct_socket() { return TLS::TLSv12::construct(this); }
    void on_socket_connected();
    void finish_up();
    void read_body(TLS::TLSv12&);

    enum class State {
        InStatus,
        InHeaders,
        InBody,
        Finished,
    };

    HttpRequest m_request;
    RefPtr<TLS::TLSv12> m_socket;
    State m_state { State::InStatus };
    int m_code { -1 };
    HashMap<String, String> m_headers;
    Vector<ByteBuffer> m_received_buffers;
    size_t m_received_size { 0 };
    bool m_sent_data { false };
    bool m_queued_finish { false };
};

}
