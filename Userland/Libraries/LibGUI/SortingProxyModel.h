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
    static NonnullRefPtr<SortingProxyModel> create(NonnullRefPtr<Model> source) { return adopt_ref(*new SortingProxyModel(move(source))); }
    virtual ~SortingProxyModel() override;

    virtual int row_count(const ModelIndex& = ModelIndex()) const override;
    virtual int column_count(const ModelIndex& = ModelIndex()) const override;
    virtual String column_name(int) const override;
    virtual Variant data(const ModelIndex&, ModelRole = ModelRole::Display) const override;
    virtual void invalidate() override;
    virtual StringView drag_data_type() const override;
    virtual ModelIndex parent_index(const ModelIndex&) const override;
    virtual ModelIndex index(int row, int column, const ModelIndex& parent) const override;
    virtual bool is_editable(const ModelIndex&) const override;
    virtual bool is_searchable() const override;
    virtual void set_data(const ModelIndex&, const Variant&) override;
    virtual Vector<ModelIndex> matches(const StringView&, unsigned = MatchesFlag::AllMatching, const ModelIndex& = ModelIndex()) override;
    virtual bool accepts_drag(const ModelIndex&, const Vector<String>& mime_types) const override;

    virtual bool is_column_sortable(int column_index) const override;

    virtual bool less_than(const ModelIndex&, const ModelIndex&) const;

    ModelIndex map_to_source(const ModelIndex&) const;
    ModelIndex map_to_proxy(const ModelIndex&) const;

    ModelRole sort_role() const { return m_sort_role; }
    void set_sort_role(ModelRole role) { m_sort_role = role; }

    virtual void sort(int column, SortOrder) override;

private:
    explicit SortingProxyModel(NonnullRefPtr<Model> source);

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
    const Model& source() const { return *m_source; }

    void update_sort(unsigned = UpdateFlag::DontInvalidateIndices);
    InternalMapIterator build_mapping(const ModelIndex& proxy_index);

    NonnullRefPtr<Model> m_source;

    HashMap<ModelIndex, NonnullOwnPtr<Mapping>> m_mappings;
    ModelRole m_sort_role { ModelRole::Sort };
    int m_last_key_column { -1 };
    SortOrder m_last_sort_order { SortOrder::Ascending };
};

}
