/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Endian.h>
#include <AK/Random.h>

#include <LibCore/Timer.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

namespace TLS {

ssize_t TLSv12::handle_server_hello_done(const ByteBuffer& buffer)
{
    if (buffer.size() < 3)
        return (i8)Error::NeedMoreData;

    size_t size = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];

    if (buffer.size() - 3 < size)
        return (i8)Error::NeedMoreData;

    return size + 3;
}

ssize_t TLSv12::handle_hello(const ByteBuffer& buffer, WritePacketStage& write_packets)
{
    write_packets = WritePacketStage::Initial;
    if (m_context.connection_status != ConnectionStatus::Disconnected && m_context.connection_status != ConnectionStatus::Renegotiating) {
        dbg() << "unexpected hello message";
        return (i8)Error::UnexpectedMessage;
    }
    ssize_t res = 0;
    size_t min_hello_size = 41;

    if (min_hello_size > buffer.size()) {
        dbg() << "need more data";
        return (i8)Error::NeedMoreData;
    }
    size_t following_bytes = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];
    res += 3;
    if (buffer.size() - res < following_bytes) {
        dbg() << "not enough data after header: " << buffer.size() - res << " < " << following_bytes;
        return (i8)Error::NeedMoreData;
    }

    if (buffer.size() - res < 2) {
        dbg() << "not enough data for version";
        return (i8)Error::NeedMoreData;
    }
    auto version = (Version)AK::convert_between_host_and_network_endian(*(const u16*)buffer.offset_pointer(res));

    res += 2;
    if (!supports_version(version))
        return (i8)Error::NotSafe;

    memcpy(m_context.remote_random, buffer.offset_pointer(res), sizeof(m_context.remote_random));
    res += sizeof(m_context.remote_random);

    u8 session_length = buffer[res++];
    if (buffer.size() - res < session_length) {
        dbg() << "not enough data for session id";
        return (i8)Error::NeedMoreData;
    }

    if (session_length && session_length <= 32) {
        memcpy(m_context.session_id, buffer.offset_pointer(res), session_length);
        m_context.session_id_size = session_length;
#ifdef TLS_DEBUG
        dbg() << "Remote session ID:";
        print_buffer(ByteBuffer::wrap(m_context.session_id, session_length));
#endif
    } else {
        m_context.session_id_size = 0;
    }
    res += session_length;

    if (buffer.size() - res < 2) {
        dbg() << "not enough data for cipher suite listing";
        return (i8)Error::NeedMoreData;
    }
    auto cipher = (CipherSuite)AK::convert_between_host_and_network_endian(*(const u16*)buffer.offset_pointer(res));
    res += 2;
    if (!supports_cipher(cipher)) {
        m_context.cipher = CipherSuite::Invalid;
        dbg() << "No supported cipher could be agreed upon";
        return (i8)Error::NoCommonCipher;
    }
    m_context.cipher = cipher;
#ifdef TLS_DEBUG
    dbg() << "Cipher: " << (u16)cipher;
#endif

    // The handshake hash function is _always_ SHA256
    m_context.handshake_hash.initialize(Crypto::Hash::HashKind::SHA256);

    if (buffer.size() - res < 1) {
        dbg() << "not enough data for compression spec";
        return (i8)Error::NeedMoreData;
    }
    u8 compression = buffer[res++];
    if (compression != 0) {
        dbg() << "Server told us to compress, we will not!";
        return (i8)Error::CompressionNotSupported;
    }

    if (res > 0) {
        if (m_context.connection_status != ConnectionStatus::Renegotiating)
            m_context.connection_status = ConnectionStatus::Negotiating;
        if (m_context.is_server) {
            dbg() << "unsupported: server mode";
            write_packets = WritePacketStage::ServerHandshake;
        }
    }

    if (res > 2) {
        res += 2;
    }

    while ((ssize_t)buffer.size() - res >= 4) {
        auto extension_type = (HandshakeExtension)AK::convert_between_host_and_network_endian(*(const u16*)buffer.offset_pointer(res));
        res += 2;
        u16 extension_length = AK::convert_between_host_and_network_endian(*(const u16*)buffer.offset_pointer(res));
        res += 2;

#ifdef TLS_DEBUG
        dbg() << "extension " << (u16)extension_type << " with length " << extension_length;
#endif
        if (extension_length) {
            if (buffer.size() - res < extension_length) {
                dbg() << "not enough data for extension";
                return (i8)Error::NeedMoreData;
            }

            // SNI
            if (extension_type == HandshakeExtension::ServerName) {
                u16 sni_host_length = AK::convert_between_host_and_network_endian(*(const u16*)buffer.offset_pointer(res + 3));
                if (buffer.size() - res - 5 < sni_host_length) {
                    dbg() << "Not enough data for sni " << (buffer.size() - res - 5) << " < " << sni_host_length;
                    return (i8)Error::NeedMoreData;
                }

                if (sni_host_length) {
                    m_context.SNI = String { (const char*)buffer.offset_pointer(res + 5), sni_host_length };
                    dbg() << "server name indicator: " << m_context.SNI;
                }
            } else if (extension_type == HandshakeExtension::ApplicationLayerProtocolNegotiation && m_context.alpn.size()) {
                if (buffer.size() - res > 2) {
                    auto alpn_length = AK::convert_between_host_and_network_endian(*(const u16*)buffer.offset_pointer(res));
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
                                dbg() << "negotiated alpn: " << alpn_str;
                                break;
                            }
                            alpn_position += alpn_length;
                            if (!m_context.is_server) // server hello must contain one ALPN
                                break;
                        }
                    }
                }
            } else if (extension_type == HandshakeExtension::SignatureAlgorithms) {
                dbg() << "supported signatures: ";
                print_buffer(buffer.slice_view(res, extension_length));
                // FIXME: what are we supposed to do here?
            }
            res += extension_length;
        }
    }

    return res;
}

