/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_MEMORY_METASPACECLOSURE_HPP
#define SHARE_MEMORY_METASPACECLOSURE_HPP

#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "metaprogramming/enableIf.hpp"
#include "oops/array.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"
#include "utilities/resizeableResourceHash.hpp"
#include <type_traits>

// The metadata hierarchy is separate from the oop hierarchy
  class MetaspaceObj;        // no C++ vtable
//class   Array;             // no C++ vtable
  class   Annotations;       // no C++ vtable
  class   ConstantPoolCache; // no C++ vtable
  class   ConstMethod;       // no C++ vtable
  class   MethodCounters;    // no C++ vtable
  class   Symbol;            // no C++ vtable
  class   Metadata;          // has C++ vtable (so do all subclasses)
  class     ConstantPool;
  class     MethodData;
  class     Method;
  class     Klass;
  class       InstanceKlass;
  class         InstanceMirrorKlass;
  class         InstanceClassLoaderKlass;
  class         InstanceRefKlass;
  class       ArrayKlass;
  class         ObjArrayKlass;
  class         TypeArrayKlass;

// class MetaspaceClosure --
//
// This class is used for iterating the objects in the HotSpot Metaspaces. It
// provides an API to walk all the reachable objects starting from a set of
// root references (such as all Klass'es in the SystemDictionary).
//
// Currently it is used for compacting the CDS archive by eliminate temporary
// objects allocated during archive creation time. See ArchiveBuilder for an example.
//
// To support MetaspaceClosure, each subclass of MetaspaceObj must provide
// a method of the type void metaspace_pointers_do(MetaspaceClosure*). This method
// should call MetaspaceClosure::push() on every pointer fields of this
// class that points to a MetaspaceObj. See Annotations::metaspace_pointers_do()
// for an example.
class MetaspaceClosure {
public:
  enum Writability {
    _writable,
    _not_writable,
    _default
  };

  enum SpecialRef {
    _method_entry_ref
  };

  // class MetaspaceClosure::Ref --
  //
  // MetaspaceClosure can be viewed as a very simple type of copying garbage
  // collector. For it to function properly, it requires each subclass of
  // MetaspaceObj to provide two methods:
  //
  //  size_t size();                                 -- to determine how much data to copy
  //  void metaspace_pointers_do(MetaspaceClosure*); -- to locate all the embedded pointers
  //
  // Calling these methods would be trivial if these two were virtual methods.
  // However, to save space, MetaspaceObj has NO vtable. The vtable is introduced
  // only in the Metadata class.
  //
  // To work around the lack of a vtable, we use the Ref class with templates
  // (see MSORef, OtherArrayRef, MSOArrayRef, and MSOPointerArrayRef)
  // so that we can statically discover the type of a object. The use of Ref
  // depends on the fact that:
  //
  // [1] We don't use polymorphic pointers for MetaspaceObj's that are not subclasses
  //     of Metadata. I.e., we don't do this:
  //     class Klass {
  //         MetaspaceObj *_obj;
  //         Array<int>* foo() { return (Array<int>*)_obj; }
  //         Symbol*     bar() { return (Symbol*)    _obj; }
  //
  // [2] All Array<T> dimensions are statically declared.
  class Ref : public CHeapObj<mtMetaspace> {
    Writability _writability;
    bool _keep_after_pushing;
    Ref* _next;
    void* _user_data;
    NONCOPYABLE(Ref);

  protected:
    virtual void** mpp() const = 0;
    Ref(Writability w) : _writability(w), _keep_after_pushing(false), _next(NULL), _user_data(NULL) {}
  public:
    virtual bool not_null() const = 0;
    virtual int size() const = 0;
    virtual void metaspace_pointers_do(MetaspaceClosure *it) const = 0;
    virtual void metaspace_pointers_do_at(MetaspaceClosure *it, address new_loc) const = 0;
    virtual MetaspaceObj::Type msotype() const = 0;
    virtual bool is_read_only_by_default() const = 0;
    virtual ~Ref() {}

