/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 * Copyright (c) 2023, networkException <networkexception@serenityos.org>
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
#include <LibJS/CyclicModule.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/ModuleLoading.h>
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

enum class HandledByHost {
    Handled,
    Unhandled,
};

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

    void dump_backtrace() const;

    void gather_roots(HashMap<Cell*, HeapRoot>&);

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

    HashMap<ByteString, GCPtr<PrimitiveString>>& byte_string_cache()
    {
        return m_byte_string_cache;
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
#if defined(AK_OS_MACOS) && defined(HAS_ADDRESS_SANITIZER)
        // We hit stack limits sooner on macOS 14 arm64 with ASAN enabled.
        return m_stack_info.size_free() < 96 * KiB;
#else
        return m_stack_info.size_free() < 32 * KiB;
#endif
    }

    // TODO: Rename this function instead of providing a second argument, now that the global object is no longer passed in.
    struct CheckStackSpaceLimitTag { };

    ThrowCompletionOr<void> push_execution_context(ExecutionContext& context, CheckStackSpaceLimitTag)
    {
        // Ensure we got some stack space left, so the next function call doesn't kill us.
        if (did_reach_stack_space_limit())
            return throw_completion<InternalError>(ErrorType::CallStackSizeExceeded);
        push_execution_context(context);
        return {};
    }

    void push_execution_context(ExecutionContext&);
    void pop_execution_context();

    // https://tc39.es/ecma262/#running-execution-context
    // At any point in time, there is at most one execution context per agent that is actually executing code.
    // This is known as the agent's running execution context.
    ExecutionContext& running_execution_context()
    {
        VERIFY(!m_execution_context_stack.is_empty());
        return *m_execution_context_stack.last();
    }
    ExecutionContext const& running_execution_context() const
    {
        VERIFY(!m_execution_context_stack.is_empty());
        return *m_execution_context_stack.last();
    }

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
        return running_execution_context().argument(index);
    }

    Value this_value() const
    {
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

        return JS::throw_completion(completion);
    }

    template<typename T, typename... Args>
    Completion throw_completion(ErrorType type, Args&&... args)
    {
        return throw_completion<T>(ByteString::formatted(type.message(), forward<Args>(args)...));
    }

    Value get_new_target();

    Object* get_import_meta();

    Object& get_global_object();

    CommonPropertyNames names;

    void run_queued_promise_jobs();
    void enqueue_promise_job(NonnullGCPtr<HeapFunction<ThrowCompletionOr<Value>()>> job, Realm*);

    void run_queued_finalization_registry_cleanup_jobs();
    void enqueue_finalization_registry_cleanup_job(FinalizationRegistry&);

    void promise_rejection_tracker(Promise&, Promise::RejectionOperation) const;

    Function<void()> on_call_stack_emptied;
    Function<void(Promise&)> on_promise_unhandled_rejection;
    Function<void(Promise&)> on_promise_rejection_handled;
    Function<void(Object const&, PropertyKey const&)> on_unimplemented_property_access;

    CustomData* custom_data() { return m_custom_data; }

    void save_execution_context_stack();
    void clear_execution_context_stack();
    void restore_execution_context_stack();

    // Do not call this method unless you are sure this is the only and first module to be loaded in this vm.
    ThrowCompletionOr<void> link_and_eval_module(Badge<Bytecode::Interpreter>, SourceTextModule& module);

    ScriptOrModule get_active_script_or_module() const;

    // NOTE: The host defined implementation described in the web spec https://html.spec.whatwg.org/multipage/webappapis.html#hostloadimportedmodule
    //       currently references proposal-import-attributes.
    //       Our implementation of this proposal is outdated however, as such we try to adapt the proposal and living standard
    //       to match our implementation for now.
    // 16.2.1.8 HostLoadImportedModule ( referrer, moduleRequest, hostDefined, payload ), https://tc39.es/proposal-import-attributes/#sec-HostLoadImportedModule
    Function<void(ImportedModuleReferrer, ModuleRequest const&, GCPtr<GraphLoadingState::HostDefined>, ImportedModulePayload)> host_load_imported_module;

    Function<HashMap<PropertyKey, Value>(SourceTextModule&)> host_get_import_meta_properties;
    Function<void(Object*, SourceTextModule const&)> host_finalize_import_meta;

    Function<Vector<ByteString>()> host_get_supported_import_attributes;

    void set_dynamic_imports_allowed(bool value) { m_dynamic_imports_allowed = value; }

    Function<void(Promise&, Promise::RejectionOperation)> host_promise_rejection_tracker;
    Function<ThrowCompletionOr<Value>(JobCallback&, Value, ReadonlySpan<Value>)> host_call_job_callback;
    Function<void(FinalizationRegistry&)> host_enqueue_finalization_registry_cleanup_job;
    Function<void(NonnullGCPtr<HeapFunction<ThrowCompletionOr<Value>()>>, Realm*)> host_enqueue_promise_job;
    Function<JS::NonnullGCPtr<JobCallback>(FunctionObject&)> host_make_job_callback;
    Function<ThrowCompletionOr<void>(Realm&)> host_ensure_can_compile_strings;
    Function<ThrowCompletionOr<void>(Object&)> host_ensure_can_add_private_element;
    Function<ThrowCompletionOr<HandledByHost>(ArrayBuffer&, size_t)> host_resize_array_buffer;

    Vector<StackTraceElement> stack_trace() const;

