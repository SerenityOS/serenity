/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/StackInfo.h>
#include <AK/Variant.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/CommonPropertyNames.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/Exception.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Identifier;
struct BindingPattern;

class VM : public RefCounted<VM> {
public:
    struct CustomData {
        virtual ~CustomData();
    };

    static NonnullRefPtr<VM> create(OwnPtr<CustomData> = {});
    ~VM();

    Heap& heap() { return m_heap; }
    const Heap& heap() const { return m_heap; }

    Interpreter& interpreter();
    Interpreter* interpreter_if_exists();

    void push_interpreter(Interpreter&);
    void pop_interpreter(Interpreter&);

    Exception* exception() { return m_exception; }
    void set_exception(Exception& exception) { m_exception = &exception; }
    void clear_exception() { m_exception = nullptr; }

    void dump_backtrace() const;

    class InterpreterExecutionScope {
    public:
        InterpreterExecutionScope(Interpreter&);
        ~InterpreterExecutionScope();

    private:
        Interpreter& m_interpreter;
    };

    void gather_roots(HashTable<Cell*>&);

#define __JS_ENUMERATE(SymbolName, snake_name) \
    Symbol* well_known_symbol_##snake_name() const { return m_well_known_symbol_##snake_name; }
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    Symbol* get_global_symbol(const String& description);

    HashMap<String, PrimitiveString*>& string_cache() { return m_string_cache; }
    PrimitiveString& empty_string() { return *m_empty_string; }
    PrimitiveString& single_ascii_character_string(u8 character)
    {
        VERIFY(character < 0x80);
        return *m_single_ascii_character_strings[character];
    }

    bool did_reach_stack_space_limit() const
    {
        // Address sanitizer (ASAN) used to check for more space but
        // currently we can't detect the stack size with it enabled.
        return m_stack_info.size_free() < 32 * KiB;
    }

    ThrowCompletionOr<void> push_execution_context(ExecutionContext& context, GlobalObject& global_object)
    {
        VERIFY(!exception());
        // Ensure we got some stack space left, so the next function call doesn't kill us.
        if (did_reach_stack_space_limit())
            return throw_completion<InternalError>(global_object, ErrorType::CallStackSizeExceeded);
        m_execution_context_stack.append(&context);
        return {};
    }

    void pop_execution_context()
    {
        m_execution_context_stack.take_last();
        if (m_execution_context_stack.is_empty() && on_call_stack_emptied)
            on_call_stack_emptied();
    }

    ExecutionContext& running_execution_context() { return *m_execution_context_stack.last(); }
    ExecutionContext const& running_execution_context() const { return *m_execution_context_stack.last(); }
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

    Value this_value(Object& global_object) const
    {
        if (m_execution_context_stack.is_empty())
            return &global_object;
        return running_execution_context().this_value;
    }

    ThrowCompletionOr<Value> resolve_this_binding(GlobalObject&);

    const StackInfo& stack_info() const { return m_stack_info; };

    u32 execution_generation() const { return m_execution_generation; }
    void finish_execution_generation() { ++m_execution_generation; }

    ThrowCompletionOr<Reference> resolve_binding(FlyString const&, Environment* = nullptr);
    ThrowCompletionOr<Reference> get_identifier_reference(Environment*, FlyString, bool strict, size_t hops = 0);

    template<typename T, typename... Args>
    void throw_exception(GlobalObject& global_object, Args&&... args)
    {
        return throw_exception(global_object, T::create(global_object, forward<Args>(args)...));
    }

    void throw_exception(Exception&);
    void throw_exception(GlobalObject& global_object, Value value)
    {
        return throw_exception(*heap().allocate<Exception>(global_object, value));
    }

    template<typename T, typename... Args>
    void throw_exception(GlobalObject& global_object, ErrorType type, Args&&... args)
    {
        return throw_exception(global_object, T::create(global_object, String::formatted(type.message(), forward<Args>(args)...)));
    }

    // 5.2.3.2 Throw an Exception, https://tc39.es/ecma262/#sec-throw-an-exception
    template<typename T, typename... Args>
    Completion throw_completion(GlobalObject& global_object, Args&&... args)
    {
        auto* error = T::create(global_object, forward<Args>(args)...);
        // NOTE: This is temporary until we remove VM::exception().
        throw_exception(global_object, error);
        return JS::throw_completion(error);
    }

    template<typename T, typename... Args>
    Completion throw_completion(GlobalObject& global_object, ErrorType type, Args&&... args)
    {
        return throw_completion<T>(global_object, String::formatted(type.message(), forward<Args>(args)...));
    }

    Value construct(FunctionObject&, FunctionObject& new_target, Optional<MarkedValueList> arguments);

    String join_arguments(size_t start_index = 0) const;

    Value get_new_target();

