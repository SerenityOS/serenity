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

#include <AK/Function.h>
#include <LibCore/Object.h>

namespace Core {

class NetworkJob : public Object {
    C_OBJECT_ABSTRACT(NetworkJob)
public:
    enum class Error {
        None,
        ConnectionFailed,
        TransmissionFailed,
        ProtocolFailed,
        Cancelled,
    };
    virtual ~NetworkJob() override;

    Function<void(bool success)> on_finish;
    Function<void(Optional<u32>, u32)> on_progress;

    bool is_cancelled() const { return m_error == Error::Cancelled; }
    bool has_error() const { return m_error != Error::None; }
    Error error() const { return m_error; }
    NetworkResponse* response() { return m_response.ptr(); }
    const NetworkResponse* response() const { return m_response.ptr(); }

    virtual void start() = 0;
    virtual void shutdown() = 0;

    void cancel()
    {
        shutdown();
        m_error = Error::Cancelled;
    }

protected:
    NetworkJob();
    void did_finish(NonnullRefPtr<NetworkResponse>&&);
    void did_fail(Error);
    void did_progress(Optional<u32> total_size, u32 downloaded);

private:
    RefPtr<NetworkResponse> m_response;
    Error m_error { Error::None };
};

const char* to_string(NetworkJob::Error);

}
