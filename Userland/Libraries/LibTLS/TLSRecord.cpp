/*
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/Extensions.h>
#include <LibTLS/TLSRecord.h>
#include <LibTLS/TLSv12.h>

namespace TLS {

u16 TLSRecord::read_u16(ReadonlyBytes buffer, size_t offset)
{
    return AK::convert_between_host_and_network_endian(ByteReader::load16(buffer.offset_pointer(offset)));
}

Result<ByteBuffer, GenericError> TLSRecord::read_sized_buffer(size_t size, size_t offset, ReadonlyBytes buffer)
{
    if (offset + size > buffer.size()) {
        return GenericError::NeedMoreData;
    }

    auto maybe_buffer = ByteBuffer::create_uninitialized(size);
    if (maybe_buffer.is_error()) {
        return GenericError::OutOfMemory;
    }

    auto output = maybe_buffer.release_value();
    output.overwrite(0, buffer.slice(offset, size).data(), size);

    return output;
}

ErrorOr<String> TLSAlert::to_string(size_t indent)
{
    StringBuilder builder;

    builder.appendff("Alert:\n");
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Level: {}\n", enum_to_string(level));
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Description: {}\n", enum_to_string(description));
    builder.append(TRY(String::repeated('\t', indent + 2)));
    builder.appendff("{}\n", enum_to_value(description));

    return builder.to_string();
}

ErrorOr<NonnullRefPtr<TLSAlert>> TLSAlert::decode(ReadonlyBytes buffer)
{
    auto alert = TRY(try_make_ref_counted<TLSAlert>());
    alert->level = static_cast<AlertLevel>(AK::convert_between_host_and_network_endian(buffer[0]));
    alert->description = static_cast<AlertDescription>(AK::convert_between_host_and_network_endian(buffer[1]));
    return alert;
}

ErrorOr<ByteBuffer> TLSAlert::encode() { TODO(); }

ErrorOr<NonnullOwnPtr<TLSRecord>> TLSRecord::decode(ReadonlyBytes payload)
{
    size_t offset { 0 };

    // FIXME: Client message boundaries are not preserved in the record layer
    // (i.e.,multiple client messages of the same ContentType MAY be coalesced into a single TLSPlaintext record,
    // or a single message MAY be fragmented across several records).
    NonnullOwnPtr<TLSRecord> record = TRY(AK::try_make<TLSRecord>());
    record->content_type = static_cast<ContentType>(AK::convert_between_host_and_network_endian(payload[offset++]));
    record->protocol_version = static_cast<ProtocolVersion>(AK::convert_between_host_and_network_endian(ByteReader::load16(payload.offset_pointer(offset))));
    offset += 2;

    auto length = AK::convert_between_host_and_network_endian(ByteReader::load16(payload.offset_pointer(offset)));
    (void)length;
    offset += 2;

    ReadonlyBytes plain = payload;
    ByteBuffer decrypted;

    switch (record->content_type) {
    case ContentType::HANDSHAKE: {
        auto type = static_cast<HandshakeType>(AK::convert_between_host_and_network_endian(plain[offset]));

        switch (type) {
        case HandshakeType::HELLO_REQUEST_RESERVED: {
            auto content = TRY(HelloRequest::decode(plain.slice(offset, plain.size() - offset)));
            record->contents.append(content);
            break;
        }

        case HandshakeType::CLIENT_HELLO: {
            auto content = TRY(ClientHello::decode(plain.slice(offset, plain.size() - offset)));
            record->contents.append(content);
            break;
        }

        case HandshakeType::SERVER_HELLO: {
            auto content = TRY(ServerHello::decode(plain.slice(offset, plain.size() - offset)));
            record->contents.append(content);
            break;
        }

        case HandshakeType::CERTIFICATE: {
            auto content = TRY(HandshakeCertificate::decode(plain.slice(offset, plain.size() - offset)));
            record->contents.append(content);
            break;
        }

        case HandshakeType::SERVER_KEY_EXCHANGE_RESERVED: {
            /*
            auto algo = get_key_exchange_algorithm(cipher_suite);
            auto content = TRY(ServerKeyExchange::decode(plain.slice(offset, plain.size() - offset), algo));
            record->contents.append(content);
            */
            break;
        }

        case HandshakeType::SERVER_HELLO_DONE_RESERVED: {
            auto content = TRY(ServerHelloDone::decode(plain.slice(offset, plain.size() - offset)));
            record->contents.append(content);
            break;
        }

        default: {
            dbgln("Unable to handle handshake of type {}", enum_to_string(type));
            // TODO();
            break;
        }
        }
        break;
    }

    case ContentType::CHANGE_CIPHER_SPEC: {
        // We ignore this packet, since its not used.
        // TODO: Toggle on encryption
        break;
    }

    case ContentType::ALERT: {
        record->alert = TRY(TLSAlert::decode(plain.slice(offset, plain.size() - offset)));
        break;
    }

    default: {
        dbgln("Unable to handle TLSRecord of type {}", enum_to_string(record->content_type));
        // TODO();
        break;
    }
    }

    return record;
}

