/*
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
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

#include "Debugger.h"
#include <AK/NonnullOwnPtr.h>
#include <LibGUI/Label.h>
#include <LibGUI/Model.h>
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
