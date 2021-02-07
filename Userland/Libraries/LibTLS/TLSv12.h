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

#pragma once

#include "Certificate.h"
#include <AK/IPv4Address.h>
#include <AK/WeakPtr.h>
#include <LibCore/Notifier.h>
#include <LibCore/Socket.h>
#include <LibCore/TCPSocket.h>
#include <LibCrypto/Authentication/HMAC.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibCrypto/Hash/HashManager.h>
#include <LibCrypto/PK/RSA.h>
#include <LibTLS/TLSPacketBuilder.h>

namespace TLS {

inline void print_buffer(ReadonlyBytes buffer)
{
    StringBuilder builder(buffer.size() * 2);
    for (auto b : buffer)
        builder.appendff("{:02x} ", b);
    dbgln("{}", builder.string_view());
}

inline void print_buffer(const ByteBuffer& buffer)
{
    print_buffer(buffer.bytes());
}

inline void print_buffer(const u8* buffer, size_t size)
{
    print_buffer(ReadonlyBytes { buffer, size });
}

class Socket;

enum class CipherSuite {
    Invalid = 0,
    AES_128_GCM_SHA256 = 0x1301,
    AES_256_GCM_SHA384 = 0x1302,
    AES_128_CCM_SHA256 = 0x1304,
    AES_128_CCM_8_SHA256 = 0x1305,

