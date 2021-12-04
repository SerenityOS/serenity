/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <LibWasm/Parser/TextFormat.h>
#include <ctype.h>

namespace Wasm {

TextFormatParseError::TextFormatParseError(GenericLexer const& lexer, String error, SourceLocation location)
    : Error(Error::from_string_literal("Text format parse error"))
    , line(0)
    , column(0)
    , error(move(error))
    , location(location)
{
    for (auto const& ch : lexer.input().substring_view(0, lexer.tell())) {
        if (ch == '\n') {
            ++line;
            column = 0;
            continue;
        }
        ++column;
    }
}

template<typename T = u64>
static ErrorOr<T, TextFormatParseError> expect_unsigned_number(GenericLexer& lexer)
{
    auto maybe_value = lexer.consume_while(isdigit).to_uint<T>();
    if (!maybe_value.has_value())
        return TextFormatParseError(lexer, "Expected a number"sv);
    return *maybe_value;
}

static ErrorOr<void, TextFormatParseError> expect_form_with_name(GenericLexer& lexer, StringView name, StringView error_if_name_mismatch)
{
    lexer.ignore_while(isspace);
    if (!lexer.consume_specific('('))
        return TextFormatParseError(lexer, "Expected '('");
    lexer.ignore_while(isspace);
    auto form_name = lexer.consume_while(isalpha);

    if (form_name != name)
        return TextFormatParseError(lexer, error_if_name_mismatch);

    return {};
}

static ErrorOr<void, TextFormatParseError> expect_literal_word(GenericLexer& lexer, StringView name, StringView error_if_name_mismatch)
{
    lexer.ignore_while(isspace);
    auto form_name = lexer.consume_while(isalpha);

    if (form_name != name)
        return TextFormatParseError(lexer, error_if_name_mismatch);

    return {};
}

static ErrorOr<void, TextFormatParseError> transfer_memory_stream(DuplexMemoryStream& input, OutputStream& output)
{
    u8 buffer[DuplexMemoryStream::chunk_size];
    while (!input.eof()) {
        auto size = input.read({ buffer, array_size(buffer) });
        output.write_or_error({ buffer, size });
        if (output.handle_any_error())
            return TextFormatParseError(0, 0, "Stream Error");
    }
    return {};
}

template<auto fn>
static ErrorOr<void, TextFormatParseError> emit_with_size(GenericLexer& lexer, OutputStream& output_stream)
{
    DuplexMemoryStream temp_stream;
    TRY(fn(lexer, temp_stream));
    if (LEB128::write_unsigned(temp_stream.size(), output_stream).is_error())
        return TextFormatParseError(0, 0, "Stream Error");
    return transfer_memory_stream(temp_stream, output_stream);
}

template<auto fn, bool drop_spaces, typename Predicate>
static ErrorOr<void, TextFormatParseError> emit_vector(GenericLexer& lexer, OutputStream& output_stream, Predicate predicate)
{
    DuplexMemoryStream temp_stream;
    size_t vector_count = 0;
    if constexpr (drop_spaces)
        lexer.ignore_while(isspace);
    while (predicate()) {
        ++vector_count;
        TRY(fn(lexer, temp_stream));
        if constexpr (drop_spaces)
            lexer.ignore_while(isspace);
    }

    if (LEB128::write_unsigned(vector_count, output_stream).is_error())
        return TextFormatParseError(0, 0, "Stream Error");
    return transfer_memory_stream(temp_stream, output_stream);
}

template<auto fn, typename Predicate>
auto emit_vector_skipping_spaces(GenericLexer& lexer, OutputStream& output_stream, Predicate&& predicate)
{
    return emit_vector<fn, true, Predicate>(lexer, output_stream, forward<Predicate>(predicate));
}

template<typename SectionType, auto parse_fn>
static ErrorOr<void, TextFormatParseError> parse_and_generate_section_contents(GenericLexer& lexer, OutputStream& output_stream)
{
    lexer.ignore_while(isspace);
    output_stream.write_or_error({ &SectionType::section_id, sizeof(u8) });
    if (output_stream.handle_any_error())
        return TextFormatParseError(0, 0, "Stream Error");
    return emit_with_size<parse_fn>(lexer, output_stream);
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_type(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "type"sv, "Expected 'type'"sv));
    lexer.ignore_while(isspace);
    auto type_name = lexer.consume_while([](auto c) { return c != ')'; });
    if (type_name == "i32"sv)
        output_stream << (u8)Wasm::Constants::i32_tag;
    else if (type_name == "i64"sv)
        output_stream << (u8)Wasm::Constants::i64_tag;
    else if (type_name == "f32"sv)
        output_stream << (u8)Wasm::Constants::f32_tag;
    else if (type_name == "f64"sv)
        output_stream << (u8)Wasm::Constants::f64_tag;
    else if (type_name.is_one_of("funcref"sv, "ref.null funcref"sv))
        output_stream << (u8)Wasm::Constants::function_reference_tag;
    else if (type_name.is_one_of("externref"sv, "ref.null externref"sv))
        output_stream << (u8)Wasm::Constants::extern_reference_tag;
    else
        return TextFormatParseError(lexer, "Expected a valid type name (i32,i64,f32,f64,funcref,externref)");

    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    if (output_stream.handle_any_error())
        return TextFormatParseError(0, 0, "Stream Error");
    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_locals(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "local"sv, "Expected 'local'"sv));
    lexer.ignore_while(isspace);
    if (!lexer.consume_specific('x'))
        return TextFormatParseError(lexer, "Expected locals count as 'x<n>'"sv);

    auto maybe_count = lexer.consume_while(isdigit).to_uint();
    if (!maybe_count.has_value())
        return TextFormatParseError(lexer, "Expected locals count as 'x<n>'"sv);

    if (LEB128::write_unsigned(*maybe_count, output_stream).is_error())
        return TextFormatParseError(0, 0, "Stream Error");

    // Optionally accept this.
    lexer.consume_specific(" of type"sv);
    TRY(parse_and_generate_type(lexer, output_stream));

    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_block_type(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "type"sv, "Expected 'type'"sv));
    TRY(expect_literal_word(lexer, "block"sv, "Expected 'block'"sv));
    if (expect_form_with_name(lexer, "empty"sv, ""sv).is_error()) {
        lexer.ignore_while(isspace);
        auto form_name = lexer.consume_while(isalpha);

        if (form_name == "index"sv) {
            lexer.ignore_while(isspace);
            auto index = TRY(expect_unsigned_number(lexer));
            if (LEB128::write_unsigned(index, output_stream).is_error())
                return TextFormatParseError(lexer, "Stream Error"sv);
            lexer.ignore_while(isspace);
        } else if (form_name == "type"sv) {
            TRY(parse_and_generate_type(lexer, output_stream));
        } else {
            return TextFormatParseError(lexer, "Invalid block type"sv);
        }
    } else {
        output_stream << (u8)Wasm::Constants::empty_block_tag;

        if (!lexer.consume_specific(')'))
            return TextFormatParseError(lexer, "Expected ')'");
    }

    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    if (output_stream.handle_any_error())
        return TextFormatParseError(lexer, "Stream Error"sv);

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_argument(GenericLexer& lexer, OutputStream& output_stream, Wasm::OpCode instruction_opcode)
{
    lexer.ignore_while(isspace);
    if (lexer.consume_specific(')'))
        return {};

    if (!lexer.consume_specific('(')) {
        // Expect a number here.
        String string_value = lexer.consume_while([](auto c) { return c == '.' || isdigit(c) || isxdigit(c) || c == 'x' || c == '-'; });
        if (string_value.is_empty())
            return TextFormatParseError(lexer, "Expected an integer");

        if (string_value.contains('.')) {
            char* endptr = nullptr;
            auto double_value = strtod(string_value.characters(), &endptr);
            if (endptr != string_value.characters() + string_value.length())
                return TextFormatParseError(lexer, "Invalid floating point literal");

            if (instruction_opcode == Wasm::Instructions::f32_const) {
                LittleEndian<u32> value = bit_cast<u32>(static_cast<float>(double_value));
                output_stream << value;
            } else if (instruction_opcode == Wasm::Instructions::f64_const) {
                LittleEndian<u64> value = bit_cast<u64>(double_value);
                output_stream << value;
            } else {
                return TextFormatParseError(lexer, "Invalid floating point argument for instruction");
            }

            if (output_stream.has_any_error())
                return TextFormatParseError(lexer, "Stream Error");
        } else {
            char* endptr = nullptr;
            auto value = strtoll(string_value.characters(), &endptr, 0);
            if (endptr != string_value.characters() + string_value.length())
                return TextFormatParseError(lexer, "Invalid integer literal");

            if (LEB128::write_signed(value, output_stream).is_error())
                return TextFormatParseError(lexer, "Stream Error");
        }

        lexer.ignore_while(isspace);
        if (!lexer.next_is(')'))
            return TextFormatParseError(lexer, "Expected ')'");
        return {};
    }

    auto form_name = lexer.consume_while(isalpha);
    if (form_name == "type"sv) {
        lexer.ignore_while(isspace);
        if (lexer.next_is("block")) {
            TODO();
        } else {
            TRY(parse_and_generate_type(lexer, output_stream));
        }
    } else if (form_name.is_one_of("data"sv, "element"sv, "function"sv, "global"sv, "label"sv, "local"sv, "table"sv)) {
        lexer.ignore_while(isspace);
        if (lexer.consume_while(isalpha) != "index"sv)
            return TextFormatParseError(lexer, "Expected 'index'");
        lexer.ignore_while(isspace);
        auto index = TRY(expect_unsigned_number(lexer));
        if (LEB128::write_unsigned(index, output_stream).is_error())
            return TextFormatParseError(lexer, "Stream Error"sv);
        lexer.ignore_while(isspace);
        if (!lexer.consume_specific(')'))
            return TextFormatParseError(lexer, "Expected ')'");
    } else if (form_name == "indirect"sv) {
        TODO();
    } else if (form_name == "memory"sv) {
        TRY(expect_form_with_name(lexer, "align"sv, "Expected 'align'"));
        lexer.ignore_while(isspace);
        auto align = TRY(expect_unsigned_number(lexer));
        if (!lexer.consume_specific(')'))
            return TextFormatParseError(lexer, "Expected ')'");

        if (LEB128::write_unsigned(align, output_stream).is_error())
            return TextFormatParseError(lexer, "Stream Error"sv);

        TRY(expect_form_with_name(lexer, "offset"sv, "Expected 'offset'"));
        lexer.ignore_while(isspace);
        auto offset = TRY(expect_unsigned_number(lexer));
        if (!lexer.consume_specific(')'))
            return TextFormatParseError(lexer, "Expected ')'");

        if (LEB128::write_unsigned(offset, output_stream).is_error())
            return TextFormatParseError(lexer, "Stream Error"sv);

        lexer.ignore_while(isspace);
        if (!lexer.consume_specific(')'))
            return TextFormatParseError(lexer, "Expected ')'");
    } else if (form_name == "structured"sv) {
        TRY(parse_and_generate_block_type(lexer, output_stream));

        if (!expect_form_with_name(lexer, "else"sv, "Expected 'else'").is_error()) {
            if (expect_form_with_name(lexer, "none"sv, ""sv).is_error()) {
                lexer.ignore_while(isdigit);
            } else {
                lexer.ignore_while(isspace);
                if (!lexer.consume_specific(')'))
                    return TextFormatParseError(lexer, "Expected ')'");
            }
            lexer.ignore_while(isspace);
            if (!lexer.consume_specific(')'))
                return TextFormatParseError(lexer, "Expected ')'");
        }

        if (!expect_form_with_name(lexer, "end"sv, "Expected 'end'").is_error()) {
            if (expect_form_with_name(lexer, "none"sv, ""sv).is_error()) {
                lexer.ignore_while(isdigit);
            } else {
                lexer.ignore_while(isspace);
                if (!lexer.consume_specific(')'))
                    return TextFormatParseError(lexer, "Expected ')'");
            }
            lexer.ignore_while(isspace);
            if (!lexer.consume_specific(')'))
                return TextFormatParseError(lexer, "Expected ')'");
        }

        lexer.ignore_while(isspace);
        if (!lexer.consume_specific(')'))
            return TextFormatParseError(lexer, "Expected ')'");
    } else if (form_name == "table_branch"sv) {
        TODO();
    } else if (form_name == "table_element"sv) {
        TODO();
    } else if (form_name == "table_table"sv) {
        TODO();
    } else if (form_name == "types"sv) {
        TODO();
    } else {
        return TextFormatParseError(lexer, "Invalid argument type");
    }

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_instruction(GenericLexer& lexer, OutputStream& output_stream)
{
    lexer.ignore_while(isspace);
    if (!lexer.consume_specific('('))
        return TextFormatParseError(lexer, "Expected '('"sv);

    auto maybe_opcode = Wasm::instruction_from_name(lexer.consume_while([](auto c) { return isalnum(c) || c == '.' || c == ':' || c == '_'; }));
    if (!maybe_opcode.has_value())
        return TextFormatParseError(lexer, "Invalid instruction name");

    auto& opcode = *maybe_opcode;
    if (opcode == (opcode.value() & 0xff)) {
        output_stream << static_cast<u8>(opcode.value());
    } else {
        if (opcode == Wasm::Instructions::structured_else) {
            output_stream << (u8)0x05;
        } else if (opcode == Wasm::Instructions::structured_end) {
            output_stream << (u8)0x0b;
        } else if (0xfc00 == (opcode.value() & 0xfc00)) {
            // Standard multibyte instruction opcode
            output_stream << (u8)0xfc;
            if (LEB128::write_unsigned(static_cast<u32>(opcode.value() & ~0xfc00), output_stream).is_error())
                return TextFormatParseError(0, 0, "Stream Error");
        } else {
            return TextFormatParseError(lexer, "Invalid opcode"sv);
        }

        if (output_stream.handle_any_error())
            return TextFormatParseError(0, 0, "Stream Error");
    }

    lexer.ignore_while(isspace);
    while (!lexer.next_is(')'))
        TRY(parse_and_generate_argument(lexer, output_stream, opcode));

    if (opcode == Wasm::Instructions::memory_init) {
        // Special handling for this, since memory.init has an explicit
        // (but currently unused) memory index argument set to zero.
        if (LEB128::write_unsigned(0u, output_stream).is_error())
            return TextFormatParseError(lexer, "Stream Error");
    }

    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'"sv);

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_expression(GenericLexer& lexer, OutputStream& output_stream)
{
    lexer.ignore_while(isspace);
    while (!lexer.next_is(')')) {
        TRY(parse_and_generate_instruction(lexer, output_stream));
        lexer.ignore_while(isspace);
    }

    u8 instruction_end_tag = 0x0b;
    output_stream << instruction_end_tag;
    if (output_stream.handle_any_error())
        return TextFormatParseError(0, 0, "Stream Error");
    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_func(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "function"sv, "Expected 'function'"sv));
    TRY(expect_form_with_name(lexer, "locals"sv, "Expected 'locals'"sv));
    TRY(emit_vector_skipping_spaces<parse_and_generate_locals>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    }));
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");
    TRY(expect_form_with_name(lexer, "body"sv, "Expected 'body'"sv));
    TRY(parse_and_generate_expression(lexer, output_stream));
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");
    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");
    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_code_section(GenericLexer& lexer, OutputStream& output_stream)
{
    return emit_vector_skipping_spaces<emit_with_size<parse_and_generate_func>>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    });
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_custom_section(GenericLexer& lexer, OutputStream& output_stream)
{
    (void)lexer;
    (void)output_stream;
    TODO();
}

