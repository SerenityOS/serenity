# Serenity C++ coding style

For low-level styling (spaces, parentheses, brace placement, etc), all code should follow the format specified in `.clang-format` in the project root.

**Important: Make sure you use `clang-format` version 14 or later!**

This document describes the coding style used for C++ code in the Serenity Operating System project. All new code should conform to this style.

We'll definitely be tweaking and amending this over time, so let's consider it a living document. :)


### Names

A combination of CamelCase and snake\_case. Use CamelCase (Capitalize the first letter, including all letters in an acronym) in a class, struct, or namespace name. Use snake\_case (all lowercase, with underscores separating words) for variable and function names.

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

Use full words, except in the rare case where an abbreviation would be more canonical and easier to understand.

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
short tabulation_index; // Goofy.
```

Data members in C++ classes should be private. Static data members should be prefixed by "s\_". Other data members should be prefixed by "m\_". Global variables should be prefixed by "g\_".

###### Right:

```cpp
class String {
public:
    ...

private:
    int m_length { 0 };
};
```

###### Wrong:

```cpp
class String {
public:
    ...

    int length { 0 };
};
```

Precede setters with the word "set". Use bare words for getters. Setter and getter names should match the names of the variables being set/gotten.

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

Precede getters that return values through out arguments with the word "get".

###### Right:

```cpp
void get_filename_and_inode_id(String&, InodeIdentifier&) const;
```

###### Wrong:

```cpp
void filename_and_inode_id(String&, InodeIdentifier&) const;
```

Use descriptive verbs in function names.

###### Right:

```cpp
bool convert_to_ascii(short*, size_t);
```

###### Wrong:

```cpp
bool to_ascii(short*, size_t);
```

When there are two getters for a variable, and one of them automatically makes sure the requested object is instantiated, prefix that getter function with `ensure_`. As it ensures that an object is created, it should consequently also return a reference, not a pointer.

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

Leave meaningless variable names out of function declarations. A good rule of thumb is if the parameter type name contains the parameter name (without trailing numbers or pluralization), then the parameter name isn't needed. Usually, there should be a parameter name for bools, strings, and numerical types.

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

Prefer enums to bools on function parameters if callers are likely to be passing constants, since named constants are easier to read at the call site. An exception to this rule is a setter function, where the name of the function already makes clear what the boolean is.

###### Right:

```cpp
do_something(something, AllowFooBar::Yes);
paint_text_with_shadows(context, ..., text_stroke_width > 0, is_horizontal());
set_resizable(false);
```

###### Wrong:

```cpp
do_something(something, false);
set_resizable(NotResizable);
```

Enum members should use InterCaps with an initial capital letter.

Prefer `const` to `#define`. Prefer inline functions to macros.

`#defined` constants should use all uppercase names with words separated by underscores.

Use `#pragma once` instead of `#define` and `#ifdef` for header guards.

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

Constructors for C++ classes should initialize their members using C++ initializer syntax. Each member (and superclass) should be indented on a separate line, with the colon or comma preceding the member on that line. Prefer initialization at member definition whenever possible.

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

Prefer index or range-for over iterators in Vector iterations for terse, easier-to-read code.

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

Both pointer types and reference types should be written with no space between the type name and the `*` or `&`.

An out argument of a function should be passed by reference except rare cases where it is optional in which case it should be passed by pointer.

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

### "using" Statements

In header files in the AK sub-library, however, it is acceptable to use "using" declarations at the end of the file to import one or more names in the AK namespace into the global scope.

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

In C++ implementation files, do not use "using" declarations of any kind to import names in the standard template library. Directly qualify the names at the point they're used instead.

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

Omit "int" when using "unsigned" modifier. Do not use "signed" modifier. Use "int" by itself instead.

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

For types with methods, prefer `class` over `struct`.

* For classes, make public getters and setters, keep members private with `m_` prefix.
* For structs, let everything be public and skip the `m_` prefix.

###### Right:

```cpp
struct Thingy {
    String name;
    int frob_count { 0 };
};

class Doohickey {
public:
    String const& name() const { return m_name; }
    int frob_count() const { return m_frob_count; }

    void jam();

private:
    String m_name;
    int m_frob_count { 0 };
}
```

###### Wrong:

```cpp
struct Thingy {
public:
    String m_name;
    int frob_count() const { return m_frob_count; }

private:
    int m_frob_count { 0 };
}

class Doohickey {
public:
    String const& name() const { return this->name; }

    void jam();

    String name;
    int frob_count { 0 };
};
```

Use a constructor to do an implicit conversion when the argument is reasonably thought of as a type conversion and the type conversion is fast. Otherwise, use the explicit keyword or a function returning the type. This only applies to single argument constructors.

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

Use a static member function named "the()" to access the instance of the singleton.

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

Make comments look like sentences by starting with a capital letter and ending with a period (punctuation). One exception may be end of line comments like this `if (x == y) // false for NaN`.

Use FIXME: (without attribution) to denote items that need to be addressed in the future.

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

Explain *why* the code does something. The code itself should already say what is happening.

###### Right:

```cpp
i++; // Go to the next page.
```

```cpp
// Let users toggle the advice functionality by clicking on catdog.
catdog_widget.on_click = [&] {
    if (advice_timer->is_active())
        advice_timer->stop();
    else
        advice_timer->start();
};
```

###### Even better:

```cpp
page_index++;
```

###### Wrong:

```cpp
i++; // Increment i.
```

```cpp
// If the user clicks, toggle the timer state.
catdog_widget.on_click = [&] {
    if (advice_timer->is_active())
        advice_timer->stop();
    else
        advice_timer->start();
};
```

### Overriding Virtual Methods

The declaration of a virtual method inside a class must be declared with the `virtual` keyword. All subclasses of that class must also specify either the `override` keyword when overriding the virtual method, or the `final` keyword when overriding the virtual method and requiring that no further subclasses can override it.

###### Right:

```cpp
class Person {
public:
    virtual String description() { ... };
}

class Student : public Person {
public:
    virtual String description() override { ... }; // This is correct because it contains both the "virtual" and "override" keywords to indicate that the method is overridden.
}

```

```cpp
class Person {
public:
    virtual String description() { ... };
}

class Student : public Person {
public:
    virtual String description() final { ... }; // This is correct because it contains both the "override" and "final" keywords to indicate that the method is overridden and that no subclasses of "Student" can override "description".
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
    String description() override { ... }; // This is incorrect because it uses only the "override" keyword to indicate that the method is virtual. Instead, it should use both the "virtual" and "override" keywords.
}
```

```cpp
class Person {
public:
    virtual String description() { ... };
}

class Student : public Person {
public:
    String description() final { ... }; // This is incorrect because it uses only the "final" keyword to indicate that the method is virtual and final. Instead, it should use both the "virtual" and "final" keywords.
}
```

```cpp
class Person {
public:
    virtual String description() { ... };
}

class Student : public Person {
public:
    virtual String description() { ... }; // This is incorrect because it uses only the "virtual" keyword to indicate that the method is overridden.
}
```

### Const placement

Use "east const" style where `const` is written on the right side of the type being qualified. See [this article](https://mariusbancila.ro/blog/2018/11/23/join-the-east-const-revolution/) for more information about east const.

###### Right:

```cpp
Salt const& m_salt;
```

###### Wrong:

```cpp
const Salt& m_salt;
```
