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
#include <AK/StackInfo.h>
#include <AK/UFixedBigInt.h>
#include <LibWasm/Types.h>

// NOTE: Special case for Wasm::Result.
#include <LibJS/Runtime/Completion.h>

namespace Wasm {

class Configuration;
struct Interpreter;

struct InstantiationError {
    ByteString error { "Unknown error" };
};
struct LinkError {
    enum OtherErrors {
        InvalidImportedModule,
    };
    Vector<ByteString> missing_imports;
    Vector<OtherErrors> other_errors;
};

AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, FunctionAddress, Arithmetic, Comparison, Increment);
AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, ExternAddress, Arithmetic, Comparison, Increment);
AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, TableAddress, Arithmetic, Comparison, Increment);
AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, GlobalAddress, Arithmetic, Comparison, Increment);
AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, ElementAddress, Arithmetic, Comparison, Increment);
AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, DataAddress, Arithmetic, Comparison, Increment);
AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(u64, MemoryAddress, Arithmetic, Comparison, Increment);

// FIXME: These should probably be made generic/virtual if/when we decide to do something more
//        fancy than just a dumb interpreter.
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
    explicit Reference()
        : m_ref(Reference::Null { ValueType(ValueType::Kind::FunctionReference) })
    {
    }

    auto& ref() const { return m_ref; }

private:
    RefType m_ref;
};

class Value {
public:
    Value()
        : m_value(0)
    {
    }

    using AnyValueType = Variant<i32, i64, float, double, u128, Reference>;
    explicit Value(AnyValueType value)
        : m_value(move(value))
    {
    }

