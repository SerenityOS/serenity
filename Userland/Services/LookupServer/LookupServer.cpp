/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LookupServer.h"
#include "ClientConnection.h"
#include "DNSPacket.h"
#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/Random.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/UDPSocket.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

namespace LookupServer {

static LookupServer* s_the;

LookupServer& LookupServer::the()
{
    VERIFY(s_the);
    return *s_the;
}

LookupServer::LookupServer()
{
    VERIFY(s_the == nullptr);
    s_the = this;

    auto config = Core::ConfigFile::get_for_system("LookupServer");
    dbgln("Using network config file at {}", config->filename());
    m_nameservers = config->read_entry("DNS", "Nameservers", "1.1.1.1,1.0.0.1").split(',');

    load_etc_hosts();

    if (config->read_bool_entry("DNS", "EnableServer")) {
        m_dns_server = DNSServer::construct(this);
        // TODO: drop root privileges here.
    }
    m_mdns = MulticastDNS::construct(this);

    m_local_server = Core::LocalServer::construct(this);
    m_local_server->on_ready_to_accept = [this]() {
        auto socket = m_local_server->accept();
        if (!socket) {
            dbgln("Failed to accept a client connection");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<ClientConnection>(socket.release_nonnull(), client_id);
    };
    bool ok = m_local_server->take_over_from_system_server();
    VERIFY(ok);
}

void LookupServer::load_etc_hosts()
{
    // The TTL we return for static data from /etc/hosts.
    // The value here is 1 day.
    static constexpr u32 static_ttl = 86400;

    auto add_answer = [this](const DNSName& name, DNSRecordType record_type, String data) {
        auto it = m_etc_hosts.find(name);
        if (it == m_etc_hosts.end()) {
            m_etc_hosts.set(name, {});
            it = m_etc_hosts.find(name);
        }
        it->value.empend(name, record_type, DNSRecordClass::IN, static_ttl, data, false);
    };

    auto file = Core::File::construct("/etc/hosts");
    if (!file->open(Core::OpenMode::ReadOnly))
        return;
    while (!file->eof()) {
        auto line = file->read_line(1024);
        if (line.is_empty())
            break;
        auto fields = line.split('\t');

        auto sections = fields[0].split('.');
        IPv4Address addr {
            (u8)atoi(sections[0].characters()),
            (u8)atoi(sections[1].characters()),
            (u8)atoi(sections[2].characters()),
            (u8)atoi(sections[3].characters()),
        };
        auto raw_addr = addr.to_in_addr_t();

        DNSName name = fields[1];
        add_answer(name, DNSRecordType::A, String { (const char*)&raw_addr, sizeof(raw_addr) });

        IPv4Address reverse_addr {
            (u8)atoi(sections[3].characters()),
            (u8)atoi(sections[2].characters()),
            (u8)atoi(sections[1].characters()),
            (u8)atoi(sections[0].characters()),
        };
        StringBuilder builder;
        builder.append(reverse_addr.to_string());
        builder.append(".in-addr.arpa");
        add_answer(builder.to_string(), DNSRecordType::PTR, name.as_string());
    }
}

Vector<DNSAnswer> LookupServer::lookup(const DNSName& name, DNSRecordType record_type)
{
    dbgln_if(LOOKUPSERVER_DEBUG, "Got request for '{}'", name.as_string());

    Vector<DNSAnswer> answers;
    auto add_answer = [&](const DNSAnswer& answer) {
        DNSAnswer answer_with_original_case {
            name,
            answer.type(),
            answer.class_code(),
            answer.ttl(),
            answer.record_data(),
            answer.mdns_cache_flush(),
        };
        answers.append(answer_with_original_case);
    };

    // First, try local data.
    if (auto local_answers = m_etc_hosts.get(name); local_answers.has_value()) {
        for (auto& answer : local_answers.value()) {
            if (answer.type() == record_type)
                add_answer(answer);
        }
        if (!answers.is_empty())
            return answers;
    }

    // Second, try our cache.
    if (auto cached_answers = m_lookup_cache.get(name); cached_answers.has_value()) {
        for (auto& answer : cached_answers.value()) {
            // TODO: Actually remove expired answers from the cache.
            if (answer.type() == record_type && !answer.has_expired()) {
                dbgln_if(LOOKUPSERVER_DEBUG, "Cache hit: {} -> {}", name.as_string(), answer.record_data());
                add_answer(answer);
            }
        }
        if (!answers.is_empty())
            return answers;
    }

    // Look up .local names using mDNS instead of DNS nameservers.
    if (name.as_string().ends_with(".local")) {
        answers = m_mdns->lookup(name, record_type);
        for (auto& answer : answers)
            put_in_cache(answer);
        return answers;
    }

    // Third, ask the upstream nameservers.
    for (auto& nameserver : m_nameservers) {
        dbgln_if(LOOKUPSERVER_DEBUG, "Doing lookup using nameserver '{}'", nameserver);
        bool did_get_response = false;
        int retries = 3;
        Vector<DNSAnswer> upstream_answers;
        do {
            upstream_answers = lookup(name, nameserver, did_get_response, record_type);
            if (did_get_response)
                break;
        } while (--retries);
        if (!upstream_answers.is_empty()) {
            for (auto& answer : upstream_answers)
                add_answer(answer);
            break;
        } else {
            if (!did_get_response)
                dbgln("Never got a response from '{}', trying next nameserver", nameserver);
            else
                dbgln("Received response from '{}' but no result(s), trying next nameserver", nameserver);
        }
    }
    if (answers.is_empty()) {
        dbgln("Tried all nameservers but never got a response :(");
        return {};
    }

    return answers;
}

Vector<DNSAnswer> LookupServer::lookup(const DNSName& name, const String& nameserver, bool& did_get_response, DNSRecordType record_type, ShouldRandomizeCase should_randomize_case)
{
    DNSPacket request;
    request.set_is_query();
    request.set_id(get_random_uniform(UINT16_MAX));
    DNSName name_in_question = name;
    if (should_randomize_case == ShouldRandomizeCase::Yes)
        name_in_question.randomize_case();
    request.add_question({ name_in_question, record_type, DNSRecordClass::IN, false });

    auto buffer = request.to_byte_buffer();

    auto udp_socket = Core::UDPSocket::construct();
    udp_socket->set_blocking(true);

    struct timeval timeout {
        1, 0
    };

    int rc = setsockopt(udp_socket->fd(), SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (rc < 0) {
        perror("setsockopt(SOL_SOCKET, SO_RCVTIMEO)");
        return {};
    }

    if (!udp_socket->connect(nameserver, 53))
        return {};

    if (!udp_socket->write(buffer))
        return {};

    u8 response_buffer[4096];
    int nrecv = udp_socket->read(response_buffer, sizeof(response_buffer));
    if (nrecv == 0)
        return {};

    did_get_response = true;

    auto o_response = DNSPacket::from_raw_packet(response_buffer, nrecv);
    if (!o_response.has_value())
        return {};

    auto& response = o_response.value();

    if (response.id() != request.id()) {
        dbgln("LookupServer: ID mismatch ({} vs {}) :(", response.id(), request.id());
        return {};
    }

    if (response.code() == DNSPacket::Code::REFUSED) {
        if (should_randomize_case == ShouldRandomizeCase::Yes) {
            // Retry with 0x20 case randomization turned off.
            return lookup(name, nameserver, did_get_response, record_type, ShouldRandomizeCase::No);
        }
        return {};
    }

    if (response.question_count() != request.question_count()) {
        dbgln("LookupServer: Question count ({} vs {}) :(", response.question_count(), request.question_count());
        return {};
    }

    // Verify the questions in our request and in their response match exactly, including case.
    for (size_t i = 0; i < request.question_count(); ++i) {
        auto& request_question = request.questions()[i];
        auto& response_question = response.questions()[i];
        bool exact_match = request_question.class_code() == response_question.class_code()
            && request_question.record_type() == response_question.record_type()
            && request_question.name().as_string() == response_question.name().as_string();
        if (!exact_match) {
            dbgln("Request and response questions do not match");
            dbgln("   Request: name=_{}_, type={}, class={}", request_question.name().as_string(), response_question.record_type(), response_question.class_code());
            dbgln("  Response: name=_{}_, type={}, class={}", response_question.name().as_string(), response_question.record_type(), response_question.class_code());
            return {};
        }
    }

    if (response.answer_count() < 1) {
        dbgln("LookupServer: No answers :(");
        return {};
    }

    Vector<DNSAnswer, 8> answers;
    for (auto& answer : response.answers()) {
        put_in_cache(answer);
        if (answer.type() != record_type)
            continue;
        answers.append(answer);
    }

    return answers;
}

void LookupServer::put_in_cache(const DNSAnswer& answer)
{
    if (answer.has_expired())
        return;

    // Prevent the cache from growing too big.
    // TODO: Evict least used entries.
    if (m_lookup_cache.size() >= 256)
        m_lookup_cache.remove(m_lookup_cache.begin());

    auto it = m_lookup_cache.find(answer.name());
    if (it == m_lookup_cache.end())
        m_lookup_cache.set(answer.name(), { answer });
    else {
        if (answer.mdns_cache_flush()) {
            auto now = time(nullptr);

            it->value.remove_all_matching([&](DNSAnswer const& other_answer) {
                if (other_answer.type() != answer.type() || other_answer.class_code() != answer.class_code())
                    return false;

                if (other_answer.received_time() >= now - 1)
                    return false;

                dbgln_if(LOOKUPSERVER_DEBUG, "Removing cache entry: {}", other_answer.name());
                return true;
            });
        }
        it->value.append(answer);
    }
}

}
