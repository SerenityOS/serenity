/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <sys/arch/i386/regs.h>

namespace Debug {

class DebugSession;

}

namespace HackStudio {

class BacktraceModel final : public GUI::Model {
public:
    static NonnullRefPtr<BacktraceModel> create(const Debug::DebugSession&, const PtraceRegisters& regs);

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_frames.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }

    virtual String column_name(int) const override
    {
        return "";
    }

    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    virtual void update() override { }
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex&) const override;

    struct FrameInfo {
        String function_name;
        u32 instruction_address;
        u32 frame_base;
    };

    const Vector<FrameInfo>& frames() const { return m_frames; }

private:
    explicit BacktraceModel(Vector<FrameInfo>&& frames)
        : m_frames(move(frames))
    {
    }

    static Vector<FrameInfo> create_backtrace(const Debug::DebugSession&, const PtraceRegisters&);

    Vector<FrameInfo> m_frames;
};

}
