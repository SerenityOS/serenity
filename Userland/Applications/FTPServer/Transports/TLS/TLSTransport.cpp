/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TLSTransport.h"
#include "ByteReader.h"
#include "ByteWriter.h"
#include <AK/MemoryStream.h>
#include <AK/Random.h>
#include <LibCore/FileStream.h>
#include <LibCore/Socket.h>

static ByteBuffer get_random_bytes(u32 count)
{
    ByteBuffer buffer = ByteBuffer::create_uninitialized(count);

    fill_with_random(buffer.data(), count);

    return buffer;
}

ByteBuffer TLSTransport::receive(int max_size, RefPtr<Core::Socket> socket)
{
    dbgln("send on tls transport");
    return socket->receive(max_size);
}

bool TLSTransport::send(ReadonlyBytes data, RefPtr<Core::Socket> socket)
{
    dbgln("send on tls transport");
    return socket->send(data);
}

TLSRecord TLSTransport::decode_tls_record(ReadonlyBytes input)
{
    FTP::ByteReader data { InputMemoryStream(input) };

    TLSRecord packet;
    packet.content_type = static_cast<ContentType>(data.read_1_bytes());
    packet.ssl_version = static_cast<SSLVersion>(data.read_2_bytes());
    u16 packet_payload_size = data.read_2_bytes();
    VERIFY(data.remaining() >= packet_payload_size);

    auto handshake_type = static_cast<HandshakeType>(data.read_1_bytes());
    u32 header_payload_size = data.read_3_bytes();
    VERIFY(data.remaining() >= header_payload_size);

    dbgln("got handshake: {}", enum_to_string(handshake_type));
    switch (handshake_type) {
    case HandshakeType::CLIENT_HELLO: {
        packet.header = parse_client_hello(data);
        break;
    }

    case HandshakeType::CLIENT_KEY_EXCHANGE: {
        packet.header = parse_client_key_exchange(data);
        break;
    }

    default: {
        dbgln("unhandled packet type: {}", enum_to_string(handshake_type));
        VERIFY_NOT_REACHED();
        break;
    }
    }

    return packet;
}

void TLSTransport::init(ReadonlyBytes input, RefPtr<Core::Socket> socket)
{
    dbgln("Received raw: {:hex-dump}", input);
    dbgln("in tls init");

    TLSRecord record = decode_tls_record(input);

    switch (static_cast<HandshakeType>(record.header->handshake_type)) {
    case HandshakeType::CLIENT_HELLO: {
        auto client_hello = static_ptr_cast<ClientHello>(record.header);

        m_context.client_random = client_hello->client_random;

        dbgln(client_hello->to_string());

        m_context.server_random = get_random_bytes(32);
        ByteBuffer server_hello = build_server_hello(client_hello);
        send(server_hello, socket);

        ByteBuffer server_certificate = build_server_certificate();
        send(server_certificate, socket);

        /*
        ByteBuffer server_key_exchange = build_server_key_exchange();
        send(server_key_exchange, socket);
        */

        ByteBuffer server_hello_done = build_server_hello_done();
        send(server_hello_done, socket);
        break;
    }

    case HandshakeType::CLIENT_KEY_EXCHANGE: {
        auto client_exchange = static_ptr_cast<ClientKeyExchange>(record.header);

        dbgln(client_exchange->to_string());

        break;
    }

    default: {
        dbgln("packet type: {}", enum_to_string(record.header->handshake_type));
        VERIFY_NOT_REACHED();
        break;
    }
    }
}

