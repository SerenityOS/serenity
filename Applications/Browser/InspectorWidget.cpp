#include "InspectorWidget.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GTabWidget.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOMTreeModel.h>
#include <LibHTML/StylePropertiesModel.h>

InspectorWidget::InspectorWidget(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    auto splitter = GSplitter::construct(Orientation::Vertical, this);
    m_dom_tree_view = GTreeView::construct(splitter);
    m_dom_tree_view->on_selection = [this](auto& index) {
        auto* node = static_cast<Node*>(index.internal_data());
        node->document().set_inspected_node(node);
        if (node->is_element()) {
            auto element = to<Element>(*node);
	    if (element.resolved_style())
            m_style_table_view->set_model(StylePropertiesModel::create(*element.resolved_style()));
	    if (element.layout_node() && element.layout_node()->has_style())
                m_computed_style_table_view->set_model(StylePropertiesModel::create(element.layout_node()->style()));
        } else {
            m_style_table_view->set_model(nullptr);
            m_computed_style_table_view->set_model(nullptr);
        }
    };
    m_style_table_view = GTableView::construct(nullptr);
    m_style_table_view->set_size_columns_to_fit_content(true);

    m_computed_style_table_view = GTableView::construct(nullptr);
    m_computed_style_table_view->set_size_columns_to_fit_content(true);

    auto tabwidget = GTabWidget::construct(splitter);
    tabwidget->add_widget("Styles", m_style_table_view);
    tabwidget->add_widget("Computed", m_computed_style_table_view);
}

InspectorWidget::~InspectorWidget()
{
}

void InspectorWidget::set_document(Document* document)
{
    if (m_document == document)
        return;
    m_document = document;
    m_dom_tree_view->set_model(DOMTreeModel::create(*document));
}
