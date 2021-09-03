/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/HashMap.h>
#include <LibCore/NetworkJob.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/Job.h>
#include <LibTLS/TLSv12.h>

namespace HTTP {

class HttpsJob final : public Job {
    C_OBJECT(HttpsJob)
public:
    explicit HttpsJob(const HttpRequest& request, OutputStream& output_stream, const Vector<Certificate>* override_certs = nullptr)
        : Job(request, output_stream)
        , m_override_ca_certificates(override_certs)
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
    virtual String read_line(size_t) override;
    virtual bool can_read() const override;
    virtual ByteBuffer receive(size_t) override;
    virtual bool eof() const override;
    virtual bool write(ReadonlyBytes) override;
    virtual bool is_established() const override { return m_socket->is_established(); }
    virtual bool should_fail_on_empty_payload() const override { return false; }
    virtual void read_while_data_available(Function<IterationDecision()>) override;

private:
    RefPtr<TLS::TLSv12> m_socket;
    const Vector<Certificate>* m_override_ca_certificates { nullptr };
};

}
