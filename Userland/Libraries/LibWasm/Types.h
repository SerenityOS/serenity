/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Debug.h>
#include <AK/DistinctNumeric.h>
#include <AK/MemoryStream.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibWasm/Constants.h>
#include <LibWasm/Forward.h>
#include <LibWasm/Opcode.h>

namespace Wasm {

enum class ParseError {
    UnexpectedEof,
    UnknownInstruction,
    ExpectedFloatingImmediate,
    ExpectedIndex,
    ExpectedKindTag,
    ExpectedSignedImmediate,
    ExpectedSize,
    ExpectedValueOrTerminator,
    InvalidImmediate,
    InvalidIndex,
    InvalidInput,
    InvalidModuleMagic,
    InvalidModuleVersion,
    InvalidSize,
    InvalidTag,
    InvalidType,
    HugeAllocationRequested,
    OutOfMemory,
    // FIXME: This should not exist!
    NotImplemented,
};

String parse_error_to_string(ParseError);

template<typename T>
using ParseResult = Result<T, ParseError>;

TYPEDEF_DISTINCT_ORDERED_ID(size_t, TypeIndex);
TYPEDEF_DISTINCT_ORDERED_ID(size_t, FunctionIndex);
TYPEDEF_DISTINCT_ORDERED_ID(size_t, TableIndex);
TYPEDEF_DISTINCT_ORDERED_ID(size_t, ElementIndex);
TYPEDEF_DISTINCT_ORDERED_ID(size_t, MemoryIndex);
TYPEDEF_DISTINCT_ORDERED_ID(size_t, LocalIndex);
TYPEDEF_DISTINCT_ORDERED_ID(size_t, GlobalIndex);
TYPEDEF_DISTINCT_ORDERED_ID(size_t, LabelIndex);
TYPEDEF_DISTINCT_ORDERED_ID(size_t, DataIndex);
TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, true, true, false, true, false, true, InstructionPointer);

ParseError with_eof_check(InputStream const& stream, ParseError error_if_not_eof);

template<typename T>
struct GenericIndexParser {
    static ParseResult<T> parse(InputStream& stream)
    {
        size_t value;
        if (!LEB128::read_unsigned(stream, value))
            return with_eof_check(stream, ParseError::ExpectedIndex);
        return T { value };
    }
};

class ReconsumableStream : public InputStream {
public:
    explicit ReconsumableStream(InputStream& stream)
        : m_stream(stream)
    {
    }

    void unread(ReadonlyBytes data) { m_buffer.append(data.data(), data.size()); }

private:
    size_t read(Bytes bytes) override
    {
        size_t bytes_read_from_buffer = 0;
        if (!m_buffer.is_empty()) {
            auto read_size = min(bytes.size(), m_buffer.size());
            m_buffer.span().slice(0, read_size).copy_to(bytes);
            bytes = bytes.slice(read_size);
            for (size_t i = 0; i < read_size; ++i)
                m_buffer.take_first();
            bytes_read_from_buffer = read_size;
        }

        return m_stream.read(bytes) + bytes_read_from_buffer;
    }
    bool unreliable_eof() const override
    {
        return m_buffer.is_empty() && m_stream.unreliable_eof();
    }
    bool read_or_error(Bytes bytes) override
    {
        if (read(bytes))
            return true;
        set_recoverable_error();
        return false;
    }
    bool discard_or_error(size_t count) override
    {
        size_t bytes_discarded_from_buffer = 0;
        if (!m_buffer.is_empty()) {
            auto read_size = min(count, m_buffer.size());
            for (size_t i = 0; i < read_size; ++i)
                m_buffer.take_first();
            bytes_discarded_from_buffer = read_size;
        }

        return m_stream.discard_or_error(count - bytes_discarded_from_buffer);
    }

    InputStream& m_stream;
    Vector<u8, 8> m_buffer;
};

