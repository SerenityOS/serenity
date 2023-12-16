/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibDebug/ProcessInspector.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <sys/arch/regs.h>

namespace Debug {

class DebugSession;

}

namespace HackStudio {

class BacktraceModel final : public GUI::Model {
public:
    static NonnullRefPtr<BacktraceModel> create(Debug::ProcessInspector const&, PtraceRegisters const& regs);

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_frames.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }

    virtual ErrorOr<String> column_name(int) const override { return String {}; }

    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex&) const override;

    struct FrameInfo {
        ByteString function_name;
        FlatPtr instruction_address { 0 };
        FlatPtr frame_base { 0 };
        Optional<Debug::DebugInfo::SourcePosition> m_source_position;
    };

    Vector<FrameInfo> const& frames() const { return m_frames; }

private:
    explicit BacktraceModel(Vector<FrameInfo>&& frames)
        : m_frames(move(frames))
    {
    }

    static Vector<FrameInfo> create_backtrace(Debug::ProcessInspector const&, PtraceRegisters const&);

    Vector<FrameInfo> m_frames;
};

}