    // We support these
    RSA_WITH_AES_128_CBC_SHA = 0x002F,
    RSA_WITH_AES_256_CBC_SHA = 0x0035,
    RSA_WITH_AES_128_CBC_SHA256 = 0x003C,
    RSA_WITH_AES_256_CBC_SHA256 = 0x003D,
    // TODO
    RSA_WITH_AES_128_GCM_SHA256 = 0x009C,
    RSA_WITH_AES_256_GCM_SHA384 = 0x009D,
};

#define ENUMERATE_ALERT_DESCRIPTIONS                        \
    ENUMERATE_ALERT_DESCRIPTION(CloseNotify, 0)             \
    ENUMERATE_ALERT_DESCRIPTION(UnexpectedMessage, 10)      \
    ENUMERATE_ALERT_DESCRIPTION(BadRecordMAC, 20)           \
    ENUMERATE_ALERT_DESCRIPTION(DecryptionFailed, 21)       \
    ENUMERATE_ALERT_DESCRIPTION(RecordOverflow, 22)         \
    ENUMERATE_ALERT_DESCRIPTION(DecompressionFailure, 30)   \
    ENUMERATE_ALERT_DESCRIPTION(HandshakeFailure, 40)       \
    ENUMERATE_ALERT_DESCRIPTION(NoCertificate, 41)          \
    ENUMERATE_ALERT_DESCRIPTION(BadCertificate, 42)         \
    ENUMERATE_ALERT_DESCRIPTION(UnsupportedCertificate, 43) \
    ENUMERATE_ALERT_DESCRIPTION(CertificateRevoked, 44)     \
    ENUMERATE_ALERT_DESCRIPTION(CertificateExpired, 45)     \
    ENUMERATE_ALERT_DESCRIPTION(CertificateUnknown, 46)     \
    ENUMERATE_ALERT_DESCRIPTION(IllegalParameter, 47)       \
    ENUMERATE_ALERT_DESCRIPTION(UnknownCA, 48)              \
    ENUMERATE_ALERT_DESCRIPTION(AccessDenied, 49)           \
    ENUMERATE_ALERT_DESCRIPTION(DecodeError, 50)            \
    ENUMERATE_ALERT_DESCRIPTION(DecryptError, 51)           \
    ENUMERATE_ALERT_DESCRIPTION(ExportRestriction, 60)      \
    ENUMERATE_ALERT_DESCRIPTION(ProtocolVersion, 70)        \
    ENUMERATE_ALERT_DESCRIPTION(InsufficientSecurity, 71)   \
    ENUMERATE_ALERT_DESCRIPTION(InternalError, 80)          \
    ENUMERATE_ALERT_DESCRIPTION(InappropriateFallback, 86)  \
    ENUMERATE_ALERT_DESCRIPTION(UserCanceled, 90)           \
    ENUMERATE_ALERT_DESCRIPTION(NoRenegotiation, 100)       \
    ENUMERATE_ALERT_DESCRIPTION(UnsupportedExtension, 110)  \
    ENUMERATE_ALERT_DESCRIPTION(NoError, 255)

enum class AlertDescription : u8 {
#define ENUMERATE_ALERT_DESCRIPTION(name, value) name = value,
    ENUMERATE_ALERT_DESCRIPTIONS
#undef ENUMERATE_ALERT_DESCRIPTION
};

constexpr static const char* alert_name(AlertDescription descriptor)
{
#define ENUMERATE_ALERT_DESCRIPTION(name, value) \
    case AlertDescription::name:                 \
        return #name;

    switch (descriptor) {
        ENUMERATE_ALERT_DESCRIPTIONS
    }

    return "Unknown";
#undef ENUMERATE_ALERT_DESCRIPTION
}

enum class Error : i8 {
    NoError = 0,
    UnknownError = -1,
    BrokenPacket = -2,
    NotUnderstood = -3,
    NoCommonCipher = -5,
    UnexpectedMessage = -6,
    CloseConnection = -7,
    CompressionNotSupported = -8,
    NotVerified = -9,
    NotSafe = -10,
    IntegrityCheckFailed = -11,
    ErrorAlert = -12,
    BrokenConnection = -13,
    BadCertificate = -14,
    UnsupportedCertificate = -15,
    NoRenegotiation = -16,
    FeatureNotSupported = -17,
    DecryptionFailed = -20,
    NeedMoreData = -21,
    TimedOut = -22,
};

enum class AlertLevel : u8 {
    Warning = 0x01,
    Critical = 0x02
};

enum HandshakeType {
    HelloRequest = 0x00,
    ClientHello = 0x01,
    ServerHello = 0x02,
    HelloVerifyRequest = 0x03,
    CertificateMessage = 0x0b,
    ServerKeyExchange = 0x0c,
    CertificateRequest = 0x0d,
    ServerHelloDone = 0x0e,
    CertificateVerify = 0x0f,
    ClientKeyExchange = 0x10,
    Finished = 0x14
};

enum class HandshakeExtension : u16 {
    ServerName = 0x00,
    ApplicationLayerProtocolNegotiation = 0x10,
    SignatureAlgorithms = 0x0d,
};

enum class WritePacketStage {
    Initial = 0,
    ClientHandshake = 1,
    ServerHandshake = 2,
    Finished = 3,
};

enum class ConnectionStatus {
    Disconnected,
    Negotiating,
    KeyExchange,
    Renegotiating,
    Established,
};

enum ClientVerificationStaus {
    Verified,
    VerificationNeeded,
};

struct Options {
#define OPTION_WITH_DEFAULTS(typ, name, ...)                    \
    static typ default_##name() { return typ { __VA_ARGS__ }; } \
    typ name = default_##name();

    OPTION_WITH_DEFAULTS(Vector<CipherSuite>, usable_cipher_suites,
        CipherSuite::RSA_WITH_AES_128_CBC_SHA256,
        CipherSuite::RSA_WITH_AES_256_CBC_SHA256,
        CipherSuite::RSA_WITH_AES_128_CBC_SHA,
        CipherSuite::RSA_WITH_AES_256_CBC_SHA,
        CipherSuite::RSA_WITH_AES_128_GCM_SHA256)

    OPTION_WITH_DEFAULTS(Version, version, Version::V12)

    OPTION_WITH_DEFAULTS(bool, use_sni, true)
    OPTION_WITH_DEFAULTS(bool, use_compression, false)
    OPTION_WITH_DEFAULTS(bool, validate_certificates, true)

#undef OPTION_WITH_DEFAULTS
};

struct Context {
    String to_string() const;
    bool verify() const;
    bool verify_chain() const;

    static void print_file(const StringView& fname);

    Options options;

    u8 remote_random[32];
    u8 local_random[32];
    u8 session_id[32];
    u8 session_id_size { 0 };
    CipherSuite cipher;
    bool is_server { false };
    Vector<Certificate> certificates;
    Certificate private_key;
    Vector<Certificate> client_certificates;
    ByteBuffer master_key;
    ByteBuffer premaster_key;
    u8 cipher_spec_set { 0 };
    struct {
        int created { 0 };
        u8 remote_mac[32];
        u8 local_mac[32];
        u8 local_iv[16];
        u8 remote_iv[16];
        u8 local_aead_iv[4];
        u8 remote_aead_iv[4];
    } crypto;

