#pragma once

#include <AK/Retained.h>
#include <LibHTML/Document.h>

Retained<Document> parse(const String& html);