class ConstrainedStream : public InputStream {
public:
    explicit ConstrainedStream(InputStream& stream, size_t size)
        : m_stream(stream)
        , m_bytes_left(size)
    {
    }

private:
    size_t read(Bytes bytes) override
    {
        auto to_read = min(m_bytes_left, bytes.size());
        auto nread = m_stream.read(bytes.slice(0, to_read));
        m_bytes_left -= nread;
        return nread;
    }
    bool unreliable_eof() const override
    {
        return m_bytes_left == 0 || m_stream.unreliable_eof();
    }
    bool read_or_error(Bytes bytes) override
    {
        if (read(bytes))
            return true;
        set_recoverable_error();
        return false;
    }
    bool discard_or_error(size_t count) override
    {
        auto to_discard = min(m_bytes_left, count);
        if (m_stream.discard_or_error(to_discard))
            m_bytes_left -= to_discard;
        return to_discard;
    }

    InputStream& m_stream;
    size_t m_bytes_left { 0 };
};

// https://webassembly.github.io/spec/core/bikeshed/#value-types%E2%91%A2
class ValueType {
public:
    enum Kind {
        I32,
        I64,
        F32,
        F64,
        FunctionReference,
        ExternReference,
        NullFunctionReference,
        NullExternReference,
    };

    explicit ValueType(Kind kind)
        : m_kind(kind)
    {
    }

    bool operator==(ValueType const&) const = default;

    auto is_reference() const { return m_kind == ExternReference || m_kind == FunctionReference || m_kind == NullExternReference || m_kind == NullFunctionReference; }
    auto is_numeric() const { return !is_reference(); }
    auto kind() const { return m_kind; }

    static ParseResult<ValueType> parse(InputStream& stream);

    static String kind_name(Kind kind)
    {
        switch (kind) {
        case I32:
            return "i32";
        case I64:
            return "i64";
        case F32:
            return "f32";
        case F64:
            return "f64";
        case FunctionReference:
            return "funcref";
        case ExternReference:
            return "externref";
        case NullFunctionReference:
            return "ref.null externref";
        case NullExternReference:
            return "ref.null funcref";
        }
        VERIFY_NOT_REACHED();
    }

private:
    Kind m_kind;
};

// https://webassembly.github.io/spec/core/bikeshed/#result-types%E2%91%A2
class ResultType {
public:
    explicit ResultType(Vector<ValueType> types)
        : m_types(move(types))
    {
    }

    auto const& types() const { return m_types; }

    static ParseResult<ResultType> parse(InputStream& stream);

private:
    Vector<ValueType> m_types;
};

// https://webassembly.github.io/spec/core/bikeshed/#function-types%E2%91%A4
class FunctionType {
public:
    FunctionType(Vector<ValueType> parameters, Vector<ValueType> results)
        : m_parameters(move(parameters))
        , m_results(move(results))
    {
    }

    auto& parameters() const { return m_parameters; }
    auto& results() const { return m_results; }

    static ParseResult<FunctionType> parse(InputStream& stream);

private:
    Vector<ValueType> m_parameters;
    Vector<ValueType> m_results;
};

// https://webassembly.github.io/spec/core/bikeshed/#limits%E2%91%A5
class Limits {
public:
    explicit Limits(u32 min, Optional<u32> max = {})
        : m_min(min)
        , m_max(move(max))
    {
    }

    auto min() const { return m_min; }
    auto& max() const { return m_max; }

    static ParseResult<Limits> parse(InputStream& stream);

private:
    u32 m_min { 0 };
    Optional<u32> m_max;
};

// https://webassembly.github.io/spec/core/bikeshed/#memory-types%E2%91%A4
class MemoryType {
public:
    explicit MemoryType(Limits limits)
        : m_limits(move(limits))
    {
    }

    auto& limits() const { return m_limits; }

    static ParseResult<MemoryType> parse(InputStream& stream);

private:
    Limits m_limits;
};

// https://webassembly.github.io/spec/core/bikeshed/#table-types%E2%91%A4
class TableType {
public:
    explicit TableType(ValueType element_type, Limits limits)
        : m_element_type(element_type)
        , m_limits(move(limits))
    {
        VERIFY(m_element_type.is_reference());
    }

    auto& limits() const { return m_limits; }
    auto& element_type() const { return m_element_type; }

    static ParseResult<TableType> parse(InputStream& stream);

private:
    ValueType m_element_type;
    Limits m_limits;
};

