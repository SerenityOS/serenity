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
#include <Kernel/Net/IPv4.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CFile.h>
#include <LibCore/CLocalServer.h>
#include <LibCore/CLocalSocket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

LookupServer::LookupServer()
{
    auto config = CConfigFile::get_for_system("LookupServer");
    dbg() << "Using network config file at " << config->file_name();
    m_dns_ip = config->read_entry("DNS", "IPAddress", "127.0.0.53");

    load_etc_hosts();

    m_local_server = CLocalServer::construct(this);
    m_local_server->on_ready_to_accept = [this]() {
        auto socket = m_local_server->accept();
        socket->on_ready_to_read = [this, socket]() {
            service_client(socket);
            RefPtr<CLocalSocket> keeper = socket;
            const_cast<CLocalSocket&>(*socket).on_ready_to_read = [] {};
        };
    };
    bool ok = m_local_server->take_over_from_system_server();
    ASSERT(ok);
}

void LookupServer::load_etc_hosts()
{
    dbg() << "Loading hosts from /etc/hosts";
    auto file = CFile::construct("/etc/hosts");
    if (!file->open(CIODevice::ReadOnly))
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
        m_dns_custom_hostnames.set(name, addr.to_string());

        IPv4Address reverse_addr {
            (u8)atoi(sections[3].characters()),
            (u8)atoi(sections[2].characters()),
            (u8)atoi(sections[1].characters()),
            (u8)atoi(sections[0].characters()),
        };
        StringBuilder builder;
        builder.append(reverse_addr.to_string());
        builder.append(".in-addr.arpa");
        m_dns_custom_hostnames.set(builder.to_string(), name);
    }
}

void LookupServer::service_client(RefPtr<CLocalSocket> socket)
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
    dbg() << "Got request for '" << hostname << "' (using IP " << m_dns_ip << ")";

    Vector<String> responses;

    if (auto known_host = m_dns_custom_hostnames.get(hostname)) {
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

Vector<String> LookupServer::lookup(const String& hostname, bool& did_timeout, unsigned short record_type)
{
    if (auto it = m_lookup_cache.find(hostname); it != m_lookup_cache.end()) {
        auto& cached_lookup = it->value;
        if (cached_lookup.record_type == record_type && cached_lookup.timestamp < (time(nullptr) + 60)) {
            return it->value.responses;
        }
        m_lookup_cache.remove(it);
    }

    DNSRequest request;
    request.add_question(hostname, record_type);

    auto buffer = request.to_byte_buffer();

    struct sockaddr_in dst_addr;

    int fd = make_dns_request_socket(dst_addr);
    if (fd < 0)
        return {};

    int nsent = sendto(fd, buffer.data(), buffer.size(), 0, (const struct sockaddr*)&dst_addr, sizeof(dst_addr));
    if (nsent < 0) {
        perror("sendto");
        return {};
    }
    ASSERT(nsent == buffer.size());

    struct sockaddr_in src_addr;
    socklen_t src_addr_len = sizeof(src_addr);
    u8 response_buffer[4096];
    ssize_t nrecv = recvfrom(fd, response_buffer, sizeof(response_buffer) - 1, 0, (struct sockaddr*)&src_addr, &src_addr_len);
    if (nrecv < 0) {
        if (errno == EAGAIN) {
            did_timeout = true;
        } else {
            perror("recvfrom");
        }
        close(fd);
        return {};
    }
    close(fd);

    response_buffer[nrecv] = '\0';

    auto o_response = DNSResponse::from_raw_response(response_buffer, nrecv);
    if (!o_response.has_value())
        return {};

    auto& response = o_response.value();

    if (response.id() != request.id()) {
        dbgprintf("LookupServer: ID mismatch (%u vs %u) :(\n", response.id(), request.id());
        return {};
    }
    if (response.question_count() != 1) {
        dbgprintf("LookupServer: Question count (%u vs %u) :(\n", response.question_count(), request.question_count());
        return {};
    }
    if (response.answer_count() < 1) {
        dbgprintf("LookupServer: Not enough answers (%u) :(\n", response.answer_count());
        return {};
    }

    Vector<String> addresses;
    for (auto& answer : response.answers()) {
        addresses.append(answer.record_data());
    }

    m_lookup_cache.set(hostname, { time(nullptr), record_type, addresses });
    return addresses;
}

int LookupServer::make_dns_request_socket(sockaddr_in& dst_addr)
{
    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        perror("socket");
        return {};
    }

    struct timeval timeout {
        1, 0
    };
    int rc = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (rc < 0) {
        perror("setsockopt(SOL_SOCKET, SO_RCVTIMEO)");
        close(fd);
        return {};
    }

    memset(&dst_addr, 0, sizeof(dst_addr));

    dst_addr.sin_family = AF_INET;
    dst_addr.sin_port = htons(53);
    rc = inet_pton(AF_INET, m_dns_ip.characters(), &dst_addr.sin_addr);
    if (rc < 0) {
        perror("inet_pton");
        close(fd);
        return rc;
    }

    return fd;
}
