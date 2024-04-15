/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/DocumentState.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(DocumentState);

DocumentState::DocumentState() = default;

DocumentState::~DocumentState() = default;

JS::NonnullGCPtr<DocumentState> DocumentState::clone() const
{
    JS::NonnullGCPtr<DocumentState> cloned = *heap().allocate_without_realm<DocumentState>();
    cloned->m_document = m_document;
    cloned->m_history_policy_container = m_history_policy_container;
    cloned->m_request_referrer = m_request_referrer;
    cloned->m_request_referrer_policy = m_request_referrer_policy;
    cloned->m_initiator_origin = m_initiator_origin;
    cloned->m_origin = m_origin;
    cloned->m_about_base_url = m_about_base_url;
    cloned->m_nested_histories = m_nested_histories;
    cloned->m_resource = m_resource;
    cloned->m_reload_pending = m_reload_pending;
    cloned->m_ever_populated = m_ever_populated;
    cloned->m_navigable_target_name = m_navigable_target_name;
    return cloned;
}

void DocumentState::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_document);
    for (auto& nested_history : m_nested_histories) {
        visitor.visit(nested_history.entries);
    }
}

}
