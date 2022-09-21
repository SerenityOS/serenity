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

2. If the IDL refers to other IDL types, you need to import those. For example, `CSSRule` has an attribute that returns a `CSSStyleSheet`, so that needs to be imported:
```webidl
#import <CSS/CSSStyleSheet.idl>

interface CSSRule {
    readonly attribute CSSStyleSheet? parentStyleSheet;
};
```

3. If the IDL starts with `[Exposed=Window]`, add the following to [`LibWeb/Bindings/WindowObjectHelper.h`](../../Userland/Libraries/LibWeb/Bindings/WindowObjectHelper.h):
    - `#include <LibWeb/Bindings/HTMLDetailsElementConstructor.h>` and
    - `#include <LibWeb/Bindings/HTMLDetailsElementPrototype.h>` to the includes list.
    - `ADD_WINDOW_OBJECT_INTERFACE(HTMLDetailsElement)      \` to the macro at the bottom.

4. Add a `libweb_js_bindings(HTML/HTMLDetailsElement)` call to [`LibWeb/idl_files.cmake`](../../Userland/Libraries/LibWeb/idl_files.cmake)

5. Forward declare the generated classes in [`LibWeb/Forward.h`](../../Userland/Libraries/LibWeb/Forward.h):
    - `HTMLDetailsElement` in its namespace.
    - `HTMLDetailsElementWrapper` in the `Web::Bindings` namespace.

6. The C++ class equivalent of the IDL interface has a few requirements:
   - It must inherit from `public RefCounted<HTMLDetailsElement>` and `public Bindings::Wrappable`
   - It must have a public `using WrapperType = Bindings::HTMLDetailsElementWrapper;`

8. If your type isn't an Event or Element, you will need to add it to [`is_wrappable_type()`](../../Meta/Lagom/Tools/CodeGenerators/LibWeb/BindingsGenerator/IDLGenerators.cpp)
   so that it can be accepted as an IDL parameter, attribute or return type.
