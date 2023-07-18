/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/StackInfo.h>
#include <AK/Variant.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Runtime/CommonPropertyNames.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Identifier;
struct BindingPattern;

class VM : public RefCounted<VM> {
public:
    struct CustomData {
        virtual ~CustomData() = default;

        virtual void spin_event_loop_until(JS::SafeFunction<bool()> goal_condition) = 0;
    };

    static ErrorOr<NonnullRefPtr<VM>> create(OwnPtr<CustomData> = {});
    ~VM();

    Heap& heap() { return m_heap; }
    Heap const& heap() const { return m_heap; }

    Bytecode::Interpreter& bytecode_interpreter();
    Bytecode::Interpreter* bytecode_interpreter_if_exists();

    Interpreter& interpreter();
    Interpreter* interpreter_if_exists();

    void push_interpreter(Interpreter&);
    void pop_interpreter(Interpreter&);

    void dump_backtrace() const;

    class InterpreterExecutionScope {
    public:
        InterpreterExecutionScope(Interpreter&);
        ~InterpreterExecutionScope();

        Interpreter& interpreter() { return m_interpreter; }

    private:
        Interpreter& m_interpreter;
    };

    void gather_roots(HashTable<Cell*>&);

#define __JS_ENUMERATE(SymbolName, snake_name)                  \
    NonnullGCPtr<Symbol> well_known_symbol_##snake_name() const \
    {                                                           \
        return *m_well_known_symbols.snake_name;                \
    }
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    HashMap<String, GCPtr<PrimitiveString>>& string_cache()
    {
        return m_string_cache;
    }

    HashMap<DeprecatedString, GCPtr<PrimitiveString>>& deprecated_string_cache()
    {
        return m_deprecated_string_cache;
    }

    PrimitiveString& empty_string() { return *m_empty_string; }

    PrimitiveString& single_ascii_character_string(u8 character)
    {
        VERIFY(character < 0x80);
        return *m_single_ascii_character_strings[character];
    }

    // This represents the list of errors from ErrorTypes.h whose messages are used in contexts which
    // must not fail to allocate when they are used. For example, we cannot allocate when we raise an
    // out-of-memory error, thus we pre-allocate that error string at VM creation time.
    enum class ErrorMessage {
        OutOfMemory,

        // Keep this last:
        __Count,
    };
    String const& error_message(ErrorMessage) const;

    bool did_reach_stack_space_limit() const
    {
        // Address sanitizer (ASAN) used to check for more space but
        // currently we can't detect the stack size with it enabled.
        return m_stack_info.size_free() < 32 * KiB;
    }

    void push_execution_context(ExecutionContext& context)
    {
        m_execution_context_stack.append(&context);
    }

    // TODO: Rename this function instead of providing a second argument, now that the global object is no longer passed in.
    struct CheckStackSpaceLimitTag { };

    ThrowCompletionOr<void> push_execution_context(ExecutionContext& context, CheckStackSpaceLimitTag)
    {
        // Ensure we got some stack space left, so the next function call doesn't kill us.
        if (did_reach_stack_space_limit())
            return throw_completion<InternalError>(ErrorType::CallStackSizeExceeded);
        m_execution_context_stack.append(&context);
        return {};
    }

    void pop_execution_context()
    {
        m_execution_context_stack.take_last();
        if (m_execution_context_stack.is_empty() && on_call_stack_emptied)
            on_call_stack_emptied();
    }

    // https://tc39.es/ecma262/#running-execution-context
    // At any point in time, there is at most one execution context per agent that is actually executing code.
    // This is known as the agent's running execution context.
    ExecutionContext& running_execution_context() { return *m_execution_context_stack.last(); }
    ExecutionContext const& running_execution_context() const { return *m_execution_context_stack.last(); }

    // https://tc39.es/ecma262/#execution-context-stack
    // The execution context stack is used to track execution contexts.
    Vector<ExecutionContext*> const& execution_context_stack() const { return m_execution_context_stack; }
    Vector<ExecutionContext*>& execution_context_stack() { return m_execution_context_stack; }

    Environment const* lexical_environment() const { return running_execution_context().lexical_environment; }
    Environment* lexical_environment() { return running_execution_context().lexical_environment; }

    Environment const* variable_environment() const { return running_execution_context().variable_environment; }
    Environment* variable_environment() { return running_execution_context().variable_environment; }

