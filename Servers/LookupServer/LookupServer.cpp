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

#include "LookupServer.h"
#include "DNSRequest.h"
#include "DNSResponse.h"
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CFile.h>
#include <LibCore/CLocalServer.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CUdpSocket.h>
#include <stdio.h>
#include <unistd.h>

LookupServer::LookupServer()
{
    auto config = Core::ConfigFile::get_for_system("LookupServer");
    dbg() << "Using network config file at " << config->file_name();
    m_nameserver = config->read_entry("DNS", "Nameserver", "1.1.1.1");

    load_etc_hosts();

    m_local_server = Core::LocalServer::construct(this);
    m_local_server->on_ready_to_accept = [this]() {
        auto socket = m_local_server->accept();
        socket->on_ready_to_read = [this, socket]() {
            service_client(socket);
            RefPtr<Core::LocalSocket> keeper = socket;
            const_cast<Core::LocalSocket&>(*socket).on_ready_to_read = [] {};
        };
    };
    bool ok = m_local_server->take_over_from_system_server();
    ASSERT(ok);
}

void LookupServer::load_etc_hosts()
{
    auto file = Core::File::construct("/etc/hosts");
    if (!file->open(Core::IODevice::ReadOnly))
        return;
    while (!file->eof()) {
        auto line = file->read_line(1024);
        if (line.is_empty())
            break;
        auto str_line = String((const char*)line.data(), line.size() - 1, Chomp);
        auto fields = str_line.split('\t');

        auto sections = fields[0].split('.');
        IPv4Address addr {
            (u8)atoi(sections[0].characters()),
            (u8)atoi(sections[1].characters()),
            (u8)atoi(sections[2].characters()),
            (u8)atoi(sections[3].characters()),
        };

        auto name = fields[1];
        m_etc_hosts.set(name, addr.to_string());

        IPv4Address reverse_addr {
            (u8)atoi(sections[3].characters()),
            (u8)atoi(sections[2].characters()),
            (u8)atoi(sections[1].characters()),
            (u8)atoi(sections[0].characters()),
        };
        StringBuilder builder;
        builder.append(reverse_addr.to_string());
        builder.append(".in-addr.arpa");
        m_etc_hosts.set(builder.to_string(), name);
    }
}

void LookupServer::service_client(RefPtr<Core::LocalSocket> socket)
{
    u8 client_buffer[1024];
    int nrecv = socket->read(client_buffer, sizeof(client_buffer) - 1);
    if (nrecv < 0) {
        perror("read");
        return;
    }

    client_buffer[nrecv] = '\0';

    char lookup_type = client_buffer[0];
    if (lookup_type != 'L' && lookup_type != 'R') {
        dbg() << "Invalid lookup_type " << lookup_type;
        return;
    }
    auto hostname = String((const char*)client_buffer + 1, nrecv - 1, Chomp);
    dbg() << "Got request for '" << hostname << "' (using IP " << m_nameserver << ")";

    Vector<String> responses;

    if (auto known_host = m_etc_hosts.get(hostname)) {
        responses.append(known_host.value());
    } else if (!hostname.is_empty()) {
        bool did_timeout;
        int retries = 3;
        do {
            did_timeout = false;
            if (lookup_type == 'L')
                responses = lookup(hostname, did_timeout, T_A);
            else if (lookup_type == 'R')
                responses = lookup(hostname, did_timeout, T_PTR);
            if (!did_timeout)
                break;
        } while (--retries);
        if (did_timeout) {
            fprintf(stderr, "LookupServer: Out of retries :(\n");
            return;
        }
    }

    if (responses.is_empty()) {
        int nsent = socket->write("Not found.\n");
        if (nsent < 0)
            perror("write");
        return;
    }
    for (auto& response : responses) {
        auto line = String::format("%s\n", response.characters());
        int nsent = socket->write(line);
        if (nsent < 0) {
            perror("write");
            break;
        }
    }
}

Vector<String> LookupServer::lookup(const String& hostname, bool& did_timeout, unsigned short record_type, ShouldRandomizeCase should_randomize_case)
{
    if (auto it = m_lookup_cache.find(hostname); it != m_lookup_cache.end()) {
        auto& cached_lookup = it->value;
        if (cached_lookup.question.record_type() == record_type) {
            Vector<String> responses;
            for (auto& cached_answer : cached_lookup.answers) {
                dbg() << "Cache hit: " << hostname << " -> " << cached_answer.record_data() << ", expired: " << cached_answer.has_expired();
                if (!cached_answer.has_expired()) {
                    responses.append(cached_answer.record_data());
                }
            }
            if (!responses.is_empty())
                return responses;
        }
        m_lookup_cache.remove(it);
    }

    DNSRequest request;
    request.add_question(hostname, record_type, should_randomize_case);

    auto buffer = request.to_byte_buffer();

    auto udp_socket = Core::UdpSocket::construct();
    udp_socket->set_blocking(true);

    struct timeval timeout {
        1, 0
    };

    int rc = setsockopt(udp_socket->fd(), SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (rc < 0) {
        perror("setsockopt(SOL_SOCKET, SO_RCVTIMEO)");
        return {};
    }

    if (!udp_socket->connect(m_nameserver, 53))
        return {};

    if (!udp_socket->write(buffer))
        return {};

    u8 response_buffer[4096];
    int nrecv = udp_socket->read(response_buffer, sizeof(response_buffer));
    if (nrecv == 0)
        return {};

    auto o_response = DNSResponse::from_raw_response(response_buffer, nrecv);
    if (!o_response.has_value())
        return {};

    auto& response = o_response.value();

    if (response.id() != request.id()) {
        dbgprintf("LookupServer: ID mismatch (%u vs %u) :(\n", response.id(), request.id());
        return {};
    }

    if (response.code() == DNSResponse::Code::REFUSED) {
        if (should_randomize_case == ShouldRandomizeCase::Yes) {
            // Retry with 0x20 case randomization turned off.
            return lookup(hostname, did_timeout, record_type, ShouldRandomizeCase::No);
        }
        return {};
    }

    if (response.question_count() != request.question_count()) {
        dbgprintf("LookupServer: Question count (%u vs %u) :(\n", response.question_count(), request.question_count());
        return {};
    }

    for (size_t i = 0; i < request.question_count(); ++i) {
        auto& request_question = request.questions()[i];
        auto& response_question = response.questions()[i];
        if (request_question != response_question) {
            dbg() << "Request and response questions do not match";
            dbg() << "   Request: {_" << request_question.name() << "_, " << request_question.record_type() << ", " << request_question.class_code() << "}";
            dbg() << "  Response: {_" << response_question.name() << "_, " << response_question.record_type() << ", " << response_question.class_code() << "}";
            return {};
        }
    }

    if (response.answer_count() < 1) {
        dbgprintf("LookupServer: Not enough answers (%u) :(\n", response.answer_count());
        return {};
    }

    Vector<String, 8> responses;
    Vector<DNSAnswer, 8> cacheable_answers;
    for (auto& answer : response.answers()) {
        responses.append(answer.record_data());
        if (!answer.has_expired())
            cacheable_answers.append(answer);
    }

    if (!cacheable_answers.is_empty()) {
        if (m_lookup_cache.size() >= 256)
            m_lookup_cache.remove(m_lookup_cache.begin());
        m_lookup_cache.set(hostname, { request.questions()[0], move(cacheable_answers) });
    }
    return responses;
}
