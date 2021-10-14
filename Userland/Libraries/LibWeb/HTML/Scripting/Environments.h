/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/Origin.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#environment
struct Environment {
    // FIXME: An id https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-id

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-creation-url
    AK::URL creation_url;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-top-level-creation-url
    AK::URL top_level_creation_url;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-top-level-origin
    Origin top_level_origin;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-target-browsing-context
    RefPtr<BrowsingContext> target_browsing_context;

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
    : public Environment
    , public JS::Realm::HostDefined {
    virtual ~EnvironmentSettingsObject() override;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-environment-target-browsing-context
    JS::ExecutionContext& realm_execution_context();

    // FIXME: A module map https://html.spec.whatwg.org/multipage/webappapis.html#concept-settings-object-module-map

    // https://html.spec.whatwg.org/multipage/webappapis.html#responsible-document
    virtual RefPtr<DOM::Document> responsible_document() = 0;

    // https://html.spec.whatwg.org/multipage/webappapis.html#api-url-character-encoding
    virtual String api_url_character_encoding() = 0;

    // https://html.spec.whatwg.org/multipage/webappapis.html#api-base-url
    virtual AK::URL api_base_url() = 0;

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-settings-object-origin
    virtual Origin origin() = 0;

    // FIXME: A policy container https://html.spec.whatwg.org/multipage/webappapis.html#concept-settings-object-policy-container

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-settings-object-cross-origin-isolated-capability
    virtual CanUseCrossOriginIsolatedAPIs cross_origin_isolated_capability() = 0;

    JS::Realm& realm();
    JS::GlobalObject& global_object();
    EventLoop& responsible_event_loop();

    RunScriptDecision can_run_script();
    void prepare_to_run_script();
    void clean_up_after_running_script();

    void prepare_to_run_callback();
    void clean_up_after_running_callback();

    void push_onto_outstanding_rejected_promises_weak_set(JS::Promise*);

    // Returns true if removed, false otherwise.
    bool remove_from_outstanding_rejected_promises_weak_set(JS::Promise*);

    void push_onto_about_to_be_notified_rejected_promises_list(JS::Handle<JS::Promise>);

    // Returns true if removed, false otherwise.
    bool remove_from_about_to_be_notified_rejected_promises_list(JS::Promise*);

    void notify_about_rejected_promises(Badge<EventLoop>);

protected:
    explicit EnvironmentSettingsObject(JS::ExecutionContext& realm_execution_context);

private:
    JS::ExecutionContext& m_realm_execution_context;
    EventLoop* m_responsible_event_loop { nullptr };

    // https://html.spec.whatwg.org/multipage/webappapis.html#outstanding-rejected-promises-weak-set
    // The outstanding rejected promises weak set must not create strong references to any of its members, and implementations are free to limit its size, e.g. by removing old entries from it when new ones are added.
    Vector<JS::Promise*> m_outstanding_rejected_promises_weak_set;

    // https://html.spec.whatwg.org/multipage/webappapis.html#about-to-be-notified-rejected-promises-list
    Vector<JS::Handle<JS::Promise>> m_about_to_be_notified_rejected_promises_list;
};

EnvironmentSettingsObject& incumbent_settings_object();
JS::Realm& incumbent_realm();
JS::GlobalObject& incumbent_global_object();

}
