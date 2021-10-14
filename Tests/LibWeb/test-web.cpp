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
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/HTML/Scripting/WindowEnvironmentSettingsObject.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Loader/ResourceLoader.h>

using namespace Test::JS;

TEST_ROOT("Userland/Libraries/LibWeb/Tests");

RefPtr<Web::InProcessWebView> g_page_view;
RefPtr<GUI::Application> g_app;
Optional<URL> next_page_to_load;
Vector<Function<JS::ThrowCompletionOr<void>(JS::Object&)>> after_initial_load_hooks;
Vector<Function<JS::ThrowCompletionOr<void>(JS::Object&)>> before_initial_load_hooks;
RefPtr<Web::DOM::Document> g_current_interpreter_document;

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

struct TestWebGlobalObject : public Web::Bindings::WindowObject {
    JS_OBJECT(TestWebGlobalObject, Web::Bindings::WindowObject);

public:
    TestWebGlobalObject(Web::DOM::Window& window)
        : Web::Bindings::WindowObject(window)
    {
    }

    virtual ~TestWebGlobalObject() override = default;

    virtual void initialize_global_object() override;

    JS_DECLARE_NATIVE_FUNCTION(load_local_page);
    JS_DECLARE_NATIVE_FUNCTION(after_initial_page_load);
    JS_DECLARE_NATIVE_FUNCTION(before_initial_page_load);
    JS_DECLARE_NATIVE_FUNCTION(wait_for_page_to_load);
};

void TestWebGlobalObject::initialize_global_object()
{
    Base::initialize_global_object();

    define_native_function("loadLocalPage", load_local_page, 1, JS::default_attributes);
    define_native_function("afterInitialPageLoad", after_initial_page_load, 1, JS::default_attributes);
    define_native_function("beforeInitialPageLoad", before_initial_page_load, 1, JS::default_attributes);
    define_native_function("waitForPageToLoad", wait_for_page_to_load, 0, JS::default_attributes);
}

TESTJS_CREATE_INTERPRETER_HOOK()
{
    // FIXME: This is a hack as the document we create needs to stay alive the entire time and we don't have insight into JavaScriptTestRUnner from here to work out the lifetime from here.
    g_current_interpreter_document = Web::DOM::Document::create();

    // FIXME: Use WindowProxy as the globalThis value.
    auto interpreter = JS::Interpreter::create<TestWebGlobalObject>(*g_vm, g_current_interpreter_document->window());

    // FIXME: Work out the creation URL.
    AK::URL creation_url;

    Web::HTML::WindowEnvironmentSettingsObject::setup(creation_url, g_vm->running_execution_context());
    return interpreter;
}

JS_DEFINE_NATIVE_FUNCTION(TestWebGlobalObject::load_local_page)
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

JS_DEFINE_NATIVE_FUNCTION(TestWebGlobalObject::after_initial_page_load)
{
    auto function = vm.argument(0);
    if (!function.is_function()) {
        dbgln("afterInitialPageLoad argument is not a function");
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Function");
    }

    after_initial_load_hooks.append([fn = JS::make_handle(&function.as_function()), &global_object](auto& page_object) -> JS::ThrowCompletionOr<void> {
        TRY(JS::call(global_object, const_cast<JS::FunctionObject&>(*fn.cell()), JS::js_undefined(), &page_object));
        return {};
    });
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(TestWebGlobalObject::before_initial_page_load)
{
    auto function = vm.argument(0);
    if (!function.is_function()) {
        dbgln("beforeInitialPageLoad argument is not a function");
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Function");
    }

    before_initial_load_hooks.append([fn = JS::make_handle(&function.as_function()), &global_object](auto& page_object) -> JS::ThrowCompletionOr<void> {
        TRY(JS::call(global_object, const_cast<JS::FunctionObject&>(*fn.cell()), JS::js_undefined(), &page_object));
        return {};
    });
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(TestWebGlobalObject::wait_for_page_to_load)
{
    // Create a new parser and immediately get its document to replace the old interpreter.
    auto document = Web::DOM::Document::create();

    // Run the "before" hooks
    for (auto& entry : before_initial_load_hooks)
        TRY(entry(document->interpreter().global_object()));

    // Set the load hook
    Web::LoadRequest request;
    request.set_url(next_page_to_load.value());

    JS::ThrowCompletionOr<void> result = {};

    auto& loader = Web::ResourceLoader::the();
    loader.load_sync(
        request,
        [&](auto data, auto&, auto) {
            Web::HTML::HTMLParser parser(document, data, "utf-8");
            // Now parse the HTML page.
            parser.run(next_page_to_load.value());
            g_page_view->set_document(&parser.document());
            // Note: Unhandled exceptions are just dropped here.

            // Run the "after" hooks
            for (auto& entry : after_initial_load_hooks) {
                auto ran_or_error = entry(document->interpreter().global_object());
                if (ran_or_error.is_error()) {
                    result = ran_or_error.release_error();
                    break;
                }
            }
        },
        [&](auto&, auto) {
            dbgln("Load of resource {} failed", next_page_to_load.value());
            result = vm.template throw_completion<JS::TypeError>(global_object, "Resource load failed");
        });

    if (result.is_error())
        return result.release_error();
    return JS::js_undefined();
}
