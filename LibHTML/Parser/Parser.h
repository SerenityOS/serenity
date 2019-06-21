#pragma once

#include <AK/Retained.h>
#include <LibHTML/DOM/Document.h>

NonnullRefPtr<Document> parse(const String& html);

