# SerenityOS Patterns

## Introduction

Over time numerous reoccurring patterns have emerged from or were adopted by
the serenity code base. This document aims to track and describe them, so they
can be propagated further and the code base can be kept consistent. 

## `TRY(...)` Error Handling

The `TRY(..)` macro is used for error propagation in the serenity code base.
The goal being to reduce the amount of boiler plate error code required to
properly handle and propagate errors throughout the code base. 

Any code surrounded by `TRY(..)` will attempt to be executed, and any error
will immediately be returned from the function. If no error occurs then the
result of the contents of the TRY will be the result of the macro's execution. 

### Examples:

Example from LibGUI: 

```cpp
#include <AK/Try.h>

... snip ...

ErrorOr<NonnullRefPtr<Menu>> Window::try_add_menu(String name)
{
    auto menu = TRY(m_menubar->try_add_menu({}, move(name)));
    if (m_window_id) {
        menu->realize_menu_if_needed();
        WindowServerConnection::the().async_add_menu(m_window_id, menu->menu_id());
    }
    return menu;
}
```

Example from the Kernel:

```cpp
#include <AK/Try.h>

... snip ...

ErrorOr<Region*> AddressSpace::allocate_region(VirtualRange const& range, StringView name, int prot, AllocationStrategy strategy)
{
    VERIFY(range.is_valid());
    OwnPtr<KString> region_name;
    if (!name.is_null())
        region_name = TRY(KString::try_create(name));
    auto vmobject = TRY(AnonymousVMObject::try_create_with_size(range.size(), strategy));
    auto region = TRY(Region::try_create_user_accessible(range, move(vmobject), 0, move(region_name), prot_to_region_access_flags(prot), Region::Cacheable::Yes, false));
    TRY(region->map(page_directory()));
    return add_region(move(region));
}
```