    address obj() const {
      // In some rare cases (see CPSlot in constantPool.hpp) we store some flags in the lowest
      // 2 bits of a MetaspaceObj pointer. Unmask these when manipulating the pointer.
      uintx p = (uintx)*mpp();
      return (address)(p & (~FLAG_MASK));
    }

    address* addr() const {
      return (address*)mpp();
    }

    void update(address new_loc) const;

    Writability writability() const { return _writability; };
    void set_keep_after_pushing()   { _keep_after_pushing = true; }
    bool keep_after_pushing()       { return _keep_after_pushing; }
    void set_user_data(void* data)  { _user_data = data; }
    void* user_data()               { return _user_data; }
    void set_next(Ref* n)           { _next = n; }
    Ref* next() const               { return _next; }

  private:
    static const uintx FLAG_MASK = 0x03;

    int flag_bits() const {
      uintx p = (uintx)*mpp();
      return (int)(p & FLAG_MASK);
    }
  };

private:
  // MSORef -- iterate an instance of MetaspaceObj
  template <class T> class MSORef : public Ref {
    T** _mpp;
    T* dereference() const {
      return *_mpp;
    }
  protected:
    virtual void** mpp() const {
      return (void**)_mpp;
    }

  public:
    MSORef(T** mpp, Writability w) : Ref(w), _mpp(mpp) {}

    virtual bool is_read_only_by_default() const { return T::is_read_only_by_default(); }
    virtual bool not_null()                const { return dereference() != NULL; }
    virtual int size()                     const { return dereference()->size(); }
    virtual MetaspaceObj::Type msotype()   const { return dereference()->type(); }

    virtual void metaspace_pointers_do(MetaspaceClosure *it) const {
      dereference()->metaspace_pointers_do(it);
    }
    virtual void metaspace_pointers_do_at(MetaspaceClosure *it, address new_loc) const {
      ((T*)new_loc)->metaspace_pointers_do(it);
    }
  };

  // abstract base class for MSOArrayRef, MSOPointerArrayRef and OtherArrayRef
  template <class T> class ArrayRef : public Ref {
    Array<T>** _mpp;
  protected:
    Array<T>* dereference() const {
      return *_mpp;
    }
    virtual void** mpp() const {
      return (void**)_mpp;
    }

    ArrayRef(Array<T>** mpp, Writability w) : Ref(w), _mpp(mpp) {}

    // all Arrays are read-only by default
    virtual bool is_read_only_by_default() const { return true; }
    virtual bool not_null()                const { return dereference() != NULL;  }
    virtual int size()                     const { return dereference()->size(); }
    virtual MetaspaceObj::Type msotype()   const { return MetaspaceObj::array_type(sizeof(T)); }
  };

  // OtherArrayRef -- iterate an instance of Array<T>, where T is NOT a subtype of MetaspaceObj.
  // T can be a primitive type, such as int, or a structure. However, we do not scan
  // the fields inside T, so you should not embed any pointers inside T.
  template <class T> class OtherArrayRef : public ArrayRef<T> {
  public:
    OtherArrayRef(Array<T>** mpp, Writability w) : ArrayRef<T>(mpp, w) {}

    virtual void metaspace_pointers_do(MetaspaceClosure *it) const {
      Array<T>* array = ArrayRef<T>::dereference();
      log_trace(cds)("Iter(OtherArray): %p [%d]", array, array->length());
    }
    virtual void metaspace_pointers_do_at(MetaspaceClosure *it, address new_loc) const {
      Array<T>* array = (Array<T>*)new_loc;
      log_trace(cds)("Iter(OtherArray): %p [%d]", array, array->length());
    }
  };

