/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibCore/TCPSocket.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/Job.h>

namespace HTTP {

class HttpJob final : public Job {
    C_OBJECT(HttpJob)
public:
    explicit HttpJob(const HttpRequest& request)
        : Job(request)
    {
    }

    virtual ~HttpJob() override
    {
    }

    virtual void start() override;
    virtual void shutdown() override;

protected:
    virtual bool should_fail_on_empty_payload() const override { return false; }
    virtual void register_on_ready_to_read(Function<void()>) override;
    virtual void register_on_ready_to_write(Function<void()>) override;
    virtual bool can_read_line() const override;
    virtual ByteBuffer read_line(size_t) override;
    virtual bool can_read() const override;
    virtual ByteBuffer receive(size_t) override;
    virtual bool eof() const override;
    virtual bool write(const ByteBuffer&) override;
    virtual bool is_established() const override { return true; }

private:
    RefPtr<Core::Socket> m_socket;
};

}
