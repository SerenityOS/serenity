/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibGfx/Color.h>
#include <LibWeb/DOM/Document.h>

class IRCLogBuffer : public RefCounted<IRCLogBuffer> {
public:
    static NonnullRefPtr<IRCLogBuffer> create();
    ~IRCLogBuffer();

    struct Message {
        time_t timestamp { 0 };
        char prefix { 0 };
        String sender;
        String text;
        Color color { Color::Black };
    };

    void add_message(char prefix, const String& name, const String& text, Color = Color::Black);
    void add_message(const String& text, Color = Color::Black);

    const Web::DOM::Document& document() const { return *m_document; }
    Web::DOM::Document& document() { return *m_document; }

private:
    IRCLogBuffer();
    RefPtr<Web::DOM::Document> m_document;
    RefPtr<Web::DOM::Element> m_container_element;
};
