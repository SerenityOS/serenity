/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/EnumBits.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <LibGUI/Model.h>
#include <LibGUI/TextBox.h>

namespace GUI {

class FilteringProxyModel final : public Model
    , public ModelClient {
public:
    enum class FilteringOptions {
        None,
        SortByScore = 1 << 1
    };

    static ErrorOr<NonnullRefPtr<FilteringProxyModel>> create(NonnullRefPtr<Model> model, FilteringOptions filtering_options = FilteringOptions::None)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) FilteringProxyModel(move(model), filtering_options));
    }

    virtual ~FilteringProxyModel() override
    {
        m_model->unregister_client(*this);
    }

    virtual int row_count(ModelIndex const& = ModelIndex()) const override;
    virtual int column_count(ModelIndex const& = ModelIndex()) const override;
    virtual ErrorOr<String> column_name(int) const override;
    virtual Variant data(ModelIndex const&, ModelRole = ModelRole::Display) const override;
    virtual void invalidate() override;
    virtual ModelIndex index(int row, int column = 0, ModelIndex const& parent = ModelIndex()) const override;
    virtual bool is_searchable() const override;
    virtual Vector<ModelIndex> matches(StringView, unsigned = MatchesFlag::AllMatching, ModelIndex const& = ModelIndex()) override;

    void set_filter_term(StringView term);

    ModelIndex map(ModelIndex const&) const;

protected:
    virtual void model_did_update([[maybe_unused]] unsigned flags) override { invalidate(); }

private:
    struct ModelIndexWithScore {
        ModelIndex index;
        int score { 0 };
    };

    void filter();
    explicit FilteringProxyModel(NonnullRefPtr<Model> model, FilteringOptions filtering_options = FilteringOptions::None)
        : m_model(move(model))
        , m_filtering_options(filtering_options)
    {
        m_model->register_client(*this);
    }

    NonnullRefPtr<Model> m_model;

    // Maps row to actual model index.
    Vector<ModelIndexWithScore> m_matching_indices;

    ByteString m_filter_term;
    FilteringOptions m_filtering_options;
};

AK_ENUM_BITWISE_OPERATORS(FilteringProxyModel::FilteringOptions);

}
