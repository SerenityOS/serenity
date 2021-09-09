# SerenityOS patterns

## Introduction

Over time numerous reoccurring patterns have emerged from or were adopted by
the serenity code base. This document aims to track and describe them so they
can be propagated further and keep the code base consistent. 

## Intrusive Lists

[Intrusive lists](https://www.data-structures-in-practice.com/intrusive-linked-lists/) are common in the Kernel and in some specific cases
are used in the SerenityOS userland. A data structure is said to be
"intrusive" when each element holds the metadata that tracks the
element's membership in the data structure. In the case of a list, this
means that every element in an intrusive linked list has a node embedded
inside of it. The main advantage of intrusive
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