ssize_t TLSv12::handle_finished(const ByteBuffer& buffer, WritePacketStage& write_packets)
{
    if (m_context.connection_status < ConnectionStatus::KeyExchange || m_context.connection_status == ConnectionStatus::Established) {
        dbg() << "unexpected finished message";
        return (i8)Error::UnexpectedMessage;
    }

    write_packets = WritePacketStage::Initial;

    if (buffer.size() < 3) {
        return (i8)Error::NeedMoreData;
    }

    size_t index = 3;

    u32 size = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];

    if (size < 12) {
#ifdef TLS_DEBUG
        dbg() << "finished packet smaller than minimum size: " << size;
#endif
        return (i8)Error::BrokenPacket;
    }

    if (size < buffer.size() - index) {
#ifdef TLS_DEBUG
        dbg() << "not enough data after length: " << size << " > " << buffer.size() - index;
#endif
        return (i8)Error::NeedMoreData;
    }

// TODO: Compare Hashes
#ifdef TLS_DEBUG
    dbg() << "FIXME: handle_finished :: Check message validity";
#endif
    m_context.connection_status = ConnectionStatus::Established;

    if (m_handshake_timeout_timer) {
        // Disable the handshake timeout timer as handshake has been established.
        m_handshake_timeout_timer->stop();
        m_handshake_timeout_timer->remove_from_parent();
        m_handshake_timeout_timer = nullptr;
    }

    if (on_tls_ready_to_write)
        on_tls_ready_to_write(*this);

    return index + size;
}

void TLSv12::build_random(PacketBuilder& builder)
{
    u8 random_bytes[48];
    size_t bytes = 48;

    AK::fill_with_random(random_bytes, bytes);

    // remove zeros from the random bytes
    for (size_t i = 0; i < bytes; ++i) {
        if (!random_bytes[i])
            random_bytes[i--] = AK::get_random<u8>();
    }

    if (m_context.is_server) {
        dbg() << "Server mode not supported";
        return;
    } else {
        *(u16*)random_bytes = AK::convert_between_host_and_network_endian((u16)Version::V12);
    }

    m_context.premaster_key = ByteBuffer::copy(random_bytes, bytes);

    const auto& certificate = m_context.certificates[0];
#ifdef TLS_DEBUG
    dbg() << "PreMaster secret";
    print_buffer(m_context.premaster_key);
#endif

    Crypto::PK::RSA_PKCS1_EME rsa(certificate.public_key.modulus(), 0, certificate.public_key.public_exponent());

    u8 out[rsa.output_size()];
    auto outbuf = ByteBuffer::wrap(out, rsa.output_size());
    rsa.encrypt(m_context.premaster_key, outbuf);

#ifdef TLS_DEBUG
    dbg() << "Encrypted: ";
    print_buffer(outbuf);
#endif

    if (!compute_master_secret(bytes)) {
        dbg() << "oh noes we could not derive a master key :(";
        return;
    }

    builder.append_u24(outbuf.size() + 2);
    builder.append((u16)outbuf.size());
    builder.append(outbuf);
}