RefPtr<ClientHello> TLSTransport::parse_client_hello(FTP::ByteReader& header_stream)
{
    auto client_hello = create<ClientHello>();
    client_hello->handshake_type = HandshakeType::CLIENT_HELLO;
    client_hello->ssl_version = static_cast<SSLVersion>(header_stream.read_2_bytes());

    client_hello->client_random = header_stream.read_bytes(32);

    u8 session_id_size = header_stream.read_1_bytes();
    client_hello->session_id = header_stream.read_bytes(session_id_size);

    u16 cipher_suite_size = header_stream.read_2_bytes();
    for (u16 i = 0; i < cipher_suite_size / sizeof(CipherSuite); i++) {
        client_hello->cipher_suites.append(static_cast<CipherSuite>(header_stream.read_2_bytes()));
    }

    u8 compression_methods_size = header_stream.read_1_bytes();
    for (u8 i = 0; i < compression_methods_size; i++) {
        client_hello->compression_methods.append(static_cast<CompressionMethod>(header_stream.read_1_bytes()));
    }

    u16 extension_data_size = header_stream.read_2_bytes();

    if (extension_data_size == 0) {
        return client_hello;
    }

    while (extension_data_size > 0) {
        ExtensionType extension_type = static_cast<ExtensionType>(header_stream.read_2_bytes());
        u16 extension_size = header_stream.read_2_bytes();

        dbgln("parsing type: {}, remaining bytes: {}", enum_to_string(extension_type), header_stream.remaining());

        extension_data_size -= 4;
        extension_data_size -= extension_size;

        switch (extension_type) {
        case ExtensionType::STATUS_REQUEST: {
            auto status_request = create<CertificateStatusRequest>();
            status_request->type = extension_type;
            status_request->size = extension_size;
            status_request->status_type = static_cast<CertificateStatusType>(header_stream.read_1_bytes());
            u16 responder_id_size = header_stream.read_2_bytes();
            status_request->responder_id = header_stream.read_bytes(responder_id_size);
            u16 request_extension_info_size = header_stream.read_2_bytes();
            status_request->request_extension_info = header_stream.read_bytes(request_extension_info_size);
            client_hello->extensions.append(move(status_request));
            break;
        }

        case ExtensionType::SUPPORTED_GROUPS: {
            auto supported_groups = create<SupportedGroups>();
            supported_groups->type = extension_type;
            supported_groups->size = extension_size;
            u16 groups_count = header_stream.read_2_bytes();
            for (u16 i = 0; i < groups_count / sizeof(SupportedGroup); i++) {
                supported_groups->groups.append(static_cast<SupportedGroup>(header_stream.read_2_bytes()));
            }
            client_hello->extensions.append(move(supported_groups));
            break;
        }

        case ExtensionType::EC_POINT_FORMATS: {
            auto point_formats = create<ECPointFormats>();
            point_formats->type = extension_type;
            point_formats->size = extension_size;
            u8 format_count = header_stream.read_1_bytes();
            for (u8 i = 0; i < format_count; i++) {
                point_formats->formats.append(static_cast<ECPointFormat>(header_stream.read_1_bytes()));
            }
            client_hello->extensions.append(move(point_formats));
            break;
        }

        case ExtensionType::SIGNATURE_ALGORITHMS: {
            auto signature_schemes = create<SignatureSchemes>();
            signature_schemes->type = extension_type;
            signature_schemes->size = extension_size;
            u16 signature_count = header_stream.read_2_bytes();
            for (u8 i = 0; i < signature_count / sizeof(SignatureScheme); i++) {
                signature_schemes->signatures.append(static_cast<SignatureScheme>(header_stream.read_2_bytes()));
            }
            client_hello->extensions.append(move(signature_schemes));
            break;
        }

        case ExtensionType::SESSION_TICKET: {
            auto session_ticket = create<SessionTicket>();
            session_ticket->type = extension_type;
            session_ticket->size = extension_size;
            client_hello->extensions.append(move(session_ticket));
            break;
        }

        case ExtensionType::ENCRYPT_THEN_MAC: {
            auto encrypt_then_mac = create<EncryptThenMac>();
            encrypt_then_mac->type = extension_type;
            encrypt_then_mac->size = extension_size;
            client_hello->extensions.append(move(encrypt_then_mac));
            break;
        }

        case ExtensionType::EXTENDED_MASTER_SECRET: {
            auto extend_master_secret = create<ExtendMasterSecret>();
            extend_master_secret->type = extension_type;
            extend_master_secret->size = extension_size;
            client_hello->extensions.append(move(extend_master_secret));
            break;
        }

        case ExtensionType::KEY_SHARE: {
            auto key_share = create<KeyShares>();
            key_share->type = extension_type;
            key_share->size = extension_size;
            u16 key_share_byte_size = header_stream.read_2_bytes();
            while (key_share_byte_size > 0) {
                SupportedGroup group = static_cast<SupportedGroup>(header_stream.read_2_bytes());

                u16 key_size = header_stream.read_2_bytes();
                ByteBuffer key = header_stream.read_bytes(key_size);

                key_share->keys.append(KeyShareEntry { group, key });

                key_share_byte_size -= 4;
                key_share_byte_size -= key_size;
            }

            client_hello->extensions.append(move(key_share));
            break;
        }

        case ExtensionType::SUPPORTED_VERSIONS: {
            auto supported_versions = create<SupportedVersions>();
            supported_versions->type = extension_type;
            supported_versions->size = extension_size;
            u8 version_count = header_stream.read_1_bytes();
            for (u8 i = 0; i < version_count / sizeof(SSLVersion); i++) {
                supported_versions->versions.append(static_cast<SSLVersion>(header_stream.read_2_bytes()));
            }
            client_hello->extensions.append(move(supported_versions));
            break;
        }

        case ExtensionType::RENEGOTIATION_INFO: {
            auto renegotiation_info = create<RenegotiationInfo>();
            renegotiation_info->type = extension_type;
            renegotiation_info->size = extension_size;
            u8 data_size = header_stream.read_1_bytes();
            renegotiation_info->data = header_stream.read_bytes(data_size);
            break;
        }

        case ExtensionType::PSK_KEY_EXCHANGE_MODES: {
            auto exchange_modes = create<PSKKeyExchangeModes>();
            exchange_modes->type = extension_type;
            exchange_modes->size = extension_size;
            u8 mode_counts = header_stream.read_1_bytes();
            for (u8 i = 0; i < mode_counts; i++) {
                exchange_modes->modes.append(static_cast<PSKKeyExchangeMode>(header_stream.read_1_bytes()));
            }
            client_hello->extensions.append(move(exchange_modes));
            break;
        }

        case ExtensionType::RECORD_SIZE_LIMIT: {
            auto size_limit = create<RecordSizeLimit>();
            size_limit->type = extension_type;
            size_limit->size = extension_size;
            size_limit->limit = header_stream.read_2_bytes();
            client_hello->extensions.append(move(size_limit));
            break;
        }

        default:
            dbgln("unhandled extension type: {}", static_cast<u16>(extension_type));
            VERIFY_NOT_REACHED();
            break;
        }
    }

    return client_hello;
}

