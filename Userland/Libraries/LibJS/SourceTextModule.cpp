/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/QuickSort.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/ModuleEnvironment.h>
#include <LibJS/SourceTextModule.h>

namespace JS {

// 2.7 Static Semantics: AssertClauseToAssertions, https://tc39.es/proposal-import-assertions/#sec-assert-clause-to-assertions
static Vector<ModuleRequest::Assertion> assert_clause_to_assertions(Vector<ModuleRequest::Assertion> const& source_assertions, Vector<DeprecatedString> const& supported_import_assertions)
{
    // AssertClause : assert { AssertEntries ,opt }
    // 1. Let assertions be AssertClauseToAssertions of AssertEntries.
    Vector<ModuleRequest::Assertion> assertions;

    // AssertEntries : AssertionKey : StringLiteral
    // AssertEntries : AssertionKey : StringLiteral , AssertEntries
    // 1. Let supportedAssertions be !HostGetSupportedImportAssertions().

    for (auto& assertion : source_assertions) {
        // 2. Let key be StringValue of AssertionKey.
        // 3. If supportedAssertions contains key,
        if (supported_import_assertions.contains_slow(assertion.key)) {
            // a. Let entry be a Record { [[Key]]: key, [[Value]]: StringValue of StringLiteral }.
            assertions.empend(assertion);
        }
    }

    // 2. Sort assertions by the code point order of the [[Key]] of each element. NOTE: This sorting is observable only in that hosts are prohibited from distinguishing among assertions by the order they occur in.
    // Note: The sorting is done in construction of the ModuleRequest object.

    // 3. Return assertions.
    return assertions;
}

// 16.2.1.3 Static Semantics: ModuleRequests, https://tc39.es/ecma262/#sec-static-semantics-modulerequests
static Vector<ModuleRequest> module_requests(Program& program, Vector<DeprecatedString> const& supported_import_assertions)
{
    // A List of all the ModuleSpecifier strings used by the module represented by this record to request the importation of a module.
    // Note: The List is source text occurrence ordered!
    struct RequestedModuleAndSourceIndex {
        u32 source_offset { 0 };
        ModuleRequest const* module_request { nullptr };
    };

    Vector<RequestedModuleAndSourceIndex> requested_modules_with_indices;

    for (auto& import_statement : program.imports())
        requested_modules_with_indices.empend(import_statement->start_offset(), &import_statement->module_request());

    for (auto& export_statement : program.exports()) {
        for (auto& export_entry : export_statement->entries()) {
            if (!export_entry.is_module_request())
                continue;
            requested_modules_with_indices.empend(export_statement->start_offset(), &export_statement->module_request());
        }
    }

    // Note: The List is source code occurrence ordered. https://tc39.es/proposal-import-assertions/#table-cyclic-module-fields
    quick_sort(requested_modules_with_indices, [&](RequestedModuleAndSourceIndex const& lhs, RequestedModuleAndSourceIndex const& rhs) {
        return lhs.source_offset < rhs.source_offset;
    });

    Vector<ModuleRequest> requested_modules_in_source_order;
    requested_modules_in_source_order.ensure_capacity(requested_modules_with_indices.size());
    for (auto& module : requested_modules_with_indices) {
        // 2.10 Static Semantics: ModuleRequests https://tc39.es/proposal-import-assertions/#sec-static-semantics-modulerequests
        if (module.module_request->assertions.is_empty()) {
            //  ExportDeclaration : export ExportFromClause FromClause ;
            //  ImportDeclaration : import ImportClause FromClause ;

            // 1. Let specifier be StringValue of the StringLiteral contained in FromClause.
            // 2. Return a ModuleRequest Record { [[Specifer]]: specifier, [[Assertions]]: an empty List }.
            requested_modules_in_source_order.empend(module.module_request->module_specifier);
        } else {
            //  ExportDeclaration : export ExportFromClause FromClause AssertClause ;
            //  ImportDeclaration : import ImportClause FromClause AssertClause ;

            // 1. Let specifier be StringValue of the StringLiteral contained in FromClause.
            // 2. Let assertions be AssertClauseToAssertions of AssertClause.
            auto assertions = assert_clause_to_assertions(module.module_request->assertions, supported_import_assertions);
            // Note: We have to modify the assertions in place because else it might keep non supported ones
            const_cast<ModuleRequest*>(module.module_request)->assertions = move(assertions);

            // 3. Return a ModuleRequest Record { [[Specifer]]: specifier, [[Assertions]]: assertions }.
            requested_modules_in_source_order.empend(module.module_request->module_specifier, module.module_request->assertions);
        }
    }

    return requested_modules_in_source_order;
}

SourceTextModule::SourceTextModule(Realm& realm, StringView filename, Script::HostDefined* host_defined, bool has_top_level_await, NonnullRefPtr<Program> body, Vector<ModuleRequest> requested_modules,
    Vector<ImportEntry> import_entries, Vector<ExportEntry> local_export_entries,
    Vector<ExportEntry> indirect_export_entries, Vector<ExportEntry> star_export_entries,
    RefPtr<ExportStatement const> default_export)
    : CyclicModule(realm, filename, has_top_level_await, move(requested_modules), host_defined)
    , m_ecmascript_code(move(body))
    , m_execution_context(realm.heap())
    , m_import_entries(move(import_entries))
    , m_local_export_entries(move(local_export_entries))
    , m_indirect_export_entries(move(indirect_export_entries))
    , m_star_export_entries(move(star_export_entries))
    , m_default_export(move(default_export))
{
}

void SourceTextModule::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_import_meta);
}