template<bool should_read_hex>
static ErrorOr<void, TextFormatParseError> parse_and_generate_data_init(GenericLexer& lexer, OutputStream& output_stream)
{
    if constexpr (should_read_hex) {
        lexer.ignore_while(isspace);
        String hex_value = lexer.consume_while(isxdigit);
        auto value = strtoul(hex_value.characters(), nullptr, 16);
        if (value > NumericLimits<u8>::max())
            return TextFormatParseError(lexer, "Invalid hex value (out of range)");
        output_stream << (u8)value;
    } else {
        auto ch = lexer.consume();
        output_stream << (u8)ch;
    }

    if (output_stream.handle_any_error())
        return TextFormatParseError(lexer, "Stream Error"sv);

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_data_section_data(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "data"sv, "Expected 'data'"sv));
    lexer.ignore_while(isspace);
    lexer.consume_specific("with value");

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific('('))
        return TextFormatParseError(lexer, "Expected '('"sv);

    lexer.ignore_while(isspace);
    auto form_name = lexer.consume_while(isalpha);

    if (form_name == "passive"sv) {
        u8 tag = 0x01;
        output_stream << tag;
        if (output_stream.handle_any_error())
            return TextFormatParseError(lexer, "Stream Error"sv);

        (void)expect_literal_word(lexer, "init"sv, ""sv);
        lexer.ignore_while(isspace);
        if (lexer.next_is(isdigit)) {
            auto size = TRY(expect_unsigned_number(lexer));
            TRY(expect_literal_word(lexer, "xu8"sv, "Expected <n>xu8"sv));
            (void)size;
        }

        lexer.ignore_while(isspace);
        if (lexer.consume_specific('"')) {
            TRY((emit_vector<parse_and_generate_data_init<false>, false>(lexer, output_stream, [&lexer] {
                return !lexer.next_is('"');
            })));
            if (!lexer.consume_specific('"'))
                return TextFormatParseError(lexer, "Expected '\"'"sv);
        } else {
            if (!lexer.consume_specific('('))
                return TextFormatParseError(lexer, "Expected '\"' or '('");

            TRY(emit_vector_skipping_spaces<parse_and_generate_data_init<true>>(lexer, output_stream, [&lexer] {
                return !lexer.next_is(')');
            }));
            if (!lexer.consume_specific(')'))
                return TextFormatParseError(lexer, "Expected ')'"sv);
        }
    } else if (form_name == "active"sv) {
        return TextFormatParseError(lexer, "Active data not implemented"sv);
    } else {
        return TextFormatParseError(lexer, "Expected 'active' or 'passive'"sv);
    }

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'"sv);

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'"sv);

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_data_section(GenericLexer& lexer, OutputStream& output_stream)
{
    return emit_vector_skipping_spaces<parse_and_generate_data_section_data>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    });
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_data_count_section(GenericLexer& lexer, OutputStream& output_stream)
{
    (void)lexer;
    (void)output_stream;
    TODO();
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_function_section_index(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "type"sv, "Expected 'type'"sv));
    TRY(expect_literal_word(lexer, "index"sv, "Expected 'index'"sv));
    lexer.ignore_while(isspace);
    auto index = TRY(expect_unsigned_number(lexer));
    if (LEB128::write_unsigned(index, output_stream).is_error())
        return TextFormatParseError(lexer, "Stream Error"sv);
    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_function_section(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(emit_vector_skipping_spaces<parse_and_generate_function_section_index>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    }));
    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_global_section(GenericLexer& lexer, OutputStream& output_stream)
{
    (void)lexer;
    (void)output_stream;
    TODO();
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_start_section(GenericLexer& lexer, OutputStream& output_stream)
{
    (void)lexer;
    (void)output_stream;
    TODO();
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_limits(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "limits"sv, "Expected 'limits'"sv));
    TRY(expect_literal_word(lexer, "min"sv, "Expected 'min'"sv));
    lexer.ignore_while(isspace);
    lexer.consume_specific('=');
    lexer.ignore_while(isspace);
    auto min_value = TRY(expect_unsigned_number(lexer));

    if (expect_literal_word(lexer, "unbounded"sv, ""sv).is_error()) {
        TRY(expect_literal_word(lexer, "max"sv, "Expected 'max' or 'unbounded'"sv));
        lexer.ignore_while(isspace);
        lexer.consume_specific('=');
        lexer.ignore_while(isspace);
        auto max_value = TRY(expect_unsigned_number(lexer));

        output_stream << (u8)1;
        if (output_stream.handle_any_error())
            return TextFormatParseError(lexer, "Stream Error"sv);

        if (LEB128::write_unsigned(min_value, output_stream).is_error())
            return TextFormatParseError(lexer, "Stream Error"sv);

        if (LEB128::write_unsigned(max_value, output_stream).is_error())
            return TextFormatParseError(lexer, "Stream Error"sv);
    } else {
        output_stream << (u8)0;
        if (output_stream.handle_any_error())
            return TextFormatParseError(lexer, "Stream Error"sv);

        if (LEB128::write_unsigned(min_value, output_stream).is_error())
            return TextFormatParseError(lexer, "Stream Error"sv);
    }

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_memory_section_memory(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "memory"sv, "Expected 'memory'"sv));
    TRY(expect_form_with_name(lexer, "type"sv, "Expected 'type'"sv));
    TRY(expect_literal_word(lexer, "memory"sv, "Expected 'memory'"sv));
    TRY(parse_and_generate_limits(lexer, output_stream));

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_memory_section(GenericLexer& lexer, OutputStream& output_stream)
{
    return emit_vector_skipping_spaces<parse_and_generate_memory_section_memory>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    });
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_element_section(GenericLexer& lexer, OutputStream& output_stream)
{
    (void)lexer;
    (void)output_stream;
    TODO();
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_export_section_export(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "export"sv, "Expected 'export'"sv));
    lexer.ignore_while(isspace);

    if (!lexer.consume_specific('`') && !lexer.consume_specific('\''))
        return TextFormatParseError(lexer, "Expected \"`\" or \"'\""sv);

    auto name = lexer.consume_until('\'');

    if (LEB128::write_unsigned(name.length(), output_stream).is_error())
        return TextFormatParseError(lexer, "Stream Error"sv);

    output_stream.write_or_error(name.bytes());
    if (output_stream.handle_any_error())
        return TextFormatParseError(lexer, "Stream Error"sv);

    // Accept 'as', but don't cry about it.
    (void)expect_literal_word(lexer, "as"sv, ""sv);

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific('('))
        return TextFormatParseError(lexer, "Expected '('"sv);

    lexer.ignore_while(isspace);
    auto form_name = lexer.consume_while(isalpha);

    u8 tag;
    if (form_name == "function"sv)
        tag = Wasm::Constants::extern_function_tag;
    else if (form_name == "table"sv)
        tag = Wasm::Constants::extern_table_tag;
    else if (form_name == "memory"sv)
        tag = Wasm::Constants::extern_memory_tag;
    else if (form_name == "global"sv)
        tag = Wasm::Constants::extern_global_tag;
    else
        return TextFormatParseError(lexer, "Invalid export type"sv);

    output_stream << tag;
    if (output_stream.handle_any_error())
        return TextFormatParseError(lexer, "Stream Error"sv);

    TRY(expect_literal_word(lexer, "index"sv, "Expected 'index'"sv));
    lexer.ignore_while(isspace);

    auto index = TRY(expect_unsigned_number(lexer));
    if (LEB128::write_unsigned(index, output_stream).is_error())
        return TextFormatParseError(lexer, "Stream Error"sv);

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_export_section(GenericLexer& lexer, OutputStream& output_stream)
{
    return emit_vector_skipping_spaces<parse_and_generate_export_section_export>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    });
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_import_section_import(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "import"sv, "Expected 'import'"sv));

    auto read_name = [&]() -> ErrorOr<StringView, TextFormatParseError> {
        lexer.ignore_while(isspace);
        if (!lexer.consume_specific('`') && !lexer.consume_specific('\''))
            return TextFormatParseError(lexer, "Expected \"`\" or \"'\""sv);

        return lexer.consume_until('\'');
    };

    auto write_name = [&](StringView name) -> ErrorOr<void, TextFormatParseError> {
        if (LEB128::write_unsigned(name.length(), output_stream).is_error())
            return TextFormatParseError(lexer, "Stream Error"sv);

        output_stream.write_or_error(name.bytes());
        if (output_stream.handle_any_error())
            return TextFormatParseError(lexer, "Stream Error"sv);
        return {};
    };

    auto import_name = TRY(read_name());
    TRY(expect_literal_word(lexer, "from"sv, "Expected 'from'"sv));
    auto module_name = TRY(read_name());

    TRY(write_name(module_name));
    TRY(write_name(import_name));

    // Accept 'as', but don't cry about it.
    (void)expect_literal_word(lexer, "as"sv, ""sv);

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific('('))
        return TextFormatParseError(lexer, "Expected '('"sv);

    lexer.ignore_while(isspace);
    if (lexer.consume_specific("type index"sv)) {
        output_stream << (u8)Wasm::Constants::extern_function_tag;
        if (output_stream.handle_any_error())
            return TextFormatParseError(lexer, "Stream Error"sv);
        lexer.ignore_while(isspace);

        auto index = TRY(expect_unsigned_number(lexer));
        if (LEB128::write_unsigned(index, output_stream).is_error())
            return TextFormatParseError(lexer, "Stream Error"sv);

        lexer.ignore_while(isspace);
        if (!lexer.consume_specific(')'))
            return TextFormatParseError(lexer, "Expected ')'");
    } else {
        TODO();
    }

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_import_section(GenericLexer& lexer, OutputStream& output_stream)
{
    return emit_vector_skipping_spaces<parse_and_generate_import_section_import>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    });
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_type_section_type(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "type"sv, "Expected 'type'"sv));
    TRY(expect_literal_word(lexer, "function"sv, "Expected 'function'"sv));
    output_stream << (u8)Wasm::Constants::function_signature_tag;
    if (output_stream.handle_any_error())
        return TextFormatParseError(lexer, "Stream Error"sv);

    TRY(expect_form_with_name(lexer, "parameters"sv, "Expected 'parameters'"sv));
    TRY(emit_vector_skipping_spaces<parse_and_generate_type>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    }));
    // parameters
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    TRY(expect_form_with_name(lexer, "results"sv, "Expected 'results'"sv));
    TRY(emit_vector_skipping_spaces<parse_and_generate_type>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    }));
    // results
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    lexer.ignore_while(isspace);
    // type
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_type_section(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(emit_vector_skipping_spaces<parse_and_generate_type_section_type>(lexer, output_stream, [&lexer] {
        return !lexer.next_is(')');
    }));
    return {};
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_table_section(GenericLexer& lexer, OutputStream& output_stream)
{
    (void)lexer;
    (void)output_stream;
    TODO();
}

static ErrorOr<void, TextFormatParseError> parse_and_generate_section(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "section"sv, "Expected 'section'"sv));

    lexer.ignore_while(isspace);
    auto section_name = lexer.consume_while(isalpha);

    if (section_name == "code"sv) {
        TRY((parse_and_generate_section_contents<Wasm::CodeSection, parse_and_generate_code_section>(lexer, output_stream)));
    } else if (section_name == "custom"sv) {
        TRY((parse_and_generate_section_contents<Wasm::CustomSection, parse_and_generate_custom_section>(lexer, output_stream)));
    } else if (section_name == "data"sv) { // "data" and "data count"
        if (lexer.consume_specific(" count"sv)) {
            TRY((parse_and_generate_section_contents<Wasm::DataCountSection, parse_and_generate_data_count_section>(lexer, output_stream)));
        } else {
            TRY((parse_and_generate_section_contents<Wasm::DataSection, parse_and_generate_data_section>(lexer, output_stream)));
        }
    } else if (section_name == "element"sv) {
        TRY((parse_and_generate_section_contents<Wasm::ElementSection, parse_and_generate_element_section>(lexer, output_stream)));
    } else if (section_name == "export"sv) {
        TRY((parse_and_generate_section_contents<Wasm::ExportSection, parse_and_generate_export_section>(lexer, output_stream)));
    } else if (section_name == "function"sv) {
        TRY((parse_and_generate_section_contents<Wasm::FunctionSection, parse_and_generate_function_section>(lexer, output_stream)));
    } else if (section_name == "global"sv) {
        TRY((parse_and_generate_section_contents<Wasm::GlobalSection, parse_and_generate_global_section>(lexer, output_stream)));
    } else if (section_name == "import"sv) {
        TRY((parse_and_generate_section_contents<Wasm::ImportSection, parse_and_generate_import_section>(lexer, output_stream)));
    } else if (section_name == "memory"sv) {
        TRY((parse_and_generate_section_contents<Wasm::MemorySection, parse_and_generate_memory_section>(lexer, output_stream)));
    } else if (section_name == "start"sv) {
        TRY((parse_and_generate_section_contents<Wasm::StartSection, parse_and_generate_start_section>(lexer, output_stream)));
    } else if (section_name == "table"sv) {
        TRY((parse_and_generate_section_contents<Wasm::TableSection, parse_and_generate_table_section>(lexer, output_stream)));
    } else if (section_name == "type"sv) {
        TRY((parse_and_generate_section_contents<Wasm::TypeSection, parse_and_generate_type_section>(lexer, output_stream)));
    } else {
        return TextFormatParseError(lexer, "Invalid section name");
    }

    lexer.ignore_while(isspace);
    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'");

    lexer.ignore_while(isspace);
    return {};
}

ErrorOr<void, TextFormatParseError> parse_and_generate_module_from_text_format(GenericLexer& lexer, OutputStream& output_stream)
{
    TRY(expect_form_with_name(lexer, "module"sv, "Expected 'module'"sv));

    // Wasm binary module magic
    output_stream.write_or_error("\0asm"sv.bytes());
    if (output_stream.handle_any_error())
        return TextFormatParseError(lexer, "Stream Error"sv);

    // Wasm binary format version
    output_stream.write_or_error("\1\0\0\0"sv.bytes());
    if (output_stream.handle_any_error())
        return TextFormatParseError(lexer, "Stream Error"sv);

    lexer.ignore_while(isspace);
    while (!lexer.next_is(')')) {
        TRY(parse_and_generate_section(lexer, output_stream));
        lexer.ignore_while(isspace);
    }

    if (!lexer.consume_specific(')'))
        return TextFormatParseError(lexer, "Expected ')'"sv);

    lexer.ignore_while(isspace);

    if (!lexer.is_eof())
        return TextFormatParseError(lexer, "Unexpected data past the end of (module ...)");

    return {};
}
}
