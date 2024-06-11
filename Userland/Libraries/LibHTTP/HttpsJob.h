/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    virtual ~HttpsJob() override
    {
    }

    bool received_client_certificates() const { return m_received_client_certificates.has_value(); }
    Vector<TLS::Certificate> take_client_certificates() const { return m_received_client_certificates.release_value(); }

    void set_certificate(ByteString certificate, ByteString key);

    Function<Vector<TLS::Certificate>()> on_certificate_requested;

private:
    explicit HttpsJob(HttpRequest&& request, Core::File& output_stream)
        : Job(move(request), output_stream)
    {
    }

    mutable Optional<Vector<TLS::Certificate>> m_received_client_certificates;
};

}
