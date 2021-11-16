/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <LibWasm/Forward.h>
#include <LibWasm/Types.h>

#if WASM_VALIDATOR_DEBUG
#    include <AK/SourceLocation.h>
#endif

namespace Wasm {

struct Context {
    Vector<FunctionType> types;
    Vector<FunctionType> functions;
    Vector<TableType> tables;
    Vector<MemoryType> memories;
    Vector<GlobalType> globals;
    Vector<ValueType> elements;
    Vector<bool> datas;
    Vector<ValueType> locals;
    Vector<ResultType> labels;
    Optional<ResultType> return_;
    AK::HashTable<FunctionIndex> references;
    size_t imported_function_count { 0 };
};

struct ValidationError : public Error {
    ValidationError(String error)
        : Error(Error::from_string_literal(error))
        , error_string(move(error))
    {
    }

    String error_string;
};

class Validator {
    AK_MAKE_NONCOPYABLE(Validator);
    AK_MAKE_NONMOVABLE(Validator);

public:
    Validator() = default;

    [[nodiscard]] Validator fork() const
    {
        return Validator { m_context };
    }

    // Module
    ErrorOr<void, ValidationError> validate(Module&);
    ErrorOr<void, ValidationError> validate(ImportSection const&);
    ErrorOr<void, ValidationError> validate(ExportSection const&);
    ErrorOr<void, ValidationError> validate(StartSection const&);
    ErrorOr<void, ValidationError> validate(DataSection const&);
    ErrorOr<void, ValidationError> validate(ElementSection const&);
    ErrorOr<void, ValidationError> validate(GlobalSection const&);
    ErrorOr<void, ValidationError> validate(MemorySection const&);
    ErrorOr<void, ValidationError> validate(TableSection const&);
    ErrorOr<void, ValidationError> validate(CodeSection const&);
    ErrorOr<void, ValidationError> validate(FunctionSection const&) { return {}; }
    ErrorOr<void, ValidationError> validate(DataCountSection const&) { return {}; }
    ErrorOr<void, ValidationError> validate(TypeSection const&) { return {}; }
    ErrorOr<void, ValidationError> validate(CustomSection const&) { return {}; }

    ErrorOr<void, ValidationError> validate(TypeIndex index) const
    {
        if (index.value() < m_context.types.size())
            return {};
        return Errors::invalid("TypeIndex"sv);
    }

    ErrorOr<void, ValidationError> validate(FunctionIndex index) const
    {
        if (index.value() < m_context.functions.size())
            return {};
        return Errors::invalid("FunctionIndex"sv);
    }

    ErrorOr<void, ValidationError> validate(MemoryIndex index) const
    {
        if (index.value() < m_context.memories.size())
            return {};
        return Errors::invalid("MemoryIndex"sv);
    }

    ErrorOr<void, ValidationError> validate(ElementIndex index) const
    {
        if (index.value() < m_context.elements.size())
            return {};
        return Errors::invalid("ElementIndex"sv);
    }

    ErrorOr<void, ValidationError> validate(DataIndex index) const
    {
        if (index.value() < m_context.datas.size())
            return {};
        return Errors::invalid("DataIndex"sv);
    }

    ErrorOr<void, ValidationError> validate(GlobalIndex index) const
    {
        if (index.value() < m_context.globals.size())
            return {};
        return Errors::invalid("GlobalIndex"sv);
    }

    ErrorOr<void, ValidationError> validate(LabelIndex index) const
    {
        if (index.value() < m_context.labels.size())
            return {};
        return Errors::invalid("LabelIndex"sv);
    }

    ErrorOr<void, ValidationError> validate(LocalIndex index) const
    {
        if (index.value() < m_context.locals.size())
            return {};
        return Errors::invalid("LocalIndex"sv);
    }

    ErrorOr<void, ValidationError> validate(TableIndex index) const
    {
        if (index.value() < m_context.tables.size())
            return {};
        return Errors::invalid("TableIndex"sv);
    }

    // Instructions
    struct StackEntry {
        StackEntry(ValueType type)
            : concrete_type(type)
            , is_known(true)
        {
        }

        explicit StackEntry()
            : concrete_type(ValueType::I32)
            , is_known(false)
        {
        }

        bool is_of_kind(ValueType::Kind kind) const
        {
            if (is_known)
                return concrete_type.kind() == kind;
            return true;
        }

        bool is_numeric() const { return !is_known || concrete_type.is_numeric(); }
        bool is_reference() const { return !is_known || concrete_type.is_reference(); }

        bool operator==(ValueType const& other) const
        {
            if (is_known)
                return concrete_type == other;
            return true;
        }

        bool operator==(StackEntry const& other) const
        {
            if (is_known && other.is_known)
                return other.concrete_type == concrete_type;
            return true;
        }

