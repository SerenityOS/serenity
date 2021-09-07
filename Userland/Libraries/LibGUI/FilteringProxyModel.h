/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <LibGUI/Model.h>
#include <LibGUI/TextBox.h>

namespace GUI {

class FilteringProxyModel final : public Model {
public:
    static NonnullRefPtr<FilteringProxyModel> construct(Model& model)
    {
        return adopt_ref(*new FilteringProxyModel(model));
    }

    virtual ~FilteringProxyModel() override {};

    virtual int row_count(ModelIndex const& = ModelIndex()) const override;
    virtual int column_count(ModelIndex const& = ModelIndex()) const override;
    virtual Variant data(ModelIndex const&, ModelRole = ModelRole::Display) const override;
    virtual void invalidate() override;
    virtual ModelIndex index(int row, int column = 0, ModelIndex const& parent = ModelIndex()) const override;
    virtual bool is_searchable() const override;
    virtual Vector<ModelIndex> matches(StringView const&, unsigned = MatchesFlag::AllMatching, ModelIndex const& = ModelIndex()) override;

    void set_filter_term(StringView const& term);

    ModelIndex map(ModelIndex const&) const;

private:
    void filter();
    explicit FilteringProxyModel(Model& model)
        : m_model(model)
    {
    }

    Model& m_model;

    // Maps row to actual model index.
    Vector<ModelIndex> m_matching_indices;

    String m_filter_term;
};

}