// https://webassembly.github.io/spec/core/bikeshed/#global-types%E2%91%A4
class GlobalType {
public:
    GlobalType(ValueType type, bool is_mutable)
        : m_type(type)
        , m_is_mutable(is_mutable)
    {
    }

    auto& type() const { return m_type; }
    auto is_mutable() const { return m_is_mutable; }

    static ParseResult<GlobalType> parse(InputStream& stream);

private:
    ValueType m_type;
    bool m_is_mutable { false };
};

// https://webassembly.github.io/spec/core/bikeshed/#binary-blocktype
class BlockType {
public:
    enum Kind {
        Empty,
        Type,
        Index,
    };

    BlockType()
        : m_kind(Empty)
        , m_empty(0)
    {
    }

    explicit BlockType(ValueType type)
        : m_kind(Type)
        , m_value_type(type)
    {
    }

    explicit BlockType(TypeIndex index)
        : m_kind(Index)
        , m_type_index(index)
    {
    }

    auto kind() const { return m_kind; }
    auto& value_type() const
    {
        VERIFY(kind() == Type);
        return m_value_type;
    }
    auto& type_index() const
    {
        VERIFY(kind() == Index);
        return m_type_index;
    }

    static ParseResult<BlockType> parse(InputStream& stream);

private:
    Kind m_kind { Empty };
    union {
        ValueType m_value_type;
        TypeIndex m_type_index;
        u8 m_empty;
    };
};

// https://webassembly.github.io/spec/core/bikeshed/#binary-instr
// https://webassembly.github.io/spec/core/bikeshed/#reference-instructions%E2%91%A6
// https://webassembly.github.io/spec/core/bikeshed/#parametric-instructions%E2%91%A6
// https://webassembly.github.io/spec/core/bikeshed/#variable-instructions%E2%91%A6
// https://webassembly.github.io/spec/core/bikeshed/#table-instructions%E2%91%A6
// https://webassembly.github.io/spec/core/bikeshed/#memory-instructions%E2%91%A6
// https://webassembly.github.io/spec/core/bikeshed/#numeric-instructions%E2%91%A6
class Instruction {
public:
    explicit Instruction(OpCode opcode)
        : m_opcode(opcode)
        , m_arguments(static_cast<u8>(0))
    {
    }

    struct TableElementArgs {
        ElementIndex element_index;
        TableIndex table_index;
    };

    struct TableTableArgs {
        TableIndex lhs;
        TableIndex rhs;
    };

    struct StructuredInstructionArgs {
        BlockType block_type;
        InstructionPointer end_ip;
        Optional<InstructionPointer> else_ip;
    };

    struct TableBranchArgs {
        Vector<LabelIndex> labels;
        LabelIndex default_;
    };

    struct IndirectCallArgs {
        TypeIndex type;
        TableIndex table;
    };

    struct MemoryArgument {
        u32 align;
        u32 offset;
    };

    template<typename T>
    explicit Instruction(OpCode opcode, T argument)
        : m_opcode(opcode)
        , m_arguments(move(argument))
    {
    }

    static ParseResult<Vector<Instruction>> parse(InputStream& stream, InstructionPointer& ip);

    auto& opcode() const { return m_opcode; }
    auto& arguments() const { return m_arguments; }

private:
    OpCode m_opcode { 0 };
    // clang-format off
    Variant<
        BlockType,
        DataIndex,
        ElementIndex,
        FunctionIndex,
        GlobalIndex,
        IndirectCallArgs,
        LabelIndex,
        LocalIndex,
        MemoryArgument,
        StructuredInstructionArgs,
        TableBranchArgs,
        TableElementArgs,
        TableIndex,
        TableTableArgs,
        ValueType,
        Vector<ValueType>,
        double,
        float,
        i32,
        i64,
        u8 // Empty state
    > m_arguments;
    // clang-format on
};

class CustomSection {
public:
    static constexpr u8 section_id = 0;

    CustomSection(String name, ByteBuffer contents)
        : m_name(move(name))
        , m_contents(move(contents))
    {
    }

    auto& name() const { return m_name; }
    auto& contents() const { return m_contents; }

    static ParseResult<CustomSection> parse(InputStream& stream);

private:
    String m_name;
    ByteBuffer m_contents;
};

