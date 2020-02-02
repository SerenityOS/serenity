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

#include <LibGUI/GModel.h>

namespace GUI {

class SortingProxyModel final : public Model {
public:
    static NonnullRefPtr<SortingProxyModel> create(NonnullRefPtr<Model>&& model) { return adopt(*new SortingProxyModel(move(model))); }
    virtual ~SortingProxyModel() override;

    virtual int row_count(const ModelIndex& = ModelIndex()) const override;
    virtual int column_count(const ModelIndex& = ModelIndex()) const override;
    virtual String row_name(int) const override;
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual Variant data(const ModelIndex&, Role = Role::Display) const override;
    virtual void update() override;
    virtual StringView drag_data_type() const override;

    virtual int key_column() const override { return m_key_column; }
    virtual SortOrder sort_order() const override { return m_sort_order; }
    virtual void set_key_column_and_sort_order(int, SortOrder) override;

    ModelIndex map_to_target(const ModelIndex&) const;

private:
    explicit SortingProxyModel(NonnullRefPtr<Model>&&);

    Model& target() { return *m_target; }
    const Model& target() const { return *m_target; }

    void resort();

    void set_sorting_case_sensitive(bool b) { m_sorting_case_sensitive = b; }
    bool is_sorting_case_sensitive() { return m_sorting_case_sensitive; }

    NonnullRefPtr<Model> m_target;
    Vector<int> m_row_mappings;
    int m_key_column { -1 };
    SortOrder m_sort_order { SortOrder::Ascending };
    bool m_sorting_case_sensitive { false };
};

}