ErrorOr<ByteBuffer> TLSRecord::encode() { TODO(); }

ErrorOr<String> TLSRecord::to_string(size_t indent)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("TLSRecord:\n");
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Content Type: {}\n", enum_to_string(content_type));
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Protocol Version: {}\n", enum_to_string(protocol_version));

    if (alert) {
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("{}", TRY(alert->to_string(indent + 1)));
    }

    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Content:\n");

    for (auto& content : contents) {
        builder.append(TRY(content->to_string(indent + 2)));
    }

    return builder.to_string();
}

ErrorOr<NonnullRefPtr<HelloRequest>> HelloRequest::decode(ReadonlyBytes) { return AK::try_make_ref_counted<HelloRequest>(); }

ErrorOr<ByteBuffer> HelloRequest::encode()
{
    auto buffer = TRY(ByteBuffer::create_zeroed(4));
    buffer.append(static_cast<u8>(handshake_type));
    buffer.append(0);
    buffer.append(0);
    buffer.append(0);
    return buffer;
}

ErrorOr<String> HelloRequest::to_string(size_t indent)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("HelloRequest\n");

    return builder.to_string();
}

static ErrorOr<Vector<RefPtr<TLSExtension>>> parse_extensions(ReadonlyBytes buffer, size_t& offset)
{
    Vector<RefPtr<TLSExtension>> extensions;

    u16 extensions_length = TLSRecord::read_u16(buffer, offset);
    offset += sizeof(extensions_length);

    auto end_index = offset + extensions_length;

    while ((buffer.size() - offset) >= 4 && offset < end_index) {
        auto extension_type = static_cast<ExtensionType>(TLSRecord::read_u16(buffer, offset));
        u16 extension_length = TLSRecord::read_u16(buffer, offset + sizeof(ExtensionType));

        dbgln_if(TLS_DEBUG, "parsing extension: {}, expected bytes: {}, remaining bytes after parse: {}",
            enum_to_string(extension_type), extension_length, (i32)buffer.size() - offset - 4 - extension_length);

        switch (extension_type) {
        case ExtensionType::SERVER_NAME: {
            auto name_list = TRY(ServerNameList::decode(buffer.slice(offset, buffer.size() - offset)));
            extensions.append(name_list.release_nonnull());
            break;
        }
        case ExtensionType::EC_POINT_FORMATS: {
            auto points = TRY(ECPointFormats::decode(buffer.slice(offset, buffer.size() - offset)));
            extensions.append(points.release_nonnull());
            break;
        }
        case ExtensionType::SIGNATURE_ALGORITHMS: {
            auto points = TRY(SignatureSchemes::decode(buffer.slice(offset, buffer.size() - offset)));
            extensions.append(points.release_nonnull());
            break;
        }
        case ExtensionType::SUPPORTED_GROUPS: {
            auto points = TRY(SupportedGroups::decode(buffer.slice(offset, buffer.size() - offset)));
            extensions.append(points.release_nonnull());
            break;
        }

        default: {
            dbgln_if(TLS_DEBUG, "Encountered unknown extension {} with length {}", enum_to_string(extension_type), extension_length);
        }
        }
        offset += sizeof(extension_type) + sizeof(extension_length) + extension_length;
    }

    dbgln_if(TLS_DEBUG, "Handshake Extensions: parsing over, remaining bytes after parse: {}", (i32)buffer.size() - offset);

    return extensions;
}

