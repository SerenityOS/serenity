/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <LibCore/MimeData.h>
#include <LibGUI/ModelIndex.h>
#include <LibGUI/ModelRole.h>
#include <LibGUI/ModelSelection.h>
#include <LibGUI/Variant.h>
#include <LibGfx/Forward.h>
#include <LibGfx/TextAlignment.h>

namespace GUI {

enum class SortOrder {
    None,
    Ascending,
    Descending
};

class ModelClient {
public:
    virtual ~ModelClient() { }

    virtual void model_did_update(unsigned flags) = 0;
};

class Model : public RefCounted<Model> {
public:
    enum UpdateFlag {
        DontInvalidateIndices = 0,
        InvalidateAllIndices = 1 << 0,
    };

    enum MatchesFlag {
        AllMatching = 0,
        FirstMatchOnly = 1 << 0,
        CaseInsensitive = 1 << 1,
        MatchAtStart = 1 << 2,
        MatchFull = 1 << 3,
    };

    virtual ~Model();

    virtual int row_count(const ModelIndex& = ModelIndex()) const = 0;
    virtual int column_count(const ModelIndex& = ModelIndex()) const = 0;
    virtual String column_name(int) const { return {}; }
    virtual Variant data(const ModelIndex&, ModelRole = ModelRole::Display) const = 0;
    virtual TriState data_matches(const ModelIndex&, const Variant&) const { return TriState::Unknown; }
    virtual void invalidate();
    virtual ModelIndex parent_index(const ModelIndex&) const { return {}; }
    virtual ModelIndex index(int row, int column = 0, const ModelIndex& parent = ModelIndex()) const;
    virtual bool is_editable(const ModelIndex&) const { return false; }
    virtual bool is_searchable() const { return false; }
    virtual void set_data(const ModelIndex&, const Variant&) { }
    virtual int tree_column() const { return 0; }
    virtual bool accepts_drag(const ModelIndex&, const Vector<String>& mime_types) const;
    virtual Vector<ModelIndex, 1> matches(const StringView&, unsigned = MatchesFlag::AllMatching, const ModelIndex& = ModelIndex()) { return {}; }

    virtual bool is_column_sortable([[maybe_unused]] int column_index) const { return true; }
    virtual void sort([[maybe_unused]] int column, SortOrder) { }

    bool is_valid(const ModelIndex& index) const
    {
        auto parent_index = this->parent_index(index);
        return index.row() >= 0 && index.row() < row_count(parent_index) && index.column() >= 0 && index.column() < column_count(parent_index);
    }

    virtual StringView drag_data_type() const { return {}; }
    virtual RefPtr<Core::MimeData> mime_data(const ModelSelection&) const;

    void register_view(Badge<AbstractView>, AbstractView&);
    void unregister_view(Badge<AbstractView>, AbstractView&);

    void register_client(ModelClient&);
    void unregister_client(ModelClient&);

protected:
    Model();

    void for_each_view(Function<void(AbstractView&)>);
    void did_update(unsigned flags = UpdateFlag::InvalidateAllIndices);

    static bool string_matches(const StringView& str, const StringView& needle, unsigned flags)
    {
        auto case_sensitivity = (flags & CaseInsensitive) ? CaseSensitivity::CaseInsensitive : CaseSensitivity::CaseSensitive;
        if (flags & MatchFull)
            return str.length() == needle.length() && str.starts_with(needle, case_sensitivity);
        if (flags & MatchAtStart)
            return str.starts_with(needle, case_sensitivity);
        return str.contains(needle, case_sensitivity);
    }

    ModelIndex create_index(int row, int column, const void* data = nullptr) const;

private:
    HashTable<AbstractView*> m_views;
    HashTable<ModelClient*> m_clients;
};

inline ModelIndex ModelIndex::parent() const
{
    return m_model ? m_model->parent_index(*this) : ModelIndex();
}

}
