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

#include "RemoteObject.h"
#include "RemoteObjectGraphModel.h"
#include "RemoteObjectPropertyModel.h"
#include "RemoteProcess.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

[[noreturn]] static void print_usage_and_exit()
{
    printf("usage: Inspector <pid>\n");
    exit(0);
}

int main(int argc, char** argv)
{
    if (argc != 2)
        print_usage_and_exit();

    bool ok;
    pid_t pid = String(argv[1]).to_int(ok);
    if (!ok)
        print_usage_and_exit();

    GUI::Application app(argc, argv);

    auto window = GUI::Window::construct();
    window->set_title("Inspector");
    window->set_rect(150, 150, 300, 500);

    auto widget = GUI::Widget::construct();
    window->set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GUI::VerticalBoxLayout>());

    auto splitter = GUI::HorizontalSplitter::construct(widget);

    RemoteProcess remote_process(pid);

    remote_process.on_update = [&] {
        if (!remote_process.process_name().is_null())
            window->set_title(String::format("Inspector: %s (%d)", remote_process.process_name().characters(), remote_process.pid()));
    };

    auto tree_view = GUI::TreeView::construct(splitter);
    tree_view->set_model(remote_process.object_graph_model());
    tree_view->set_activates_on_selection(true);

    auto properties_table_view = GUI::TableView::construct(splitter);
    properties_table_view->set_size_columns_to_fit_content(true);

    tree_view->on_activation = [&](auto& index) {
        auto* remote_object = static_cast<RemoteObject*>(index.internal_data());
        properties_table_view->set_model(remote_object->property_model());
    };

    window->show();
    remote_process.update();
    return app.exec();
}
