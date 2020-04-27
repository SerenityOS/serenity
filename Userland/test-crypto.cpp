#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibLine/Editor.h>
#include <stdio.h>

static const char* secret_key = "WellHelloFreinds";
static const char* cipher = "AES_CBC";
static const char* filename = nullptr;
static int key_bits = 128;
static bool encrypting = false;
static bool binary = false;
static bool interactive = false;
static bool run_tests = false;

// listAllTests
int aes_cbc_tests();

// stop listing tests

void print_buffer(const ByteBuffer& buffer, size_t split)
{
    for (size_t i = 0; i < buffer.size(); ++i) {
        if (i % split == 0 && i)
            puts("");
        printf("%02x", buffer[i]);
    }
    puts("");
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

auto main(int argc, char** argv) -> int
{
    Core::ArgsParser parser;
    parser.add_option(secret_key, "Set the secret key (must be key-bits bits)", "secret-key", 'k', "secret key");
    parser.add_option(key_bits, "Size of the key", "key-bits", 'b', "key-bits");
    parser.add_option(filename, "Read from file", "file", 'f', "from file");
    parser.add_option(encrypting, "Encrypt the message", "encrypt", 'e');
    parser.add_option(binary, "Force binary output", "force-binary", 0);
    parser.add_option(interactive, "Force binary output", "interactive", 'i');
    parser.add_option(run_tests, "Run tests for the specified suite", "tests", 't');
    parser.add_option(cipher, "Set the Cipher used", "cipher", 'c', "cipher name");
    parser.parse(argc, argv);

    if (StringView(cipher) == "AES_CBC") {
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
        if (interactive) {
            Line::Editor editor;
            editor.initialize();
            for (;;) {
                auto line = editor.get_line("> ");
                aes_cbc(line.characters(), line.length());
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
            aes_cbc((const char*)buffer.data(), buffer.size());
        }
    } else {
        printf("Unknown cipher suite '%s'", cipher);
        return 1;
    }

    return 0;
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

int aes_cbc_tests()
{
    aes_cbc_test_encrypt();
    aes_cbc_test_decrypt();
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