    Crypto::Hash::Manager handshake_hash;

    ByteBuffer message_buffer;
    u64 remote_sequence_number { 0 };
    u64 local_sequence_number { 0 };

    ConnectionStatus connection_status { ConnectionStatus::Disconnected };
    u8 critical_error { 0 };
    Error error_code { Error::NoError };

    ByteBuffer tls_buffer;

    ByteBuffer application_buffer;

    bool is_child { false };

    struct {
        // Server Name Indicator
        String SNI; // I hate your existence
    } extensions;

    u8 request_client_certificate { 0 };

    ByteBuffer cached_handshake;

    ClientVerificationStaus client_verified { Verified };

    bool connection_finished { false };

    // message flags
    u8 handshake_messages[11] { 0 };
    ByteBuffer user_data;
    Vector<Certificate> root_ceritificates;

    Vector<String> alpn;
    StringView negotiated_alpn;

    size_t send_retries { 0 };

    time_t handshake_initiation_timestamp { 0 };
};

class TLSv12 : public Core::Socket {
    C_OBJECT(TLSv12)
public:
    ByteBuffer& write_buffer() { return m_context.tls_buffer; }
    bool is_established() const { return m_context.connection_status == ConnectionStatus::Established; }
    virtual bool connect(const String&, int) override;

    void set_sni(const StringView& sni)
    {
        if (m_context.is_server || m_context.critical_error || m_context.connection_status != ConnectionStatus::Disconnected) {
            dbgln("invalid state for set_sni");
            return;
        }
        m_context.extensions.SNI = sni;
    }

    Optional<Certificate> parse_asn1(ReadonlyBytes, bool client_cert = false) const;
    bool load_certificates(ReadonlyBytes pem_buffer);
    bool load_private_key(ReadonlyBytes pem_buffer);

    void set_root_certificates(Vector<Certificate>);

    bool add_client_key(ReadonlyBytes certificate_pem_buffer, ReadonlyBytes key_pem_buffer);
    bool add_client_key(Certificate certificate)
    {
        m_context.client_certificates.append(move(certificate));
        return true;
    }

    ByteBuffer finish_build();

    const StringView& alpn() const { return m_context.negotiated_alpn; }
    void add_alpn(const StringView& alpn);
    bool has_alpn(const StringView& alpn) const;

    bool supports_cipher(CipherSuite suite) const
    {
        return suite == CipherSuite::RSA_WITH_AES_128_CBC_SHA256
            || suite == CipherSuite::RSA_WITH_AES_256_CBC_SHA256
            || suite == CipherSuite::RSA_WITH_AES_128_CBC_SHA
            || suite == CipherSuite::RSA_WITH_AES_256_CBC_SHA
            || suite == CipherSuite::RSA_WITH_AES_128_GCM_SHA256;
    }

    bool supports_version(Version v) const
    {
        return v == Version::V12;
    }

    Optional<ByteBuffer> read();
    ByteBuffer read(size_t max_size);

    bool write(ReadonlyBytes);
    void alert(AlertLevel, AlertDescription);

    bool can_read_line() const { return m_context.application_buffer.size() && memchr(m_context.application_buffer.data(), '\n', m_context.application_buffer.size()); }
    bool can_read() const { return m_context.application_buffer.size() > 0; }
    String read_line(size_t max_size);

    Function<void(TLSv12&)> on_tls_ready_to_read;
    Function<void(TLSv12&)> on_tls_ready_to_write;
    Function<void(AlertDescription)> on_tls_error;
    Function<void()> on_tls_connected;
    Function<void()> on_tls_finished;
    Function<void(TLSv12&)> on_tls_certificate_request;

private:
    explicit TLSv12(Core::Object* parent, Options = {});

    virtual bool common_connect(const struct sockaddr*, socklen_t) override;

    void consume(ReadonlyBytes record);