ErrorOr<NonnullRefPtr<ClientHello>> ClientHello::decode(ReadonlyBytes buffer)
{
    auto type = static_cast<HandshakeType>(buffer[0]);
    VERIFY(type == HandshakeType::CLIENT_HELLO);

    size_t length = buffer[1] * 0x10000 + buffer[2] * 0x100 + buffer[3];
    if (buffer.size() < length) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    size_t offset { 4 };
    auto client_hello = TRY(try_make_ref_counted<ClientHello>());

    client_hello->client_version = static_cast<ProtocolVersion>(TLSRecord::read_u16(buffer, offset));
    offset += sizeof(ProtocolVersion);

    memcpy(client_hello->client_random, buffer.offset_pointer(offset), sizeof(client_hello->client_random));
    offset += sizeof(client_hello->client_random);

    auto session_length = buffer[offset++];
    memcpy(client_hello->session_id, buffer.offset_pointer(offset), session_length);
    offset += session_length;

    u16 cipher_suite_length = TLSRecord::read_u16(buffer, offset);
    offset += sizeof(cipher_suite_length);

    auto cipher_suite_count = cipher_suite_length / 2;

    u16 cipher_index = 0;
    while (buffer.size() - offset >= 2 && cipher_index++ < cipher_suite_count) {
        client_hello->cipher_suites.append(static_cast<CipherSuite>(TLSRecord::read_u16(buffer, offset)));
        offset += sizeof(CipherSuite);
    }

    auto compression_method_length = buffer[offset++];
    u16 compression_method_index = 0;
    while (buffer.size() - offset >= 1 && compression_method_index++ < compression_method_length) {
        client_hello->compression_methods.append(static_cast<CompressionMethod>(buffer[offset++]));
    }

    client_hello->extensions = TRY(parse_extensions(buffer, offset));

    return client_hello;
}

ErrorOr<ByteBuffer> ClientHello::encode() { TODO(); }

ErrorOr<String> ClientHello::to_string(size_t indent)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Server Hello:\n");
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Server version: {}\n", enum_to_string(client_version));
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Server random: {:hex-dump}\n", client_random);
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Session ID: {:hex-dump}\n", session_id);
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Cipher suits: {}\n", cipher_suites.size());
    for (auto& cipher : cipher_suites) {
        builder.append(TRY(String::repeated('\t', indent + 2)));
        builder.appendff("{}\n", enum_to_string(cipher));
    }
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Compression methods: {}\n", compression_methods.size());
    for (auto& compression : compression_methods) {
        builder.append(TRY(String::repeated('\t', indent + 2)));
        builder.appendff("{}\n", enum_to_string(compression));
    }
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Extensions: {}\n", extensions.size());
    for (auto& extension : extensions) {
        builder.append(TRY(extension->to_string(indent + 2)));
    }

    return builder.to_string();
}

