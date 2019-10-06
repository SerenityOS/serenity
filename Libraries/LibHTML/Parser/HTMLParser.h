#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibHTML/DOM/Document.h>

NonnullRefPtr<Document> parse_html(const String&, const URL& = URL());

