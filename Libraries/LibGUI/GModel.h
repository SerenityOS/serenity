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
#include <LibDraw/TextAlignment.h>
#include <LibGUI/GModelIndex.h>
#include <LibGUI/GVariant.h>

class Font;
class GAbstractView;

enum class GSortOrder {
    None,
    Ascending,
    Descending
};

class GModel : public RefCounted<GModel> {
public:
    struct ColumnMetadata {
        int preferred_width { 0 };
        TextAlignment text_alignment { TextAlignment::CenterLeft };
        const Font* font { nullptr };
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

    virtual ~GModel();

    virtual int row_count(const GModelIndex& = GModelIndex()) const = 0;
    virtual int column_count(const GModelIndex& = GModelIndex()) const = 0;
    virtual String row_name(int) const { return {}; }
    virtual String column_name(int) const { return {}; }
    virtual ColumnMetadata column_metadata(int) const { return {}; }
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const = 0;
    virtual void update() = 0;
    virtual GModelIndex parent_index(const GModelIndex&) const { return {}; }
    virtual GModelIndex index(int row, int column = 0, const GModelIndex& = GModelIndex()) const { return create_index(row, column); }
    virtual GModelIndex sibling(int row, int column, const GModelIndex& parent) const;
    virtual bool is_editable(const GModelIndex&) const { return false; }
    virtual void set_data(const GModelIndex&, const GVariant&) {}
    virtual int tree_column() const { return 0; }

    bool is_valid(const GModelIndex& index) const
    {
        auto parent_index = this->parent_index(index);
        return index.row() >= 0 && index.row() < row_count(parent_index) && index.column() >= 0 && index.column() < column_count(parent_index);
    }

    virtual int key_column() const { return -1; }
    virtual GSortOrder sort_order() const { return GSortOrder::None; }
    virtual void set_key_column_and_sort_order(int, GSortOrder) {}

    virtual StringView drag_data_type() const { return {}; }

    void register_view(Badge<GAbstractView>, GAbstractView&);
    void unregister_view(Badge<GAbstractView>, GAbstractView&);

    Function<void()> on_update;

protected:
    GModel();

    void for_each_view(Function<void(GAbstractView&)>);
    void did_update();

    GModelIndex create_index(int row, int column, const void* data = nullptr) const;

private:
    HashTable<GAbstractView*> m_views;
};

inline GModelIndex GModelIndex::parent() const
{
    return m_model ? m_model->parent_index(*this) : GModelIndex();
}
