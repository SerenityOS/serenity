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

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <LibCore/Timer.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

namespace TLS {

void TLSv12::write_packet(ByteBuffer& packet)
{
    m_context.tls_buffer.append(packet.data(), packet.size());
    if (m_context.connection_status > ConnectionStatus::Disconnected) {
        if (!m_has_scheduled_write_flush) {
            dbgln_if(TLS_DEBUG, "Scheduling write of {}", m_context.tls_buffer.size());
            deferred_invoke([this](auto&) { write_into_socket(); });
            m_has_scheduled_write_flush = true;
        } else {
            // multiple packet are available, let's flush some out
            dbgln_if(TLS_DEBUG, "Flushing scheduled write of {}", m_context.tls_buffer.size());
            write_into_socket();
            // the deferred invoke is still in place
            m_has_scheduled_write_flush = true;
        }
    }
}

void TLSv12::update_packet(ByteBuffer& packet)
{
    u32 header_size = 5;
    *(u16*)packet.offset_pointer(3) = AK::convert_between_host_and_network_endian((u16)(packet.size() - header_size));

    if (packet[0] != (u8)MessageType::ChangeCipher) {
        if (packet[0] == (u8)MessageType::Handshake && packet.size() > header_size) {
            u8 handshake_type = packet[header_size];
            if (handshake_type != HandshakeType::HelloRequest && handshake_type != HandshakeType::HelloVerifyRequest) {
                update_hash(packet.bytes().slice(header_size, packet.size() - header_size));
            }
        }
        if (m_context.cipher_spec_set && m_context.crypto.created) {
            size_t length = packet.size() - header_size;
            size_t block_size, padding, mac_size;

            if (!is_aead()) {
                block_size = m_aes_local.cbc->cipher().block_size();
                // If the length is already a multiple a block_size,
                // an entire block of padding is added.
                // In short, we _never_ have no padding.
                mac_size = mac_length();
                length += mac_size;
                padding = block_size - length % block_size;
                length += padding;
            } else {
                block_size = m_aes_local.gcm->cipher().block_size();
                padding = 0;
                mac_size = 0; // AEAD provides its own authentication scheme.
            }

            if (m_context.crypto.created == 1) {
                // `buffer' will continue to be encrypted
                auto buffer = ByteBuffer::create_uninitialized(length);
                size_t buffer_position = 0;
                auto iv_size = iv_length();

                // copy the packet, sans the header
                buffer.overwrite(buffer_position, packet.offset_pointer(header_size), packet.size() - header_size);
                buffer_position += packet.size() - header_size;

                ByteBuffer ct;

                if (is_aead()) {
                    // We need enough space for a header, the data, a tag, and the IV
                    ct = ByteBuffer::create_uninitialized(length + header_size + iv_size + 16);

                    // copy the header over
                    ct.overwrite(0, packet.data(), header_size - 2);

                    // AEAD AAD (13)
                    // Seq. no (8)
                    // content type (1)
                    // version (2)
                    // length (2)
                    u8 aad[13];
                    Bytes aad_bytes { aad, 13 };
                    OutputMemoryStream aad_stream { aad_bytes };

                    u64 seq_no = AK::convert_between_host_and_network_endian(m_context.local_sequence_number);
                    u16 len = AK::convert_between_host_and_network_endian((u16)(packet.size() - header_size));

                    aad_stream.write({ &seq_no, sizeof(seq_no) });
                    aad_stream.write(packet.bytes().slice(0, 3)); // content-type + version
                    aad_stream.write({ &len, sizeof(len) });      // length
                    VERIFY(aad_stream.is_end());

                    // AEAD IV (12)
                    // IV (4)
                    // (Nonce) (8)
                    // -- Our GCM impl takes 16 bytes
                    // zero (4)
                    u8 iv[16];
                    Bytes iv_bytes { iv, 16 };
                    Bytes { m_context.crypto.local_aead_iv, 4 }.copy_to(iv_bytes);
                    AK::fill_with_random(iv_bytes.offset(4), 8);
                    memset(iv_bytes.offset(12), 0, 4);

                    // write the random part of the iv out
                    iv_bytes.slice(4, 8).copy_to(ct.bytes().slice(header_size));

                    // Write the encrypted data and the tag
                    m_aes_local.gcm->encrypt(
                        packet.bytes().slice(header_size, length),
                        ct.bytes().slice(header_size + 8, length),
                        iv_bytes,
                        aad_bytes,
                        ct.bytes().slice(header_size + 8 + length, 16));

                    VERIFY(header_size + 8 + length + 16 == ct.size());

                } else {
                    // We need enough space for a header, iv_length bytes of IV and whatever the packet contains
                    ct = ByteBuffer::create_uninitialized(length + header_size + iv_size);

                    // copy the header over
                    ct.overwrite(0, packet.data(), header_size - 2);

                    // get the appropricate HMAC value for the entire packet
                    auto mac = hmac_message(packet, {}, mac_size, true);

                    // write the MAC
                    buffer.overwrite(buffer_position, mac.data(), mac.size());
                    buffer_position += mac.size();

                    // Apply the padding (a packet MUST always be padded)
                    memset(buffer.offset_pointer(buffer_position), padding - 1, padding);
                    buffer_position += padding;

                    VERIFY(buffer_position == buffer.size());

                    auto iv = ByteBuffer::create_uninitialized(iv_size);
                    AK::fill_with_random(iv.data(), iv.size());

                    // write it into the ciphertext portion of the message
                    ct.overwrite(header_size, iv.data(), iv.size());

                    VERIFY(header_size + iv_size + length == ct.size());
                    VERIFY(length % block_size == 0);

                    // get a block to encrypt into
                    auto view = ct.bytes().slice(header_size + iv_size, length);
                    m_aes_local.cbc->encrypt(buffer, view, iv);
                }

                // store the correct ciphertext length into the packet
                u16 ct_length = (u16)ct.size() - header_size;

                *(u16*)ct.offset_pointer(header_size - 2) = AK::convert_between_host_and_network_endian(ct_length);

                // replace the packet with the ciphertext
                packet = ct;
            }
        }
    }
    ++m_context.local_sequence_number;
}

void TLSv12::update_hash(ReadonlyBytes message)
{
    m_context.handshake_hash.update(message);
}

ByteBuffer TLSv12::hmac_message(const ReadonlyBytes& buf, const Optional<ReadonlyBytes> buf2, size_t mac_length, bool local)
{
    u64 sequence_number = AK::convert_between_host_and_network_endian(local ? m_context.local_sequence_number : m_context.remote_sequence_number);
    ensure_hmac(mac_length, local);
    auto& hmac = local ? *m_hmac_local : *m_hmac_remote;
#if TLS_DEBUG
    dbgln("========================= PACKET DATA ==========================");
    print_buffer((const u8*)&sequence_number, sizeof(u64));
    print_buffer(buf.data(), buf.size());
    if (buf2.has_value())
        print_buffer(buf2.value().data(), buf2.value().size());
    dbgln("========================= PACKET DATA ==========================");
#endif
    hmac.update((const u8*)&sequence_number, sizeof(u64));
    hmac.update(buf);
    if (buf2.has_value() && buf2.value().size()) {
        hmac.update(buf2.value());
    }
    auto digest = hmac.digest();
    auto mac = ByteBuffer::copy(digest.immutable_data(), digest.data_length());

    if constexpr (TLS_DEBUG) {
        dbgln("HMAC of the block for sequence number {}", sequence_number);
        print_buffer(mac);
    }

    return mac;
}

ssize_t TLSv12::handle_message(ReadonlyBytes buffer)
{
    auto res { 5ll };
    size_t header_size = res;
    ssize_t payload_res = 0;

    dbgln_if(TLS_DEBUG, "buffer size: {}", buffer.size());

    if (buffer.size() < 5) {
        return (i8)Error::NeedMoreData;
    }

    auto type = (MessageType)buffer[0];
    size_t buffer_position { 1 };

    // FIXME: Read the version and verify it

    if constexpr (TLS_DEBUG) {
        auto version = (Version) * (const u16*)buffer.offset_pointer(buffer_position);
        dbgln("type={}, version={}", (u8)type, (u16)version);
    }

    buffer_position += 2;

    auto length = AK::convert_between_host_and_network_endian(*(const u16*)buffer.offset_pointer(buffer_position));
    dbgln_if(TLS_DEBUG, "record length: {} at offset: {}", length, buffer_position);
    buffer_position += 2;

    if (buffer_position + length > buffer.size()) {
        dbgln_if(TLS_DEBUG, "record length more than what we have: {}", buffer.size());
        return (i8)Error::NeedMoreData;
    }

    dbgln_if(TLS_DEBUG, "message type: {}, length: {}", (u8)type, length);
    auto plain = buffer.slice(buffer_position, buffer.size() - buffer_position);

    ByteBuffer decrypted;

    if (m_context.cipher_spec_set && type != MessageType::ChangeCipher) {
        if constexpr (TLS_DEBUG) {
            dbgln("Encrypted: ");
            print_buffer(buffer.slice(header_size, length));
        }

        if (is_aead()) {
            VERIFY(m_aes_remote.gcm);

            if (length < 24) {
                dbgln("Invalid packet length");
                auto packet = build_alert(true, (u8)AlertDescription::DecryptError);
                write_packet(packet);
                return (i8)Error::BrokenPacket;
            }

            auto packet_length = length - iv_length() - 16;
            auto payload = plain;
            decrypted = ByteBuffer::create_uninitialized(packet_length);

            // AEAD AAD (13)
            // Seq. no (8)
            // content type (1)
            // version (2)
            // length (2)
            u8 aad[13];
            Bytes aad_bytes { aad, 13 };
            OutputMemoryStream aad_stream { aad_bytes };

            u64 seq_no = AK::convert_between_host_and_network_endian(m_context.remote_sequence_number);
            u16 len = AK::convert_between_host_and_network_endian((u16)packet_length);

            aad_stream.write({ &seq_no, sizeof(seq_no) });      // Sequence number
            aad_stream.write(buffer.slice(0, header_size - 2)); // content-type + version
            aad_stream.write({ &len, sizeof(u16) });
            VERIFY(aad_stream.is_end());

            auto nonce = payload.slice(0, iv_length());
            payload = payload.slice(iv_length());

            // AEAD IV (12)
            // IV (4)
            // (Nonce) (8)
            // -- Our GCM impl takes 16 bytes
            // zero (4)
            u8 iv[16];
            Bytes iv_bytes { iv, 16 };
            Bytes { m_context.crypto.remote_aead_iv, 4 }.copy_to(iv_bytes);
            nonce.copy_to(iv_bytes.slice(4));
            memset(iv_bytes.offset(12), 0, 4);

            auto ciphertext = payload.slice(0, payload.size() - 16);
            auto tag = payload.slice(ciphertext.size());

            auto consistency = m_aes_remote.gcm->decrypt(
                ciphertext,
                decrypted,
                iv_bytes,
                aad_bytes,
                tag);

            if (consistency != Crypto::VerificationConsistency::Consistent) {
                dbgln("integrity check failed (tag length {})", tag.size());
                auto packet = build_alert(true, (u8)AlertDescription::BadRecordMAC);
                write_packet(packet);

                return (i8)Error::IntegrityCheckFailed;
            }

            plain = decrypted;
        } else {
            VERIFY(m_aes_remote.cbc);
            auto iv_size = iv_length();

            decrypted = m_aes_remote.cbc->create_aligned_buffer(length - iv_size);
            auto iv = buffer.slice(header_size, iv_size);

            Bytes decrypted_span = decrypted;
            m_aes_remote.cbc->decrypt(buffer.slice(header_size + iv_size, length - iv_size), decrypted_span, iv);

            length = decrypted_span.size();

#if TLS_DEBUG
            dbgln("Decrypted: ");
            print_buffer(decrypted);
#endif

            auto mac_size = mac_length();
            if (length < mac_size) {
                dbgln("broken packet");
                auto packet = build_alert(true, (u8)AlertDescription::DecryptError);
                write_packet(packet);
                return (i8)Error::BrokenPacket;
            }

            length -= mac_size;

            const u8* message_hmac = decrypted_span.offset(length);
            u8 temp_buf[5];
            memcpy(temp_buf, buffer.offset_pointer(0), 3);
            *(u16*)(temp_buf + 3) = AK::convert_between_host_and_network_endian(length);
            auto hmac = hmac_message({ temp_buf, 5 }, decrypted_span.slice(0, length), mac_size);
            auto message_mac = ReadonlyBytes { message_hmac, mac_size };
            if (hmac != message_mac) {
                dbgln("integrity check failed (mac length {})", mac_size);
                dbgln("mac received:");
                print_buffer(message_mac);
                dbgln("mac computed:");
                print_buffer(hmac);
                auto packet = build_alert(true, (u8)AlertDescription::BadRecordMAC);
                write_packet(packet);

                return (i8)Error::IntegrityCheckFailed;
            }
            plain = decrypted.bytes().slice(0, length);
        }
    }
    m_context.remote_sequence_number++;

    switch (type) {
    case MessageType::ApplicationData:
        if (m_context.connection_status != ConnectionStatus::Established) {
            dbgln("unexpected application data");
            payload_res = (i8)Error::UnexpectedMessage;
            auto packet = build_alert(true, (u8)AlertDescription::UnexpectedMessage);
            write_packet(packet);
        } else {
            dbgln_if(TLS_DEBUG, "application data message of size {}", plain.size());

            m_context.application_buffer.append(plain.data(), plain.size());
        }
        break;
    case MessageType::Handshake:
#if TLS_DEBUG
        dbgln("tls handshake message");
#endif
        payload_res = handle_payload(plain);
        break;
    case MessageType::ChangeCipher:
        if (m_context.connection_status != ConnectionStatus::KeyExchange) {
            dbgln("unexpected change cipher message");
            auto packet = build_alert(true, (u8)AlertDescription::UnexpectedMessage);
            payload_res = (i8)Error::UnexpectedMessage;
        } else {
#if TLS_DEBUG
            dbgln("change cipher spec message");
#endif
            m_context.cipher_spec_set = true;
            m_context.remote_sequence_number = 0;
        }
        break;
    case MessageType::Alert:
        dbgln_if(TLS_DEBUG, "alert message of length {}", length);
        if (length >= 2) {
            if constexpr (TLS_DEBUG)
                print_buffer(plain);

            auto level = plain[0];
            auto code = plain[1];
            if (level == (u8)AlertLevel::Critical) {
                dbgln("We were alerted of a critical error: {} ({})", code, alert_name((AlertDescription)code));
                m_context.critical_error = code;
                try_disambiguate_error();
                res = (i8)Error::UnknownError;
            } else {
                dbgln("Alert: {}", code);
            }
            if (code == 0) {
                // close notify
                res += 2;
                alert(AlertLevel::Critical, AlertDescription::CloseNotify);
                m_context.connection_finished = true;
                if (!m_context.cipher_spec_set) {
                    // AWS CloudFront hits this.
                    dbgln("Server sent a close notify and we haven't agreed on a cipher suite. Treating it as a handshake failure.");
                    m_context.critical_error = (u8)AlertDescription::HandshakeFailure;
                    try_disambiguate_error();
                }
            }
            m_context.error_code = (Error)code;
        }
        break;
    default:
        dbgln("message not understood");
        return (i8)Error::NotUnderstood;
    }

    if (payload_res < 0)
        return payload_res;

    if (res > 0)
        return header_size + length;

    return res;
}

}
