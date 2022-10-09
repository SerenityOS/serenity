/*
 * Copyright (c) 2022, Michiel Visser <opensource@webmichiel.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibCompress/Brotli.h>
#include <LibCore/Stream.h>

static void run_test(StringView const file_name)
{
    // This makes sure that the tests will run both on target and in Lagom.
#ifdef AK_OS_SERENITY
    String path = String::formatted("/usr/Tests/LibCompress/brotli-test-files/{}", file_name);
#else
    String path = String::formatted("brotli-test-files/{}", file_name);
#endif

    auto cmp_file = MUST(Core::Stream::File::open(path, Core::Stream::OpenMode::Read));
    auto cmp_data = MUST(cmp_file->read_all());

    String path_compressed = String::formatted("{}.br", path);

    auto file = MUST(Core::Stream::File::open(path_compressed, Core::Stream::OpenMode::Read));
    auto brotli_stream = Compress::BrotliDecompressionStream { *file };
    auto data = MUST(brotli_stream.read_all());

    EXPECT_EQ(data, cmp_data);
}

TEST_CASE(brotli_decompress_uncompressed)
{
    run_test("wellhello.txt"sv);
}

TEST_CASE(brotli_decompress_simple)
{
    run_test("hello.txt"sv);
}

TEST_CASE(brotli_decompress_simple2)
{
    run_test("wellhello2.txt"sv);
}

TEST_CASE(brotli_decompress_lorem)
{
    run_test("lorem.txt"sv);
}

TEST_CASE(brotli_decompress_lorem2)
{
    run_test("lorem2.txt"sv);
}

TEST_CASE(brotli_decompress_transform)
{
    run_test("transform.txt"sv);
}

TEST_CASE(brotli_decompress_serenityos_html)
{
    run_test("serenityos.html"sv);
}

TEST_CASE(brotli_decompress_happy3rd_html)
{
    run_test("happy3rd.html"sv);
}

TEST_CASE(brotli_decompress_katica_regular_10_font)
{
    run_test("KaticaRegular10.font"sv);
}

TEST_CASE(brotli_single_z)
{
    run_test("single-z.txt"sv);
}

TEST_CASE(brotli_decompress_zero_one_bin)
{
    // This makes sure that the tests will run both on target and in Lagom.
#ifdef AK_OS_SERENITY
    String path = "/usr/Tests/LibCompress/brotli-test-files/zero-one.bin";
#else
    String path = "brotli-test-files/zero-one.bin";
#endif

    String path_compressed = String::formatted("{}.br", path);

    auto file = MUST(Core::Stream::File::open(path_compressed, Core::Stream::OpenMode::Read));
    auto brotli_stream = Compress::BrotliDecompressionStream { *file };

    u8 buffer_raw[4096];
    Bytes buffer { buffer_raw, 4096 };

    size_t bytes_read = 0;
    while (true) {
        size_t nread = MUST(brotli_stream.read(buffer)).size();
        if (nread == 0)
            break;

        for (size_t i = 0; i < nread; i++) {
            if (bytes_read < 16 * MiB)
                EXPECT(buffer[i] == 0);
            else
                EXPECT(buffer[i] == 1);
        }

        bytes_read += nread;
    }
    EXPECT(bytes_read == 32 * MiB);
    EXPECT(brotli_stream.is_eof());
}
