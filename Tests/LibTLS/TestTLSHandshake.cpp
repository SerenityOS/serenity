/*
 * Copyright (c) 2021, Peter Bocan  <me@pbocan.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Socket.h>
#include <LibCore/System.h>
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

ErrorOr<Vector<Certificate>> load_certificates();
ByteString locate_ca_certs_file();

ByteString locate_ca_certs_file()
{
    if (FileSystem::exists(ca_certs_file)) {
        return ca_certs_file;
    }
    auto on_target_path = ByteString("/etc/cacert.pem");
    if (FileSystem::exists(on_target_path)) {
        return on_target_path;
    }
    return "";
}

ErrorOr<Vector<Certificate>> load_certificates()
{
    auto cacert_file = TRY(Core::File::open(locate_ca_certs_file(), Core::File::OpenMode::Read));
    auto data = TRY(cacert_file->read_until_eof());
    return TRY(DefaultRootCACertificates::parse_pem_root_certificate_authorities(data));
}

TEST_CASE(test_TLS_hello_handshake)
{
    Core::EventLoop loop;
    TLS::Options options;
    options.set_root_certificates(TRY_OR_FAIL(load_certificates()));
    options.set_alert_handler([&](TLS::AlertDescription) {
        FAIL("Connection failure");
        loop.quit(1);
    });
    options.set_finish_callback([&] {
        loop.quit(0);
    });

    auto tls = TRY_OR_FAIL(TLS::TLSv12::connect(DEFAULT_SERVER, port, move(options)));
    ByteBuffer contents;
    tls->on_ready_to_read = [&] {
        (void)TRY_OR_FAIL(tls->read_some(contents.must_get_bytes_for_writing(4 * KiB)));
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

// A self-signed certificate for "localhost". The server side of the test below never uses its private key.
constexpr auto localhost_certificate = "MIIByzCCATSgAwIBAgIJAJN0Qh82eru3MA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNV"
                                       "BAMMCWxvY2FsaG9zdDAgFw0yNjExMDYxMzI0NDVaGA8yMTI2MTAxMzEzMjQ0NVow"
                                       "FDESMBAGA1UEAwwJbG9jYWxob3N0MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB"
                                       "gQC+36a0UHSQ5UJEDOHXhps+YR4hqJJrGlPN14yOr5j/AQFAKyyQioa3B4tUbZl2"
                                       "1Jhn95K1qEz+cgBPmoaUMBmSaigfqfQxjwJ6EcafzmUKCRXWxRwHYvWy7fg9bdxM"
                                       "liF+6xEcuOlWOLD8SxlAQUArS5/fKQF89eBb+GBO9Q2m3wIDAQABoyMwITAPBgNV"
                                       "HRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwICpDANBgkqhkiG9w0BAQsFAAOBgQAJ"
                                       "sWX4BuofEKi6H98EQLtGdHWGsLmHkxfAIzNgCEzzlNVx/BNrUfef7oClNMAzsZvQ"
                                       "gKWbaOHk0BMdtfDLsrcIb+BVsI3mkojDGy17aVuwD8/V1sKIuacElBmOPLF3d1YG"
                                       "xFJdYKQIQYcdhfsw+wkW8bBcMdSgKnWPeMcNlO7KQw=="sv;

TEST_CASE(test_TLS_finished_without_change_cipher_spec)
{
    Core::EventLoop loop;

    auto certificate_der = TRY_OR_FAIL(decode_base64(localhost_certificate));
    auto certificate = TRY_OR_FAIL(TLS::Certificate::parse_certificate(certificate_der));

    int fds[2];
    TRY_OR_FAIL(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fds));
    auto client_socket = TRY_OR_FAIL(Core::LocalSocket::adopt_fd(fds[0]));
    auto server_socket = TRY_OR_FAIL(Core::LocalSocket::adopt_fd(fds[1]));

    ByteBuffer server_flight;
    auto append_handshake_record = [&](TLS::HandshakeType type, ReadonlyBytes body) {
        size_t record_length = body.size() + 4;
        u8 header[] = {
            to_underlying(TLS::ContentType::HANDSHAKE), 0x03, 0x03,
            static_cast<u8>(record_length >> 8), static_cast<u8>(record_length),
            to_underlying(type),
            static_cast<u8>(body.size() >> 16), static_cast<u8>(body.size() >> 8), static_cast<u8>(body.size())
        };
        server_flight.append(header, sizeof(header));
        server_flight.append(body);
    };

    // Version, an all-zero random, no session id, TLS_RSA_WITH_AES_128_GCM_SHA256 and no compression.
    Array<u8, 38> server_hello {};
    server_hello[0] = 0x03;
    server_hello[1] = 0x03;
    server_hello[35] = to_underlying(TLS::CipherSuite::TLS_RSA_WITH_AES_128_GCM_SHA256) >> 8;
    server_hello[36] = to_underlying(TLS::CipherSuite::TLS_RSA_WITH_AES_128_GCM_SHA256) & 0xff;
    append_handshake_record(TLS::HandshakeType::SERVER_HELLO, server_hello);

    ByteBuffer certificate_list;
    for (size_t length : { certificate_der.size() + 3, certificate_der.size() }) {
        certificate_list.append(static_cast<u8>(length >> 16));
        certificate_list.append(static_cast<u8>(length >> 8));
        certificate_list.append(static_cast<u8>(length));
    }
    certificate_list.append(certificate_der);
    append_handshake_record(TLS::HandshakeType::CERTIFICATE, certificate_list);

    append_handshake_record(TLS::HandshakeType::SERVER_HELLO_DONE_RESERVED, {});

    // A Finished message that is not preceded by a ChangeCipherSpec, and so arrives in plaintext.
    Array<u8, 12> verify_data {};
    append_handshake_record(TLS::HandshakeType::FINISHED, verify_data);

    TRY_OR_FAIL(server_socket->write_until_depleted(server_flight));

    TLS::Options options;
    options.set_root_certificates(Vector { certificate });
    auto tls = TLS::TLSv12::connect("localhost"sv, *client_socket, move(options));
    EXPECT(tls.is_error() || !tls.value()->is_established());
}