    ByteBuffer hmac_message(const ReadonlyBytes& buf, const Optional<ReadonlyBytes> buf2, size_t mac_length, bool local = false);
    void ensure_hmac(size_t digest_size, bool local);

    void update_packet(ByteBuffer& packet);
    void update_hash(ReadonlyBytes in, size_t header_size);

    void write_packet(ByteBuffer& packet);

    ByteBuffer build_client_key_exchange();
    ByteBuffer build_server_key_exchange();

    ByteBuffer build_hello();
    ByteBuffer build_finished();
    ByteBuffer build_certificate();
    ByteBuffer build_done();
    ByteBuffer build_alert(bool critical, u8 code);
    ByteBuffer build_change_cipher_spec();
    ByteBuffer build_verify_request();
    void build_random(PacketBuilder&);

    bool flush();
    void write_into_socket();
    void read_from_socket();

    bool check_connection_state(bool read);

    ssize_t handle_hello(ReadonlyBytes, WritePacketStage&);
    ssize_t handle_finished(ReadonlyBytes, WritePacketStage&);
    ssize_t handle_certificate(ReadonlyBytes);
    ssize_t handle_server_key_exchange(ReadonlyBytes);
    ssize_t handle_server_hello_done(ReadonlyBytes);
    ssize_t handle_verify(ReadonlyBytes);
    ssize_t handle_payload(ReadonlyBytes);
    ssize_t handle_message(ReadonlyBytes);
    ssize_t handle_random(ReadonlyBytes);

    size_t asn1_length(ReadonlyBytes, size_t* octets);

    void pseudorandom_function(Bytes output, ReadonlyBytes secret, const u8* label, size_t label_length, ReadonlyBytes seed, ReadonlyBytes seed_b);

    size_t key_length() const
    {
        switch (m_context.cipher) {
        case CipherSuite::AES_128_CCM_8_SHA256:
        case CipherSuite::AES_128_CCM_SHA256:
        case CipherSuite::AES_128_GCM_SHA256:
        case CipherSuite::Invalid:
        case CipherSuite::RSA_WITH_AES_128_CBC_SHA256:
        case CipherSuite::RSA_WITH_AES_128_CBC_SHA:
        case CipherSuite::RSA_WITH_AES_128_GCM_SHA256:
        default:
            return 128 / 8;
        case CipherSuite::AES_256_GCM_SHA384:
        case CipherSuite::RSA_WITH_AES_256_CBC_SHA:
        case CipherSuite::RSA_WITH_AES_256_CBC_SHA256:
        case CipherSuite::RSA_WITH_AES_256_GCM_SHA384:
            return 256 / 8;
        }
    }
    size_t mac_length() const
    {
        switch (m_context.cipher) {
        case CipherSuite::RSA_WITH_AES_128_CBC_SHA:
        case CipherSuite::RSA_WITH_AES_256_CBC_SHA:
            return Crypto::Hash::SHA1::digest_size();
        case CipherSuite::AES_256_GCM_SHA384:
        case CipherSuite::RSA_WITH_AES_256_GCM_SHA384:
            return Crypto::Hash::SHA512::digest_size();
        case CipherSuite::AES_128_CCM_8_SHA256:
        case CipherSuite::AES_128_CCM_SHA256:
        case CipherSuite::AES_128_GCM_SHA256:
        case CipherSuite::Invalid:
        case CipherSuite::RSA_WITH_AES_128_CBC_SHA256:
        case CipherSuite::RSA_WITH_AES_128_GCM_SHA256:
        case CipherSuite::RSA_WITH_AES_256_CBC_SHA256:
        default:
            return Crypto::Hash::SHA256::digest_size();
        }
    }
    size_t iv_length() const
    {
        switch (m_context.cipher) {
        case CipherSuite::AES_128_CCM_8_SHA256:
        case CipherSuite::AES_128_CCM_SHA256:
        case CipherSuite::Invalid:
        case CipherSuite::RSA_WITH_AES_128_CBC_SHA256:
        case CipherSuite::RSA_WITH_AES_128_CBC_SHA:
        case CipherSuite::RSA_WITH_AES_256_CBC_SHA256:
        case CipherSuite::RSA_WITH_AES_256_CBC_SHA:
        default:
            return 16;
        case CipherSuite::AES_128_GCM_SHA256:
        case CipherSuite::AES_256_GCM_SHA384:
        case CipherSuite::RSA_WITH_AES_128_GCM_SHA256:
        case CipherSuite::RSA_WITH_AES_256_GCM_SHA384:
            return 8; // 4 bytes of fixed IV, 8 random (nonce) bytes, 4 bytes for counter
                      // GCM specifically asks us to transmit only the nonce, the counter is zero
                      // and the fixed IV is derived from the premaster key.
        }
    }