class TypeSection {
public:
    static constexpr u8 section_id = 1;

    explicit TypeSection(Vector<FunctionType> types)
        : m_types(move(types))
    {
    }

    auto& types() const { return m_types; }

    static ParseResult<TypeSection> parse(InputStream& stream);

private:
    Vector<FunctionType> m_types;
};

class ImportSection {
public:
    class Import {
    public:
        using ImportDesc = Variant<TypeIndex, TableType, MemoryType, GlobalType, FunctionType>;
        Import(String module, String name, ImportDesc description)
            : m_module(move(module))
            , m_name(move(name))
            , m_description(move(description))
        {
        }

        auto& module() const { return m_module; }
        auto& name() const { return m_name; }
        auto& description() const { return m_description; }

        static ParseResult<Import> parse(InputStream& stream);

    private:
        template<typename T>
        static ParseResult<Import> parse_with_type(auto&& stream, auto&& module, auto&& name)
        {
            auto result = T::parse(stream);
            if (result.is_error())
                return result.error();
            return Import { module.release_value(), name.release_value(), result.release_value() };
        };

        String m_module;
        String m_name;
        ImportDesc m_description;
    };

public:
    static constexpr u8 section_id = 2;

    explicit ImportSection(Vector<Import> imports)
        : m_imports(move(imports))
    {
    }

    auto& imports() const { return m_imports; }

    static ParseResult<ImportSection> parse(InputStream& stream);

private:
    Vector<Import> m_imports;
};

class FunctionSection {
public:
    static constexpr u8 section_id = 3;

    explicit FunctionSection(Vector<TypeIndex> types)
        : m_types(move(types))
    {
    }

    auto& types() const { return m_types; }

    static ParseResult<FunctionSection> parse(InputStream& stream);

private:
    Vector<TypeIndex> m_types;
};

class TableSection {
public:
    class Table {
    public:
        explicit Table(TableType type)
            : m_type(move(type))
        {
        }

        auto& type() const { return m_type; }

        static ParseResult<Table> parse(InputStream& stream);

    private:
        TableType m_type;
    };

public:
    static constexpr u8 section_id = 4;

    explicit TableSection(Vector<Table> tables)
        : m_tables(move(tables))
    {
    }

    auto& tables() const { return m_tables; };

    static ParseResult<TableSection> parse(InputStream& stream);

private:
    Vector<Table> m_tables;
};

class MemorySection {
public:
    class Memory {
    public:
        explicit Memory(MemoryType type)
            : m_type(move(type))
        {
        }

        auto& type() const { return m_type; }

        static ParseResult<Memory> parse(InputStream& stream);

    private:
        MemoryType m_type;
    };

public:
    static constexpr u8 section_id = 5;

    explicit MemorySection(Vector<Memory> memories)
        : m_memories(move(memories))
    {
    }

    auto& memories() const { return m_memories; }

    static ParseResult<MemorySection> parse(InputStream& stream);

private:
    Vector<Memory> m_memories;
};

class Expression {
public:
    explicit Expression(Vector<Instruction> instructions)
        : m_instructions(move(instructions))
    {
    }

    auto& instructions() const { return m_instructions; }

    static ParseResult<Expression> parse(InputStream& stream);

private:
    Vector<Instruction> m_instructions;
};

class GlobalSection {
public:
    class Global {
    public:
        explicit Global(GlobalType type, Expression expression)
            : m_type(move(type))
            , m_expression(move(expression))
        {
        }

        auto& type() const { return m_type; }
        auto& expression() const { return m_expression; }

        static ParseResult<Global> parse(InputStream& stream);

    private:
        GlobalType m_type;
        Expression m_expression;
    };

public:
    static constexpr u8 section_id = 6;

    explicit GlobalSection(Vector<Global> entries)
        : m_entries(move(entries))
    {
    }

    auto& entries() const { return m_entries; }

    static ParseResult<GlobalSection> parse(InputStream& stream);

private:
    Vector<Global> m_entries;
};

