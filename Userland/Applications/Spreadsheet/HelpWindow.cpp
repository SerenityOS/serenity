/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HelpWindow.h"
#include "SpreadsheetWidget.h"
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Frame.h>
#include <LibGUI/ListView.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/Splitter.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/Layout/Node.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Spreadsheet {

class HelpListModel final : public GUI::Model {
public:
    static NonnullRefPtr<HelpListModel> create() { return adopt_ref(*new HelpListModel); }

    virtual ~HelpListModel() override = default;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_keys.size(); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role = GUI::ModelRole::Display) const override
    {
        if (role == GUI::ModelRole::Display) {
            return key(index);
        }

        return {};
    }

    String key(const GUI::ModelIndex& index) const { return m_keys[index.row()]; }

    void set_from(JsonObject const& object)
    {
        m_keys.clear();
        object.for_each_member([this](auto& name, auto&) {
            m_keys.append(name);
        });
        AK::quick_sort(m_keys);
        invalidate();
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
    set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-help.png"sv).release_value_but_fixme_should_propagate_errors());

    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.set_fill_with_background_color(true);

    auto& splitter = widget.add<GUI::HorizontalSplitter>();
    auto& left_frame = splitter.add<GUI::Frame>();
    left_frame.set_layout<GUI::VerticalBoxLayout>();
    left_frame.set_fixed_width(100);
    m_listview = left_frame.add<GUI::ListView>();
    m_listview->set_activates_on_selection(true);
    m_listview->set_model(HelpListModel::create());

    m_webview = splitter.add<WebView::OutOfProcessWebView>();
    m_webview->on_link_click = [this](auto& url, auto&, auto&&) {
        VERIFY(url.protocol() == "spreadsheet");
        if (url.host() == "example") {
            auto entry = LexicalPath::basename(url.path());
            auto doc_option = m_docs.get(entry);
            if (!doc_option.is_object()) {
                GUI::MessageBox::show_error(this, String::formatted("No documentation entry found for '{}'", url.path()));
                return;
            }
            auto& doc = doc_option.as_object();
            const auto& name = url.fragment();

            auto* example_data_ptr = doc.get_ptr("example_data"sv);
            if (!example_data_ptr || !example_data_ptr->is_object()) {
                GUI::MessageBox::show_error(this, String::formatted("No example data found for '{}'", url.path()));
                return;
            }
            auto& example_data = example_data_ptr->as_object();

            if (!example_data.has_object(name)) {
                GUI::MessageBox::show_error(this, String::formatted("Example '{}' not found for '{}'", name, url.path()));
                return;
            }
            auto& value = example_data.get(name);

            auto window = GUI::Window::construct(this);
            window->resize(size());
            window->set_icon(icon());
            window->set_title(String::formatted("Spreadsheet Help - Example {} for {}", name, entry));
            window->on_close = [window = window.ptr()] { window->remove_from_parent(); };

            auto& widget = window->set_main_widget<SpreadsheetWidget>(window, NonnullRefPtrVector<Sheet> {}, false);
            auto sheet = Sheet::from_json(value.as_object(), widget.workbook());
            if (!sheet) {
                GUI::MessageBox::show_error(this, String::formatted("Corrupted example '{}' in '{}'", name, url.path()));
                return;
            }

            widget.add_sheet(sheet.release_nonnull());
            window->show();
        } else if (url.host() == "doc") {
            auto entry = LexicalPath::basename(url.path());
            m_webview->load(URL::create_with_data("text/html", render(entry)));
        } else {
            dbgln("Invalid spreadsheet action domain '{}'", url.host());
        }
    };

    m_listview->on_activation = [this](auto& index) {
        if (!m_webview)
            return;

        auto key = static_cast<HelpListModel*>(m_listview->model())->key(index);
        m_webview->load(URL::create_with_data("text/html", render(key)));
    };
}

String HelpWindow::render(StringView key)
{
    VERIFY(m_docs.has_object(key));
    auto& doc = m_docs.get(key).as_object();

    auto name = doc.get("name"sv).to_string();
    auto argc = doc.get("argc"sv).to_u32(0);
    VERIFY(doc.has_array("argnames"sv));
    auto& argnames = doc.get("argnames"sv).as_array();

    auto docstring = doc.get("doc"sv).to_string();

    StringBuilder markdown_builder;

    markdown_builder.append("# NAME\n`"sv);
    markdown_builder.append(name);
    markdown_builder.append("`\n\n"sv);

    markdown_builder.append("# ARGUMENTS\n"sv);
    if (argc > 0)
        markdown_builder.appendff("{} required argument(s):\n", argc);
    else
        markdown_builder.append("No required arguments.\n"sv);

    for (size_t i = 0; i < argc; ++i)
        markdown_builder.appendff("- `{}`\n", argnames.at(i).to_string());

    if (argc > 0)
        markdown_builder.append("\n"sv);

    if ((size_t)argnames.size() > argc) {
        auto opt_count = argnames.size() - argc;
        markdown_builder.appendff("{} optional argument(s):\n", opt_count);
        for (size_t i = argc; i < (size_t)argnames.size(); ++i)
            markdown_builder.appendff("- `{}`\n", argnames.at(i).to_string());
        markdown_builder.append("\n"sv);
    }

    markdown_builder.append("# DESCRIPTION\n"sv);
    markdown_builder.append(docstring);
    markdown_builder.append("\n\n"sv);

    if (doc.has("examples"sv)) {
        auto& examples = doc.get("examples"sv);
        VERIFY(examples.is_object());
        markdown_builder.append("# EXAMPLES\n"sv);
        examples.as_object().for_each_member([&](auto& text, auto& description_value) {
            dbgln("```js\n{}\n```\n\n- {}\n", text, description_value.to_string());
            markdown_builder.appendff("```js\n{}\n```\n\n- {}\n", text, description_value.to_string());
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
}
