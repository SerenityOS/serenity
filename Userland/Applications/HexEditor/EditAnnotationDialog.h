/*
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "EditAnnotationWidget.h"
#include "HexDocument.h"
#include "Selection.h"
#include <LibGUI/Button.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/NumericInput.h>
#include <LibGUI/TextEditor.h>

class EditAnnotationDialog : public GUI::Dialog {
    C_OBJECT_ABSTRACT(EditAnnotationDialog)

public:
    static ExecResult show_create_dialog(GUI::Window* parent_window, HexDocument&, Selection);
    static ExecResult show_edit_dialog(GUI::Window* parent_window, HexDocument&, Annotation&);
    static ErrorOr<NonnullRefPtr<EditAnnotationDialog>> try_create(GUI::Window* parent_window, HexDocument&, Variant<Annotation*, Selection>);

private:
    EditAnnotationDialog(GUI::Window* parent_window, NonnullRefPtr<HexEditor::EditAnnotationWidget>, HexDocument&, Variant<Annotation*, Selection>);
    virtual ~EditAnnotationDialog() override = default;

    WeakPtr<HexDocument> m_document;
    Optional<Annotation&> m_annotation;

    RefPtr<GUI::NumericInput> m_start_offset;
    RefPtr<GUI::NumericInput> m_end_offset;
    RefPtr<GUI::ColorInput> m_background_color;
    RefPtr<GUI::TextEditor> m_comments;
    RefPtr<GUI::Button> m_save_button;
    RefPtr<GUI::Button> m_cancel_button;
};
