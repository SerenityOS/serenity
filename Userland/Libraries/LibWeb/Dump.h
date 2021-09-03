/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Forward.h>
#include <LibWeb/Forward.h>

namespace Web {

void dump_tree(StringBuilder&, DOM::Node const&);
void dump_tree(DOM::Node const&);
void dump_tree(StringBuilder&, Layout::Node const&, bool show_box_model = false, bool show_specified_style = false, bool colorize = false);
void dump_tree(Layout::Node const&, bool show_box_model = false, bool show_specified_style = false);
void dump_sheet(StringBuilder&, CSS::StyleSheet const&);
void dump_sheet(CSS::StyleSheet const&);
void dump_rule(StringBuilder&, CSS::CSSRule const&);
void dump_rule(CSS::CSSRule const&);
void dump_style_rule(StringBuilder&, CSS::CSSStyleRule const&);
void dump_import_rule(StringBuilder&, CSS::CSSImportRule const&);
void dump_selector(StringBuilder&, CSS::Selector const&);
void dump_selector(CSS::Selector const&);

}
