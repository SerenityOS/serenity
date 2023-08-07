/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Forward.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Scripting/ModuleMap.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#environment
struct Environment {
    virtual ~Environment() = default;

    // An id https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-id
    DeprecatedString id;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-creation-url
    AK::URL creation_url;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-top-level-creation-url
    AK::URL top_level_creation_url;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-top-level-origin
    Origin top_level_origin;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-target-browsing-context
    JS::GCPtr<BrowsingContext> target_browsing_context;

    // FIXME: An active service worker https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-active-service-worker

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-execution-ready-flag
    bool execution_ready { false };
};

enum class CanUseCrossOriginIsolatedAPIs {
    No,
    Yes,
};

enum class RunScriptDecision {
    Run,
    DoNotRun,
};

// https://html.spec.whatwg.org/multipage/webappapis.html#environment-settings-object
struct EnvironmentSettingsObject
    : public JS::Cell
    , public Environment {
    JS_CELL(EnvironmentSettingsObject, JS::Cell);

    virtual ~EnvironmentSettingsObject() override;
    virtual void initialize(JS::Realm&) override;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-target-browsing-context
    JS::ExecutionContext& realm_execution_context();

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-settings-object-module-map
    ModuleMap& module_map();

    // https://html.spec.whatwg.org/multipage/webappapis.html#responsible-document
    virtual JS::GCPtr<DOM::Document> responsible_document() = 0;

    // https://html.spec.whatwg.org/multipage/webappapis.html#api-url-character-encoding
    virtual DeprecatedString api_url_character_encoding() = 0;

    // https://html.spec.whatwg.org/multipage/webappapis.html#api-base-url
    virtual AK::URL api_base_url() = 0;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-settings-object-origin
    virtual Origin origin() = 0;

    // A policy container https://html.spec.whatwg.org/multipage/webappapis.html#concept-settings-object-policy-container
    virtual PolicyContainer policy_container() = 0;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-settings-object-cross-origin-isolated-capability
    virtual CanUseCrossOriginIsolatedAPIs cross_origin_isolated_capability() = 0;

    AK::URL parse_url(StringView);

    JS::Realm& realm();
    JS::Object& global_object();
    EventLoop& responsible_event_loop();

    RunScriptDecision can_run_script();
    void prepare_to_run_script();
    void clean_up_after_running_script();

    void prepare_to_run_callback();
    void clean_up_after_running_callback();

    void push_onto_outstanding_rejected_promises_weak_set(JS::Promise*);

    // Returns true if removed, false otherwise.
    bool remove_from_outstanding_rejected_promises_weak_set(JS::Promise*);

    void push_onto_about_to_be_notified_rejected_promises_list(JS::NonnullGCPtr<JS::Promise>);

    // Returns true if removed, false otherwise.
    bool remove_from_about_to_be_notified_rejected_promises_list(JS::NonnullGCPtr<JS::Promise>);

    void notify_about_rejected_promises(Badge<EventLoop>);

    bool is_scripting_enabled() const;
    bool is_scripting_disabled() const;

    bool module_type_allowed(DeprecatedString const& module_type) const;

    void disallow_further_import_maps();

protected:
    explicit EnvironmentSettingsObject(NonnullOwnPtr<JS::ExecutionContext>);

    virtual void visit_edges(Cell::Visitor&) override;

private:
    NonnullOwnPtr<JS::ExecutionContext> m_realm_execution_context;
    JS::GCPtr<ModuleMap> m_module_map;

    EventLoop* m_responsible_event_loop { nullptr };

    // https://html.spec.whatwg.org/multipage/webappapis.html#outstanding-rejected-promises-weak-set
    // The outstanding rejected promises weak set must not create strong references to any of its members, and implementations are free to limit its size, e.g. by removing old entries from it when new ones are added.
    Vector<JS::GCPtr<JS::Promise>> m_outstanding_rejected_promises_weak_set;

    // https://html.spec.whatwg.org/multipage/webappapis.html#about-to-be-notified-rejected-promises-list
    Vector<JS::Handle<JS::Promise>> m_about_to_be_notified_rejected_promises_list;
};

EnvironmentSettingsObject& incumbent_settings_object();
JS::Realm& incumbent_realm();
JS::Object& incumbent_global_object();
EnvironmentSettingsObject& current_settings_object();
JS::Object& current_global_object();
JS::Realm& relevant_realm(JS::Object const&);
EnvironmentSettingsObject& relevant_settings_object(JS::Object const&);
EnvironmentSettingsObject& relevant_settings_object(DOM::Node const&);
JS::Object& relevant_global_object(JS::Object const&);
JS::Realm& entry_realm();
EnvironmentSettingsObject& entry_settings_object();
JS::Object& entry_global_object();
JS::VM& relevant_agent(JS::Object const&);
[[nodiscard]] bool is_secure_context(Environment const&);
[[nodiscard]] bool is_non_secure_context(Environment const&);

}