ErrorOr<NonnullRefPtr<ServerHello>> ServerHello::decode(ReadonlyBytes buffer)
{
    auto type = static_cast<HandshakeType>(buffer[0]);
    VERIFY(type == HandshakeType::SERVER_HELLO);

    size_t length = buffer[1] * 0x10000 + buffer[2] * 0x100 + buffer[3];
    if (buffer.size() < length) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    size_t offset { 4 };
    auto server_hello = TRY(try_make_ref_counted<ServerHello>());

    server_hello->server_version = static_cast<ProtocolVersion>(TLSRecord::read_u16(buffer, offset));
    offset += sizeof(ProtocolVersion);

    memcpy(server_hello->server_random, buffer.offset_pointer(offset), sizeof(server_hello->server_random));
    offset += sizeof(server_hello->server_random);

    auto session_length = buffer[offset++];
    memcpy(server_hello->session_id, buffer.offset_pointer(offset), session_length);
    offset += session_length;

    server_hello->cipher_suite = static_cast<CipherSuite>(TLSRecord::read_u16(buffer, offset));
    offset += sizeof(CipherSuite);

    if (!TLSv12::supports_cipher(server_hello->cipher_suite)) {
        dbgln_if(TLS_DEBUG, "No supported cipher could be agreed upon");
        return AK::Error::from_string_view(enum_to_string(GenericError::NoCommonCipher));
    }

    server_hello->compression_method = static_cast<CompressionMethod>(buffer[offset++]);
    if (server_hello->compression_method != CompressionMethod::NONE)
        return AK::Error::from_string_view(enum_to_string(GenericError::CompressionNotSupported));

    server_hello->extensions = TRY(parse_extensions(buffer, offset));

    return server_hello;
}

ErrorOr<ByteBuffer> ServerHello::encode() { TODO(); }

ErrorOr<String> ServerHello::to_string(size_t indent)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Server Hello:\n");
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Server version: {}\n", enum_to_string(server_version));
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Server random: {:hex-dump}\n", server_random);
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Session ID: {:hex-dump}\n", session_id);
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Cipher suite: {}\n", enum_to_string(cipher_suite));
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Compression method: {}\n", enum_to_string(compression_method));
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Extensions: {}\n", extensions.size());
    for (auto& extension : extensions) {
        builder.append(TRY(extension->to_string(indent + 2)));
    }

    return builder.to_string();
}

ErrorOr<NonnullRefPtr<HandshakeCertificate>> HandshakeCertificate::decode(ReadonlyBytes buffer)
{
    size_t offset = { 0 };
    auto type = static_cast<HandshakeType>(buffer[offset++]);
    VERIFY(type == HandshakeType::CERTIFICATE);

    size_t extension_length = buffer[offset] * 0x10000 + buffer[offset + 1] * 0x100 + buffer[offset + 2];
    offset += 3;
    if (buffer.size() < extension_length) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    size_t certificates_length = buffer[offset] * 0x10000 + buffer[offset + 1] * 0x100 + buffer[offset + 2];
    offset += 3;

    auto handshake_certificates = TRY(try_make_ref_counted<HandshakeCertificate>());
    while (offset < certificates_length - 7) {
        size_t certificate_size = buffer[offset] * 0x10000 + buffer[offset + 1] * 0x100 + buffer[offset + 2];
        offset += 3;

        auto certificate = TRY(Certificate::parse_certificate(buffer.slice(offset, certificate_size), false));
        handshake_certificates->certificates.append(certificate);
        offset += certificate_size;
    }

    return handshake_certificates;
}

ErrorOr<ByteBuffer> HandshakeCertificate::encode() { TODO(); }