// 16.2.1.6.1 ParseModule ( sourceText, realm, hostDefined ), https://tc39.es/ecma262/#sec-parsemodule
Result<NonnullGCPtr<SourceTextModule>, Vector<ParserError>> SourceTextModule::parse(StringView source_text, Realm& realm, StringView filename, Script::HostDefined* host_defined)
{
    // 1. Let body be ParseText(sourceText, Module).
    auto parser = Parser(Lexer(source_text, filename), Program::Type::Module);
    auto body = parser.parse_program();

    // 2. If body is a List of errors, return body.
    if (parser.has_errors())
        return parser.errors();

    // Needed for 2.7 Static Semantics: AssertClauseToAssertions, https://tc39.es/proposal-import-assertions/#sec-assert-clause-to-assertions
    // 1. Let supportedAssertions be !HostGetSupportedImportAssertions().
    auto supported_assertions = realm.vm().host_get_supported_import_assertions();

    // 3. Let requestedModules be the ModuleRequests of body.
    auto requested_modules = module_requests(*body, supported_assertions);

    // 4. Let importEntries be ImportEntries of body.
    Vector<ImportEntry> import_entries;
    for (auto const& import_statement : body->imports())
        import_entries.extend(import_statement->entries());

    // 5. Let importedBoundNames be ImportedLocalNames(importEntries).
    // Note: Since we have to potentially extract the import entry we just use importEntries
    //       In the future it might be an optimization to have a set/map of string to speed up the search.

    // 6. Let indirectExportEntries be a new empty List.
    Vector<ExportEntry> indirect_export_entries;

    // 7. Let localExportEntries be a new empty List.
    Vector<ExportEntry> local_export_entries;

    // 8. Let starExportEntries be a new empty List.
    Vector<ExportEntry> star_export_entries;

    // Note: Not in the spec but makes it easier to find the default.
    RefPtr<ExportStatement const> default_export;

    // 9. Let exportEntries be ExportEntries of body.
    // 10. For each ExportEntry Record ee of exportEntries, do
    for (auto const& export_statement : body->exports()) {

        if (export_statement->is_default_export()) {
            VERIFY(!default_export);
            VERIFY(export_statement->entries().size() == 1);
            VERIFY(export_statement->has_statement());

            auto const& entry = export_statement->entries()[0];
            VERIFY(entry.kind == ExportEntry::Kind::NamedExport);
            VERIFY(!entry.is_module_request());
            VERIFY(import_entries.find_if(
                                     [&](ImportEntry const& import_entry) {
                                         return import_entry.local_name == entry.local_or_import_name;
                                     })
                       .is_end());
            default_export = export_statement;
        }

        for (auto const& export_entry : export_statement->entries()) {

            // Special case, export {} from "module" should add "module" to
            // required_modules but not any import or export so skip here.
            if (export_entry.kind == ExportEntry::Kind::EmptyNamedExport) {
                VERIFY(export_statement->entries().size() == 1);
                break;
            }

            // a. If ee.[[ModuleRequest]] is null, then
            if (!export_entry.is_module_request()) {

                auto in_imported_bound_names = import_entries.find_if(
                    [&](ImportEntry const& import_entry) {
                        return import_entry.local_name == export_entry.local_or_import_name;
                    });

                // i. If ee.[[LocalName]] is not an element of importedBoundNames, then
                if (in_imported_bound_names.is_end()) {
                    // 1. Append ee to localExportEntries.
                    local_export_entries.empend(export_entry);
                }
                // ii. Else,
                else {
                    // 1. Let ie be the element of importEntries whose [[LocalName]] is the same as ee.[[LocalName]].
                    auto& import_entry = *in_imported_bound_names;

                    // 2. If ie.[[ImportName]] is namespace-object, then
                    if (import_entry.is_namespace) {
                        // a. NOTE: This is a re-export of an imported module namespace object.
                        // b. Append ee to localExportEntries.
                        local_export_entries.empend(export_entry);
                    }
                    // 3. Else,
                    else {
                        // a. NOTE: This is a re-export of a single name.
                        // b. Append the ExportEntry Record { [[ModuleRequest]]: ie.[[ModuleRequest]], [[ImportName]]: ie.[[ImportName]], [[LocalName]]: null, [[ExportName]]: ee.[[ExportName]] } to indirectExportEntries.
                        indirect_export_entries.empend(ExportEntry::indirect_export_entry(import_entry.module_request(), import_entry.import_name, export_entry.export_name));
                    }
                }
            }
            // b. Else if ee.[[ImportName]] is all-but-default, then
            else if (export_entry.kind == ExportEntry::Kind::ModuleRequestAllButDefault) {
                // i. Assert: ee.[[ExportName]] is null.
                VERIFY(export_entry.export_name.is_null());
                // ii. Append ee to starExportEntries.
                star_export_entries.empend(export_entry);
            }
            // c. Else,
            else {
                // i. Append ee to indirectExportEntries.
                indirect_export_entries.empend(export_entry);
            }
        }
    }

    // 11. Let async be body Contains await.
    bool async = body->has_top_level_await();

    // 12. Return Source Text Module Record {
    //          [[Realm]]: realm, [[Environment]]: empty, [[Namespace]]: empty, [[CycleRoot]]: empty, [[HasTLA]]: async,
    //          [[AsyncEvaluation]]: false, [[TopLevelCapability]]: empty, [[AsyncParentModules]]: « »,
    //          [[PendingAsyncDependencies]]: empty, [[Status]]: unlinked, [[EvaluationError]]: empty,
    //          [[HostDefined]]: hostDefined, [[ECMAScriptCode]]: body, [[Context]]: empty, [[ImportMeta]]: empty,
    //          [[RequestedModules]]: requestedModules, [[ImportEntries]]: importEntries, [[LocalExportEntries]]: localExportEntries,
    //          [[IndirectExportEntries]]: indirectExportEntries, [[StarExportEntries]]: starExportEntries, [[DFSIndex]]: empty, [[DFSAncestorIndex]]: empty }.
    return realm.heap().allocate_without_realm<SourceTextModule>(
        realm,
        filename,
        host_defined,
        async,
        move(body),
        move(requested_modules),
        move(import_entries),
        move(local_export_entries),
        move(indirect_export_entries),
        move(star_export_entries),
        move(default_export));
}

