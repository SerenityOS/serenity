/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "FuzzySearchAlgorithms.h"
#include <AK/NonnullRefPtr.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>

struct HaystackEntry {
    String text;
    double score { 0 };
};

class FuzzyHaystackModel final : public GUI::Model {
public:
    static NonnullRefPtr<FuzzyHaystackModel> create(const Vector<HaystackEntry>& haystack, const FuzzySearchAlgorithms::SearchOptions& options)
    {
        return adopt_ref(*new FuzzyHaystackModel(haystack, options));
    }

    virtual ~FuzzyHaystackModel() override { }

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_filtered_haystack.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }
    virtual void update() override
    {
        m_filtered_haystack.clear_with_capacity();
        for (auto& entry : m_haystack) {
            entry.score = FuzzySearchAlgorithms::fzf_match_v1(entry.text, m_needle, m_options);
            if (entry.score >= 0)
                m_filtered_haystack.append(&entry);
        }
        quick_sort(m_filtered_haystack, [](auto& a, auto& b) { return a->score < b->score; });
        did_update();
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role = GUI::ModelRole::Display) const override
    {
        if (role == GUI::ModelRole::Display) {
            return m_filtered_haystack.at(index.row())->text;
        }

        return {};
    }

    void set_needle(const String& needle)
    {
        if (needle != m_needle) {
            m_needle = needle;
            update();
        }
    }

    int unfiltered_row_count() const { return m_haystack.size(); }

private:
    explicit FuzzyHaystackModel(const Vector<HaystackEntry>& haystack, const FuzzySearchAlgorithms::SearchOptions& options)
        : m_haystack(move(haystack))
        , m_options(move(options))

    {
        for (auto& entry : m_haystack) {
            m_filtered_haystack.append(&entry);
        }
    }

    Vector<HaystackEntry*> m_filtered_haystack;
    Vector<HaystackEntry> m_haystack;
    FuzzySearchAlgorithms::SearchOptions m_options { false };
    String m_needle;
};
