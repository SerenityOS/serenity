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

#include <AK/Debug.h>
#include <LibGemini/GeminiResponse.h>
#include <LibGemini/Job.h>
#include <stdio.h>
#include <unistd.h>

namespace Gemini {

Job::Job(const GeminiRequest& request, OutputStream& output_stream)
    : Core::NetworkJob(output_stream)
    , m_request(request)
{
}

Job::~Job()
{
}

void Job::flush_received_buffers()
{
    for (size_t i = 0; i < m_received_buffers.size(); ++i) {
        auto& payload = m_received_buffers[i];
        auto written = do_write(payload);
        m_received_size -= written;
        if (written == payload.size()) {
            // FIXME: Make this a take-first-friendly object?
            m_received_buffers.take_first();
            continue;
        }
        ASSERT(written < payload.size());
        payload = payload.slice(written, payload.size() - written);
        return;
    }
}

void Job::on_socket_connected()
{
    register_on_ready_to_write([this] {
        if (m_sent_data)
            return;
        m_sent_data = true;
        auto raw_request = m_request.to_raw_request();

        if constexpr (debug_job) {
            dbgln("Job: raw_request:");
            dbgln("{}", String::copy(raw_request));
        }
        bool success = write(raw_request);
        if (!success)
            deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
    });
    register_on_ready_to_read([this] {
        if (is_cancelled())
            return;

        if (m_state == State::InStatus) {
            if (!can_read_line())
                return;

            auto line = read_line(PAGE_SIZE);
            if (line.is_null()) {
                fprintf(stderr, "Job: Expected status line\n");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }

            auto parts = line.split_limit(' ', 2);
            if (parts.size() != 2) {
                warnln("Job: Expected 2-part status line, got '{}'", line);
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            auto status = parts[0].to_uint();
            if (!status.has_value()) {
                fprintf(stderr, "Job: Expected numeric status code\n");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            m_status = status.value();
            m_meta = parts[1];

            if (m_status >= 10 && m_status < 20) {
                m_state = State::Finished;
            } else if (m_status >= 20 && m_status < 30) {
                m_state = State::InBody;
            } else if (m_status >= 30 && m_status < 40) {
                m_state = State::Finished;
            } else if (m_status >= 40 && m_status < 50) {
                m_state = State::Finished;
            } else if (m_status >= 50 && m_status < 60) {
                m_state = State::Finished;
            } else if (m_status >= 60 && m_status < 70) {
                m_state = State::InBody;
            } else {
                fprintf(stderr, "Job: Expected status between 10 and 69; instead got %d\n", m_status);
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            return;
        }

        ASSERT(m_state == State::InBody || m_state == State::Finished);

        read_while_data_available([&] {
            auto read_size = 64 * KiB;

            auto payload = receive(read_size);
            if (!payload) {
                if (eof()) {
                    finish_up();
                    return IterationDecision::Break;
                }

                if (should_fail_on_empty_payload()) {
                    deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
                    return IterationDecision::Break;
                }
            }

            m_received_buffers.append(payload);
            m_received_size += payload.size();
            flush_received_buffers();

            deferred_invoke([this](auto&) { did_progress({}, m_received_size); });

            return IterationDecision::Continue;
        });

        if (!is_established()) {
#if JOB_DEBUG
            dbgln("Connection appears to have closed, finishing up");
#endif
            finish_up();
        }
    });
}

void Job::finish_up()
{
    m_state = State::Finished;
    flush_received_buffers();
    if (m_received_size != 0) {
        // We have to wait for the client to consume all the downloaded data
        // before we can actually call `did_finish`. in a normal flow, this should
        // never be hit since the client is reading as we are writing, unless there
        // are too many concurrent downloads going on.
        deferred_invoke([this](auto&) {
            finish_up();
        });
        return;
    }

    auto response = GeminiResponse::create(m_status, m_meta);
    deferred_invoke([this, response](auto&) {
        did_finish(move(response));
    });
}
}