// 16.2.1.6.2 GetExportedNames ( [ exportStarSet ] ), https://tc39.es/ecma262/#sec-getexportednames
ThrowCompletionOr<Vector<DeprecatedFlyString>> SourceTextModule::get_exported_names(VM& vm, Vector<Module*> export_star_set)
{
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] get_export_names of {}", filename());
    // 1. If exportStarSet is not present, set exportStarSet to a new empty List.
    // Note: This is done by default argument

    // 2. If exportStarSet contains module, then
    if (export_star_set.contains_slow(this)) {
        // a. Assert: We've reached the starting point of an export * circularity.
        // FIXME: How do we check that?

        // b. Return a new empty List.
        return Vector<DeprecatedFlyString> {};
    }

    // 3. Append module to exportStarSet.
    export_star_set.append(this);

    // 4. Let exportedNames be a new empty List.
    Vector<DeprecatedFlyString> exported_names;

    // 5. For each ExportEntry Record e of module.[[LocalExportEntries]], do
    for (auto& entry : m_local_export_entries) {
        // a. Assert: module provides the direct binding for this export.
        // FIXME: How do we check that?

        // b. Append e.[[ExportName]] to exportedNames.
        exported_names.empend(entry.export_name);
    }

    // 6. For each ExportEntry Record e of module.[[IndirectExportEntries]], do
    for (auto& entry : m_indirect_export_entries) {
        // a. Assert: module provides the direct binding for this export.
        // FIXME: How do we check that?

        // b. Append e.[[ExportName]] to exportedNames.
        exported_names.empend(entry.export_name);
    }

    // 7. For each ExportEntry Record e of module.[[StarExportEntries]], do
    for (auto& entry : m_star_export_entries) {
        // a. Let requestedModule be ? HostResolveImportedModule(module, e.[[ModuleRequest]]).
        auto requested_module = TRY(vm.host_resolve_imported_module(NonnullGCPtr<Module>(*this), entry.module_request()));

        // b. Let starNames be ? requestedModule.GetExportedNames(exportStarSet).
        auto star_names = TRY(requested_module->get_exported_names(vm, export_star_set));

        // c. For each element n of starNames, do
        for (auto& name : star_names) {
            // i. If SameValue(n, "default") is false, then
            if (name != "default"sv) {
                // 1. If n is not an element of exportedNames, then
                if (!exported_names.contains_slow(name)) {
                    // a. Append n to exportedNames.
                    exported_names.empend(name);
                }
            }
        }
    }

    // 8. Return exportedNames.
    return exported_names;
}

