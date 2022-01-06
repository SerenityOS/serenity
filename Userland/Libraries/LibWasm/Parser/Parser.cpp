/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LEB128.h>
#include <AK/ScopeGuard.h>
#include <AK/ScopeLogger.h>
#include <LibWasm/Types.h>

namespace Wasm {

ParseError with_eof_check(InputStream const& stream, ParseError error_if_not_eof)
{
    if (stream.unreliable_eof())
        return ParseError::UnexpectedEof;
    return error_if_not_eof;
}

template<typename T>
static auto parse_vector(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger;
    if constexpr (requires { T::parse(stream); }) {
        using ResultT = typename decltype(T::parse(stream))::ValueType;
        size_t count;
        if (!LEB128::read_unsigned(stream, count))
            return ParseResult<Vector<ResultT>> { with_eof_check(stream, ParseError::ExpectedSize) };

        Vector<ResultT> entries;
        for (size_t i = 0; i < count; ++i) {
            auto result = T::parse(stream);
            if (result.is_error())
                return ParseResult<Vector<ResultT>> { result.error() };
            entries.append(result.release_value());
        }
        return ParseResult<Vector<ResultT>> { move(entries) };
    } else {
        size_t count;
        if (!LEB128::read_unsigned(stream, count))
            return ParseResult<Vector<T>> { with_eof_check(stream, ParseError::ExpectedSize) };

        Vector<T> entries;
        for (size_t i = 0; i < count; ++i) {
            if constexpr (IsSame<T, size_t>) {
                size_t value;
                if (!LEB128::read_unsigned(stream, value))
                    return ParseResult<Vector<T>> { with_eof_check(stream, ParseError::ExpectedSize) };
                entries.append(value);
            } else if constexpr (IsSame<T, ssize_t>) {
                ssize_t value;
                if (!LEB128::read_signed(stream, value))
                    return ParseResult<Vector<T>> { with_eof_check(stream, ParseError::ExpectedSize) };
                entries.append(value);
            } else if constexpr (IsSame<T, u8>) {
                if (count > Constants::max_allowed_vector_size)
                    return ParseResult<Vector<T>> { ParseError::HugeAllocationRequested };
                entries.resize(count);
                if (!stream.read_or_error({ entries.data(), entries.size() }))
                    return ParseResult<Vector<T>> { with_eof_check(stream, ParseError::InvalidInput) };
                break; // Note: We read this all in one go!
            }
        }
        return ParseResult<Vector<T>> { move(entries) };
    }
}

static ParseResult<String> parse_name(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger;
    auto data = parse_vector<u8>(stream);
    if (data.is_error())
        return data.error();

    return String::copy(data.value());
}

template<typename T>
struct ParseUntilAnyOfResult {
    u8 terminator { 0 };
    Vector<T> values;
};
template<typename T, u8... terminators, typename... Args>
static ParseResult<ParseUntilAnyOfResult<T>> parse_until_any_of(InputStream& stream, Args&... args) requires(requires(InputStream& stream, Args... args) { T::parse(stream, args...); })
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger;
    ReconsumableStream new_stream { stream };
    ScopeGuard drain_errors {
        [&] {
            new_stream.handle_any_error();
        }
    };

    ParseUntilAnyOfResult<T> result;
    for (;;) {
        u8 byte;
        new_stream >> byte;
        if (new_stream.has_any_error())
            return with_eof_check(stream, ParseError::ExpectedValueOrTerminator);

        constexpr auto equals = [](auto&& a, auto&& b) { return a == b; };

        if ((... || equals(byte, terminators))) {
            result.terminator = byte;
            return result;
        }

        new_stream.unread({ &byte, 1 });
        auto parse_result = T::parse(new_stream, args...);
        if (parse_result.is_error())
            return parse_result.error();

        result.values.extend(parse_result.release_value());
    }
}

ParseResult<ValueType> ValueType::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("ValueType");
    u8 tag;
    stream >> tag;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);
    switch (tag) {
    case Constants::i32_tag:
        return ValueType(I32);
    case Constants::i64_tag:
        return ValueType(I64);
    case Constants::f32_tag:
        return ValueType(F32);
    case Constants::f64_tag:
        return ValueType(F64);
    case Constants::function_reference_tag:
        return ValueType(FunctionReference);
    case Constants::extern_reference_tag:
        return ValueType(ExternReference);
    default:
        return with_eof_check(stream, ParseError::InvalidTag);
    }
}

ParseResult<ResultType> ResultType::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("ResultType");
    auto types = parse_vector<ValueType>(stream);
    if (types.is_error())
        return types.error();
    return ResultType { types.release_value() };
}

ParseResult<FunctionType> FunctionType::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("FunctionType");
    u8 tag;
    stream >> tag;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);

    if (tag != Constants::function_signature_tag) {
        dbgln("Expected 0x60, but found {:#x}", tag);
        return with_eof_check(stream, ParseError::InvalidTag);
    }

    auto parameters_result = parse_vector<ValueType>(stream);
    if (parameters_result.is_error())
        return parameters_result.error();
    auto results_result = parse_vector<ValueType>(stream);
    if (results_result.is_error())
        return results_result.error();

    return FunctionType { parameters_result.release_value(), results_result.release_value() };
}