    // https://tc39.es/ecma262/#current-realm
    // The value of the Realm component of the running execution context is also called the current Realm Record.
    Realm const* current_realm() const { return running_execution_context().realm; }
    Realm* current_realm() { return running_execution_context().realm; }

    // https://tc39.es/ecma262/#active-function-object
    // The value of the Function component of the running execution context is also called the active function object.
    FunctionObject const* active_function_object() const { return running_execution_context().function; }
    FunctionObject* active_function_object() { return running_execution_context().function; }

    bool in_strict_mode() const;

    size_t argument_count() const
    {
        if (m_execution_context_stack.is_empty())
            return 0;
        return running_execution_context().arguments.size();
    }

    Value argument(size_t index) const
    {
        if (m_execution_context_stack.is_empty())
            return {};
        auto& arguments = running_execution_context().arguments;
        return index < arguments.size() ? arguments[index] : js_undefined();
    }

    Value this_value() const
    {
        VERIFY(!m_execution_context_stack.is_empty());
        return running_execution_context().this_value;
    }

    ThrowCompletionOr<Value> resolve_this_binding();

    StackInfo const& stack_info() const { return m_stack_info; }

    HashMap<String, NonnullGCPtr<Symbol>> const& global_symbol_registry() const { return m_global_symbol_registry; }
    HashMap<String, NonnullGCPtr<Symbol>>& global_symbol_registry() { return m_global_symbol_registry; }

    u32 execution_generation() const { return m_execution_generation; }
    void finish_execution_generation() { ++m_execution_generation; }

    ThrowCompletionOr<Reference> resolve_binding(DeprecatedFlyString const&, Environment* = nullptr);
    ThrowCompletionOr<Reference> get_identifier_reference(Environment*, DeprecatedFlyString, bool strict, size_t hops = 0);

    // 5.2.3.2 Throw an Exception, https://tc39.es/ecma262/#sec-throw-an-exception
    template<typename T, typename... Args>
    Completion throw_completion(Args&&... args)
    {
        auto& realm = *current_realm();
        auto completion = T::create(realm, forward<Args>(args)...);

        if constexpr (IsSame<decltype(completion), ThrowCompletionOr<NonnullGCPtr<T>>>)
            return JS::throw_completion(MUST_OR_THROW_OOM(completion));
        else
            return JS::throw_completion(completion);
    }

    template<typename T, typename... Args>
    Completion throw_completion(ErrorType type, Args&&... args)
    {
        return throw_completion<T>(DeprecatedString::formatted(type.message(), forward<Args>(args)...));
    }

    Value get_new_target();

    Object* get_import_meta();

    Object& get_global_object();

    CommonPropertyNames names;

    void run_queued_promise_jobs();
    void enqueue_promise_job(Function<ThrowCompletionOr<Value>()> job, Realm*);

    void run_queued_finalization_registry_cleanup_jobs();
    void enqueue_finalization_registry_cleanup_job(FinalizationRegistry&);

    void promise_rejection_tracker(Promise&, Promise::RejectionOperation) const;

    Function<void()> on_call_stack_emptied;
    Function<void(Promise&)> on_promise_unhandled_rejection;
    Function<void(Promise&)> on_promise_rejection_handled;

    CustomData* custom_data() { return m_custom_data; }

    ThrowCompletionOr<void> destructuring_assignment_evaluation(NonnullRefPtr<BindingPattern const> const& target, Value value);
    ThrowCompletionOr<void> binding_initialization(DeprecatedFlyString const& target, Value value, Environment* environment);
    ThrowCompletionOr<void> binding_initialization(NonnullRefPtr<BindingPattern const> const& target, Value value, Environment* environment);

    ThrowCompletionOr<Value> named_evaluation_if_anonymous_function(ASTNode const& expression, DeprecatedFlyString const& name);

    void save_execution_context_stack();
    void restore_execution_context_stack();

    // Do not call this method unless you are sure this is the only and first module to be loaded in this vm.
    ThrowCompletionOr<void> link_and_eval_module(Badge<Interpreter>, SourceTextModule& module);
    ThrowCompletionOr<void> link_and_eval_module(Badge<Bytecode::Interpreter>, SourceTextModule& module);

    ScriptOrModule get_active_script_or_module() const;

    Function<ThrowCompletionOr<NonnullGCPtr<Module>>(ScriptOrModule, ModuleRequest const&)> host_resolve_imported_module;
    Function<ThrowCompletionOr<void>(ScriptOrModule, ModuleRequest, PromiseCapability const&)> host_import_module_dynamically;
    Function<void(ScriptOrModule, ModuleRequest const&, PromiseCapability const&, Promise*)> host_finish_dynamic_import;

