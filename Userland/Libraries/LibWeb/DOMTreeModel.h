/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibWeb/Forward.h>

namespace Web {

class DOMTreeModel final : public GUI::Model {
public:
    static NonnullRefPtr<DOMTreeModel> create(DOM::Document& document)
    {
        return adopt_ref(*new DOMTreeModel(document));
    }

    virtual ~DOMTreeModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;

    GUI::ModelIndex index_for_node(DOM::Node*) const;

private:
    explicit DOMTreeModel(DOM::Document&);

    NonnullRefPtr<DOM::Document> m_document;

    GUI::Icon m_document_icon;
    GUI::Icon m_element_icon;
    GUI::Icon m_text_icon;
};

}