ErrorOr<String> HandshakeCertificate::to_string(size_t indent)
{
    StringBuilder builder;

    for (auto& certificate : certificates) {
        builder.append(TRY(String::repeated('\t', indent)));
        builder.appendff("Handshake Certificate:\n");
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("Version: {}\n", certificate.version);
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("Serial Number: {}\n", TRY(certificate.serial_number.to_base(16)));
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("Signature Algorithm: {}\n", to_underlying(certificate.algorithm));

        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("Validity:\n");
        builder.append(TRY(String::repeated('\t', indent + 2)));
        builder.appendff("Not Before: {}\n", TRY(certificate.validity.not_before.to_string()));
        builder.append(TRY(String::repeated('\t', indent + 2)));
        builder.appendff("Not After: {}\n", TRY(certificate.validity.not_after.to_string()));

        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("Issuer: {}\n", TRY(certificate.issuer.to_string()));
        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("Subject: {}\n", TRY(certificate.subject.to_string()));

        builder.append(TRY(String::repeated('\t', indent + 1)));
        builder.appendff("Subject Public Key Info:\n");
        builder.append(TRY(String::repeated('\t', indent + 2)));
        builder.appendff("Algorithm: {}\n", to_underlying(certificate.public_key.algorithm));
        switch (certificate.public_key.algorithm) {
        default:
            builder.append(TRY(String::repeated('\t', indent + 3)));
            builder.appendff("Public-Key: {:hex-dump}\n", certificate.public_key.raw_key.bytes());
            break;
        }
    }

    return builder.to_string();
}

ErrorOr<NonnullRefPtr<HandshakeCertificateVerify>> HandshakeCertificateVerify::decode(ReadonlyBytes) { TODO(); }
ErrorOr<ByteBuffer> HandshakeCertificateVerify::encode() { TODO(); }
ErrorOr<String> HandshakeCertificateVerify::to_string(size_t) { TODO(); }

static ErrorOr<ByteBuffer> decode_u8_buffer(ReadonlyBytes bytes, size_t& offset)
{
    u8 buffer_size = bytes[offset];
    offset += sizeof(u8);

    auto maybe_buffer = TLSRecord::read_sized_buffer(buffer_size, offset, bytes);
    if (maybe_buffer.is_error()) {
        return AK::Error::from_errno((i8)maybe_buffer.release_error());
    }

    offset += buffer_size;
    return maybe_buffer.release_value();
}

static ErrorOr<ByteBuffer> decode_u16_buffer(ReadonlyBytes bytes, size_t& offset)
{
    u16 buffer_size = AK::convert_between_host_and_network_endian(ByteReader::load16(bytes.offset_pointer(offset)));
    offset += sizeof(u16);

    auto maybe_buffer = TLSRecord::read_sized_buffer(buffer_size, offset, bytes);
    if (maybe_buffer.is_error()) {
        return AK::Error::from_string_view(enum_to_string(maybe_buffer.error()));
    }

    offset += buffer_size;
    return maybe_buffer.release_value();
}

static ErrorOr<CurveExchange> decode_curve_exchange(ReadonlyBytes buffer, size_t& offset)
{
    CurveExchange exchange;
    exchange.p = TRY(decode_u16_buffer(buffer, offset));
    exchange.g = TRY(decode_u16_buffer(buffer, offset));
    exchange.Ys = TRY(decode_u16_buffer(buffer, offset));
    return exchange;
}

static ErrorOr<ExplicitPrime> decode_explicit_prime(ReadonlyBytes buffer, size_t& offset)
{
    ExplicitPrime parameter;
    parameter.prime = TRY(decode_u8_buffer(buffer, offset));
    return parameter;
}

static ErrorOr<ExplicitChar> decode_explicit_char(ReadonlyBytes buffer, size_t& offset)
{
    ExplicitChar parameter;

    parameter.m = TLSRecord::TLSRecord::read_u16(buffer, offset);
    offset += sizeof(u16);

    auto basis = static_cast<ECBasisType>(AK::convert_between_host_and_network_endian(buffer[offset++]));
    if (basis == ECBasisType::EC_BASIS_TRINOMINAL) {
        ECTrinominal trinominal;
        trinominal.k = TRY(decode_u8_buffer(buffer, offset));
        parameter.basis = trinominal;
    } else if (basis == ECBasisType::EC_BASIS_PENTANOMINAL) {
        ECPentanominal pentanominal;
        pentanominal.k1 = TRY(decode_u8_buffer(buffer, offset));
        pentanominal.k2 = TRY(decode_u8_buffer(buffer, offset));
        pentanominal.k3 = TRY(decode_u8_buffer(buffer, offset));
        parameter.basis = pentanominal;
    } else {
        VERIFY_NOT_REACHED();
    }

    return parameter;
}

