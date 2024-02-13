/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/Focus.h>
#include <LibWeb/HTML/HTMLDialogElement.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLDialogElement);

HTMLDialogElement::HTMLDialogElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDialogElement::~HTMLDialogElement() = default;

void HTMLDialogElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLDialogElementPrototype>(realm, "HTMLDialogElement"_fly_string));
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-show
WebIDL::ExceptionOr<void> HTMLDialogElement::show()
{
    // 1. If this has an open attribute and the is modal flag of this is false, then return.
    // FIXME: Add modal flag check here when modal dialog support is added
    if (has_attribute(AttributeNames::open))
        return {};

    // FIXME: 2. If this has an open attribute, then throw an "InvalidStateError" DOMException.

    // 3. Add an open attribute to this, whose value is the empty string.
    TRY(set_attribute(AttributeNames::open, {}));

    // FIXME 4. Set this's previously focused element to the focused element.
    // FIXME 5. Run hide all popovers given this's node document.

    // 6. Run the dialog focusing steps given this.
    run_dialog_focusing_steps();

    return {};
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-showmodal
void HTMLDialogElement::show_modal()
{
    dbgln("(STUBBED) HTMLDialogElement::show_modal()");
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-close
void HTMLDialogElement::close(Optional<String> return_value)
{
    // 1. If returnValue is not given, then set it to null.
    // 2. Close the dialog this with returnValue.
    close_the_dialog(move(return_value));
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-returnvalue
String HTMLDialogElement::return_value() const
{
    return m_return_value;
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-returnvalue
void HTMLDialogElement::set_return_value(String return_value)
{
    m_return_value = move(return_value);
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#close-the-dialog
void HTMLDialogElement::close_the_dialog(Optional<String> result)
{
    // 1. If subject does not have an open attribute, then return.
    if (!has_attribute(AttributeNames::open))
        return;

    // 2. Remove subject's open attribute.
    remove_attribute(AttributeNames::open);

    // FIXME: 3. If the is modal flag of subject is true, then request an element to be removed from the top layer given subject.
    // FIXME: 4. Let wasModal be the value of subject's is modal flag.
    // FIXME: 5. Set the is modal flag of subject to false.

    // 6. If result is not null, then set the returnValue attribute to result.
    if (result.has_value())
        set_return_value(result.release_value());

    // FIXME: 7. If subject's previously focused element is not null, then:
    //           1. Let element be subject's previously focused element.
    //           2. Set subject's previously focused element to null.
    //           3. If subject's node document's focused area of the document's DOM anchor is a shadow-including inclusive descendant of element,
    //              or wasModal is true, then run the focusing steps for element; the viewport should not be scrolled by doing this step.

    // 8. Queue an element task on the user interaction task source given the subject element to fire an event named close at subject.
    queue_an_element_task(HTML::Task::Source::UserInteraction, [this] {
        auto close_event = DOM::Event::create(realm(), HTML::EventNames::close);
        dispatch_event(close_event);
    });

    // FIXME: 9. If subject's close watcher is not null, then:
    //           1. Destroy subject's close watcher.
    //           2. Set subject's close watcher to null.
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dialog-focusing-steps
void HTMLDialogElement::run_dialog_focusing_steps()
{
    // 1. Let control be null
    JS::GCPtr<Element> control = nullptr;

    // FIXME 2. If subject has the autofocus attribute, then set control to subject.
    // FIXME 3. If control is null, then set control to the focus delegate of subject.

    // 4. If control is null, then set control to subject.
    if (!control)
        control = this;

    // 5. Run the focusing steps for control.
    run_focusing_steps(control);
}

}