ParseResult<Limits> Limits::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Limits");
    u8 flag;
    stream >> flag;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);

    if (flag > 1)
        return with_eof_check(stream, ParseError::InvalidTag);

    size_t min;
    if (!LEB128::read_unsigned(stream, min))
        return with_eof_check(stream, ParseError::ExpectedSize);

    Optional<u32> max;
    if (flag) {
        size_t value;
        if (!LEB128::read_unsigned(stream, value))
            return with_eof_check(stream, ParseError::ExpectedSize);
        max = value;
    }

    return Limits { static_cast<u32>(min), move(max) };
}

ParseResult<MemoryType> MemoryType::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("MemoryType");
    auto limits_result = Limits::parse(stream);
    if (limits_result.is_error())
        return limits_result.error();
    return MemoryType { limits_result.release_value() };
}

ParseResult<TableType> TableType::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("TableType");
    auto type_result = ValueType::parse(stream);
    if (type_result.is_error())
        return type_result.error();
    if (!type_result.value().is_reference())
        return with_eof_check(stream, ParseError::InvalidType);
    auto limits_result = Limits::parse(stream);
    if (limits_result.is_error())
        return limits_result.error();
    return TableType { type_result.release_value(), limits_result.release_value() };
}

ParseResult<GlobalType> GlobalType::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("GlobalType");
    auto type_result = ValueType::parse(stream);
    if (type_result.is_error())
        return type_result.error();
    u8 mutable_;
    stream >> mutable_;

    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);

    if (mutable_ > 1)
        return with_eof_check(stream, ParseError::InvalidTag);

    return GlobalType { type_result.release_value(), mutable_ == 0x01 };
}

ParseResult<BlockType> BlockType::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("BlockType");
    u8 kind;
    stream >> kind;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);
    if (kind == Constants::empty_block_tag)
        return BlockType {};

    {
        InputMemoryStream value_stream { ReadonlyBytes { &kind, 1 } };
        if (auto value_type = ValueType::parse(value_stream); !value_type.is_error())
            return BlockType { value_type.release_value() };
    }

    ReconsumableStream new_stream { stream };
    new_stream.unread({ &kind, 1 });
    ScopeGuard drain_errors {
        [&] {
            new_stream.handle_any_error();
        }
    };

    ssize_t index_value;
    if (!LEB128::read_signed(new_stream, index_value))
        return with_eof_check(stream, ParseError::ExpectedIndex);

    if (index_value < 0) {
        dbgln("Invalid type index {}", index_value);
        return with_eof_check(stream, ParseError::InvalidIndex);
    }

    return BlockType { TypeIndex(index_value) };
}

