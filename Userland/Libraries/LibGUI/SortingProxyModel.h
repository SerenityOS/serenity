/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>

namespace GUI {

class SortingProxyModel
    : public Model
    , private ModelClient {
public:
    static ErrorOr<NonnullRefPtr<SortingProxyModel>> create(NonnullRefPtr<Model> source)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) SortingProxyModel(move(source)));
    }

    virtual ~SortingProxyModel() override;

    virtual int tree_column() const override { return m_source->tree_column(); }
    virtual int row_count(ModelIndex const& = ModelIndex()) const override;
    virtual int column_count(ModelIndex const& = ModelIndex()) const override;
    virtual ErrorOr<String> column_name(int) const override;
    virtual Variant data(ModelIndex const&, ModelRole = ModelRole::Display) const override;
    virtual void invalidate() override;
    virtual StringView drag_data_type() const override;
    virtual ModelIndex parent_index(ModelIndex const&) const override;
    virtual ModelIndex index(int row, int column, ModelIndex const& parent) const override;
    virtual bool is_editable(ModelIndex const&) const override;
    virtual bool is_searchable() const override;
    virtual void set_data(ModelIndex const&, Variant const&) override;
    virtual Vector<ModelIndex> matches(StringView, unsigned = MatchesFlag::AllMatching, ModelIndex const& = ModelIndex()) override;
    virtual bool accepts_drag(ModelIndex const&, Core::MimeData const&) const override;

    virtual bool is_column_sortable(int column_index) const override;

    virtual bool less_than(ModelIndex const&, ModelIndex const&) const;

    ModelIndex map_to_source(ModelIndex const&) const;
    ModelIndex map_to_proxy(ModelIndex const&) const;

    ModelRole sort_role() const { return m_sort_role; }
    void set_sort_role(ModelRole role) { m_sort_role = role; }

    virtual void sort(int column, SortOrder) override;

protected:
    explicit SortingProxyModel(NonnullRefPtr<Model> source);

private:
    // NOTE: The internal_data() of indices points to the corresponding Mapping object for that index.
    struct Mapping {
        Vector<int> source_rows;
        Vector<int> proxy_rows;
        ModelIndex source_parent;
    };

    using InternalMapIterator = HashMap<ModelIndex, NonnullOwnPtr<Mapping>>::IteratorType;

    void sort_mapping(Mapping&, int column, SortOrder);

    // ^ModelClient
    virtual void model_did_update(unsigned) override;

    Model& source() { return *m_source; }
    Model const& source() const { return *m_source; }

    void update_sort(unsigned = UpdateFlag::DontInvalidateIndices);
    InternalMapIterator build_mapping(ModelIndex const& proxy_index);

    NonnullRefPtr<Model> m_source;

    HashMap<ModelIndex, NonnullOwnPtr<Mapping>> m_mappings;
    ModelRole m_sort_role { ModelRole::Sort };
    int m_last_key_column { -1 };
    SortOrder m_last_sort_order { SortOrder::Ascending };
};

}
