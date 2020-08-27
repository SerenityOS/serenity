/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Random.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCrypto/Authentication/HMAC.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibCrypto/Checksum/Adler32.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibCrypto/Hash/MD5.h>
#include <LibCrypto/Hash/SHA1.h>
#include <LibCrypto/Hash/SHA2.h>
#include <LibCrypto/PK/RSA.h>
#include <LibLine/Editor.h>
#include <LibTLS/TLSv12.h>
#include <limits.h>
#include <stdio.h>
#include <time.h>

static const char* secret_key = "WellHelloFreinds";
static const char* suite = nullptr;
static const char* filename = nullptr;
static const char* server = nullptr;
static int key_bits = 128;
static bool binary = false;
static bool interactive = false;
static bool run_tests = false;
static int port = 443;
static bool in_ci = false;

static struct timeval start_time {
    0, 0
};
static bool g_some_test_failed = false;
static bool encrypting = true;

constexpr const char* DEFAULT_DIGEST_SUITE { "HMAC-SHA256" };
constexpr const char* DEFAULT_CHECKSUM_SUITE { "CRC32" };
constexpr const char* DEFAULT_HASH_SUITE { "SHA256" };
constexpr const char* DEFAULT_CIPHER_SUITE { "AES_CBC" };
constexpr const char* DEFAULT_SERVER { "www.google.com" };

// listAllTests
// Cipher
static int aes_cbc_tests();
static int aes_ctr_tests();

// Hash
static int md5_tests();
static int sha1_tests();
static int sha256_tests();
static int sha512_tests();

// Authentication
static int hmac_md5_tests();
static int hmac_sha256_tests();
static int hmac_sha512_tests();
static int hmac_sha1_tests();

// Public-Key
static int rsa_tests();

// TLS
static int tls_tests();

// Big Integer
static int bigint_tests();

// Checksum
static int adler32_tests();
static int crc32_tests();

// stop listing tests

static void print_buffer(ReadonlyBytes buffer, int split)
{
    for (size_t i = 0; i < buffer.size(); ++i) {
        if (split > 0) {
            if (i % split == 0 && i) {
                printf("    ");
                for (size_t j = i - split; j < i; ++j) {
                    auto ch = buffer[j];
                    printf("%c", ch >= 32 && ch <= 127 ? ch : '.'); // silly hack
                }
                puts("");
            }
        }
        printf("%02x ", buffer[i]);
    }
    puts("");
}

static Core::EventLoop g_loop;

static int run(Function<void(const char*, size_t)> fn)
{
    if (interactive) {
        auto editor = Line::Editor::construct();
        editor->initialize();
        for (;;) {
            auto line_result = editor->get_line("> ");

            if (line_result.is_error())
                break;
            auto& line = line_result.value();

            if (line == ".wait") {
                g_loop.exec();
            } else {
                fn(line.characters(), line.length());
                g_loop.pump();
            }
        }
    } else {
        if (filename == nullptr) {
            puts("must specify a file name");
            return 1;
        }
        if (!Core::File::exists(filename)) {
            puts("File does not exist");
            return 1;
        }
        auto file = Core::File::open(filename, Core::IODevice::OpenMode::ReadOnly);
        if (file.is_error()) {
            printf("That's a weird file man...\n");
            return 1;
        }
        auto buffer = file.value()->read_all();
        fn((const char*)buffer.data(), buffer.size());
        g_loop.exec();
    }
    return 0;
}

static void tls(const char* message, size_t len)
{
    static RefPtr<TLS::TLSv12> tls;
    static ByteBuffer write {};
    if (!tls) {
        tls = TLS::TLSv12::construct(nullptr);
        tls->connect(server ?: DEFAULT_SERVER, port);
        tls->on_tls_ready_to_read = [](auto& tls) {
            auto buffer = tls.read();
            if (buffer.has_value())
                fprintf(stdout, "%.*s", (int)buffer.value().size(), buffer.value().data());
        };
        tls->on_tls_ready_to_write = [&](auto&) {
            if (write.size()) {
                tls->write(write);
                write.clear();
            }
        };
        tls->on_tls_error = [&](auto) {
            g_loop.quit(1);
        };
        tls->on_tls_finished = [&]() {
            g_loop.quit(0);
        };
    }
    write.append(message, len);
    write.append("\r\n", 2);
}

static void aes_cbc(const char* message, size_t len)
{
    auto buffer = ByteBuffer::wrap(const_cast<char*>(message), len);
    // FIXME: Take iv as an optional parameter
    auto iv = ByteBuffer::create_zeroed(Crypto::Cipher::AESCipher::block_size());

    if (encrypting) {
        Crypto::Cipher::AESCipher::CBCMode cipher(
            ByteBuffer::wrap(const_cast<char*>(secret_key), strlen(secret_key)),
            key_bits,
            Crypto::Cipher::Intent::Encryption);

        auto enc = cipher.create_aligned_buffer(buffer.size());
        auto enc_span = enc.bytes();
        cipher.encrypt(buffer, enc_span, iv);

        if (binary)
            printf("%.*s", (int)enc_span.size(), enc_span.data());
        else
            print_buffer(enc_span, Crypto::Cipher::AESCipher::block_size());
    } else {
        Crypto::Cipher::AESCipher::CBCMode cipher(
            ByteBuffer::wrap(const_cast<char*>(secret_key), strlen(secret_key)),
            key_bits,
            Crypto::Cipher::Intent::Decryption);
        auto dec = cipher.create_aligned_buffer(buffer.size());
        auto dec_span = dec.bytes();
        cipher.decrypt(buffer, dec_span, iv);
        printf("%.*s\n", (int)dec_span.size(), dec_span.data());
    }
}

static void adler32(const char* message, size_t len)
{
    auto checksum = Crypto::Checksum::Adler32({ (const u8*)message, len });
    printf("%#10X\n", checksum.digest());
}

static void crc32(const char* message, size_t len)
{
    auto checksum = Crypto::Checksum::CRC32({ (const u8*)message, len });
    printf("%#10X\n", checksum.digest());
}

static void md5(const char* message, size_t len)
{
    auto digest = Crypto::Hash::MD5::hash((const u8*)message, len);
    if (binary)
        printf("%.*s", (int)Crypto::Hash::MD5::digest_size(), digest.data);
    else
        print_buffer({ digest.data, Crypto::Hash::MD5::digest_size() }, -1);
}

static void hmac_md5(const char* message, size_t len)
{
    Crypto::Authentication::HMAC<Crypto::Hash::MD5> hmac(secret_key);
    auto mac = hmac.process((const u8*)message, len);
    if (binary)
        printf("%.*s", (int)hmac.digest_size(), mac.data);
    else
        print_buffer({ mac.data, hmac.digest_size() }, -1);
}

static void sha1(const char* message, size_t len)
{
    auto digest = Crypto::Hash::SHA1::hash((const u8*)message, len);
    if (binary)
        printf("%.*s", (int)Crypto::Hash::SHA1::digest_size(), digest.data);
    else
        print_buffer({ digest.data, Crypto::Hash::SHA1::digest_size() }, -1);
}

static void sha256(const char* message, size_t len)
{
    auto digest = Crypto::Hash::SHA256::hash((const u8*)message, len);
    if (binary)
        printf("%.*s", (int)Crypto::Hash::SHA256::digest_size(), digest.data);
    else
        print_buffer({ digest.data, Crypto::Hash::SHA256::digest_size() }, -1);
}

static void hmac_sha256(const char* message, size_t len)
{
    Crypto::Authentication::HMAC<Crypto::Hash::SHA256> hmac(secret_key);
    auto mac = hmac.process((const u8*)message, len);
    if (binary)
        printf("%.*s", (int)hmac.digest_size(), mac.data);
    else
        print_buffer({ mac.data, hmac.digest_size() }, -1);
}

static void sha512(const char* message, size_t len)
{
    auto digest = Crypto::Hash::SHA512::hash((const u8*)message, len);
    if (binary)
        printf("%.*s", (int)Crypto::Hash::SHA512::digest_size(), digest.data);
    else
        print_buffer({ digest.data, Crypto::Hash::SHA512::digest_size() }, -1);
}

static void hmac_sha512(const char* message, size_t len)
{
    Crypto::Authentication::HMAC<Crypto::Hash::SHA512> hmac(secret_key);
    auto mac = hmac.process((const u8*)message, len);
    if (binary)
        printf("%.*s", (int)hmac.digest_size(), mac.data);
    else
        print_buffer({ mac.data, hmac.digest_size() }, -1);
}

auto main(int argc, char** argv) -> int
{
    const char* mode = nullptr;
    Core::ArgsParser parser;
    parser.add_positional_argument(mode, "mode to operate in ('list' to see modes and descriptions)", "mode");

    parser.add_option(secret_key, "Set the secret key (default key is 'WellHelloFriends')", "secret-key", 'k', "secret key");
    parser.add_option(key_bits, "Size of the key", "key-bits", 'b', "key-bits");
    parser.add_option(filename, "Read from file", "file", 'f', "from file");
    parser.add_option(binary, "Force binary output", "force-binary", 0);
    parser.add_option(interactive, "REPL mode", "interactive", 'i');
    parser.add_option(run_tests, "Run tests for the specified suite", "tests", 't');
    parser.add_option(suite, "Set the suite used", "suite-name", 'n', "suite name");
    parser.add_option(server, "Set the server to talk to (only for `tls')", "server-address", 's', "server-address");
    parser.add_option(port, "Set the port to talk to (only for `tls')", "port", 'p', "port");
    parser.add_option(in_ci, "CI Test mode", "ci-mode", 'c');
    parser.parse(argc, argv);

    StringView mode_sv { mode };
    if (mode_sv == "list") {
        puts("test-crypto modes");
        puts("\tdigest - Access digest (authentication) functions");
        puts("\thash - Access hash functions");
        puts("\tchecksum - Access checksum functions");
        puts("\tencrypt -- Access encryption functions");
        puts("\tdecrypt -- Access decryption functions");
        puts("\ttls -- Connect to a peer over TLS 1.2");
        puts("\tlist -- List all known modes");
        puts("these modes only contain tests");
        puts("\ttest -- Run every test suite");
        puts("\tbigint -- Run big integer test suite");
        puts("\tpk -- Run Public-key system tests");
        return 0;
    }

    if (mode_sv == "hash") {
        if (suite == nullptr)
            suite = DEFAULT_HASH_SUITE;
        StringView suite_sv { suite };

        if (suite_sv == "MD5") {
            if (run_tests)
                return md5_tests();
            return run(md5);
        }
        if (suite_sv == "SHA1") {
            if (run_tests)
                return sha1_tests();
            return run(sha1);
        }
        if (suite_sv == "SHA256") {
            if (run_tests)
                return sha256_tests();
            return run(sha256);
        }
        if (suite_sv == "SHA512") {
            if (run_tests)
                return sha512_tests();
            return run(sha512);
        }
        printf("unknown hash function '%s'\n", suite);
        return 1;
    }
    if (mode_sv == "checksum") {
        if (suite == nullptr)
            suite = DEFAULT_CHECKSUM_SUITE;
        StringView suite_sv { suite };

        if (suite_sv == "CRC32") {
            if (run_tests)
                return crc32_tests();
            return run(crc32);
        }
        if (suite_sv == "Adler32") {
            if (run_tests)
                return adler32_tests();
            return run(adler32);
        }
        printf("unknown checksum function '%s'\n", suite);
        return 1;
    }
    if (mode_sv == "digest") {
        if (suite == nullptr)
            suite = DEFAULT_DIGEST_SUITE;
        StringView suite_sv { suite };

        if (suite_sv == "HMAC-MD5") {
            if (run_tests)
                return hmac_md5_tests();
            return run(hmac_md5);
        }
        if (suite_sv == "HMAC-SHA256") {
            if (run_tests)
                return hmac_sha256_tests();
            return run(hmac_sha256);
        }
        if (suite_sv == "HMAC-SHA512") {
            if (run_tests)
                return hmac_sha512_tests();
            return run(hmac_sha512);
        }
        if (suite_sv == "HMAC-SHA1") {
            if (run_tests)
                return hmac_sha1_tests();
        }
        printf("unknown hash function '%s'\n", suite);
        return 1;
    }
    if (mode_sv == "pk") {
        return rsa_tests();
    }
    if (mode_sv == "bigint") {
        return bigint_tests();
    }
    if (mode_sv == "tls") {
        if (run_tests)
            return tls_tests();
        return run(tls);
    }
    if (mode_sv == "test") {
        encrypting = true;
        aes_cbc_tests();
        aes_ctr_tests();

        encrypting = false;
        aes_cbc_tests();
        aes_ctr_tests();

        md5_tests();
        sha1_tests();
        sha256_tests();
        sha512_tests();

        hmac_md5_tests();
        hmac_sha256_tests();
        hmac_sha512_tests();
        hmac_sha1_tests();

        rsa_tests();

        if (!in_ci) {
            // Do not run these in CI to avoid tests with variables outside our control.
            tls_tests();
        }

        bigint_tests();

        return g_some_test_failed ? 1 : 0;
    }
    encrypting = mode_sv == "encrypt";
    if (encrypting || mode_sv == "decrypt") {
        if (suite == nullptr)
            suite = DEFAULT_CIPHER_SUITE;
        StringView suite_sv { suite };

        if (StringView(suite) == "AES_CBC") {
            if (run_tests)
                return aes_cbc_tests();

            if (!Crypto::Cipher::AESCipher::KeyType::is_valid_key_size(key_bits)) {
                printf("Invalid key size for AES: %d\n", key_bits);
                return 1;
            }
            if (strlen(secret_key) != (size_t)key_bits / 8) {
                printf("Key must be exactly %d bytes long\n", key_bits / 8);
                return 1;
            }
            return run(aes_cbc);
        } else {
            printf("Unknown cipher suite '%s'\n", suite);
            return 1;
        }
    }
    printf("Unknown mode '%s', check out the list of modes\n", mode);
    return 1;
}

