/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

#define ENUMERATE_TARGETS(T) \
    T(BMPLoader)             \
    T(ELF)                   \
    T(Gemini)                \
    T(GIFLoader)             \
    T(HttpRequest)           \
    T(ICOLoader)             \
    T(JPGLoader)             \
    T(Js)                    \
    T(Markdown)              \
    T(PBMLoader)             \
    T(PGMLoader)             \
    T(PNGLoader)             \
    T(PPMLoader)             \
    T(QOILoader)             \
    T(RegexECMA262)          \
    T(RegexPosixExtended)    \
    T(Shell)                 \
    T(TTF)                   \
    T(URL)

#undef __ENUMERATE_TARGET
#define __ENUMERATE_TARGET(x) extern "C" int Test##x(const uint8_t*, size_t);
ENUMERATE_TARGETS(__ENUMERATE_TARGET)
#undef __ENUMERATE_TARGET

#define LLVMFuzzerTestOneInput TestBMPLoader
#include <Meta/Lagom/Fuzzers/FuzzBMPLoader.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestELF
#include <Meta/Lagom/Fuzzers/FuzzELF.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestGemini
#include <Meta/Lagom/Fuzzers/FuzzGemini.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestGIFLoader
#include <Meta/Lagom/Fuzzers/FuzzGIFLoader.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestHttpRequest
#include <Meta/Lagom/Fuzzers/FuzzHttpRequest.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestICOLoader
#include <Meta/Lagom/Fuzzers/FuzzICOLoader.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestJPGLoader
#include <Meta/Lagom/Fuzzers/FuzzJPGLoader.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestJs
#include <Meta/Lagom/Fuzzers/FuzzJs.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestMarkdown
#include <Meta/Lagom/Fuzzers/FuzzMarkdown.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestPBMLoader
#include <Meta/Lagom/Fuzzers/FuzzPBMLoader.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestPGMLoader
#include <Meta/Lagom/Fuzzers/FuzzPGMLoader.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestPNGLoader
#include <Meta/Lagom/Fuzzers/FuzzPNGLoader.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestPPMLoader
#include <Meta/Lagom/Fuzzers/FuzzPPMLoader.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestQOILoader
#include <Meta/Lagom/Fuzzers/FuzzQOILoader.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestRegexECMA262
#include <Meta/Lagom/Fuzzers/FuzzRegexECMA262.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestRegexPosixExtended
#include <Meta/Lagom/Fuzzers/FuzzRegexPosixExtended.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestShell
#include <Meta/Lagom/Fuzzers/FuzzShell.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestTTF
#include <Meta/Lagom/Fuzzers/FuzzTTF.cpp>
#undef LLVMFuzzerTestOneInput

#define LLVMFuzzerTestOneInput TestURL
#include <Meta/Lagom/Fuzzers/FuzzURL.cpp>
#undef LLVMFuzzerTestOneInput

static auto parse_target_name(const String& name)
{
    if (name == "list") {
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

int main(int argc, char** argv)
{
    const char* type;
    const char* filename;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(type, "Type of fuzzing target to run (use \"list\" to list all existing)", "target-kind");
    args_parser.add_positional_argument(filename, "Input file", "filename");
    args_parser.parse(argc, argv);

    auto fn = parse_target_name(type);

    auto file = Core::File::open(filename, Core::OpenMode::ReadOnly);
    if (file.is_error()) {
        warnln("Cannot read from file: {}", file.error());
        exit(1);
    }

    auto input = file.value()->read_all();

    return fn(input.data(), input.size());
}
