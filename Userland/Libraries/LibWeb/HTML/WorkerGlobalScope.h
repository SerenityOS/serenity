/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/WorkerLocation.h>
#include <LibWeb/HTML/WorkerNavigator.h>

#define ENUMERATE_WORKER_GLOBAL_SCOPE_EVENT_HANDLERS(E)       \
    E(onerror, HTML::EventNames::error)                       \
    E(onlanguagechange, HTML::EventNames::languagechange)     \
    E(ononline, HTML::EventNames::online)                     \
    E(onoffline, HTML::EventNames::offline)                   \
    E(onrejectionhandled, HTML::EventNames::rejectionhandled) \
    E(onunhandledrejection, HTML::EventNames::unhandledrejection)

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/workers.html#the-workerglobalscope-common-interface
// WorkerGlobalScope is the base class of each real WorkerGlobalScope that will be created when the
// user agent runs the run a worker algorithm.
class WorkerGlobalScope
    : public RefCounted<WorkerGlobalScope>
    , public DOM::EventTarget
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::WorkerGlobalScopeWrapper;

    using RefCounted::ref;
    using RefCounted::unref;

    virtual ~WorkerGlobalScope() override;

    // ^EventTarget
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    // Following methods are from the WorkerGlobalScope IDL definition
    // https://html.spec.whatwg.org/multipage/workers.html#the-workerglobalscope-common-interface

    // https://html.spec.whatwg.org/multipage/workers.html#dom-workerglobalscope-self
    NonnullRefPtr<WorkerGlobalScope const> self() const { return *this; }

    NonnullRefPtr<WorkerLocation const> location() const;
    NonnullRefPtr<WorkerNavigator const> navigator() const;
    DOM::ExceptionOr<void> import_scripts(Vector<String> urls);

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                  \
    void set_##attribute_name(Optional<Bindings::CallbackType>); \
    Bindings::CallbackType* attribute_name();
    ENUMERATE_WORKER_GLOBAL_SCOPE_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

    // Following methods are from the WindowOrWorkerGlobalScope mixin
    // https://html.spec.whatwg.org/multipage/webappapis.html#windoworworkerglobalscope-mixin

    String origin() const;
    bool is_secure_context() const;
    bool cross_origin_isolated() const;
    DOM::ExceptionOr<String> btoa(String const& data) const;
    DOM::ExceptionOr<String> atob(String const& data) const;

    // Non-IDL public methods

    AK::URL const& url() const { return m_url.value(); }
    void set_url(AK::URL const& url) { m_url = url; }

    // Spec note: While the WorkerLocation object is created after the WorkerGlobalScope object,
    //            this is not problematic as it cannot be observed from script.
    void set_location(NonnullRefPtr<WorkerLocation> loc) { m_location = move(loc); }

protected:
    explicit WorkerGlobalScope();

private:
    RefPtr<WorkerLocation> m_location;

    // FIXME: Implement WorkerNavigator according to the spec
    NonnullRefPtr<WorkerNavigator> m_navigator;

    // FIXME: Add all these internal slots

    // https://html.spec.whatwg.org/multipage/workers.html#concept-WorkerGlobalScope-owner-set
    // A WorkerGlobalScope object has an associated owner set (a set of Document and WorkerGlobalScope objects). It is initially empty and populated when the worker is created or obtained.
    //     Note: It is a set, instead of a single owner, to accommodate SharedWorkerGlobalScope objects.

    // https://html.spec.whatwg.org/multipage/workers.html#concept-workerglobalscope-type
    // A WorkerGlobalScope object has an associated type ("classic" or "module"). It is set during creation.

    // https://html.spec.whatwg.org/multipage/workers.html#concept-workerglobalscope-url
    // A WorkerGlobalScope object has an associated url (null or a URL). It is initially null.
    Optional<AK::URL> m_url;

    // https://html.spec.whatwg.org/multipage/workers.html#concept-workerglobalscope-name
    // A WorkerGlobalScope object has an associated name (a string). It is set during creation.
    //  Note: The name can have different semantics for each subclass of WorkerGlobalScope.
    //        For DedicatedWorkerGlobalScope instances, it is simply a developer-supplied name, useful mostly for debugging purposes.
    //        For SharedWorkerGlobalScope instances, it allows obtaining a reference to a common shared worker via the SharedWorker() constructor.
    //        For ServiceWorkerGlobalScope objects, it doesn't make sense (and as such isn't exposed through the JavaScript API at all).

    // https://html.spec.whatwg.org/multipage/workers.html#concept-workerglobalscope-policy-container
    // A WorkerGlobalScope object has an associated policy container (a policy container). It is initially a new policy container.

    // https://html.spec.whatwg.org/multipage/workers.html#concept-workerglobalscope-embedder-policy
    // A WorkerGlobalScope object has an associated embedder policy (an embedder policy).

    // https://html.spec.whatwg.org/multipage/workers.html#concept-workerglobalscope-module-map
    // A WorkerGlobalScope object has an associated module map. It is a module map, initially empty.

    // https://html.spec.whatwg.org/multipage/workers.html#concept-workerglobalscope-cross-origin-isolated-capability
    bool m_cross_origin_isolated_capability { false };
};

}