class ExportSection {
private:
    using ExportDesc = Variant<FunctionIndex, TableIndex, MemoryIndex, GlobalIndex>;

public:
    class Export {
    public:
        explicit Export(String name, ExportDesc description)
            : m_name(move(name))
            , m_description(move(description))
        {
        }

        auto& name() const { return m_name; }
        auto& description() const { return m_description; }

        static ParseResult<Export> parse(InputStream& stream);

    private:
        String m_name;
        ExportDesc m_description;
    };

    static constexpr u8 section_id = 7;

    explicit ExportSection(Vector<Export> entries)
        : m_entries(move(entries))
    {
    }

    auto& entries() const { return m_entries; }

    static ParseResult<ExportSection> parse(InputStream& stream);

private:
    Vector<Export> m_entries;
};

class StartSection {
public:
    class StartFunction {
    public:
        explicit StartFunction(FunctionIndex index)
            : m_index(index)
        {
        }

        auto& index() const { return m_index; }

        static ParseResult<StartFunction> parse(InputStream& stream);

    private:
        FunctionIndex m_index;
    };

    static constexpr u8 section_id = 8;

    explicit StartSection(StartFunction func)
        : m_function(move(func))
    {
    }

    auto& function() const { return m_function; }

    static ParseResult<StartSection> parse(InputStream& stream);

private:
    StartFunction m_function;
};

class ElementSection {
public:
    struct Active {
        TableIndex index;
        Expression expression;
    };
    struct Declarative {
    };
    struct Passive {
    };

    struct SegmentType0 {
        // FIXME: Implement me!
        static ParseResult<SegmentType0> parse(InputStream& stream);

        Vector<FunctionIndex> function_indices;
        Active mode;
    };
    struct SegmentType1 {
        static ParseResult<SegmentType1> parse(InputStream& stream);

        Vector<FunctionIndex> function_indices;
    };
    struct SegmentType2 {
        // FIXME: Implement me!
        static ParseResult<SegmentType2> parse(InputStream& stream);
    };
    struct SegmentType3 {
        // FIXME: Implement me!
        static ParseResult<SegmentType3> parse(InputStream& stream);
    };
    struct SegmentType4 {
        // FIXME: Implement me!
        static ParseResult<SegmentType4> parse(InputStream& stream);
    };
    struct SegmentType5 {
        // FIXME: Implement me!
        static ParseResult<SegmentType5> parse(InputStream& stream);
    };
    struct SegmentType6 {
        // FIXME: Implement me!
        static ParseResult<SegmentType6> parse(InputStream& stream);
    };
    struct SegmentType7 {
        // FIXME: Implement me!
        static ParseResult<SegmentType7> parse(InputStream& stream);
    };

    struct Element {
        static ParseResult<Element> parse(InputStream&);

        ValueType type;
        Vector<Expression> init;
        Variant<Active, Passive, Declarative> mode;
    };

    static constexpr u8 section_id = 9;

    explicit ElementSection(Vector<Element> segs)
        : m_segments(move(segs))
    {
    }

    auto& segments() const { return m_segments; }

    static ParseResult<ElementSection> parse(InputStream& stream);

private:
    Vector<Element> m_segments;
};

class Locals {
public:
    explicit Locals(u32 n, ValueType type)
        : m_n(n)
        , m_type(type)
    {
    }

    // Yikes...
    auto n() const { return m_n; }
    auto& type() const { return m_type; }

    static ParseResult<Locals> parse(InputStream& stream);

private:
    u32 m_n { 0 };
    ValueType m_type;
};

class CodeSection {
public:
    // https://webassembly.github.io/spec/core/bikeshed/#binary-func
    class Func {
    public:
        explicit Func(Vector<Locals> locals, Expression body)
            : m_locals(move(locals))
            , m_body(move(body))
        {
        }

        auto& locals() const { return m_locals; }
        auto& body() const { return m_body; }

        static ParseResult<Func> parse(InputStream& stream);

    private:
        Vector<Locals> m_locals;
        Expression m_body;
    };
    class Code {
    public:
        explicit Code(u32 size, Func func)
            : m_size(size)
            , m_func(move(func))
        {
        }

        auto size() const { return m_size; }
        auto& func() const { return m_func; }

        static ParseResult<Code> parse(InputStream& stream);

