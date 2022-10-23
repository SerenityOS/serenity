/*
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/Scripting/ImportMap.h>
#include <LibWeb/HTML/Scripting/ModuleScript.h>

namespace Web::HTML {

using ModuleCallback = Function<void(JavaScriptModuleScript*)>;

class DescendantFetchingContext : public RefCounted<DescendantFetchingContext> {
public:
    static NonnullRefPtr<DescendantFetchingContext> create() { return adopt_ref(*new DescendantFetchingContext); }

    ~DescendantFetchingContext() = default;

    size_t pending_count() const { return m_pending_count; };
    void set_pending_count(size_t count) { m_pending_count = count; }
    void decrement_pending_count() { --m_pending_count; }

    bool failed() const { return m_failed; }
    void set_failed(bool failed) { m_failed = failed; }

    void on_complete(JavaScriptModuleScript* module_script) { m_on_complete(module_script); };
    void set_on_complete(ModuleCallback on_complete) { m_on_complete = move(on_complete); }

private:
    DescendantFetchingContext() = default;

    size_t m_pending_count { 0 };
    bool m_failed { false };

    ModuleCallback m_on_complete;
};

String module_type_from_module_request(JS::ModuleRequest const&);
WebIDL::ExceptionOr<AK::URL> resolve_module_specifier(Optional<Script&> referring_script, String const& specifier);
WebIDL::ExceptionOr<Optional<AK::URL>> resolve_imports_match(String const& normalized_specifier, Optional<AK::URL> as_url, ModuleSpecifierMap const&);
Optional<AK::URL> resolve_url_like_module_specifier(String const& specifier, AK::URL const& base_url);

void fetch_internal_module_script_graph(JS::ModuleRequest const& module_request, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, Script& referring_script, HashTable<ModuleLocationTuple> const& visited_set, ModuleCallback on_complete);
void fetch_external_module_script_graph(AK::URL const&, EnvironmentSettingsObject& settings_object, ModuleCallback on_complete);
void fetch_inline_module_script_graph(String const& filename, String const& source_text, AK::URL const& base_url, EnvironmentSettingsObject& settings_object, ModuleCallback on_complete);

void fetch_descendants_of_a_module_script(JavaScriptModuleScript& module_script, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, HashTable<ModuleLocationTuple> visited_set, ModuleCallback callback);
void fetch_descendants_of_and_link_a_module_script(JavaScriptModuleScript& module_script, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, HashTable<ModuleLocationTuple> const& visited_set, ModuleCallback on_complete);

enum class TopLevelModule {
    Yes,
    No
};

void fetch_single_module_script(AK::URL const&, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, EnvironmentSettingsObject& module_map_settings_object, AK::URL const& referrer, Optional<JS::ModuleRequest> const&, TopLevelModule, ModuleCallback callback);

}
