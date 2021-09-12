/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Debugger.h"
#include <AK/NonnullOwnPtr.h>
#include <LibGUI/Label.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Widget.h>
#include <sys/arch/i386/regs.h>

namespace HackStudio {

class UnavailableDisassemblyWidget final : public GUI::Frame {
    C_OBJECT(UnavailableDisassemblyWidget)
public:
    virtual ~UnavailableDisassemblyWidget() override { }

    const String& reason() const { return m_reason; }
    void set_reason(const String& text) { m_reason = text; }

private:
    UnavailableDisassemblyWidget(const String& reason)
        : m_reason(reason)
    {
    }

    virtual void paint_event(GUI::PaintEvent& event) override;

    String m_reason;
};

class DisassemblyWidget final : public GUI::Widget {
    C_OBJECT(DisassemblyWidget)
public:
    virtual ~DisassemblyWidget() override { }

    void update_state(const Debug::DebugSession&, const PtraceRegisters&);
    void program_stopped();

private:
    DisassemblyWidget();

    void show_disassembly();
    void hide_disassembly(const String&);

    RefPtr<GUI::Widget> m_top_container;
    RefPtr<GUI::TableView> m_disassembly_view;
    RefPtr<GUI::Label> m_function_name_label;
    RefPtr<UnavailableDisassemblyWidget> m_unavailable_disassembly_widget;
};

}
