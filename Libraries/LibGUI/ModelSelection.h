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

#include <AK/HashTable.h>
#include <LibGUI/ModelIndex.h>

namespace GUI {

class AbstractView;

class ModelSelection {
public:
    ModelSelection(AbstractView& view)
        : m_view(view)
    {
    }

    int size() const { return m_indexes.size(); }
    bool is_empty() const { return m_indexes.is_empty(); }
    bool contains(const ModelIndex& index) const { return m_indexes.contains(index); }
    bool contains_row(int row) const
    {
        for (auto& index : m_indexes) {
            if (index.row() == row)
                return true;
        }
        return false;
    }

    void set(const ModelIndex&);
    void add(const ModelIndex&);
    void toggle(const ModelIndex&);
    bool remove(const ModelIndex&);
    void clear();

    template<typename Callback>
    void for_each_index(Callback callback)
    {
        for (auto& index : indexes())
            callback(index);
    }

    template<typename Callback>
    void for_each_index(Callback callback) const
    {
        for (auto& index : indexes())
            callback(index);
    }

    Vector<ModelIndex> indexes() const
    {
        Vector<ModelIndex> selected_indexes;

        for (auto& index : m_indexes)
            selected_indexes.append(index);

        return selected_indexes;
    }

    // FIXME: This doesn't guarantee that what you get is the lowest or "first" index selected..
    ModelIndex first() const
    {
        if (m_indexes.is_empty())
            return {};
        return *m_indexes.begin();
    }

private:
    AbstractView& m_view;
    HashTable<ModelIndex> m_indexes;
};

}
