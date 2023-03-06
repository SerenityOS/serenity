/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>

namespace HackStudio {

class ClassViewWidget final : public GUI::Widget {
    C_OBJECT(ClassViewWidget)
public:
    virtual ~ClassViewWidget() override { }

    void refresh();

private:
    ClassViewWidget();

    RefPtr<GUI::TreeView> m_class_tree;
};

// Note: A ClassViewNode stores a raw pointer to the Declaration from ProjectDeclarations and a StringView into its name.
// We should take care to update the ClassViewModel whenever the declarations change, because otherwise we may be holding pointers to freed memory.
// This is currently achieved with the on_update callback of ProjectDeclarations.
struct ClassViewNode {
    StringView name;
    CodeComprehension::Declaration const* declaration { nullptr };
    Vector<NonnullOwnPtr<ClassViewNode>> children;
    ClassViewNode* parent { nullptr };

    explicit ClassViewNode(StringView name)
        : name(name) {};
};

class ClassViewModel final : public GUI::Model {
public:
    static RefPtr<ClassViewModel> create();
    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole role) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& parent_index = GUI::ModelIndex()) const override;

private:
    explicit ClassViewModel();
    void add_declaration(CodeComprehension::Declaration const&);
    Vector<NonnullOwnPtr<ClassViewNode>> m_root_scope;
};

}
