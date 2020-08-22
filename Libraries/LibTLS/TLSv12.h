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

inline void print_buffer(const ByteBuffer& buffer)
{
    for (size_t i { 0 }; i < buffer.size(); ++i)
        dbgprintf("%02x ", buffer[i]);
    dbgprintf("\n");
}

inline void print_buffer(const u8* buffer, size_t size)
{
    for (size_t i { 0 }; i < size; ++i)
        dbgprintf("%02x ", buffer[i]);
    dbgprintf("\n");
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

enum class CertificateKeyAlgorithm {
    Unsupported = 0x00,
    RSA_RSA = 0x01,
    RSA_MD5 = 0x04,
    RSA_SHA1 = 0x05,
    RSA_SHA256 = 0x0b,
    RSA_SHA512 = 0x0d,
};

struct Certificate {
    u16 version;
    CertificateKeyAlgorithm algorithm;
    CertificateKeyAlgorithm key_algorithm;
    CertificateKeyAlgorithm ec_algorithm;
    ByteBuffer exponent;
    Crypto::PK::RSAPublicKey<Crypto::UnsignedBigInteger> public_key;
    Crypto::PK::RSAPrivateKey<Crypto::UnsignedBigInteger> private_key;
    String issuer_country;
    String issuer_state;
    String issuer_location;
    String issuer_entity;
    String issuer_subject;
    String not_before;
    String not_after;
    String country;
    String state;
    String location;
    String entity;
    String subject;
    u8** SAN;
    u16 SAN_length;
    u8* ocsp;
    Crypto::UnsignedBigInteger serial_number;
    ByteBuffer sign_key;
    ByteBuffer fingerprint;
    ByteBuffer der;
    ByteBuffer data;

    bool is_valid() const;
};

struct Context {
    String to_string() const;
    bool verify() const;
    bool verify_chain() const;

    static void print_file(const StringView& fname);

    u8 remote_random[32];
    // To be predictable
    u8 local_random[32];
    u8 session_id[32];
    u8 session_id_size { 0 };
    CipherSuite cipher;
    Version version;
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

    String SNI; // I hate your existence

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
            dbg() << "invalid state for set_sni";
            return;
        }
        m_context.SNI = sni;
    }

    Optional<Certificate> parse_asn1(const ByteBuffer& buffer, bool client_cert = false) const;
    bool load_certificates(const ByteBuffer& pem_buffer);
    bool load_private_key(const ByteBuffer& pem_buffer);

    bool add_client_key(const ByteBuffer& certificate_pem_buffer, const ByteBuffer& key_pem_buffer);
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
        return suite == CipherSuite::RSA_WITH_AES_128_CBC_SHA256 || suite == CipherSuite::RSA_WITH_AES_256_CBC_SHA256 || suite == CipherSuite::RSA_WITH_AES_128_CBC_SHA || suite == CipherSuite::RSA_WITH_AES_256_CBC_SHA;
    }

    bool supports_version(Version v) const
    {
        return v == Version::V12;
    }

    Optional<ByteBuffer> read();
    ByteBuffer read(size_t max_size);

    bool write(const ByteBuffer& buffer);
    void alert(AlertLevel, AlertDescription);

    bool can_read_line() const { return m_context.application_buffer.size() && memchr(m_context.application_buffer.data(), '\n', m_context.application_buffer.size()); }
    bool can_read() const { return m_context.application_buffer.size() > 0; }
    ByteBuffer read_line(size_t max_size);

    Function<void(TLSv12&)> on_tls_ready_to_read;
    Function<void(TLSv12&)> on_tls_ready_to_write;
    Function<void(AlertDescription)> on_tls_error;
    Function<void()> on_tls_connected;
    Function<void()> on_tls_finished;
    Function<void(TLSv12&)> on_tls_certificate_request;

private:
    explicit TLSv12(Core::Object* parent, Version version = Version::V12);

    virtual bool common_connect(const struct sockaddr*, socklen_t) override;

    void consume(const ByteBuffer& record);

    ByteBuffer hmac_message(const ReadonlyBytes& buf, const Optional<ReadonlyBytes> buf2, size_t mac_length, bool local = false);
    void ensure_hmac(size_t digest_size, bool local);

    void update_packet(ByteBuffer& packet);
    void update_hash(const ByteBuffer& in);

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

    ssize_t handle_hello(const ByteBuffer& buffer, WritePacketStage&);
    ssize_t handle_finished(const ByteBuffer& buffer, WritePacketStage&);
    ssize_t handle_certificate(const ByteBuffer& buffer);
    ssize_t handle_server_key_exchange(const ByteBuffer& buffer);
    ssize_t handle_server_hello_done(const ByteBuffer& buffer);
    ssize_t handle_verify(const ByteBuffer& buffer);
    ssize_t handle_payload(const ByteBuffer& buffer);
    ssize_t handle_message(const ByteBuffer& buffer);
    ssize_t handle_random(const ByteBuffer& buffer);

    size_t asn1_length(const ByteBuffer& buffer, size_t* octets);

    void pseudorandom_function(ByteBuffer& output, const ByteBuffer& secret, const u8* label, size_t label_length, const ByteBuffer& seed, const ByteBuffer& seed_b);

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
            return 12;
        }
    }

    bool expand_key();

    bool compute_master_secret(size_t length);

    void try_disambiguate_error() const;

    Context m_context;

    OwnPtr<Crypto::Authentication::HMAC<Crypto::Hash::Manager>> m_hmac_local;
    OwnPtr<Crypto::Authentication::HMAC<Crypto::Hash::Manager>> m_hmac_remote;

    OwnPtr<Crypto::Cipher::AESCipher::CBCMode> m_aes_local;
    OwnPtr<Crypto::Cipher::AESCipher::CBCMode> m_aes_remote;

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
