/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibTextEditing/API.h>
#include <LibTextEditing/Forward.h>

namespace TextEditing {

// This is implemented by users of LibTextEditing to talk to the engine.
class Interface : public RefCounted<Interface> {
public:
    virtual ~Interface() = default;

    void set_engine(Badge<Engine>, WeakPtr<Engine>);

    virtual void set_status_bar(String) = 0;
    virtual void set_document_name(String) = 0;
    // The file location is just symbolic.
    virtual void set_file_location(FileLocation) = 0;

    // Depending on what the text is, it might overwrite multiple lines.
    // When a newline is encountered, all the rest of the text on the line is also removed.
    virtual void set_text(Position start, AK::Utf8View) = 0;
    // This API can also insert lines.
    virtual void insert_text(Position start, AK::Utf8View) = 0;
    // Shift the text a number of lines down (positive) or up (negative).
    // The filler text must be enough to fill the empty space, having exactly as many lines as were shifted.
    // There MUST NOT be a final newline (as that would represent an empty line).
    virtual void shift_text(int lines, AK::Utf8View filler) = 0;

    // One selection of length 1 means a normal cursor.
    virtual void set_selections(Vector<Range, 1>) = 0;

private:
    friend class Engine;

    WeakPtr<Engine> m_engine;
};

}
