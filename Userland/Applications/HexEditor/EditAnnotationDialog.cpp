/*
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EditAnnotationDialog.h"
#include <LibGUI/MessageBox.h>

static Gfx::Color s_most_recent_color { Color::from_argb(0xfffce94f) };

GUI::Dialog::ExecResult EditAnnotationDialog::show_create_dialog(GUI::Window* parent_window, HexDocument& document, Selection selection)
{
    auto dialog_or_error = EditAnnotationDialog::try_create(parent_window, document, selection);
    if (dialog_or_error.is_error()) {
        GUI::MessageBox::show(parent_window, MUST(String::formatted("{}", dialog_or_error.error())), "Error while opening Create Annotation dialog"sv, GUI::MessageBox::Type::Error);
        return ExecResult::Aborted;
    }

    auto dialog = dialog_or_error.release_value();
    return dialog->exec();
}

GUI::Dialog::ExecResult EditAnnotationDialog::show_edit_dialog(GUI::Window* parent_window, HexDocument& document, Annotation& annotation)
{
    auto dialog_or_error = EditAnnotationDialog::try_create(parent_window, document, &annotation);
    if (dialog_or_error.is_error()) {
        GUI::MessageBox::show(parent_window, MUST(String::formatted("{}", dialog_or_error.error())), "Error while opening Edit Annotation dialog"sv, GUI::MessageBox::Type::Error);
        return ExecResult::Aborted;
    }

    auto dialog = dialog_or_error.release_value();
    return dialog->exec();
}

ErrorOr<NonnullRefPtr<EditAnnotationDialog>> EditAnnotationDialog::try_create(GUI::Window* parent_window, HexDocument& hex_document, Variant<Annotation*, Selection> selection_or_annotation)
{
    auto widget = TRY(HexEditor::EditAnnotationWidget::try_create());
    return adopt_nonnull_ref_or_enomem(new (nothrow) EditAnnotationDialog(parent_window, move(widget), hex_document, move(selection_or_annotation)));
}

EditAnnotationDialog::EditAnnotationDialog(GUI::Window* parent_window, NonnullRefPtr<HexEditor::EditAnnotationWidget> widget, HexDocument& hex_document, Variant<Annotation*, Selection> selection_or_annotation)
    : GUI::Dialog(parent_window)
    , m_document(hex_document.make_weak_ptr())
{
    resize(260, 140);
    set_resizable(false);
    set_main_widget(widget);

    m_start_offset = find_descendant_of_type_named<GUI::NumericInput>("start_offset");
    m_end_offset = find_descendant_of_type_named<GUI::NumericInput>("end_offset");
    m_background_color = find_descendant_of_type_named<GUI::ColorInput>("background_color");
    m_comments = find_descendant_of_type_named<GUI::TextEditor>("comments");
    m_save_button = find_descendant_of_type_named<GUI::DialogButton>("save_button");
    m_cancel_button = find_descendant_of_type_named<GUI::DialogButton>("cancel_button");

    // FIXME: This could be specified in GML, but the GML doesn't like property setters that aren't `set_FOO()`.
    m_background_color->set_color_has_alpha_channel(false);
    // FIXME: Move this to GML too.
    m_comments->set_wrapping_mode(GUI::TextEditor::WrapAtWords);
    // FIXME: `font_type: "Normal"` in GML once the compiler supports that.
    m_comments->set_font(widget->font());

    // NOTE: The NumericInput stores an i64, so not all size_t values can fit. But I don't think we'll be
    //       hex-editing files larger than 9000 petabytes for the foreseeable future!
    VERIFY(hex_document.size() <= NumericLimits<i64>::max());
    m_start_offset->set_min(0);
    m_start_offset->set_max(hex_document.size() - 1);
    m_end_offset->set_min(0);
    m_end_offset->set_max(hex_document.size() - 1);

    selection_or_annotation.visit(
        [this](Annotation*& annotation) {
            m_annotation = *annotation;
            set_title("Edit Annotation"sv);
            set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/annotation.png"sv).release_value_but_fixme_should_propagate_errors());
            VERIFY(m_annotation->start_offset <= NumericLimits<i64>::max());
            VERIFY(m_annotation->end_offset <= NumericLimits<i64>::max());
            m_start_offset->set_value(m_annotation->start_offset);
            m_end_offset->set_value(m_annotation->end_offset);
            m_background_color->set_color(m_annotation->background_color);
            m_comments->set_text(m_annotation->comments);
        },
        [this](Selection& selection) {
            set_title("Add Annotation"sv);
            set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/annotation-add.png"sv).release_value_but_fixme_should_propagate_errors());
            // Selection start is inclusive, and end is exclusive.
            // Therefore, if the selection isn't empty, we need to subtract 1 from the end offset.
            m_start_offset->set_value(selection.start);
            m_end_offset->set_value(selection.is_empty() ? selection.end : selection.end - 1);
            // Default to the most recently used annotation color.
            m_background_color->set_color(s_most_recent_color);
            m_comments->clear();
        });

    m_save_button->on_click = [this](auto) {
        auto start_offset = static_cast<size_t>(m_start_offset->value());
        auto end_offset = static_cast<size_t>(m_end_offset->value());
        Annotation result {
            .start_offset = min(start_offset, end_offset),
            .end_offset = max(start_offset, end_offset),
            .background_color = m_background_color->color(),
            .comments = MUST(String::from_byte_string(m_comments->text())),
        };
        if (m_annotation.has_value()) {
            *m_annotation = move(result);
            if (m_document)
                m_document->annotations().invalidate();
        } else {
            if (m_document)
                m_document->annotations().add_annotation(result);
        }
        s_most_recent_color = m_background_color->color();
        done(ExecResult::OK);
    };
    m_cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };
}