RefPtr<ClientKeyExchange> TLSTransport::parse_client_key_exchange(FTP::ByteReader& header_stream)
{
    auto key_exchange = create<ClientKeyExchange>();
    key_exchange->handshake_type = HandshakeType::CLIENT_KEY_EXCHANGE;

    u32 key_length = header_stream.read_1_bytes();
    key_exchange->public_key = header_stream.read_bytes(key_length);

    dbgln("remaining: {}", header_stream.remaining());

    return key_exchange;
}

ByteBuffer TLSTransport::build_server_hello(ClientHello* client_hello)
{
    FTP::ByteWriter output;

    // Packet header
    output.write_1_bytes(static_cast<u8>(ContentType::HANDSHAKE));
    output.write_2_bytes(static_cast<u16>(SSLVersion::VERSION_1_2));
    u8 packet_size_offset = output.length();
    output.write_2_bytes(0);

    // TLS header (Server hello)
    output.write_1_bytes(static_cast<u8>(HandshakeType::SERVER_HELLO));
    u8 message_size_offset = output.length();
    output.write_3_bytes(0);
    output.write_2_bytes(static_cast<u16>(SSLVersion::VERSION_1_2));

    output.write_bytes(m_context.server_random);

    output.write_1_bytes(client_hello->session_id.size());
    output.write_bytes(client_hello->session_id.bytes());

    // FIXME: find first supported cipher in the list from the client_hello
    output.write_2_bytes(static_cast<u16>(CipherSuite::TLS_RSA_WITH_AES_128_CBC_SHA));

    output.write_1_bytes(static_cast<u8>(CompressionMethod::NONE));

    // extensions
    u8 extensions_size_offset = output.length();
    output.write_2_bytes(0);

    output.write_2_bytes(static_cast<u16>(ExtensionType::RENEGOTIATION_INFO));
    output.write_2_bytes(1);
    output.write_1_bytes(0);

    // Since the offset includes the "size" part as well, we skip an additional "size" bytes
    output.set_2_bytes(extensions_size_offset, output.length() - extensions_size_offset - 2);
    output.set_3_bytes(message_size_offset, output.length() - message_size_offset - 3);
    output.set_2_bytes(packet_size_offset, output.length() - packet_size_offset - 2);

    return output.build();
}

