/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "NumericInput.h"
#include "PDFViewer.h"
#include "SidebarWidget.h"
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Widget.h>

class PDFViewer;
class PagedErrorsModel;

class PDFViewerWidget final : public GUI::Widget {
    C_OBJECT(PDFViewerWidget)

public:
    ~PDFViewerWidget() override = default;

    void initialize_menubar(GUI::Window&);
    void open_file(Core::File&);

private:
    PDFViewerWidget();

    void initialize_toolbar(GUI::Toolbar&);
    PDF::PDFErrorOr<void> try_open_file(Core::File&);

    RefPtr<PDFViewer> m_viewer;
    RefPtr<SidebarWidget> m_sidebar;
    NonnullRefPtr<PagedErrorsModel> m_paged_errors_model;
    RefPtr<GUI::TreeView> m_errors_tree_view;
    RefPtr<NumericInput> m_page_text_box;
    RefPtr<GUI::Label> m_total_page_label;
    RefPtr<GUI::Action> m_go_to_prev_page_action;
    RefPtr<GUI::Action> m_go_to_next_page_action;
    RefPtr<GUI::Action> m_toggle_sidebar_action;
    RefPtr<GUI::Action> m_zoom_in_action;
    RefPtr<GUI::Action> m_zoom_out_action;
    RefPtr<GUI::Action> m_reset_zoom_action;
    RefPtr<GUI::Action> m_rotate_counterclockwise_action;
    RefPtr<GUI::Action> m_rotate_clockwise_action;
    GUI::ActionGroup m_page_view_action_group;
    RefPtr<GUI::Action> m_page_view_mode_single;
    RefPtr<GUI::Action> m_page_view_mode_multiple;
    RefPtr<GUI::CheckBox> m_show_clipping_paths;
    RefPtr<GUI::CheckBox> m_show_images;

    bool m_sidebar_open { false };
    ByteBuffer m_buffer;
};