// 16.2.1.6.4 InitializeEnvironment ( ), https://tc39.es/ecma262/#sec-source-text-module-record-initialize-environment
ThrowCompletionOr<void> SourceTextModule::initialize_environment(VM& vm)
{
    // 1. For each ExportEntry Record e of module.[[IndirectExportEntries]], do
    for (auto& entry : m_indirect_export_entries) {
        // a. Let resolution be ? module.ResolveExport(e.[[ExportName]]).
        auto resolution = TRY(resolve_export(vm, entry.export_name));
        // b. If resolution is null or ambiguous, throw a SyntaxError exception.
        if (!resolution.is_valid())
            return vm.throw_completion<SyntaxError>(ErrorType::InvalidOrAmbiguousExportEntry, entry.export_name);

        // c. Assert: resolution is a ResolvedBinding Record.
        VERIFY(resolution.is_valid());
    }

    // 2. Assert: All named exports from module are resolvable.
    // Note: We check all the indirect export entries above in step 1 and all
    // the local named exports are resolvable by construction.

    // 3. Let realm be module.[[Realm]].
    // 4. Assert: realm is not undefined.
    // Note: This must be true because we use a reference.

    // 5. Let env be NewModuleEnvironment(realm.[[GlobalEnv]]).
    auto environment = vm.heap().allocate_without_realm<ModuleEnvironment>(&realm().global_environment());

    // 6. Set module.[[Environment]] to env.
    set_environment(environment);

    // 7. For each ImportEntry Record in of module.[[ImportEntries]], do
    for (auto& import_entry : m_import_entries) {
        // a. Let importedModule be ! HostResolveImportedModule(module, in.[[ModuleRequest]]).
        auto imported_module = MUST(vm.host_resolve_imported_module(NonnullGCPtr<Module>(*this), import_entry.module_request()));
        // b. NOTE: The above call cannot fail because imported module requests are a subset of module.[[RequestedModules]], and these have been resolved earlier in this algorithm.

        // c. If in.[[ImportName]] is namespace-object, then
        if (import_entry.is_namespace) {
            // i. Let namespace be ? GetModuleNamespace(importedModule).
            auto* namespace_ = TRY(imported_module->get_module_namespace(vm));

            // ii. Perform ! env.CreateImmutableBinding(in.[[LocalName]], true).
            MUST(environment->create_immutable_binding(vm, import_entry.local_name, true));

            // iii. Perform ! env.InitializeBinding(in.[[LocalName]], namespace, normal).
            MUST(environment->initialize_binding(vm, import_entry.local_name, namespace_, Environment::InitializeBindingHint::Normal));
        }
        // d. Else,
        else {
            // i. Let resolution be ? importedModule.ResolveExport(in.[[ImportName]]).
            auto resolution = TRY(imported_module->resolve_export(vm, import_entry.import_name));

            // ii. If resolution is null or ambiguous, throw a SyntaxError exception.
            if (!resolution.is_valid())
                return vm.throw_completion<SyntaxError>(ErrorType::InvalidOrAmbiguousExportEntry, import_entry.import_name);

            // iii. If resolution.[[BindingName]] is namespace, then
            if (resolution.is_namespace()) {
                // 1. Let namespace be ? GetModuleNamespace(resolution.[[Module]]).
                auto* namespace_ = TRY(resolution.module->get_module_namespace(vm));

                // 2. Perform ! env.CreateImmutableBinding(in.[[LocalName]], true).
                MUST(environment->create_immutable_binding(vm, import_entry.local_name, true));

                // 3. Perform ! env.InitializeBinding(in.[[LocalName]], namespace, normal).
                MUST(environment->initialize_binding(vm, import_entry.local_name, namespace_, Environment::InitializeBindingHint::Normal));
            }
            // iv. Else,
            else {
                // 1. Perform env.CreateImportBinding(in.[[LocalName]], resolution.[[Module]], resolution.[[BindingName]]).
                MUST(environment->create_import_binding(import_entry.local_name, resolution.module, resolution.export_name));
            }
        }
    }

    // 8. Let moduleContext be a new ECMAScript code execution context.
    // Note: this has already been created during the construction of this object.

    // 9. Set the Function of moduleContext to null.

    // 10. Assert: module.[[Realm]] is not undefined.
    // Note: This must be true because we use a reference.

    // 11. Set the Realm of moduleContext to module.[[Realm]].
    m_execution_context.realm = &realm();

    // 12. Set the ScriptOrModule of moduleContext to module.
    m_execution_context.script_or_module = NonnullGCPtr<Module>(*this);

    // 13. Set the VariableEnvironment of moduleContext to module.[[Environment]].
    m_execution_context.variable_environment = environment;

    // 14. Set the LexicalEnvironment of moduleContext to module.[[Environment]].
    m_execution_context.lexical_environment = environment;

    // 15. Set the PrivateEnvironment of moduleContext to null.

    // 16. Set module.[[Context]] to moduleContext.
    // Note: We're already working on that one.

    // 17. Push moduleContext onto the execution context stack; moduleContext is now the running execution context.
    TRY(vm.push_execution_context(m_execution_context, {}));

    // 18. Let code be module.[[ECMAScriptCode]].

    // 19. Let varDeclarations be the VarScopedDeclarations of code.
    // Note: We just loop through them in step 21.

    // 20. Let declaredVarNames be a new empty List.
    Vector<DeprecatedFlyString> declared_var_names;

    // 21. For each element d of varDeclarations, do
    // a. For each element dn of the BoundNames of d, do
    // NOTE: Due to the use of MUST with `create_mutable_binding` and `initialize_binding` below,
    //       an exception should not result from `for_each_var_declared_name`.
    MUST(m_ecmascript_code->for_each_var_declared_name([&](auto const& name) {
        // i. If dn is not an element of declaredVarNames, then
        if (!declared_var_names.contains_slow(name)) {
            // 1. Perform ! env.CreateMutableBinding(dn, false).
            MUST(environment->create_mutable_binding(vm, name, false));

            // 2. Perform ! env.InitializeBinding(dn, undefined, normal).
            MUST(environment->initialize_binding(vm, name, js_undefined(), Environment::InitializeBindingHint::Normal));

            // 3. Append dn to declaredVarNames.
            declared_var_names.empend(name);
        }
    }));

    // 22. Let lexDeclarations be the LexicallyScopedDeclarations of code.
    // Note: We only loop through them in step 24.

    // 23. Let privateEnv be null.
    PrivateEnvironment* private_environment = nullptr;

    // 24. For each element d of lexDeclarations, do
    // NOTE: Due to the use of MUST in the callback, an exception should not result from `for_each_lexically_scoped_declaration`.
    MUST(m_ecmascript_code->for_each_lexically_scoped_declaration([&](Declaration const& declaration) {
        // a. For each element dn of the BoundNames of d, do
        // NOTE: Due to the use of MUST with `create_immutable_binding`, `create_mutable_binding` and `initialize_binding` below,
        //       an exception should not result from `for_each_bound_identifier`.
        MUST(declaration.for_each_bound_identifier([&](auto const& identifier) {
            auto const& name = identifier.string();
            // i. If IsConstantDeclaration of d is true, then
            if (declaration.is_constant_declaration()) {
                // 1. Perform ! env.CreateImmutableBinding(dn, true).
                MUST(environment->create_immutable_binding(vm, name, true));
            }
            // ii. Else,
            else {
                // 1. Perform ! env.CreateMutableBinding(dn, false).
                MUST(environment->create_mutable_binding(vm, name, false));
            }

            // iii. If d is a FunctionDeclaration, a GeneratorDeclaration, an AsyncFunctionDeclaration, or an AsyncGeneratorDeclaration, then
            if (declaration.is_function_declaration()) {
                VERIFY(is<FunctionDeclaration>(declaration));
                auto const& function_declaration = static_cast<FunctionDeclaration const&>(declaration);

                // 1. Let fo be InstantiateFunctionObject of d with arguments env and privateEnv.
                // NOTE: Special case if the function is a default export of an anonymous function
                //       it has name "*default*" but internally should have name "default".
                DeprecatedFlyString function_name = function_declaration.name();
                if (function_name == ExportStatement::local_name_for_default)
                    function_name = "default"sv;
                auto function = ECMAScriptFunctionObject::create(realm(), function_name, function_declaration.source_text(), function_declaration.body(), function_declaration.parameters(), function_declaration.function_length(), function_declaration.local_variables_names(), environment, private_environment, function_declaration.kind(), function_declaration.is_strict_mode(), function_declaration.might_need_arguments_object(), function_declaration.contains_direct_call_to_eval());

                // 2. Perform ! env.InitializeBinding(dn, fo, normal).
                MUST(environment->initialize_binding(vm, name, function, Environment::InitializeBindingHint::Normal));
            }
        }));
    }));

    // Note: The default export name is also part of the local lexical declarations but
    //       instead of making that a special case in the parser we just check it here.
    //       This is only needed for things which are not declarations.
    //       For more info check Parser::parse_export_statement.
    //       Furthermore, that declaration is not constant. so we take 24.a.ii
    if (m_default_export) {
        VERIFY(m_default_export->has_statement());

        auto const& statement = m_default_export->statement();
        if (!is<Declaration>(statement)) {
            auto const& name = m_default_export->entries()[0].local_or_import_name;
            dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Adding default export to lexical declarations: local name: {}, Expression: {}", name, statement.class_name());

            // 1. Perform ! env.CreateMutableBinding(dn, false).
            MUST(environment->create_mutable_binding(vm, name, false));

            // Note: Since this is not a function declaration 24.a.iii never applies
        }
    }

    // 25. Remove moduleContext from the execution context stack.
    vm.pop_execution_context();

    // 26. Return unused.
    return {};
}

