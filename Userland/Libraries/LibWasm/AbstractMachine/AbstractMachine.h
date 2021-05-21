/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/OwnPtr.h>
#include <AK/Result.h>
#include <LibWasm/Types.h>

namespace Wasm {

class Configuration;
struct Interpreter;

struct InstantiationError {
    String error { "Unknown error" };
};
struct LinkError {
    enum OtherErrors {
        InvalidImportedModule,
    };
    Vector<String> missing_imports;
    Vector<OtherErrors> other_errors;
};

TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, true, true, false, false, false, true, FunctionAddress);
TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, true, true, false, false, false, true, ExternAddress);
TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, true, true, false, false, false, true, TableAddress);
TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, true, true, false, false, false, true, GlobalAddress);
TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, true, true, false, false, false, true, MemoryAddress);

// FIXME: These should probably be made generic/virtual if/when we decide to do something more
//        fancy than just a dumb interpreter.
class Value {
public:
    using AnyValueType = Variant<i32, i64, float, double, FunctionAddress, ExternAddress>;
    explicit Value(AnyValueType value)
        : m_value(move(value))
        , m_type(ValueType::I32)
    {
        if (m_value.has<i32>())
            m_type = ValueType { ValueType::I32 };
        else if (m_value.has<i64>())
            m_type = ValueType { ValueType::I64 };
        else if (m_value.has<float>())
            m_type = ValueType { ValueType::F32 };
        else if (m_value.has<double>())
            m_type = ValueType { ValueType::F64 };
        else if (m_value.has<FunctionAddress>())
            m_type = ValueType { ValueType::FunctionReference };
        else if (m_value.has<ExternAddress>())
            m_type = ValueType { ValueType::ExternReference };
        else
            VERIFY_NOT_REACHED();
    }

