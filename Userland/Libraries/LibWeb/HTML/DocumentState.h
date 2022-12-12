/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Heap/Cell.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/PolicyContainers.h>
#include <LibWeb/ReferrerPolicy/ReferrerPolicy.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-2
class DocumentState final : public JS::Cell {
    JS_CELL(DocumentState, JS::Cell);

public:
    virtual ~DocumentState();

    enum class Client {
        Tag,
    };

    enum class NoReferrer {
        Tag,
    };

    [[nodiscard]] JS::GCPtr<DOM::Document> document() const { return m_document; }
    void set_document(JS::GCPtr<DOM::Document> document) { m_document = document; }

    [[nodiscard]] Variant<PolicyContainer, Client> history_policy_container() const { return m_history_policy_container; }
    void set_history_policy_container(Variant<PolicyContainer, Client> history_policy_container) { m_history_policy_container = move(history_policy_container); }

    [[nodiscard]] Variant<NoReferrer, Client, AK::URL> request_referrer() const { return m_request_referrer; }
    void set_request_referrer(Variant<NoReferrer, Client, AK::URL> request_referrer) { m_request_referrer = move(request_referrer); }

    [[nodiscard]] ReferrerPolicy::ReferrerPolicy request_referrer_policy() const { return m_request_referrer_policy; }
    void set_request_referrer_policy(ReferrerPolicy::ReferrerPolicy request_referrer_policy) { m_request_referrer_policy = move(request_referrer_policy); }

    [[nodiscard]] Optional<HTML::Origin> initiator_origin() const { return m_initiator_origin; }
    void set_initiator_origin(Optional<HTML::Origin> initiator_origin) { m_initiator_origin = move(initiator_origin); }

    [[nodiscard]] Optional<HTML::Origin> origin() const { return m_origin; }
    void set_origin(Optional<HTML::Origin> origin) { m_origin = move(origin); }

    [[nodiscard]] bool reload_pending() const { return m_reload_pending; }
    void set_reload_pending(bool reload_pending) { m_reload_pending = reload_pending; }

    [[nodiscard]] bool ever_populated() const { return m_ever_populated; }
    void set_ever_populated(bool ever_populated) { m_ever_populated = ever_populated; }

    [[nodiscard]] String ever_navigable_target_name() const { return m_navigable_target_name; }
    void set_navigable_target_name(String navigable_target_name) { m_navigable_target_name = navigable_target_name; }

private:
    DocumentState();

    void visit_edges(Cell::Visitor&) override;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-document
    JS::GCPtr<DOM::Document> m_document;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-history-policy-container
    Variant<PolicyContainer, Client> m_history_policy_container { Client::Tag };

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-request-referrer
    Variant<NoReferrer, Client, AK::URL> m_request_referrer { Client::Tag };

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-request-referrer-policy
    ReferrerPolicy::ReferrerPolicy m_request_referrer_policy { ReferrerPolicy::DEFAULT_REFERRER_POLICY };

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-initiator-origin
    Optional<HTML::Origin> m_initiator_origin;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-origin
    Optional<HTML::Origin> m_origin;

    // FIXME: https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-nested-histories

    // FIXME: https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-resource

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-reload-pending
    bool m_reload_pending { false };

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-ever-populated
    bool m_ever_populated { false };

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#document-state-nav-target-name
    String m_navigable_target_name;
};

}
