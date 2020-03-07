/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "InspectorWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/TabWidget.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOMTreeModel.h>
#include <LibHTML/StylePropertiesModel.h>

InspectorWidget::InspectorWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    auto& splitter = add<GUI::VerticalSplitter>();
    m_dom_tree_view = splitter.add<GUI::TreeView>();
    m_dom_tree_view->on_selection = [this](auto& index) {
        auto* node = static_cast<Web::Node*>(index.internal_data());
        node->document().set_inspected_node(node);
        if (node->is_element()) {
            auto element = Web::to<Web::Element>(*node);
            if (element.resolved_style()) {
                m_style_table_view->set_model(Web::StylePropertiesModel::create(*element.resolved_style()));
                m_computed_style_table_view->set_model(Web::StylePropertiesModel::create(*element.computed_style()));
            }
        } else {
            m_style_table_view->set_model(nullptr);
            m_computed_style_table_view->set_model(nullptr);
        }
    };

    auto& tab_widget = splitter.add<GUI::TabWidget>();

    m_style_table_view = tab_widget.add_tab<GUI::TableView>("Styles");
    m_style_table_view->set_size_columns_to_fit_content(true);

    m_computed_style_table_view = tab_widget.add_tab<GUI::TableView>("Computed");
    m_computed_style_table_view->set_size_columns_to_fit_content(true);
}

InspectorWidget::~InspectorWidget()
{
}

void InspectorWidget::set_document(Web::Document* document)
{
    if (m_document == document)
        return;
    m_document = document;
    m_dom_tree_view->set_model(Web::DOMTreeModel::create(*document));
}
