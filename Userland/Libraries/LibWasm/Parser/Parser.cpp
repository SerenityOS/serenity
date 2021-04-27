/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LEB128.h>
#include <AK/ScopeGuard.h>
#include <LibWasm/Types.h>

#if WASM_BINPARSER_DEBUG
#    define DEBUG_SPAM
#    include <AK/ScopeLogger.h>
#endif

namespace Wasm {

ParseError with_eof_check(const InputStream& stream, ParseError error_if_not_eof)
{
    if (stream.unreliable_eof())
        return ParseError::UnexpectedEof;
    return error_if_not_eof;
}

template<typename T>
static auto parse_vector(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger;
#endif
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
                if (count > 64 * KiB)
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger;
#endif
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
template<typename T, typename... Args>
static ParseResult<ParseUntilAnyOfResult<T>> parse_until_any_of(InputStream& stream, Args... terminators)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger;
#endif
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

        if ((... || (byte == terminators))) {
            result.terminator = byte;
            return result;
        }

        new_stream.unread({ &byte, 1 });
        auto parse_result = T::parse(new_stream);
        if (parse_result.is_error())
            return parse_result.error();

        result.values.append(parse_result.release_value());
    }
}

ParseResult<ValueType> ValueType::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("ValueType");
#endif
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("ResultType");
#endif
    auto types = parse_vector<ValueType>(stream);
    if (types.is_error())
        return types.error();
    return ResultType { types.release_value() };
}

ParseResult<FunctionType> FunctionType::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("FunctionType");
#endif
    u8 tag;
    stream >> tag;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);

    if (tag != Constants::function_signature_tag) {
        dbgln("Expected 0x60, but found 0x{:x}", tag);
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Limits");
#endif
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
        if (LEB128::read_unsigned(stream, value))
            return with_eof_check(stream, ParseError::ExpectedSize);
        max = value;
    }

    return Limits { static_cast<u32>(min), move(max) };
}

ParseResult<MemoryType> MemoryType::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("MemoryType");
#endif
    auto limits_result = Limits::parse(stream);
    if (limits_result.is_error())
        return limits_result.error();
    return MemoryType { limits_result.release_value() };
}

ParseResult<TableType> TableType::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("TableType");
#endif
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("GlobalType");
#endif
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("BlockType");
#endif
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

    if (index_value < 0)
        return with_eof_check(stream, ParseError::InvalidIndex);

    return BlockType { TypeIndex(index_value) };
}

ParseResult<Instruction> Instruction::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Instruction");
#endif
    u8 byte;
    stream >> byte;
    if (stream.has_any_error())
        return with_eof_check(stream, ParseError::ExpectedKindTag);
    OpCode opcode { byte };

    if (opcode == Instructions::block || opcode == Instructions::loop || opcode == Instructions::if_) {
        auto block_type = BlockType::parse(stream);
        if (block_type.is_error())
            return block_type.error();
        auto result = parse_until_any_of<Instruction>(stream, 0x0b, 0x05);
        if (result.is_error())
            return result.error();

        if (result.value().terminator == 0x0b) {
            // block/loop/if without else
            NonnullOwnPtrVector<Instruction> instructions;
            for (auto& entry : result.value().values)
                instructions.append(make<Instruction>(move(entry)));

            return Instruction { opcode, BlockAndInstructionSet { block_type.release_value(), move(instructions) } };
        }

        VERIFY(result.value().terminator == 0x05);
        NonnullOwnPtrVector<Instruction> left_instructions, right_instructions;
        for (auto& entry : result.value().values)
            left_instructions.append(make<Instruction>(move(entry)));
        // if with else
        {
            auto result = parse_until_any_of<Instruction>(stream, 0x0b);
            if (result.is_error())
                return result.error();

            for (auto& entry : result.value().values)
                right_instructions.append(make<Instruction>(move(entry)));

            return Instruction { opcode, BlockAndTwoInstructionSets { block_type.release_value(), move(left_instructions), move(right_instructions) } };
        }
    }

    // FIXME: Parse all the other instructions
    TODO();
}

ParseResult<CustomSection> CustomSection::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("CustomSection");
#endif
    auto name = parse_name(stream);
    if (name.is_error())
        return name.error();

    auto data_buffer = ByteBuffer::create_uninitialized(64);
    while (!stream.has_any_error() && !stream.unreliable_eof()) {
        char buf[16];
        auto size = stream.read({ buf, 16 });
        if (size == 0)
            break;
        data_buffer.append(buf, size);
    }

    return CustomSection(name.release_value(), move(data_buffer));
}

ParseResult<TypeSection> TypeSection::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("TypeSection");
#endif
    auto types = parse_vector<FunctionType>(stream);
    if (types.is_error())
        return types.error();
    return TypeSection { types.release_value() };
}

