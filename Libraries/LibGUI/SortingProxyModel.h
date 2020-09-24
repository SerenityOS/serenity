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

#include <LibGUI/Model.h>

namespace GUI {

class SortingProxyModel
    : public Model
    , private ModelClient {
public:
    static NonnullRefPtr<SortingProxyModel> create(NonnullRefPtr<Model> source) { return adopt(*new SortingProxyModel(move(source))); }
    virtual ~SortingProxyModel() override;

    virtual int row_count(const ModelIndex& = ModelIndex()) const override;
    virtual int column_count(const ModelIndex& = ModelIndex()) const override;
    virtual String column_name(int) const override;
    virtual Variant data(const ModelIndex&, ModelRole = ModelRole::Display) const override;
    virtual void update() override;
    virtual StringView drag_data_type() const override;
    virtual ModelIndex parent_index(const ModelIndex&) const override;
    virtual ModelIndex index(int row, int column, const ModelIndex& parent) const override;
    virtual bool is_editable(const ModelIndex&) const override;
    virtual void set_data(const ModelIndex&, const Variant&) override;

    virtual bool is_column_sortable(int column_index) const override;

    virtual bool less_than(const ModelIndex&, const ModelIndex&) const;

    ModelIndex map_to_source(const ModelIndex&) const;
    ModelIndex map_to_proxy(const ModelIndex&) const;

    ModelRole sort_role() const { return m_sort_role; }
    void set_sort_role(ModelRole role) { m_sort_role = role; }

    virtual void sort(int column, SortOrder) override;

private:
    explicit SortingProxyModel(NonnullRefPtr<Model> source);

    // NOTE: The internal_data() of indexes points to the corresponding Mapping object for that index.
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

    void invalidate(unsigned flags = Model::UpdateFlag::DontInvalidateIndexes);
    InternalMapIterator build_mapping(const ModelIndex& proxy_index);

    NonnullRefPtr<Model> m_source;

    HashMap<ModelIndex, NonnullOwnPtr<Mapping>> m_mappings;
    ModelRole m_sort_role { ModelRole::Sort };
    int m_last_key_column { -1 };
    SortOrder m_last_sort_order { SortOrder::Ascending };
};

}
