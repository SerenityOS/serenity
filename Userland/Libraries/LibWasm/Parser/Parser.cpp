/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LEB128.h>
#include <AK/ScopeLogger.h>
#include <LibWasm/Types.h>

namespace Wasm {

template<typename T>
static ParseResult<Vector<T>> parse_vector(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger;
    size_t count;
    if (!LEB128::read_unsigned(stream, count))
        return ParseError::InvalidInput;

    Vector<T> entries;
    for (size_t i = 0; i < count; ++i) {
        if constexpr (IsSame<T, size_t>) {
            size_t value;
            if (!LEB128::read_unsigned(stream, value))
                return ParseError::InvalidInput;
            entries.append(value);
        } else if constexpr (IsSame<T, ssize_t>) {
            ssize_t value;
            if (!LEB128::read_signed(stream, value))
                return ParseError::InvalidInput;
            entries.append(value);
        } else if constexpr (IsSame<T, u8>) {
            entries.resize(count);
            if (!stream.read_or_error({ entries.data(), entries.size() }))
                return ParseError::InvalidInput;
            break; // Note: We read this all in one go!
        } else {
            auto result = T::parse(stream);
            if (result.is_error())
                return result.error();
            entries.append(result.release_value());
        }
    }

    return entries;
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
template<typename T, typename... Args>
static ParseResult<ParseUntilAnyOfResult<T>> parse_until_any_of(InputStream& stream, Args... terminators) requires(requires(InputStream& stream) { T::parse(stream); })
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger;
    ReconsumableStream new_stream { stream };
    ParseUntilAnyOfResult<T> result;
    for (;;) {
        u8 byte;
        new_stream >> byte;
        if (new_stream.has_any_error())
            return ParseError::InvalidInput;

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
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("ValueType");
    u8 tag;
    stream >> tag;
    if (stream.has_any_error())
        return ParseError::InvalidInput;
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
        return ParseError::InvalidInput;
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
        return ParseError::InvalidInput;

    if (tag != Constants::function_signature_tag) {
        dbgln("Expected 0x60, but found 0x{:x}", tag);
        return ParseError::InvalidInput;
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
        return ParseError::InvalidInput;

    if (flag > 1)
        return ParseError::InvalidInput;

    size_t min;
    if (!LEB128::read_unsigned(stream, min))
        return ParseError::InvalidInput;

    Optional<u32> max;
    if (flag) {
        size_t value;
        if (LEB128::read_unsigned(stream, value))
            return ParseError::InvalidInput;
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
        return ParseError::InvalidInput;
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
        return ParseError::InvalidInput;

    if (mutable_ > 1)
        return ParseError::InvalidInput;

    return GlobalType { type_result.release_value(), mutable_ == 0x01 };
}

ParseResult<BlockType> BlockType::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("BlockType");
    u8 kind;
    stream >> kind;
    if (stream.has_any_error())
        return ParseError::InvalidInput;
    if (kind == Constants::empty_block_tag)
        return BlockType {};

    {
        InputMemoryStream value_stream { ReadonlyBytes { &kind, 1 } };
        if (auto value_type = ValueType::parse(value_stream); !value_type.is_error())
            return BlockType { value_type.release_value() };
    }

    ReconsumableStream new_stream { stream };
    new_stream.unread({ &kind, 1 });

    ssize_t index_value;
    if (!LEB128::read_signed(new_stream, index_value))
        return ParseError::InvalidInput;

    if (index_value < 0)
        return ParseError::InvalidInput;

    return BlockType { TypeIndex(index_value) };
}

ParseResult<Instruction> Instruction::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Instruction");
    u8 byte;
    stream >> byte;
    if (stream.has_any_error())
        return ParseError::InvalidInput;
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
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("CustomSection");
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
        return ParseError::InvalidInput;

    switch (tag) {
    case Constants::extern_function_tag: {
        size_t index;
        if (!LEB128::read_unsigned(stream, index))
            return ParseError::InvalidInput;
        return Import { module.release_value(), name.release_value(), TypeIndex { index } };
    }
    case Constants::extern_table_tag:
        return parse_with_type<TableType>(stream, module, name);
    case Constants::extern_memory_tag:
        return parse_with_type<MemoryType>(stream, module, name);
    case Constants::extern_global_tag:
        return parse_with_type<GlobalType>(stream, module, name);
    default:
        return ParseError::InvalidInput;
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
    typed_indices.resize(indices.value().size());
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
    auto memorys = parse_vector<Memory>(stream);
    if (memorys.is_error())
        return memorys.error();
    return MemorySection { memorys.release_value() };
}

ParseResult<Expression> Expression::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Expression");
    auto instructions = parse_until_any_of<Instruction>(stream, 0x0b);
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
        return ParseError::InvalidInput;

    size_t index;
    if (!LEB128::read_unsigned(stream, index))
        return ParseError::InvalidInput;

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
        return ParseError::InvalidInput;
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
    size_t index;
    if (!LEB128::read_unsigned(stream, index))
        return ParseError::InvalidInput;
    return StartFunction { FunctionIndex { index } };
}

ParseResult<StartSection> StartSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("StartSection");
    auto result = StartFunction::parse(stream);
    if (result.is_error())
        return result.error();
    return StartSection { result.release_value() };
}

ParseResult<ElementSection::Element> ElementSection::Element::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Element");
    size_t table_index;
    if (!LEB128::read_unsigned(stream, table_index))
        return ParseError::InvalidInput;
    auto offset = Expression::parse(stream);
    if (offset.is_error())
        return offset.error();
    auto init = parse_vector<size_t>(stream);
    if (init.is_error())
        return init.error();

