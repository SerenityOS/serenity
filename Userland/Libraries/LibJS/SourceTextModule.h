/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/AST.h>
#include <LibJS/CyclicModule.h>
#include <LibJS/Forward.h>
#include <LibJS/Parser.h>

namespace JS {

// 16.2.1.6 Source Text Module Records, https://tc39.es/ecma262/#sec-source-text-module-records
class SourceTextModule final : public CyclicModule {
    JS_CELL(SourceTextModule, CyclicModule);

public:
    using ImportEntry = ImportStatement::ImportEntry;
    using ExportEntry = ExportStatement::ExportEntry;

    static Result<NonnullGCPtr<SourceTextModule>, Vector<Parser::Error>> parse(StringView source_text, Realm&, StringView filename = {}, Script::HostDefined* host_defined = nullptr);

    Program const& parse_node() const { return *m_ecmascript_code; }

    virtual ThrowCompletionOr<Vector<FlyString>> get_exported_names(VM& vm, Vector<Module*> export_star_set) override;
    virtual ThrowCompletionOr<ResolvedBinding> resolve_export(VM& vm, FlyString const& export_name, Vector<ResolvedBinding> resolve_set = {}) override;

    Object* import_meta() { return m_import_meta; }
    void set_import_meta(Badge<MetaProperty>, Object* import_meta) { m_import_meta = import_meta; }

protected:
    virtual ThrowCompletionOr<void> initialize_environment(VM& vm) override;
    virtual ThrowCompletionOr<void> execute_module(VM& vm, GCPtr<PromiseCapability> capability) override;

private:
    SourceTextModule(Realm&, StringView filename, Script::HostDefined* host_defined, bool has_top_level_await, NonnullRefPtr<Program> body, Vector<ModuleRequest> requested_modules,
        Vector<ImportEntry> import_entries, Vector<ExportEntry> local_export_entries,
        Vector<ExportEntry> indirect_export_entries, Vector<ExportEntry> star_export_entries,
        RefPtr<ExportStatement> default_export);

    virtual void visit_edges(Cell::Visitor&) override;

    NonnullRefPtr<Program> m_ecmascript_code;      // [[ECMAScriptCode]]
    ExecutionContext m_execution_context;          // [[Context]]
    GCPtr<Object> m_import_meta;                   // [[ImportMeta]]
    Vector<ImportEntry> m_import_entries;          // [[ImportEntries]]
    Vector<ExportEntry> m_local_export_entries;    // [[LocalExportEntries]]
    Vector<ExportEntry> m_indirect_export_entries; // [[IndirectExportEntries]]
    Vector<ExportEntry> m_star_export_entries;     // [[StarExportEntries]]

    RefPtr<ExportStatement> m_default_export; // Note: Not from the spec
};

}