Note: Our `TRY(...)` macro functions similarly to the `?` [operator in rust](https://doc.rust-lang.org/book/ch09-02-recoverable-errors-with-result.html#a-shortcut-for-propagating-errors-the--operator).

## `MUST(...)` Error Handling

The `MUST(...)` macro is similar to `TRY(...)` except the macro enforces that
the code run inside the macro must succeed, otherwise we assert.

### Example:

```cpp
#include <AK/Try.h>
#include <AK/String.h>

... snip ...

void log_that_can_not_fail(StringView fmtstr, TypeErasedFormatParams& params)
{
    StringBuilder builder;
    MUST(vformat(builder, fmtstr, params));
    return builder.to_string();
}
```

## The `serenity_main(..)` program entry point

Serenity has moved to a pattern where executables do not expose a normal C
main function. A `serenity_main(..)` is exposed instead. The main reasoning
is that the `Main::Arguments` struct can provide arguments in a more idiomatic
way that fits with the serenity API surface area. The ErrorOr<int> likewise
allows the program to propagate errors seamlessly with the `TRY(...)` macro,
avoiding a significant amount of clunky C style error handling.

These executables are then linked with the `LibMain` library, which will link in
the normal C `int main(int, char**)` function which will call into the programs
`serenity_main(..)` on program startup.

The creation of the pattern was documented in the following video:
[OS hacking: A better main() for SerenityOS C++ programs](https://www.youtube.com/watch?v=5PciKJW1rUc)

### Examples:

A function `main(..)` would normally look something like:

```cpp
int main(int argc, char** argv)
{
   return 0;
}
```

Instead, `serenity_main(..)` is defined like this:

```cpp
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    return 0; 
}
```

## Intrusive Lists

[Intrusive lists](https://www.data-structures-in-practice.com/intrusive-linked-lists/) are common in the Kernel and in some specific cases
are used in the SerenityOS userland. A data structure is said to be
"intrusive" when each element holds the metadata that tracks the
element's membership in the data structure. In the case of a list, this
means that every element in an intrusive linked list has a node embedded
inside it. The main advantage of intrusive
data structures is you don't need to worry about handling out of memory (OOM)
on insertion into the data structure. This means error handling code is
much simpler than say, using a `Vector` in environments that need to be durable
to OOM.

The common pattern for declaring an intrusive list is to add the storage
for the intrusive list node as a private member. A public type alias is
then used to expose the list type to anyone who might need to create it.
Here is an example from the `Region` class in the Kernel:

```cpp
class Region final
    : public Weakable<Region> {

public:

... snip ...

private:
    bool m_syscall_region : 1 { false };

    IntrusiveListNode<Region> m_memory_manager_list_node;
    IntrusiveListNode<Region> m_vmobject_list_node;

public:
    using ListInMemoryManager = IntrusiveList<&Region::m_memory_manager_list_node>;
    using ListInVMObject = IntrusiveList<&Region::m_vmobject_list_node>;
};
```

You can then use the list by referencing the public type alias like so:

```cpp
class MemoryManager {

... snip ...

    Region::ListInMemoryManager m_kernel_regions;
    Vector<UsedMemoryRange> m_used_memory_ranges;
    Vector<PhysicalMemoryRange> m_physical_memory_ranges;
    Vector<ContiguousReservedMemoryRange> m_reserved_memory_ranges;
};
```

## Static Assertions of the size of a type

It's a universal pattern to use `static_assert` to validate the size of a
type matches the author's expectations. Unfortunately when these assertions
fail they don't give you the values that actually caused the failure. This
forces one to go investigate by printing out the size, or checking it in a
debugger, etc. 

For this reason `AK::AssertSize` was added. It exploits the fact that the
compiler will emit template argument values for compiler errors to provide
debugging information. Instead of getting no information you'll get the actual
type sizes in your compiler error output.

Example Usage:

```cpp
#include <AK/StdLibExtras.h>

struct Empty { };

static_assert(AssertSize<Empty, 1>());
```

## String View Literals

`AK::StringView` support for `operator"" sv` which is a special string literal operator that was added as of
[C++17 to enable `std::string_view` literals](https://en.cppreference.com/w/cpp/string/basic_string_view/operator%22%22sv).

```cpp
[[nodiscard]] ALWAYS_INLINE constexpr AK::StringView operator"" sv(const char* cstring, size_t length)
{
    return AK::StringView(cstring, length);
}
```

This allows `AK::StringView` to be constructed from string literals with no runtime
cost to find the string length, and the data the `AK::StringView` points to will 
reside in the data section of the binary.

Example Usage:
```cpp
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibTest/TestCase.h>

TEST_CASE(string_view_literal_operator)
{
    StringView literal_view = "foo"sv;
    String test_string = "foo";

    EXPECT_EQ(literal_view.length(), test_string.length());
    EXPECT_EQ(literal_view, test_string);
}
```

## Source Location

C++20 added std::source_location, which lets you capture the
callers __FILE__ / __LINE__ / __FUNCTION__ etc as a default
argument to functions.
See: https://en.cppreference.com/w/cpp/utility/source_location

`AK::SourceLocation` is the implementation of this feature in
SerenityOS. It's become the idiomatic way to capture the location
when adding extra debugging instrumentation, without resorting to
litering the code with preprocessor macros.

To use it, you can add the `AK::SourceLocation` as a default argument
to any function, using `AK::SourceLocation::current()` to initialize the
default argument.

Example Usage:
```cpp
#include <AK/SourceLocation.h>
#include <AK/StringView.h>

static StringView example_fn(const SourceLocation& loc = SourceLocation::current())
{
    return loc.function_name();
}

int main(int, char**)
{
    return example_fn().length();
}
```

If you only want to only capture `AK::SourceLocation` data with a certain debug macro enabled, avoid
adding `#ifdef`'s to all functions which have the  `AK::SourceLocation` argument. Since SourceLocation
is just a simple struct, you can just declare an empty class which can be optimized away by the
compiler, and alias both to the same name.

Example Usage:

```cpp

#if LOCK_DEBUG
#    include <AK/SourceLocation.h>
#endif

#if LOCK_DEBUG
using LockLocation = SourceLocation;
#else
struct LockLocation {
    static constexpr LockLocation current() { return {}; }

private:
    constexpr LockLocation() = default;
};
#endif
```