  // MSOArrayRef -- iterate an instance of Array<T>, where T is a subtype of MetaspaceObj.
  // We recursively call T::metaspace_pointers_do() for each element in this array.
  template <class T> class MSOArrayRef : public ArrayRef<T> {
  public:
    MSOArrayRef(Array<T>** mpp, Writability w) : ArrayRef<T>(mpp, w) {}

    virtual void metaspace_pointers_do(MetaspaceClosure *it) const {
      metaspace_pointers_do_at_impl(it, ArrayRef<T>::dereference());
    }
    virtual void metaspace_pointers_do_at(MetaspaceClosure *it, address new_loc) const {
      metaspace_pointers_do_at_impl(it, (Array<T>*)new_loc);
    }
  private:
    void metaspace_pointers_do_at_impl(MetaspaceClosure *it, Array<T>* array) const {
      log_trace(cds)("Iter(MSOArray): %p [%d]", array, array->length());
      for (int i = 0; i < array->length(); i++) {
        T* elm = array->adr_at(i);
        elm->metaspace_pointers_do(it);
      }
    }
  };

  // MSOPointerArrayRef -- iterate an instance of Array<T*>, where T is a subtype of MetaspaceObj.
  // We recursively call MetaspaceClosure::push() for each pointer in this array.
  template <class T> class MSOPointerArrayRef : public ArrayRef<T*> {
  public:
    MSOPointerArrayRef(Array<T*>** mpp, Writability w) : ArrayRef<T*>(mpp, w) {}

    virtual void metaspace_pointers_do(MetaspaceClosure *it) const {
      metaspace_pointers_do_at_impl(it, ArrayRef<T*>::dereference());
    }
    virtual void metaspace_pointers_do_at(MetaspaceClosure *it, address new_loc) const {
      metaspace_pointers_do_at_impl(it, (Array<T*>*)new_loc);
    }
  private:
    void metaspace_pointers_do_at_impl(MetaspaceClosure *it, Array<T*>* array) const {
      log_trace(cds)("Iter(MSOPointerArray): %p [%d]", array, array->length());
      for (int i = 0; i < array->length(); i++) {
        T** mpp = array->adr_at(i);
        it->push(mpp);
      }
    }
  };

  // Normally, chains of references like a->b->c->d are iterated recursively. However,
  // if recursion is too deep, we save the Refs in _pending_refs, and push them later in
  // MetaspaceClosure::finish(). This avoids overflowing the C stack.
  static const int MAX_NEST_LEVEL = 5;
  Ref* _pending_refs;
  int _nest_level;
  Ref* _enclosing_ref;

  void push_impl(Ref* ref);
  void do_push(Ref* ref);

public:
  MetaspaceClosure(): _pending_refs(NULL), _nest_level(0), _enclosing_ref(NULL) {}
  ~MetaspaceClosure();

  void finish();

  // enclosing_ref() is used to compute the offset of a field in a C++ class. For example
  // class Foo { intx scala; Bar* ptr; }
  //    Foo *f = 0x100;
  // when the f->ptr field is iterated with do_ref() on 64-bit platforms, we will have
  //    do_ref(Ref* r) {
  //       r->addr() == 0x108;                // == &f->ptr;
  //       enclosing_ref()->obj() == 0x100;   // == foo
  // So we know that we are iterating upon a field at offset 8 of the object at 0x100.
  //
  // Note that if we have stack overflow, do_pending_ref(r) will be called first and
  // do_ref(r) will be called later, for the same r. In this case, enclosing_ref() is valid only
  // when do_pending_ref(r) is called, and will return NULL when do_ref(r) is called.
  Ref* enclosing_ref() const {
    return _enclosing_ref;
  }

  // This is called when a reference is placed in _pending_refs. Override this
  // function if you're using enclosing_ref(). See notes above.
  virtual void do_pending_ref(Ref* ref) {}

