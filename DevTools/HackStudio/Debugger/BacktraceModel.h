/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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
#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <sys/arch/i386/regs.h>

class BacktraceModel final : public GUI::Model {
public:
    static RefPtr<BacktraceModel> create(const PtraceRegisters& regs);

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_frames.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }

    virtual String column_name(int) const override
    {
        return "";
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, Role role = Role::Display) const override;

    virtual void update() override {}
    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& = GUI::ModelIndex()) const override { return create_index(row, column, &m_frames.at(row)); }

private:
    struct FrameInfo {
        String function_name;
        u32 address_in_frame;
    };

    explicit BacktraceModel(Vector<FrameInfo>&& frames)
        : m_frames(move(frames))
    {
    }

    static Vector<FrameInfo> create_backtrace(const PtraceRegisters&);

    Vector<FrameInfo> m_frames;
};
