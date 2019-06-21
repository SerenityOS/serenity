#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibHTML/DOM/Document.h>

NonnullRefPtr<Document> parse(const String& html);