// 16.2.1.6.3 ResolveExport ( exportName [ , resolveSet ] ), https://tc39.es/ecma262/#sec-resolveexport
ThrowCompletionOr<ResolvedBinding> SourceTextModule::resolve_export(VM& vm, DeprecatedFlyString const& export_name, Vector<ResolvedBinding> resolve_set)
{
    // 1. If resolveSet is not present, set resolveSet to a new empty List.
    // Note: This is done by the default argument.

    // 2. For each Record { [[Module]], [[ExportName]] } r of resolveSet, do
    for (auto& [type, module, exported_name] : resolve_set) {
        // a. If module and r.[[Module]] are the same Module Record and SameValue(exportName, r.[[ExportName]]) is true, then
        if (module == this && exported_name == export_name) {
            // i. Assert: This is a circular import request.

            // ii. Return null.
            return ResolvedBinding::null();
        }
    }

    // 3. Append the Record { [[Module]]: module, [[ExportName]]: exportName } to resolveSet.
    resolve_set.append({ ResolvedBinding::Type::BindingName, this, export_name });

    // 4. For each ExportEntry Record e of module.[[LocalExportEntries]], do
    for (auto& entry : m_local_export_entries) {
        // a. If SameValue(exportName, e.[[ExportName]]) is true, then
        if (export_name != entry.export_name)
            continue;

        // i. Assert: module provides the direct binding for this export.
        // FIXME: What does this mean?

        // ii. Return ResolvedBinding Record { [[Module]]: module, [[BindingName]]: e.[[LocalName]] }.
        return ResolvedBinding {
            ResolvedBinding::Type::BindingName,
            this,
            entry.local_or_import_name,
        };
    }

    // 5. For each ExportEntry Record e of module.[[IndirectExportEntries]], do
    for (auto& entry : m_indirect_export_entries) {
        // a. If SameValue(exportName, e.[[ExportName]]) is true, then
        if (export_name != entry.export_name)
            continue;

        // i. Let importedModule be ? HostResolveImportedModule(module, e.[[ModuleRequest]]).
        auto imported_module = TRY(vm.host_resolve_imported_module(NonnullGCPtr<Module>(*this), entry.module_request()));

        // ii. If e.[[ImportName]] is all, then
        if (entry.kind == ExportEntry::Kind::ModuleRequestAll) {
            // 1. Assert: module does not provide the direct binding for this export.
            // FIXME: What does this mean? / How do we check this

            // 2. Return ResolvedBinding Record { [[Module]]: importedModule, [[BindingName]]: namespace }.
            return ResolvedBinding {
                ResolvedBinding::Type::Namespace,
                imported_module.ptr(),
                {}
            };
        }
        // iii. Else,
        else {
            // 1. Assert: module imports a specific binding for this export.
            // FIXME: What does this mean? / How do we check this

            // 2. Return ? importedModule.ResolveExport(e.[[ImportName]], resolveSet).
            return imported_module->resolve_export(vm, entry.local_or_import_name, resolve_set);
        }
    }

    // 6. If SameValue(exportName, "default") is true, then
    if (export_name == "default"sv) {
        // a. Assert: A default export was not explicitly defined by this module.
        // FIXME: What does this mean? / How do we check this

        // b. Return null.
        return ResolvedBinding::null();
        // c. NOTE: A default export cannot be provided by an export * from "mod" declaration.
    }

    // 7. Let starResolution be null.
    ResolvedBinding star_resolution = ResolvedBinding::null();

    // 8. For each ExportEntry Record e of module.[[StarExportEntries]], do
    for (auto& entry : m_star_export_entries) {
        // a. Let importedModule be ? HostResolveImportedModule(module, e.[[ModuleRequest]]).
        auto imported_module = TRY(vm.host_resolve_imported_module(NonnullGCPtr<Module>(*this), entry.module_request()));

        // b. Let resolution be ? importedModule.ResolveExport(exportName, resolveSet).
        auto resolution = TRY(imported_module->resolve_export(vm, export_name, resolve_set));

        // c. If resolution is ambiguous, return ambiguous.
        if (resolution.is_ambiguous())
            return ResolvedBinding::ambiguous();

        // d. If resolution is not null, then
        if (resolution.type == ResolvedBinding::Null)
            continue;

        // i. Assert: resolution is a ResolvedBinding Record.
        VERIFY(resolution.is_valid());

        // ii. If starResolution is null, set starResolution to resolution.
        if (star_resolution.type == ResolvedBinding::Null) {
            star_resolution = resolution;
        }
        // iii. Else,
        else {
            // 1. Assert: There is more than one * import that includes the requested name.
            // FIXME: Assert this

            // 2. If resolution.[[Module]] and starResolution.[[Module]] are not the same Module Record, return ambiguous.
            if (resolution.module != star_resolution.module)
                return ResolvedBinding::ambiguous();

            // 3. If resolution.[[BindingName]] is namespace and starResolution.[[BindingName]] is not namespace, or if resolution.[[BindingName]] is not namespace and starResolution.[[BindingName]] is namespace, return ambiguous.
            if (resolution.is_namespace() != star_resolution.is_namespace())
                return ResolvedBinding::ambiguous();

            // 4. If resolution.[[BindingName]] is a String, starResolution.[[BindingName]] is a String, and SameValue(resolution.[[BindingName]], starResolution.[[BindingName]]) is false, return ambiguous.
            if (!resolution.is_namespace() && resolution.export_name != star_resolution.export_name) {
                // Note: Because we know from the previous if that either both are namespaces or both are string we can check just one
                return ResolvedBinding::ambiguous();
            }
        }
    }

    // 9. Return starResolution.
    return star_resolution;
}

