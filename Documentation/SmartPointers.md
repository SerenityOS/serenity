# SerenityOS smart pointers

----
## Introduction

There are three main C++ smart pointer types used in SerenityOS. Each type describes the ownership (or lack thereof) of the pointee.

The reason for using these pointers is to make it explicit through code who owns which resources, and how ownership is transferred. They also serve as a guard against memory leaks and use-after-free bugs.


----
## OwnPtr<T> and NonnullOwnPtr<T>

`OwnPtr` is used for single-owner objects. An object held in an `OwnPtr` is owned by that `OwnPtr`, and not by anybody else.

This means that the `OwnPtr` is responsible for deleting the pointee when the `OwnPtr` goes out of scope.

`NonnullOwnPtr` is a special variant of `OwnPtr` with one additional property: it cannot be null. `NonnullOwnPtr` is suitable as a return type from functions that are guaranteed to never return null, and as an argument type where ownership is transferred, and the argument may not be null. In other words, if `OwnPtr` is "\*", then `NonnullOwnPtr` is "&".

There is a `make<T>()` helper that creates a new object and returns it wrapped in an `NonnullOwnPtr`.

```cpp
{
    NonnullOwnPtr<Foo> my_object = make<Foo>();
    my_object->do_stuff();
    // my_object goes out of scope here, and the Foo will be deleted.
}
```

Note: A `NonnullOwnPtr` can be assigned to an `OwnPtr` but not vice versa. To transform an known-non-null `OwnPtr` into a `NonnullOwnPtr`, use `OwnPtr::release_nonnull()`.

----
## RefPtr<T> and NonnullRefPtr<T>

`RefPtr` is used for multiple-owner objects. An object held by a `RefPtr` is owned together by every pointer pointing to that object.

Shared ownership is implemented via reference counting.

`NonnullRefPtr` is a special variant of `RefPtr` with one additional property: it cannot be null. `NonnullRefPtr` is suitable as a return type from functions that are guaranteed to never return null, and as an argument type where the argument may not be null. In other words, if `RefPtr` is "\*", then `NonnullRefPtr` is "&".

Objects can only be held by `RefPtr` if they meet certain criteria. Specifically, they need to implement the functions `ref()` and `unref()`.

To make a class `T` reference-counted, you can simply make it inherit from `RefCounted<T>`. This will add all the necessary pieces to `T`.

**Note:** When constructing an object that derives from `RefCounted`, the reference count starts out at 1 (since 0 would mean that the object has no owners and should be deleted.) The object must therefore be "adopted" by someone who takes responsibility of that 1. This is done through the global `adopt()` function:

```cpp
class Bar : public RefCounted<Bar> {
    ...
};

RefPtr<Bar> our_object = adopt(*new Bar);
RefPtr<Bar> another_owner = our_object;
```

In the above example, the Bar object will only be deleted once both `our_object` and `another_owner` are gone.

Note: A `NonnullRefPtr` can be assigned to a `RefPtr` but not vice versa. To transform an known-non-null `RefPtr` into a `NonnullRefPtr`, either use `RefPtr::release_nonnull()` or simply dereference the `RefPtr` using its `operator*`.

----
## WeakPtr<T>

`WeakPtr` is used for objects that somebody else owns. When the pointee of a `WeakPtr` is deleted, the `WeakPtr` will magically become null.

Behind the scenes, this is implemented using the `Weakable` template. If you want to make it possible for a class `T` to be weakly-pointed-to, have it inherit from `Weakable<T>`.

To create a `WeakPtr` to a weakable object, use `make_weak_ptr()`:

```cpp
class Baz : public Weakable<Baz> {
    ....
};

WeakPtr<Baz> a_baz;
{
    OwnPtr<Baz> my_baz = make<Baz>();
    a_baz = my_baz->make_weak_ptr();
    // a_baz now points to my_baz
}
// a_baz is now null, since my_baz went out of scope.
```