    template<typename T>
    requires(sizeof(T) == sizeof(u64)) explicit Value(ValueType type, T raw_value)
        : m_value(0)
        , m_type(type)
    {
        switch (type.kind()) {
        case ValueType::Kind::ExternReference:
            m_value = ExternAddress { bit_cast<u64>(raw_value) };
            break;
        case ValueType::Kind::FunctionReference:
            m_value = FunctionAddress { bit_cast<u64>(raw_value) };
            break;
        case ValueType::Kind::I32:
            m_value = static_cast<i32>(bit_cast<i64>(raw_value));
            break;
        case ValueType::Kind::I64:
            m_value = static_cast<i64>(bit_cast<u64>(raw_value));
            break;
        case ValueType::Kind::F32:
            m_value = static_cast<float>(bit_cast<double>(raw_value));
            break;
        case ValueType::Kind::F64:
            m_value = bit_cast<double>(raw_value);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    Value(const Value& value)
        : m_value(AnyValueType { value.m_value })
        , m_type(value.m_type)
    {
    }

    Value(Value&& value)
        : m_value(move(value.m_value))
        , m_type(move(value.m_type))
    {
    }

    Value& operator=(Value&& value)
    {
        m_value = move(value.m_value);
        m_type = move(value.m_type);
        return *this;
    }

    template<typename T>
    Optional<T> to()
    {
        Optional<T> result;
        m_value.visit(
            [&](auto value) {
                if constexpr (IsSame<T, decltype(value)>)
                    result = value;
                else if constexpr (!IsFloatingPoint<T> && IsSame<decltype(value), MakeSigned<T>>)
                    result = value;
            },
            [&](const FunctionAddress& address) {
                if constexpr (IsSame<T, FunctionAddress>)
                    result = address;
            },
            [&](const ExternAddress& address) {
                if constexpr (IsSame<T, ExternAddress>)
                    result = address;
            });
        return result;
    }

    auto& type() const { return m_type; }
    auto& value() const { return m_value; }

private:
    AnyValueType m_value;
    ValueType m_type;
};

struct Trap {
    // Empty value type
};

class Result {
public:
    explicit Result(Vector<Value> values)
        : m_values(move(values))
    {
    }

    Result(Trap)
        : m_is_trap(true)
    {
    }

    auto& values() const { return m_values; }
    auto& values() { return m_values; }
    auto is_trap() const { return m_is_trap; }

private:
    Vector<Value> m_values;
    bool m_is_trap { false };
};

using ExternValue = Variant<FunctionAddress, TableAddress, MemoryAddress, GlobalAddress>;

class ExportInstance {
public:
    explicit ExportInstance(String name, ExternValue value)
        : m_name(move(name))
        , m_value(move(value))
    {
    }

    auto& name() const { return m_name; }
    auto& value() const { return m_value; }

private:
    String m_name;
    ExternValue m_value;
};

class ModuleInstance {
public:
    explicit ModuleInstance(
        Vector<FunctionType> types, Vector<FunctionAddress> function_addresses, Vector<TableAddress> table_addresses,
        Vector<MemoryAddress> memory_addresses, Vector<GlobalAddress> global_addresses, Vector<ExportInstance> exports)
        : m_types(move(types))
        , m_functions(move(function_addresses))
        , m_tables(move(table_addresses))
        , m_memories(move(memory_addresses))
        , m_globals(move(global_addresses))
        , m_exports(move(exports))
    {
    }

    ModuleInstance() = default;

    auto& types() const { return m_types; }
    auto& functions() const { return m_functions; }
    auto& tables() const { return m_tables; }
    auto& memories() const { return m_memories; }
    auto& globals() const { return m_globals; }
    auto& exports() const { return m_exports; }

    auto& types() { return m_types; }
    auto& functions() { return m_functions; }
    auto& tables() { return m_tables; }
    auto& memories() { return m_memories; }
    auto& globals() { return m_globals; }
    auto& exports() { return m_exports; }

private:
    Vector<FunctionType> m_types;
    Vector<FunctionAddress> m_functions;
    Vector<TableAddress> m_tables;
    Vector<MemoryAddress> m_memories;
    Vector<GlobalAddress> m_globals;
    Vector<ExportInstance> m_exports;
};

class WasmFunction {
public:
    explicit WasmFunction(const FunctionType& type, const ModuleInstance& module, const Module::Function& code)
        : m_type(type)
        , m_module(module)
        , m_code(code)
    {
    }

    auto& type() const { return m_type; }
    auto& module() const { return m_module; }
    auto& code() const { return m_code; }

private:
    FunctionType m_type;
    const ModuleInstance& m_module;
    const Module::Function& m_code;
};

class HostFunction {
public:
    explicit HostFunction(AK::Function<Result(Configuration&, Vector<Value>&)> function, const FunctionType& type)
        : m_function(move(function))
        , m_type(type)
    {
    }

    auto& function() { return m_function; }
    auto& type() const { return m_type; }

private:
    AK::Function<Result(Configuration&, Vector<Value>&)> m_function;
    FunctionType m_type;
};

using FunctionInstance = Variant<WasmFunction, HostFunction>;

class Reference {
public:
    struct Null {
        ValueType type;
    };
    struct Func {
        FunctionAddress address;
    };
    struct Extern {
        ExternAddress address;
    };

    using RefType = Variant<Null, Func, Extern>;
    explicit Reference(RefType ref)
        : m_ref(move(ref))
    {
    }

    auto& ref() const { return m_ref; }

private:
    RefType m_ref;
};

class TableInstance {
public:
    explicit TableInstance(const TableType& type, Vector<Optional<Reference>> elements)
        : m_elements(move(elements))
        , m_type(type)
    {
    }

    auto& elements() const { return m_elements; }
    auto& elements() { return m_elements; }
    auto& type() const { return m_type; }

private:
    Vector<Optional<Reference>> m_elements;
    const TableType& m_type;
};

class MemoryInstance {
public:
    explicit MemoryInstance(const MemoryType& type)
        : m_type(type)
    {
        grow(m_type.limits().min() * Constants::page_size);
    }

    auto& type() const { return m_type; }
    auto size() const { return m_size; }
    auto& data() const { return m_data; }
    auto& data() { return m_data; }

    bool grow(size_t size_to_grow)
    {
        if (size_to_grow == 0)
            return true;
        auto new_size = m_data.size() + size_to_grow;
        if (m_type.limits().max().value_or(new_size) < new_size)
            return false;
        auto previous_size = m_size;
        m_data.grow(new_size);
        m_size = new_size;
        // The spec requires that we zero out everything on grow
        __builtin_memset(m_data.offset_pointer(previous_size), 0, size_to_grow);
        return true;
    }

private:
    const MemoryType& m_type;
    size_t m_size { 0 };
    ByteBuffer m_data;
};

class GlobalInstance {
public:
    explicit GlobalInstance(Value value, bool is_mutable)
        : m_mutable(is_mutable)
        , m_value(move(value))
    {
    }

    auto is_mutable() const { return m_mutable; }
    auto& value() const { return m_value; }
    void set_value(Value value)
    {
        VERIFY(is_mutable());
        m_value = move(value);
    }

private:
    bool m_mutable { false };
    Value m_value;
};

class Store {
public:
    Store() = default;

    Optional<FunctionAddress> allocate(ModuleInstance& module, const Module::Function& function);
    Optional<FunctionAddress> allocate(HostFunction&&);
    Optional<TableAddress> allocate(const TableType&);
    Optional<MemoryAddress> allocate(const MemoryType&);
    Optional<GlobalAddress> allocate(const GlobalType&, Value);

    FunctionInstance* get(FunctionAddress);
    TableInstance* get(TableAddress);
    MemoryInstance* get(MemoryAddress);
    GlobalInstance* get(GlobalAddress);

private:
    Vector<FunctionInstance> m_functions;
    Vector<TableInstance> m_tables;
    Vector<MemoryInstance> m_memories;
    Vector<GlobalInstance> m_globals;
};

class Label {
public:
    explicit Label(size_t arity, InstructionPointer continuation)
        : m_arity(arity)
        , m_continuation(continuation)
    {
    }

    auto continuation() const { return m_continuation; }
    auto arity() const { return m_arity; }

private:
    size_t m_arity { 0 };
    InstructionPointer m_continuation;
};

class Frame {
    AK_MAKE_NONCOPYABLE(Frame);

public:
    explicit Frame(const ModuleInstance& module, Vector<Value> locals, const Expression& expression, size_t arity)
        : m_module(module)
        , m_locals(move(locals))
        , m_expression(expression)
        , m_arity(arity)
    {
    }

    auto& module() const { return m_module; }
    auto& locals() const { return m_locals; }
    auto& locals() { return m_locals; }
    auto& expression() const { return m_expression; }
    auto arity() const { return m_arity; }

private:
    const ModuleInstance& m_module;
    Vector<Value> m_locals;
    const Expression& m_expression;
    size_t m_arity { 0 };
};

class Stack {
public:
    using EntryType = Variant<NonnullOwnPtr<Value>, NonnullOwnPtr<Label>, NonnullOwnPtr<Frame>>;
    Stack() = default;

    [[nodiscard]] bool is_empty() const { return m_data.is_empty(); }
    void push(EntryType entry) { m_data.append(move(entry)); }
    auto pop() { return m_data.take_last(); }
    auto& peek() const { return m_data.last(); }

    auto size() const { return m_data.size(); }
    auto& entries() const { return m_data; }

private:
    Vector<EntryType> m_data;
};

using InstantiationResult = AK::Result<NonnullOwnPtr<ModuleInstance>, InstantiationError>;

class AbstractMachine {
public:
    explicit AbstractMachine() = default;

    // Load and instantiate a module, and link it into this interpreter.
    InstantiationResult instantiate(const Module&, Vector<ExternValue>);
    Result invoke(FunctionAddress, Vector<Value>);

    auto& store() const { return m_store; }
    auto& store() { return m_store; }

    Function<bool(Configuration&, InstructionPointer&, const Instruction&)> pre_interpret_hook;
    Function<bool(Configuration&, InstructionPointer&, const Instruction&, const Interpreter&)> post_interpret_hook;

private:
    Optional<InstantiationError> allocate_all(const Module&, ModuleInstance&, Vector<ExternValue>&, Vector<Value>& global_values);
    Store m_store;
};

class Linker {
public:
    struct Name {
        String module;
        String name;
        ImportSection::Import::ImportDesc type;
    };

    explicit Linker(const Module& module)
        : m_module(module)
    {
    }

    // Link a module, the import 'module name' is ignored with this.
    void link(const ModuleInstance&);

    // Link a bunch of qualified values, also matches 'module name'.
    void link(const HashMap<Name, ExternValue>&);

    auto& unresolved_imports()
    {
        populate();
        return m_unresolved_imports;
    }

    AK::Result<Vector<ExternValue>, LinkError> finish();

private:
    void populate();

    const Module& m_module;
    HashMap<Name, ExternValue> m_resolved_imports;
    HashTable<Name> m_unresolved_imports;
    Vector<Name> m_ordered_imports;
    Optional<LinkError> m_error;
};

}

template<>
struct AK::Traits<Wasm::Linker::Name> : public AK::GenericTraits<Wasm::Linker::Name> {
    static constexpr bool is_trivial() { return false; }
    static unsigned hash(const Wasm::Linker::Name& entry) { return pair_int_hash(entry.module.hash(), entry.name.hash()); }
    static bool equals(const Wasm::Linker::Name& a, const Wasm::Linker::Name& b) { return a.name == b.name && a.module == b.module; }
};
