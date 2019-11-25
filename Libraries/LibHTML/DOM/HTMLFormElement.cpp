#include <AK/StringBuilder.h>
#include <LibHTML/DOM/HTMLFormElement.h>
#include <LibHTML/DOM/HTMLInputElement.h>
#include <LibHTML/Frame.h>
#include <LibHTML/HtmlView.h>

HTMLFormElement::HTMLFormElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLFormElement::~HTMLFormElement()
{
}

void HTMLFormElement::submit()
{
    if (action().is_null()) {
        dbg() << "Unsupported form action ''";
        return;
    }

    if (method().to_lowercase() != "get") {
        dbg() << "Unsupported form method '" << method() << "'";
        return;
    }

    URL url(document().complete_url(action()));

    struct NameAndValue {
        String name;
        String value;
    };

    Vector<NameAndValue> parameters;

    for_each_in_subtree([&](auto& node) {
        if (is<HTMLInputElement>(node)) {
            auto& input = to<HTMLInputElement>(node);
            if (!input.name().is_null())
                parameters.append({ input.name(), input.value() });
        }
        return IterationDecision::Continue;
    });

    StringBuilder builder;
    for (int i = 0; i < parameters.size(); ++i) {
        builder.append(parameters[i].name);
        builder.append('=');
        builder.append(parameters[i].value);
        if (i != parameters.size() - 1)
            builder.append('&');
    }
    url.set_query(builder.to_string());

    // FIXME: We shouldn't let the form just do this willy-nilly.
    document().frame()->html_view()->load(url);
}
