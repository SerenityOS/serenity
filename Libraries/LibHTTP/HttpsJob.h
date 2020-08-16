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
#include <LibHTTP/Job.h>
#include <LibTLS/TLSv12.h>

namespace HTTP {

class HttpsJob final : public Job {
    C_OBJECT(HttpsJob)
public:
    explicit HttpsJob(const HttpRequest& request)
        : Job(request)
    {
    }

    virtual ~HttpsJob() override
    {
    }

    virtual void start() override;
    virtual void shutdown() override;
    void set_certificate(String certificate, String key);

    Function<void(HttpsJob&)> on_certificate_requested;

protected:
    virtual void register_on_ready_to_read(Function<void()>) override;
    virtual void register_on_ready_to_write(Function<void()>) override;
    virtual bool can_read_line() const override;
    virtual ByteBuffer read_line(size_t) override;
    virtual bool can_read() const override;
    virtual ByteBuffer receive(size_t) override;
    virtual bool eof() const override;
    virtual bool write(const ByteBuffer&) override;
    virtual bool is_established() const override { return m_socket->is_established(); }
    virtual bool should_fail_on_empty_payload() const override { return false; }
    virtual void read_while_data_available(Function<IterationDecision()>) override;

private:
    RefPtr<TLS::TLSv12> m_socket;
};

}