    CommonPropertyNames names;

    void run_queued_promise_jobs();
    void enqueue_promise_job(NativeFunction&);

    void run_queued_finalization_registry_cleanup_jobs();
    void enqueue_finalization_registry_cleanup_job(FinalizationRegistry&);

    void promise_rejection_tracker(const Promise&, Promise::RejectionOperation) const;

    Function<void()> on_call_stack_emptied;
    Function<void(const Promise&)> on_promise_unhandled_rejection;
    Function<void(const Promise&)> on_promise_rejection_handled;

    ThrowCompletionOr<void> initialize_instance_elements(Object& object, ECMAScriptFunctionObject& constructor);

    CustomData* custom_data() { return m_custom_data; }

    ThrowCompletionOr<void> destructuring_assignment_evaluation(NonnullRefPtr<BindingPattern> const& target, Value value, GlobalObject& global_object);
    ThrowCompletionOr<void> binding_initialization(FlyString const& target, Value value, Environment* environment, GlobalObject& global_object);
    ThrowCompletionOr<void> binding_initialization(NonnullRefPtr<BindingPattern> const& target, Value value, Environment* environment, GlobalObject& global_object);

    ThrowCompletionOr<Value> named_evaluation_if_anonymous_function(GlobalObject& global_object, ASTNode const& expression, FlyString const& name);

    void save_execution_context_stack();
    void restore_execution_context_stack();

    // Do not call this method unless you are sure this is the only and first module to be loaded in this vm.
    ThrowCompletionOr<void> link_and_eval_module(Badge<Interpreter>, SourceTextModule& module);

    ScriptOrModule get_active_script_or_module() const;

    Function<ThrowCompletionOr<NonnullRefPtr<Module>>(ScriptOrModule, ModuleRequest const&)> host_resolve_imported_module;
    Function<void(ScriptOrModule, ModuleRequest, PromiseCapability)> host_import_module_dynamically;
    Function<void(ScriptOrModule, ModuleRequest const&, PromiseCapability, Promise*)> host_finish_dynamic_import;

    Function<HashMap<PropertyKey, Value>(SourceTextModule const&)> host_get_import_meta_properties;
    Function<void(Object*, SourceTextModule const&)> host_finalize_import_meta;

    Function<Vector<String>()> host_get_supported_import_assertions;

    void enable_default_host_import_module_dynamically_hook();

private:
    explicit VM(OwnPtr<CustomData>);

    ThrowCompletionOr<void> property_binding_initialization(BindingPattern const& binding, Value value, Environment* environment, GlobalObject& global_object);
    ThrowCompletionOr<void> iterator_binding_initialization(BindingPattern const& binding, Iterator& iterator_record, Environment* environment, GlobalObject& global_object);

    ThrowCompletionOr<NonnullRefPtr<Module>> resolve_imported_module(ScriptOrModule referencing_script_or_module, ModuleRequest const& module_request);
    ThrowCompletionOr<void> link_and_eval_module(Module& module);

    void import_module_dynamically(ScriptOrModule referencing_script_or_module, ModuleRequest module_request, PromiseCapability promise_capability);
    void finish_dynamic_import(ScriptOrModule referencing_script_or_module, ModuleRequest module_request, PromiseCapability promise_capability, Promise* inner_promise);

    Exception* m_exception { nullptr };

    HashMap<String, PrimitiveString*> m_string_cache;

    Heap m_heap;
    Vector<Interpreter*> m_interpreters;

    Vector<ExecutionContext*> m_execution_context_stack;

    Vector<Vector<ExecutionContext*>> m_saved_execution_context_stacks;

    StackInfo m_stack_info;

    HashMap<String, Symbol*> m_global_symbol_map;

    Vector<NativeFunction*> m_promise_jobs;

    Vector<FinalizationRegistry*> m_finalization_registry_cleanup_jobs;

    PrimitiveString* m_empty_string { nullptr };
    PrimitiveString* m_single_ascii_character_strings[128] {};

    struct StoredModule {
        ScriptOrModule referencing_script_or_module;
        String filepath;
        String type;
        NonnullRefPtr<Module> module;
        bool has_once_started_linking { false };
    };

    StoredModule* get_stored_module(ScriptOrModule const& script_or_module, String const& filepath, String const& type);

    Vector<StoredModule> m_loaded_modules;

#define __JS_ENUMERATE(SymbolName, snake_name) \
    Symbol* m_well_known_symbol_##snake_name { nullptr };
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    u32 m_execution_generation { 0 };

    OwnPtr<CustomData> m_custom_data;
};

ALWAYS_INLINE Heap& Cell::heap() const
{
    return HeapBlock::from_cell(this)->heap();
}

ALWAYS_INLINE VM& Cell::vm() const
{
    return heap().vm();
}

}
