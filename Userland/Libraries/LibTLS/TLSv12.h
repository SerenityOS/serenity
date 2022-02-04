/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Certificate.h"
#include <AK/IPv4Address.h>
#include <AK/WeakPtr.h>
#include <LibCore/Notifier.h>
#include <LibCore/Stream.h>
#include <LibCore/Timer.h>
#include <LibCrypto/Authentication/HMAC.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibCrypto/Hash/HashManager.h>
#include <LibCrypto/PK/RSA.h>
#include <LibTLS/CipherSuite.h>
#include <LibTLS/TLSPacketBuilder.h>

namespace TLS {

inline void print_buffer(ReadonlyBytes buffer)
{
    dbgln("{:hex-dump}", buffer);
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

enum class NameType : u8 {
    HostName = 0x00,
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

// Note for the 16 iv length instead of 8:
// 4 bytes of fixed IV, 8 random (nonce) bytes, 4 bytes for counter
// GCM specifically asks us to transmit only the nonce, the counter is zero
// and the fixed IV is derived from the premaster key.
#define ENUMERATE_CIPHERS(C)                                                                                                                          \
    C(true, CipherSuite::RSA_WITH_AES_128_CBC_SHA, KeyExchangeAlgorithm::RSA, CipherAlgorithm::AES_128_CBC, Crypto::Hash::SHA1, 16, false)            \
    C(true, CipherSuite::RSA_WITH_AES_256_CBC_SHA, KeyExchangeAlgorithm::RSA, CipherAlgorithm::AES_256_CBC, Crypto::Hash::SHA1, 16, false)            \
    C(true, CipherSuite::RSA_WITH_AES_128_CBC_SHA256, KeyExchangeAlgorithm::RSA, CipherAlgorithm::AES_128_CBC, Crypto::Hash::SHA256, 16, false)       \
    C(true, CipherSuite::RSA_WITH_AES_256_CBC_SHA256, KeyExchangeAlgorithm::RSA, CipherAlgorithm::AES_256_CBC, Crypto::Hash::SHA256, 16, false)       \
    C(true, CipherSuite::RSA_WITH_AES_128_GCM_SHA256, KeyExchangeAlgorithm::RSA, CipherAlgorithm::AES_128_GCM, Crypto::Hash::SHA256, 8, true)         \
    C(true, CipherSuite::RSA_WITH_AES_256_GCM_SHA384, KeyExchangeAlgorithm::RSA, CipherAlgorithm::AES_256_GCM, Crypto::Hash::SHA384, 8, true)         \
    C(true, CipherSuite::DHE_RSA_WITH_AES_128_GCM_SHA256, KeyExchangeAlgorithm::DHE_RSA, CipherAlgorithm::AES_128_GCM, Crypto::Hash::SHA256, 8, true) \
    C(true, CipherSuite::DHE_RSA_WITH_AES_256_GCM_SHA384, KeyExchangeAlgorithm::DHE_RSA, CipherAlgorithm::AES_256_GCM, Crypto::Hash::SHA384, 8, true)

constexpr KeyExchangeAlgorithm get_key_exchange_algorithm(CipherSuite suite)
{
    switch (suite) {
#define C(is_supported, suite, key_exchange, cipher, hash, iv_size, is_aead) \
    case suite:                                                              \
        return key_exchange;
        ENUMERATE_CIPHERS(C)
#undef C
    default:
        return KeyExchangeAlgorithm::Invalid;
    }
}

constexpr CipherAlgorithm get_cipher_algorithm(CipherSuite suite)
{
    switch (suite) {
#define C(is_supported, suite, key_exchange, cipher, hash, iv_size, is_aead) \
    case suite:                                                              \
        return cipher;
        ENUMERATE_CIPHERS(C)
#undef C
    default:
        return CipherAlgorithm::Invalid;
    }
}

struct Options {
    static Vector<CipherSuite> default_usable_cipher_suites()
    {
        Vector<CipherSuite> cipher_suites;
#define C(is_supported, suite, key_exchange, cipher, hash, iv_size, is_aead) \
    if constexpr (is_supported)                                              \
        cipher_suites.empend(suite);
        ENUMERATE_CIPHERS(C)
#undef C
        return cipher_suites;
    }
    Vector<CipherSuite> usable_cipher_suites = default_usable_cipher_suites();

#define OPTION_WITH_DEFAULTS(typ, name, ...)                    \
    static typ default_##name() { return typ { __VA_ARGS__ }; } \
    typ name = default_##name();                                \
    Options& set_##name(typ new_value)&                         \
    {                                                           \
        name = move(new_value);                                 \
        return *this;                                           \
    }                                                           \
    Options&& set_##name(typ new_value)&&                       \
    {                                                           \
        name = move(new_value);                                 \
        return move(*this);                                     \
    }

    OPTION_WITH_DEFAULTS(Version, version, Version::V12)
    OPTION_WITH_DEFAULTS(Vector<SignatureAndHashAlgorithm>, supported_signature_algorithms,
        { HashAlgorithm::SHA512, SignatureAlgorithm::RSA },
        { HashAlgorithm::SHA384, SignatureAlgorithm::RSA },
        { HashAlgorithm::SHA256, SignatureAlgorithm::RSA },
        { HashAlgorithm::SHA1, SignatureAlgorithm::RSA });

    OPTION_WITH_DEFAULTS(bool, use_sni, true)
    OPTION_WITH_DEFAULTS(bool, use_compression, false)
    OPTION_WITH_DEFAULTS(bool, validate_certificates, true)
    OPTION_WITH_DEFAULTS(Optional<Vector<Certificate>>, root_certificates, )
    OPTION_WITH_DEFAULTS(Function<void(AlertDescription)>, alert_handler, [](auto) {})
    OPTION_WITH_DEFAULTS(Function<void()>, finish_callback, [] {})
    OPTION_WITH_DEFAULTS(Function<Vector<Certificate>()>, certificate_provider, [] { return Vector<Certificate> {}; })

#undef OPTION_WITH_DEFAULTS
};

struct Context {
    bool verify_chain() const;

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
    bool close_notify { false };
    bool has_invoked_finish_or_error_callback { false };

    // message flags
    u8 handshake_messages[11] { 0 };
    ByteBuffer user_data;
    Vector<Certificate> root_certificates;

    Vector<String> alpn;
    StringView negotiated_alpn;

    size_t send_retries { 0 };

    time_t handshake_initiation_timestamp { 0 };

    struct {
        ByteBuffer p;
        ByteBuffer g;
        ByteBuffer Ys;
    } server_diffie_hellman_params;
};

class TLSv12 final : public Core::Stream::Socket {
private:
    Core::Stream::Socket& underlying_stream()
    {
        return *m_stream.visit([&](auto& stream) -> Core::Stream::Socket* { return stream; });
    }
    Core::Stream::Socket const& underlying_stream() const
    {
        return *m_stream.visit([&](auto& stream) -> Core::Stream::Socket const* { return stream; });
    }

public:
    virtual bool is_readable() const override { return true; }
    virtual bool is_writable() const override { return true; }

    /// Reads into a buffer, with the maximum size being the size of the buffer.
    /// The amount of bytes read can be smaller than the size of the buffer.
    /// Returns either the amount of bytes read, or an errno in the case of
    /// failure.
    virtual ErrorOr<size_t> read(Bytes) override;

    /// Tries to write the entire contents of the buffer. It is possible for
    /// less than the full buffer to be written. Returns either the amount of
    /// bytes written into the stream, or an errno in the case of failure.
    virtual ErrorOr<size_t> write(ReadonlyBytes) override;

    virtual bool is_eof() const override { return m_context.connection_finished && m_context.application_buffer.is_empty(); }

    virtual bool is_open() const override { return is_established(); }
    virtual void close() override;

    virtual ErrorOr<size_t> pending_bytes() const override { return m_context.application_buffer.size(); }
    virtual ErrorOr<bool> can_read_without_blocking(int = 0) const override { return !m_context.application_buffer.is_empty(); }
    virtual ErrorOr<void> set_blocking(bool block) override
    {
        VERIFY(!block);
        return {};
    }
    virtual ErrorOr<void> set_close_on_exec(bool enabled) override { return underlying_stream().set_close_on_exec(enabled); }

    virtual void set_notifications_enabled(bool enabled) override { underlying_stream().set_notifications_enabled(enabled); }

    static ErrorOr<NonnullOwnPtr<TLSv12>> connect(String const& host, u16 port, Options = {});
    static ErrorOr<NonnullOwnPtr<TLSv12>> connect(String const& host, Core::Stream::Socket& underlying_stream, Options = {});

    using StreamVariantType = Variant<OwnPtr<Core::Stream::Socket>, Core::Stream::Socket*>;
    explicit TLSv12(StreamVariantType, Options);

    bool is_established() const { return m_context.connection_status == ConnectionStatus::Established; }

    void set_sni(StringView sni)
    {
        if (m_context.is_server || m_context.critical_error || m_context.connection_status != ConnectionStatus::Disconnected) {
            dbgln("invalid state for set_sni");
            return;
        }
        m_context.extensions.SNI = sni;
    }

    bool load_certificates(ReadonlyBytes pem_buffer);
    bool load_private_key(ReadonlyBytes pem_buffer);

    void set_root_certificates(Vector<Certificate>);

    static Vector<Certificate> parse_pem_certificate(ReadonlyBytes certificate_pem_buffer, ReadonlyBytes key_pem_buffer);

    ByteBuffer finish_build();

    StringView alpn() const { return m_context.negotiated_alpn; }
    void add_alpn(StringView alpn);
    bool has_alpn(StringView alpn) const;

    bool supports_cipher(CipherSuite suite) const
    {
        switch (suite) {
#define C(is_supported, suite, key_exchange, cipher, hash, iv_size, is_aead) \
    case suite:                                                              \
        return is_supported;
            ENUMERATE_CIPHERS(C)
#undef C
        default:
            return false;
        }
    }

    bool supports_version(Version v) const
    {
        return v == Version::V12;
    }

    void alert(AlertLevel, AlertDescription);

    bool can_read_line() const { return m_context.application_buffer.size() && memchr(m_context.application_buffer.data(), '\n', m_context.application_buffer.size()); }
    bool can_read() const { return m_context.application_buffer.size() > 0; }
    String read_line(size_t max_size);

    Function<void(AlertDescription)> on_tls_error;
    Function<void()> on_tls_finished;
    Function<void(TLSv12&)> on_tls_certificate_request;
    Function<void()> on_connected;

private:
    void setup_connection();

    void consume(ReadonlyBytes record);

    ByteBuffer hmac_message(ReadonlyBytes buf, const Optional<ReadonlyBytes> buf2, size_t mac_length, bool local = false);
    void ensure_hmac(size_t digest_size, bool local);

    void update_packet(ByteBuffer& packet);
    void update_hash(ReadonlyBytes in, size_t header_size);

    void write_packet(ByteBuffer& packet);

    ByteBuffer build_client_key_exchange();
    ByteBuffer build_server_key_exchange();

    ByteBuffer build_hello();
    ByteBuffer build_handshake_finished();
    ByteBuffer build_certificate();
    ByteBuffer build_done();
    ByteBuffer build_alert(bool critical, u8 code);
    ByteBuffer build_change_cipher_spec();
    ByteBuffer build_verify_request();
    void build_rsa_pre_master_secret(PacketBuilder&);
    void build_dhe_rsa_pre_master_secret(PacketBuilder&);

    ErrorOr<bool> flush();
    void write_into_socket();
    ErrorOr<void> read_from_socket();

    bool check_connection_state(bool read);
    void notify_client_for_app_data();

    ssize_t handle_server_hello(ReadonlyBytes, WritePacketStage&);
    ssize_t handle_handshake_finished(ReadonlyBytes, WritePacketStage&);
    ssize_t handle_certificate(ReadonlyBytes);
    ssize_t handle_server_key_exchange(ReadonlyBytes);
    ssize_t handle_dhe_rsa_server_key_exchange(ReadonlyBytes);
    ssize_t handle_server_hello_done(ReadonlyBytes);
    ssize_t handle_certificate_verify(ReadonlyBytes);
    ssize_t handle_handshake_payload(ReadonlyBytes);
    ssize_t handle_message(ReadonlyBytes);
    ssize_t handle_random(ReadonlyBytes);

    void pseudorandom_function(Bytes output, ReadonlyBytes secret, const u8* label, size_t label_length, ReadonlyBytes seed, ReadonlyBytes seed_b);

    size_t key_length() const
    {
        switch (m_context.cipher) {
#define C(is_supported, suite, key_exchange, cipher, hash, iv_size, is_aead) \
    case suite:                                                              \
        return cipher_key_size(cipher) / 8;
            ENUMERATE_CIPHERS(C)
#undef C
        default:
            return 128 / 8;
        }
    }

    size_t mac_length() const
    {
        switch (m_context.cipher) {
#define C(is_supported, suite, key_exchange, cipher, hash, iv_size, is_aead) \
    case suite:                                                              \
        return hash ::digest_size();
            ENUMERATE_CIPHERS(C)
#undef C
        default:
            return Crypto::Hash::SHA256::digest_size();
        }
    }

    Crypto::Hash::HashKind hmac_hash() const
    {
        switch (mac_length()) {
        case Crypto::Hash::SHA512::DigestSize:
            return Crypto::Hash::HashKind::SHA512;
        case Crypto::Hash::SHA384::DigestSize:
            return Crypto::Hash::HashKind::SHA384;
        case Crypto::Hash::SHA256::DigestSize:
        case Crypto::Hash::SHA1::DigestSize:
        default:
            return Crypto::Hash::HashKind::SHA256;
        }
    }

    size_t iv_length() const
    {
        switch (m_context.cipher) {
#define C(is_supported, suite, key_exchange, cipher, hash, iv_size, is_aead) \
    case suite:                                                              \
        return iv_size;
            ENUMERATE_CIPHERS(C)
#undef C
        default:
            return 16;
        }
    }

    bool is_aead() const
    {
        switch (m_context.cipher) {
#define C(is_supported, suite, key_exchange, cipher, hash, iv_size, is_aead) \
    case suite:                                                              \
        return is_aead;
            ENUMERATE_CIPHERS(C)
#undef C
        default:
            return false;
        }
    }

    bool expand_key();

    bool compute_master_secret_from_pre_master_secret(size_t length);

    Optional<size_t> verify_chain_and_get_matching_certificate(StringView host) const;

    void try_disambiguate_error() const;

    bool m_eof { false };
    StreamVariantType m_stream;
    Context m_context;

    OwnPtr<Crypto::Authentication::HMAC<Crypto::Hash::Manager>> m_hmac_local;
    OwnPtr<Crypto::Authentication::HMAC<Crypto::Hash::Manager>> m_hmac_remote;

    using CipherVariant = Variant<
        Empty,
        Crypto::Cipher::AESCipher::CBCMode,
        Crypto::Cipher::AESCipher::GCMMode>;
    CipherVariant m_cipher_local {};
    CipherVariant m_cipher_remote {};

    bool m_has_scheduled_write_flush { false };
    bool m_has_scheduled_app_data_flush { false };
    i32 m_max_wait_time_for_handshake_in_seconds { 10 };

    RefPtr<Core::Timer> m_handshake_timeout_timer;
};

}
