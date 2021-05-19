/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Random.h>

#include <LibCore/Timer.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

namespace TLS {

ssize_t TLSv12::handle_server_hello(ReadonlyBytes buffer, WritePacketStage& write_packets)
{
    write_packets = WritePacketStage::Initial;
    if (m_context.connection_status != ConnectionStatus::Disconnected && m_context.connection_status != ConnectionStatus::Renegotiating) {
        dbgln("unexpected hello message");
        return (i8)Error::UnexpectedMessage;
    }
    ssize_t res = 0;
    size_t min_hello_size = 41;

    if (min_hello_size > buffer.size()) {
        dbgln("need more data");
        return (i8)Error::NeedMoreData;
    }
    size_t following_bytes = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];
    res += 3;
    if (buffer.size() - res < following_bytes) {
        dbgln("not enough data after header: {} < {}", buffer.size() - res, following_bytes);
        return (i8)Error::NeedMoreData;
    }

    if (buffer.size() - res < 2) {
        dbgln("not enough data for version");
        return (i8)Error::NeedMoreData;
    }
    auto version = static_cast<Version>(AK::convert_between_host_and_network_endian(ByteReader::load16(buffer.offset_pointer(res))));

    res += 2;
    if (!supports_version(version))
        return (i8)Error::NotSafe;

    memcpy(m_context.remote_random, buffer.offset_pointer(res), sizeof(m_context.remote_random));
    res += sizeof(m_context.remote_random);

    u8 session_length = buffer[res++];
    if (buffer.size() - res < session_length) {
        dbgln("not enough data for session id");
        return (i8)Error::NeedMoreData;
    }

    if (session_length && session_length <= 32) {
        memcpy(m_context.session_id, buffer.offset_pointer(res), session_length);
        m_context.session_id_size = session_length;
        if constexpr (TLS_DEBUG) {
            dbgln("Remote session ID:");
            print_buffer(ReadonlyBytes { m_context.session_id, session_length });
        }
    } else {
        m_context.session_id_size = 0;
    }
    res += session_length;

    if (buffer.size() - res < 2) {
        dbgln("not enough data for cipher suite listing");
        return (i8)Error::NeedMoreData;
    }
    auto cipher = static_cast<CipherSuite>(AK::convert_between_host_and_network_endian(ByteReader::load16(buffer.offset_pointer(res))));
    res += 2;
    if (!supports_cipher(cipher)) {
        m_context.cipher = CipherSuite::Invalid;
        dbgln("No supported cipher could be agreed upon");
        return (i8)Error::NoCommonCipher;
    }
    m_context.cipher = cipher;
    dbgln_if(TLS_DEBUG, "Cipher: {}", (u16)cipher);

    // The handshake hash function is _always_ SHA256
    m_context.handshake_hash.initialize(Crypto::Hash::HashKind::SHA256);

    // Compression method
    if (buffer.size() - res < 1)
        return (i8)Error::NeedMoreData;
    u8 compression = buffer[res++];
    if (compression != 0)
        return (i8)Error::CompressionNotSupported;

    if (m_context.connection_status != ConnectionStatus::Renegotiating)
        m_context.connection_status = ConnectionStatus::Negotiating;
    if (m_context.is_server) {
        dbgln("unsupported: server mode");
        write_packets = WritePacketStage::ServerHandshake;
    }

    // Presence of extensions is determined by availability of bytes after compression_method
    if (buffer.size() - res >= 2) {
        auto extensions_bytes_total = AK::convert_between_host_and_network_endian(ByteReader::load16(buffer.offset_pointer(res += 2)));
        dbgln_if(TLS_DEBUG, "Extensions bytes total: {}", extensions_bytes_total);
    }

