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

#include <AK/Function.h>
#include <AK/String.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Variant.h>

class VBWidget;

class VBProperty {
    friend class VBWidget;

public:
    VBProperty(VBWidget&, const String& name, const GUI::Variant& value);
    VBProperty(VBWidget&, const String& name, Function<GUI::Variant(const GUI::Widget&)>&& getter, Function<void(GUI::Widget&, const GUI::Variant&)>&& setter);
    ~VBProperty();

    String name() const { return m_name; }
    const GUI::Variant& value() const { return m_value; }
    void set_value(const GUI::Variant&);

    bool is_readonly() const { return m_readonly; }
    void set_readonly(bool b) { m_readonly = b; }

    void sync();

private:
    VBWidget& m_widget;
    String m_name;
    GUI::Variant m_value;
    Function<GUI::Variant(const GUI::Widget&)> m_getter;
    Function<void(GUI::Widget&, const GUI::Variant&)> m_setter;
    bool m_readonly { false };
};
