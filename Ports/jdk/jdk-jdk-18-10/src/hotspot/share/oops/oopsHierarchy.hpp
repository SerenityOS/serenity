/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_OOPSHIERARCHY_HPP
#define SHARE_OOPS_OOPSHIERARCHY_HPP

#include "metaprogramming/integralConstant.hpp"
#include "metaprogramming/primitiveConversions.hpp"
#include "utilities/globalDefinitions.hpp"

// OBJECT hierarchy
// This hierarchy is a representation hierarchy, i.e. if A is a superclass
// of B, A's representation is a prefix of B's representation.

// Global offset instead of address for an oop within a java object.
enum class narrowOop : uint32_t { null = 0 };

// If compressed klass pointers then use narrowKlass.
typedef juint  narrowKlass;

typedef void* OopOrNarrowOopStar;

#ifndef CHECK_UNHANDLED_OOPS

typedef class oopDesc*                    oop;
typedef class   instanceOopDesc*            instanceOop;
typedef class   arrayOopDesc*               arrayOop;
typedef class     objArrayOopDesc*            objArrayOop;
typedef class     typeArrayOopDesc*           typeArrayOop;

#else

// When CHECK_UNHANDLED_OOPS is defined, an "oop" is a class with a
// carefully chosen set of constructors and conversion operators to go
// to and from the underlying oopDesc pointer type.
//
// Because oop and its subclasses <type>Oop are class types, arbitrary
// conversions are not accepted by the compiler.  Applying a cast to
// an oop will cause the best matched conversion operator to be
// invoked returning the underlying oopDesc* type if appropriate.
// No copy constructors, explicit user conversions or operators of
// numerical type should be defined within the oop class. Most C++
// compilers will issue a compile time error concerning the overloading
// ambiguity between operators of numerical and pointer types. If
// a conversion to or from an oop to a numerical type is needed,
// use the inline template methods, cast_*_oop, defined below.
//
// Converting NULL to oop to Handle implicit is no longer accepted by the
// compiler because there are too many steps in the conversion.  Use Handle()
// instead, which generates less code anyway.

class Thread;
class oopDesc;

extern "C" bool CheckUnhandledOops;

class oop {
  oopDesc* _o;

  void register_oop();
  void unregister_oop();

  void register_if_checking() {
    if (CheckUnhandledOops) register_oop();
  }

public:
  oop()             : _o(nullptr) { register_if_checking(); }
  oop(const oop& o) : _o(o._o)    { register_if_checking(); }
  oop(oopDesc* o)   : _o(o)       { register_if_checking(); }
  ~oop() {
    if (CheckUnhandledOops) unregister_oop();
  }

  oopDesc* obj() const                 { return _o; }
  oopDesc* operator->() const          { return _o; }
  operator oopDesc* () const           { return _o; }

  bool operator==(const oop& o) const  { return _o == o._o; }
  bool operator!=(const oop& o) const  { return _o != o._o; }

  bool operator==(std::nullptr_t) const     { return _o == nullptr; }
  bool operator!=(std::nullptr_t) const     { return _o != nullptr; }

  oop& operator=(const oop& o)         { _o = o._o; return *this; }
};

template<>
struct PrimitiveConversions::Translate<oop> : public TrueType {
  typedef oop Value;
  typedef oopDesc* Decayed;

  static Decayed decay(Value x) { return x.obj(); }
  static Value recover(Decayed x) { return oop(x); }
};

#define DEF_OOP(type)                                                      \
   class type##OopDesc;                                                    \
   class type##Oop : public oop {                                          \
     public:                                                               \
       type##Oop() : oop() {}                                              \
       type##Oop(const type##Oop& o) : oop(o) {}                           \
       type##Oop(const oop& o) : oop(o) {}                                 \
       type##Oop(type##OopDesc* o) : oop((oopDesc*)o) {}                   \
       operator type##OopDesc* () const { return (type##OopDesc*)obj(); }  \
       type##OopDesc* operator->() const {                                 \
            return (type##OopDesc*)obj();                                  \
       }                                                                   \
       type##Oop& operator=(const type##Oop& o) {                          \
            oop::operator=(o);                                             \
            return *this;                                                  \
       }                                                                   \
   };                                                                      \
                                                                           \
   template<>                                                              \
   struct PrimitiveConversions::Translate<type##Oop> : public TrueType {   \
     typedef type##Oop Value;                                              \
     typedef type##OopDesc* Decayed;                                       \
                                                                           \
     static Decayed decay(Value x) { return (type##OopDesc*)x.obj(); }     \
     static Value recover(Decayed x) { return type##Oop(x); }              \
   };

DEF_OOP(instance);
DEF_OOP(array);
DEF_OOP(objArray);
DEF_OOP(typeArray);

#endif // CHECK_UNHANDLED_OOPS

// Cast functions to convert to and from oops.
template <typename T> inline oop cast_to_oop(T value) {
  return (oopDesc*)value;
}
template <typename T> inline T cast_from_oop(oop o) {
  return (T)(CHECK_UNHANDLED_OOPS_ONLY((oopDesc*))o);
}

// The metadata hierarchy is separate from the oop hierarchy

//      class MetaspaceObj
class   ConstMethod;
class   ConstantPoolCache;
class   MethodData;
//      class Metadata
class   Method;
class   ConstantPool;
//      class CHeapObj
class   CompiledICHolder;


// The klass hierarchy is separate from the oop hierarchy.

class Klass;
class   InstanceKlass;
class     InstanceMirrorKlass;
class     InstanceClassLoaderKlass;
class     InstanceRefKlass;
class   ArrayKlass;
class     ObjArrayKlass;
class     TypeArrayKlass;

#endif // SHARE_OOPS_OOPSHIERARCHY_HPP