#define I_TEST(thing)                       \
    {                                       \
        printf("Testing " #thing "... ");   \
        fflush(stdout);                     \
        gettimeofday(&start_time, nullptr); \
    }
#define PASS                                                                          \
    {                                                                                 \
        struct timeval end_time {                                                     \
            0, 0                                                                      \
        };                                                                            \
        gettimeofday(&end_time, nullptr);                                             \
        time_t interval_s = end_time.tv_sec - start_time.tv_sec;                      \
        suseconds_t interval_us = end_time.tv_usec;                                   \
        if (interval_us < start_time.tv_usec) {                                       \
            interval_s -= 1;                                                          \
            interval_us += 1000000;                                                   \
        }                                                                             \
        interval_us -= start_time.tv_usec;                                            \
        printf("PASS %llds %lldus\n", (long long)interval_s, (long long)interval_us); \
    }
#define FAIL(reason)                   \
    do {                               \
        printf("FAIL: " #reason "\n"); \
        g_some_test_failed = true;     \
    } while (0)

static ByteBuffer operator""_b(const char* string, size_t length)
{
    dbg() << "Create byte buffer of size " << length;
    return ByteBuffer::copy(string, length);
}

// tests go after here
// please be reasonable with orders kthx
static void aes_cbc_test_name();
static void aes_cbc_test_encrypt();
static void aes_cbc_test_decrypt();
static void aes_ctr_test_name();
static void aes_ctr_test_encrypt();
static void aes_ctr_test_decrypt();

static void md5_test_name();
static void md5_test_hash();
static void md5_test_consecutive_updates();

static void sha1_test_name();
static void sha1_test_hash();

static void sha256_test_name();
static void sha256_test_hash();

static void sha512_test_name();
static void sha512_test_hash();

static void hmac_md5_test_name();
static void hmac_md5_test_process();

static void hmac_sha256_test_name();
static void hmac_sha256_test_process();

static void hmac_sha512_test_name();
static void hmac_sha512_test_process();

static void hmac_sha1_test_name();
static void hmac_sha1_test_process();

static void rsa_test_encrypt();
static void rsa_test_der_parse();
static void rsa_test_encrypt_decrypt();
static void rsa_emsa_pss_test_create();
static void bigint_test_number_theory(); // FIXME: we should really move these num theory stuff out

static void tls_test_client_hello();

static void bigint_test_fibo500();
static void bigint_addition_edgecases();
static void bigint_subtraction();
static void bigint_multiplication();
static void bigint_division();
static void bigint_base10();
static void bigint_import_export();
static void bigint_bitwise();

static void bigint_test_signed_fibo500();
static void bigint_signed_addition_edgecases();
static void bigint_signed_subtraction();
static void bigint_signed_multiplication();
static void bigint_signed_division();
static void bigint_signed_base10();
static void bigint_signed_import_export();
static void bigint_signed_bitwise();

static int aes_cbc_tests()
{
    aes_cbc_test_name();
    if (encrypting) {
        aes_cbc_test_encrypt();
    } else {
        aes_cbc_test_decrypt();
    }

    return g_some_test_failed ? 1 : 0;
}

static void aes_cbc_test_name()
{
    I_TEST((AES CBC class name));
    Crypto::Cipher::AESCipher::CBCMode cipher("WellHelloFriends"_b, 128, Crypto::Cipher::Intent::Encryption);
    if (cipher.class_name() != "AES_CBC")
        FAIL(Invalid class name);
    else
        PASS;
}

static void aes_cbc_test_encrypt()
{
    auto test_it = [](auto& cipher, auto& result) {
        auto in = "This is a test! This is another test!"_b;
        auto out = cipher.create_aligned_buffer(in.size());
        auto iv = ByteBuffer::create_zeroed(Crypto::Cipher::AESCipher::block_size());
        auto out_span = out.bytes();
        cipher.encrypt(in, out_span, iv);
        if (out.size() != sizeof(result))
            FAIL(size mismatch);
        else if (memcmp(out_span.data(), result, out_span.size()) != 0) {
            FAIL(invalid data);
            print_buffer(out_span, Crypto::Cipher::AESCipher::block_size());
        } else
            PASS;
    };
    {
        I_TEST((AES CBC with 128 bit key | Encrypt))
        u8 result[] {
            0xb8, 0x06, 0x7c, 0xf2, 0xa9, 0x56, 0x63, 0x58, 0x2d, 0x5c, 0xa1, 0x4b, 0xc5, 0xe3, 0x08,
            0xcf, 0xb5, 0x93, 0xfb, 0x67, 0xb6, 0xf7, 0xaf, 0x45, 0x34, 0x64, 0x70, 0x9e, 0xc9, 0x1a,
            0x8b, 0xd3, 0x70, 0x45, 0xf0, 0x79, 0x65, 0xca, 0xb9, 0x03, 0x88, 0x72, 0x1c, 0xdd, 0xab,
            0x45, 0x6b, 0x1c
        };
        Crypto::Cipher::AESCipher::CBCMode cipher("WellHelloFriends"_b, 128, Crypto::Cipher::Intent::Encryption);
        test_it(cipher, result);
    }
    {
        I_TEST((AES CBC with 192 bit key | Encrypt))
        u8 result[] {
            0xae, 0xd2, 0x70, 0xc4, 0x9c, 0xaa, 0x83, 0x33, 0xd3, 0xd3, 0xac, 0x11, 0x65, 0x35, 0xf7,
            0x19, 0x48, 0x7c, 0x7a, 0x8a, 0x95, 0x64, 0xe7, 0xc6, 0x0a, 0xdf, 0x10, 0x06, 0xdc, 0x90,
            0x68, 0x51, 0x09, 0xd7, 0x3b, 0x48, 0x1b, 0x8a, 0xd3, 0x50, 0x09, 0xba, 0xfc, 0xde, 0x11,
            0xe0, 0x3f, 0xcb
        };
        Crypto::Cipher::AESCipher::CBCMode cipher("Well Hello Friends! whf!"_b, 192, Crypto::Cipher::Intent::Encryption);
        test_it(cipher, result);
    }
    {
        I_TEST((AES CBC with 256 bit key | Encrypt))
        u8 result[] {
            0x0a, 0x44, 0x4d, 0x62, 0x9e, 0x8b, 0xd8, 0x11, 0x80, 0x48, 0x2a, 0x32, 0x53, 0x61, 0xe7,
            0x59, 0x62, 0x55, 0x9e, 0xf4, 0xe6, 0xad, 0xea, 0xc5, 0x0b, 0xf6, 0xbc, 0x6a, 0xcb, 0x9c,
            0x47, 0x9f, 0xc2, 0x21, 0xe6, 0x19, 0x62, 0xc3, 0x75, 0xca, 0xab, 0x2d, 0x18, 0xa1, 0x54,
            0xd1, 0x41, 0xe6
        };
        Crypto::Cipher::AESCipher::CBCMode cipher("WellHelloFriendsWellHelloFriends"_b, 256, Crypto::Cipher::Intent::Encryption);
        test_it(cipher, result);
    }
    {
        I_TEST((AES CBC with 256 bit key | Encrypt with unsigned key))
        u8 result[] {
            0x18, 0x71, 0x80, 0x4c, 0x28, 0x07, 0x55, 0x3c, 0x05, 0x33, 0x36, 0x3f, 0x19, 0x38, 0x5c,
            0xbe, 0xf8, 0xb8, 0x0e, 0x0e, 0x66, 0x67, 0x63, 0x9c, 0xbf, 0x73, 0xcd, 0x82, 0xf9, 0xcb,
            0x9d, 0x81, 0x56, 0xc6, 0x75, 0x14, 0x8b, 0x79, 0x60, 0xb0, 0xdf, 0xaa, 0x2c, 0x2b, 0xd4,
            0xd6, 0xa0, 0x46
        };
        u8 key[] { 0x0a, 0x8c, 0x5b, 0x0d, 0x8a, 0x68, 0x43, 0xf7, 0xaf, 0xc0, 0xe3, 0x4e, 0x4b, 0x43, 0xaa, 0x28, 0x69, 0x9b, 0x6f, 0xe7, 0x24, 0x82, 0x1c, 0x71, 0x86, 0xf6, 0x2b, 0x87, 0xd6, 0x8b, 0x8f, 0xf1 };
        Crypto::Cipher::AESCipher::CBCMode cipher(ByteBuffer::wrap(key, 32), 256, Crypto::Cipher::Intent::Encryption);
        test_it(cipher, result);
    }
    // TODO: Test non-CMS padding options
}
static void aes_cbc_test_decrypt()
{
    auto test_it = [](auto& cipher, auto& result, auto result_len) {
        auto true_value = "This is a test! This is another test!";
        auto in = ByteBuffer::copy(result, result_len);
        auto out = cipher.create_aligned_buffer(in.size());
        auto iv = ByteBuffer::create_zeroed(Crypto::Cipher::AESCipher::block_size());
        auto out_span = out.bytes();
        cipher.decrypt(in, out_span, iv);
        if (out_span.size() != strlen(true_value)) {
            FAIL(size mismatch);
            printf("Expected %zu bytes but got %zu\n", strlen(true_value), out_span.size());
        } else if (memcmp(out_span.data(), true_value, strlen(true_value)) != 0) {
            FAIL(invalid data);
            print_buffer(out_span, Crypto::Cipher::AESCipher::block_size());
        } else
            PASS;
    };
    {
        I_TEST((AES CBC with 128 bit key | Decrypt))
        u8 result[] {
            0xb8, 0x06, 0x7c, 0xf2, 0xa9, 0x56, 0x63, 0x58, 0x2d, 0x5c, 0xa1, 0x4b, 0xc5, 0xe3, 0x08,
            0xcf, 0xb5, 0x93, 0xfb, 0x67, 0xb6, 0xf7, 0xaf, 0x45, 0x34, 0x64, 0x70, 0x9e, 0xc9, 0x1a,
            0x8b, 0xd3, 0x70, 0x45, 0xf0, 0x79, 0x65, 0xca, 0xb9, 0x03, 0x88, 0x72, 0x1c, 0xdd, 0xab,
            0x45, 0x6b, 0x1c
        };
        Crypto::Cipher::AESCipher::CBCMode cipher("WellHelloFriends"_b, 128, Crypto::Cipher::Intent::Decryption);
        test_it(cipher, result, 48);
    }
    {
        I_TEST((AES CBC with 192 bit key | Decrypt))
        u8 result[] {
            0xae, 0xd2, 0x70, 0xc4, 0x9c, 0xaa, 0x83, 0x33, 0xd3, 0xd3, 0xac, 0x11, 0x65, 0x35, 0xf7,
            0x19, 0x48, 0x7c, 0x7a, 0x8a, 0x95, 0x64, 0xe7, 0xc6, 0x0a, 0xdf, 0x10, 0x06, 0xdc, 0x90,
            0x68, 0x51, 0x09, 0xd7, 0x3b, 0x48, 0x1b, 0x8a, 0xd3, 0x50, 0x09, 0xba, 0xfc, 0xde, 0x11,
            0xe0, 0x3f, 0xcb
        };
        Crypto::Cipher::AESCipher::CBCMode cipher("Well Hello Friends! whf!"_b, 192, Crypto::Cipher::Intent::Decryption);
        test_it(cipher, result, 48);
    }
    {
        I_TEST((AES CBC with 256 bit key | Decrypt))
        u8 result[] {
            0x0a, 0x44, 0x4d, 0x62, 0x9e, 0x8b, 0xd8, 0x11, 0x80, 0x48, 0x2a, 0x32, 0x53, 0x61, 0xe7,
            0x59, 0x62, 0x55, 0x9e, 0xf4, 0xe6, 0xad, 0xea, 0xc5, 0x0b, 0xf6, 0xbc, 0x6a, 0xcb, 0x9c,
            0x47, 0x9f, 0xc2, 0x21, 0xe6, 0x19, 0x62, 0xc3, 0x75, 0xca, 0xab, 0x2d, 0x18, 0xa1, 0x54,
            0xd1, 0x41, 0xe6
        };
        Crypto::Cipher::AESCipher::CBCMode cipher("WellHelloFriendsWellHelloFriends"_b, 256, Crypto::Cipher::Intent::Decryption);
        test_it(cipher, result, 48);
    }
    // TODO: Test non-CMS padding options
}

static int aes_ctr_tests()
{
    aes_ctr_test_name();
    if (encrypting) {
        aes_ctr_test_encrypt();
    } else {
        aes_ctr_test_decrypt();
    }

    return g_some_test_failed ? 1 : 0;
}

static void aes_ctr_test_name()
{
    I_TEST((AES CTR class name));
    Crypto::Cipher::AESCipher::CTRMode cipher("WellHelloFriends"_b, 128, Crypto::Cipher::Intent::Encryption);
    if (cipher.class_name() != "AES_CTR")
        FAIL(Invalid class name);
    else
        PASS;
}

#define AS_BB(x) (ByteBuffer::wrap((x), sizeof((x)) / sizeof((x)[0])))
static void aes_ctr_test_encrypt()
{
    auto test_it = [](auto key, auto ivec, auto in, auto out_expected) {
        // nonce is already included in ivec.
        Crypto::Cipher::AESCipher::CTRMode cipher(key, 8 * key.size(), Crypto::Cipher::Intent::Encryption);
        ByteBuffer out_actual = ByteBuffer::create_zeroed(in.size());
        Bytes out_span = out_actual.bytes();
        cipher.encrypt(in, out_span, ivec);
        if (out_expected.size() != out_actual.size()) {
            FAIL(size mismatch);
            printf("Expected %zu bytes but got %zu\n", out_expected.size(), out_span.size());
            print_buffer(out_span, Crypto::Cipher::AESCipher::block_size());
        } else if (memcmp(out_expected.data(), out_span.data(), out_expected.size()) != 0) {
            FAIL(invalid data);
            print_buffer(out_span, Crypto::Cipher::AESCipher::block_size());
        } else
            PASS;
    };
    // From RFC 3686, Section 6
    {
        // Test Vector #1
        I_TEST((AES CTR 16 octets with 128 bit key | Encrypt))
        u8 key[] {
            0xae, 0x68, 0x52, 0xf8, 0x12, 0x10, 0x67, 0xcc, 0x4b, 0xf7, 0xa5, 0x76, 0x55, 0x77, 0xf3, 0x9e
        };
        u8 ivec[] {
            0x00, 0x00, 0x00, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 in[] {
            0x53, 0x69, 0x6e, 0x67, 0x6c, 0x65, 0x20, 0x62, 0x6c, 0x6f, 0x63, 0x6b, 0x20, 0x6d, 0x73, 0x67
        };
        u8 out[] {
            0xe4, 0x09, 0x5d, 0x4f, 0xb7, 0xa7, 0xb3, 0x79, 0x2d, 0x61, 0x75, 0xa3, 0x26, 0x13, 0x11, 0xb8
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    {
        // Test Vector #2
        I_TEST((AES CTR 32 octets with 128 bit key | Encrypt))
        u8 key[] {
            0x7e, 0x24, 0x06, 0x78, 0x17, 0xfa, 0xe0, 0xd7, 0x43, 0xd6, 0xce, 0x1f, 0x32, 0x53, 0x91, 0x63
        };
        u8 ivec[] {
            0x00, 0x6c, 0xb6, 0xdb, 0xc0, 0x54, 0x3b, 0x59, 0xda, 0x48, 0xd9, 0x0b, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 in[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f
        };
        u8 out[] {
            0x51, 0x04, 0xa1, 0x06, 0x16, 0x8a, 0x72, 0xd9, 0x79, 0x0d, 0x41, 0xee, 0x8e, 0xda, 0xd3, 0x88,
            0xeb, 0x2e, 0x1e, 0xfc, 0x46, 0xda, 0x57, 0xc8, 0xfc, 0xe6, 0x30, 0xdf, 0x91, 0x41, 0xbe, 0x28
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    {
        // Test Vector #3
        I_TEST((AES CTR 36 octets with 128 bit key | Encrypt))
        u8 key[] {
            0x76, 0x91, 0xbe, 0x03, 0x5e, 0x50, 0x20, 0xa8, 0xac, 0x6e, 0x61, 0x85, 0x29, 0xf9, 0xa0, 0xdc
        };
        u8 ivec[] {
            0x00, 0xe0, 0x01, 0x7b, 0x27, 0x77, 0x7f, 0x3f, 0x4a, 0x17, 0x86, 0xf0, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 in[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23
        };
        u8 out[] {
            0xc1, 0xcf, 0x48, 0xa8, 0x9f, 0x2f, 0xfd, 0xd9, 0xcf, 0x46, 0x52, 0xe9, 0xef, 0xdb, 0x72, 0xd7, 0x45, 0x40, 0xa4, 0x2b, 0xde, 0x6d, 0x78, 0x36, 0xd5, 0x9a, 0x5c, 0xea, 0xae, 0xf3, 0x10, 0x53, 0x25, 0xb2, 0x07, 0x2f
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    {
        // Test Vector #4
        I_TEST((AES CTR 16 octets with 192 bit key | Encrypt))
        u8 key[] {
            0x16, 0xaf, 0x5b, 0x14, 0x5f, 0xc9, 0xf5, 0x79, 0xc1, 0x75, 0xf9, 0x3e, 0x3b, 0xfb, 0x0e, 0xed, 0x86, 0x3d, 0x06, 0xcc, 0xfd, 0xb7, 0x85, 0x15
        };
        u8 ivec[] {
            0x00, 0x00, 0x00, 0x48, 0x36, 0x73, 0x3c, 0x14, 0x7d, 0x6d, 0x93, 0xcb, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 in[] {
            0x53, 0x69, 0x6e, 0x67, 0x6c, 0x65, 0x20, 0x62, 0x6c, 0x6f, 0x63, 0x6b, 0x20, 0x6d, 0x73, 0x67
        };
        u8 out[] {
            0x4b, 0x55, 0x38, 0x4f, 0xe2, 0x59, 0xc9, 0xc8, 0x4e, 0x79, 0x35, 0xa0, 0x03, 0xcb, 0xe9, 0x28
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    {
        // Test Vector #5
        I_TEST((AES CTR 32 octets with 192 bit key | Encrypt))
        u8 key[] {
            0x7c, 0x5c, 0xb2, 0x40, 0x1b, 0x3d, 0xc3, 0x3c, 0x19, 0xe7, 0x34, 0x08, 0x19, 0xe0, 0xf6, 0x9c, 0x67, 0x8c, 0x3d, 0xb8, 0xe6, 0xf6, 0xa9, 0x1a
        };
        u8 ivec[] {
            0x00, 0x96, 0xb0, 0x3b, 0x02, 0x0c, 0x6e, 0xad, 0xc2, 0xcb, 0x50, 0x0d, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 in[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f
        };
        u8 out[] {
            0x45, 0x32, 0x43, 0xfc, 0x60, 0x9b, 0x23, 0x32, 0x7e, 0xdf, 0xaa, 0xfa, 0x71, 0x31, 0xcd, 0x9f, 0x84, 0x90, 0x70, 0x1c, 0x5a, 0xd4, 0xa7, 0x9c, 0xfc, 0x1f, 0xe0, 0xff, 0x42, 0xf4, 0xfb, 0x00
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    {
        // Test Vector #6
        I_TEST((AES CTR 36 octets with 192 bit key | Encrypt))
        u8 key[] {
            0x02, 0xbf, 0x39, 0x1e, 0xe8, 0xec, 0xb1, 0x59, 0xb9, 0x59, 0x61, 0x7b, 0x09, 0x65, 0x27, 0x9b, 0xf5, 0x9b, 0x60, 0xa7, 0x86, 0xd3, 0xe0, 0xfe
        };
        u8 ivec[] {
            0x00, 0x07, 0xbd, 0xfd, 0x5c, 0xbd, 0x60, 0x27, 0x8d, 0xcc, 0x09, 0x12, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 in[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23
        };
        u8 out[] {
            0x96, 0x89, 0x3f, 0xc5, 0x5e, 0x5c, 0x72, 0x2f, 0x54, 0x0b, 0x7d, 0xd1, 0xdd, 0xf7, 0xe7, 0x58, 0xd2, 0x88, 0xbc, 0x95, 0xc6, 0x91, 0x65, 0x88, 0x45, 0x36, 0xc8, 0x11, 0x66, 0x2f, 0x21, 0x88, 0xab, 0xee, 0x09, 0x35
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    {
        // Test Vector #7
        I_TEST((AES CTR 16 octets with 256 bit key | Encrypt))
        u8 key[] {
            0x77, 0x6b, 0xef, 0xf2, 0x85, 0x1d, 0xb0, 0x6f, 0x4c, 0x8a, 0x05, 0x42, 0xc8, 0x69, 0x6f, 0x6c, 0x6a, 0x81, 0xaf, 0x1e, 0xec, 0x96, 0xb4, 0xd3, 0x7f, 0xc1, 0xd6, 0x89, 0xe6, 0xc1, 0xc1, 0x04
        };
        u8 ivec[] {
            0x00, 0x00, 0x00, 0x60, 0xdb, 0x56, 0x72, 0xc9, 0x7a, 0xa8, 0xf0, 0xb2, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 in[] {
            0x53, 0x69, 0x6e, 0x67, 0x6c, 0x65, 0x20, 0x62, 0x6c, 0x6f, 0x63, 0x6b, 0x20, 0x6d, 0x73, 0x67
        };
        u8 out[] {
            0x14, 0x5a, 0xd0, 0x1d, 0xbf, 0x82, 0x4e, 0xc7, 0x56, 0x08, 0x63, 0xdc, 0x71, 0xe3, 0xe0, 0xc0
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    {
        // Test Vector #8
        I_TEST((AES CTR 32 octets with 256 bit key | Encrypt))
        u8 key[] {
            0xf6, 0xd6, 0x6d, 0x6b, 0xd5, 0x2d, 0x59, 0xbb, 0x07, 0x96, 0x36, 0x58, 0x79, 0xef, 0xf8, 0x86, 0xc6, 0x6d, 0xd5, 0x1a, 0x5b, 0x6a, 0x99, 0x74, 0x4b, 0x50, 0x59, 0x0c, 0x87, 0xa2, 0x38, 0x84
        };
        u8 ivec[] {
            0x00, 0xfa, 0xac, 0x24, 0xc1, 0x58, 0x5e, 0xf1, 0x5a, 0x43, 0xd8, 0x75, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 in[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f
        };
        u8 out[] {
            0xf0, 0x5e, 0x23, 0x1b, 0x38, 0x94, 0x61, 0x2c, 0x49, 0xee, 0x00, 0x0b, 0x80, 0x4e, 0xb2, 0xa9, 0xb8, 0x30, 0x6b, 0x50, 0x8f, 0x83, 0x9d, 0x6a, 0x55, 0x30, 0x83, 0x1d, 0x93, 0x44, 0xaf, 0x1c
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    {
        // Test Vector #9
        I_TEST((AES CTR 36 octets with 256 bit key | Encrypt))
        u8 key[] {
            0xff, 0x7a, 0x61, 0x7c, 0xe6, 0x91, 0x48, 0xe4, 0xf1, 0x72, 0x6e, 0x2f, 0x43, 0x58, 0x1d, 0xe2, 0xaa, 0x62, 0xd9, 0xf8, 0x05, 0x53, 0x2e, 0xdf, 0xf1, 0xee, 0xd6, 0x87, 0xfb, 0x54, 0x15, 0x3d
        };
        u8 ivec[] {
            0x00, 0x1c, 0xc5, 0xb7, 0x51, 0xa5, 0x1d, 0x70, 0xa1, 0xc1, 0x11, 0x48, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 in[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23
        };
        u8 out[] {
            0xeb, 0x6c, 0x52, 0x82, 0x1d, 0x0b, 0xbb, 0xf7, 0xce, 0x75, 0x94, 0x46, 0x2a, 0xca, 0x4f, 0xaa, 0xb4, 0x07, 0xdf, 0x86, 0x65, 0x69, 0xfd, 0x07, 0xf4, 0x8c, 0xc0, 0xb5, 0x83, 0xd6, 0x07, 0x1f, 0x1e, 0xc0, 0xe6, 0xb8
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    // Manual test case
    {
        // This test checks whether counter overflow crashes.
        I_TEST((AES CTR 36 octets with 256 bit key, high counter | Encrypt))
        u8 key[] {
            0xff, 0x7a, 0x61, 0x7c, 0xe6, 0x91, 0x48, 0xe4, 0xf1, 0x72, 0x6e, 0x2f, 0x43, 0x58, 0x1d, 0xe2, 0xaa, 0x62, 0xd9, 0xf8, 0x05, 0x53, 0x2e, 0xdf, 0xf1, 0xee, 0xd6, 0x87, 0xfb, 0x54, 0x15, 0x3d
        };
        u8 ivec[] {
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff
        };
        u8 in[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23
        };
        u8 out[] {
            // Pasted from the output. The actual success condition is
            // not crashing when incrementing the counter.
            0x6e, 0x8c, 0xfc, 0x59, 0x08, 0xa8, 0xc0, 0xf1, 0xe6, 0x85, 0x96, 0xe9, 0xc5, 0x40, 0xb6, 0x8b, 0xfe, 0x28, 0x72, 0xe2, 0x24, 0x11, 0x7e, 0x59, 0xef, 0xac, 0x5c, 0xe1, 0x06, 0x89, 0x09, 0xab, 0xf8, 0x90, 0x1c, 0x66
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
}

static void aes_ctr_test_decrypt()
{
    auto test_it = [](auto key, auto ivec, auto in, auto out_expected) {
        // nonce is already included in ivec.
        Crypto::Cipher::AESCipher::CTRMode cipher(key, 8 * key.size(), Crypto::Cipher::Intent::Decryption);
        ByteBuffer out_actual = ByteBuffer::create_zeroed(in.size());
        auto out_span = out_actual.bytes();
        cipher.decrypt(in, out_span, ivec);
        if (out_expected.size() != out_span.size()) {
            FAIL(size mismatch);
            printf("Expected %zu bytes but got %zu\n", out_expected.size(), out_span.size());
            print_buffer(out_span, Crypto::Cipher::AESCipher::block_size());
        } else if (memcmp(out_expected.data(), out_span.data(), out_expected.size()) != 0) {
            FAIL(invalid data);
            print_buffer(out_span, Crypto::Cipher::AESCipher::block_size());
        } else
            PASS;
    };
    // From RFC 3686, Section 6
    {
        // Test Vector #1
        I_TEST((AES CTR 16 octets with 128 bit key | Decrypt))
        u8 key[] {
            0xae, 0x68, 0x52, 0xf8, 0x12, 0x10, 0x67, 0xcc, 0x4b, 0xf7, 0xa5, 0x76, 0x55, 0x77, 0xf3, 0x9e
        };
        u8 ivec[] {
            0x00, 0x00, 0x00, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 + 1 // See CTR.h
        };
        u8 out[] {
            0x53, 0x69, 0x6e, 0x67, 0x6c, 0x65, 0x20, 0x62, 0x6c, 0x6f, 0x63, 0x6b, 0x20, 0x6d, 0x73, 0x67
        };
        u8 in[] {
            0xe4, 0x09, 0x5d, 0x4f, 0xb7, 0xa7, 0xb3, 0x79, 0x2d, 0x61, 0x75, 0xa3, 0x26, 0x13, 0x11, 0xb8
        };
        test_it(AS_BB(key), AS_BB(ivec), AS_BB(in), AS_BB(out));
    }
    // If encryption works, then decryption works, too.
}

static int md5_tests()
{
    md5_test_name();
    md5_test_hash();
    md5_test_consecutive_updates();
    return g_some_test_failed ? 1 : 0;
}

static void md5_test_name()
{
    I_TEST((MD5 class name));
    Crypto::Hash::MD5 md5;
    if (md5.class_name() != "MD5")
        FAIL(Invalid class name);
    else
        PASS;
}

static void md5_test_hash()
{
    {
        I_TEST((MD5 Hashing | "Well hello friends"));
        u8 result[] {
            0xaf, 0x04, 0x3a, 0x08, 0x94, 0x38, 0x6e, 0x7f, 0xbf, 0x73, 0xe4, 0xaa, 0xf0, 0x8e, 0xee, 0x4c
        };
        auto digest = Crypto::Hash::MD5::hash("Well hello friends");

        if (memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::MD5::digest_size() }, -1);
        } else {
            PASS;
        }
    }
    // RFC tests
    {
        I_TEST((MD5 Hashing | ""));
        u8 result[] {
            0xd4, 0x1d, 0x8c, 0xd9, 0x8f, 0x00, 0xb2, 0x04, 0xe9, 0x80, 0x09, 0x98, 0xec, 0xf8, 0x42, 0x7e
        };
        auto digest = Crypto::Hash::MD5::hash("");

        if (memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::MD5::digest_size() }, -1);
        } else {
            PASS;
        }
    }
    {
        I_TEST((MD5 Hashing | "a"));
        u8 result[] {
            0x0c, 0xc1, 0x75, 0xb9, 0xc0, 0xf1, 0xb6, 0xa8, 0x31, 0xc3, 0x99, 0xe2, 0x69, 0x77, 0x26, 0x61
        };
        auto digest = Crypto::Hash::MD5::hash("a");

        if (memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::MD5::digest_size() }, -1);
        } else {
            PASS;
        }
    }
    {
        I_TEST((MD5 Hashing | "abcdefghijklmnopqrstuvwxyz"));
        u8 result[] {
            0xc3, 0xfc, 0xd3, 0xd7, 0x61, 0x92, 0xe4, 0x00, 0x7d, 0xfb, 0x49, 0x6c, 0xca, 0x67, 0xe1, 0x3b
        };
        auto digest = Crypto::Hash::MD5::hash("abcdefghijklmnopqrstuvwxyz");

        if (memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::MD5::digest_size() }, -1);
        } else {
            PASS;
        }
    }
    {
        I_TEST((MD5 Hashing | Long Sequence));
        u8 result[] {
            0x57, 0xed, 0xf4, 0xa2, 0x2b, 0xe3, 0xc9, 0x55, 0xac, 0x49, 0xda, 0x2e, 0x21, 0x07, 0xb6, 0x7a
        };
        auto digest = Crypto::Hash::MD5::hash("12345678901234567890123456789012345678901234567890123456789012345678901234567890");

        if (memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::MD5::digest_size() }, -1);
        } else {
            PASS;
        }
    }
}

static void md5_test_consecutive_updates()
{
    {
        I_TEST((MD5 Hashing | Multiple Updates));
        u8 result[] {
            0xaf, 0x04, 0x3a, 0x08, 0x94, 0x38, 0x6e, 0x7f, 0xbf, 0x73, 0xe4, 0xaa, 0xf0, 0x8e, 0xee, 0x4c
        };
        Crypto::Hash::MD5 md5;

        md5.update("Well");
        md5.update(" hello ");
        md5.update("friends");
        auto digest = md5.digest();

        if (memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) != 0)
            FAIL(Invalid hash);
        else
            PASS;
    }
    {
        I_TEST((MD5 Hashing | Reuse));
        Crypto::Hash::MD5 md5;

        md5.update("Well");
        md5.update(" hello ");
        md5.update("friends");
        auto digest0 = md5.digest();

        md5.update("Well");
        md5.update(" hello ");
        md5.update("friends");
        auto digest1 = md5.digest();

        if (memcmp(digest0.data, digest1.data, Crypto::Hash::MD5::digest_size()) != 0)
            FAIL(Cannot reuse);
        else
            PASS;
    }
}

static int hmac_md5_tests()
{
    hmac_md5_test_name();
    hmac_md5_test_process();
    return g_some_test_failed ? 1 : 0;
}

static int hmac_sha256_tests()
{
    hmac_sha256_test_name();
    hmac_sha256_test_process();
    return g_some_test_failed ? 1 : 0;
}

static int hmac_sha512_tests()
{
    hmac_sha512_test_name();
    hmac_sha512_test_process();
    return g_some_test_failed ? 1 : 0;
}

static int hmac_sha1_tests()
{
    hmac_sha1_test_name();
    hmac_sha1_test_process();
    return g_some_test_failed ? 1 : 0;
}

static void hmac_md5_test_name()
{
    I_TEST((HMAC - MD5 | Class name));
    Crypto::Authentication::HMAC<Crypto::Hash::MD5> hmac("Well Hello Friends");
    if (hmac.class_name() != "HMAC-MD5")
        FAIL(Invalid class name);
    else
        PASS;
}

static void hmac_md5_test_process()
{
    {
        I_TEST((HMAC - MD5 | Basic));
        Crypto::Authentication::HMAC<Crypto::Hash::MD5> hmac("Well Hello Friends");
        u8 result[] {
            0x3b, 0x5b, 0xde, 0x30, 0x3a, 0x54, 0x7b, 0xbb, 0x09, 0xfe, 0x78, 0x89, 0xbc, 0x9f, 0x22, 0xa3
        };
        auto mac = hmac.process("Some bogus data");
        if (memcmp(result, mac.data, hmac.digest_size()) != 0) {
            FAIL(Invalid mac);
            print_buffer({ mac.data, hmac.digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((HMAC - MD5 | Reuse));
        Crypto::Authentication::HMAC<Crypto::Hash::MD5> hmac("Well Hello Friends");

        auto mac_0 = hmac.process("Some bogus data");
        auto mac_1 = hmac.process("Some bogus data");

        if (memcmp(mac_0.data, mac_1.data, hmac.digest_size()) != 0) {
            FAIL(Cannot reuse);
        } else
            PASS;
    }
}

static void hmac_sha1_test_name()
{
    I_TEST((HMAC - SHA1 | Class name));
    Crypto::Authentication::HMAC<Crypto::Hash::SHA1> hmac("Well Hello Friends");
    if (hmac.class_name() != "HMAC-SHA1")
        FAIL(Invalid class name);
    else
        PASS;
}

static void hmac_sha1_test_process()
{
    {
        I_TEST((HMAC - SHA1 | Basic));
        u8 key[] { 0xc8, 0x52, 0xe5, 0x4a, 0x2c, 0x03, 0x2b, 0xc9, 0x63, 0xd3, 0xc2, 0x79, 0x0f, 0x76, 0x43, 0xef, 0x36, 0xc3, 0x7a, 0xca };
        Crypto::Authentication::HMAC<Crypto::Hash::SHA1> hmac(ByteBuffer::wrap(key, 20));
        u8 result[] {
            0x2c, 0x57, 0x32, 0x61, 0x3b, 0xa7, 0x84, 0x87, 0x0e, 0x4f, 0x42, 0x07, 0x2f, 0xf0, 0xe7, 0x41, 0xd7, 0x15, 0xf4, 0x56
        };
        u8 value[] {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x16, 0x03, 0x03, 0x00, 0x10, 0x14, 0x00, 0x00, 0x0c, 0xa1, 0x91, 0x1a, 0x20, 0x59, 0xb5, 0x45, 0xa9, 0xb4, 0xad, 0x75, 0x3e
        };
        auto mac = hmac.process(value, 29);
        if (memcmp(result, mac.data, hmac.digest_size()) != 0) {
            FAIL(Invalid mac);
            print_buffer({ mac.data, hmac.digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((HMAC - SHA1 | Reuse));
        u8 key[] { 0xc8, 0x52, 0xe5, 0x4a, 0x2c, 0x03, 0x2b, 0xc9, 0x63, 0xd3, 0xc2, 0x79, 0x0f, 0x76, 0x43, 0xef, 0x36, 0xc3, 0x7a, 0xca };
        Crypto::Authentication::HMAC<Crypto::Hash::SHA1> hmac(ByteBuffer::wrap(key, 20));
        u8 result[] {
            0x2c, 0x57, 0x32, 0x61, 0x3b, 0xa7, 0x84, 0x87, 0x0e, 0x4f, 0x42, 0x07, 0x2f, 0xf0, 0xe7, 0x41, 0xd7, 0x15, 0xf4, 0x56
        };
        u8 value[] {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x16, 0x03, 0x03, 0x00, 0x10, 0x14, 0x00, 0x00, 0x0c, 0xa1, 0x91, 0x1a, 0x20, 0x59, 0xb5, 0x45, 0xa9, 0xb4, 0xad, 0x75, 0x3e
        };
        hmac.update(value, 8);
        hmac.update(value + 8, 5);
        hmac.update(value + 13, 16);
        auto mac = hmac.digest();
        if (memcmp(result, mac.data, hmac.digest_size()) != 0) {
            FAIL(Invalid mac);
            print_buffer({ mac.data, hmac.digest_size() }, -1);
        } else
            PASS;
    }
}

static int sha1_tests()
{
    sha1_test_name();
    sha1_test_hash();
    return g_some_test_failed ? 1 : 0;
}

static void sha1_test_name()
{
    I_TEST((SHA1 class name));
    Crypto::Hash::SHA1 sha;
    if (sha.class_name() != "SHA1") {
        FAIL(Invalid class name);
        printf("%s\n", sha.class_name().characters());
    } else
        PASS;
}

static void sha1_test_hash()
{
    {
        I_TEST((SHA256 Hashing | ""));
        u8 result[] {
            0xda, 0x39, 0xa3, 0xee, 0x5e, 0x6b, 0x4b, 0x0d, 0x32, 0x55, 0xbf, 0xef, 0x95, 0x60, 0x18, 0x90, 0xaf, 0xd8, 0x07, 0x09
        };
        auto digest = Crypto::Hash::SHA1::hash("");
        if (memcmp(result, digest.data, Crypto::Hash::SHA1::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::SHA1::digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((SHA256 Hashing | Long String));
        u8 result[] {
            0x12, 0x15, 0x1f, 0xb1, 0x04, 0x44, 0x93, 0xcc, 0xed, 0x54, 0xa6, 0xb8, 0x7e, 0x93, 0x37, 0x7b, 0xb2, 0x13, 0x39, 0xdb
        };
        auto digest = Crypto::Hash::SHA1::hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        if (memcmp(result, digest.data, Crypto::Hash::SHA1::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::SHA1::digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((SHA256 Hashing | Successive Updates));
        u8 result[] {
            0xd6, 0x6e, 0xce, 0xd1, 0xf4, 0x08, 0xc6, 0xd8, 0x35, 0xab, 0xf0, 0xc9, 0x05, 0x26, 0xa4, 0xb2, 0xb8, 0xa3, 0x7c, 0xd3
        };
        auto hasher = Crypto::Hash::SHA1 {};
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaaaaaaaa");
        hasher.update("aaaaaaaaa");
        auto digest = hasher.digest();
        if (memcmp(result, digest.data, Crypto::Hash::SHA1::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::SHA1::digest_size() }, -1);
        } else
            PASS;
    }
}

static int sha256_tests()
{
    sha256_test_name();
    sha256_test_hash();
    return g_some_test_failed ? 1 : 0;
}

static void sha256_test_name()
{
    I_TEST((SHA256 class name));
    Crypto::Hash::SHA256 sha;
    if (sha.class_name() != "SHA256") {
        FAIL(Invalid class name);
        printf("%s\n", sha.class_name().characters());
    } else
        PASS;
}

static void sha256_test_hash()
{
    {
        I_TEST((SHA256 Hashing | "Well hello friends"));
        u8 result[] {
            0x9a, 0xcd, 0x50, 0xf9, 0xa2, 0xaf, 0x37, 0xe4, 0x71, 0xf7, 0x61, 0xc3, 0xfe, 0x7b, 0x8d, 0xea, 0x56, 0x17, 0xe5, 0x1d, 0xac, 0x80, 0x2f, 0xe6, 0xc1, 0x77, 0xb7, 0x4a, 0xbf, 0x0a, 0xbb, 0x5a
        };
        auto digest = Crypto::Hash::SHA256::hash("Well hello friends");
        if (memcmp(result, digest.data, Crypto::Hash::SHA256::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::SHA256::digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((SHA256 Hashing | ""));
        u8 result[] {
            0xe3, 0xb0, 0xc4, 0x42, 0x98, 0xfc, 0x1c, 0x14, 0x9a, 0xfb, 0xf4, 0xc8, 0x99, 0x6f, 0xb9, 0x24, 0x27, 0xae, 0x41, 0xe4, 0x64, 0x9b, 0x93, 0x4c, 0xa4, 0x95, 0x99, 0x1b, 0x78, 0x52, 0xb8, 0x55
        };
        auto digest = Crypto::Hash::SHA256::hash("");
        if (memcmp(result, digest.data, Crypto::Hash::SHA256::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::SHA256::digest_size() }, -1);
        } else
            PASS;
    }
}

static void hmac_sha256_test_name()
{
    I_TEST((HMAC - SHA256 | Class name));
    Crypto::Authentication::HMAC<Crypto::Hash::SHA256> hmac("Well Hello Friends");
    if (hmac.class_name() != "HMAC-SHA256")
        FAIL(Invalid class name);
    else
        PASS;
}

static void hmac_sha256_test_process()
{
    {
        I_TEST((HMAC - SHA256 | Basic));
        Crypto::Authentication::HMAC<Crypto::Hash::SHA256> hmac("Well Hello Friends");
        u8 result[] {
            0x1a, 0xf2, 0x20, 0x62, 0xde, 0x3b, 0x84, 0x65, 0xc1, 0x25, 0x23, 0x99, 0x76, 0x15, 0x1b, 0xec, 0x15, 0x21, 0x82, 0x1f, 0x23, 0xca, 0x11, 0x66, 0xdd, 0x8c, 0x6e, 0xf1, 0x81, 0x3b, 0x7f, 0x1b
        };
        auto mac = hmac.process("Some bogus data");
        if (memcmp(result, mac.data, hmac.digest_size()) != 0) {
            FAIL(Invalid mac);
            print_buffer({ mac.data, hmac.digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((HMAC - SHA256 | DataSize > FinalBlockDataSize));
        Crypto::Authentication::HMAC<Crypto::Hash::SHA256> hmac("Well Hello Friends");
        u8 result[] = {
            0x9b, 0xa3, 0x9e, 0xf3, 0xb4, 0x30, 0x5f, 0x6f, 0x67, 0xd0, 0xa8, 0xb0, 0xf0, 0xcb, 0x12, 0xf5, 0x85, 0xe2, 0x19, 0xba, 0x0c, 0x8b, 0xe5, 0x43, 0xf0, 0x93, 0x39, 0xa8, 0xa3, 0x07, 0xf1, 0x95
        };
        auto mac = hmac.process("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        if (memcmp(result, mac.data, hmac.digest_size()) != 0) {
            FAIL(Invalid mac);
            print_buffer({ mac.data, hmac.digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((HMAC - SHA256 | DataSize == BlockSize));
        Crypto::Authentication::HMAC<Crypto::Hash::SHA256> hmac("Well Hello Friends");
        u8 result[] = {
            0x1d, 0x90, 0xce, 0x68, 0x45, 0x0b, 0xba, 0xd6, 0xbe, 0x1c, 0xb2, 0x3a, 0xea, 0x7f, 0xac, 0x4b, 0x68, 0x08, 0xa4, 0x77, 0x81, 0x2a, 0xad, 0x5d, 0x05, 0xe2, 0x15, 0xe8, 0xf4, 0xcb, 0x06, 0xaf
        };
        auto mac = hmac.process("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        if (memcmp(result, mac.data, hmac.digest_size()) != 0) {
            FAIL(Invalid mac);
            print_buffer({ mac.data, hmac.digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((HMAC - SHA256 | Reuse));
        Crypto::Authentication::HMAC<Crypto::Hash::SHA256> hmac("Well Hello Friends");

        auto mac_0 = hmac.process("Some bogus data");
        auto mac_1 = hmac.process("Some bogus data");

        if (memcmp(mac_0.data, mac_1.data, hmac.digest_size()) != 0) {
            FAIL(Cannot reuse);
        } else
            PASS;
    }
}

static int sha512_tests()
{
    sha512_test_name();
    sha512_test_hash();
    return g_some_test_failed ? 1 : 0;
}

static void sha512_test_name()
{
    I_TEST((SHA512 class name));
    Crypto::Hash::SHA512 sha;
    if (sha.class_name() != "SHA512") {
        FAIL(Invalid class name);
        printf("%s\n", sha.class_name().characters());
    } else
        PASS;
}

static void sha512_test_hash()
{
    {
        I_TEST((SHA512 Hashing | "Well hello friends"));
        u8 result[] {
            0x00, 0xfe, 0x68, 0x09, 0x71, 0x0e, 0xcb, 0x2b, 0xe9, 0x58, 0x00, 0x13, 0x69, 0x6a, 0x9e, 0x9e, 0xbd, 0x09, 0x1b, 0xfe, 0x14, 0xc9, 0x13, 0x82, 0xc7, 0x40, 0x34, 0xfe, 0xca, 0xe6, 0x87, 0xcb, 0x26, 0x36, 0x92, 0xe6, 0x34, 0x94, 0x3a, 0x11, 0xe5, 0xbb, 0xb5, 0xeb, 0x8e, 0x70, 0xef, 0x64, 0xca, 0xf7, 0x21, 0xb1, 0xde, 0xf2, 0x34, 0x85, 0x6f, 0xa8, 0x56, 0xd8, 0x23, 0xa1, 0x3b, 0x29
        };
        auto digest = Crypto::Hash::SHA512::hash("Well hello friends");
        if (memcmp(result, digest.data, Crypto::Hash::SHA512::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::SHA512::digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((SHA512 Hashing | ""));
        u8 result[] {
            0xcf, 0x83, 0xe1, 0x35, 0x7e, 0xef, 0xb8, 0xbd, 0xf1, 0x54, 0x28, 0x50, 0xd6, 0x6d, 0x80, 0x07, 0xd6, 0x20, 0xe4, 0x05, 0x0b, 0x57, 0x15, 0xdc, 0x83, 0xf4, 0xa9, 0x21, 0xd3, 0x6c, 0xe9, 0xce, 0x47, 0xd0, 0xd1, 0x3c, 0x5d, 0x85, 0xf2, 0xb0, 0xff, 0x83, 0x18, 0xd2, 0x87, 0x7e, 0xec, 0x2f, 0x63, 0xb9, 0x31, 0xbd, 0x47, 0x41, 0x7a, 0x81, 0xa5, 0x38, 0x32, 0x7a, 0xf9, 0x27, 0xda, 0x3e
        };
        auto digest = Crypto::Hash::SHA512::hash("");
        if (memcmp(result, digest.data, Crypto::Hash::SHA512::digest_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer({ digest.data, Crypto::Hash::SHA512::digest_size() }, -1);
        } else
            PASS;
    }
}

static void hmac_sha512_test_name()
{
    I_TEST((HMAC - SHA512 | Class name));
    Crypto::Authentication::HMAC<Crypto::Hash::SHA512> hmac("Well Hello Friends");
    if (hmac.class_name() != "HMAC-SHA512")
        FAIL(Invalid class name);
    else
        PASS;
}

static void hmac_sha512_test_process()
{
    {
        I_TEST((HMAC - SHA512 | Basic));
        Crypto::Authentication::HMAC<Crypto::Hash::SHA512> hmac("Well Hello Friends");
        u8 result[] {
            0xeb, 0xa8, 0x34, 0x11, 0xfd, 0x5b, 0x46, 0x5b, 0xef, 0xbb, 0x67, 0x5e, 0x7d, 0xc2, 0x7c, 0x2c, 0x6b, 0xe1, 0xcf, 0xe6, 0xc7, 0xe4, 0x7d, 0xeb, 0xca, 0x97, 0xb7, 0x4c, 0xd3, 0x4d, 0x6f, 0x08, 0x9f, 0x0d, 0x3a, 0xf1, 0xcb, 0x00, 0x79, 0x78, 0x2f, 0x05, 0x8e, 0xeb, 0x94, 0x48, 0x0d, 0x50, 0x64, 0x3b, 0xca, 0x70, 0xe2, 0x69, 0x38, 0x4f, 0xe4, 0xb0, 0x49, 0x0f, 0xc5, 0x4c, 0x7a, 0xa7
        };
        auto mac = hmac.process("Some bogus data");
        if (memcmp(result, mac.data, hmac.digest_size()) != 0) {
            FAIL(Invalid mac);
            print_buffer({ mac.data, hmac.digest_size() }, -1);
        } else
            PASS;
    }
    {
        I_TEST((HMAC - SHA512 | Reuse));
        Crypto::Authentication::HMAC<Crypto::Hash::SHA512> hmac("Well Hello Friends");

        auto mac_0 = hmac.process("Some bogus data");
        auto mac_1 = hmac.process("Some bogus data");

        if (memcmp(mac_0.data, mac_1.data, hmac.digest_size()) != 0) {
            FAIL(Cannot reuse);
        } else
            PASS;
    }
}

static int rsa_tests()
{
    rsa_test_encrypt();
    rsa_test_der_parse();
    bigint_test_number_theory();
    rsa_test_encrypt_decrypt();
    rsa_emsa_pss_test_create();
    return g_some_test_failed ? 1 : 0;
}

static void rsa_test_encrypt()
{
    {
        I_TEST((RSA RAW | Encryption));
        ByteBuffer data { "hellohellohellohellohellohellohellohellohellohellohellohello123-"_b };
        u8 result[] { 0x6f, 0x7b, 0xe2, 0xd3, 0x95, 0xf8, 0x8d, 0x87, 0x6d, 0x10, 0x5e, 0xc3, 0xcd, 0xf7, 0xbb, 0xa6, 0x62, 0x8e, 0x45, 0xa0, 0xf1, 0xe5, 0x0f, 0xdf, 0x69, 0xcb, 0xb6, 0xd5, 0x42, 0x06, 0x7d, 0x72, 0xa9, 0x5e, 0xae, 0xbf, 0xbf, 0x0f, 0xe0, 0xeb, 0x31, 0x31, 0xca, 0x8a, 0x81, 0x1e, 0xb9, 0xec, 0x6d, 0xcc, 0xb8, 0xa4, 0xac, 0xa3, 0x31, 0x05, 0xa9, 0xac, 0xc9, 0xd3, 0xe6, 0x2a, 0x18, 0xfe };
        Crypto::PK::RSA rsa(
            "8126832723025844890518845777858816391166654950553329127845898924164623511718747856014227624997335860970996746552094406240834082304784428582653994490504519"_bigint,
            "4234603516465654167360850580101327813936403862038934287300450163438938741499875303761385527882335478349599685406941909381269804396099893549838642251053393"_bigint,
            "65537"_bigint);
        u8 buffer[rsa.output_size()];
        auto buf = ByteBuffer::wrap(buffer, sizeof(buffer));
        rsa.encrypt(data, buf);
        if (memcmp(result, buf.data(), buf.size())) {
            FAIL(Invalid encryption result);
            print_buffer(buf, 16);
        } else {
            PASS;
        }
    }
    {
        I_TEST((RSA PKCS #1 1.5 | Encryption));
        ByteBuffer data { "hellohellohellohellohellohellohellohellohello123-"_b };
        Crypto::PK::RSA_PKCS1_EME rsa(
            "8126832723025844890518845777858816391166654950553329127845898924164623511718747856014227624997335860970996746552094406240834082304784428582653994490504519"_bigint,
            "4234603516465654167360850580101327813936403862038934287300450163438938741499875303761385527882335478349599685406941909381269804396099893549838642251053393"_bigint,
            "65537"_bigint);
        u8 buffer[rsa.output_size()];
        auto buf = ByteBuffer::wrap(buffer, sizeof(buffer));
        rsa.encrypt(data, buf);
        rsa.decrypt(buf, buf);

        if (memcmp(buf.data(), "hellohellohellohellohellohellohellohellohello123-", 49))
            FAIL(Invalid encryption);
        else {
            dbg() << "out size " << buf.size() << " values: " << StringView { (char*)buf.data(), buf.size() };

            PASS;
        }
    }
}

static void bigint_test_number_theory()
{
    {
        I_TEST((Number Theory | Modular Inverse));
        if (Crypto::NumberTheory::ModularInverse(7, 87) == 25) {
            PASS;
        } else {
            FAIL(Invalid result);
        }
    }
    {
        struct {
            Crypto::UnsignedBigInteger base;
            Crypto::UnsignedBigInteger exp;
            Crypto::UnsignedBigInteger mod;
            Crypto::UnsignedBigInteger expected;
        } mod_pow_tests[] = {
            { "2988348162058574136915891421498819466320163312926952423791023078876139"_bigint, "2351399303373464486466122544523690094744975233415544072992656881240319"_bigint, "10000"_bigint, "3059"_bigint },
            { "24231"_bigint, "12448"_bigint, "14679"_bigint, "4428"_bigint },
            { "1005404"_bigint, "8352654"_bigint, "8161408"_bigint, "2605696"_bigint },
            { "3665005778"_bigint, "3244425589"_bigint, "565668506"_bigint, "524766494"_bigint },
            { "10662083169959689657"_bigint, "11605678468317533000"_bigint, "1896834583057209739"_bigint, "1292743154593945858"_bigint },
            { "99667739213529524852296932424683448520"_bigint, "123394910770101395416306279070921784207"_bigint, "238026722756504133786938677233768788719"_bigint, "197165477545023317459748215952393063201"_bigint },
            { "49368547511968178788919424448914214709244872098814465088945281575062739912239"_bigint, "25201856190991298572337188495596990852134236115562183449699512394891190792064"_bigint, "45950460777961491021589776911422805972195170308651734432277141467904883064645"_bigint, "39917885806532796066922509794537889114718612292469285403012781055544152450051"_bigint },
            { "48399385336454791246880286907257136254351739111892925951016159217090949616810"_bigint, "5758661760571644379364752528081901787573279669668889744323710906207949658569"_bigint, "32812120644405991429173950312949738783216437173380339653152625840449006970808"_bigint, "7948464125034399875323770213514649646309423451213282653637296324080400293584"_bigint },
        };

        for (auto test_case : mod_pow_tests) {
            I_TEST((Number Theory | Modular Power));
            auto actual = Crypto::NumberTheory::ModularPower(
                test_case.base, test_case.exp, test_case.mod);

            if (actual == test_case.expected) {
                PASS;
            } else {
                FAIL(Wrong result);
                printf("b: %s\ne: %s\nm: %s\nexpect: %s\nactual: %s\n",
                    test_case.base.to_base10().characters(), test_case.exp.to_base10().characters(), test_case.mod.to_base10().characters(), test_case.expected.to_base10().characters(), actual.to_base10().characters());
            }
        }
    }
    {
        struct {
            Crypto::UnsignedBigInteger candidate;
            bool expected_result;
        } primality_tests[] = {
            { "1180591620717411303424"_bigint, false },                  // 2**70
            { "620448401733239439360000"_bigint, false },                // 25!
            { "953962166440690129601298432"_bigint, false },             // 12**25
            { "620448401733239439360000"_bigint, false },                // 25!
            { "147926426347074375"_bigint, false },                      // 35! / 2**32
            { "340282366920938429742726440690708343523"_bigint, false }, // 2 factors near 2^64
            { "73"_bigint, true },
            { "6967"_bigint, true },
            { "787649"_bigint, true },
            { "73513949"_bigint, true },
            { "6691236901"_bigint, true },
            { "741387182759"_bigint, true },
            { "67466615915827"_bigint, true },
            { "9554317039214687"_bigint, true },
            { "533344522150170391"_bigint, true },
            { "18446744073709551557"_bigint, true }, // just below 2**64
        };

        for (auto test_case : primality_tests) {
            I_TEST((Number Theory | Primality));
            bool actual_result = Crypto::NumberTheory::is_probably_prime(test_case.candidate);
            if (test_case.expected_result == actual_result) {
                PASS;
            } else {
                FAIL(Wrong primality guess);
                printf("The number %s is %sa prime, but the test said it is %sa prime!\n",
                    test_case.candidate.to_base10().characters(), test_case.expected_result ? "" : "not ", actual_result ? "" : "not ");
            }
        }
    }
    {
        struct {
            Crypto::UnsignedBigInteger min;
            Crypto::UnsignedBigInteger max;
        } primality_tests[] = {
            { "1"_bigint, "1000000"_bigint },
            { "10000000000"_bigint, "20000000000"_bigint },
            { "1000"_bigint, "200000000000000000"_bigint },
            { "200000000000000000"_bigint, "200000000000010000"_bigint },
        };

        for (auto test_case : primality_tests) {
            I_TEST((Number Theory | Random numbers));
            auto actual_result = Crypto::NumberTheory::random_number(test_case.min, test_case.max);
            if (actual_result < test_case.min) {
                FAIL(Too small);
                printf("The generated number %s is smaller than the requested minimum %s. (max = %s)\n", actual_result.to_base10().characters(), test_case.min.to_base10().characters(), test_case.max.to_base10().characters());
            } else if (!(actual_result < test_case.max)) {
                FAIL(Too large);
                printf("The generated number %s is larger-or-equal to the requested maximum %s. (min = %s)\n", actual_result.to_base10().characters(), test_case.max.to_base10().characters(), test_case.min.to_base10().characters());
            } else {
                PASS;
            }
        }
    }
    {
        I_TEST((Number Theory | Random distribution));
        auto actual_result = Crypto::NumberTheory::random_number(
            "1"_bigint,
            "100000000000000000000000000000"_bigint);         // 10**29
        if (actual_result < "100000000000000000000"_bigint) { // 10**20
            FAIL(Too small);
            printf("The generated number %s is extremely small. This *can* happen by pure chance, but should happen only once in a billion times. So it's probably an error.\n", actual_result.to_base10().characters());
        } else if ("99999999900000000000000000000"_bigint < actual_result) { // 10**29 - 10**20
            FAIL(Too large);
            printf("The generated number %s is extremely large. This *can* happen by pure chance, but should happen only once in a billion times. So it's probably an error.\n", actual_result.to_base10().characters());
        } else {
            PASS;
        }
    }
}

static void rsa_emsa_pss_test_create()
{
    {
        // This is a template validity test
        I_TEST((RSA EMSA_PSS | Construction));
        Crypto::PK::RSA rsa;
        Crypto::PK::RSA_EMSA_PSS<Crypto::Hash::SHA256> rsa_esma_pss(rsa);
        PASS;
    }
}

static void rsa_test_der_parse()
{
    I_TEST((RSA | ASN1 DER / PEM encoded Key import));
    auto privkey = R"(-----BEGIN RSA PRIVATE KEY-----
MIIBOgIBAAJBAJsrIYHxs1YL9tpfodaWs1lJoMdF4kgFisUFSj6nvBhJUlmBh607AlgTaX0E
DGPYycXYGZ2n6rqmms5lpDXBpUcCAwEAAQJAUNpPkmtEHDENxsoQBUXvXDYeXdePSiIBJhpU
joNOYoR5R9z5oX2cpcyykQ58FC2vKKg+x8N6xczG7qO95tw5UQIhAN354CP/FA+uTeJ6KJ+i
zCBCl58CjNCzO0s5HTc56el5AiEAsvPKXo5/9gS/S4UzDRP6abq7GreixTfjR8LXidk3FL8C
IQCTjYI861Y+hjMnlORkGSdvWlTHUj6gjEOh4TlWeJzQoQIgAxMZOQKtxCZUuxFwzRq4xLRG
nrDlBQpuxz7bwSyQO7UCIHrYMnDohgNbwtA5ZpW3H1cKKQQvueWm6sxW9P5sUrZ3
-----END RSA PRIVATE KEY-----)";

    Crypto::PK::RSA rsa(privkey);
    if (rsa.public_key().public_exponent() == 65537) {
        if (rsa.private_key().private_exponent() == "4234603516465654167360850580101327813936403862038934287300450163438938741499875303761385527882335478349599685406941909381269804396099893549838642251053393"_bigint) {
            PASS;
        } else
            FAIL(Invalid private exponent);
    } else {
        FAIL(Invalid public exponent);
    }
}

static void rsa_test_encrypt_decrypt()
{
    I_TEST((RSA | Encrypt));
    dbg() << " creating rsa object";
    Crypto::PK::RSA rsa(
        "9527497237087650398000977129550904920919162360737979403539302312977329868395261515707123424679295515888026193056908173564681660256268221509339074678416049"_bigint,
        "39542231845947188736992321577701849924317746648774438832456325878966594812143638244746284968851807975097653255909707366086606867657273809465195392910913"_bigint,
        "65537"_bigint);
    dbg() << "Output size: " << rsa.output_size();
    auto dec = ByteBuffer::create_zeroed(rsa.output_size());
    auto enc = ByteBuffer::create_zeroed(rsa.output_size());
    enc.overwrite(0, "WellHelloFriendsWellHelloFriendsWellHelloFriendsWellHelloFriends", 64);

    rsa.encrypt(enc, dec);
    rsa.decrypt(dec, enc);

    dbg() << "enc size " << enc.size() << " dec size " << dec.size();

    if (memcmp(enc.data(), "WellHelloFriendsWellHelloFriendsWellHelloFriendsWellHelloFriends", 64) != 0) {
        FAIL(Could not encrypt then decrypt);
    } else {
        PASS;
    }
}

static int tls_tests()
{
    tls_test_client_hello();
    return g_some_test_failed ? 1 : 0;
}

static void tls_test_client_hello()
{
    I_TEST((TLS | Connect and Data Transfer));
    Core::EventLoop loop;
    RefPtr<TLS::TLSv12> tls = TLS::TLSv12::construct(nullptr);
    bool sent_request = false;
    ByteBuffer contents = ByteBuffer::create_uninitialized(0);
    tls->on_tls_ready_to_write = [&](TLS::TLSv12& tls) {
        if (sent_request)
            return;
        sent_request = true;
        if (!tls.write("GET / HTTP/1.1\r\nHost: "_b)) {
            FAIL(write(0) failed);
            loop.quit(0);
        }
        auto* the_server = (const u8*)(server ?: DEFAULT_SERVER);
        if (!tls.write(ByteBuffer::wrap(const_cast<u8*>(the_server), strlen((const char*)the_server)))) {
            FAIL(write(1) failed);
            loop.quit(0);
        }
        if (!tls.write("\r\nConnection : close\r\n\r\n"_b)) {
            FAIL(write(2) failed);
            loop.quit(0);
        }
    };
    tls->on_tls_ready_to_read = [&](TLS::TLSv12& tls) {
        auto data = tls.read();
        if (!data.has_value()) {
            FAIL(No data received);
            loop.quit(1);
        } else {
            //            print_buffer(data.value(), 16);
            contents.append(data.value().data(), data.value().size());
        }
    };
    tls->on_tls_finished = [&] {
        PASS;
        auto file = Core::File::open("foo.response", Core::IODevice::WriteOnly);
        if (file.is_error()) {
            printf("Can't write there, %s\n", file.error().characters());
            loop.quit(2);
            return;
        }
        file.value()->write(contents);
        file.value()->close();
        loop.quit(0);
    };
    tls->on_tls_error = [&](TLS::AlertDescription) {
        FAIL(Connection failure);
        loop.quit(1);
    };
    if (!tls->connect(server ?: DEFAULT_SERVER, port)) {
        FAIL(connect() failed);
        return;
    }
    loop.exec();
}

static int adler32_tests()
{
    auto do_test = [](ReadonlyBytes input, u32 expected_result) {
        I_TEST((CRC32));

        auto pass = Crypto::Checksum::Adler32(input).digest() == expected_result;

        if (pass) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    };

    do_test(String("").bytes(), 0x1);
    do_test(String("a").bytes(), 0x00620062);
    do_test(String("abc").bytes(), 0x024d0127);
    do_test(String("message digest").bytes(), 0x29750586);
    do_test(String("abcdefghijklmnopqrstuvwxyz").bytes(), 0x90860b20);

    return g_some_test_failed ? 1 : 0;
}

static int crc32_tests()
{
    auto do_test = [](ReadonlyBytes input, u32 expected_result) {
        I_TEST((Adler32));

        auto pass = Crypto::Checksum::CRC32(input).digest() == expected_result;

        if (pass) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    };

    do_test(String("").bytes(), 0x0);
    do_test(String("The quick brown fox jumps over the lazy dog").bytes(), 0x414FA339);
    do_test(String("various CRC algorithms input data").bytes(), 0x9BD366AE);

    return g_some_test_failed ? 1 : 0;
}

static int bigint_tests()
{
    bigint_test_fibo500();
    bigint_addition_edgecases();
    bigint_subtraction();
    bigint_multiplication();
    bigint_division();
    bigint_base10();
    bigint_import_export();
    bigint_bitwise();

    bigint_test_signed_fibo500();
    bigint_signed_addition_edgecases();
    bigint_signed_subtraction();
    bigint_signed_multiplication();
    bigint_signed_division();
    bigint_signed_base10();
    bigint_signed_import_export();
    bigint_signed_bitwise();

    return g_some_test_failed ? 1 : 0;
}

static Crypto::UnsignedBigInteger bigint_fibonacci(size_t n)
{
    Crypto::UnsignedBigInteger num1(0);
    Crypto::UnsignedBigInteger num2(1);
    for (size_t i = 0; i < n; ++i) {
        Crypto::UnsignedBigInteger t = num1.plus(num2);
        num2 = num1;
        num1 = t;
    }
    return num1;
}

static Crypto::SignedBigInteger bigint_signed_fibonacci(size_t n)
{
    Crypto::SignedBigInteger num1(0);
    Crypto::SignedBigInteger num2(1);
    for (size_t i = 0; i < n; ++i) {
        Crypto::SignedBigInteger t = num1.plus(num2);
        num2 = num1;
        num1 = t;
    }
    return num1;
}
static void bigint_test_fibo500()
{
    {
        I_TEST((BigInteger | Fibonacci500));
        bool pass = (bigint_fibonacci(500).words() == AK::Vector<u32> { 315178285, 505575602, 1883328078, 125027121, 3649625763, 347570207, 74535262, 3832543808, 2472133297, 1600064941, 65273441 });

        if (pass) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}

static void bigint_addition_edgecases()
{
    {
        I_TEST((BigInteger | Edge Cases));
        Crypto::UnsignedBigInteger num1;
        Crypto::UnsignedBigInteger num2(70);
        Crypto::UnsignedBigInteger num3 = num1.plus(num2);
        bool pass = (num3 == num2);
        pass &= (num1 == Crypto::UnsignedBigInteger(0));

        if (pass) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | Borrow with zero));
        Crypto::UnsignedBigInteger num1({ UINT32_MAX - 3, UINT32_MAX });
        Crypto::UnsignedBigInteger num2({ UINT32_MAX - 2, 0 });
        if (num1.plus(num2).words() == Vector<u32> { 4294967289, 0, 1 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}

static void bigint_subtraction()
{
    {
        I_TEST((BigInteger | Simple Subtraction 1));
        Crypto::UnsignedBigInteger num1(80);
        Crypto::UnsignedBigInteger num2(70);

        if (num1.minus(num2) == Crypto::UnsignedBigInteger(10)) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | Simple Subtraction 2));
        Crypto::UnsignedBigInteger num1(50);
        Crypto::UnsignedBigInteger num2(70);

        if (num1.minus(num2).is_invalid()) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | Subtraction with borrow));
        Crypto::UnsignedBigInteger num1(UINT32_MAX);
        Crypto::UnsignedBigInteger num2(1);
        Crypto::UnsignedBigInteger num3 = num1.plus(num2);
        Crypto::UnsignedBigInteger result = num3.minus(num2);
        if (result == num1) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | Subtraction with large numbers));
        Crypto::UnsignedBigInteger num1 = bigint_fibonacci(343);
        Crypto::UnsignedBigInteger num2 = bigint_fibonacci(218);
        Crypto::UnsignedBigInteger result = num1.minus(num2);
        if ((result.plus(num2) == num1)
            && (result.words() == Vector<u32> { 811430588, 2958904896, 1130908877, 2830569969, 3243275482, 3047460725, 774025231, 7990 })) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | Subtraction with large numbers 2));
        Crypto::UnsignedBigInteger num1(Vector<u32> { 1483061863, 446680044, 1123294122, 191895498, 3347106536, 16, 0, 0, 0 });
        Crypto::UnsignedBigInteger num2(Vector<u32> { 4196414175, 1117247942, 1123294122, 191895498, 3347106536, 16 });
        Crypto::UnsignedBigInteger result = num1.minus(num2);
        // this test only verifies that we don't crash on an assertion
        PASS;
    }
    {
        I_TEST((BigInteger | Subtraction Regression 1));
        auto num = Crypto::UnsignedBigInteger { 1 }.shift_left(256);
        if (num.minus(1).words() == Vector<u32> { 4294967295, 4294967295, 4294967295, 4294967295, 4294967295, 4294967295, 4294967295, 4294967295, 0 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}

static void bigint_multiplication()
{
    {
        I_TEST((BigInteger | Simple Multiplication));
        Crypto::UnsignedBigInteger num1(8);
        Crypto::UnsignedBigInteger num2(251);
        Crypto::UnsignedBigInteger result = num1.multiplied_by(num2);
        if (result.words() == Vector<u32> { 2008 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | Multiplications with big numbers 1));
        Crypto::UnsignedBigInteger num1 = bigint_fibonacci(200);
        Crypto::UnsignedBigInteger num2(12345678);
        Crypto::UnsignedBigInteger result = num1.multiplied_by(num2);
        if (result.words() == Vector<u32> { 669961318, 143970113, 4028714974, 3164551305, 1589380278, 2 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | Multiplications with big numbers 2));
        Crypto::UnsignedBigInteger num1 = bigint_fibonacci(200);
        Crypto::UnsignedBigInteger num2 = bigint_fibonacci(341);
        Crypto::UnsignedBigInteger result = num1.multiplied_by(num2);
        if (result.words() == Vector<u32> { 3017415433, 2741793511, 1957755698, 3731653885, 3154681877, 785762127, 3200178098, 4260616581, 529754471, 3632684436, 1073347813, 2516430 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}
static void bigint_division()
{
    {
        I_TEST((BigInteger | Simple Division));
        Crypto::UnsignedBigInteger num1(27194);
        Crypto::UnsignedBigInteger num2(251);
        auto result = num1.divided_by(num2);
        Crypto::UnsignedDivisionResult expected = { Crypto::UnsignedBigInteger(108), Crypto::UnsignedBigInteger(86) };
        if (result.quotient == expected.quotient && result.remainder == expected.remainder) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | Division with big numbers));
        Crypto::UnsignedBigInteger num1 = bigint_fibonacci(386);
        Crypto::UnsignedBigInteger num2 = bigint_fibonacci(238);
        auto result = num1.divided_by(num2);
        Crypto::UnsignedDivisionResult expected = {
            Crypto::UnsignedBigInteger(Vector<u32> { 2300984486, 2637503534, 2022805584, 107 }),
            Crypto::UnsignedBigInteger(Vector<u32> { 1483061863, 446680044, 1123294122, 191895498, 3347106536, 16, 0, 0, 0 })
        };
        if (result.quotient == expected.quotient && result.remainder == expected.remainder) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | Combined test));
        auto num1 = bigint_fibonacci(497);
        auto num2 = bigint_fibonacci(238);
        auto div_result = num1.divided_by(num2);
        if (div_result.quotient.multiplied_by(num2).plus(div_result.remainder) == num1) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}

static void bigint_base10()
{
    {
        I_TEST((BigInteger | From String));
        auto result = Crypto::UnsignedBigInteger::from_base10("57195071295721390579057195715793");
        if (result.words() == Vector<u32> { 3806301393, 954919431, 3879607298, 721 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((BigInteger | To String));
        auto result = Crypto::UnsignedBigInteger { Vector<u32> { 3806301393, 954919431, 3879607298, 721 } }.to_base10();
        if (result == "57195071295721390579057195715793") {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}

static void bigint_import_export()
{
    {
        I_TEST((BigInteger | BigEndian Decode / Encode roundtrip));
        u8 random_bytes[128];
        u8 target_buffer[128];
        AK::fill_with_random(random_bytes, 128);
        auto encoded = Crypto::UnsignedBigInteger::import_data(random_bytes, 128);
        encoded.export_data({ target_buffer, 128 });
        if (memcmp(target_buffer, random_bytes, 128) != 0)
            FAIL(Could not roundtrip);
        else
            PASS;
    }
    {
        I_TEST((BigInteger | BigEndian Encode / Decode roundtrip));
        u8 target_buffer[128];
        auto encoded = "12345678901234567890"_bigint;
        auto size = encoded.export_data({ target_buffer, 128 });
        auto decoded = Crypto::UnsignedBigInteger::import_data(target_buffer, size);
        if (encoded != decoded)
            FAIL(Could not roundtrip);
        else
            PASS;
    }
    {
        I_TEST((BigInteger | BigEndian Import));
        auto number = Crypto::UnsignedBigInteger::import_data("hello");
        if (number == "448378203247"_bigint) {
            PASS;
        } else {
            FAIL(Invalid value);
        }
    }
    {
        I_TEST((BigInteger | BigEndian Export));
        auto number = "448378203247"_bigint;
        char exported[8] { 0 };
        auto exported_length = number.export_data({ exported, 8 }, true);
        if (exported_length == 5 && memcmp(exported + 3, "hello", 5) == 0) {
            PASS;
        } else {
            FAIL(Invalid value);
            print_buffer({ exported - exported_length + 8, exported_length }, -1);
        }
    }
}

static void bigint_bitwise()
{
    {
        I_TEST((BigInteger | Basic bitwise or));
        auto num1 = "1234567"_bigint;
        auto num2 = "1234567"_bigint;
        if (num1.bitwise_or(num2) == num1) {
            PASS;
        } else {
            FAIL(Invalid value);
        }
    }
    {
        I_TEST((BigInteger | Bitwise or handles different lengths));
        auto num1 = "1234567"_bigint;
        auto num2 = "123456789012345678901234567890"_bigint;
        auto expected = "123456789012345678901234622167"_bigint;
        auto result = num1.bitwise_or(num2);
        if (result == expected) {
            PASS;
        } else {
            FAIL(Invalid value);
        }
    }
    {
        I_TEST((BigInteger | Basic bitwise and));
        auto num1 = "1234567"_bigint;
        auto num2 = "1234561"_bigint;
        if (num1.bitwise_and(num2) == "1234561"_bigint) {
            PASS;
        } else {
            FAIL(Invalid value);
        }
    }
    {
        I_TEST((BigInteger | Bitwise and handles different lengths));
        auto num1 = "1234567"_bigint;
        auto num2 = "123456789012345678901234567890"_bigint;
        if (num1.bitwise_and(num2) == "1180290"_bigint) {
            PASS;
        } else {
            FAIL(Invalid value);
        }
    }
    {
        I_TEST((BigInteger | Basic bitwise xor));
        auto num1 = "1234567"_bigint;
        auto num2 = "1234561"_bigint;
        if (num1.bitwise_xor(num2) == 6) {
            PASS;
        } else {
            FAIL(Invalid value);
        }
    }
    {
        I_TEST((BigInteger | Bitwise xor handles different lengths));
        auto num1 = "1234567"_bigint;
        auto num2 = "123456789012345678901234567890"_bigint;
        if (num1.bitwise_xor(num2) == "123456789012345678901233441877"_bigint) {
            PASS;
        } else {
            FAIL(Invalid value);
        }
    }
}

static void bigint_test_signed_fibo500()
{
    {
        I_TEST((Signed BigInteger | Fibonacci500));
        bool pass = (bigint_signed_fibonacci(500).unsigned_value().words() == AK::Vector<u32> { 315178285, 505575602, 1883328078, 125027121, 3649625763, 347570207, 74535262, 3832543808, 2472133297, 1600064941, 65273441 });

        if (pass) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}

static void bigint_signed_addition_edgecases()
{
    {
        I_TEST((Signed BigInteger | Borrow with zero));
        Crypto::SignedBigInteger num1 { Crypto::UnsignedBigInteger { { UINT32_MAX - 3, UINT32_MAX } }, false };
        Crypto::SignedBigInteger num2 { Crypto::UnsignedBigInteger { UINT32_MAX - 2 }, false };
        if (num1.plus(num2).unsigned_value().words() == Vector<u32> { 4294967289, 0, 1 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | Addition to other sign));
        Crypto::SignedBigInteger num1 = INT32_MAX;
        Crypto::SignedBigInteger num2 = num1;
        num2.negate();
        if (num1.plus(num2) == Crypto::SignedBigInteger { 0 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}

static void bigint_signed_subtraction()
{
    {
        I_TEST((Signed BigInteger | Simple Subtraction 1));
        Crypto::SignedBigInteger num1(80);
        Crypto::SignedBigInteger num2(70);

        if (num1.minus(num2) == Crypto::SignedBigInteger(10)) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | Simple Subtraction 2));
        Crypto::SignedBigInteger num1(50);
        Crypto::SignedBigInteger num2(70);

        if (num1.minus(num2) == Crypto::SignedBigInteger { -20 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | Subtraction with borrow));
        Crypto::SignedBigInteger num1(Crypto::UnsignedBigInteger { UINT32_MAX });
        Crypto::SignedBigInteger num2(1);
        Crypto::SignedBigInteger num3 = num1.plus(num2);
        Crypto::SignedBigInteger result = num2.minus(num3);
        num1.negate();
        if (result == num1) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | Subtraction with large numbers));
        Crypto::SignedBigInteger num1 = bigint_signed_fibonacci(343);
        Crypto::SignedBigInteger num2 = bigint_signed_fibonacci(218);
        Crypto::SignedBigInteger result = num2.minus(num1);
        auto expected = Crypto::UnsignedBigInteger { Vector<u32> { 811430588, 2958904896, 1130908877, 2830569969, 3243275482, 3047460725, 774025231, 7990 } };
        if ((result.plus(num1) == num2)
            && (result.unsigned_value() == expected)) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | Subtraction with large numbers 2));
        Crypto::SignedBigInteger num1(Crypto::UnsignedBigInteger { Vector<u32> { 1483061863, 446680044, 1123294122, 191895498, 3347106536, 16, 0, 0, 0 } });
        Crypto::SignedBigInteger num2(Crypto::UnsignedBigInteger { Vector<u32> { 4196414175, 1117247942, 1123294122, 191895498, 3347106536, 16 } });
        Crypto::SignedBigInteger result = num1.minus(num2);
        // this test only verifies that we don't crash on an assertion
        PASS;
    }
}

static void bigint_signed_multiplication()
{
    {
        I_TEST((Signed BigInteger | Simple Multiplication));
        Crypto::SignedBigInteger num1(8);
        Crypto::SignedBigInteger num2(-251);
        Crypto::SignedBigInteger result = num1.multiplied_by(num2);
        if (result == Crypto::SignedBigInteger { -2008 }) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | Multiplications with big numbers 1));
        Crypto::SignedBigInteger num1 = bigint_signed_fibonacci(200);
        Crypto::SignedBigInteger num2(-12345678);
        Crypto::SignedBigInteger result = num1.multiplied_by(num2);
        if (result.unsigned_value().words() == Vector<u32> { 669961318, 143970113, 4028714974, 3164551305, 1589380278, 2 } && result.is_negative()) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | Multiplications with big numbers 2));
        Crypto::SignedBigInteger num1 = bigint_signed_fibonacci(200);
        Crypto::SignedBigInteger num2 = bigint_signed_fibonacci(341);
        num1.negate();
        Crypto::SignedBigInteger result = num1.multiplied_by(num2);
        if (result.unsigned_value().words() == Vector<u32> { 3017415433, 2741793511, 1957755698, 3731653885, 3154681877, 785762127, 3200178098, 4260616581, 529754471, 3632684436, 1073347813, 2516430 } && result.is_negative()) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}
static void bigint_signed_division()
{
    {
        I_TEST((Signed BigInteger | Simple Division));
        Crypto::SignedBigInteger num1(27194);
        Crypto::SignedBigInteger num2(-251);
        auto result = num1.divided_by(num2);
        Crypto::SignedDivisionResult expected = { Crypto::SignedBigInteger(-108), Crypto::SignedBigInteger(86) };
        if (result.quotient == expected.quotient && result.remainder == expected.remainder) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | Division with big numbers));
        Crypto::SignedBigInteger num1 = bigint_signed_fibonacci(386);
        Crypto::SignedBigInteger num2 = bigint_signed_fibonacci(238);
        num1.negate();
        auto result = num1.divided_by(num2);
        Crypto::SignedDivisionResult expected = {
            Crypto::SignedBigInteger(Crypto::UnsignedBigInteger { Vector<u32> { 2300984486, 2637503534, 2022805584, 107 } }, true),
            Crypto::SignedBigInteger(Crypto::UnsignedBigInteger { Vector<u32> { 1483061863, 446680044, 1123294122, 191895498, 3347106536, 16, 0, 0, 0 } }, true)
        };
        if (result.quotient == expected.quotient && result.remainder == expected.remainder) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | Combined test));
        auto num1 = bigint_signed_fibonacci(497);
        auto num2 = bigint_signed_fibonacci(238);
        num1.negate();
        auto div_result = num1.divided_by(num2);
        if (div_result.quotient.multiplied_by(num2).plus(div_result.remainder) == num1) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}

static void bigint_signed_base10()
{
    {
        I_TEST((Signed BigInteger | From String));
        auto result = Crypto::SignedBigInteger::from_base10("-57195071295721390579057195715793");
        if (result.unsigned_value().words() == Vector<u32> { 3806301393, 954919431, 3879607298, 721 } && result.is_negative()) {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
    {
        I_TEST((Signed BigInteger | To String));
        auto result = Crypto::SignedBigInteger { Crypto::UnsignedBigInteger { Vector<u32> { 3806301393, 954919431, 3879607298, 721 } }, true }.to_base10();
        if (result == "-57195071295721390579057195715793") {
            PASS;
        } else {
            FAIL(Incorrect Result);
        }
    }
}

static void bigint_signed_import_export()
{
    {
        I_TEST((Signed BigInteger | BigEndian Decode / Encode roundtrip));
        u8 random_bytes[129];
        u8 target_buffer[129];
        random_bytes[0] = 1;
        AK::fill_with_random(random_bytes + 1, 128);
        auto encoded = Crypto::SignedBigInteger::import_data(random_bytes, 129);
        encoded.export_data({ target_buffer, 129 });
        if (memcmp(target_buffer, random_bytes, 129) != 0)
            FAIL(Could not roundtrip);
        else
            PASS;
    }
    {
        I_TEST((Signed BigInteger | BigEndian Encode / Decode roundtrip));
        u8 target_buffer[128];
        auto encoded = "-12345678901234567890"_sbigint;
        auto size = encoded.export_data({ target_buffer, 128 });
        auto decoded = Crypto::SignedBigInteger::import_data(target_buffer, size);
        if (encoded != decoded)
            FAIL(Could not roundtrip);
        else
            PASS;
    }
}

static void bigint_signed_bitwise()
{
    {
        I_TEST((Signed BigInteger | Bitwise or handles sign));
        auto num1 = "-1234567"_sbigint;
        auto num2 = "1234567"_sbigint;
        if (num1.bitwise_or(num2) == num1) {
            PASS;
        } else {
            FAIL(Invalid value);
        }
    }
}
