/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "HelpWindow.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Frame.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Splitter.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/OutOfProcessWebView.h>

namespace Spreadsheet {

class HelpListModel final : public GUI::Model {
public:
    static NonnullRefPtr<HelpListModel> create() { return adopt(*new HelpListModel); }

    virtual ~HelpListModel() override { }

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_keys.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }
    virtual void update() override { }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role = GUI::ModelRole::Display) const override
    {
        if (role == GUI::ModelRole::Display) {
            return key(index);
        }

        return {};
    }

    String key(const GUI::ModelIndex& index) const { return m_keys[index.row()]; }

    void set_from(const JsonObject& object)
    {
        m_keys.clear();
        object.for_each_member([this](auto& name, auto&) {
            m_keys.append(name);
        });
        did_update();
    }

private:
    HelpListModel()
    {
    }

    Vector<String> m_keys;
};

RefPtr<HelpWindow> HelpWindow::s_the { nullptr };

HelpWindow::HelpWindow(GUI::Window* parent)
    : GUI::Window(parent)
{
    resize(530, 365);
    set_title("Spreadsheet Functions Help");

    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_layout<GUI::VerticalBoxLayout>().set_margins({ 4, 4, 4, 4 });
    widget.set_fill_with_background_color(true);

    auto& splitter = widget.add<GUI::HorizontalSplitter>();
    auto& left_frame = splitter.add<GUI::Frame>();
    left_frame.set_layout<GUI::VerticalBoxLayout>().set_margins({ 0, 0, 0, 0 });
    left_frame.set_preferred_size(100, 0);
    left_frame.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_listview = left_frame.add<GUI::ListView>();
    m_listview->set_activates_on_selection(true);
    m_listview->set_model(HelpListModel::create());

    m_webview = splitter.add<Web::OutOfProcessWebView>();

    m_listview->on_activation = [this](auto& index) {
        if (!m_webview)
            return;

        m_webview->load(URL::create_with_data("text/html", render(index)));
    };
}

String HelpWindow::render(const GUI::ModelIndex& index)
{
    auto key = static_cast<HelpListModel*>(m_listview->model())->key(index);
    auto doc_option = m_docs.get(key);
    ASSERT(doc_option.is_object());

    auto& doc = doc_option.as_object();

    auto name = doc.get("name").to_string();
    auto argc = doc.get("argc").to_u32(0);
    auto argnames_value = doc.get("argnames");
    ASSERT(argnames_value.is_array());
    auto& argnames = argnames_value.as_array();

    auto docstring = doc.get("doc").to_string();
    auto examples_value = doc.get_or("examples", JsonObject {});
    ASSERT(examples_value.is_object());
    auto& examples = examples_value.as_object();

    StringBuilder markdown_builder;

    markdown_builder.append("# NAME\n`");
    markdown_builder.append(name);
    markdown_builder.append("`\n\n");

    markdown_builder.append("# ARGUMENTS\n");
    if (argc > 0)
        markdown_builder.appendff("{} required argument(s):\n", argc);
    else
        markdown_builder.appendf("No required arguments.\n");

    for (size_t i = 0; i < argc; ++i)
        markdown_builder.appendff("- `{}`\n", argnames.at(i).to_string());

    if (argc > 0)
        markdown_builder.append("\n");

    if ((size_t)argnames.size() > argc) {
        auto opt_count = argnames.size() - argc;
        markdown_builder.appendff("{} optional argument(s):\n", opt_count);
        for (size_t i = argc; i < (size_t)argnames.size(); ++i)
            markdown_builder.appendff("- `{}`\n", argnames.at(i).to_string());
        markdown_builder.append("\n");
    }

    markdown_builder.append("# DESCRIPTION\n");
    markdown_builder.append(docstring);
    markdown_builder.append("\n\n");

    if (!examples.is_empty()) {
        markdown_builder.append("# EXAMPLES\n");
        examples.for_each_member([&](auto& text, auto& description_value) {
            markdown_builder.appendff("- {}\n\n```js\n{}\n```\n", description_value.to_string(), text);
        });
    }

    auto document = Markdown::Document::parse(markdown_builder.string_view());
    return document->render_to_html();
}

void HelpWindow::set_docs(JsonObject&& docs)
{
    m_docs = move(docs);
    static_cast<HelpListModel*>(m_listview->model())->set_from(m_docs);
    m_listview->update();
}

HelpWindow::~HelpWindow()
{
}

}