ParseResult<Vector<Instruction>> Instruction::parse(InputStream& stream, InstructionPointer& ip)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Instruction");
    u8 byte;
    stream >> byte;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);
    OpCode opcode { byte };
    ++ip;

    switch (opcode.value()) {
    case Instructions::block.value():
    case Instructions::loop.value():
    case Instructions::if_.value(): {
        auto block_type = BlockType::parse(stream);
        if (block_type.is_error())
            return block_type.error();
        Vector<Instruction> instructions;
        InstructionPointer end_ip, else_ip;

        {
            auto result = parse_until_any_of<Instruction, 0x0b, 0x05>(stream, ip);
            if (result.is_error())
                return result.error();

            if (result.value().terminator == 0x0b) {
                // block/loop/if without else
                result.value().values.append(Instruction { Instructions::structured_end });

                // Transform op(..., instr*) -> op(...) instr* op(end(ip))
                result.value().values.prepend(Instruction { opcode, StructuredInstructionArgs { BlockType { block_type.release_value() }, ip.value(), {} } });
                ++ip;
                return result.release_value().values;
            }

            // Transform op(..., instr*, instr*) -> op(...) instr* op(else(ip) instr* op(end(ip))
            VERIFY(result.value().terminator == 0x05);
            instructions.extend(result.release_value().values);
            instructions.append(Instruction { Instructions::structured_else });
            ++ip;
            else_ip = ip.value();
        }
        // if with else
        {
            auto result = parse_until_any_of<Instruction, 0x0b>(stream, ip);
            if (result.is_error())
                return result.error();
            instructions.extend(result.release_value().values);
            instructions.append(Instruction { Instructions::structured_end });
            ++ip;
            end_ip = ip.value();
        }

        instructions.prepend(Instruction { opcode, StructuredInstructionArgs { BlockType { block_type.release_value() }, end_ip, else_ip } });
        return instructions;
    }
    case Instructions::br.value():
    case Instructions::br_if.value(): {
        // branches with a single label immediate
        auto index = GenericIndexParser<LabelIndex>::parse(stream);
        if (index.is_error())
            return index.error();

        return Vector { Instruction { opcode, index.release_value() } };
    }
    case Instructions::br_table.value(): {
        // br_table label* label
        auto labels = parse_vector<GenericIndexParser<LabelIndex>>(stream);
        if (labels.is_error())
            return labels.error();

        auto default_label = GenericIndexParser<LabelIndex>::parse(stream);
        if (default_label.is_error())
            return default_label.error();

        return Vector { Instruction { opcode, TableBranchArgs { labels.release_value(), default_label.release_value() } } };
    }
    case Instructions::call.value(): {
        // call function
        auto function_index = GenericIndexParser<FunctionIndex>::parse(stream);
        if (function_index.is_error())
            return function_index.error();

        return Vector { Instruction { opcode, function_index.release_value() } };
    }
    case Instructions::call_indirect.value(): {
        // call_indirect type table
        auto type_index = GenericIndexParser<TypeIndex>::parse(stream);
        if (type_index.is_error())
            return type_index.error();

        auto table_index = GenericIndexParser<TableIndex>::parse(stream);
        if (table_index.is_error())
            return table_index.error();

        return Vector { Instruction { opcode, IndirectCallArgs { type_index.release_value(), table_index.release_value() } } };
    }
    case Instructions::i32_load.value():
    case Instructions::i64_load.value():
    case Instructions::f32_load.value():
    case Instructions::f64_load.value():
    case Instructions::i32_load8_s.value():
    case Instructions::i32_load8_u.value():
    case Instructions::i32_load16_s.value():
    case Instructions::i32_load16_u.value():
    case Instructions::i64_load8_s.value():
    case Instructions::i64_load8_u.value():
    case Instructions::i64_load16_s.value():
    case Instructions::i64_load16_u.value():
    case Instructions::i64_load32_s.value():
    case Instructions::i64_load32_u.value():
    case Instructions::i32_store.value():
    case Instructions::i64_store.value():
    case Instructions::f32_store.value():
    case Instructions::f64_store.value():
    case Instructions::i32_store8.value():
    case Instructions::i32_store16.value():
    case Instructions::i64_store8.value():
    case Instructions::i64_store16.value():
    case Instructions::i64_store32.value(): {
        // op (align offset)
        size_t align, offset;
        if (!LEB128::read_unsigned(stream, align))
            return with_eof_check(stream, ParseError::InvalidInput);
        if (!LEB128::read_unsigned(stream, offset))
            return with_eof_check(stream, ParseError::InvalidInput);

        return Vector { Instruction { opcode, MemoryArgument { static_cast<u32>(align), static_cast<u32>(offset) } } };
    }
    case Instructions::local_get.value():
    case Instructions::local_set.value():
    case Instructions::local_tee.value(): {
        auto index = GenericIndexParser<LocalIndex>::parse(stream);
        if (index.is_error())
            return index.error();

        return Vector { Instruction { opcode, index.release_value() } };
    }
    case Instructions::global_get.value():
    case Instructions::global_set.value(): {
        auto index = GenericIndexParser<GlobalIndex>::parse(stream);
        if (index.is_error())
            return index.error();

        return Vector { Instruction { opcode, index.release_value() } };
    }
    case Instructions::memory_size.value():
    case Instructions::memory_grow.value(): {
        // op 0x0
        // The zero is currently unused.
        u8 unused;
        stream >> unused;
        if (stream.has_any_error())
            return with_eof_check(stream, ParseError::ExpectedKindTag);
        if (unused != 0x00) {
            dbgln("Invalid tag in memory_grow {}", unused);
            return with_eof_check(stream, ParseError::InvalidTag);
        }

        return Vector { Instruction { opcode } };
    }
    case Instructions::i32_const.value(): {
        i32 value;
        if (!LEB128::read_signed(stream, value))
            return with_eof_check(stream, ParseError::ExpectedSignedImmediate);

        return Vector { Instruction { opcode, value } };
    }
    case Instructions::i64_const.value(): {
        // op literal
        i64 value;
        if (!LEB128::read_signed(stream, value))
            return with_eof_check(stream, ParseError::ExpectedSignedImmediate);

        return Vector { Instruction { opcode, value } };
    }
    case Instructions::f32_const.value(): {
        // op literal
        LittleEndian<u32> value;
        stream >> value;
        if (stream.has_any_error())
            return with_eof_check(stream, ParseError::ExpectedFloatingImmediate);

        auto floating = bit_cast<float>(static_cast<u32>(value));
        return Vector { Instruction { opcode, floating } };
    }
    case Instructions::f64_const.value(): {
        // op literal
        LittleEndian<u64> value;
        stream >> value;
        if (stream.has_any_error())
            return with_eof_check(stream, ParseError::ExpectedFloatingImmediate);

        auto floating = bit_cast<double>(static_cast<u64>(value));
        return Vector { Instruction { opcode, floating } };
    }
    case Instructions::table_get.value():
    case Instructions::table_set.value(): {
        auto index = GenericIndexParser<TableIndex>::parse(stream);
        if (index.is_error())
            return index.error();

        return Vector { Instruction { opcode, index.release_value() } };
    }
    case Instructions::select_typed.value(): {
        auto types = parse_vector<ValueType>(stream);
        if (types.is_error())
            return types.error();

        return Vector { Instruction { opcode, types.release_value() } };
    }
    case Instructions::ref_null.value(): {
        auto type = ValueType::parse(stream);
        if (type.is_error())
            return type.error();
        if (!type.value().is_reference())
            return ParseError::InvalidType;

        return Vector { Instruction { opcode, type.release_value() } };
    }
    case Instructions::ref_func.value(): {
        auto index = GenericIndexParser<FunctionIndex>::parse(stream);
        if (index.is_error())
            return index.error();

        return Vector { Instruction { opcode, index.release_value() } };
    }
    case Instructions::ref_is_null.value():
    case Instructions::unreachable.value():
    case Instructions::nop.value():
    case Instructions::return_.value():
    case Instructions::drop.value():
    case Instructions::select.value():
    case Instructions::i32_eqz.value():
    case Instructions::i32_eq.value():
    case Instructions::i32_ne.value():
    case Instructions::i32_lts.value():
    case Instructions::i32_ltu.value():
    case Instructions::i32_gts.value():
    case Instructions::i32_gtu.value():
    case Instructions::i32_les.value():
    case Instructions::i32_leu.value():
    case Instructions::i32_ges.value():
    case Instructions::i32_geu.value():
    case Instructions::i64_eqz.value():
    case Instructions::i64_eq.value():
    case Instructions::i64_ne.value():
    case Instructions::i64_lts.value():
    case Instructions::i64_ltu.value():
    case Instructions::i64_gts.value():
    case Instructions::i64_gtu.value():
    case Instructions::i64_les.value():
    case Instructions::i64_leu.value():
    case Instructions::i64_ges.value():
    case Instructions::i64_geu.value():
    case Instructions::f32_eq.value():
    case Instructions::f32_ne.value():
    case Instructions::f32_lt.value():
    case Instructions::f32_gt.value():
    case Instructions::f32_le.value():
    case Instructions::f32_ge.value():
    case Instructions::f64_eq.value():
    case Instructions::f64_ne.value():
    case Instructions::f64_lt.value():
    case Instructions::f64_gt.value():
    case Instructions::f64_le.value():
    case Instructions::f64_ge.value():
    case Instructions::i32_clz.value():
    case Instructions::i32_ctz.value():
    case Instructions::i32_popcnt.value():
    case Instructions::i32_add.value():
    case Instructions::i32_sub.value():
    case Instructions::i32_mul.value():
    case Instructions::i32_divs.value():
    case Instructions::i32_divu.value():
    case Instructions::i32_rems.value():
    case Instructions::i32_remu.value():
    case Instructions::i32_and.value():
    case Instructions::i32_or.value():
    case Instructions::i32_xor.value():
    case Instructions::i32_shl.value():
    case Instructions::i32_shrs.value():
    case Instructions::i32_shru.value():
    case Instructions::i32_rotl.value():
    case Instructions::i32_rotr.value():
    case Instructions::i64_clz.value():
    case Instructions::i64_ctz.value():
    case Instructions::i64_popcnt.value():
    case Instructions::i64_add.value():
    case Instructions::i64_sub.value():
    case Instructions::i64_mul.value():
    case Instructions::i64_divs.value():
    case Instructions::i64_divu.value():
    case Instructions::i64_rems.value():
    case Instructions::i64_remu.value():
    case Instructions::i64_and.value():
    case Instructions::i64_or.value():
    case Instructions::i64_xor.value():
    case Instructions::i64_shl.value():
    case Instructions::i64_shrs.value():
    case Instructions::i64_shru.value():
    case Instructions::i64_rotl.value():
    case Instructions::i64_rotr.value():
    case Instructions::f32_abs.value():
    case Instructions::f32_neg.value():
    case Instructions::f32_ceil.value():
    case Instructions::f32_floor.value():
    case Instructions::f32_trunc.value():
    case Instructions::f32_nearest.value():
    case Instructions::f32_sqrt.value():
    case Instructions::f32_add.value():
    case Instructions::f32_sub.value():
    case Instructions::f32_mul.value():
    case Instructions::f32_div.value():
    case Instructions::f32_min.value():
    case Instructions::f32_max.value():
    case Instructions::f32_copysign.value():
    case Instructions::f64_abs.value():
    case Instructions::f64_neg.value():
    case Instructions::f64_ceil.value():
    case Instructions::f64_floor.value():
    case Instructions::f64_trunc.value():
    case Instructions::f64_nearest.value():
    case Instructions::f64_sqrt.value():
    case Instructions::f64_add.value():
    case Instructions::f64_sub.value():
    case Instructions::f64_mul.value():
    case Instructions::f64_div.value():
    case Instructions::f64_min.value():
    case Instructions::f64_max.value():
    case Instructions::f64_copysign.value():
    case Instructions::i32_wrap_i64.value():
    case Instructions::i32_trunc_sf32.value():
    case Instructions::i32_trunc_uf32.value():
    case Instructions::i32_trunc_sf64.value():
    case Instructions::i32_trunc_uf64.value():
    case Instructions::i64_extend_si32.value():
    case Instructions::i64_extend_ui32.value():
    case Instructions::i64_trunc_sf32.value():
    case Instructions::i64_trunc_uf32.value():
    case Instructions::i64_trunc_sf64.value():
    case Instructions::i64_trunc_uf64.value():
    case Instructions::f32_convert_si32.value():
    case Instructions::f32_convert_ui32.value():
    case Instructions::f32_convert_si64.value():
    case Instructions::f32_convert_ui64.value():
    case Instructions::f32_demote_f64.value():
    case Instructions::f64_convert_si32.value():
    case Instructions::f64_convert_ui32.value():
    case Instructions::f64_convert_si64.value():
    case Instructions::f64_convert_ui64.value():
    case Instructions::f64_promote_f32.value():
    case Instructions::i32_reinterpret_f32.value():
    case Instructions::i64_reinterpret_f64.value():
    case Instructions::f32_reinterpret_i32.value():
    case Instructions::f64_reinterpret_i64.value():
    case Instructions::i32_extend8_s.value():
    case Instructions::i32_extend16_s.value():
    case Instructions::i64_extend8_s.value():
    case Instructions::i64_extend16_s.value():
    case Instructions::i64_extend32_s.value():
        return Vector { Instruction { opcode } };
    case 0xfc: {
        // These are multibyte instructions.
        u32 selector;
        if (!LEB128::read_unsigned(stream, selector))
            return with_eof_check(stream, ParseError::InvalidInput);
        switch (selector) {
        case Instructions::i32_trunc_sat_f32_s_second:
        case Instructions::i32_trunc_sat_f32_u_second:
        case Instructions::i32_trunc_sat_f64_s_second:
        case Instructions::i32_trunc_sat_f64_u_second:
        case Instructions::i64_trunc_sat_f32_s_second:
        case Instructions::i64_trunc_sat_f32_u_second:
        case Instructions::i64_trunc_sat_f64_s_second:
        case Instructions::i64_trunc_sat_f64_u_second:
            return Vector { Instruction { OpCode { 0xfc00 | selector } } };
        case Instructions::memory_init_second: {
            auto index = GenericIndexParser<DataIndex>::parse(stream);
            if (index.is_error())
                return index.error();
            u8 unused;
            stream >> unused;
            if (stream.has_any_error())
                return with_eof_check(stream, ParseError::InvalidInput);
            if (unused != 0x00)
                return ParseError::InvalidImmediate;
            return Vector { Instruction { OpCode { 0xfc00 | selector }, index.release_value() } };
        }
        case Instructions::data_drop_second: {
            auto index = GenericIndexParser<DataIndex>::parse(stream);
            if (index.is_error())
                return index.error();
            return Vector { Instruction { OpCode { 0xfc00 | selector }, index.release_value() } };
        }
        case Instructions::memory_copy_second: {
            for (size_t i = 0; i < 2; ++i) {
                u8 unused;
                stream >> unused;
                if (stream.has_any_error())
                    return with_eof_check(stream, ParseError::InvalidInput);
                if (unused != 0x00)
                    return ParseError::InvalidImmediate;
            }
            return Vector { Instruction { OpCode { 0xfc00 | selector } } };
        }
        case Instructions::memory_fill_second: {
            u8 unused;
            stream >> unused;
            if (stream.has_any_error())
                return with_eof_check(stream, ParseError::InvalidInput);
            if (unused != 0x00)
                return ParseError::InvalidImmediate;
            return Vector { Instruction { OpCode { 0xfc00 | selector } } };
        }
        case Instructions::table_init_second: {
            auto element_index = GenericIndexParser<ElementIndex>::parse(stream);
            if (element_index.is_error())
                return element_index.error();
            auto table_index = GenericIndexParser<TableIndex>::parse(stream);
            if (table_index.is_error())
                return table_index.error();
            return Vector { Instruction { OpCode { 0xfc00 | selector }, TableElementArgs { element_index.release_value(), table_index.release_value() } } };
        }
        case Instructions::elem_drop_second: {
            auto element_index = GenericIndexParser<ElementIndex>::parse(stream);
            if (element_index.is_error())
                return element_index.error();
            return Vector { Instruction { OpCode { 0xfc00 | selector }, element_index.release_value() } };
        }
        case Instructions::table_copy_second: {
            auto lhs = GenericIndexParser<TableIndex>::parse(stream);
            if (lhs.is_error())
                return lhs.error();
            auto rhs = GenericIndexParser<TableIndex>::parse(stream);
            if (rhs.is_error())
                return rhs.error();
            return Vector { Instruction { OpCode { 0xfc00 | selector }, TableTableArgs { lhs.release_value(), rhs.release_value() } } };
        }
        case Instructions::table_grow_second:
        case Instructions::table_size_second:
        case Instructions::table_fill_second: {
            auto index = GenericIndexParser<TableIndex>::parse(stream);
            if (index.is_error())
                return index.error();
            return Vector { Instruction { OpCode { 0xfc00 | selector }, index.release_value() } };
        }
        default:
            return ParseError::UnknownInstruction;
        }
    }
    }

    return ParseError::UnknownInstruction;
}

