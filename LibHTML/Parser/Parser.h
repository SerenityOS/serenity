#pragma once

#include <AK/Retained.h>
#include <LibHTML/DOM/Document.h>

Retained<Document> parse(const String& html);

