/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Document.h>

namespace Web {

bool build_xml_document(DOM::Document& document, ByteBuffer const& data);
bool parse_document(DOM::Document& document, ByteBuffer const& data);
JS::GCPtr<DOM::Document> load_document(Optional<HTML::NavigationParams> navigation_params);
JS::GCPtr<DOM::Document> create_document_for_inline_content(JS::GCPtr<HTML::Navigable> navigable, Optional<String> navigation_id, StringView content_html);

}