// 16.2.1.6.5 ExecuteModule ( [ capability ] ), https://tc39.es/ecma262/#sec-source-text-module-record-execute-module
ThrowCompletionOr<void> SourceTextModule::execute_module(VM& vm, GCPtr<PromiseCapability> capability)
{
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] SourceTextModule::execute_module({}, PromiseCapability @ {})", filename(), capability.ptr());

    // 1. Let moduleContext be a new ECMAScript code execution context.
    ExecutionContext module_context { vm.heap() };

    // Note: This is not in the spec but we require it.
    module_context.is_strict_mode = true;

    // 2. Set the Function of moduleContext to null.

    // 3. Set the Realm of moduleContext to module.[[Realm]].
    module_context.realm = &realm();

    // 4. Set the ScriptOrModule of moduleContext to module.
    module_context.script_or_module = NonnullGCPtr<Module>(*this);

    // 5. Assert: module has been linked and declarations in its module environment have been instantiated.
    VERIFY(m_status != ModuleStatus::Unlinked && m_status != ModuleStatus::Linking && environment());

    // 6. Set the VariableEnvironment of moduleContext to module.[[Environment]].
    module_context.variable_environment = environment();

    // 7. Set the LexicalEnvironment of moduleContext to module.[[Environment]].
    module_context.lexical_environment = environment();

    // 8. Suspend the currently running execution context.
    // FIXME: We don't have suspend yet

    // 9. If module.[[HasTLA]] is false, then
    if (!m_has_top_level_await) {
        // a. Assert: capability is not present.
        VERIFY(capability == nullptr);
        // b. Push moduleContext onto the execution context stack; moduleContext is now the running execution context.
        TRY(vm.push_execution_context(module_context, {}));

        // c. Let result be the result of evaluating module.[[ECMAScriptCode]].
        Completion result;

        if (auto* bytecode_interpreter = vm.bytecode_interpreter_if_exists()) {
            auto maybe_executable = Bytecode::compile(vm, m_ecmascript_code, FunctionKind::Normal, "ShadowRealmEval"sv);
            if (maybe_executable.is_error())
                result = maybe_executable.release_error();
            else {
                auto executable = maybe_executable.release_value();

                auto value_and_frame = bytecode_interpreter->run_and_return_frame(realm(), *executable, nullptr);
                if (value_and_frame.value.is_error()) {
                    result = value_and_frame.value.release_error();
                } else {
                    // Resulting value is in the accumulator.
                    result = value_and_frame.frame->registers.at(0).value_or(js_undefined());
                }
            }
        } else {
            result = m_ecmascript_code->execute(vm.interpreter());
        }

        // d. Let env be moduleContext's LexicalEnvironment.
        auto env = module_context.lexical_environment;
        VERIFY(is<DeclarativeEnvironment>(*env));

        // e. Set result to DisposeResources(env, result).
        result = dispose_resources(vm, static_cast<DeclarativeEnvironment*>(env.ptr()), result);

        // f. Suspend moduleContext and remove it from the execution context stack.
        vm.pop_execution_context();

        // g. Resume the context that is now on the top of the execution context stack as the running execution context.
        // FIXME: We don't have resume yet.

        // h. If result is an abrupt completion, then
        if (result.is_error()) {
            // i. Return ? result.
            return result.release_error();
        }
    }
    // 10. Else,
    else {
        // a. Assert: capability is a PromiseCapability Record.
        VERIFY(capability != nullptr);

        // b. Perform AsyncBlockStart(capability, module.[[ECMAScriptCode]], moduleContext).
        async_block_start<NonnullRefPtr<Statement const>>(vm, m_ecmascript_code, *capability, module_context);
    }

    // 11. Return unused.
    return {};
}

}