static ErrorOr<ECCurveExchange> decode_ec_curve_exchange(ReadonlyBytes buffer, size_t& offset)
{
    ECCurveExchange exchange;
    exchange.curve_type = static_cast<ECCurveType>(AK::convert_between_host_and_network_endian(buffer[offset]));
    offset += sizeof(ECCurveType);

    if (exchange.curve_type == ECCurveType::NAMED_CURVE) {
        exchange.curve_params = static_cast<SupportedGroup>(TLSRecord::read_u16(buffer, offset));
        offset += sizeof(SupportedGroup);
    } else {
        ECCurveParameters params;

        if (exchange.curve_type == ECCurveType::EXPLICIT_PRIME) {
            params.data = TRY(decode_explicit_prime(buffer, offset));
        } else if (exchange.curve_type == ECCurveType::EXPLICIT_CHAR2) {
            params.data = TRY(decode_explicit_char(buffer, offset));
        } else {
            VERIFY_NOT_REACHED();
        }

        params.curve.a = TRY(decode_u8_buffer(buffer, offset));
        params.curve.b = TRY(decode_u8_buffer(buffer, offset));
        params.base = TRY(decode_u8_buffer(buffer, offset));
        params.order = TRY(decode_u8_buffer(buffer, offset));
        params.cofactor = TRY(decode_u8_buffer(buffer, offset));

        exchange.curve_params = params;
    }

    return exchange;
}

static ErrorOr<void> decode_key_exchange_signature(ReadonlyBytes buffer, size_t& offset, ServerKeyExchange& server_key_exchange)
{
    server_key_exchange.signature_scheme = static_cast<SignatureScheme>(TLSRecord::read_u16(buffer, offset));
    offset += sizeof(SignatureScheme);

    server_key_exchange.signature = TRY(decode_u16_buffer(buffer, offset));
    return {};
}

ErrorOr<NonnullRefPtr<ServerKeyExchange>> ServerKeyExchange::decode(ReadonlyBytes buffer, KeyExchangeAlgorithm exchange_algorithm)
{
    auto type = static_cast<HandshakeType>(buffer[0]);
    VERIFY(type == HandshakeType::SERVER_KEY_EXCHANGE_RESERVED);

    size_t length = buffer[1] * 0x10000 + buffer[2] * 0x100 + buffer[3];
    if (buffer.size() < length) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    size_t offset { 4 };
    auto server_key_exchange = TRY(try_make_ref_counted<ServerKeyExchange>());

    switch (exchange_algorithm) {
    // https://datatracker.ietf.org/doc/html/rfc5246#section-7.4.3
    case KeyExchangeAlgorithm::RSA:
    case KeyExchangeAlgorithm::DH_DSS:
    case KeyExchangeAlgorithm::DH_RSA: {
        break;
    }
    case KeyExchangeAlgorithm::DH_anon: {
        CurveExchange exchange = TRY(decode_curve_exchange(buffer, offset));
        server_key_exchange->public_key = exchange.Ys;
        server_key_exchange->exchange_parameters = exchange;
        break;
    }

    case KeyExchangeAlgorithm::DHE_DSS:
    case KeyExchangeAlgorithm::DHE_RSA: {
        CurveExchange exchange = TRY(decode_curve_exchange(buffer, offset));
        server_key_exchange->public_key = exchange.Ys;
        server_key_exchange->exchange_parameters = exchange;
        TRY(decode_key_exchange_signature(buffer, offset, server_key_exchange));
        break;
    }

    case KeyExchangeAlgorithm::ECDHE_RSA: {
        ECCurveExchange exchange = TRY(decode_ec_curve_exchange(buffer, offset));
        server_key_exchange->public_key = TRY(decode_u8_buffer(buffer, offset));
        server_key_exchange->exchange_parameters = exchange;
        TRY(decode_key_exchange_signature(buffer, offset, server_key_exchange));
        break;
    }

    default: {
        dbgln("Unhandled server key exchange algorithm, {}", enum_to_string(exchange_algorithm));
        VERIFY_NOT_REACHED();
        break;
    }
    }

    return server_key_exchange;
}

