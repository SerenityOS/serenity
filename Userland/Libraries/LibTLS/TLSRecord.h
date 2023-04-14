/*
 * Copyright (c) 2022, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Result.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibTLS/Certificate.h>
#include <LibTLS/Extensions.h>

namespace TLS {

using CipherVariant = Variant<
    Empty,
    Crypto::Cipher::AESCipher::CBCMode,
    Crypto::Cipher::AESCipher::GCMMode>;

class TLSHandshake : public RefCounted<TLSHandshake> {
public:
    HandshakeType handshake_type;

    virtual ErrorOr<String> to_string(size_t) { TODO(); }
    virtual ~TLSHandshake() {};
};

class TLSAlert : public RefCounted<TLSAlert> {
public:
    AlertLevel level;
    AlertDescription description;

    ErrorOr<String> to_string(size_t);
    static ErrorOr<NonnullRefPtr<TLSAlert>> decode(ReadonlyBytes);
    ErrorOr<ByteBuffer> encode();
};

class TLSRecord {
public:
    ContentType content_type;
    ProtocolVersion protocol_version;
    Vector<RefPtr<TLSHandshake>> contents;
    RefPtr<TLSAlert> alert;

    ErrorOr<String> to_string(size_t);
    static ErrorOr<NonnullOwnPtr<TLSRecord>> decode(ReadonlyBytes);
    ErrorOr<ByteBuffer> encode();

    static Result<ByteBuffer, GenericError> read_sized_buffer(size_t, size_t, ReadonlyBytes);
    static u16 read_u16(ReadonlyBytes, size_t);
};

class HelloRequest : public TLSHandshake {
public:
    ErrorOr<String> to_string(size_t) override;
    static ErrorOr<NonnullRefPtr<HelloRequest>> decode(ReadonlyBytes);
    ErrorOr<ByteBuffer> encode();
};

class ClientHello : public TLSHandshake {
public:
    ProtocolVersion client_version;
    u8 client_random[32] {};
    u8 session_id[32] {};
    Vector<CipherSuite> cipher_suites;
    Vector<CompressionMethod> compression_methods;
    Vector<RefPtr<TLSExtension>> extensions;

    ErrorOr<String> to_string(size_t) override;
    static ErrorOr<NonnullRefPtr<ClientHello>> decode(ReadonlyBytes);
    ErrorOr<ByteBuffer> encode();
};

class ServerHello : public TLSHandshake {
public:
    ProtocolVersion server_version;
    u8 server_random[32] {};
    u8 session_id[32] {};
    CipherSuite cipher_suite;
    CompressionMethod compression_method;
    Vector<RefPtr<TLSExtension>> extensions;

    ErrorOr<String> to_string(size_t) override;
    static ErrorOr<NonnullRefPtr<ServerHello>> decode(ReadonlyBytes);
    ErrorOr<ByteBuffer> encode();
};

class HandshakeCertificate : public TLSHandshake {
public:
    Vector<Certificate> certificates;

    ErrorOr<String> to_string(size_t) override;
    static ErrorOr<NonnullRefPtr<HandshakeCertificate>> decode(ReadonlyBytes);
    ErrorOr<ByteBuffer> encode();
};

class HandshakeCertificateVerify : public TLSHandshake {
public:
    SignatureAlgorithm algorithm;
    ByteBuffer signature;

    ErrorOr<String> to_string(size_t) override;
    static ErrorOr<NonnullRefPtr<HandshakeCertificateVerify>> decode(ReadonlyBytes);
    ErrorOr<ByteBuffer> encode();
};

struct ECCurve {
    ByteBuffer a;
    ByteBuffer b;
};

struct ExplicitPrime {
    ByteBuffer prime;
};

struct ECTrinominal {
    ByteBuffer k;
};

struct ECPentanominal {
    ByteBuffer k1;
    ByteBuffer k2;
    ByteBuffer k3;
};

struct ExplicitChar {
    u16 m;
    Variant<Empty, ECTrinominal, ECPentanominal> basis;
};

struct ECCurveParameters {
    Variant<Empty, ExplicitPrime, ExplicitChar> data;
    ECCurve curve;
    ByteBuffer base;
    ByteBuffer order;
    ByteBuffer cofactor;
};

struct ECCurveExchange {
    ECCurveType curve_type;
    Variant<Empty, SupportedGroup, ECCurveParameters> curve_params;
};

struct CurveExchange {
    ByteBuffer p;
    ByteBuffer g;
    ByteBuffer Ys;
};

class ServerKeyExchange : public TLSHandshake {
public:
    Variant<Empty, CurveExchange, ECCurveExchange> exchange_parameters;
    ByteBuffer public_key;
    SignatureScheme signature_scheme;
    ByteBuffer signature;

    ErrorOr<String> to_string(size_t) override;
    static ErrorOr<NonnullRefPtr<ServerKeyExchange>> decode(ReadonlyBytes, KeyExchangeAlgorithm);
    ErrorOr<ByteBuffer> encode();
};

class ServerHelloDone : public TLSHandshake {
public:
    ErrorOr<String> to_string(size_t) override;
    static ErrorOr<NonnullRefPtr<ServerHelloDone>> decode(ReadonlyBytes);
    ErrorOr<ByteBuffer> encode();
};

}
