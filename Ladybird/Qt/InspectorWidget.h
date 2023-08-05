/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ModelTranslator.h"
#include "WebContentView.h"
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <LibWeb/CSS/Selector.h>
#include <QWidget>

class QTreeView;
class QTableView;

namespace Ladybird {

class InspectorWidget final : public QWidget {
    Q_OBJECT
public:
    InspectorWidget();
    virtual ~InspectorWidget() = default;

    struct Selection {
        i32 dom_node_id { 0 };
        Optional<Web::CSS::Selector::PseudoElement> pseudo_element {};
        bool operator==(Selection const& other) const = default;
    };

    bool dom_loaded() const { return m_dom_loaded; }

    void set_selection(Selection);
    void clear_selection();

    void select_default_node();

    void clear_dom_json();
    void set_dom_json(StringView dom_json);

    void set_accessibility_json(StringView accessibility_json);

    void load_style_json(StringView computed_style_json, StringView resolved_style_json, StringView custom_properties_json);
    void clear_style_json();

    Function<ErrorOr<WebContentView::DOMNodeProperties>(i32, Optional<Web::CSS::Selector::PseudoElement>)> on_dom_node_inspected;
    Function<void()> on_close;

private:
    void set_selection(GUI::ModelIndex);
    void closeEvent(QCloseEvent*) override;

    Selection m_selection;

    ModelTranslator m_dom_model {};
    ModelTranslator m_accessibility_model {};
    ModelTranslator m_computed_style_model {};
    ModelTranslator m_resolved_style_model {};
    ModelTranslator m_custom_properties_model {};

    QTreeView* m_dom_tree_view { nullptr };

    bool m_dom_loaded { false };
    Optional<Selection> m_pending_selection {};
};

}
