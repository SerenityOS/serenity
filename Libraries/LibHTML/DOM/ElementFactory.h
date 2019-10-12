#pragma once

#include <LibHTML/DOM/Element.h>

NonnullRefPtr<Element> create_element(Document&, const String& tag_name);