ParseResult<CustomSection> CustomSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("CustomSection");
    auto name = parse_name(stream);
    if (name.is_error())
        return name.error();

    ByteBuffer data_buffer;
    if (data_buffer.try_resize(64).is_error())
        return ParseError::OutOfMemory;

    while (!stream.has_any_error() && !stream.unreliable_eof()) {
        char buf[16];
        auto size = stream.read({ buf, 16 });
        if (size == 0)
            break;
        if (data_buffer.try_append(buf, size).is_error())
            return with_eof_check(stream, ParseError::HugeAllocationRequested);
    }

    return CustomSection(name.release_value(), move(data_buffer));
}

ParseResult<TypeSection> TypeSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("TypeSection");
    auto types = parse_vector<FunctionType>(stream);
    if (types.is_error())
        return types.error();
    return TypeSection { types.release_value() };
}

ParseResult<ImportSection::Import> ImportSection::Import::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Import");
    auto module = parse_name(stream);
    if (module.is_error())
        return module.error();
    auto name = parse_name(stream);
    if (name.is_error())
        return name.error();
    u8 tag;
    stream >> tag;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);

    switch (tag) {
    case Constants::extern_function_tag: {
        auto index = GenericIndexParser<TypeIndex>::parse(stream);
        if (index.is_error())
            return index.error();
        return Import { module.release_value(), name.release_value(), index.release_value() };
    }
    case Constants::extern_table_tag:
        return parse_with_type<TableType>(stream, module, name);
    case Constants::extern_memory_tag:
        return parse_with_type<MemoryType>(stream, module, name);
    case Constants::extern_global_tag:
        return parse_with_type<GlobalType>(stream, module, name);
    default:
        return with_eof_check(stream, ParseError::InvalidTag);
    }
}