    Vector<FunctionIndex> typed_init;
    typed_init.ensure_capacity(init.value().size());
    for (auto entry : init.value())
        typed_init.unchecked_append(entry);

    return Element { TableIndex { table_index }, offset.release_value(), move(typed_init) };
}

ParseResult<ElementSection> ElementSection::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("ElementSection");
    auto result = Element::parse(stream);
    if (result.is_error())
        return result.error();
    return ElementSection { result.release_value() };
}

ParseResult<Locals> Locals::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Locals");
    size_t count;
    if (!LEB128::read_unsigned(stream, count))
        return ParseError::InvalidInput;
    // TODO: Disallow too many entries.
    auto type = ValueType::parse(stream);
    if (type.is_error())
        return type.error();

    return Locals { static_cast<u32>(count), type.release_value() };
}

ParseResult<Func> Func::parse(InputStream& stream)
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
        return ParseError::InvalidInput;

    auto constrained_stream = ConstrainedStream { stream, size };
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
        return ParseError::InvalidInput;

    if (tag > 0x02)
        return ParseError::InvalidInput;

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
        size_t index;
        stream >> index;
        if (stream.has_any_error())
            return ParseError::InvalidInput;
        auto expr = Expression::parse(stream);
        if (expr.is_error())
            return expr.error();
        auto init = parse_vector<u8>(stream);
        if (init.is_error())
            return init.error();
        return Data { Active { init.release_value(), { index }, expr.release_value() } };
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
    // FIXME: Implement parsing optional values!
    return ParseError::InvalidInput;
}

ParseResult<Module> Module::parse(InputStream& stream)
{
    ScopeLogger<WASM_BINPARSER_DEBUG> logger("Module");
    u8 buf[4];
    if (!stream.read_or_error({ buf, 4 }))
        return ParseError::InvalidInput;
    if (Bytes { buf, 4 } != wasm_magic.span())
        return ParseError::InvalidInput;

    if (!stream.read_or_error({ buf, 4 }))
        return ParseError::InvalidInput;
    if (Bytes { buf, 4 } != wasm_version.span())
        return ParseError::InvalidInput;

    Vector<AnySection> sections;
    for (;;) {
        u8 section_id;
        stream >> section_id;
        if (stream.unreliable_eof()) {
            stream.handle_any_error();
            break;
        }
        if (stream.has_any_error())
            return ParseError::InvalidInput;

        size_t section_size;
        if (!LEB128::read_unsigned(stream, section_size))
            return ParseError::InvalidInput;

        auto section_stream = ConstrainedStream { stream, section_size };

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
            return ParseError::InvalidInput;
        }
    }

    return Module { move(sections) };
}

}
