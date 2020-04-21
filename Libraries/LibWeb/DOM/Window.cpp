/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibCore/Timer.h>
#include <LibGUI/DisplayLink.h>
#include <LibGUI/MessageBox.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibWeb/DOM/Window.h>

namespace Web {

NonnullRefPtr<Window> Window::create_with_document(Document& document)
{
    return adopt(*new Window(document));
}

Window::Window(Document& document)
    : m_document(document)
{
}

Window::~Window()
{
}

void Window::alert(const String& message)
{
    GUI::MessageBox::show(message, "Alert", GUI::MessageBox::Type::Information);
}

bool Window::confirm(const String& message)
{
    auto confirm_result = GUI::MessageBox::show(message, "Confirm", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OKCancel);
    return confirm_result == GUI::Dialog::ExecResult::ExecOK;
}

void Window::set_interval(JS::Function& callback, i32 interval)
{
    // FIXME: This leaks the interval timer and makes it unstoppable.
    (void)Core::Timer::construct(interval, [handle = make_handle(&callback)] {
        auto* function = const_cast<JS::Function*>(static_cast<const JS::Function*>(handle.cell()));
        auto& interpreter = function->interpreter();
        interpreter.call(function);
    }).leak_ref();
}

void Window::set_timeout(JS::Function& callback, i32 interval)
{
    // FIXME: This leaks the interval timer and makes it unstoppable.
    auto& timer = Core::Timer::construct(interval, [handle = make_handle(&callback)] {
        auto* function = const_cast<JS::Function*>(static_cast<const JS::Function*>(handle.cell()));
        auto& interpreter = function->interpreter();
        interpreter.call(function);
    }).leak_ref();
    timer.set_single_shot(true);
}

i32 Window::request_animation_frame(JS::Function& callback)
{
    // FIXME: This is extremely fake!
    static double fake_timestamp = 0;

    i32 link_id = GUI::DisplayLink::register_callback([handle = make_handle(&callback)](i32 link_id) {
        auto* function = const_cast<JS::Function*>(static_cast<const JS::Function*>(handle.cell()));
        auto& interpreter = function->interpreter();
        JS::MarkedValueList arguments(interpreter.heap());
        arguments.append(JS::Value(fake_timestamp));
        fake_timestamp += 10;
        interpreter.call(function, {}, move(arguments));
        GUI::DisplayLink::unregister_callback(link_id);
    });

    // FIXME: Don't hand out raw DisplayLink ID's to JavaScript!
    return link_id;
}

void Window::cancel_animation_frame(i32 id)
{
    // FIXME: We should not be passing untrusted numbers to DisplayLink::unregister_callback()!
    GUI::DisplayLink::unregister_callback(id);
}

}