private:
    using ErrorMessages = AK::Array<String, to_underlying(ErrorMessage::__Count)>;

    struct WellKnownSymbols {
#define __JS_ENUMERATE(SymbolName, snake_name) \
    GCPtr<Symbol> snake_name;
        JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
    };

    VM(OwnPtr<CustomData>, ErrorMessages);

    void load_imported_module(ImportedModuleReferrer, ModuleRequest const&, GCPtr<GraphLoadingState::HostDefined>, ImportedModulePayload);
    ThrowCompletionOr<void> link_and_eval_module(CyclicModule&);

    void set_well_known_symbols(WellKnownSymbols well_known_symbols) { m_well_known_symbols = move(well_known_symbols); }

    HashMap<String, GCPtr<PrimitiveString>> m_string_cache;
    HashMap<ByteString, GCPtr<PrimitiveString>> m_byte_string_cache;

    Heap m_heap;

    Vector<ExecutionContext*> m_execution_context_stack;

    Vector<Vector<ExecutionContext*>> m_saved_execution_context_stacks;

    StackInfo m_stack_info;

    // GlobalSymbolRegistry, https://tc39.es/ecma262/#table-globalsymbolregistry-record-fields
    HashMap<String, NonnullGCPtr<Symbol>> m_global_symbol_registry;

    Vector<NonnullGCPtr<HeapFunction<ThrowCompletionOr<Value>()>>> m_promise_jobs;

    Vector<GCPtr<FinalizationRegistry>> m_finalization_registry_cleanup_jobs;

    GCPtr<PrimitiveString> m_empty_string;
    GCPtr<PrimitiveString> m_single_ascii_character_strings[128] {};
    ErrorMessages m_error_messages;

    struct StoredModule {
        ImportedModuleReferrer referrer;
        ByteString filename;
        ByteString type;
        Handle<Module> module;
        bool has_once_started_linking { false };
    };

    StoredModule* get_stored_module(ImportedModuleReferrer const& script_or_module, ByteString const& filename, ByteString const& type);

    Vector<StoredModule> m_loaded_modules;

    WellKnownSymbols m_well_known_symbols;

    u32 m_execution_generation { 0 };

    OwnPtr<CustomData> m_custom_data;

    OwnPtr<Bytecode::Interpreter> m_bytecode_interpreter;

    bool m_dynamic_imports_allowed { false };
};

template<typename GlobalObjectType, typename... Args>
[[nodiscard]] static NonnullOwnPtr<ExecutionContext> create_simple_execution_context(VM& vm, Args&&... args)
{
    auto root_execution_context = MUST(Realm::initialize_host_defined_realm(
        vm,
        [&](Realm& realm_) -> GlobalObject* {
            return vm.heap().allocate_without_realm<GlobalObjectType>(realm_, forward<Args>(args)...);
        },
        nullptr));
    return root_execution_context;
}

}
