/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/StringUtils.h>
#include <LibGUI/Model.h>
#include <LibGUI/PersistentModelIndex.h>

namespace GUI {

class ViewModel
    : public Model
    , private ModelClient {
public:
    static NonnullRefPtr<ViewModel> create(NonnullRefPtr<Model> source) { return adopt_ref(*new ViewModel(move(source))); }
    virtual ~ViewModel() override;

    virtual int row_count(ModelIndex const& = ModelIndex()) const override;
    virtual int column_count(ModelIndex const& = ModelIndex()) const override;
    virtual String column_name(int) const override;
    virtual Variant data(ModelIndex const&, ModelRole = ModelRole::Display) const override;
    virtual TriState data_matches(ModelIndex const&, Variant const&) const override;
    virtual ModelIndex parent_index(ModelIndex const&) const override;
    virtual ModelIndex index(int row, int column = 0, ModelIndex const& parent = ModelIndex()) const override;
    virtual bool is_editable(ModelIndex const&) const override;
    virtual bool is_searchable() const override;
    virtual void set_data(ModelIndex const&, Variant const&) override;
    virtual int tree_column() const override;
    virtual bool accepts_drag(ModelIndex const&, Vector<String> const& mime_types) const override;
    virtual Vector<ModelIndex, 1> matches(StringView const&, unsigned = MatchesFlag::AllMatching, ModelIndex const& parent = ModelIndex()) override;
    virtual void invalidate() override;

    virtual bool is_column_sortable(int column_index) const override;
    virtual void sort(Vector<SortSpec> const&) override;
    void filter(String term);

    ModelRole sort_role() const { return m_sort_role; }
    void set_sort_role(ModelRole);

    CaseSensitivity case_sensitivity() const { return m_case_sensitivity; }
    void set_case_sensitivity(CaseSensitivity);

    // NOTE: This is required by some views to get data from the source
    // model they otherwise couldn't (see DirectoryView).
    ModelIndex source_index_from_proxy(ModelIndex const&) const;

private:
    explicit ViewModel(NonnullRefPtr<Model> source);

    // NOTE: The internal_data() of indices points to the corresponding
    // Mapping object for that index.
    struct Mapping {
        Vector<PersistentModelIndex> proxied_rows;
        PersistentModelIndex source_parent;
    };

    // This is used to backup the persistent model indices before a change
    // (but after the source model has changed).
    struct SourceProxyPair {
        ModelIndex source_index;
        ModelIndex proxy_index;
    };

    Mapping* mapping_for_index(ModelIndex const&) const;
    ModelIndex proxy_index_from_source(ModelIndex const&) const;
    ModelIndex index_from_source_parent(int row, int column, ModelIndex const& source_parent) const;

    Mapping* get_or_create_mapping(const ModelIndex& source_parent);

    bool less_than(ModelIndex const& parent, int a, int b);

    void sort_mapping(Mapping&);
    bool fails_filter(ModelIndex const&);
    void filter_mapping(Mapping&);

    void sort_impl();
    void filter_impl();
    Vector<SourceProxyPair> backup_persistent_indices(Mapping const&);
    void update_persistent_indices(Vector<SourceProxyPair> const&);

    // ^ModelClient
    virtual void model_did_update(unsigned) override;
    virtual void model_did_insert_rows(ModelIndex const& parent, int first, int last) override;
    virtual void model_did_move_rows(ModelIndex const& source_parent, int first, int last, ModelIndex const& target_parent, int target_index) override;
    virtual void model_did_delete_rows(ModelIndex const& parent, int first, int last) override;

    // If this flag is set when the model receives a generic model_did_update,
    // then we don't invalidate anything (since we already granularly updated
    // based on what the model told us).
    bool m_received_granular_update { false };

    // NOTE: This maps from the source's parent indices to our mappings.
    HashMap<PersistentModelIndex, NonnullOwnPtr<Mapping>> m_mappings;
    NonnullRefPtr<Model> m_source;

    // Filtering
    String m_filter_term;

    // Sorting
    ModelRole m_sort_role { ModelRole::Sort };
    Vector<SortSpec> m_sort_specs {
        { -1, SortOrder::Ascending }
    };

    CaseSensitivity m_case_sensitivity { CaseSensitivity::CaseSensitive };
};

}
