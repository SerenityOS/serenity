/*
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/HTML/CORSSettingAttribute.h>
#include <LibWeb/HTML/Scripting/ImportMap.h>
#include <LibWeb/HTML/Scripting/ModuleMap.h>
#include <LibWeb/HTML/Scripting/ModuleScript.h>
#include <LibWeb/ReferrerPolicy/ReferrerPolicy.h>

namespace Web::HTML {

using OnFetchScriptComplete = JS::SafeFunction<void(JS::GCPtr<Script>)>;

// https://html.spec.whatwg.org/multipage/webappapis.html#script-fetch-options
struct ScriptFetchOptions {
    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-script-fetch-options-nonce
    String cryptographic_nonce {};

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-script-fetch-options-integrity
    String integrity_metadata {};

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-script-fetch-options-parser
    Optional<Fetch::Infrastructure::Request::ParserMetadata> parser_metadata;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-script-fetch-options-credentials
    Fetch::Infrastructure::Request::CredentialsMode credentials_mode { Fetch::Infrastructure::Request::CredentialsMode::SameOrigin };

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-script-fetch-options-referrer-policy
    Optional<ReferrerPolicy::ReferrerPolicy> referrer_policy;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-script-fetch-options-render-blocking
    bool render_blocking { false };

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-script-fetch-options-fetch-priority
    Fetch::Infrastructure::Request::Priority fetch_priority {};
};

class DescendantFetchingContext : public RefCounted<DescendantFetchingContext> {
public:
    static NonnullRefPtr<DescendantFetchingContext> create() { return adopt_ref(*new DescendantFetchingContext); }

    ~DescendantFetchingContext() = default;

    size_t pending_count() const { return m_pending_count; }
    void set_pending_count(size_t count) { m_pending_count = count; }
    void decrement_pending_count() { --m_pending_count; }

    bool failed() const { return m_failed; }
    void set_failed(bool failed) { m_failed = failed; }

    void on_complete(JavaScriptModuleScript* module_script) { m_on_complete(module_script); }
    void set_on_complete(OnFetchScriptComplete on_complete) { m_on_complete = move(on_complete); }

private:
    DescendantFetchingContext() = default;

    size_t m_pending_count { 0 };
    bool m_failed { false };

    OnFetchScriptComplete m_on_complete;
};

DeprecatedString module_type_from_module_request(JS::ModuleRequest const&);
WebIDL::ExceptionOr<AK::URL> resolve_module_specifier(Optional<Script&> referring_script, DeprecatedString const& specifier);
WebIDL::ExceptionOr<Optional<AK::URL>> resolve_imports_match(DeprecatedString const& normalized_specifier, Optional<AK::URL> as_url, ModuleSpecifierMap const&);
Optional<AK::URL> resolve_url_like_module_specifier(DeprecatedString const& specifier, AK::URL const& base_url);

WebIDL::ExceptionOr<void> fetch_classic_script(JS::NonnullGCPtr<HTMLScriptElement>, AK::URL const&, EnvironmentSettingsObject& settings_object, ScriptFetchOptions options, CORSSettingAttribute cors_setting, String character_encoding, OnFetchScriptComplete on_complete);
void fetch_internal_module_script_graph(JS::Realm&, JS::ModuleRequest const& module_request, EnvironmentSettingsObject& fetch_client_settings_object, Fetch::Infrastructure::Request::Destination, ScriptFetchOptions const&, Script& referring_script, HashTable<ModuleLocationTuple> const& visited_set, OnFetchScriptComplete on_complete);
void fetch_external_module_script_graph(JS::Realm&, AK::URL const&, EnvironmentSettingsObject& settings_object, ScriptFetchOptions const&, OnFetchScriptComplete on_complete);
void fetch_inline_module_script_graph(JS::Realm& realm, DeprecatedString const& filename, DeprecatedString const& source_text, AK::URL const& base_url, EnvironmentSettingsObject& settings_object, OnFetchScriptComplete on_complete);

void fetch_descendants_of_a_module_script(JS::Realm&, JavaScriptModuleScript& module_script, EnvironmentSettingsObject& fetch_client_settings_object, Fetch::Infrastructure::Request::Destination, HashTable<ModuleLocationTuple> visited_set, OnFetchScriptComplete callback);
void fetch_descendants_of_and_link_a_module_script(JS::Realm&, JavaScriptModuleScript& module_script, EnvironmentSettingsObject& fetch_client_settings_object, Fetch::Infrastructure::Request::Destination, HashTable<ModuleLocationTuple> const& visited_set, OnFetchScriptComplete on_complete);

enum class TopLevelModule {
    Yes,
    No
};

void fetch_single_module_script(JS::Realm&, AK::URL const&, EnvironmentSettingsObject& fetch_client, Fetch::Infrastructure::Request::Destination, ScriptFetchOptions const&, EnvironmentSettingsObject& settings_object, Web::Fetch::Infrastructure::Request::ReferrerType const&, Optional<JS::ModuleRequest> const&, TopLevelModule, OnFetchScriptComplete callback);

}
