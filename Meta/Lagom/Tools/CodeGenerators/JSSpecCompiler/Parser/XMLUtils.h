/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibXML/Forward.h>

#include "Parser/ParseError.h"

namespace JSSpecCompiler {

struct IgnoreComments {
    ParseErrorOr<void> operator()(XML::Node::Comment const&) { return {}; }
};

inline constexpr IgnoreComments ignore_comments {};

bool contains_empty_text(XML::Node const* node);

ParseErrorOr<StringView> get_attribute_by_name(XML::Node const* node, StringView attribute_name);

ParseErrorOr<StringView> get_text_contents(XML::Node const* node);

ParseErrorOr<XML::Node const*> get_only_child(XML::Node const* element, StringView tag_name);

}
