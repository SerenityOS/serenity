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

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <LibGfx/TextAlignment.h>
#include <LibGUI/ModelIndex.h>
#include <LibGUI/Variant.h>

namespace Gfx {
class Font;
}

namespace GUI {

class AbstractView;

enum class SortOrder {
    None,
    Ascending,
    Descending
};

class Model : public RefCounted<Model> {
public:
    struct ColumnMetadata {
        int preferred_width { 0 };
        Gfx::TextAlignment text_alignment { Gfx::TextAlignment::CenterLeft };
        const Gfx::Font* font { nullptr };
        enum class Sortable {
            False,
            True,
        };
        Sortable sortable { Sortable::True };
    };

    enum class Role {
        Display,
        Sort,
        Custom,
        ForegroundColor,
        BackgroundColor,
        Icon,
        Font,
        DragData,
    };

    virtual ~Model();

    virtual int row_count(const ModelIndex& = ModelIndex()) const = 0;
    virtual int column_count(const ModelIndex& = ModelIndex()) const = 0;
    virtual String row_name(int) const { return {}; }
    virtual String column_name(int) const { return {}; }
    virtual ColumnMetadata column_metadata(int) const { return {}; }
    virtual Variant data(const ModelIndex&, Role = Role::Display) const = 0;
    virtual void update() = 0;
    virtual ModelIndex parent_index(const ModelIndex&) const { return {}; }
    virtual ModelIndex index(int row, int column = 0, const ModelIndex& = ModelIndex()) const { return create_index(row, column); }
    virtual ModelIndex sibling(int row, int column, const ModelIndex& parent) const;
    virtual bool is_editable(const ModelIndex&) const { return false; }
    virtual void set_data(const ModelIndex&, const Variant&) {}
    virtual int tree_column() const { return 0; }

    bool is_valid(const ModelIndex& index) const
    {
        auto parent_index = this->parent_index(index);
        return index.row() >= 0 && index.row() < row_count(parent_index) && index.column() >= 0 && index.column() < column_count(parent_index);
    }

    virtual int key_column() const { return -1; }
    virtual SortOrder sort_order() const { return SortOrder::None; }
    virtual void set_key_column_and_sort_order(int, SortOrder) {}

    virtual StringView drag_data_type() const { return {}; }

    void register_view(Badge<AbstractView>, AbstractView&);
    void unregister_view(Badge<AbstractView>, AbstractView&);

    Function<void()> on_update;

protected:
    Model();

    void for_each_view(Function<void(AbstractView&)>);
    void did_update();

    ModelIndex create_index(int row, int column, const void* data = nullptr) const;

private:
    HashTable<AbstractView*> m_views;
};

inline ModelIndex ModelIndex::parent() const
{
    return m_model ? m_model->parent_index(*this) : ModelIndex();
}

}
