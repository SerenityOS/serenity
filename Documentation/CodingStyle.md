# Serenity C++ coding style

This document describes the coding style used for C++ code in the Serenity Operating System project. All new code should conform to this style.

We'll definitely be tweaking and amending this over time, so let's consider it a living document. :)

### Indentation

[](#indentation-no-tabs) Use spaces, not tabs. Tabs should only appear in files that require them for semantic meaning, like Makefiles.

[](#indentation-4-spaces) The indent size is 4 spaces.

###### Right:

```cpp
int main()
{
    return 0;
}
```

###### Wrong:

```cpp
int main() 
{
        return 0;
}
```

[](#indentation-namespace) The contents of an outermost `namespace` block should not be indented.

###### Right:

```cpp
// Container.h
namespace AK {

class Container {
    Container();
    ...
};

}

// Container.cpp
namespace AK {

Container::Container()
{
    ...
}

}
```

###### Wrong:

```cpp
// Container.h
namespace AK {

    class Container {
        Container();
        ...
    };

}

// Container.cpp
namespace AK {

    Container::Container()
    {
        ...
    }

}
```

[](#indentation-case-label) A case label should line up with its switch statement. The case statement is indented.

###### Right:

```cpp
switch (condition) {
case FooCondition:
case BarCondition:
    ++i;
    break;
default:
    ++i;
}
```

###### Wrong:

```cpp
switch (condition) {
    case FooCondition:
    case BarCondition:
        ++i;
        break;
    default:
        --i;
}
```

[](#indentation-wrap-bool-op) Boolean expressions at the same nesting level that span multiple lines should have their operators on the left side of the line instead of the right side.

###### Right:

```cpp
return type == Type::SomeType
    || type == Type::OtherType
    || (another_condition && !bad_thing);
```


###### Wrong:

```cpp
return type == Type::SomeType ||
    type == Type::OtherType ||
    (another_condition && !bad_thing);
```


### Spacing

[](#spacing-unary-op) Do not place spaces around unary operators.

###### Right:

```cpp
++i;
```


###### Wrong:

```cpp
++ i;
```

[](#spacing-binary-ternary-op) **Do** place spaces around binary and ternary operators.

###### Right:

```cpp
y = m * x + b;
f(a, b);
c = a | b;
return condition ? 1 : 0;
```

###### Wrong:

```cpp
y=m*x+b;
f(a,b);
c = a|b;
return condition ? 1:0;
```

[](#spacing-for-colon) Place spaces around the colon in a range-based for loop.

###### Right:

```cpp
Vector<ModuleInfo> modules;
for (auto& module : modules)
    register_module(module);
```

###### Wrong:

```cpp
Vector<ModuleInfo> modules;
for (auto& module: modules)
    register_module(module);
```

[](#spacing-comma-semicolon) Do not place spaces before comma and semicolon.

###### Right:

```cpp
for (int i = 0; i < 10; ++i)
    do_something();

f(a, b);
```

###### Wrong:

```cpp
for (int i = 0 ; i < 10 ; ++i)
    do_something();

f(a , b) ;
```

[](#spacing-control-paren) Place spaces between control statements and their parentheses.

###### Right:

```cpp
if (condition)
    do_it();
```

###### Wrong:

```cpp
if(condition)
    do_it();
```

[](#spacing-function-paren) Do not place spaces between a function and its parentheses, or between a parenthesis and its content.

###### Right:

```cpp
f(a, b);
```

###### Wrong:

```cpp
f (a, b);
f( a, b );
```

[](#spacing-braced-init) When initializing an object, place a space before the leading brace as well as between the braces and their content.

###### Right:

```cpp
Foo foo { bar };
```

###### Wrong:

```cpp
Foo foo{ bar };
Foo foo {bar};
```

### Line breaking

[](#linebreaking-multiple-statements) Each statement should get its own line.

###### Right:

```cpp
++x;
++y;
if (condition)
    do_it();
```

###### Wrong:

```cpp
x++; y++;
if (condition) do_it();
```

[](#linebreaking-else-braces) An `else` statement should go on the same line as a preceding close brace if one is present, else it should line up with the `if` statement.

###### Right:

```cpp
if (condition) {
    ...
} else {
    ...
}

if (condition)
    do_something();
else
    do_something_else();

if (condition) {
    do_something();
} else {
    ...
}
```

###### Wrong:

```cpp
if (condition) {
    ...
}
else {
    ...
}

if (condition) do_something(); else do_something_else();

if (condition) do_something(); else {
    ...
}
```

[](#linebreaking-else-if) An `else if` statement should be written as an `if` statement when the prior `if` concludes with a `return` statement.

###### Right:

```cpp
if (condition) {
    ...
    return some_value;
}
if (condition) {
    ...
}
```

###### Wrong:

```cpp
if (condition) {
    ...
    return some_value;
} else if (condition) {
    ...
}
```

### Braces

[](#braces-function) Function definitions: place each brace on its own line.

###### Right:

```cpp
int main()
{
    ...
}
```

###### Wrong:

```cpp
int main() {
    ...
}
```

[](#braces-blocks) Other braces: place the open brace on the line preceding the code block; place the close brace on its own line.

###### Right:

```cpp
class MyClass {
    ...
};

namespace AK {
    ...
}

for (int i = 0; i < 10; ++i) {
    ...
}
```

###### Wrong:

```cpp
class MyClass 
{
    ...
};
```

[](#braces-one-line) One-line control clauses should not use braces unless comments are included or a single statement spans multiple lines.

###### Right:

```cpp
if (condition)
    do_it();

if (condition) {
    // Some comment.
    do_it();
}

if (condition) {
    my_function(really_long_param1, really_long_param2, ...
        really_long_param5);
}
```

###### Wrong:

```cpp
if (condition) {
    do_it();
}

if (condition)
    // Some comment.
    do_it();

if (condition)
    my_function(really_long_param1, really_long_param2, ...
        really_long_param5);
```

[](#braces-empty-block) Control clauses without a body should use empty braces:

###### Right:

```cpp
for ( ; current; current = current->next) { }
```

###### Wrong:

```cpp
for ( ; current; current = current->next);
```

### Null, false and zero

[](#zero-null) In C++, the null pointer value should be written as `nullptr`. In C, it should be written as `NULL`.

[](#zero-bool) C++ and C `bool` values should be written as `true` and `false`.

[](#zero-comparison) Tests for true/false, null/non-null, and zero/non-zero should all be done without equality comparisons.

###### Right:

```cpp
if (condition)
    do_it();

if (!ptr)
    return;

if (!count)
    return;
```

###### Wrong:

```cpp
if (condition == true)
    do_it();

if (ptr == nullptr)
    return;

if (count == 0)
    return;
```

### Floating point literals

[](#float-suffixes) Unless required in order to force floating point math, do not append `.0`, `.f` and `.0f` to floating point literals.

###### Right:

```cpp
const double duration = 60;

void set_diameter(float diameter)
{
    radius = diameter / 2;
}

set_diameter(10);

const int frames_per_second = 12;
double frame_duration = 1.0 / frames_per_second;
```

###### Wrong:

```cpp
const double duration = 60.0;

void set_diameter(float diameter)
{
    radius = diameter / 2.f;
}

set_diameter(10.f);

const int frames_per_second = 12;
double frame_duration = 1 / frames_per_second; // Integer division.
```

### Names

[](#names-basic) A combination of CamelCase and snake_case. Use CamelCase (Capitalize the first letter, including all letters in an acronym) in a class, struct, or namespace name. Use snake\_case (all lowercase, with underscores separating words) for variable and function names.

###### Right:

```cpp
struct Entry;
size_t buffer_size;
class FileDescriptor;
String absolute_path();
```

###### Wrong:

```cpp
struct data;
size_t bufferSize;
class Filedescriptor;
String MIME_Type();
```

[](#names-full-words) Use full words, except in the rare case where an abbreviation would be more canonical and easier to understand.

###### Right:

```cpp
size_t character_size;
size_t length;
short tab_index; // More canonical.
```

###### Wrong:

```cpp
size_t char_size;
size_t len;
short tabulation_index; // Bizarre.
```

[](#names-data-members) Data members in C++ classes should be private. Static data members should be prefixed by "s\_". Other data members should be prefixed by "m\_". Global variables should be prefixed by "g\_".

###### Right:

```cpp
class String {
public:
    ...

private:
    int m_length;
};
```

###### Wrong:

```cpp
class String {
public:
    ...

    int length;
};
```

[](#names-bool) Precede boolean values with words like "is" and "did".

###### Right:

```cpp
bool is_valid;
bool did_send_data;
```

###### Wrong:

```cpp
bool valid;
bool sent_data;
```

[](#names-setter-getter) Precede setters with the word "set". Use bare words for getters. Setter and getter names should match the names of the variables being set/gotten.

###### Right:

```cpp
void set_count(int); // Sets m_count.
int count() const; // Returns m_count.
```

###### Wrong:

```cpp
void set_count(int); // Sets m_the_count.
int get_count() const; // Returns m_the_count.
```

[](#names-out-argument) Precede getters that return values through out arguments with the word "get".

###### Right:

```cpp
void get_filename_and_inode_id(String&, InodeIdentifier&) const;
```

###### Wrong:

```cpp
void filename_and_inode_id(String&, InodeIdentifier&) const;
```

[](#names-verb) Use descriptive verbs in function names.

###### Right:

```cpp
bool convert_to_ascii(short*, size_t);
```

###### Wrong:

```cpp
bool to_ascii(short*, size_t);
```

[](#names-if-exists) The getter function for a member variable should not have any suffix or prefix indicating the function can optionally create or initialize the member variable. Prefix the getter function which automatically creates the object with `ensure_` if there is a variant which doesn't.

###### Right:

```cpp
Inode* inode();
Inode& ensure_inode();
```

###### Wrong:

```cpp
Inode& inode();
Inode* ensure_inode();
```

###### Right:

```cpp
Frame* frame();
```

###### Wrong:

```cpp
Frame* ensure_frame();
```

[](#names-variable-name-in-function-decl) Leave meaningless variable names out of function declarations. A good rule of thumb is if the parameter type name contains the parameter name (without trailing numbers or pluralization), then the parameter name isn't needed. Usually, there should be a parameter name for bools, strings, and numerical types.

###### Right:

```cpp
void set_count(int);

void do_something(Context*);
```

###### Wrong:

```cpp
void set_count(int count);

void do_something(Context* context);
```

[](#names-enum-to-bool) Prefer enums to bools on function parameters if callers are likely to be passing constants, since named constants are easier to read at the call site. An exception to this rule is a setter function, where the name of the function already makes clear what the boolean is.

###### Right:

```cpp
do_something(something, AllowFooBar);
paint_text_with_shadows(context, ..., text_stroke_width > 0, is_horizontal());
set_resizable(false);
```

###### Wrong:

```cpp
do_something(something, false);
set_resizable(NotResizable);
```

[](#names-enum-members) Enum members should use InterCaps with an initial capital letter.

[](#names-const-to-define) Prefer `const` to `#define`. Prefer inline functions to macros.

[](#names-define-constants) `#defined` constants should use all uppercase names with words separated by underscores.

[](#header-guards) Use `#pragma once` instead of `#define` and `#ifdef` for header guards.

###### Right:

```cpp
// MyClass.h
#pragma once
```

###### Wrong:

```cpp
// MyClass.h
#ifndef MyClass_h
#define MyClass_h
```

### Other Punctuation

[](#punctuation-member-init) Constructors for C++ classes should initialize their members using C++ initializer syntax. Each member (and superclass) should be indented on a separate line, with the colon or comma preceding the member on that line. Prefer initialization at member definition whenever possible.

###### Right:

```cpp
class MyClass {
    ...
    Document* m_document { nullptr };
    int m_my_member { 0 };
};

MyClass::MyClass(Document* document)
    : MySuperClass()
    , m_document(document)
{
}

MyOtherClass::MyOtherClass()
    : MySuperClass()
{
}
```

###### Wrong:

```cpp
MyClass::MyClass(Document* document) : MySuperClass()
{
    m_myMember = 0;
    m_document = document;
}

MyClass::MyClass(Document* document) : MySuperClass()
    : m_my_member(0) // This should be in the header.
{
    m_document = document;
}

MyOtherClass::MyOtherClass() : MySuperClass() {}
```

[](#punctuation-vector-index) Prefer index or range-for over iterators in Vector iterations for terse, easier-to-read code.

###### Right:

```cpp
for (auto& child : children)
    child->do_child_thing();
```


#### OK:

```cpp
for (int i = 0; i < children.size(); ++i)
    children[i]->do_child_thing();
```

###### Wrong:

```cpp
for (auto it = children.begin(); it != children.end(); ++it)
    (*it)->do_child_thing();
```

### Pointers and References

[](#pointers-cpp) **Pointer and reference types in C++ code**
Both pointer types and reference types should be written with no space between the type name and the `*` or `&`.

###### Right:

```cpp
GraphicsBitmap* Thingy::bitmap_for(Context context)
{
    auto* bitmap = static_cast<SVGStyledElement*>(node());
    const KCDashArray& dashes = dashArray();
```

###### Wrong:

```cpp
Image *SVGStyledElement::doSomething(PaintInfo &paintInfo)
{
    SVGStyledElement *element = static_cast<SVGStyledElement *>(node());
    const KCDashArray &dashes = dashArray();
```

[](#pointers-out-argument) An out argument of a function should be passed by reference except rare cases where it is optional in which case it should be passed by pointer.

###### Right:

```cpp
void MyClass::get_some_value(OutArgumentType& out_argument) const
{
    out_argument = m_value;
}

void MyClass::do_something(OutArgumentType* out_argument) const
{
    do_the_thing();
    if (out_argument)
        *out_argument = m_value;
}
```

###### Wrong:

```cpp
void MyClass::get_some_value(OutArgumentType* outArgument) const
{
    *out_argument = m_value;
}
```

### #include Statements

[](#include-others) `#include` statements should be in sorted order (case sensitive, as done by the command-line sort tool or an IDE sort selection command). Don't bother to organize them in a logical order.

###### Right:

```cpp
// HTMLDivElement.cpp
#include "Attribute.h"
#include "HTMLElement.h"
#include "QualifiedName.h"
```

###### Wrong:

```cpp
// HTMLDivElement.cpp
#include "HTMLElement.h"
#include "HTMLDivElement.h"
#include "QualifiedName.h"
#include "Attribute.h"
```

### "using" Statements

[](#using-ak) In header files in the AK sub-library, however, it is acceptable to use "using" declarations at the end of the file to import one or more names in the AK namespace into the global scope.

###### Right:

```cpp
// AK/Vector.h

namespace AK {

} // namespace AK

using AK::Vector;
```

###### Wrong:

```cpp
// AK/Vector.h

namespace AK {

} // namespace AK

using namespace AK;
```

###### Wrong:

```cpp
// runtime/Object.h

namespace AK {

} // namespace AK

using AK::SomethingOrOther;
```

[](#using-in-cpp) In C++ implementation files, do not use "using" declarations of any kind to import names in the standard template library. Directly qualify the names at the point they're used instead.

###### Right:

```cpp
// File.cpp

std::swap(a, b);
c = std::numeric_limits<int>::max()
```

###### Wrong:

```cpp
// File.cpp

using std::swap;
swap(a, b);
```

###### Wrong:

```cpp
// File.cpp

using namespace std;
swap(a, b);
```

### Types

[](#types-unsigned) Omit "int" when using "unsigned" modifier. Do not use "signed" modifier. Use "int" by itself instead.

###### Right:

```cpp
unsigned a;
int b;
```

###### Wrong:

```cpp
unsigned int a; // Doesn't omit "int".
signed b; // Uses "signed" instead of "int".
signed int c; // Doesn't omit "signed".
```

### Classes

[](#classes-explicit) Use a constructor to do an implicit conversion when the argument is reasonably thought of as a type conversion and the type conversion is fast. Otherwise, use the explicit keyword or a function returning the type. This only applies to single argument constructors.

###### Right:

```cpp
class LargeInt {
public:
    LargeInt(int);
...

class Vector {
public:
    explicit Vector(int size); // Not a type conversion.
    Vector create(Array); // Costly conversion.
...

```

###### Wrong:

```cpp
class Task {
public:
    Task(ExecutionContext&); // Not a type conversion.
    explicit Task(); // No arguments.
    explicit Task(ExecutionContext&, Other); // More than one argument.
...
```

### Singleton pattern

[](#singleton-static-member) Use a static member function named "the()" to access the instance of the singleton.

###### Right:

```cpp
class UniqueObject {
public:
    static UniqueObject& the();
...
```

###### Wrong:

```cpp
class UniqueObject {
public:
    static UniqueObject& shared();
...
```

###### Wrong:

```cpp
class UniqueObject {
...
};

UniqueObject& my_unique_object(); // Free function.
```

### Comments

[](#comments-eol) Use only _one_ space before end of line comments and in between sentences in comments.

###### Right:

```cpp
f(a, b); // This explains why the function call was done. This is another sentence.
```

###### Wrong:

```cpp
int i;    // This is a comment with several spaces before it, which is a non-conforming style.
double f; // This is another comment.  There are two spaces before this sentence which is a non-conforming style.
```

[](#comments-sentences) Make comments look like sentences by starting with a capital letter and ending with a period (punctation). One exception may be end of line comments like this `if (x == y) // false for NaN`.

[](#comments-fixme) Use FIXME: (without attribution) to denote items that need to be addressed in the future.

###### Right:

```cpp
draw_jpg(); // FIXME: Make this code handle jpg in addition to the png support.
```

###### Wrong:

```cpp
draw_jpg(); // FIXME(joe): Make this code handle jpg in addition to the png support.
```

```cpp
draw_jpg(); // TODO: Make this code handle jpg in addition to the png support.
```

### Overriding Virtual Methods

[](#override-methods) The declaration of a virtual method inside a class must be declared with the `virtual` keyword. All subclasses of that class must either specify the `override` keyword when overriding the virtual method or the `final` keyword when overriding the virtual method and requiring that no further subclasses can override it.

###### Right:

```cpp
class Person {
public:
    virtual String description() { ... };
}

class Student : public Person {
public:
    virtual String description() override { ... }; // This is correct because it only contains the "override" keyword to indicate that the method is overridden.
}

```

```cpp
class Person {
public:
    virtual String description() { ... };
}

class Student : public Person {
public:
    virtual String description() final { ... }; // This is correct because it only contains the "final" keyword to indicate that the method is overridden and that no subclasses of "Student" can override "description".
}

```

###### Wrong:

```cpp
class Person {
public:
    virtual String description() { ... };
}

class Student : public Person {
public:
    String description() override { ... }; // This is incorrect because it uses only the "override" keywords to indicate that the method is virtual. Instead, it should use both the "virtual" and "override" keywords.
}
```

```cpp
class Person {
public:
    virtual String description() { ... };
}

class Student : public Person {
public:
    String description() final { ... }; // This is incorrect because it uses only the "final" keywords to indicate that the method is virtual and final. Instead, it should use both the "virtual" and "final" keywords.
}
```

```cpp
class Person {
public:
    virtual String description() { ... };
}

class Student : public Person {
public:
    virtual String description() { ... }; // This is incorrect because it uses the "virtual" keyword to indicate that the method is overridden.
}
```

