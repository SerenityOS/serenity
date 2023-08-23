/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/SessionHistoryEntry.h>

namespace Web::HTML {

void SessionHistoryEntry::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(document_state);
    visitor.visit(original_source_browsing_context);
}

SessionHistoryEntry::SessionHistoryEntry()
    : classic_history_api_state(MUST(structured_serialize_for_storage(vm(), JS::js_null())))
    , navigation_api_state(MUST(structured_serialize_for_storage(vm(), JS::js_undefined())))
    , navigation_api_key(MUST(Crypto::generate_random_uuid()))
    , navigation_api_id(MUST(Crypto::generate_random_uuid()))
{
}

}