ParseResult<ImportSection::Import> ImportSection::Import::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Import");
#endif
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("ImportSection");
#endif
    auto imports = parse_vector<Import>(stream);
    if (imports.is_error())
        return imports.error();
    return ImportSection { imports.release_value() };
}

ParseResult<FunctionSection> FunctionSection::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("FunctionSection");
#endif
    auto indices = parse_vector<size_t>(stream);
    if (indices.is_error())
        return indices.error();

    Vector<TypeIndex> typed_indices;
    typed_indices.resize(indices.value().size());
    for (auto entry : indices.value())
        typed_indices.append(entry);

    return FunctionSection { move(typed_indices) };
}

ParseResult<TableSection::Table> TableSection::Table::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Table");
#endif
    auto type = TableType::parse(stream);
    if (type.is_error())
        return type.error();
    return Table { type.release_value() };
}

ParseResult<TableSection> TableSection::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("TableSection");
#endif
    auto tables = parse_vector<Table>(stream);
    if (tables.is_error())
        return tables.error();
    return TableSection { tables.release_value() };
}

ParseResult<MemorySection::Memory> MemorySection::Memory::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Memory");
#endif
    auto type = MemoryType::parse(stream);
    if (type.is_error())
        return type.error();
    return Memory { type.release_value() };
}

ParseResult<MemorySection> MemorySection::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("MemorySection");
#endif
    auto memorys = parse_vector<Memory>(stream);
    if (memorys.is_error())
        return memorys.error();
    return MemorySection { memorys.release_value() };
}

ParseResult<Expression> Expression::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Expression");
#endif
    auto instructions = parse_until_any_of<Instruction>(stream, 0x0b);
    if (instructions.is_error())
        return instructions.error();

    return Expression { move(instructions.value().values) };
}

ParseResult<GlobalSection::Global> GlobalSection::Global::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Global");
#endif
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("GlobalSection");
#endif
    auto result = parse_vector<Global>(stream);
    if (result.is_error())
        return result.error();
    return GlobalSection { result.release_value() };
}

ParseResult<ExportSection::Export> ExportSection::Export::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Export");
#endif
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("ExportSection");
#endif
    auto result = parse_vector<Export>(stream);
    if (result.is_error())
        return result.error();
    return ExportSection { result.release_value() };
}

ParseResult<StartSection::StartFunction> StartSection::StartFunction::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("StartFunction");
#endif
    auto index = GenericIndexParser<FunctionIndex>::parse(stream);
    if (index.is_error())
        return index.error();
    return StartFunction { index.release_value() };
}

ParseResult<StartSection> StartSection::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("StartSection");
#endif
    auto result = StartFunction::parse(stream);
    if (result.is_error())
        return result.error();
    return StartSection { result.release_value() };
}

ParseResult<ElementSection::Element> ElementSection::Element::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Element");
#endif
    auto table_index = GenericIndexParser<TableIndex>::parse(stream);
    if (table_index.is_error())
        return table_index.error();
    auto offset = Expression::parse(stream);
    if (offset.is_error())
        return offset.error();
    auto init = parse_vector<GenericIndexParser<FunctionIndex>>(stream);
    if (init.is_error())
        return init.error();

    return Element { table_index.release_value(), offset.release_value(), init.release_value() };
}

ParseResult<ElementSection> ElementSection::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("ElementSection");
#endif
    auto result = Element::parse(stream);
    if (result.is_error())
        return result.error();
    return ElementSection { result.release_value() };
}

ParseResult<Locals> Locals::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Locals");
#endif
    size_t count;
    if (!LEB128::read_unsigned(stream, count))
        return with_eof_check(stream, ParseError::InvalidSize);
    // TODO: Disallow too many entries.
    auto type = ValueType::parse(stream);
    if (type.is_error())
        return type.error();

    return Locals { static_cast<u32>(count), type.release_value() };
}

ParseResult<Func> Func::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Func");
#endif
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Code");
#endif
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("CodeSection");
#endif
    auto result = parse_vector<Code>(stream);
    if (result.is_error())
        return result.error();
    return CodeSection { result.release_value() };
}

ParseResult<DataSection::Data> DataSection::Data::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Data");
#endif
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
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("DataSection");
#endif
    auto data = parse_vector<Data>(stream);
    if (data.is_error())
        return data.error();

    return DataSection { data.release_value() };
}

ParseResult<DataCountSection> DataCountSection::parse([[maybe_unused]] InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("DataCountSection");
#endif
    // FIXME: Implement parsing optional values!
    return with_eof_check(stream, ParseError::NotImplemented);
}

ParseResult<Module> Module::parse(InputStream& stream)
{
#if WASM_BINPARSER_DEBUG
    ScopeLogger logger("Module");
#endif
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
        default:
            return with_eof_check(stream, ParseError::InvalidIndex);
        }
    }

    return Module { move(sections) };
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
    }
    return "Unknown error";
}
}
