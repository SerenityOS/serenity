#include <AK/StringView.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    auto js = AK::StringView(static_cast<const unsigned char*>(data), size);
    auto lexer = JS::Lexer(js);
    auto parser = JS::Parser(lexer);
    parser.parse_program();
    return 0;
}
