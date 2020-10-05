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

#include <AK/Traits.h>
#include <LibGUI/Forward.h>
#include <LibGUI/ModelRole.h>

namespace GUI {

class ModelIndex {
    friend class Model;

public:
    ModelIndex() { }

    bool is_valid() const { return m_model && m_row != -1 && m_column != -1; }
    int row() const { return m_row; }
    int column() const { return m_column; }

    void* internal_data() const { return m_internal_data; }

    ModelIndex parent() const;

    bool operator==(const ModelIndex& other) const
    {
        return m_model == other.m_model && m_row == other.m_row && m_column == other.m_column && m_internal_data == other.m_internal_data;
    }

    bool operator!=(const ModelIndex& other) const
    {
        return !(*this == other);
    }

    const Model* model() const { return m_model; }

    Variant data(ModelRole = ModelRole::Display) const;

private:
    ModelIndex(const Model& model, int row, int column, void* internal_data)
        : m_model(&model)
        , m_row(row)
        , m_column(column)
        , m_internal_data(internal_data)
    {
    }

    const Model* m_model { nullptr };
    int m_row { -1 };
    int m_column { -1 };
    void* m_internal_data { nullptr };
};

const LogStream& operator<<(const LogStream&, const ModelIndex&);

}

namespace AK {

template<>
struct Formatter<GUI::ModelIndex> : Formatter<StringView> {
    void format(TypeErasedFormatParams&, FormatBuilder&, const GUI::ModelIndex&);
};

template<>
struct Traits<GUI::ModelIndex> : public GenericTraits<GUI::ModelIndex> {
    static unsigned hash(const GUI::ModelIndex& index) { return pair_int_hash(index.row(), index.column()); }
};

}