ParseResult<ImportSection> ImportSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("ImportSection");
    auto imports = parse_vector<Import>(stream);
    if (imports.is_error())
        return imports.error();
    return ImportSection { imports.release_value() };
}

ParseResult<FunctionSection> FunctionSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("FunctionSection");
    auto indices = parse_vector<size_t>(stream);
    if (indices.is_error())
        return indices.error();

    Vector<TypeIndex> typed_indices;
    typed_indices.ensure_capacity(indices.value().size());
    for (auto entry : indices.value())
        typed_indices.append(entry);

    return FunctionSection { move(typed_indices) };
}

ParseResult<TableSection::Table> TableSection::Table::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Table");
    auto type = TableType::parse(stream);
    if (type.is_error())
        return type.error();
    return Table { type.release_value() };
}

ParseResult<TableSection> TableSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("TableSection");
    auto tables = parse_vector<Table>(stream);
    if (tables.is_error())
        return tables.error();
    return TableSection { tables.release_value() };
}

ParseResult<MemorySection::Memory> MemorySection::Memory::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Memory");
    auto type = MemoryType::parse(stream);
    if (type.is_error())
        return type.error();
    return Memory { type.release_value() };
}

ParseResult<MemorySection> MemorySection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("MemorySection");
    auto memories = parse_vector<Memory>(stream);
    if (memories.is_error())
        return memories.error();
    return MemorySection { memories.release_value() };
}

