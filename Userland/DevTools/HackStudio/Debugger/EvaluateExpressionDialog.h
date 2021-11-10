/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>
#include <LibWeb/InProcessWebView.h>

namespace HackStudio {

class EvaluateExpressionDialog : public GUI::Dialog {
    C_OBJECT(EvaluateExpressionDialog);

private:
    explicit EvaluateExpressionDialog(Window* parent_window);

    void build(Window* parent_window);
    void handle_evaluation(const String& expression);
    void set_output(StringView html);

    NonnullOwnPtr<JS::Interpreter> m_interpreter;
    RefPtr<GUI::TextBox> m_text_editor;
    RefPtr<Web::InProcessWebView> m_output_view;
    RefPtr<Web::DOM::Element> m_output_container;
    RefPtr<GUI::Button> m_evaluate_button;
    RefPtr<GUI::Button> m_close_button;
};

}