ErrorOr<ByteBuffer> ServerKeyExchange::encode() { TODO(); }

ErrorOr<String> ServerKeyExchange::to_string(size_t indent)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Server Key Exchange:\n");

    TRY(exchange_parameters.visit(
        [](Empty) -> ErrorOr<void> {
            VERIFY_NOT_REACHED();
            return AK::Error::from_string_view("Empty exchange params"sv);
        },
        [&](CurveExchange exchange) -> ErrorOr<void> {
            builder.append(TRY(String::repeated('\t', indent + 1)));
            builder.appendff("Curve Exchange:\n");
            builder.append(TRY(String::repeated('\t', indent + 2)));
            builder.appendff("P: {:hex-dump}\n", exchange.p.bytes());
            builder.append(TRY(String::repeated('\t', indent + 2)));
            builder.appendff("G: {:hex-dump}\n", exchange.g.bytes());
            return {};
        },
        [&](ECCurveExchange exchange) -> ErrorOr<void> {
            builder.append(TRY(String::repeated('\t', indent + 1)));
            builder.appendff("EC Curve Exchange:\n");
            builder.append(TRY(String::repeated('\t', indent + 2)));
            builder.appendff("Curve Type: {}\n", enum_to_string(exchange.curve_type));
            TRY(exchange.curve_params.visit(
                [](Empty) -> ErrorOr<void> {
                    VERIFY_NOT_REACHED();
                    return AK::Error::from_string_view("Empty curve params"sv);
                },
                [&](SupportedGroup group) -> ErrorOr<void> {
                    builder.append(TRY(String::repeated('\t', indent + 2)));
                    builder.appendff("Curve Name: {}\n", enum_to_string(group));
                    return {};
                },
                [&](ECCurveParameters params) -> ErrorOr<void> {
                    (void)params;
                    builder.append(TRY(String::repeated('\t', indent + 2)));
                    builder.appendff("DUMP FOR ECCurveParameters not configured");
                    return {};
                }));
            return {};
        }));

    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Public Key: {:hex-dump}\n", public_key.bytes());
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Signature Scheme: {}\n", enum_to_string(signature_scheme));
    builder.append(TRY(String::repeated('\t', indent + 1)));
    builder.appendff("Signature: {:hex-dump}\n", signature.bytes());

    return builder.to_string();
}

ErrorOr<NonnullRefPtr<ServerHelloDone>> ServerHelloDone::decode(ReadonlyBytes buffer)
{
    auto type = static_cast<HandshakeType>(buffer[0]);
    VERIFY(type == HandshakeType::SERVER_HELLO_DONE_RESERVED);

    size_t length = buffer[1] * 0x10000 + buffer[2] * 0x100 + buffer[3];
    if (buffer.size() < length) {
        return AK::Error::from_string_view(enum_to_string(GenericError::NeedMoreData));
    }

    return TRY(try_make_ref_counted<ServerHelloDone>());
}

ErrorOr<ByteBuffer> ServerHelloDone::encode()
{
    auto buffer = TRY(ByteBuffer::create_zeroed(4));
    buffer.append(static_cast<u8>(handshake_type));
    buffer.append(0);
    buffer.append(0);
    buffer.append(0);
    return buffer;
}

ErrorOr<String> ServerHelloDone::to_string(size_t indent)
{
    StringBuilder builder;

    builder.append(TRY(String::repeated('\t', indent)));
    builder.appendff("Server Hello Done");

    return builder.to_string();
}
}
