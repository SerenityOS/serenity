/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Forward.h>

namespace Web {

void dump_tree(StringBuilder&, const DOM::Node&);
void dump_tree(const DOM::Node&);
void dump_tree(StringBuilder&, const Layout::Node&, bool show_box_model = false, bool show_specified_style = false, bool colorize = false);
void dump_tree(const Layout::Node&, bool show_box_model = false, bool show_specified_style = false);
void dump_sheet(StringBuilder&, const CSS::StyleSheet&);
void dump_sheet(const CSS::StyleSheet&);
void dump_rule(StringBuilder&, const CSS::CSSRule&);
void dump_rule(const CSS::CSSRule&);
void dump_style_rule(StringBuilder&, const CSS::CSSStyleRule&);
void dump_import_rule(StringBuilder&, const CSS::CSSImportRule&);
void dump_selector(StringBuilder&, const CSS::Selector&);
void dump_selector(const CSS::Selector&);

}
