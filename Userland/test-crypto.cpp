#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibCrypto/Hash/MD5.h>
#include <LibLine/Editor.h>
#include <stdio.h>

static const char* secret_key = "WellHelloFreinds";
static const char* suite = nullptr;
static const char* filename = nullptr;
static int key_bits = 128;
static bool binary = false;
static bool interactive = false;
static bool run_tests = false;

static bool encrypting = true;

constexpr const char* DEFAULT_HASH_SUITE { "MD5" };
constexpr const char* DEFAULT_CIPHER_SUITE { "AES_CBC" };

// listAllTests
// Cipher
int aes_cbc_tests();

// Hash
int md5_tests();

// stop listing tests

void print_buffer(const ByteBuffer& buffer, int split)
{
    for (size_t i = 0; i < buffer.size(); ++i) {
        if (split > 0) {
            if (i % split == 0 && i)
                puts("");
        }
        printf("%02x", buffer[i]);
    }
    puts("");
}

int run(Function<void(const char*, size_t)> fn)
{
    if (interactive) {
        Line::Editor editor;
        editor.initialize();
        for (;;) {
            auto line = editor.get_line("> ");
            fn(line.characters(), line.length());
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
        auto buffer = file->read_all();
        fn((const char*)buffer.data(), buffer.size());
    }
    return 0;
}

void aes_cbc(const char* message, size_t len)
{
    auto buffer = ByteBuffer::wrap(message, len);
    // FIXME: Take iv as an optional parameter
    auto iv = ByteBuffer::create_zeroed(Crypto::AESCipher::block_size());

    if (encrypting) {
        Crypto::AESCipher::CBCMode cipher(secret_key, key_bits, Crypto::Intent::Encryption);

        auto enc = cipher.create_aligned_buffer(buffer.size());
        cipher.encrypt(buffer, enc, iv);

        if (binary)
            printf("%.*s", (int)enc.size(), enc.data());
        else
            print_buffer(enc, Crypto::AESCipher::block_size());
    } else {
        Crypto::AESCipher::CBCMode cipher(secret_key, key_bits, Crypto::Intent::Decryption);
        auto dec = cipher.create_aligned_buffer(buffer.size());
        cipher.decrypt(buffer, dec, iv);
        printf("%.*s\n", (int)dec.size(), dec.data());
    }
}

void md5(const char* message, size_t len)
{
    auto digest = Crypto::MD5::hash((const u8*)message, len);
    if (binary)
        printf("%.*s", (int)Crypto::MD5::block_size(), digest.data);
    else
        print_buffer(ByteBuffer::wrap(digest.data, Crypto::MD5::block_size()), Crypto::MD5::block_size());
}

auto main(int argc, char** argv) -> int
{
    const char* mode = nullptr;
    Core::ArgsParser parser;
    parser.add_positional_argument(mode, "mode to operate in ('list' to see modes and descriptions)", "mode");

    parser.add_option(secret_key, "Set the secret key (must be key-bits bits)", "secret-key", 'k', "secret key");
    parser.add_option(key_bits, "Size of the key", "key-bits", 'b', "key-bits");
    parser.add_option(filename, "Read from file", "file", 'f', "from file");
    parser.add_option(binary, "Force binary output", "force-binary", 0);
    parser.add_option(interactive, "REPL mode", "interactive", 'i');
    parser.add_option(run_tests, "Run tests for the specified suite", "tests", 't');
    parser.add_option(suite, "Set the suite used", "suite-name", 'n', "suite name");
    parser.parse(argc, argv);

    StringView mode_sv { mode };
    if (mode_sv == "list") {
        puts("Crypt modes");
        puts("\thash - Access hash functions");
        puts("\tencrypt -- Access encryption functions");
        puts("\tdecrypt -- Access decryption functions");
        puts("\tlist -- List all known modes");
        return 0;
    }
    if (mode_sv == "hash") {
        if (suite == nullptr)
            suite = DEFAULT_HASH_SUITE;

        if (StringView(suite) == "MD5") {
            if (run_tests)
                return md5_tests();
            return run(md5);
        } else {
            printf("unknown hash function '%s'\n", suite);
            return 1;
        }
    }
    encrypting = mode_sv == "encrypt";
    if (encrypting || mode_sv == "decrypt") {
        if (suite == nullptr)
            suite = DEFAULT_CIPHER_SUITE;

        if (StringView(suite) == "AES_CBC") {
            if (run_tests)
                return aes_cbc_tests();

            if (!Crypto::AESCipher::KeyType::is_valid_key_size(key_bits)) {
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

#define I_TEST(thing)                     \
    {                                     \
        printf("Testing " #thing "... "); \
    }
#define PASS printf("PASS\n")
#define FAIL(reason) printf("FAIL: " #reason "\n")

ByteBuffer operator""_b(const char* string, size_t length)
{
    dbg() << "Create byte buffer of size " << length;
    return ByteBuffer::copy(string, length);
}

// tests go after here
// please be reasonable with orders kthx
void aes_cbc_test_encrypt();
void aes_cbc_test_decrypt();

void md5_test_hash();
void md5_test_consecutive_updates();

int aes_cbc_tests()
{
    if (encrypting) {
        aes_cbc_test_encrypt();
    } else {
        aes_cbc_test_decrypt();
    }

    return 0;
}

void aes_cbc_test_encrypt()
{
    auto test_it = [](auto& cipher, auto& result) {
        auto in = "This is a test! This is another test!"_b;
        auto out = cipher.create_aligned_buffer(in.size());
        auto iv = ByteBuffer::create_zeroed(Crypto::AESCipher::block_size());
        cipher.encrypt(in, out, iv);
        if (out.size() != sizeof(result))
            FAIL(size mismatch);
        else if (memcmp(out.data(), result, out.size()) != 0) {
            FAIL(invalid data);
            print_buffer(out, Crypto::AESCipher::block_size());
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
        Crypto::AESCipher::CBCMode cipher("WellHelloFriends", 128, Crypto::Intent::Encryption);
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
        Crypto::AESCipher::CBCMode cipher("Well Hello Friends! whf!", 192, Crypto::Intent::Encryption);
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
        Crypto::AESCipher::CBCMode cipher("WellHelloFriendsWellHelloFriends", 256, Crypto::Intent::Encryption);
        test_it(cipher, result);
    }
    // TODO: Test non-CMS padding options
}
void aes_cbc_test_decrypt()
{
    auto test_it = [](auto& cipher, auto& result, auto result_len) {
        auto true_value = "This is a test! This is another test!";
        auto in = ByteBuffer::copy(result, result_len);
        auto out = cipher.create_aligned_buffer(in.size());
        auto iv = ByteBuffer::create_zeroed(Crypto::AESCipher::block_size());
        cipher.decrypt(in, out, iv);
        if (out.size() != strlen(true_value)) {
            FAIL(size mismatch);
            printf("Expected %zu bytes but got %zu\n", strlen(true_value), out.size());
        } else if (memcmp(out.data(), true_value, strlen(true_value)) != 0) {
            FAIL(invalid data);
            print_buffer(out, Crypto::AESCipher::block_size());
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
        Crypto::AESCipher::CBCMode cipher("WellHelloFriends", 128, Crypto::Intent::Decryption);
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
        Crypto::AESCipher::CBCMode cipher("Well Hello Friends! whf!", 192, Crypto::Intent::Decryption);
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
        Crypto::AESCipher::CBCMode cipher("WellHelloFriendsWellHelloFriends", 256, Crypto::Intent::Decryption);
        test_it(cipher, result, 48);
    }
    // TODO: Test non-CMS padding options
}

int md5_tests()
{
    md5_test_hash();
    md5_test_consecutive_updates();
    return 0;
}

void md5_test_hash()
{
    {
        I_TEST((MD5 Hashing | "Well hello friends"));
        u8 result[] {
            0xaf, 0x04, 0x3a, 0x08, 0x94, 0x38, 0x6e, 0x7f, 0xbf, 0x73, 0xe4, 0xaa, 0xf0, 0x8e, 0xee, 0x4c
        };
        auto digest = Crypto::MD5::hash("Well hello friends");

        if (memcmp(result, digest.data, Crypto::MD5::block_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer(ByteBuffer::wrap(digest.data, Crypto::MD5::block_size()), -1);
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
        auto digest = Crypto::MD5::hash("");

        if (memcmp(result, digest.data, Crypto::MD5::block_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer(ByteBuffer::wrap(digest.data, Crypto::MD5::block_size()), -1);
        } else {
            PASS;
        }
    }
    {
        I_TEST((MD5 Hashing | "a"));
        u8 result[] {
            0x0c, 0xc1, 0x75, 0xb9, 0xc0, 0xf1, 0xb6, 0xa8, 0x31, 0xc3, 0x99, 0xe2, 0x69, 0x77, 0x26, 0x61
        };
        auto digest = Crypto::MD5::hash("a");

        if (memcmp(result, digest.data, Crypto::MD5::block_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer(ByteBuffer::wrap(digest.data, Crypto::MD5::block_size()), -1);
        } else {
            PASS;
        }
    }
    {
        I_TEST((MD5 Hashing | "abcdefghijklmnopqrstuvwxyz"));
        u8 result[] {
            0xc3, 0xfc, 0xd3, 0xd7, 0x61, 0x92, 0xe4, 0x00, 0x7d, 0xfb, 0x49, 0x6c, 0xca, 0x67, 0xe1, 0x3b
        };
        auto digest = Crypto::MD5::hash("abcdefghijklmnopqrstuvwxyz");

        if (memcmp(result, digest.data, Crypto::MD5::block_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer(ByteBuffer::wrap(digest.data, Crypto::MD5::block_size()), -1);
        } else {
            PASS;
        }
    }
    {
        I_TEST((MD5 Hashing | Long Sequence));
        u8 result[] {
            0x57, 0xed, 0xf4, 0xa2, 0x2b, 0xe3, 0xc9, 0x55, 0xac, 0x49, 0xda, 0x2e, 0x21, 0x07, 0xb6, 0x7a
        };
        auto digest = Crypto::MD5::hash("12345678901234567890123456789012345678901234567890123456789012345678901234567890");

        if (memcmp(result, digest.data, Crypto::MD5::block_size()) != 0) {
            FAIL(Invalid hash);
            print_buffer(ByteBuffer::wrap(digest.data, Crypto::MD5::block_size()), -1);
        } else {
            PASS;
        }
    }
}

void md5_test_consecutive_updates()
{
    {
        I_TEST((MD5 Hashing | Multiple Updates));
        u8 result[] {
            0xaf, 0x04, 0x3a, 0x08, 0x94, 0x38, 0x6e, 0x7f, 0xbf, 0x73, 0xe4, 0xaa, 0xf0, 0x8e, 0xee, 0x4c
        };
        Crypto::MD5 md5;

        md5.update("Well");
        md5.update(" hello ");
        md5.update("friends");
        auto digest = md5.digest();

        if (memcmp(result, digest.data, Crypto::MD5::block_size()) != 0)
            FAIL(Invalid hash);
        else
            PASS;
    }
    {
        I_TEST((MD5 Hashing | Reuse));
        Crypto::MD5 md5;

        md5.update("Well");
        md5.update(" hello ");
        md5.update("friends");
        auto digest0 = md5.digest();

        md5.update("Well");
        md5.update(" hello ");
        md5.update("friends");
        auto digest1 = md5.digest();

        if (memcmp(digest0.data, digest1.data, Crypto::MD5::block_size()) != 0)
            FAIL(Cannot reuse);
        else
            PASS;
    }
}