    template<typename T>
    requires(sizeof(T) == sizeof(u64)) explicit Value(ValueType type, T raw_value)
        : m_value(0)
    {
        switch (type.kind()) {
        case ValueType::Kind::ExternReference:
            m_value = Reference { Reference::Extern { { bit_cast<u64>(raw_value) } } };
            break;
        case ValueType::Kind::FunctionReference:
            m_value = Reference { Reference::Func { { bit_cast<u64>(raw_value) } } };
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
        case ValueType::Kind::NullFunctionReference:
            VERIFY(raw_value == 0);
            m_value = Reference { Reference::Null { ValueType(ValueType::Kind::FunctionReference) } };
            break;
        case ValueType::Kind::NullExternReference:
            VERIFY(raw_value == 0);
            m_value = Reference { Reference::Null { ValueType(ValueType::Kind::ExternReference) } };
            break;
        case ValueType::Kind::V128:
            m_value = u128(0ull, bit_cast<u64>(raw_value));
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    template<SameAs<u128> T>
    explicit Value(T raw_value)
        : m_value(raw_value)
    {
    }

    ALWAYS_INLINE Value(Value const& value) = default;
    ALWAYS_INLINE Value(Value&& value) = default;
    ALWAYS_INLINE Value& operator=(Value&& value) = default;
    ALWAYS_INLINE Value& operator=(Value const& value) = default;

    template<typename T>
    ALWAYS_INLINE Optional<T> to() const
    {
        Optional<T> result;
        m_value.visit(
            [&](auto value) {
                if constexpr (IsSame<T, decltype(value)> || (!IsFloatingPoint<T> && IsSame<decltype(value), MakeSigned<T>>)) {
                    result = static_cast<T>(value);
                } else if constexpr (!IsFloatingPoint<T> && IsConvertible<decltype(value), T>) {
                    // NOTE: No implicit vector <-> scalar conversion.
                    if constexpr (!IsSame<T, u128>) {
                        if (AK::is_within_range<T>(value))
                            result = static_cast<T>(value);
                    }
                }
            },
            [&](u128 value) {
                if constexpr (IsSame<T, u128>)
                    result = value;
            },
            [&](Reference const& value) {
                if constexpr (IsSame<T, Reference>) {
                    result = value;
                } else if constexpr (IsSame<T, Reference::Func>) {
                    if (auto ptr = value.ref().template get_pointer<Reference::Func>())
                        result = *ptr;
                } else if constexpr (IsSame<T, Reference::Extern>) {
                    if (auto ptr = value.ref().template get_pointer<Reference::Extern>())
                        result = *ptr;
                } else if constexpr (IsSame<T, Reference::Null>) {
                    if (auto ptr = value.ref().template get_pointer<Reference::Null>())
                        result = *ptr;
                }
            });
        return result;
    }

    ValueType type() const
    {
        return ValueType(m_value.visit(
            [](i32) { return ValueType::Kind::I32; },
            [](i64) { return ValueType::Kind::I64; },
            [](float) { return ValueType::Kind::F32; },
            [](double) { return ValueType::Kind::F64; },
            [](u128) { return ValueType::Kind::V128; },
            [&](Reference const& type) {
                return type.ref().visit(
                    [](Reference::Func const&) { return ValueType::Kind::FunctionReference; },
                    [](Reference::Null const& null_type) {
                        return null_type.type.kind() == ValueType::ExternReference ? ValueType::Kind::NullExternReference : ValueType::Kind::NullFunctionReference;
                    },
                    [](Reference::Extern const&) { return ValueType::Kind::ExternReference; });
            }));
    }
    auto& value() const { return m_value; }

private:
    AnyValueType m_value;
};

struct Trap {
    ByteString reason;
};

// A variant of Result that does not include external reasons for error (JS::Completion, for now).
class PureResult {
public:
    explicit PureResult(Vector<Value> values)
        : m_result(move(values))
    {
    }

    PureResult(Trap trap)
        : m_result(move(trap))
    {
    }

    auto is_trap() const { return m_result.has<Trap>(); }
    auto& values() const { return m_result.get<Vector<Value>>(); }
    auto& values() { return m_result.get<Vector<Value>>(); }
    auto& trap() const { return m_result.get<Trap>(); }
    auto& trap() { return m_result.get<Trap>(); }

private:
    friend class Result;
    explicit PureResult(Variant<Vector<Value>, Trap>&& result)
        : m_result(move(result))
    {
    }

    Variant<Vector<Value>, Trap> m_result;
};

class Result {
public:
    explicit Result(Vector<Value> values)
        : m_result(move(values))
    {
    }

    Result(Trap trap)
        : m_result(move(trap))
    {
    }

    Result(JS::Completion completion)
        : m_result(move(completion))
    {
        VERIFY(m_result.get<JS::Completion>().is_abrupt());
    }

    Result(PureResult&& result)
        : m_result(result.m_result.downcast<decltype(m_result)>())
    {
    }

    auto is_trap() const { return m_result.has<Trap>(); }
    auto is_completion() const { return m_result.has<JS::Completion>(); }
    auto& values() const { return m_result.get<Vector<Value>>(); }
    auto& values() { return m_result.get<Vector<Value>>(); }
    auto& trap() const { return m_result.get<Trap>(); }
    auto& trap() { return m_result.get<Trap>(); }
    auto& completion() { return m_result.get<JS::Completion>(); }
    auto& completion() const { return m_result.get<JS::Completion>(); }

    PureResult assert_wasm_result() &&
    {
        VERIFY(!is_completion());
        return PureResult(move(m_result).downcast<Vector<Value>, Trap>());
    }

private:
    Variant<Vector<Value>, Trap, JS::Completion> m_result;
};

using ExternValue = Variant<FunctionAddress, TableAddress, MemoryAddress, GlobalAddress>;

class ExportInstance {
public:
    explicit ExportInstance(ByteString name, ExternValue value)
        : m_name(move(name))
        , m_value(move(value))
    {
    }

    auto& name() const { return m_name; }
    auto& value() const { return m_value; }

private:
    ByteString m_name;
    ExternValue m_value;
};

class ModuleInstance {
public:
    explicit ModuleInstance(
        Vector<FunctionType> types, Vector<FunctionAddress> function_addresses, Vector<TableAddress> table_addresses,
        Vector<MemoryAddress> memory_addresses, Vector<GlobalAddress> global_addresses, Vector<DataAddress> data_addresses,
        Vector<ExportInstance> exports)
        : m_types(move(types))
        , m_functions(move(function_addresses))
        , m_tables(move(table_addresses))
        , m_memories(move(memory_addresses))
        , m_globals(move(global_addresses))
        , m_datas(move(data_addresses))
        , m_exports(move(exports))
    {
    }

    ModuleInstance() = default;

    auto& types() const { return m_types; }
    auto& functions() const { return m_functions; }
    auto& tables() const { return m_tables; }
    auto& memories() const { return m_memories; }
    auto& globals() const { return m_globals; }
    auto& elements() const { return m_elements; }
    auto& datas() const { return m_datas; }
    auto& exports() const { return m_exports; }

    auto& types() { return m_types; }
    auto& functions() { return m_functions; }
    auto& tables() { return m_tables; }
    auto& memories() { return m_memories; }
    auto& globals() { return m_globals; }
    auto& elements() { return m_elements; }
    auto& datas() { return m_datas; }
    auto& exports() { return m_exports; }

private:
    Vector<FunctionType> m_types;
    Vector<FunctionAddress> m_functions;
    Vector<TableAddress> m_tables;
    Vector<MemoryAddress> m_memories;
    Vector<GlobalAddress> m_globals;
    Vector<ElementAddress> m_elements;
    Vector<DataAddress> m_datas;
    Vector<ExportInstance> m_exports;
};

class WasmFunction {
public:
    explicit WasmFunction(FunctionType const& type, ModuleInstance const& module, Module::Function const& code)
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
    ModuleInstance const& m_module;
    Module::Function const& m_code;
};

class HostFunction {
public:
    explicit HostFunction(AK::Function<Result(Configuration&, Vector<Value>&)> function, FunctionType const& type)
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

class TableInstance {
public:
    explicit TableInstance(TableType const& type, Vector<Reference> elements)
        : m_elements(move(elements))
        , m_type(type)
    {
    }

    auto& elements() const { return m_elements; }
    auto& elements() { return m_elements; }
    auto& type() const { return m_type; }

    bool grow(u32 size_to_grow, Reference const& fill_value)
    {
        if (size_to_grow == 0)
            return true;
        size_t new_size = m_elements.size() + size_to_grow;
        if (auto max = m_type.limits().max(); max.has_value()) {
            if (max.value() < new_size)
                return false;
        }
        if (new_size >= NumericLimits<u32>::max()) {
            return false;
        }
        auto previous_size = m_elements.size();
        if (m_elements.try_resize(new_size).is_error())
            return false;
        for (size_t i = previous_size; i < m_elements.size(); ++i)
            m_elements[i] = fill_value;
        return true;
    }

private:
    Vector<Reference> m_elements;
    TableType m_type;
};

class MemoryInstance {
public:
    static ErrorOr<MemoryInstance> create(MemoryType const& type)
    {
        MemoryInstance instance { type };

        if (!instance.grow(type.limits().min() * Constants::page_size))
            return Error::from_string_literal("Failed to grow to requested size");

        return { move(instance) };
    }

    auto& type() const { return m_type; }
    auto size() const { return m_size; }
    auto& data() const { return m_data; }
    auto& data() { return m_data; }

    enum class InhibitGrowCallback {
        No,
        Yes,
    };

    bool grow(size_t size_to_grow, InhibitGrowCallback inhibit_callback = InhibitGrowCallback::No)
    {
        if (size_to_grow == 0)
            return true;
        u64 new_size = m_data.size() + size_to_grow;
        // Can't grow past 2^16 pages.
        if (new_size >= Constants::page_size * 65536)
            return false;
        if (auto max = m_type.limits().max(); max.has_value()) {
            if (max.value() * Constants::page_size < new_size)
                return false;
        }
        auto previous_size = m_size;
        if (m_data.try_resize(new_size).is_error())
            return false;
        m_size = new_size;
        // The spec requires that we zero out everything on grow
        __builtin_memset(m_data.offset_pointer(previous_size), 0, size_to_grow);

        // NOTE: This exists because wasm-js-api wants to execute code after a successful grow,
        //       See [this issue](https://github.com/WebAssembly/spec/issues/1635) for more details.
        if (inhibit_callback == InhibitGrowCallback::No && successful_grow_hook)
            successful_grow_hook();

        return true;
    }

    Function<void()> successful_grow_hook;

private:
    explicit MemoryInstance(MemoryType const& type)
        : m_type(type)
    {
    }

    MemoryType m_type;
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
    GlobalType type() const { return { m_value.type(), is_mutable() }; }
    void set_value(Value value)
    {
        VERIFY(is_mutable());
        m_value = move(value);
    }

private:
    bool m_mutable { false };
    Value m_value;
};

class DataInstance {
public:
    explicit DataInstance(Vector<u8> data)
        : m_data(move(data))
    {
    }

    size_t size() const { return m_data.size(); }

    Vector<u8>& data() { return m_data; }
    Vector<u8> const& data() const { return m_data; }

private:
    Vector<u8> m_data;
};

class ElementInstance {
public:
    explicit ElementInstance(ValueType type, Vector<Reference> references)
        : m_type(move(type))
        , m_references(move(references))
    {
    }

    auto& type() const { return m_type; }
    auto& references() const { return m_references; }

private:
    ValueType m_type;
    Vector<Reference> m_references;
};

class Store {
public:
    Store() = default;

    Optional<FunctionAddress> allocate(ModuleInstance& module, Module::Function const& function);
    Optional<FunctionAddress> allocate(HostFunction&&);
    Optional<TableAddress> allocate(TableType const&);
    Optional<MemoryAddress> allocate(MemoryType const&);
    Optional<DataAddress> allocate_data(Vector<u8>);
    Optional<GlobalAddress> allocate(GlobalType const&, Value);
    Optional<ElementAddress> allocate(ValueType const&, Vector<Reference>);

    FunctionInstance* get(FunctionAddress);
    TableInstance* get(TableAddress);
    MemoryInstance* get(MemoryAddress);
    GlobalInstance* get(GlobalAddress);
    DataInstance* get(DataAddress);
    ElementInstance* get(ElementAddress);

private:
    Vector<FunctionInstance> m_functions;
    Vector<TableInstance> m_tables;
    Vector<MemoryInstance> m_memories;
    Vector<GlobalInstance> m_globals;
    Vector<ElementInstance> m_elements;
    Vector<DataInstance> m_datas;
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
    InstructionPointer m_continuation { 0 };
};

class Frame {
public:
    explicit Frame(ModuleInstance const& module, Vector<Value> locals, Expression const& expression, size_t arity)
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
    ModuleInstance const& m_module;
    Vector<Value> m_locals;
    Expression const& m_expression;
    size_t m_arity { 0 };
};

class Stack {
public:
    using EntryType = Variant<Value, Label, Frame>;
    Stack() = default;

    [[nodiscard]] ALWAYS_INLINE bool is_empty() const { return m_data.is_empty(); }
    ALWAYS_INLINE void push(EntryType entry) { m_data.append(move(entry)); }
    ALWAYS_INLINE auto pop() { return m_data.take_last(); }
    ALWAYS_INLINE auto& peek() const { return m_data.last(); }
    ALWAYS_INLINE auto& peek() { return m_data.last(); }

    ALWAYS_INLINE auto size() const { return m_data.size(); }
    ALWAYS_INLINE auto& entries() const { return m_data; }
    ALWAYS_INLINE auto& entries() { return m_data; }

private:
    Vector<EntryType, 1024> m_data;
};

using InstantiationResult = AK::ErrorOr<NonnullOwnPtr<ModuleInstance>, InstantiationError>;

class AbstractMachine {
public:
    explicit AbstractMachine() = default;

    // Validate a module; permanently sets the module's validity status.
    ErrorOr<void, ValidationError> validate(Module&);
    // Load and instantiate a module, and link it into this interpreter.
    InstantiationResult instantiate(Module const&, Vector<ExternValue>);
    Result invoke(FunctionAddress, Vector<Value>);
    Result invoke(Interpreter&, FunctionAddress, Vector<Value>);

    auto& store() const { return m_store; }
    auto& store() { return m_store; }

    void enable_instruction_count_limit() { m_should_limit_instruction_count = true; }

private:
    Optional<InstantiationError> allocate_all_initial_phase(Module const&, ModuleInstance&, Vector<ExternValue>&, Vector<Value>& global_values, Vector<FunctionAddress>& own_functions);
    Optional<InstantiationError> allocate_all_final_phase(Module const&, ModuleInstance&, Vector<Vector<Reference>>& elements);
    Store m_store;
    StackInfo m_stack_info;
    bool m_should_limit_instruction_count { false };
};

class Linker {
public:
    struct Name {
        ByteString module;
        ByteString name;
        ImportSection::Import::ImportDesc type;
    };

    explicit Linker(Module const& module)
        : m_module(module)
    {
    }

    // Link a module, the import 'module name' is ignored with this.
    void link(ModuleInstance const&);

    // Link a bunch of qualified values, also matches 'module name'.
    void link(HashMap<Name, ExternValue> const&);

    auto& unresolved_imports()
    {
        populate();
        return m_unresolved_imports;
    }

    AK::ErrorOr<Vector<ExternValue>, LinkError> finish();

private:
    void populate();

    Module const& m_module;
    HashMap<Name, ExternValue> m_resolved_imports;
    HashTable<Name> m_unresolved_imports;
    Vector<Name> m_ordered_imports;
    Optional<LinkError> m_error;
};

}

template<>
struct AK::Traits<Wasm::Linker::Name> : public AK::DefaultTraits<Wasm::Linker::Name> {
    static constexpr bool is_trivial() { return false; }
    static unsigned hash(Wasm::Linker::Name const& entry) { return pair_int_hash(entry.module.hash(), entry.name.hash()); }
    static bool equals(Wasm::Linker::Name const& a, Wasm::Linker::Name const& b) { return a.name == b.name && a.module == b.module; }
};