ParseResult<Expression> Expression::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Expression");
    InstructionPointer ip { 0 };
    auto instructions = parse_until_any_of<Instruction, 0x0b>(stream, ip);
    if (instructions.is_error())
        return instructions.error();

    return Expression { move(instructions.value().values) };
}

ParseResult<GlobalSection::Global> GlobalSection::Global::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Global");
    auto type = GlobalType::parse(stream);
    if (type.is_error())
        return type.error();
    auto exprs = Expression::parse(stream);
    if (exprs.is_error())
        return exprs.error();
    return Global { type.release_value(), exprs.release_value() };
}

ParseResult<GlobalSection> GlobalSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("GlobalSection");
    auto result = parse_vector<Global>(stream);
    if (result.is_error())
        return result.error();
    return GlobalSection { result.release_value() };
}

ParseResult<ExportSection::Export> ExportSection::Export::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Export");
    auto name = parse_name(stream);
    if (name.is_error())
        return name.error();
    u8 tag;
    stream >> tag;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);

    size_t index;
    if (!LEB128::read_unsigned(stream, index))
        return with_eof_check(stream, ParseError::ExpectedIndex);

    switch (tag) {
    case Constants::extern_function_tag:
        return Export { name.release_value(), ExportDesc { FunctionIndex { index } } };
    case Constants::extern_table_tag:
        return Export { name.release_value(), ExportDesc { TableIndex { index } } };
    case Constants::extern_memory_tag:
        return Export { name.release_value(), ExportDesc { MemoryIndex { index } } };
    case Constants::extern_global_tag:
        return Export { name.release_value(), ExportDesc { GlobalIndex { index } } };
    default:
        return with_eof_check(stream, ParseError::InvalidTag);
    }
}

ParseResult<ExportSection> ExportSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("ExportSection");
    auto result = parse_vector<Export>(stream);
    if (result.is_error())
        return result.error();
    return ExportSection { result.release_value() };
}

ParseResult<StartSection::StartFunction> StartSection::StartFunction::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("StartFunction");
    auto index = GenericIndexParser<FunctionIndex>::parse(stream);
    if (index.is_error())
        return index.error();
    return StartFunction { index.release_value() };
}

ParseResult<StartSection> StartSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("StartSection");
    auto result = StartFunction::parse(stream);
    if (result.is_error())
        return result.error();
    return StartSection { result.release_value() };
}

ParseResult<ElementSection::SegmentType0> ElementSection::SegmentType0::parse(InputStream& stream)
{
    auto expression = Expression::parse(stream);
    if (expression.is_error())
        return expression.error();
    auto indices = parse_vector<GenericIndexParser<FunctionIndex>>(stream);
    if (indices.is_error())
        return indices.error();

    return SegmentType0 { indices.release_value(), Active { 0, expression.release_value() } };
}

ParseResult<ElementSection::SegmentType1> ElementSection::SegmentType1::parse(InputStream& stream)
{
    u8 kind;
    stream >> kind;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);
    if (kind != 0)
        return ParseError::InvalidTag;
    auto indices = parse_vector<GenericIndexParser<FunctionIndex>>(stream);
    if (indices.is_error())
        return indices.error();

    return SegmentType1 { indices.release_value() };
}

ParseResult<ElementSection::SegmentType2> ElementSection::SegmentType2::parse(InputStream& stream)
{
    dbgln("Type 2");
    (void)stream;
    return ParseError::NotImplemented;
}

ParseResult<ElementSection::SegmentType3> ElementSection::SegmentType3::parse(InputStream& stream)
{
    dbgln("Type 3");
    (void)stream;
    return ParseError::NotImplemented;
}

ParseResult<ElementSection::SegmentType4> ElementSection::SegmentType4::parse(InputStream& stream)
{
    dbgln("Type 4");
    (void)stream;
    return ParseError::NotImplemented;
}

ParseResult<ElementSection::SegmentType5> ElementSection::SegmentType5::parse(InputStream& stream)
{
    dbgln("Type 5");
    (void)stream;
    return ParseError::NotImplemented;
}

