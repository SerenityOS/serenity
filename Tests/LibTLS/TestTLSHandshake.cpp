/*
 * Copyright (c) 2021, Peter Bocan  <me@pbocan.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ConfigFile.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCrypto/ASN1/ASN1.h>
#include <LibTLS/TLSv12.h>
#include <LibTest/TestCase.h>

static const char* ca_certs_file = "./ca_certs.ini";
static int port = 443;

constexpr const char* DEFAULT_SERVER { "www.google.com" };

static ByteBuffer operator""_b(const char* string, size_t length)
{
    return ByteBuffer::copy(string, length).release_value();
}

Vector<Certificate> load_certificates();
String locate_ca_certs_file();

String locate_ca_certs_file()
{
    if (Core::File::exists(ca_certs_file)) {
        return ca_certs_file;
    }
    auto on_target_path = String("/etc/ca_certs.ini");
    if (Core::File::exists(on_target_path)) {
        return on_target_path;
    }
    return "";
}

Vector<Certificate> load_certificates()
{
    Vector<Certificate> certificates;
    auto ca_certs_filepath = locate_ca_certs_file();
    if (ca_certs_filepath == "") {
        warnln("Could not locate ca_certs.ini file.");
        return certificates;
    }

    auto config = Core::ConfigFile::open(ca_certs_filepath);
    auto now = Core::DateTime::now();
    auto last_year = Core::DateTime::create(now.year() - 1);
    auto next_year = Core::DateTime::create(now.year() + 1);
    for (auto& entity : config->groups()) {
        Certificate cert;
        cert.subject.subject = entity;
        cert.issuer.subject = config->read_entry(entity, "issuer_subject", entity);
        cert.subject.country = config->read_entry(entity, "country");
        cert.not_before = Crypto::ASN1::parse_generalized_time(config->read_entry(entity, "not_before", "")).value_or(last_year);
        cert.not_after = Crypto::ASN1::parse_generalized_time(config->read_entry(entity, "not_after", "")).value_or(next_year);
        certificates.append(move(cert));
    }
    return certificates;
}

static Vector<Certificate> s_root_ca_certificates = load_certificates();

TEST_CASE(test_TLS_hello_handshake)
{
    Core::EventLoop loop;
    RefPtr<TLS::TLSv12> tls = TLS::TLSv12::construct(nullptr);
    tls->set_root_certificates(s_root_ca_certificates);
    bool sent_request = false;
    ByteBuffer contents;
    tls->set_on_tls_ready_to_write([&](TLS::TLSv12& tls) {
        if (sent_request)
            return;
        sent_request = true;
        Core::deferred_invoke([&tls] { tls.set_on_tls_ready_to_write(nullptr); });
        if (!tls.write("GET / HTTP/1.1\r\nHost: "_b)) {
            FAIL("write(0) failed");
            loop.quit(0);
        }
        auto* the_server = DEFAULT_SERVER;
        if (!tls.write(StringView(the_server).bytes())) {
            FAIL("write(1) failed");
            loop.quit(0);
        }
        if (!tls.write("\r\nConnection : close\r\n\r\n"_b)) {
            FAIL("write(2) failed");
            loop.quit(0);
        }
    });
    tls->on_tls_ready_to_read = [&](TLS::TLSv12& tls) {
        auto data = tls.read();
        if (!data.has_value()) {
            FAIL("No data received");
            loop.quit(1);
        } else {
            //            print_buffer(data.value(), 16);
            if (contents.try_append(data.value().data(), data.value().size()).is_error()) {
                FAIL("Allocation failure");
                loop.quit(1);
            }
        }
    };
    tls->on_tls_finished = [&] {
        loop.quit(0);
    };
    tls->on_tls_error = [&](TLS::AlertDescription) {
        FAIL("Connection failure");
        loop.quit(1);
    };
    if (!tls->connect(DEFAULT_SERVER, port)) {
        FAIL("connect() failed");
        return;
    }
    loop.exec();
}
