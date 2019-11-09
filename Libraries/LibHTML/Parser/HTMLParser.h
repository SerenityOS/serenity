#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibHTML/DOM/Document.h>

class DocumentFragment;

RefPtr<Document> parse_html_document(const StringView&, const URL& = URL());
RefPtr<DocumentFragment> parse_html_fragment(Document&, const StringView&);
String escape_html_entities(const StringView&);
