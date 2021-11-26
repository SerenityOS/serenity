# Adding a new IDL file

Serenity's build system does a lot of work of turning the IDL from a Web spec into code, but there are a few things you'll need to do yourself.

For the sake of example, let's say you're wanting to add the `HTMLDetailsElement`.

1. Create `LibWeb/HTML/HTMLDetailsElement.idl` with the contents of the IDL section of the spec. In this case, that would be:
```webidl
[Exposed=Window]
interface HTMLDetailsElement : HTMLElement {
    [HTMLConstructor] constructor();

    [CEReactions] attribute boolean open;
};
```

2. If the IDL starts with `[Exposed=Window]`, remove that line from the .idl file, and add the following to [`LibWeb/Bindings/WindowObjectHelper.h`](../../Userland/Libraries/LibWeb/Bindings/WindowObjectHelper.h):
    - `#include <LibWeb/Bindings/HTMLDetailsElementConstructor.h>` and
    - `#include <LibWeb/Bindings/HTMLDetailsElementPrototype.h>` to the includes list.
    - `ADD_WINDOW_OBJECT_INTERFACE(HTMLDetailsElement)      \` to the macro at the bottom.

3. Add a `libweb_js_wrapper()` call to [`LibWeb/CMakeLists.txt`](../../Userland/Libraries/LibWeb/CMakeLists.txt)

4. Forward declare the generated classes in [`LibWeb/Forward.h`](../../Userland/Libraries/LibWeb/Forward.h):
    - `HTMLDetailsElement` in its namespace.
    - `HTMLDetailsElementWrapper` in the `Web::Bindings` namespace.

5. If your interface is an Event type:
    - Add `#import <DOM/Event.idl>` at the top of the IDL file.
    - Open [`LibWeb/Bindings/EventWrapperFactory.cpp`](../../Userland/Libraries/LibWeb/Bindings/EventWrapperFactory.cpp) and add an `#include` directive and `if` statement for your new Event type.