        ValueType concrete_type;
        bool is_known { true };
    };

    // This is a wrapper that can model "polymorphic" stacks,
    // by treating unknown stack entries as a potentially infinite number of entries
    class Stack : private Vector<StackEntry> {
    public:
        // The unknown entry will never be popped off, so we can safely use the original `is_empty`.
        using Vector<StackEntry>::is_empty;
        using Vector<StackEntry>::last;
        using Vector<StackEntry>::at;

        StackEntry take_last()
        {
            if (last().is_known)
                return Vector<StackEntry>::take_last();
            return last();
        }
        void append(StackEntry entry)
        {
            if (!entry.is_known)
                m_did_insert_unknown_entry = true;
            Vector<StackEntry>::append(entry);
        }

        size_t actual_size() const { return Vector<StackEntry>::size(); }
        size_t size() const { return m_did_insert_unknown_entry ? static_cast<size_t>(-1) : actual_size(); }

        Vector<StackEntry> release_vector() { return exchange(static_cast<Vector<StackEntry>&>(*this), Vector<StackEntry> {}); }

        bool operator==(Stack const& other) const;

    private:
        bool m_did_insert_unknown_entry { false };
    };

    struct ExpressionTypeResult {
        Vector<StackEntry> result_types;
        bool is_constant { false };
    };
    ErrorOr<ExpressionTypeResult, ValidationError> validate(Expression const&, Vector<ValueType> const&);
    ErrorOr<void, ValidationError> validate(Instruction const& instruction, Stack& stack, bool& is_constant);
    template<u32 opcode>
    ErrorOr<void, ValidationError> validate_instruction(Instruction const&, Stack& stack, bool& is_constant);

    // Types
    bool type_is_subtype_of(ValueType const& candidate_subtype, ValueType const& candidate_supertype);
    ErrorOr<void, ValidationError> validate(Limits const&, size_t k); // n <= 2^k-1 && m? <= 2^k-1
    ErrorOr<FunctionType, ValidationError> validate(BlockType const&);
    ErrorOr<void, ValidationError> validate(FunctionType const&) { return {}; }
    ErrorOr<void, ValidationError> validate(TableType const&);
    ErrorOr<void, ValidationError> validate(MemoryType const&);
    ErrorOr<void, ValidationError> validate(GlobalType const&) { return {}; }

private:
    explicit Validator(Context context)
        : m_context(move(context))
    {
    }

    struct Errors {
        static ValidationError invalid(StringView name) { return String::formatted("Invalid {}", name); }

        template<typename Expected, typename Given>
        static ValidationError invalid(StringView name, Expected expected, Given given)
        {
            return String::formatted("Invalid {}, expected {} but got {}", name, expected, given);
        }

        template<typename... Args>
        static ValidationError non_conforming_types(StringView name, Args... args)
        {
            return String::formatted("Non-conforming types for {}: {}", name, Vector { args... });
        }

        static ValidationError duplicate_export_name(StringView name) { return String::formatted("Duplicate exported name '{}'", name); }

        template<typename T, typename U, typename V>
        static ValidationError out_of_bounds(StringView name, V value, T min, U max) { return String::formatted("Value {} for {} is out of bounds ({},{})", value, name, min, max); }

#if WASM_VALIDATOR_DEBUG
        static ValidationError invalid_stack_state(SourceLocation location = SourceLocation::current());
#else
        static ValidationError invalid_stack_state();
#endif
    };

    enum class ChildScopeKind {
        Block,
        Loop,
        IfWithoutElse,
        IfWithElse,
        Else,
    };

    struct BlockDetails {
        size_t initial_stack_size { 0 };
        struct IfDetails {
            Stack initial_stack;
            Stack true_branch_stack;
        };
        Variant<IfDetails, Empty> details;
    };

    Context m_context;
    Vector<Context> m_parent_contexts;
    Vector<ChildScopeKind> m_entered_scopes;
    Vector<BlockDetails> m_block_details;
    Vector<FunctionType> m_entered_blocks;
};

}

template<>
struct AK::Formatter<Wasm::Validator::StackEntry> : public AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Wasm::Validator::StackEntry const& value)
    {
        if (value.is_known)
            return Formatter<StringView>::format(builder, Wasm::ValueType::kind_name(value.concrete_type.kind()));

        return Formatter<StringView>::format(builder, "<unknown>"sv);
    }
};

template<>
struct AK::Formatter<Wasm::ValueType> : public AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Wasm::ValueType const& value)
    {
        return Formatter<StringView>::format(builder, Wasm::ValueType::kind_name(value.kind()));
    }
};

template<>
struct AK::Formatter<Wasm::ValidationError> : public AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Wasm::ValidationError const& error)
    {
        return Formatter<StringView>::format(builder, error.error_string);
    }
};