    bool is_aead() const
    {
        switch (m_context.cipher) {
        case CipherSuite::AES_128_GCM_SHA256:
        case CipherSuite::AES_256_GCM_SHA384:
        case CipherSuite::RSA_WITH_AES_128_GCM_SHA256:
        case CipherSuite::RSA_WITH_AES_256_GCM_SHA384:
            return true;
        default:
            return false;
        }
    }

    bool expand_key();

    bool compute_master_secret(size_t length);

    Optional<size_t> verify_chain_and_get_matching_certificate(const StringView& host) const;

    void try_disambiguate_error() const;

    Context m_context;

    OwnPtr<Crypto::Authentication::HMAC<Crypto::Hash::Manager>> m_hmac_local;
    OwnPtr<Crypto::Authentication::HMAC<Crypto::Hash::Manager>> m_hmac_remote;

    struct {
        OwnPtr<Crypto::Cipher::AESCipher::CBCMode> cbc;
        OwnPtr<Crypto::Cipher::AESCipher::GCMMode> gcm;
    } m_aes_local, m_aes_remote;

    bool m_has_scheduled_write_flush { false };
    i32 m_max_wait_time_for_handshake_in_seconds { 10 };

    RefPtr<Core::Timer> m_handshake_timeout_timer;
};

namespace Constants {
constexpr static const u32 version_id[] { 1, 1, 1, 0 };
constexpr static const u32 pk_id[] { 1, 1, 7, 0 };
constexpr static const u32 serial_id[] { 1, 1, 2, 1, 0 };
constexpr static const u32 issurer_id[] { 1, 1, 4, 0 };
constexpr static const u32 owner_id[] { 1, 1, 6, 0 };
constexpr static const u32 validity_id[] { 1, 1, 5, 0 };
constexpr static const u32 algorithm_id[] { 1, 1, 3, 0 };
constexpr static const u32 sign_id[] { 1, 3, 2, 1, 0 };
constexpr static const u32 priv_id[] { 1, 4, 0 };
constexpr static const u32 priv_der_id[] { 1, 3, 1, 0 };
constexpr static const u32 ecc_priv_id[] { 1, 2, 0 };

constexpr static const u8 country_oid[] { 0x55, 0x04, 0x06, 0x00 };
constexpr static const u8 state_oid[] { 0x55, 0x04, 0x08, 0x00 };
constexpr static const u8 location_oid[] { 0x55, 0x04, 0x07, 0x00 };
constexpr static const u8 entity_oid[] { 0x55, 0x04, 0x0A, 0x00 };
constexpr static const u8 subject_oid[] { 0x55, 0x04, 0x03, 0x00 };
constexpr static const u8 unit_oid[] { 0x55, 0x04, 0x0B, 0x00 };
constexpr static const u8 san_oid[] { 0x55, 0x1D, 0x11, 0x00 };
constexpr static const u8 ocsp_oid[] { 0x2B, 0x06, 0x01, 0x05, 0x05, 0x07, 0x30, 0x01, 0x00 };

static constexpr const u8 RSA_SIGN_RSA_OID[] = { 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x00 };
static constexpr const u8 RSA_SIGN_MD5_OID[] = { 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x04, 0x00 };
static constexpr const u8 RSA_SIGN_SHA1_OID[] = { 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x05, 0x00 };
static constexpr const u8 RSA_SIGN_SHA256_OID[] = { 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x0b, 0x00 };
static constexpr const u8 RSA_SIGN_SHA384_OID[] = { 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x0c, 0x00 };
static constexpr const u8 RSA_SIGN_SHA512_OID[] = { 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x0d, 0x00 };

}

}