    private:
        u32 m_size { 0 };
        Func m_func;
    };

    static constexpr u8 section_id = 10;

    explicit CodeSection(Vector<Code> funcs)
        : m_functions(move(funcs))
    {
    }

    auto& functions() const { return m_functions; }

    static ParseResult<CodeSection> parse(InputStream& stream);

private:
    Vector<Code> m_functions;
};

class DataSection {
public:
    class Data {
    public:
        struct Passive {
            Vector<u8> init;
        };
        struct Active {
            Vector<u8> init;
            MemoryIndex index;
            Expression offset;
        };
        using Value = Variant<Passive, Active>;

        explicit Data(Value value)
            : m_value(move(value))
        {
        }

        auto& value() const { return m_value; }

        static ParseResult<Data> parse(InputStream& stream);

    private:
        Value m_value;
    };

    static constexpr u8 section_id = 11;

    explicit DataSection(Vector<Data> data)
        : m_data(move(data))
    {
    }

    auto& data() const { return m_data; }

    static ParseResult<DataSection> parse(InputStream& stream);

private:
    Vector<Data> m_data;
};

class DataCountSection {
public:
    static constexpr u8 section_id = 12;

    explicit DataCountSection(Optional<u32> count)
        : m_count(move(count))
    {
    }

    auto& count() const { return m_count; }

    static ParseResult<DataCountSection> parse(InputStream& stream);

private:
    Optional<u32> m_count;
};

class Module {
public:
    enum class ValidationStatus {
        Unchecked,
        Invalid,
        Valid,
    };

    class Function {
    public:
        explicit Function(TypeIndex type, Vector<ValueType> local_types, Expression body)
            : m_type(type)
            , m_local_types(move(local_types))
            , m_body(move(body))
        {
        }

        auto& type() const { return m_type; }
        auto& locals() const { return m_local_types; }
        auto& body() const { return m_body; }

    private:
        TypeIndex m_type;
        Vector<ValueType> m_local_types;
        Expression m_body;
    };

    using AnySection = Variant<
        CustomSection,
        TypeSection,
        ImportSection,
        FunctionSection,
        TableSection,
        MemorySection,
        GlobalSection,
        ExportSection,
        StartSection,
        ElementSection,
        CodeSection,
        DataSection,
        DataCountSection>;

    static constexpr Array<u8, 4> wasm_magic { 0, 'a', 's', 'm' };
    static constexpr Array<u8, 4> wasm_version { 1, 0, 0, 0 };

    explicit Module(Vector<AnySection> sections)
        : m_sections(move(sections))
    {
        if (!populate_sections()) {
            m_validation_status = ValidationStatus::Invalid;
            m_validation_error = "Failed to populate module sections"sv;
        }
    }

    auto& sections() const { return m_sections; }
    auto& functions() const { return m_functions; }
    auto& type(TypeIndex index) const
    {
        FunctionType const* type = nullptr;
        for_each_section_of_type<TypeSection>([&](TypeSection const& section) {
            type = &section.types().at(index.value());
        });

        VERIFY(type != nullptr);
        return *type;
    }

    template<typename T, typename Callback>
    void for_each_section_of_type(Callback&& callback) const
    {
        for (auto& section : m_sections) {
            if (auto ptr = section.get_pointer<T>())
                callback(*ptr);
        }
    }
    template<typename T, typename Callback>
    void for_each_section_of_type(Callback&& callback)
    {
        for (auto& section : m_sections) {
            if (auto ptr = section.get_pointer<T>())
                callback(*ptr);
        }
    }

    void set_validation_status(ValidationStatus status, Badge<Validator>) { set_validation_status(status); }
    ValidationStatus validation_status() const { return m_validation_status; }
    StringView validation_error() const { return *m_validation_error; }
    void set_validation_error(String error) { m_validation_error = move(error); }

    static ParseResult<Module> parse(InputStream& stream);

private:
    bool populate_sections();
    void set_validation_status(ValidationStatus status) { m_validation_status = status; }

    Vector<AnySection> m_sections;
    Vector<Function> m_functions;
    ValidationStatus m_validation_status { ValidationStatus::Unchecked };
    Optional<String> m_validation_error;
};
}
