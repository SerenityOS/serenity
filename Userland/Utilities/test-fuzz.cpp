/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 * Copyright (c) 2023, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>
#include <unistd.h>

// TODO: Look into generating this from the authoritative list of fuzzing targets in fuzzer.cmake.
#define ENUMERATE_TARGETS(T) \
    T(ASN1)                  \
    T(Base64Roundtrip)       \
    T(BLAKE2b)               \
    T(BMPLoader)             \
    T(Brotli)                \
    T(CSSParser)             \
    T(DDSLoader)             \
    T(DNSPacket)             \
    T(DeflateCompression)    \
    T(DeflateDecompression)  \
    T(ELF)                   \
    T(FlacLoader)            \
    T(Gemini)                \
    T(GIFLoader)             \
    T(GzipDecompression)     \
    T(GzipRoundtrip)         \
    T(HIDReportDescriptor)   \
    T(HttpRequest)           \
    T(ICCProfile)            \
    T(ICOLoader)             \
    T(ILBMLoader)            \
    T(IMAPParser)            \
    T(JBIG2Loader)           \
    T(JPEG2000Loader)        \
    T(JPEGLoader)            \
    T(Js)                    \
    T(JsonParser)            \
    T(LzmaDecompression)     \
    T(LzmaRoundtrip)         \
    T(Markdown)              \
    T(MatroskaReader)        \
    T(MD5)                   \
    T(MP3Loader)             \
    T(PAMLoader)             \
    T(PBMLoader)             \
    T(PDF)                   \
    T(PEM)                   \
    T(PGMLoader)             \
    T(PNGLoader)             \
    T(Poly1305)              \
    T(PPMLoader)             \
    T(QOALoader)             \
    T(QOILoader)             \
    T(QuotedPrintableParser) \
    T(RegexECMA262)          \
    T(RegexPosixBasic)       \
    T(RegexPosixExtended)    \
    T(RSAKeyParsing)         \
    T(SHA1)                  \
    T(SHA256)                \
    T(SHA384)                \
    T(SHA512)                \
    T(Shell)                 \
    T(ShellPosix)            \
    T(SQLParser)             \
    T(Tar)                   \
    T(TextDecoder)           \
    T(TGALoader)             \
    T(TIFFLoader)            \
    T(TTF)                   \
    T(TinyVGLoader)          \
    T(URL)                   \
    T(VP9Decoder)            \
    T(WasmParser)            \
    T(WAVLoader)             \
    T(WebPLoader)            \
    T(WOFF)                  \
    T(WOFF2)                 \
    T(XML)                   \
    T(Zip)                   \
    T(ZlibDecompression)

#undef __ENUMERATE_TARGET
#define __ENUMERATE_TARGET(x) extern "C" int Test##x(uint8_t const*, size_t);
ENUMERATE_TARGETS(__ENUMERATE_TARGET)
#undef __ENUMERATE_TARGET

static auto parse_target_name(StringView name)
{
    if (name == "list"sv) {
        outln("The following targets are included:");
#undef __ENUMERATE_TARGET
#define __ENUMERATE_TARGET(x) outln(#x);
        ENUMERATE_TARGETS(__ENUMERATE_TARGET)
#undef __ENUMERATE_TARGET
        exit(0);
    }

#undef __ENUMERATE_TARGET
#define __ENUMERATE_TARGET(x) \
    if (name == #x)           \
        return Test##x;
    ENUMERATE_TARGETS(__ENUMERATE_TARGET)
#undef __ENUMERATE_TARGET

    warnln("Unknown fuzzing target \"{}\". Try \"list\" to get a full list.", name);
    exit(1);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView type;
    StringView filename;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(type, "Type of fuzzing target to run (use \"list\" to list all existing)", "target-kind");
    args_parser.add_positional_argument(filename, "Input file", "filename", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (arguments.strings.size() <= 2 && arguments.strings[1] != "list"sv) {
        args_parser.print_usage_terminal(stderr, arguments.strings[0]);
        return 0;
    }

    auto fn = parse_target_name(type);

    auto file = TRY(Core::File::open(filename, Core::File::OpenMode::Read));
    auto input = TRY(file->read_until_eof());

    return fn(input.data(), input.size());
}
