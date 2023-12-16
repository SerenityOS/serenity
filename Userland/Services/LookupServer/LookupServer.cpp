/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LookupServer.h"
#include "ConnectionFromClient.h"
#include <AK/BufferedStream.h>
#include <AK/ByteString.h>
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/Random.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>
#include <LibDNS/Packet.h>
#include <limits.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

namespace LookupServer {

static LookupServer* s_the;
// NOTE: This is the TTL we return for the hostname or answers from /etc/hosts.
static constexpr u32 s_static_ttl = 86400;

LookupServer& LookupServer::the()
{
    VERIFY(s_the);
    return *s_the;
}

LookupServer::LookupServer()
{
    VERIFY(s_the == nullptr);
    s_the = this;

    auto config = Core::ConfigFile::open_for_system("LookupServer").release_value_but_fixme_should_propagate_errors();
    dbgln("Using network config file at {}", config->filename());
    m_nameservers = config->read_entry("DNS", "Nameservers", "1.1.1.1,1.0.0.1").split(',');

    load_etc_hosts();

    auto maybe_file_watcher = Core::FileWatcher::create();
    // NOTE: If this happens during startup, something is very wrong.
    if (maybe_file_watcher.is_error()) {
        dbgln("Core::FileWatcher::create(): {}", maybe_file_watcher.error());
        VERIFY_NOT_REACHED();
    }
    m_file_watcher = maybe_file_watcher.release_value();

    m_file_watcher->on_change = [this](auto&) {
        dbgln("Reloading '/etc/hosts' because it was changed.");
        load_etc_hosts();
    };

    auto result = m_file_watcher->add_watch("/etc/hosts", Core::FileWatcherEvent::Type::ContentModified | Core::FileWatcherEvent::Type::Deleted);
    // NOTE: If this happens during startup, something is very wrong.
    if (result.is_error()) {
        dbgln("Core::FileWatcher::add_watch(): {}", result.error());
        VERIFY_NOT_REACHED();
    } else if (!result.value()) {
        dbgln("Core::FileWatcher::add_watch(): {}", result.value());
        VERIFY_NOT_REACHED();
    }

    if (config->read_bool_entry("DNS", "EnableServer")) {
        m_dns_server = DNSServer::construct(this);
        // TODO: drop root privileges here.
    }
    m_mdns = MulticastDNS::construct(this);

    m_server = MUST(IPC::MultiServer<ConnectionFromClient>::try_create());
}

void LookupServer::load_etc_hosts()
{
    auto new_hosts_or_error = this->try_load_etc_hosts();
    if (new_hosts_or_error.is_error())
        dbgln("Ignoring '/etc/hosts', keeping old values");
    else
        m_etc_hosts = new_hosts_or_error.release_value();
}

ErrorOr<HashMap<Name, Vector<Answer>, Name::Traits>> LookupServer::try_load_etc_hosts()
{
    HashMap<Name, Vector<Answer>, Name::Traits> map;
    auto add_answer = [&map](Name const& name, RecordType record_type, ByteString data) -> ErrorOr<void> {
        // FIXME: Since try_ensure does not return a reference to the contained value, we have to
        // retrieve it separately. This is a try_ensure bug that should be fixed.
        TRY(map.try_ensure(name, []() { return Vector<Answer> {}; }));
        auto& entry = map.find(name)->value;
        return entry.try_empend(name, record_type, RecordClass::IN, s_static_ttl, move(data), false);
    };

    auto file_or_error = Core::File::open("/etc/hosts"sv, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        dbgln("Failed to open '/etc/hosts'");
        return file_or_error.release_error();
    }
    auto file = TRY(Core::InputBufferedFile::create(file_or_error.release_value()));
    auto buffer = TRY(ByteBuffer::create_uninitialized(1 * KiB));

    u32 line_number = 0;
    while (TRY(file->can_read_line())) {
        auto original_line = TRY(file->read_line(buffer));
        ++line_number;
        if (original_line.is_empty())
            break;
        auto trimmed_line = original_line.trim_whitespace();
        auto replaced_line = trimmed_line.replace(" "sv, "\t"sv, ReplaceMode::All);
        auto fields = replaced_line.split_view('\t');

        if (fields.size() < 2) {
            dbgln("Failed to parse line {} from '/etc/hosts': '{}'", line_number, original_line);
            continue;
        }

        if (fields.size() > 2)
            dbgln("Line {} from '/etc/hosts' ('{}') has more than two parts, only the first two are used.", line_number, original_line);

        auto maybe_address = IPv4Address::from_string(fields[0]);
        if (!maybe_address.has_value()) {
            dbgln("Failed to parse line {} from '/etc/hosts': '{}'", line_number, original_line);
            continue;
        }

        auto raw_addr = maybe_address->to_in_addr_t();

        Name name { fields[1] };
        TRY(add_answer(name, RecordType::A, ByteString { (char const*)&raw_addr, sizeof(raw_addr) }));

        StringBuilder builder;
        TRY(builder.try_append(maybe_address->to_byte_string_reversed()));
        TRY(builder.try_append(".in-addr.arpa"sv));
        TRY(add_answer(builder.to_byte_string(), RecordType::PTR, name.as_string()));
    }

    return map;
}

static ByteString get_hostname()
{
    char buffer[_POSIX_HOST_NAME_MAX];
    VERIFY(gethostname(buffer, sizeof(buffer)) == 0);
    return buffer;
}

ErrorOr<Vector<Answer>> LookupServer::lookup(Name const& name, RecordType record_type)
{
    dbgln_if(LOOKUPSERVER_DEBUG, "Got request for '{}'", name.as_string());

    Vector<Answer> answers;
    auto add_answer = [&](Answer const& answer) {
        Answer answer_with_original_case {
            name,
            answer.type(),
            answer.class_code(),
            answer.ttl(),
            answer.record_data(),
            answer.mdns_cache_flush(),
        };
        answers.append(answer_with_original_case);
    };

    // First, try /etc/hosts.
    if (auto local_answers = m_etc_hosts.find(name); local_answers != m_etc_hosts.end()) {
        for (auto& answer : local_answers->value) {
            if (answer.type() == record_type)
                add_answer(answer);
        }
        if (!answers.is_empty())
            return answers;
    }

    // Second, try the hostname.
    // NOTE: We don't cache the hostname since it could change during runtime.
    if (record_type == RecordType::A && get_hostname() == name) {
        IPv4Address address = { 127, 0, 0, 1 };
        auto raw_address = address.to_in_addr_t();
        Answer answer { name, RecordType::A, RecordClass::IN, s_static_ttl, ByteString { (char const*)&raw_address, sizeof(raw_address) }, false };
        answers.append(move(answer));
        return answers;
    }

    // Third, try our cache.
    if (auto cached_answers = m_lookup_cache.find(name); cached_answers != m_lookup_cache.end()) {
        for (auto& answer : cached_answers->value) {
            // TODO: Actually remove expired answers from the cache.
            if (answer.type() == record_type && !answer.has_expired()) {
                dbgln_if(LOOKUPSERVER_DEBUG, "Cache hit: {} -> {}", name.as_string(), answer.record_data());
                add_answer(answer);
            }
        }
        if (!answers.is_empty())
            return answers;
    }

    // Fourth, look up .local names using mDNS instead of DNS nameservers.
    if (name.as_string().ends_with(".local"sv)) {
        answers = TRY(m_mdns->lookup(name, record_type));
        for (auto& answer : answers)
            put_in_cache(answer);
        return answers;
    }

    // Fifth, ask the upstream nameservers.
    for (auto& nameserver : m_nameservers) {
        dbgln_if(LOOKUPSERVER_DEBUG, "Doing lookup using nameserver '{}'", nameserver);
        bool did_get_response = false;
        int retries = 3;
        Vector<Answer> upstream_answers;
        do {
            auto upstream_answers_or_error = lookup(name, nameserver, did_get_response, record_type);
            if (upstream_answers_or_error.is_error())
                continue;
            upstream_answers = upstream_answers_or_error.release_value();
            if (did_get_response)
                break;
        } while (--retries);
        if (!upstream_answers.is_empty()) {
            for (auto& answer : upstream_answers) {
                add_answer(answer);
                put_in_cache(answer);
            }
            break;
        } else {
            if (!did_get_response)
                dbgln("Never got a response from '{}', trying next nameserver", nameserver);
            else
                dbgln("Received response from '{}' but no result(s), trying next nameserver", nameserver);
        }
    }

    // Sixth, fail.
    if (answers.is_empty()) {
        dbgln("Tried all nameservers but never got a response :(");
        return Vector<Answer> {};
    }

    return answers;
}

ErrorOr<Vector<Answer>> LookupServer::lookup(Name const& name, ByteString const& nameserver, bool& did_get_response, RecordType record_type, ShouldRandomizeCase should_randomize_case)
{
    Packet request;
    request.set_is_query();
    request.set_id(get_random_uniform(UINT16_MAX));
    Name name_in_question = name;
    if (should_randomize_case == ShouldRandomizeCase::Yes)
        name_in_question.randomize_case();
    request.add_question({ name_in_question, record_type, RecordClass::IN, false });

    auto buffer = TRY(request.to_byte_buffer());

    auto udp_socket = TRY(Core::UDPSocket::connect(nameserver, 53, Duration::from_seconds(1)));
    TRY(udp_socket->set_blocking(true));

    TRY(udp_socket->write_until_depleted(buffer));

    u8 response_buffer[4096];
    auto nrecv = TRY(udp_socket->read_some({ response_buffer, sizeof(response_buffer) })).size();
    if (udp_socket->is_eof())
        return Vector<Answer> {};

    did_get_response = true;

    auto response_or_error = Packet::from_raw_packet({ response_buffer, nrecv });
    if (response_or_error.is_error())
        return Vector<Answer> {};

    auto response = response_or_error.release_value();

    if (response.id() != request.id()) {
        dbgln("LookupServer: ID mismatch ({} vs {}) :(", response.id(), request.id());
        return Vector<Answer> {};
    }

    if (response.code() == Packet::Code::REFUSED) {
        if (should_randomize_case == ShouldRandomizeCase::Yes) {
            // Retry with 0x20 case randomization turned off.
            return lookup(name, nameserver, did_get_response, record_type, ShouldRandomizeCase::No);
        }
        return Vector<Answer> {};
    }

    if (response.question_count() != request.question_count()) {
        dbgln("LookupServer: Question count ({} vs {}) :(", response.question_count(), request.question_count());
        return Vector<Answer> {};
    }

    // Verify the questions in our request and in their response match, ignoring case.
    for (size_t i = 0; i < request.question_count(); ++i) {
        auto& request_question = request.questions()[i];
        auto& response_question = response.questions()[i];
        bool match = request_question.class_code() == response_question.class_code()
            && request_question.record_type() == response_question.record_type()
            && request_question.name().as_string().equals_ignoring_ascii_case(response_question.name().as_string());
        if (!match) {
            dbgln("Request and response questions do not match");
            dbgln("   Request: name=_{}_, type={}, class={}", request_question.name().as_string(), response_question.record_type(), response_question.class_code());
            dbgln("  Response: name=_{}_, type={}, class={}", response_question.name().as_string(), response_question.record_type(), response_question.class_code());
            return Vector<Answer> {};
        }
    }

    if (response.answer_count() < 1) {
        dbgln("LookupServer: No answers :(");
        return Vector<Answer> {};
    }

    Vector<Answer, 8> answers;
    for (auto& answer : response.answers()) {
        put_in_cache(answer);
        if (answer.type() != record_type)
            continue;
        answers.append(answer);
    }

    return answers;
}

void LookupServer::put_in_cache(Answer const& answer)
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

            it->value.remove_all_matching([&](Answer const& other_answer) {
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
