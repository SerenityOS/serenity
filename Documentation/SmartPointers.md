# Serenity smart pointers

----
## Introduction

There are three main C++ smart pointer types used in the Serenity operating system. Each type describes the ownership (or lack thereof) of the pointee.

The reason for using these pointers is to make it explicit through code who owns which resources, and how ownership is transferred. They also serve as a guard against memory leaks and use-after-free bugs.


----
## OwnPtr<T> and NonnullOwnPtr<T>

OwnPtr is used for single-owner objects. An object held by an OwnPtr is owned by that OwnPtr, and not by anybody else.

This means that the OwnPtr is responsible for deleting the pointee when the OwnPtr goes out of scope.

NonnullOwnPtr<T> is a special variant of OwnPtr with one additional property: it cannot be null. NonnullOwnPtr is suitable as a return type from functions that are guaranteed to never return null, and as an argument type where ownership is transferred, and the argument may not be null. In other words, if OwnPtr is "\*", then NonnullOwnPtr is "&".

There is a make<T>() helper that creates a new object and returns it wrapped in an NonnullOwnPtr.

    {
        NonnullOwnPtr<Foo> my_object = make<Foo>();
        my_object->do_stuff();
        // my_object goes out of scope here, and the Foo will be deleted.
    }


----
## RefPtr<T> and NonnullRefPtr<T>

RefPtr is used for multiple-owner objects. An object held by a RefPtr is owned together by every pointer pointing to that object.

Shared ownership is implemented via reference counting.

NonnullRefPtr<T> is a special variant of RefPtr with one additional property: it cannot be null. NonnullRefPtr is suitable as a return type from functions that are guaranteed to never return null, and as an argument type where the argument may not be null. In other words, if RefPtr is "\*", then NonnullRefPtr is "&".

Objects can only be held by RefPtr if they meet certain criteria. Specifically, they need to implement the functions `ref()` and `unref()`.

To make a class T reference counted, you can simply make it inherit from RefCounted<T>. This will add all the necessary pieces to T.

**Note:** When constructing a RefCounted-derived class, the reference count starts out at 1 (since 0 would mean that the object has no owners and should be deleted.) The object must therefore be "adopted" by someone who takes responsibility of that 1. This is done through the global `adopt()` function:

    class Bar : public RefCounted<Bar> {
        ...
    };

    RefPtr<Bar> our_object = adopt(*new Bar);
    RefPtr<Bar> another_owner = our_object;

In the above example, the Bar object will only be deleted once both "our\_object" and "another\_owner" are gone.

----
## WeakPtr<T>

WeakPtr is used for object that somebody else owns. When the pointee of a WeakPtr is deleted, the WeakPtr will magically become null.

Behind the scenes, this is implemented using the Weakable<T> template. If you want to make it possible for a class to be weakly-pointed-to, have it inherit from Weakable<T>.

To create a WeakPtr<T>, use `make_weak_ptr()`:

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