  // returns true if we want to keep iterating the pointers embedded inside <ref>
  virtual bool do_ref(Ref* ref, bool read_only) = 0;

private:
  template <class REF_TYPE, typename T>
  void push_with_ref(T** mpp, Writability w) {
    push_impl(new REF_TYPE(mpp, w));
  }

public:
  // When MetaspaceClosure::push(...) is called, pick the correct Ref subtype to handle it:
  //
  // MetaspaceClosure*      it = ...;
  // Klass*                 o  = ...;  it->push(&o);     => MSORef
  // Array<int>*            a1 = ...;  it->push(&a1);    => OtherArrayRef
  // Array<Annotation>*     a2 = ...;  it->push(&a2);    => MSOArrayRef
  // Array<Klass*>*         a3 = ...;  it->push(&a3);    => MSOPointerArrayRef
  // Array<Array<Klass*>*>* a4 = ...;  it->push(&a4);    => MSOPointerArrayRef
  // Array<Annotation*>*    a5 = ...;  it->push(&a5);    => MSOPointerArrayRef
  //
  // Note that the following will fail to compile (to prevent you from adding new fields
  // into the MetaspaceObj subtypes that cannot be properly copied by CDS):
  //
  // Hashtable*             h  = ...;  it->push(&h);     => Hashtable is not a subclass of MetaspaceObj
  // Array<Hashtable*>*     a6 = ...;  it->push(&a6);    => Hashtable is not a subclass of MetaspaceObj
  // Array<int*>*           a7 = ...;  it->push(&a7);    => int       is not a subclass of MetaspaceObj

  template <typename T>
  void push(T** mpp, Writability w = _default) {
    static_assert(std::is_base_of<MetaspaceObj, T>::value, "Do not push pointers of arbitrary types");
    push_with_ref<MSORef<T>>(mpp, w);
  }

  template <typename T, ENABLE_IF(!std::is_base_of<MetaspaceObj, T>::value)>
  void push(Array<T>** mpp, Writability w = _default) {
    push_with_ref<OtherArrayRef<T>>(mpp, w);
  }

  template <typename T, ENABLE_IF(std::is_base_of<MetaspaceObj, T>::value)>
  void push(Array<T>** mpp, Writability w = _default) {
    push_with_ref<MSOArrayRef<T>>(mpp, w);
  }

  template <typename T>
  void push(Array<T*>** mpp, Writability w = _default) {
    static_assert(std::is_base_of<MetaspaceObj, T>::value, "Do not push Arrays of arbitrary pointer types");
    push_with_ref<MSOPointerArrayRef<T>>(mpp, w);
  }

#if 0
  // Enable this block if you're changing the push(...) methods, to test for types that should be
  // disallowed. Each of the following "push" calls should result in a compile-time error.
  void test_disallowed_types(MetaspaceClosure* it) {
    Hashtable<bool, mtInternal>* h  = NULL;
    it->push(&h);

    Array<Hashtable<bool, mtInternal>*>* a6 = NULL;
    it->push(&a6);

    Array<int*>* a7 = NULL;
    it->push(&a7);
  }
#endif

  template <class T> void push_method_entry(T** mpp, intptr_t* p) {
    Ref* ref = new MSORef<T>(mpp, _default);
    push_special(_method_entry_ref, ref, (intptr_t*)p);
    if (!ref->keep_after_pushing()) {
      delete ref;
    }
  }

  // This is for tagging special pointers that are not a reference to MetaspaceObj. It's currently
  // used to mark the method entry points in Method/ConstMethod.
  virtual void push_special(SpecialRef type, Ref* obj, intptr_t* p) {
    assert(type == _method_entry_ref, "only special type allowed for now");
  }
};

// This is a special MetaspaceClosure that visits each unique MetaspaceObj once.
class UniqueMetaspaceClosure : public MetaspaceClosure {
  static const int INITIAL_TABLE_SIZE = 15889;
  static const int MAX_TABLE_SIZE     = 1000000;

  // Do not override. Returns true if we are discovering ref->obj() for the first time.
  virtual bool do_ref(Ref* ref, bool read_only);

public:
  // Gets called the first time we discover an object.
  virtual bool do_unique_ref(Ref* ref, bool read_only) = 0;
  UniqueMetaspaceClosure() : _has_been_visited(INITIAL_TABLE_SIZE, MAX_TABLE_SIZE) {}

private:
  ResizeableResourceHashtable<address, bool, ResourceObj::C_HEAP,
                              mtClassShared> _has_been_visited;
};

#endif // SHARE_MEMORY_METASPACECLOSURE_HPP