ssize_t TLSv12::handle_payload(const ByteBuffer& vbuffer)
{
    if (m_context.connection_status == ConnectionStatus::Established) {
#ifdef TLS_DEBUG
        dbg() << "Renegotiation attempt ignored";
#endif
        // FIXME: We should properly say "NoRenegotiation", but that causes a handshake failure
        //        so we just roll with it and pretend that we _did_ renegotiate
        //        This will cause issues when we decide to have long-lasting connections, but
        //        we do not have those at the moment :^)
        return 1;
    }
    auto buffer = vbuffer;
    auto buffer_length = buffer.size();
    auto original_length = buffer_length;
    while (buffer_length >= 4 && !m_context.critical_error) {
        ssize_t payload_res = 0;
        if (buffer_length < 1)
            return (i8)Error::NeedMoreData;
        auto type = buffer[0];
        auto write_packets { WritePacketStage::Initial };
        size_t payload_size = buffer[1] * 0x10000 + buffer[2] * 0x100 + buffer[3] + 3;
#ifdef TLS_DEBUG
        dbg() << "payload size: " << payload_size << " buffer length: " << buffer_length;
#endif
        if (payload_size + 1 > buffer_length)
            return (i8)Error::NeedMoreData;

        switch (type) {
        case HelloRequest:
            if (m_context.handshake_messages[0] >= 1) {
                dbg() << "unexpected hello request message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[0];
            dbg() << "hello request (renegotiation?)";
            if (m_context.connection_status == ConnectionStatus::Established) {
                // renegotiation
                payload_res = (i8)Error::NoRenegotiation;
            } else {
                // :shrug:
                payload_res = (i8)Error::UnexpectedMessage;
            }
            break;
        case ClientHello:
            // FIXME: We only support client mode right now
            if (m_context.is_server) {
                ASSERT_NOT_REACHED();
            }
            payload_res = (i8)Error::UnexpectedMessage;
            break;
        case ServerHello:
            if (m_context.handshake_messages[2] >= 1) {
                dbg() << "unexpected server hello message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[2];
#ifdef TLS_DEBUG
            dbg() << "server hello";
#endif
            if (m_context.is_server) {
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                payload_res = handle_hello(buffer.slice_view(1, payload_size), write_packets);
            }
            break;
        case HelloVerifyRequest:
            dbg() << "unsupported: DTLS";
            payload_res = (i8)Error::UnexpectedMessage;
            break;
        case CertificateMessage:
            if (m_context.handshake_messages[4] >= 1) {
                dbg() << "unexpected certificate message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[4];
#ifdef TLS_DEBUG
            dbg() << "certificate";
#endif
            if (m_context.connection_status == ConnectionStatus::Negotiating) {
                if (m_context.is_server) {
                    dbg() << "unsupported: server mode";
                    ASSERT_NOT_REACHED();
                }
                payload_res = handle_certificate(buffer.slice_view(1, payload_size));
                if (m_context.certificates.size()) {
                    auto it = m_context.certificates.find([&](auto& cert) { return cert.is_valid(); });

                    if (it.is_end()) {
                        // no valid certificates
                        dbg() << "No valid certificates found";
                        payload_res = (i8)Error::BadCertificate;
                        m_context.critical_error = payload_res;
                        break;
                    }

                    // swap the first certificate with the valid one
                    if (it.index() != 0)
                        swap(m_context.certificates[0], m_context.certificates[it.index()]);
                }
            } else {
                payload_res = (i8)Error::UnexpectedMessage;
            }
            break;
        case ServerKeyExchange:
            if (m_context.handshake_messages[5] >= 1) {
                dbg() << "unexpected server key exchange message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[5];
#ifdef TLS_DEBUG
            dbg() << "server key exchange";
#endif
            if (m_context.is_server) {
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                payload_res = handle_server_key_exchange(buffer.slice_view(1, payload_size));
            }
            break;
        case CertificateRequest:
            if (m_context.handshake_messages[6] >= 1) {
                dbg() << "unexpected certificate request message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[6];
            if (m_context.is_server) {
                dbg() << "invalid request";
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                // we do not support "certificate request"
                dbg() << "certificate request";
                if (on_tls_certificate_request)
                    on_tls_certificate_request(*this);
                m_context.client_verified = VerificationNeeded;
            }
            break;
        case ServerHelloDone:
            if (m_context.handshake_messages[7] >= 1) {
                dbg() << "unexpected server hello done message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[7];
#ifdef TLS_DEBUG
            dbg() << "server hello done";
#endif
            if (m_context.is_server) {
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                payload_res = handle_server_hello_done(buffer.slice_view(1, payload_size));
                if (payload_res > 0)
                    write_packets = WritePacketStage::ClientHandshake;
            }
            break;
        case CertificateVerify:
            if (m_context.handshake_messages[8] >= 1) {
                dbg() << "unexpected certificate verify message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[8];
#ifdef TLS_DEBUG
            dbg() << "certificate verify";
#endif
            if (m_context.connection_status == ConnectionStatus::KeyExchange) {
                payload_res = handle_verify(buffer.slice_view(1, payload_size));
            } else {
                payload_res = (i8)Error::UnexpectedMessage;
            }
            break;
        case ClientKeyExchange:
            if (m_context.handshake_messages[9] >= 1) {
                dbg() << "unexpected client key exchange message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[9];
#ifdef TLS_DEBUG
            dbg() << "client key exchange";
#endif
            if (m_context.is_server) {
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                payload_res = (i8)Error::UnexpectedMessage;
            }
            break;
        case Finished:
            if (m_context.cached_handshake) {
                m_context.cached_handshake.clear();
            }
            if (m_context.handshake_messages[10] >= 1) {
                dbg() << "unexpected finished message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[10];
#ifdef TLS_DEBUG
            dbg() << "finished";
#endif
            payload_res = handle_finished(buffer.slice_view(1, payload_size), write_packets);
            if (payload_res > 0) {
                memset(m_context.handshake_messages, 0, sizeof(m_context.handshake_messages));
            }
            break;
        default:
            dbg() << "message type not understood: " << type;
            return (i8)Error::NotUnderstood;
        }

        if (type != HelloRequest) {
            update_hash(buffer.slice_view(0, payload_size + 1));
        }

        // if something went wrong, send an alert about it
        if (payload_res < 0) {
            switch ((Error)payload_res) {
            case Error::UnexpectedMessage: {
                auto packet = build_alert(true, (u8)AlertDescription::UnexpectedMessage);
                write_packet(packet);
                break;
            }
            case Error::CompressionNotSupported: {
                auto packet = build_alert(true, (u8)AlertDescription::DecompressionFailure);
                write_packet(packet);
                break;
            }
            case Error::BrokenPacket: {
                auto packet = build_alert(true, (u8)AlertDescription::DecodeError);
                write_packet(packet);
                break;
            }
            case Error::NotVerified: {
                auto packet = build_alert(true, (u8)AlertDescription::BadRecordMAC);
                write_packet(packet);
                break;
            }
            case Error::BadCertificate: {
                auto packet = build_alert(true, (u8)AlertDescription::BadCertificate);
                write_packet(packet);
                break;
            }
            case Error::UnsupportedCertificate: {
                auto packet = build_alert(true, (u8)AlertDescription::UnsupportedCertificate);
                write_packet(packet);
                break;
            }
            case Error::NoCommonCipher: {
                auto packet = build_alert(true, (u8)AlertDescription::InsufficientSecurity);
                write_packet(packet);
                break;
            }
            case Error::NotUnderstood: {
                auto packet = build_alert(true, (u8)AlertDescription::InternalError);
                write_packet(packet);
                break;
            }
            case Error::NoRenegotiation: {
                auto packet = build_alert(true, (u8)AlertDescription::NoRenegotiation);
                write_packet(packet);
                break;
            }
            case Error::DecryptionFailed: {
                auto packet = build_alert(true, (u8)AlertDescription::DecryptionFailed);
                write_packet(packet);
                break;
            }
            case Error::NeedMoreData:
                // Ignore this, as it's not an "error"
                break;
            default:
                dbg() << "Unknown TLS::Error with value " << payload_res;
                ASSERT_NOT_REACHED();
                break;
            }
            if (payload_res < 0)
                return payload_res;
        }
        switch (write_packets) {
        case WritePacketStage::Initial:
            // nothing to write
            break;
        case WritePacketStage::ClientHandshake:
            if (m_context.client_verified == VerificationNeeded) {
#ifdef TLS_DEBUG
                dbg() << "> Client Certificate";
#endif
                auto packet = build_certificate();
                write_packet(packet);
                m_context.client_verified = Verified;
            }
            {
#ifdef TLS_DEBUG
                dbg() << "> Key exchange";
#endif
                auto packet = build_client_key_exchange();
                write_packet(packet);
            }
            {
#ifdef TLS_DEBUG
                dbg() << "> change cipher spec";
#endif
                auto packet = build_change_cipher_spec();
                write_packet(packet);
            }
            m_context.cipher_spec_set = 1;
            m_context.local_sequence_number = 0;
            {
#ifdef TLS_DEBUG
                dbg() << "> client finished";
#endif
                auto packet = build_finished();
                write_packet(packet);
            }
            m_context.cipher_spec_set = 0;
            break;
        case WritePacketStage::ServerHandshake:
            // server handshake
            dbg() << "UNSUPPORTED: Server mode";
            ASSERT_NOT_REACHED();
            break;
        case WritePacketStage::Finished:
            // finished
            {
#ifdef TLS_DEBUG
                dbg() << "> change cipher spec";
#endif
                auto packet = build_change_cipher_spec();
                write_packet(packet);
            }
            {
#ifdef TLS_DEBUG
                dbg() << "> client finished";
#endif
                auto packet = build_finished();
                write_packet(packet);
            }
            m_context.connection_status = ConnectionStatus::Established;
            break;
        }
        payload_size++;
        buffer_length -= payload_size;
        buffer = buffer.slice(payload_size, buffer_length);
    }
    return original_length;
}

}
