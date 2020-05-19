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

#include <LibCore/Timer.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

namespace TLS {

void TLSv12::write_packet(ByteBuffer& packet)
{
    m_context.tls_buffer.append(packet.data(), packet.size());
    if (!m_has_scheduled_write_flush && m_context.connection_status > ConnectionStatus::Disconnected) {
#ifdef TLS_DEBUG
        dbg() << "Scheduling write of " << m_context.tls_buffer.size();
#endif
        deferred_invoke([this](auto&) { write_into_socket(); });
    }
}

void TLSv12::update_packet(ByteBuffer& packet)
{
    u32 header_size = 5;
    *(u16*)packet.offset_pointer(3) = convert_between_host_and_network((u16)(packet.size() - header_size));

    if (packet[0] != (u8)MessageType::ChangeCipher) {
        if (packet[0] == (u8)MessageType::Handshake && packet.size() > header_size) {
            u8 handshake_type = packet[header_size];
            if (handshake_type != HandshakeType::HelloRequest && handshake_type != HandshakeType::HelloVerifyRequest) {
                update_hash(packet.slice_view(header_size, packet.size() - header_size));
            }
        }
        if (m_context.cipher_spec_set && m_context.crypto.created) {
            size_t length = packet.size() - header_size + mac_length();
            auto block_size = m_aes_local->cipher().block_size();
            // if length is a multiple of block size, pad it up again
            // since it seems no one handles aligned unpadded blocks
            size_t padding = 0;
            if (length % block_size == 0) {
                padding = block_size;
                length += padding;
            }
            size_t mac_size = mac_length();

            if (m_context.crypto.created == 1) {
                // `buffer' will continue to be encrypted
                auto buffer = ByteBuffer::create_zeroed(length);
                size_t buffer_position = 0;
                u16 aligned_length = length + block_size - length % block_size;
                auto iv_size = iv_length();

                // we need enough space for a header, iv_length bytes of IV and whatever the packet contains
                auto ct = ByteBuffer::create_zeroed(aligned_length + header_size + iv_size);

                // copy the header over
                ct.overwrite(0, packet.data(), header_size - 2);

                // copy the packet, sans the header
                buffer.overwrite(buffer_position, packet.offset_pointer(header_size), packet.size() - header_size);
                buffer_position += packet.size() - header_size;

                // get the appropricate HMAC value for the entire packet
                auto mac = hmac_message(packet, {}, mac_size, true);

                // write the MAC
                buffer.overwrite(buffer_position, mac.data(), mac.size());
                buffer_position += mac.size();

                // if there's some padding to be done (since a packet MUST always be padded)
                // apply it manually
                if (padding) {
                    memset(buffer.offset_pointer(buffer_position), padding - 1, padding);
                    buffer_position += padding;
                }

                // should be the same value, but the manual padding
                // throws a wrench into our plans
                buffer.trim(buffer_position);

                // FIXME: REALLY Should be filled with random bytes
                auto iv = ByteBuffer::create_zeroed(iv_size);

                // write it into the ciphertext portion of the message
                ct.overwrite(header_size, iv.data(), iv.size());
                ct.trim(length + block_size - length % block_size + header_size + block_size - padding);

                // get a block to encrypt into
                auto view = ct.slice_view(header_size + iv_size, length + block_size - length % block_size + block_size - padding - iv_size);

                // encrypt the message
                m_aes_local->encrypt(buffer, view, iv);

                // store the correct ciphertext length into the packet
                u16 ct_length = (u16)ct.size() - header_size;
                *(u16*)ct.offset_pointer(header_size - 2) = convert_between_host_and_network(ct_length);

                // replace the packet with the ciphertext
                packet = ct;
            }
        }
    }
    ++m_context.local_sequence_number;
}

void TLSv12::update_hash(const ByteBuffer& message)
{
    m_context.handshake_hash.update(message);
}

ByteBuffer TLSv12::hmac_message(const ByteBuffer& buf, const Optional<ByteBuffer> buf2, size_t mac_length, bool local)
{
    u64 sequence_number = convert_between_host_and_network(local ? m_context.local_sequence_number : m_context.remote_sequence_number);
    ensure_hmac(mac_length, local);
    auto& hmac = local ? *m_hmac_local : *m_hmac_remote;
#ifdef TLS_DEBUG
    dbg() << "========================= PACKET DATA ==========================";
    print_buffer((const u8*)&sequence_number, sizeof(u64));
    print_buffer(buf);
    if (buf2.has_value())
        print_buffer(buf2.value());
    dbg() << "========================= PACKET DATA ==========================";
#endif
    hmac.update((const u8*)&sequence_number, sizeof(u64));
    hmac.update(buf);
    if (buf2.has_value() && buf2.value().size()) {
        hmac.update(buf2.value());
    }
    auto digest = hmac.digest();
    auto mac = ByteBuffer::copy(digest.immutable_data(), digest.data_length());
#ifdef TLS_DEBUG
    dbg() << "HMAC of the block for sequence number " << m_context.local_sequence_number;
    print_buffer(mac);
#endif
    return mac;
}

ssize_t TLSv12::handle_message(const ByteBuffer& buffer)
{
    auto res { 5ll };
    size_t header_size = res;
    ssize_t payload_res = 0;

#ifdef TLS_DEBUG
    dbg() << "buffer size: " << buffer.size();
#endif
    if (buffer.size() < 5) {
        return (i8)Error::NeedMoreData;
    }

    auto type = (MessageType)buffer[0];
    size_t buffer_position { 1 };

    // FIXME: Read the version and verify it
#ifdef TLS_DEBUG
    auto version = (Version) * (const u16*)buffer.offset_pointer(buffer_position);
    dbg() << "type: " << (u8)type << " version: " << (u16)version;
#endif
    buffer_position += 2;

    auto length = convert_between_host_and_network(*(const u16*)buffer.offset_pointer(buffer_position));
#ifdef TLS_DEBUG
    dbg() << "record length: " << length << " at offset: " << buffer_position;
#endif
    buffer_position += 2;

    if (buffer_position + length > buffer.size()) {
        dbg() << "record length more than what we have: " << buffer.size();
        return (i8)Error::NeedMoreData;
    }

#ifdef TLS_DEBUG
    dbg() << "message type: " << (u8)type << ", length: " << length;
#endif
    ByteBuffer plain = buffer.slice_view(buffer_position, buffer.size() - buffer_position);

    if (m_context.cipher_spec_set && type != MessageType::ChangeCipher) {
#ifdef TLS_DEBUG
        dbg() << "Encrypted: ";
        print_buffer(buffer.slice_view(header_size, length));
#endif

        ASSERT(m_aes_remote);
        auto iv_size = iv_length();

        auto decrypted = m_aes_remote->create_aligned_buffer(length - iv_size);
        auto iv = buffer.slice_view(header_size, iv_size);

        m_aes_remote->decrypt(buffer.slice_view(header_size + iv_size, length - iv_size), decrypted, iv);

        length = decrypted.size();

#ifdef TLS_DEBUG
        dbg() << "Decrypted: ";
        print_buffer(decrypted);
#endif

        auto mac_size = mac_length();
        if (length < mac_size) {
            dbg() << "broken packet";
            auto packet = build_alert(true, (u8)AlertDescription::DecryptError);
            write_packet(packet);
            return (i8)Error::BrokenPacket;
        }

        const u8* message_hmac = decrypted.offset_pointer(length - mac_size);
        u8 temp_buf[5];
        memcpy(temp_buf, buffer.offset_pointer(0), 3);
        *(u16*)(temp_buf + 3) = convert_between_host_and_network(length);
        auto hmac = hmac_message(ByteBuffer::wrap(temp_buf, 5), decrypted, mac_size);
        auto message_mac = ByteBuffer::wrap(message_hmac, mac_size);
        if (hmac != message_mac) {
            dbg() << "integrity check failed (mac length " << length << ")";
            dbg() << "mac received:";
            print_buffer(message_mac);
            dbg() << "mac computed:";
            print_buffer(hmac);
            auto packet = build_alert(true, (u8)AlertDescription::BadRecordMAC);
            write_packet(packet);

            return (i8)Error::IntegrityCheckFailed;
        }
        plain = decrypted.slice(0, length - mac_size);
    }
    m_context.remote_sequence_number++;

    switch (type) {
    case MessageType::ApplicationData:
        if (m_context.connection_status != ConnectionStatus::Established) {
            dbg() << "unexpected application data";
            payload_res = (i8)Error::UnexpectedMessage;
            auto packet = build_alert(true, (u8)AlertDescription::UnexpectedMessage);
            write_packet(packet);
        } else {
#ifdef TLS_DEBUG
            dbg() << "application data message of size " << plain.size();
#endif

            m_context.application_buffer.append(plain.data(), plain.size());
        }
        break;
    case MessageType::Handshake:
#ifdef TLS_DEBUG
        dbg() << "tls handshake message";
#endif
        payload_res = handle_payload(plain);
        break;
    case MessageType::ChangeCipher:
        if (m_context.connection_status != ConnectionStatus::KeyExchange) {
            dbg() << "unexpected change cipher message";
            auto packet = build_alert(true, (u8)AlertDescription::UnexpectedMessage);
            payload_res = (i8)Error::UnexpectedMessage;
        } else {
#ifdef TLS_DEBUG
            dbg() << "change cipher spec message";
#endif
            m_context.cipher_spec_set = true;
            m_context.remote_sequence_number = 0;
        }
        break;
    case MessageType::Alert:
        dbg() << "alert message of length " << length;
        if (length >= 2) {
            print_buffer(plain);
            auto level = plain[0];
            auto code = plain[1];
            if (level == (u8)AlertLevel::Critical) {
                dbg() << "We were alerted of a critical error: " << code << " (" << alert_name((AlertDescription)code) << ")";
                m_context.critical_error = code;
                try_disambiguate_error();
                res = (i8)Error::UnknownError;
            } else {
                dbg() << "Alert: " << code;
            }
            if (code == 0) {
                // close notify
                res += 2;
                auto closure_alert = build_alert(true, (u8)AlertDescription::CloseNotify);
                write_packet(closure_alert);
                flush();
                m_context.connection_finished = true;
            }
            m_context.error_code = (Error)code;
        }
        break;
    default:
        dbg() << "message not understood";
        return (i8)Error::NotUnderstood;
    }

    if (payload_res < 0)
        return payload_res;

    if (res > 0)
        return header_size + length;

    return res;
}

}
