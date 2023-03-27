/*
 * Copyright (c) 2021, Peter Bocan  <me@pbocan.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/EventLoop.h>
#include <LibCrypto/ASN1/ASN1.h>
#include <LibCrypto/ASN1/PEM.h>
#include <LibFileSystem/FileSystem.h>
#include <LibTLS/TLSv12.h>
#include <LibTest/TestCase.h>

static StringView ca_certs_file = "./cacert.pem"sv;
static int port = 443;

constexpr auto DEFAULT_SERVER = "www.google.com"sv;

static ByteBuffer operator""_b(char const* string, size_t length)
{
    return ByteBuffer::copy(string, length).release_value();
}

Vector<Certificate> load_certificates();
DeprecatedString locate_ca_certs_file();

DeprecatedString locate_ca_certs_file()
{
    if (FileSystem::exists(ca_certs_file)) {
        return ca_certs_file;
    }
    auto on_target_path = DeprecatedString("/etc/cacert.pem");
    if (FileSystem::exists(on_target_path)) {
        return on_target_path;
    }
    return "";
}

Vector<Certificate> load_certificates()
{
    auto cacert_file = MUST(Core::File::open(locate_ca_certs_file(), Core::File::OpenMode::Read));
    auto data = MUST(cacert_file->read_until_eof());
    return MUST(DefaultRootCACertificates::the().reload_certificates(data));
}

static Vector<Certificate> s_root_ca_certificates = load_certificates();

TEST_CASE(test_TLS_hello_handshake)
{
    Core::EventLoop loop;
    TLS::Options options;
    options.set_root_certificates(s_root_ca_certificates);
    options.set_alert_handler([&](TLS::AlertDescription) {
        FAIL("Connection failure");
        loop.quit(1);
    });
    options.set_finish_callback([&] {
        loop.quit(0);
    });

    auto tls = MUST(TLS::TLSv12::connect(DEFAULT_SERVER, port, move(options)));
    ByteBuffer contents;
    tls->on_ready_to_read = [&] {
        auto read_bytes = MUST(tls->read_some(contents.must_get_bytes_for_writing(4 * KiB)));
        if (read_bytes.is_empty()) {
            FAIL("No data received");
            loop.quit(1);
        }
        loop.quit(0);
    };

    if (tls->write_until_depleted("GET / HTTP/1.1\r\nHost: "_b).is_error()) {
        FAIL("write(0) failed");
        return;
    }

    auto the_server = DEFAULT_SERVER;
    if (tls->write_until_depleted(the_server.bytes()).is_error()) {
        FAIL("write(1) failed");
        return;
    }
    if (tls->write_until_depleted("\r\nConnection : close\r\n\r\n"_b).is_error()) {
        FAIL("write(2) failed");
        return;
    }

    loop.exec();
}
