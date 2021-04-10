/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
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

#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>

namespace HackStudio {

class ClassViewWidget final : public GUI::Widget {
    C_OBJECT(ClassViewWidget)
public:
    virtual ~ClassViewWidget() override { }
    ClassViewWidget();

    void refresh();

private:
    RefPtr<GUI::TreeView> m_class_tree;
};

// Note: A ClassViewNode stores a raw pointer to the Declaration from ProjectDeclarations and a StringView into its name.
// We should take care to update the ClassViewModel whenever the declarations change, because otherwise we may be holding pointers to freed memory.
// This is currently achieved with the on_update callback of ProjectDeclarations.
struct ClassViewNode {
    StringView name;
    const GUI::AutocompleteProvider::Declaration* declaration { nullptr };
    NonnullOwnPtrVector<ClassViewNode> children;
    ClassViewNode* parent { nullptr };

    explicit ClassViewNode(const StringView& name)
        : name(name) {};
};

class ClassViewModel final : public GUI::Model {
public:
    static RefPtr<ClassViewModel> create();
    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole role) const override;
    virtual void update() override { did_update(); }
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& parent_index = GUI::ModelIndex()) const override;

private:
    explicit ClassViewModel();
    void add_declaration(const GUI::AutocompleteProvider::Declaration&);
    NonnullOwnPtrVector<ClassViewNode> m_root_scope;
};

}
