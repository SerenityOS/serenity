/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibTest/JavaScriptTestRunner.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Loader/ResourceLoader.h>

using namespace Test::JS;

TEST_ROOT("Userland/Libraries/LibWeb/Tests");

RefPtr<Web::InProcessWebView> g_page_view;
RefPtr<GUI::Application> g_app;
Optional<URL> next_page_to_load;
Vector<Function<void(JS::Object&)>> after_initial_load_hooks;
Vector<Function<void(JS::Object&)>> before_initial_load_hooks;

TESTJS_MAIN_HOOK()
{
    g_vm = Web::Bindings::main_thread_vm();
    g_app = GUI::Application::construct(g_test_argc, g_test_argv);
    auto window = GUI::Window::construct();
    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    auto& view = main_widget.add<Web::InProcessWebView>();
    view.set_document(Web::DOM::Document::create());
    g_page_view = view;
}

TESTJS_GLOBAL_FUNCTION(load_local_page, loadLocalPage)
{
    auto name = TRY(vm.argument(0).to_string(global_object));

    // Clear the hooks
    before_initial_load_hooks.clear();
    after_initial_load_hooks.clear();

    // Set the load URL
    if (name.starts_with('/'))
        next_page_to_load = URL::create_with_file_protocol(name);
    else
        next_page_to_load = URL::create_with_file_protocol(LexicalPath::join(g_test_root, "Pages", name).string());
    return JS::js_undefined();
}

TESTJS_GLOBAL_FUNCTION(after_initial_page_load, afterInitialPageLoad)
{
    auto function = vm.argument(0);
    if (!function.is_function()) {
        dbgln("afterInitialPageLoad argument is not a function");
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Function");
    }

    after_initial_load_hooks.append([fn = JS::make_handle(&function.as_function()), &global_object](auto& page_object) {
        [[maybe_unused]] auto unused = JS::call(global_object, const_cast<JS::FunctionObject&>(*fn.cell()), JS::js_undefined(), &page_object);
    });
    return JS::js_undefined();
}

TESTJS_GLOBAL_FUNCTION(before_initial_page_load, beforeInitialPageLoad)
{
    auto function = vm.argument(0);
    if (!function.is_function()) {
        dbgln("beforeInitialPageLoad argument is not a function");
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Function");
    }

    before_initial_load_hooks.append([fn = JS::make_handle(&function.as_function()), &global_object](auto& page_object) {
        [[maybe_unused]] auto unused = JS::call(global_object, const_cast<JS::FunctionObject&>(*fn.cell()), JS::js_undefined(), &page_object);
    });
    return JS::js_undefined();
}

TESTJS_GLOBAL_FUNCTION(wait_for_page_to_load, waitForPageToLoad)
{
    // Create a new parser and immediately get its document to replace the old interpreter.
    auto document = Web::DOM::Document::create();

    // Run the "before" hooks
    for (auto& entry : before_initial_load_hooks)
        entry(document->interpreter().global_object());

    // Set the load hook
    Web::LoadRequest request;
    request.set_url(next_page_to_load.value());

    auto& loader = Web::ResourceLoader::the();
    loader.load_sync(
        request,
        [&](auto data, auto&, auto) {
            Web::HTML::HTMLParser parser(document, data, "utf-8");
            // Now parse the HTML page.
            parser.run(next_page_to_load.value());
            g_page_view->set_document(&parser.document());
            if (vm.exception()) {
                // FIXME: Should we do something about this? the document itself threw unhandled exceptions...
                vm.clear_exception();
            }

            // Run the "after" hooks
            for (auto& entry : after_initial_load_hooks) {
                entry(document->interpreter().global_object());
                if (vm.exception())
                    break;
            }
        },
        [&](auto&, auto) {
            dbgln("Load of resource {} failed", next_page_to_load.value());
            vm.throw_exception<JS::TypeError>(global_object, "Resource load failed");
        });

    return JS::js_undefined();
}
