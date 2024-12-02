/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Forward.h>
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
    virtual ~ModelClient() = default;

    virtual void model_did_update(unsigned flags) = 0;

    virtual void model_did_insert_rows([[maybe_unused]] ModelIndex const& parent, [[maybe_unused]] int first, [[maybe_unused]] int last) { }
    virtual void model_did_insert_columns([[maybe_unused]] ModelIndex const& parent, [[maybe_unused]] int first, [[maybe_unused]] int last) { }
    virtual void model_did_move_rows([[maybe_unused]] ModelIndex const& source_parent, [[maybe_unused]] int first, [[maybe_unused]] int last, [[maybe_unused]] ModelIndex const& target_parent, [[maybe_unused]] int target_index) { }
    virtual void model_did_move_columns([[maybe_unused]] ModelIndex const& source_parent, [[maybe_unused]] int first, [[maybe_unused]] int last, [[maybe_unused]] ModelIndex const& target_parent, [[maybe_unused]] int target_index) { }
    virtual void model_did_delete_rows([[maybe_unused]] ModelIndex const& parent, [[maybe_unused]] int first, [[maybe_unused]] int last) { }
    virtual void model_did_delete_columns([[maybe_unused]] ModelIndex const& parent, [[maybe_unused]] int first, [[maybe_unused]] int last) { }
};

class Model : public RefCounted<Model> {
public:
    enum UpdateFlag {
        DontInvalidateIndices = 0,
        InvalidateAllIndices = 1 << 0,
        DontResizeColumns = 1 << 1,
    };

    enum MatchesFlag {
        AllMatching = 0,
        FirstMatchOnly = 1 << 0,
        CaseInsensitive = 1 << 1,
        MatchAtStart = 1 << 2,
        MatchFull = 1 << 3,
    };

    struct MatchResult {
        TriState matched { TriState::Unknown };
        int score { 0 };
    };

    virtual ~Model();

    virtual int row_count(ModelIndex const& = ModelIndex()) const = 0;
    virtual int column_count(ModelIndex const& = ModelIndex()) const = 0;
    virtual ErrorOr<String> column_name(int) const { return String {}; }
    virtual Variant data(ModelIndex const&, ModelRole = ModelRole::Display) const = 0;
    virtual MatchResult data_matches(ModelIndex const&, Variant const&) const { return {}; }
    virtual void invalidate();
    virtual ModelIndex parent_index(ModelIndex const&) const { return {}; }
    virtual ModelIndex index(int row, int column = 0, ModelIndex const& parent = ModelIndex()) const;
    virtual bool is_editable(ModelIndex const&) const { return false; }
    virtual bool is_searchable() const { return false; }
    virtual void set_data(ModelIndex const&, Variant const&) { }
    virtual int tree_column() const { return 0; }
    virtual bool accepts_drag(ModelIndex const&, Core::MimeData const&) const;
    virtual Vector<ModelIndex> matches(StringView, unsigned = MatchesFlag::AllMatching, ModelIndex const& = ModelIndex()) { return {}; }

    virtual bool is_column_sortable([[maybe_unused]] int column_index) const { return true; }
    virtual void sort([[maybe_unused]] int column, SortOrder) { }

    bool is_within_range(ModelIndex const& index) const
    {
        auto parent_index = this->parent_index(index);
        return index.row() >= 0 && index.row() < row_count(parent_index) && index.column() >= 0 && index.column() < column_count(parent_index);
    }

    virtual StringView drag_data_type() const { return {}; }
    virtual RefPtr<Core::MimeData> mime_data(ModelSelection const&) const;

    void register_view(Badge<AbstractView>, AbstractView&);
    void unregister_view(Badge<AbstractView>, AbstractView&);

    void register_client(ModelClient&);
    void unregister_client(ModelClient&);

    WeakPtr<PersistentHandle> register_persistent_index(Badge<PersistentModelIndex>, ModelIndex const&);

    // NOTE: This is a public version of create_index() which is normally protected,
    //       but this can be used when creating a model translator like in Ladybird.
    ModelIndex unsafe_create_index(int row, int column, void const* data = nullptr) const
    {
        return create_index(row, column, data);
    }

protected:
    Model();

    void for_each_view(Function<void(AbstractView&)>);
    void for_each_client(Function<void(ModelClient&)>);
    void did_update(unsigned flags = UpdateFlag::InvalidateAllIndices);

    static bool string_matches(StringView str, StringView needle, unsigned flags)
    {
        auto case_sensitivity = (flags & CaseInsensitive) ? CaseSensitivity::CaseInsensitive : CaseSensitivity::CaseSensitive;
        if (flags & MatchFull)
            return str.length() == needle.length() && str.starts_with(needle, case_sensitivity);
        if (flags & MatchAtStart)
            return str.starts_with(needle, case_sensitivity);
        return str.contains(needle, case_sensitivity);
    }

    ModelIndex create_index(int row, int column, void const* data = nullptr) const;

    void begin_insert_rows(ModelIndex const& parent, int first, int last);
    void begin_insert_columns(ModelIndex const& parent, int first, int last);
    void begin_move_rows(ModelIndex const& source_parent, int first, int last, ModelIndex const& target_parent, int target_index);
    void begin_move_columns(ModelIndex const& source_parent, int first, int last, ModelIndex const& target_parent, int target_index);
    void begin_delete_rows(ModelIndex const& parent, int first, int last);
    void begin_delete_columns(ModelIndex const& parent, int first, int last);

    void end_insert_rows();
    void end_insert_columns();
    void end_move_rows();
    void end_move_columns();
    void end_delete_rows();
    void end_delete_columns();

    void change_persistent_index_list(Vector<ModelIndex> const& old_indices, Vector<ModelIndex> const& new_indices);

private:
    enum class OperationType {
        Invalid = 0,
        Insert,
        Move,
        Delete,
        Reset
    };
    enum class Direction {
        Row,
        Column
    };

    struct Operation {
        OperationType type { OperationType::Invalid };
        Direction direction { Direction::Row };
        ModelIndex source_parent;
        int first { 0 };
        int last { 0 };
        ModelIndex target_parent;
        int target { 0 };

        Operation(OperationType type)
            : type(type)
        {
        }

        Operation(OperationType type, Direction direction, ModelIndex const& parent, int first, int last)
            : type(type)
            , direction(direction)
            , source_parent(parent)
            , first(first)
            , last(last)
        {
        }

        Operation(OperationType type, Direction direction, ModelIndex const& source_parent, int first, int last, ModelIndex const& target_parent, int target)
            : type(type)
            , direction(direction)
            , source_parent(source_parent)
            , first(first)
            , last(last)
            , target_parent(target_parent)
            , target(target)
        {
        }
    };

    void handle_insert(Operation const&);
    void handle_move(Operation const&);
    void handle_delete(Operation const&);

    template<bool IsRow>
    void save_deleted_indices(ModelIndex const& parent, int first, int last);

    HashMap<ModelIndex, OwnPtr<PersistentHandle>> m_persistent_handles;
    Vector<Operation> m_operation_stack;
    // NOTE: We need to save which indices have been deleted before the delete
    // actually happens, because we can't figure out which persistent handles
    // belong to us in end_delete_rows/columns (because accessing the parents of
    // the indices might be impossible).
    Vector<Vector<ModelIndex>> m_deleted_indices_stack;

    HashTable<AbstractView*> m_views;
    HashTable<ModelClient*> m_clients;
};

inline ModelIndex ModelIndex::parent() const
{
    return m_model ? m_model->parent_index(*this) : ModelIndex();
}

}
