/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ModelTranslator.h"
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <LibWeb/CSS/Selector.h>
#include <QWidget>

class QTreeView;

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

    void clear_dom_json();
    void set_dom_json(StringView dom_json);

    Function<void(i32, Optional<Web::CSS::Selector::PseudoElement>)> on_dom_node_inspected;
    Function<void()> on_close;

private:
    void set_selection(GUI::ModelIndex);
    void closeEvent(QCloseEvent*) override;

    QTreeView* m_tree_view { nullptr };
    ModelTranslator m_dom_model {};
    Selection m_selection;
};

}
