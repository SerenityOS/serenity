/*
 * Copyright (c) 2024, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/NumberFormat.h>
#include <AK/Random.h>
#include <AK/Tuple.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCrypto/AEAD/ChaCha20Poly1305.h>
#include <LibCrypto/Authentication/GHash.h>
#include <LibCrypto/Authentication/HMAC.h>
#include <LibCrypto/Authentication/Poly1305.h>
#include <LibCrypto/Checksum/Adler32.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibCrypto/Cipher/ChaCha20.h>
#include <LibCrypto/Forward.h>
#include <LibCrypto/Hash/BLAKE2b.h>
#include <LibCrypto/Hash/MD5.h>
#include <LibCrypto/Hash/MGF.h>
#include <LibCrypto/Hash/PBKDF2.h>
#include <LibCrypto/Hash/SHA1.h>
#include <LibCrypto/Hash/SHA2.h>
#include <LibMain/Main.h>

#define ALL_ALGORITHMS(E)                                            \
    E(md5, hash, Hash::MD5)                                          \
    E(sha1, hash, Hash::SHA1)                                        \
    E(sha256, hash, Hash::SHA256)                                    \
    E(sha512, hash, Hash::SHA512)                                    \
    E(blake2b, hash, Hash::BLAKE2b)                                  \
    E(adler32, checksum, Checksum::Adler32)                          \
    E(crc32, checksum, Checksum::CRC32)                              \
    E(hmac_md5, auth, Authentication::HMAC<Crypto::Hash::MD5>)       \
    E(hmac_sha1, auth, Authentication::HMAC<Crypto::Hash::SHA1>)     \
    E(hmac_sha256, auth, Authentication::HMAC<Crypto::Hash::SHA256>) \
    E(hmac_sha512, auth, Authentication::HMAC<Crypto::Hash::SHA512>) \
    E(poly1305, auth, Authentication::Poly1305)                      \
    E(ghash, auth, Authentication::GHash)                            \
    E(aes_128_cbc, cipher, Cipher::AESCipher::CBCMode, 128)          \
    E(aes_128_ctr, cipher, Cipher::AESCipher::CTRMode, 128)          \
    E(aes_128_gcm, cipher, Cipher::AESCipher::GCMMode, 128)          \
    E(aes_256_cbc, cipher, Cipher::AESCipher::CBCMode, 256)          \
    E(aes_256_ctr, cipher, Cipher::AESCipher::CTRMode, 256)          \
    E(chacha20_128, cipher, Cipher::ChaCha20, 128, 96)               \
    E(chacha20_256, cipher, Cipher::ChaCha20, 256, 96)

struct Timings {
    u64 total_us { 0 };
    u64 min_us { NumericLimits<u64>::max() };
    u64 max_us { 0 };
    size_t count { 0 };
    size_t unit_bytes { 0 };
};
static HashMap<StringView, HashMap<size_t, Timings>> g_all_timings;
static auto g_time_slice_per_size = Duration::from_seconds(3);

constexpr size_t sizes_in_bytes[] = { 16, 1 * KiB, 16 * KiB, 256 * KiB, 1 * MiB, 16 * MiB };

static void run_benchmark_with_all_sizes(StringView name, Function<void(ByteBuffer&)> func)
{
    for (auto size : sizes_in_bytes) {
        auto result = ByteBuffer::create_uninitialized(size);
        if (result.is_error()) {
            warnln("Failed to allocate buffer of size {}", size);
            continue;
        }
        auto buffer = result.release_value();
        fill_with_random(buffer);

        Timings timing_result { .unit_bytes = size };
        warn("Running benchmark for {} with size {} for ~{}ms...", name, size, g_time_slice_per_size.to_milliseconds());
        auto total_timer = Core::ElapsedTimer::start_new(Core::TimerType::Precise);
        for (; total_timer.elapsed_time() < g_time_slice_per_size;) {
            auto timer = Core::ElapsedTimer::start_new(Core::TimerType::Precise);
            func(buffer);
            auto elapsed = timer.elapsed_time();
            timing_result.max_us = max(timing_result.max_us, elapsed.to_microseconds());
            timing_result.min_us = min(timing_result.min_us, elapsed.to_microseconds());
            timing_result.total_us += elapsed.to_microseconds();
            timing_result.count++;
        }
        g_all_timings.ensure(name).set(size, timing_result);
        warnln("{}ms, {} ops, {}/s", total_timer.elapsed_milliseconds(), timing_result.count, human_readable_quantity(timing_result.unit_bytes * timing_result.count / timing_result.total_us * 1'000'000));
    }
}

template<typename Algorithm>
static ErrorOr<void> run_hash_benchmark(StringView name)
{
    run_benchmark_with_all_sizes(name, [](auto& buffer) {
        auto digest = Algorithm::hash(buffer);
        AK::taint_for_optimizer(digest);
    });
    return {};
}

template<typename Algorithm>
static ErrorOr<void> run_checksum_benchmark(StringView name)
{
    run_benchmark_with_all_sizes(name, [](auto& buffer) {
        Algorithm checksum;
        checksum.update(buffer);
        auto digest = checksum.digest();
        AK::taint_for_optimizer(digest);
    });
    return {};
}

template<typename Algorithm>
static ErrorOr<void> run_auth_benchmark(StringView name)
{
    auto key = TRY(ByteBuffer::create_uninitialized(128));
    fill_with_random(key);
    run_benchmark_with_all_sizes(name, [&](auto& buffer) {
        Algorithm auth(key.bytes());
        if constexpr (requires { auth.process(buffer.bytes()); }) {
            auto tag = auth.process(buffer.bytes());
            AK::taint_for_optimizer(tag);
        } else if constexpr (IsSame<Algorithm, Crypto::Authentication::GHash>) {
            auto tag = auth.process(buffer.bytes(), buffer.bytes());
            AK::taint_for_optimizer(tag);
        } else {
            auth.update(buffer.bytes());
            auto digest = auth.digest();
            AK::taint_for_optimizer(digest);
        }
    });
    return {};
}

template<typename Algorithm, typename... Options>
static ErrorOr<void> run_cipher_benchmark(StringView name, size_t key_bits, Options... options)
{
    auto key = TRY(ByteBuffer::create_uninitialized(key_bits / 8));
    fill_with_random(key);

    auto out_buffer = TRY(ByteBuffer::create_uninitialized(16 * MiB));

    auto iv = TRY(ByteBuffer::create_uninitialized(key_bits / 8));
    fill_with_random(iv);

    auto remaining_options = Tuple { options... };

    ByteBuffer nonce;
    constexpr auto needs_nonce = remaining_options.size() > 0;
    if constexpr (needs_nonce) {
        nonce = TRY(ByteBuffer::create_uninitialized(remaining_options.template get<0>()));
        fill_with_random(nonce);
    }

    run_benchmark_with_all_sizes(name, [&](auto& buffer) {
        Optional<Algorithm> cipher;
        if constexpr (needs_nonce)
            cipher = Algorithm(key.bytes(), nonce);
        else
            cipher = Algorithm(key.bytes(), key_bits, Crypto::Cipher::Intent::Encryption);
        auto out_bytes = out_buffer.bytes();
        if constexpr (requires { cipher->encrypt(buffer, out_bytes, iv.bytes()); })
            cipher->encrypt(buffer, out_bytes, iv.bytes());
        else
            cipher->encrypt(buffer, out_bytes);
        AK::taint_for_optimizer(out_buffer);
    });
    return {};
}

static ErrorOr<void> benchmark(StringView algorithm)
{
#define BENCH(name, type, algo, ...)                                                       \
    if (algorithm == #name) {                                                              \
        outln("Benchmarking {}...", #name);                                                \
        return run_##type##_benchmark<Crypto::algo>(#name##sv __VA_OPT__(, ) __VA_ARGS__); \
    }

    ALL_ALGORITHMS(BENCH);

#undef BENCH

    return Error::from_string_literal("Unknown algorithm");
}

static void print_benchmark_results()
{
    // algo, size, min, max, avg, throughput
    outln("{:<20} {:<10} {:<10} {:<10} {:<10} {:<10}", "Algorithm", "Size", "Min us/op", "Max us/op", "Avg us/op", "Throughput");
    for (auto& [algo, timings] : g_all_timings) {
        for (auto size : sizes_in_bytes) {
            auto t = timings.get(size);
            if (!t.has_value())
                continue;
            auto& timing = t.value();
            outln("{:<20} {:<10} {:<10} {:<10} {:<10} {:<10}/s",
                algo,
                human_readable_size(timing.unit_bytes),
                timing.min_us,
                timing.max_us,
                timing.total_us / timing.count,
                human_readable_quantity(timing.unit_bytes * timing.count / timing.total_us * 1'000'000));
        }
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<ByteString> algorithms;
    Optional<StringView> time_slice;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(algorithms, "Algorithms (or categories) to benchmark", "algorithms", Core::ArgsParser::Required::Yes);
    args_parser.add_option(time_slice, "Time slice for each benchmark size in milliseconds", "time-slice", 't', "time-slice");
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::None,
        .help_string = "List all available algorithms",
        .long_name = "list",
        .short_name = 'l',
        .accept_value = [](auto) -> bool {
            warnln("{:<20} {:<10}", "Algorithm", "Type");
#define BENCH(name, type, ...) \
    warnln("{:<20} {:<10}", #name, #type);

            ALL_ALGORITHMS(BENCH);
#undef BENCH
            exit(0);
        },
    });
    args_parser.set_general_help("Benchmark LibCrypto's implementations of various cryptographic algorithms");

    args_parser.parse(arguments);

    if (time_slice.has_value()) {
        if (auto slice = time_slice->to_number<u32>(); slice.has_value())
            g_time_slice_per_size = Duration::from_milliseconds(*slice);
        else
            return Error::from_string_literal("Invalid time slice value");
    }

    for (auto const& algorithm : algorithms) {
        auto found = false;
#define BENCH(name, type, ...)     \
    if (algorithm == #type) {      \
        TRY(benchmark(#name##sv)); \
        found = true;              \
    }

        ALL_ALGORITHMS(BENCH);
#undef BENCH

        if (!found)
            TRY(benchmark(algorithm));
    }

    print_benchmark_results();

    return 0;
}