ByteBuffer TLSTransport::build_server_certificate()
{
    FTP::ByteWriter output;

    // Packet header
    output.write_1_bytes(static_cast<u8>(ContentType::HANDSHAKE));
    output.write_2_bytes(static_cast<u16>(SSLVersion::VERSION_1_2));
    u8 packet_size_offset = output.length();
    output.write_2_bytes(0);

    // TLS header (Server certificate)
    output.write_1_bytes(static_cast<u8>(HandshakeType::CERTIFICATE));
    u8 message_size_offset = output.length();
    output.write_3_bytes(0);

    u8 certificates_size_offset = output.length();
    output.write_3_bytes(0);

    u8 certificate_size_offset = output.length();
    output.write_3_bytes(0);

    auto stream_or_error = Core::InputFileStream::open("/usr/cert.der");
    if (!stream_or_error.is_error()) {
        auto& stream = stream_or_error.value();

        auto buffer = ByteBuffer::create_uninitialized(4 * KiB);
        while (!stream.has_any_error() && buffer.size() > 0) {
            auto nread = stream.read(buffer);
            buffer.resize(nread);
            output.write_bytes(buffer.bytes());
        }
    }

    // Since the offset includes the "size" part as well, we skip an additional "size" bytes
    output.set_3_bytes(certificate_size_offset, output.length() - certificate_size_offset - 3);
    output.set_3_bytes(certificates_size_offset, output.length() - certificates_size_offset - 3);
    output.set_3_bytes(message_size_offset, output.length() - message_size_offset - 3);
    output.set_2_bytes(packet_size_offset, output.length() - packet_size_offset - 2);

    return output.build();
}

ByteBuffer TLSTransport::build_server_key_exchange()
{
    FTP::ByteWriter output;

    output.write_1_bytes(static_cast<u8>(ContentType::HANDSHAKE));
    output.write_2_bytes(static_cast<u16>(SSLVersion::VERSION_1_2));

    // TODO

    return output.build();
}

ByteBuffer TLSTransport::build_server_hello_done()
{
    FTP::ByteWriter output;

    output.write_1_bytes(static_cast<u8>(ContentType::HANDSHAKE));
    output.write_2_bytes(static_cast<u16>(SSLVersion::VERSION_1_2));
    u8 packet_size_offset = output.length();
    output.write_2_bytes(0);

    output.write_1_bytes(static_cast<u8>(HandshakeType::SERVER_HELLO_DONE));
    output.write_3_bytes(0); // 0 bytes of data

    output.set_2_bytes(packet_size_offset, output.length() - packet_size_offset - 2);
    return output.build();
}

String ClientHello::to_string()
{
    StringBuilder builder;

    builder.appendff("Client Hello:\n");
    builder.appendff("\tSSL Version: {}\n", enum_to_string(ssl_version));
    builder.appendff("\tClient random: {:hex-dump}\n", client_random.bytes());
    builder.appendff("\tSession ID: {:hex-dump}\n", session_id.bytes());
    builder.appendff("\tCipher suits:\n");
    for (auto cipher : cipher_suites) {
        builder.appendff("\t\t{}\n", enum_to_string(cipher));
    }
    builder.appendff("\tCompression methods:\n");
    for (auto compression : compression_methods) {
        builder.appendff("\t\t{}\n", enum_to_string(compression));
    }
    builder.appendff("\tExtensions:\n");
    for (auto& extension : extensions) {
        builder.appendff("{}", extension.to_string(2));
    }

    return builder.to_string();
}

String ClientKeyExchange::to_string()
{
    StringBuilder builder;

    builder.appendff("Client Key Exchange:\n");
    builder.appendff("\tPublic Key: {:hex-dump}\n", public_key.bytes());

    return builder.to_string();
}