    while (buffer.size() - res >= 4) {
        auto extension_type = (HandshakeExtension)AK::convert_between_host_and_network_endian(ByteReader::load16(buffer.offset_pointer(res)));
        res += 2;
        u16 extension_length = AK::convert_between_host_and_network_endian(ByteReader::load16(buffer.offset_pointer(res)));
        res += 2;

        dbgln_if(TLS_DEBUG, "Extension {} with length {}", (u16)extension_type, extension_length);

        if (buffer.size() - res < extension_length)
            return (i8)Error::NeedMoreData;

        if (extension_type == HandshakeExtension::ServerName) {
            // RFC6066 section 3: SNI extension_data can be empty in the server hello
            if (extension_length > 0) {
                // ServerNameList total size
                if (buffer.size() - res < 2)
                    return (i8)Error::NeedMoreData;
                auto sni_name_list_bytes = AK::convert_between_host_and_network_endian(ByteReader::load16(buffer.offset_pointer(res += 2)));
                dbgln_if(TLS_DEBUG, "SNI: expecting ServerNameList of {} bytes", sni_name_list_bytes);

                // Exactly one ServerName should be present
                if (buffer.size() - res < 3)
                    return (i8)Error::NeedMoreData;
                auto sni_name_type = (NameType)(*(const u8*)buffer.offset_pointer(res++));
                auto sni_name_length = AK::convert_between_host_and_network_endian(ByteReader::load16(buffer.offset_pointer(res += 2)));

                if (sni_name_type != NameType::HostName)
                    return (i8)Error::NotUnderstood;

                if (sizeof(sni_name_type) + sizeof(sni_name_length) + sni_name_length != sni_name_list_bytes)
                    return (i8)Error::BrokenPacket;

                // Read out the host_name
                if (buffer.size() - res < sni_name_length)
                    return (i8)Error::NeedMoreData;
                m_context.extensions.SNI = String { (const char*)buffer.offset_pointer(res), sni_name_length };
                res += sni_name_length;
                dbgln("SNI host_name: {}", m_context.extensions.SNI);
            }
        } else if (extension_type == HandshakeExtension::ApplicationLayerProtocolNegotiation && m_context.alpn.size()) {
            if (buffer.size() - res > 2) {
                auto alpn_length = AK::convert_between_host_and_network_endian(ByteReader::load16(buffer.offset_pointer(res)));
                if (alpn_length && alpn_length <= extension_length - 2) {
                    const u8* alpn = buffer.offset_pointer(res + 2);
                    size_t alpn_position = 0;
                    while (alpn_position < alpn_length) {
                        u8 alpn_size = alpn[alpn_position++];
                        if (alpn_size + alpn_position >= extension_length)
                            break;
                        String alpn_str { (const char*)alpn + alpn_position, alpn_length };
                        if (alpn_size && m_context.alpn.contains_slow(alpn_str)) {
                            m_context.negotiated_alpn = alpn_str;
                            dbgln("negotiated alpn: {}", alpn_str);
                            break;
                        }
                        alpn_position += alpn_length;
                        if (!m_context.is_server) // server hello must contain one ALPN
                            break;
                    }
                }
            }
            res += extension_length;
        } else if (extension_type == HandshakeExtension::SignatureAlgorithms) {
            dbgln("supported signatures: ");
            print_buffer(buffer.slice(res, extension_length));
            res += extension_length;
            // FIXME: what are we supposed to do here?
        } else {
            dbgln("Encountered unknown extension {} with length {}", (u16)extension_type, extension_length);
            res += extension_length;
        }
    }

    return res;
}

ssize_t TLSv12::handle_server_hello_done(ReadonlyBytes buffer)
{
    if (buffer.size() < 3)
        return (i8)Error::NeedMoreData;

    size_t size = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];

    if (buffer.size() - 3 < size)
        return (i8)Error::NeedMoreData;

    return size + 3;
}

ByteBuffer TLSv12::build_server_key_exchange()
{
    dbgln("FIXME: build_server_key_exchange");
    return {};
}

ssize_t TLSv12::handle_server_key_exchange(ReadonlyBytes)
{
    switch (get_signature_algorithm(m_context.cipher)) {
    case SignatureAlgorithm::Anonymous:
        dbgln("Client key exchange for Anonymous signature is not implemented");
        TODO();
        break;
    case SignatureAlgorithm::RSA:
    case SignatureAlgorithm::DSA:
        // RFC 5246 section 7.4.3. Server Key Exchange Message
        // It is not legal to send the server key exchange message for RSA, DH_DSS, DH_RSA
        dbgln("Server key exchange received for RSA or DSA is not legal");
        return (i8)Error::UnexpectedMessage;
    case SignatureAlgorithm::ECDSA:
        dbgln("Client key exchange for ECDSA signature is not implemented");
        TODO();
        break;
    default:
        dbgln("Unknonwn client key exchange signature algorithm");
        VERIFY_NOT_REACHED();
        break;
    }
    return 0;
}

}
