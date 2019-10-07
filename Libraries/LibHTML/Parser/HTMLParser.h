#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibHTML/DOM/Document.h>

NonnullRefPtr<Document> parse_html(const StringView&, const URL& = URL());

