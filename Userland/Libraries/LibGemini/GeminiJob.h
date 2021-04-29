/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/NetworkJob.h>
#include <LibGemini/GeminiRequest.h>
#include <LibGemini/GeminiResponse.h>
#include <LibGemini/Job.h>
#include <LibTLS/TLSv12.h>

namespace Gemini {

class GeminiJob final : public Job {
    C_OBJECT(GeminiJob)
public:
    explicit GeminiJob(const GeminiRequest& request, OutputStream& output_stream, const Vector<Certificate>* override_certificates = nullptr)
        : Job(request, output_stream)
        , m_override_ca_certificates(override_certificates)
    {
    }

    virtual ~GeminiJob() override
    {
    }

    virtual void start() override;
    virtual void shutdown() override;
    void set_certificate(String certificate, String key);

    Function<void(GeminiJob&)> on_certificate_requested;

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