ParseResult<ElementSection::SegmentType6> ElementSection::SegmentType6::parse(InputStream& stream)
{
    dbgln("Type 6");
    (void)stream;
    return ParseError::NotImplemented;
}

ParseResult<ElementSection::SegmentType7> ElementSection::SegmentType7::parse(InputStream& stream)
{
    dbgln("Type 7");
    (void)stream;
    return ParseError::NotImplemented;
}

ParseResult<ElementSection::Element> ElementSection::Element::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Element");
    u8 tag;
    stream >> tag;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);

    switch (tag) {
    case 0x00:
        if (auto result = SegmentType0::parse(stream); result.is_error()) {
            return result.error();
        } else {
            Vector<Instruction> instructions;
            for (auto& index : result.value().function_indices)
                instructions.empend(Instructions::ref_func, index);
            return Element { ValueType(ValueType::FunctionReference), { Expression { move(instructions) } }, move(result.value().mode) };
        }
    case 0x01:
        if (auto result = SegmentType1::parse(stream); result.is_error()) {
            return result.error();
        } else {
            Vector<Instruction> instructions;
            for (auto& index : result.value().function_indices)
                instructions.empend(Instructions::ref_func, index);
            return Element { ValueType(ValueType::FunctionReference), { Expression { move(instructions) } }, Passive {} };
        }
    case 0x02:
        if (auto result = SegmentType2::parse(stream); result.is_error()) {
            return result.error();
        } else {
            return ParseError::NotImplemented;
        }
    case 0x03:
        if (auto result = SegmentType3::parse(stream); result.is_error()) {
            return result.error();
        } else {
            return ParseError::NotImplemented;
        }
    case 0x04:
        if (auto result = SegmentType4::parse(stream); result.is_error()) {
            return result.error();
        } else {
            return ParseError::NotImplemented;
        }
    case 0x05:
        if (auto result = SegmentType5::parse(stream); result.is_error()) {
            return result.error();
        } else {
            return ParseError::NotImplemented;
        }
    case 0x06:
        if (auto result = SegmentType6::parse(stream); result.is_error()) {
            return result.error();
        } else {
            return ParseError::NotImplemented;
        }
    case 0x07:
        if (auto result = SegmentType7::parse(stream); result.is_error()) {
            return result.error();
        } else {
            return ParseError::NotImplemented;
        }
    default:
        return ParseError::InvalidTag;
    }
}

ParseResult<ElementSection> ElementSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("ElementSection");
    auto result = parse_vector<Element>(stream);
    if (result.is_error())
        return result.error();
    return ElementSection { result.release_value() };
}

ParseResult<Locals> Locals::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Locals");
    size_t count;
    if (!LEB128::read_unsigned(stream, count))
        return with_eof_check(stream, ParseError::InvalidSize);

    if (count > Constants::max_allowed_function_locals_per_type)
        return with_eof_check(stream, ParseError::HugeAllocationRequested);

    auto type = ValueType::parse(stream);
    if (type.is_error())
        return type.error();

    return Locals { static_cast<u32>(count), type.release_value() };
}

ParseResult<CodeSection::Func> CodeSection::Func::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Func");
    auto locals = parse_vector<Locals>(stream);
    if (locals.is_error())
        return locals.error();
    auto body = Expression::parse(stream);
    if (body.is_error())
        return body.error();
    return Func { locals.release_value(), body.release_value() };
}

ParseResult<CodeSection::Code> CodeSection::Code::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Code");
    size_t size;
    if (!LEB128::read_unsigned(stream, size))
        return with_eof_check(stream, ParseError::InvalidSize);

    auto constrained_stream = ConstrainedStream { stream, size };
    ScopeGuard drain_errors {
        [&] {
            constrained_stream.handle_any_error();
        }
    };

    auto func = Func::parse(constrained_stream);
    if (func.is_error())
        return func.error();

    return Code { static_cast<u32>(size), func.release_value() };
}

ParseResult<CodeSection> CodeSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("CodeSection");
    auto result = parse_vector<Code>(stream);
    if (result.is_error())
        return result.error();
    return CodeSection { result.release_value() };
}

ParseResult<DataSection::Data> DataSection::Data::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Data");
    u8 tag;
    stream >> tag;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);

    if (tag > 0x02)
        return with_eof_check(stream, ParseError::InvalidTag);

    if (tag == 0x00) {
        auto expr = Expression::parse(stream);
        if (expr.is_error())
            return expr.error();
        auto init = parse_vector<u8>(stream);
        if (init.is_error())
            return init.error();
        return Data { Active { init.release_value(), { 0 }, expr.release_value() } };
    }
    if (tag == 0x01) {
        auto init = parse_vector<u8>(stream);
        if (init.is_error())
            return init.error();
        return Data { Passive { init.release_value() } };
    }
    if (tag == 0x02) {
        auto index = GenericIndexParser<MemoryIndex>::parse(stream);
        if (index.is_error())
            return index.error();
        auto expr = Expression::parse(stream);
        if (expr.is_error())
            return expr.error();
        auto init = parse_vector<u8>(stream);
        if (init.is_error())
            return init.error();
        return Data { Active { init.release_value(), index.release_value(), expr.release_value() } };
    }
    VERIFY_NOT_REACHED();
}

ParseResult<DataSection> DataSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("DataSection");
    auto data = parse_vector<Data>(stream);
    if (data.is_error())
        return data.error();

    return DataSection { data.release_value() };
}

