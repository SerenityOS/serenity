/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "../Transport.h"
#include "ByteReader.h"
#include "Extensions.h"
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibCore/TCPSocket.h>

#pragma once

class TLSContext {
public:
    ByteBuffer client_random;
    ByteBuffer server_random;
};

class TLSHeader : public RefCounted<TLSHeader> {
public:
    HandshakeType handshake_type {};
    SSLVersion ssl_version {};

    virtual ~TLSHeader() {};
};

class TLSRecord {
public:
    ContentType content_type;
    SSLVersion ssl_version {};
    RefPtr<TLSHeader> header;
};

class ClientHello : public TLSHeader {
public:
    SSLVersion ssl_version;
    ByteBuffer client_random;
    ByteBuffer session_id;
    Vector<CipherSuite> cipher_suites;
    Vector<CompressionMethod> compression_methods;
    NonnullRefPtrVector<TLSExtension> extensions;

    String to_string();
};

class ClientKeyExchange : public TLSHeader {
public:
    ByteBuffer public_key;

    String to_string();
};

class TLSTransport : public TransportBase {
public:
    void init(ReadonlyBytes input, RefPtr<Core::Socket> connection);
    ByteBuffer receive(int max_size, RefPtr<Core::Socket> connection);
    bool send(ReadonlyBytes, RefPtr<Core::Socket> connection);

    bool is_init() { return false; }

private:
    TLSRecord decode_tls_record(ReadonlyBytes);

    RefPtr<ClientHello> parse_client_hello(FTP::ByteReader&);
    RefPtr<ClientKeyExchange> parse_client_key_exchange(FTP::ByteReader&);

    ByteBuffer build_server_hello(ClientHello*);
    ByteBuffer build_server_certificate();
    ByteBuffer build_server_key_exchange();
    ByteBuffer build_server_hello_done();

    TLSContext m_context;
};