    Function<HashMap<PropertyKey, Value>(SourceTextModule&)> host_get_import_meta_properties;
    Function<void(Object*, SourceTextModule const&)> host_finalize_import_meta;

    Function<Vector<DeprecatedString>()> host_get_supported_import_assertions;

    void enable_default_host_import_module_dynamically_hook();

    Function<void(Promise&, Promise::RejectionOperation)> host_promise_rejection_tracker;
    Function<ThrowCompletionOr<Value>(JobCallback&, Value, MarkedVector<Value>)> host_call_job_callback;
    Function<void(FinalizationRegistry&)> host_enqueue_finalization_registry_cleanup_job;
    Function<void(Function<ThrowCompletionOr<Value>()>, Realm*)> host_enqueue_promise_job;
    Function<JobCallback(FunctionObject&)> host_make_job_callback;
    Function<ThrowCompletionOr<void>(Realm&)> host_ensure_can_compile_strings;
    Function<ThrowCompletionOr<void>(Object&)> host_ensure_can_add_private_element;

    // Execute a specific AST node either in AST or BC interpreter, depending on which one is enabled by default.
    // NOTE: This is meant as a temporary stopgap until everything is bytecode.
    ThrowCompletionOr<Value> execute_ast_node(ASTNode const&);

private:
    using ErrorMessages = AK::Array<String, to_underlying(ErrorMessage::__Count)>;

    struct WellKnownSymbols {
#define __JS_ENUMERATE(SymbolName, snake_name) \
    GCPtr<Symbol> snake_name;
        JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
    };

    VM(OwnPtr<CustomData>, ErrorMessages);

    ThrowCompletionOr<void> property_binding_initialization(BindingPattern const& binding, Value value, Environment* environment);
    ThrowCompletionOr<void> iterator_binding_initialization(BindingPattern const& binding, IteratorRecord& iterator_record, Environment* environment);

    ThrowCompletionOr<NonnullGCPtr<Module>> resolve_imported_module(ScriptOrModule referencing_script_or_module, ModuleRequest const& module_request);
    ThrowCompletionOr<void> link_and_eval_module(Module& module);

    ThrowCompletionOr<void> import_module_dynamically(ScriptOrModule referencing_script_or_module, ModuleRequest module_request, PromiseCapability const& promise_capability);
    void finish_dynamic_import(ScriptOrModule referencing_script_or_module, ModuleRequest module_request, PromiseCapability const& promise_capability, Promise* inner_promise);

    void set_well_known_symbols(WellKnownSymbols well_known_symbols) { m_well_known_symbols = move(well_known_symbols); }

    HashMap<String, GCPtr<PrimitiveString>> m_string_cache;
    HashMap<DeprecatedString, GCPtr<PrimitiveString>> m_deprecated_string_cache;

    Heap m_heap;
    Vector<Interpreter*> m_interpreters;

    Vector<ExecutionContext*> m_execution_context_stack;

    Vector<Vector<ExecutionContext*>> m_saved_execution_context_stacks;

    StackInfo m_stack_info;

    // GlobalSymbolRegistry, https://tc39.es/ecma262/#table-globalsymbolregistry-record-fields
    HashMap<String, NonnullGCPtr<Symbol>> m_global_symbol_registry;

    Vector<Function<ThrowCompletionOr<Value>()>> m_promise_jobs;

    Vector<GCPtr<FinalizationRegistry>> m_finalization_registry_cleanup_jobs;

    GCPtr<PrimitiveString> m_empty_string;
    GCPtr<PrimitiveString> m_single_ascii_character_strings[128] {};
    ErrorMessages m_error_messages;

    struct StoredModule {
        ScriptOrModule referencing_script_or_module;
        DeprecatedString filename;
        DeprecatedString type;
        Handle<Module> module;
        bool has_once_started_linking { false };
    };

    StoredModule* get_stored_module(ScriptOrModule const& script_or_module, DeprecatedString const& filename, DeprecatedString const& type);

    Vector<StoredModule> m_loaded_modules;

    WellKnownSymbols m_well_known_symbols;

    u32 m_execution_generation { 0 };

    OwnPtr<CustomData> m_custom_data;

    OwnPtr<Bytecode::Interpreter> m_bytecode_interpreter;
};

}