ParseResult<DataCountSection> DataCountSection::parse([[maybe_unused]] InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("DataCountSection");
    u32 value;
    if (!LEB128::read_unsigned(stream, value)) {
        if (stream.unreliable_eof()) {
            // The section simply didn't contain anything.
            return DataCountSection { {} };
        }
        return ParseError::ExpectedSize;
    }

    return DataCountSection { value };
}

ParseResult<Module> Module::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Module");
    u8 buf[4];
    if (!stream.read_or_error({ buf, 4 }))
        return with_eof_check(stream, ParseError::InvalidInput);
    if (Bytes { buf, 4 } != wasm_magic.span())
        return with_eof_check(stream, ParseError::InvalidModuleMagic);

    if (!stream.read_or_error({ buf, 4 }))
        return with_eof_check(stream, ParseError::InvalidInput);
    if (Bytes { buf, 4 } != wasm_version.span())
        return with_eof_check(stream, ParseError::InvalidModuleVersion);

    Vector<AnySection> sections;
    for (;;) {
        u8 section_id;
        stream >> section_id;
        if (stream.unreliable_eof()) {
            stream.handle_any_error();
            break;
        }
        if (stream.has_any_error())
            return with_eof_check(stream, ParseError::ExpectedIndex);

        size_t section_size;
        if (!LEB128::read_unsigned(stream, section_size))
            return with_eof_check(stream, ParseError::ExpectedSize);

        auto section_stream = ConstrainedStream { stream, section_size };
        ScopeGuard drain_errors {
            [&] {
                section_stream.handle_any_error();
            }
        };

        switch (section_id) {
        case CustomSection::section_id: {
            if (auto section = CustomSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case TypeSection::section_id: {
            if (auto section = TypeSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case ImportSection::section_id: {
            if (auto section = ImportSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case FunctionSection::section_id: {
            if (auto section = FunctionSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case TableSection::section_id: {
            if (auto section = TableSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case MemorySection::section_id: {
            if (auto section = MemorySection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case GlobalSection::section_id: {
            if (auto section = GlobalSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case ExportSection::section_id: {
            if (auto section = ExportSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case StartSection::section_id: {
            if (auto section = StartSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case ElementSection::section_id: {
            if (auto section = ElementSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case CodeSection::section_id: {
            if (auto section = CodeSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case DataSection::section_id: {
            if (auto section = DataSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        case DataCountSection::section_id: {
            if (auto section = DataCountSection::parse(section_stream); !section.is_error()) {
                sections.append(section.release_value());
                continue;
            } else {
                return section.error();
            }
        }
        default:
            return with_eof_check(stream, ParseError::InvalidIndex);
        }
    }

    return Module { move(sections) };
}

bool Module::populate_sections()
{
    auto is_ok = true;
    FunctionSection const* function_section { nullptr };
    for_each_section_of_type<FunctionSection>([&](FunctionSection const& section) { function_section = &section; });
    for_each_section_of_type<CodeSection>([&](CodeSection const& section) {
        if (!function_section) {
            is_ok = false;
            return;
        }
        size_t index = 0;
        for (auto& entry : section.functions()) {
            if (function_section->types().size() <= index) {
                is_ok = false;
                return;
            }
            auto& type_index = function_section->types()[index];
            Vector<ValueType> locals;
            for (auto& local : entry.func().locals()) {
                for (size_t i = 0; i < local.n(); ++i)
                    locals.append(local.type());
            }
            m_functions.empend(type_index, move(locals), entry.func().body());
            ++index;
        }
    });
    return is_ok;
}

String parse_error_to_string(ParseError error)
{
    switch (error) {
    case ParseError::UnexpectedEof:
        return "Unexpected end-of-file";
    case ParseError::ExpectedIndex:
        return "Expected a valid index value";
    case ParseError::ExpectedKindTag:
        return "Expected a valid kind tag";
    case ParseError::ExpectedSize:
        return "Expected a valid LEB128-encoded size";
    case ParseError::ExpectedValueOrTerminator:
        return "Expected either a terminator or a value";
    case ParseError::InvalidIndex:
        return "An index parsed was semantically invalid";
    case ParseError::InvalidInput:
        return "Input data contained invalid bytes";
    case ParseError::InvalidModuleMagic:
        return "Incorrect module magic (did not match \\0asm)";
    case ParseError::InvalidModuleVersion:
        return "Incorrect module version";
    case ParseError::InvalidSize:
        return "A parsed size did not make sense in context";
    case ParseError::InvalidTag:
        return "A parsed tag did not make sense in context";
    case ParseError::InvalidType:
        return "A parsed type did not make sense in context";
    case ParseError::NotImplemented:
        return "The parser encountered an unimplemented feature";
    case ParseError::HugeAllocationRequested:
        return "Parsing caused an attempt to allocate a very big chunk of memory, likely malformed data";
    case ParseError::OutOfMemory:
        return "The parser hit an OOM condition";
    case ParseError::ExpectedFloatingImmediate:
        return "Expected a floating point immediate";
    case ParseError::ExpectedSignedImmediate:
        return "Expected a signed integer immediate";
    case ParseError::InvalidImmediate:
        return "A parsed instruction immediate was invalid for the instruction it was used for";
    case ParseError::UnknownInstruction:
        return "A parsed instruction was not known to this parser";
    }
    return "Unknown error";
}
}
