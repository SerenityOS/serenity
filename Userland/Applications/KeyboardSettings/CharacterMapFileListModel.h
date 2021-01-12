/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include <AK/Vector.h>
#include <LibGUI/Model.h>

class CharacterMapFileListModel final : public GUI::Model {
public:
    static NonnullRefPtr<CharacterMapFileListModel> create(Vector<String>& file_names)
    {
        return adopt(*new CharacterMapFileListModel(file_names));
    }

    virtual ~CharacterMapFileListModel() override { }

    virtual int row_count(const GUI::ModelIndex&) const override
    {
        return m_file_names.size();
    }

    virtual int column_count(const GUI::ModelIndex&) const override
    {
        return 1;
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        ASSERT(index.is_valid());
        ASSERT(index.column() == 0);

        if (role == GUI::ModelRole::Display)
            return m_file_names.at(index.row());

        return {};
    }

    virtual void update() override
    {
        did_update();
    }

private:
    explicit CharacterMapFileListModel(Vector<String>& file_names)
        : m_file_names(file_names)
    {
    }

    Vector<String>& m_file_names;
};
