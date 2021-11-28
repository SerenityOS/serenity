/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/TCPSocket.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/Job.h>

namespace HTTP {

class HttpJob final : public Job {
    C_OBJECT(HttpJob)
public:
    virtual ~HttpJob() override
    {
    }

    virtual void start(NonnullRefPtr<Core::Socket>) override;
    virtual void shutdown(ShutdownMode) override;

    Core::Socket const* socket() const { return m_socket; }
    URL url() const { return m_request.url(); }

protected:
    virtual bool should_fail_on_empty_payload() const override { return false; }
    virtual void register_on_ready_to_read(Function<void()>) override;
    virtual void register_on_ready_to_write(Function<void()>) override;
    virtual bool can_read_line() const override;
    virtual String read_line(size_t) override;
    virtual bool can_read() const override;
    virtual ByteBuffer receive(size_t) override;
    virtual bool eof() const override;
    virtual bool write(ReadonlyBytes) override;
    virtual bool is_established() const override { return true; }

private:
    explicit HttpJob(HttpRequest&& request, OutputStream& output_stream)
        : Job(move(request), output_stream)
    {
    }

    RefPtr<Core::Socket> m_socket;
};

}
