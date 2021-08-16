/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CODE_DEPENDENCIES_HPP
#define SHARE_CODE_DEPENDENCIES_HPP

#include "ci/ciCallSite.hpp"
#include "ci/ciKlass.hpp"
#include "ci/ciMethod.hpp"
#include "ci/ciMethodHandle.hpp"
#include "code/compressedStream.hpp"
#include "code/nmethod.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/hashtable.hpp"

//** Dependencies represent assertions (approximate invariants) within
// the runtime system, e.g. class hierarchy changes.  An example is an
// assertion that a given method is not overridden; another example is
// that a type has only one concrete subtype.  Compiled code which
// relies on such assertions must be discarded if they are overturned
// by changes in the runtime system.  We can think of these assertions
// as approximate invariants, because we expect them to be overturned
// very infrequently.  We are willing to perform expensive recovery
// operations when they are overturned.  The benefit, of course, is
// performing optimistic optimizations (!) on the object code.
//
// Changes in the class hierarchy due to dynamic linking or
// class evolution can violate dependencies.  There is enough
// indexing between classes and nmethods to make dependency
// checking reasonably efficient.

class ciEnv;
class nmethod;
class OopRecorder;
class xmlStream;
class CompileLog;
class CompileTask;
class DepChange;
class   KlassDepChange;
class     NewKlassDepChange;
class     KlassInitDepChange;
class   CallSiteDepChange;
class NoSafepointVerifier;

class Dependencies: public ResourceObj {
 public:
  // Note: In the comments on dependency types, most uses of the terms
  // subtype and supertype are used in a "non-strict" or "inclusive"
  // sense, and are starred to remind the reader of this fact.
  // Strict uses of the terms use the word "proper".
  //
  // Specifically, every class is its own subtype* and supertype*.
  // (This trick is easier than continually saying things like "Y is a
  // subtype of X or X itself".)
  //
  // Sometimes we write X > Y to mean X is a proper supertype of Y.
  // The notation X > {Y, Z} means X has proper subtypes Y, Z.
  // The notation X.m > Y means that Y inherits m from X, while
  // X.m > Y.m means Y overrides X.m.  A star denotes abstractness,
  // as *I > A, meaning (abstract) interface I is a super type of A,
  // or A.*m > B.m, meaning B.m implements abstract method A.m.
  //
  // In this module, the terms "subtype" and "supertype" refer to
  // Java-level reference type conversions, as detected by
  // "instanceof" and performed by "checkcast" operations.  The method
  // Klass::is_subtype_of tests these relations.  Note that "subtype"
  // is richer than "subclass" (as tested by Klass::is_subclass_of),
  // since it takes account of relations involving interface and array
  // types.
  //
  // To avoid needless complexity, dependencies involving array types
  // are not accepted.  If you need to make an assertion about an
  // array type, make the assertion about its corresponding element
  // types.  Any assertion that might change about an array type can
  // be converted to an assertion about its element type.
  //
  // Most dependencies are evaluated over a "context type" CX, which
  // stands for the set Subtypes(CX) of every Java type that is a subtype*
  // of CX.  When the system loads a new class or interface N, it is
  // responsible for re-evaluating changed dependencies whose context
  // type now includes N, that is, all super types of N.
  //
  enum DepType {
    end_marker = 0,

    // An 'evol' dependency simply notes that the contents of the
    // method were used.  If it evolves (is replaced), the nmethod
    // must be recompiled.  No other dependencies are implied.
    evol_method,
    FIRST_TYPE = evol_method,

    // A context type CX is a leaf it if has no proper subtype.
    leaf_type,

    // An abstract class CX has exactly one concrete subtype CC.
    abstract_with_unique_concrete_subtype,

    // Given a method M1 and a context class CX, the set MM(CX, M1) of
    // "concrete matching methods" in CX of M1 is the set of every
    // concrete M2 for which it is possible to create an invokevirtual
    // or invokeinterface call site that can reach either M1 or M2.
    // That is, M1 and M2 share a name, signature, and vtable index.
    // We wish to notice when the set MM(CX, M1) is just {M1}, or
    // perhaps a set of two {M1,M2}, and issue dependencies on this.

    // The set MM(CX, M1) can be computed by starting with any matching
    // concrete M2 that is inherited into CX, and then walking the
    // subtypes* of CX looking for concrete definitions.

    // The parameters to this dependency are the method M1 and the
    // context class CX.  M1 must be either inherited in CX or defined
    // in a subtype* of CX.  It asserts that MM(CX, M1) is no greater
    // than {M1}.
    unique_concrete_method_2, // one unique concrete method under CX

    // In addition to the method M1 and the context class CX, the parameters
    // to this dependency are the resolved class RC1 and the
    // resolved method RM1. It asserts that MM(CX, M1, RC1, RM1)
    // is no greater than {M1}. RC1 and RM1 are used to improve the precision
    // of the analysis.
    unique_concrete_method_4, // one unique concrete method under CX

    // This dependency asserts that no instances of class or it's
    // subclasses require finalization registration.
    no_finalizable_subclasses,

    // This dependency asserts when the CallSite.target value changed.
    call_site_target_value,

    TYPE_LIMIT
  };
  enum {
    LG2_TYPE_LIMIT = 4,  // assert(TYPE_LIMIT <= (1<<LG2_TYPE_LIMIT))

    // handy categorizations of dependency types:
    all_types           = ((1 << TYPE_LIMIT) - 1) & ((~0u) << FIRST_TYPE),

    non_klass_types     = (1 << call_site_target_value),
    klass_types         = all_types & ~non_klass_types,

    non_ctxk_types      = (1 << evol_method) | (1 << call_site_target_value),
    implicit_ctxk_types = 0,
    explicit_ctxk_types = all_types & ~(non_ctxk_types | implicit_ctxk_types),

    max_arg_count = 4,   // current maximum number of arguments (incl. ctxk)

    // A "context type" is a class or interface that
    // provides context for evaluating a dependency.
    // When present, it is one of the arguments (dep_context_arg).
    //
    // If a dependency does not have a context type, there is a
    // default context, depending on the type of the dependency.
    // This bit signals that a default context has been compressed away.
    default_context_type_bit = (1<<LG2_TYPE_LIMIT)
  };

  static const char* dep_name(DepType dept);
  static int         dep_args(DepType dept);

  static bool is_klass_type(           DepType dept) { return dept_in_mask(dept, klass_types        ); }

  static bool has_explicit_context_arg(DepType dept) { return dept_in_mask(dept, explicit_ctxk_types); }
  static bool has_implicit_context_arg(DepType dept) { return dept_in_mask(dept, implicit_ctxk_types); }

  static int           dep_context_arg(DepType dept) { return has_explicit_context_arg(dept) ? 0 : -1; }
  static int  dep_implicit_context_arg(DepType dept) { return has_implicit_context_arg(dept) ? 0 : -1; }

  static void check_valid_dependency_type(DepType dept);

#if INCLUDE_JVMCI
  // A Metadata* or object value recorded in an OopRecorder
  class DepValue {
   private:
    // Unique identifier of the value within the associated OopRecorder that
    // encodes both the category of the value (0: invalid, positive: metadata, negative: object)
    // and the index within a category specific array (metadata: index + 1, object: -(index + 1))
    int _id;

   public:
    DepValue() : _id(0) {}
    DepValue(OopRecorder* rec, Metadata* metadata, DepValue* candidate = NULL) {
      assert(candidate == NULL || candidate->is_metadata(), "oops");
      if (candidate != NULL && candidate->as_metadata(rec) == metadata) {
        _id = candidate->_id;
      } else {
        _id = rec->find_index(metadata) + 1;
      }
    }
    DepValue(OopRecorder* rec, jobject obj, DepValue* candidate = NULL) {
      assert(candidate == NULL || candidate->is_object(), "oops");
      if (candidate != NULL && candidate->as_object(rec) == obj) {
        _id = candidate->_id;
      } else {
        _id = -(rec->find_index(obj) + 1);
      }
    }

    // Used to sort values in ascending order of index() with metadata values preceding object values
    int sort_key() const { return -_id; }

    bool operator == (const DepValue& other) const   { return other._id == _id; }

    bool is_valid() const             { return _id != 0; }
    int  index() const                { assert(is_valid(), "oops"); return _id < 0 ? -(_id + 1) : _id - 1; }
    bool is_metadata() const          { assert(is_valid(), "oops"); return _id > 0; }
    bool is_object() const            { assert(is_valid(), "oops"); return _id < 0; }

    Metadata*  as_metadata(OopRecorder* rec) const    { assert(is_metadata(), "oops"); return rec->metadata_at(index()); }
    Klass*     as_klass(OopRecorder* rec) const {
      Metadata* m = as_metadata(rec);
      assert(m != NULL, "as_metadata returned NULL");
      assert(m->is_klass(), "oops");
      return (Klass*) m;
    }
    Method*    as_method(OopRecorder* rec) const {
      Metadata* m = as_metadata(rec);
      assert(m != NULL, "as_metadata returned NULL");
      assert(m->is_method(), "oops");
      return (Method*) m;
    }
    jobject    as_object(OopRecorder* rec) const      { assert(is_object(), "oops"); return rec->oop_at(index()); }
  };
#endif // INCLUDE_JVMCI

 private:
  // State for writing a new set of dependencies:
  GrowableArray<int>*       _dep_seen;  // (seen[h->ident] & (1<<dept))
  GrowableArray<ciBaseObject*>*  _deps[TYPE_LIMIT];
#if INCLUDE_JVMCI
  bool _using_dep_values;
  GrowableArray<DepValue>*  _dep_values[TYPE_LIMIT];
#endif

  static const char* _dep_name[TYPE_LIMIT];
  static int         _dep_args[TYPE_LIMIT];

  static bool dept_in_mask(DepType dept, int mask) {
    return (int)dept >= 0 && dept < TYPE_LIMIT && ((1<<dept) & mask) != 0;
  }

  bool note_dep_seen(int dept, ciBaseObject* x) {
    assert(dept < BitsPerInt, "oob");
    int x_id = x->ident();
    assert(_dep_seen != NULL, "deps must be writable");
    int seen = _dep_seen->at_grow(x_id, 0);
    _dep_seen->at_put(x_id, seen | (1<<dept));
    // return true if we've already seen dept/x
    return (seen & (1<<dept)) != 0;
  }

#if INCLUDE_JVMCI
  bool note_dep_seen(int dept, DepValue x) {
    assert(dept < BitsPerInt, "oops");
    // place metadata deps at even indexes, object deps at odd indexes
    int x_id = x.is_metadata() ? x.index() * 2 : (x.index() * 2) + 1;
    assert(_dep_seen != NULL, "deps must be writable");
    int seen = _dep_seen->at_grow(x_id, 0);
    _dep_seen->at_put(x_id, seen | (1<<dept));
    // return true if we've already seen dept/x
    return (seen & (1<<dept)) != 0;
  }
#endif

  bool maybe_merge_ctxk(GrowableArray<ciBaseObject*>* deps,
                        int ctxk_i, ciKlass* ctxk);
#if INCLUDE_JVMCI
  bool maybe_merge_ctxk(GrowableArray<DepValue>* deps,
                        int ctxk_i, DepValue ctxk);
#endif

  void sort_all_deps();
  size_t estimate_size_in_bytes();

  // Initialize _deps, etc.
  void initialize(ciEnv* env);

  // State for making a new set of dependencies:
  OopRecorder* _oop_recorder;

  // Logging support
  CompileLog* _log;

  address  _content_bytes;  // everything but the oop references, encoded
  size_t   _size_in_bytes;

 public:
  // Make a new empty dependencies set.
  Dependencies(ciEnv* env) {
    initialize(env);
  }
#if INCLUDE_JVMCI
  Dependencies(Arena* arena, OopRecorder* oop_recorder, CompileLog* log);
#endif

 private:
  // Check for a valid context type.
  // Enforce the restriction against array types.
  static void check_ctxk(ciKlass* ctxk) {
    assert(ctxk->is_instance_klass(), "java types only");
  }
  static void check_ctxk_concrete(ciKlass* ctxk) {
    assert(is_concrete_klass(ctxk->as_instance_klass()), "must be concrete");
  }
  static void check_ctxk_abstract(ciKlass* ctxk) {
    check_ctxk(ctxk);
    assert(!is_concrete_klass(ctxk->as_instance_klass()), "must be abstract");
  }
  static void check_unique_method(ciKlass* ctxk, ciMethod* m) {
    assert(!m->can_be_statically_bound(ctxk->as_instance_klass()), "redundant");
  }

  void assert_common_1(DepType dept, ciBaseObject* x);
  void assert_common_2(DepType dept, ciBaseObject* x0, ciBaseObject* x1);
  void assert_common_4(DepType dept, ciKlass* ctxk, ciBaseObject* x1, ciBaseObject* x2, ciBaseObject* x3);

 public:
  // Adding assertions to a new dependency set at compile time:
  void assert_evol_method(ciMethod* m);
  void assert_leaf_type(ciKlass* ctxk);
  void assert_abstract_with_unique_concrete_subtype(ciKlass* ctxk, ciKlass* conck);
  void assert_unique_concrete_method(ciKlass* ctxk, ciMethod* uniqm);
  void assert_unique_concrete_method(ciKlass* ctxk, ciMethod* uniqm, ciKlass* resolved_klass, ciMethod* resolved_method);
  void assert_has_no_finalizable_subclasses(ciKlass* ctxk);
  void assert_call_site_target_value(ciCallSite* call_site, ciMethodHandle* method_handle);

#if INCLUDE_JVMCI
 private:
  static void check_ctxk(Klass* ctxk) {
    assert(ctxk->is_instance_klass(), "java types only");
  }
  static void check_ctxk_abstract(Klass* ctxk) {
    check_ctxk(ctxk);
    assert(ctxk->is_abstract(), "must be abstract");
  }
  static void check_unique_method(Klass* ctxk, Method* m) {
    assert(!m->can_be_statically_bound(InstanceKlass::cast(ctxk)), "redundant");
  }

  void assert_common_1(DepType dept, DepValue x);
  void assert_common_2(DepType dept, DepValue x0, DepValue x1);

 public:
  void assert_evol_method(Method* m);
  void assert_has_no_finalizable_subclasses(Klass* ctxk);
  void assert_leaf_type(Klass* ctxk);
  void assert_unique_concrete_method(Klass* ctxk, Method* uniqm);
  void assert_abstract_with_unique_concrete_subtype(Klass* ctxk, Klass* conck);
  void assert_call_site_target_value(oop callSite, oop methodHandle);
#endif // INCLUDE_JVMCI

  // Define whether a given method or type is concrete.
  // These methods define the term "concrete" as used in this module.
  // For this module, an "abstract" class is one which is non-concrete.
  //
  // Future optimizations may allow some classes to remain
  // non-concrete until their first instantiation, and allow some
  // methods to remain non-concrete until their first invocation.
  // In that case, there would be a middle ground between concrete
  // and abstract (as defined by the Java language and VM).
  static bool is_concrete_klass(Klass* k);    // k is instantiable
  static bool is_concrete_method(Method* m, Klass* k);  // m is invocable
  static Klass* find_finalizable_subclass(InstanceKlass* ik);

  static bool is_concrete_root_method(Method* uniqm, InstanceKlass* ctxk);
  static Klass* find_witness_AME(InstanceKlass* ctxk, Method* m, KlassDepChange* changes = NULL);

  // These versions of the concreteness queries work through the CI.
  // The CI versions are allowed to skew sometimes from the VM
  // (oop-based) versions.  The cost of such a difference is a
  // (safely) aborted compilation, or a deoptimization, or a missed
  // optimization opportunity.
  //
  // In order to prevent spurious assertions, query results must
  // remain stable within any single ciEnv instance.  (I.e., they must
  // not go back into the VM to get their value; they must cache the
  // bit in the CI, either eagerly or lazily.)
  static bool is_concrete_klass(ciInstanceKlass* k); // k appears instantiable
  static bool has_finalizable_subclass(ciInstanceKlass* k);

  // As a general rule, it is OK to compile under the assumption that
  // a given type or method is concrete, even if it at some future
  // point becomes abstract.  So dependency checking is one-sided, in
  // that it permits supposedly concrete classes or methods to turn up
  // as really abstract.  (This shouldn't happen, except during class
  // evolution, but that's the logic of the checking.)  However, if a
  // supposedly abstract class or method suddenly becomes concrete, a
  // dependency on it must fail.

  // Checking old assertions at run-time (in the VM only):
  static Klass* check_evol_method(Method* m);
  static Klass* check_leaf_type(InstanceKlass* ctxk);
  static Klass* check_abstract_with_unique_concrete_subtype(InstanceKlass* ctxk, Klass* conck, NewKlassDepChange* changes = NULL);
  static Klass* check_unique_concrete_method(InstanceKlass* ctxk, Method* uniqm, NewKlassDepChange* changes = NULL);
  static Klass* check_unique_concrete_method(InstanceKlass* ctxk, Method* uniqm, Klass* resolved_klass, Method* resolved_method, KlassDepChange* changes = NULL);
  static Klass* check_has_no_finalizable_subclasses(InstanceKlass* ctxk, NewKlassDepChange* changes = NULL);
  static Klass* check_call_site_target_value(oop call_site, oop method_handle, CallSiteDepChange* changes = NULL);
  // A returned Klass* is NULL if the dependency assertion is still
  // valid.  A non-NULL Klass* is a 'witness' to the assertion
  // failure, a point in the class hierarchy where the assertion has
  // been proven false.  For example, if check_leaf_type returns
  // non-NULL, the value is a subtype of the supposed leaf type.  This
  // witness value may be useful for logging the dependency failure.
  // Note that, when a dependency fails, there may be several possible
  // witnesses to the failure.  The value returned from the check_foo
  // method is chosen arbitrarily.

  // The 'changes' value, if non-null, requests a limited spot-check
  // near the indicated recent changes in the class hierarchy.
  // It is used by DepStream::spot_check_dependency_at.

  // Detecting possible new assertions:
  static Klass*  find_unique_concrete_subtype(InstanceKlass* ctxk);
  static Method* find_unique_concrete_method(InstanceKlass* ctxk, Method* m,
                                             Klass** participant = NULL); // out parameter
  static Method* find_unique_concrete_method(InstanceKlass* ctxk, Method* m, Klass* resolved_klass, Method* resolved_method);

#ifdef ASSERT
  static bool verify_method_context(InstanceKlass* ctxk, Method* m);
#endif // ASSERT

  // Create the encoding which will be stored in an nmethod.
  void encode_content_bytes();

  address content_bytes() {
    assert(_content_bytes != NULL, "encode it first");
    return _content_bytes;
  }
  size_t size_in_bytes() {
    assert(_content_bytes != NULL, "encode it first");
    return _size_in_bytes;
  }

  OopRecorder* oop_recorder() { return _oop_recorder; }
  CompileLog*  log()          { return _log; }

  void copy_to(nmethod* nm);

  DepType validate_dependencies(CompileTask* task, char** failure_detail = NULL);

  void log_all_dependencies();

  void log_dependency(DepType dept, GrowableArray<ciBaseObject*>* args) {
    ResourceMark rm;
    int argslen = args->length();
    write_dependency_to(log(), dept, args);
    guarantee(argslen == args->length(),
              "args array cannot grow inside nested ResoureMark scope");
  }

  void log_dependency(DepType dept,
                      ciBaseObject* x0,
                      ciBaseObject* x1 = NULL,
                      ciBaseObject* x2 = NULL,
                      ciBaseObject* x3 = NULL) {
    if (log() == NULL) {
      return;
    }
    ResourceMark rm;
    GrowableArray<ciBaseObject*>* ciargs =
                new GrowableArray<ciBaseObject*>(dep_args(dept));
    assert (x0 != NULL, "no log x0");
    ciargs->push(x0);

    if (x1 != NULL) {
      ciargs->push(x1);
    }
    if (x2 != NULL) {
      ciargs->push(x2);
    }
    if (x3 != NULL) {
      ciargs->push(x3);
    }
    assert(ciargs->length() == dep_args(dept), "");
    log_dependency(dept, ciargs);
  }

  class DepArgument : public ResourceObj {
   private:
    bool  _is_oop;
    bool  _valid;
    void* _value;
   public:
    DepArgument() : _is_oop(false), _valid(false), _value(NULL) {}
    DepArgument(oop v): _is_oop(true), _valid(true), _value(v) {}
    DepArgument(Metadata* v): _is_oop(false), _valid(true), _value(v) {}

    bool is_null() const               { return _value == NULL; }
    bool is_oop() const                { return _is_oop; }
    bool is_metadata() const           { return !_is_oop; }
    bool is_klass() const              { return is_metadata() && metadata_value()->is_klass(); }
    bool is_method() const             { return is_metadata() && metadata_value()->is_method(); }

    oop oop_value() const              { assert(_is_oop && _valid, "must be"); return cast_to_oop(_value); }
    Metadata* metadata_value() const   { assert(!_is_oop && _valid, "must be"); return (Metadata*) _value; }
  };

  static void print_dependency(DepType dept,
                               GrowableArray<DepArgument>* args,
                               Klass* witness = NULL, outputStream* st = tty);

 private:
  // helper for encoding common context types as zero:
  static ciKlass* ctxk_encoded_as_null(DepType dept, ciBaseObject* x);

  static Klass* ctxk_encoded_as_null(DepType dept, Metadata* x);

  static void write_dependency_to(CompileLog* log,
                                  DepType dept,
                                  GrowableArray<ciBaseObject*>* args,
                                  Klass* witness = NULL);
  static void write_dependency_to(CompileLog* log,
                                  DepType dept,
                                  GrowableArray<DepArgument>* args,
                                  Klass* witness = NULL);
  static void write_dependency_to(xmlStream* xtty,
                                  DepType dept,
                                  GrowableArray<DepArgument>* args,
                                  Klass* witness = NULL);
 public:
  // Use this to iterate over an nmethod's dependency set.
  // Works on new and old dependency sets.
  // Usage:
  //
  // ;
  // Dependencies::DepType dept;
  // for (Dependencies::DepStream deps(nm); deps.next(); ) {
  //   ...
  // }
  //
  // The caller must be in the VM, since oops are not wrapped in handles.
  class DepStream {
  private:
    nmethod*              _code;   // null if in a compiler thread
    Dependencies*         _deps;   // null if not in a compiler thread
    CompressedReadStream  _bytes;
#ifdef ASSERT
    size_t                _byte_limit;
#endif

    // iteration variables:
    DepType               _type;
    int                   _xi[max_arg_count+1];

    void initial_asserts(size_t byte_limit) NOT_DEBUG({});

    inline Metadata* recorded_metadata_at(int i);
    inline oop recorded_oop_at(int i);

    Klass* check_klass_dependency(KlassDepChange* changes);
    Klass* check_new_klass_dependency(NewKlassDepChange* changes);
    Klass* check_klass_init_dependency(KlassInitDepChange* changes);
    Klass* check_call_site_dependency(CallSiteDepChange* changes);

    void trace_and_log_witness(Klass* witness);

  public:
    DepStream(Dependencies* deps)
      : _code(NULL),
        _deps(deps),
        _bytes(deps->content_bytes())
    {
      initial_asserts(deps->size_in_bytes());
    }
    DepStream(nmethod* code)
      : _code(code),
        _deps(NULL),
        _bytes(code->dependencies_begin())
    {
      initial_asserts(code->dependencies_size());
    }

    bool next();

    DepType type()               { return _type; }
    bool is_oop_argument(int i)  { return type() == call_site_target_value; }
    uintptr_t get_identifier(int i);

    int argument_count()         { return dep_args(type()); }
    int argument_index(int i)    { assert(0 <= i && i < argument_count(), "oob");
                                   return _xi[i]; }
    Metadata* argument(int i);     // => recorded_oop_at(argument_index(i))
    oop argument_oop(int i);         // => recorded_oop_at(argument_index(i))
    InstanceKlass* context_type();

    bool is_klass_type()         { return Dependencies::is_klass_type(type()); }

    Method* method_argument(int i) {
      Metadata* x = argument(i);
      assert(x->is_method(), "type");
      return (Method*) x;
    }
    Klass* type_argument(int i) {
      Metadata* x = argument(i);
      assert(x->is_klass(), "type");
      return (Klass*) x;
    }

    // The point of the whole exercise:  Is this dep still OK?
    Klass* check_dependency() {
      Klass* result = check_klass_dependency(NULL);
      if (result != NULL)  return result;
      return check_call_site_dependency(NULL);
    }

    // A lighter version:  Checks only around recent changes in a class
    // hierarchy.  (See Universe::flush_dependents_on.)
    Klass* spot_check_dependency_at(DepChange& changes);

    // Log the current dependency to xtty or compilation log.
    void log_dependency(Klass* witness = NULL);

    // Print the current dependency to tty.
    void print_dependency(Klass* witness = NULL, bool verbose = false, outputStream* st = tty);
  };
  friend class Dependencies::DepStream;

  static void print_statistics();
};


class DependencySignature : public ResourceObj {
 private:
  int                   _args_count;
  uintptr_t             _argument_hash[Dependencies::max_arg_count];
  Dependencies::DepType _type;

 public:
  DependencySignature(Dependencies::DepStream& dep) {
    _args_count = dep.argument_count();
    _type = dep.type();
    for (int i = 0; i < _args_count; i++) {
      _argument_hash[i] = dep.get_identifier(i);
    }
  }

  static bool     equals(DependencySignature const& s1, DependencySignature const& s2);
  static unsigned hash  (DependencySignature const& s1) { return s1.arg(0) >> 2; }

  int args_count()             const { return _args_count; }
  uintptr_t arg(int idx)       const { return _argument_hash[idx]; }
  Dependencies::DepType type() const { return _type; }

};


// Every particular DepChange is a sub-class of this class.
class DepChange : public StackObj {
 public:
  // What kind of DepChange is this?
  virtual bool is_klass_change()      const { return false; }
  virtual bool is_new_klass_change()  const { return false; }
  virtual bool is_klass_init_change() const { return false; }
  virtual bool is_call_site_change()  const { return false; }

  virtual void mark_for_deoptimization(nmethod* nm) = 0;

  // Subclass casting with assertions.
  KlassDepChange*    as_klass_change() {
    assert(is_klass_change(), "bad cast");
    return (KlassDepChange*) this;
  }
  NewKlassDepChange* as_new_klass_change() {
    assert(is_new_klass_change(), "bad cast");
    return (NewKlassDepChange*) this;
  }
  KlassInitDepChange* as_klass_init_change() {
    assert(is_klass_init_change(), "bad cast");
    return (KlassInitDepChange*) this;
  }
  CallSiteDepChange* as_call_site_change() {
    assert(is_call_site_change(), "bad cast");
    return (CallSiteDepChange*) this;
  }

  void print();

 public:
  enum ChangeType {
    NO_CHANGE = 0,              // an uninvolved klass
    Change_new_type,            // a newly loaded type
    Change_new_sub,             // a super with a new subtype
    Change_new_impl,            // an interface with a new implementation
    CHANGE_LIMIT,
    Start_Klass = CHANGE_LIMIT  // internal indicator for ContextStream
  };

  // Usage:
  // for (DepChange::ContextStream str(changes); str.next(); ) {
  //   Klass* k = str.klass();
  //   switch (str.change_type()) {
  //     ...
  //   }
  // }
  class ContextStream : public StackObj {
   private:
    DepChange&  _changes;
    friend class DepChange;

    // iteration variables:
    ChangeType  _change_type;
    Klass*      _klass;
    Array<InstanceKlass*>* _ti_base;    // i.e., transitive_interfaces
    int         _ti_index;
    int         _ti_limit;

    // start at the beginning:
    void start();

   public:
    ContextStream(DepChange& changes)
      : _changes(changes)
    { start(); }

    ContextStream(DepChange& changes, NoSafepointVerifier& nsv)
      : _changes(changes)
      // the nsv argument makes it safe to hold oops like _klass
    { start(); }

    bool next();

    ChangeType change_type()     { return _change_type; }
    Klass*     klass()           { return _klass; }
  };
  friend class DepChange::ContextStream;
};


// A class hierarchy change coming through the VM (under the Compile_lock).
// The change is structured as a single type with any number of supers
// and implemented interface types.  Other than the type, any of the
// super types can be context types for a relevant dependency, which the
// type could invalidate.
class KlassDepChange : public DepChange {
 private:
  // each change set is rooted in exactly one type (at present):
  InstanceKlass* _type;

  void initialize();

 protected:
  // notes the type, marks it and all its super-types
  KlassDepChange(InstanceKlass* type) : _type(type) {
    initialize();
  }

  // cleans up the marks
  ~KlassDepChange();

 public:
  // What kind of DepChange is this?
  virtual bool is_klass_change() const { return true; }

  virtual void mark_for_deoptimization(nmethod* nm) {
    nm->mark_for_deoptimization(/*inc_recompile_counts=*/true);
  }

  InstanceKlass* type() { return _type; }

  // involves_context(k) is true if k == _type or any of its super types
  bool involves_context(Klass* k);
};

// A class hierarchy change: new type is loaded.
class NewKlassDepChange : public KlassDepChange {
 public:
  NewKlassDepChange(InstanceKlass* new_type) : KlassDepChange(new_type) {}

  // What kind of DepChange is this?
  virtual bool is_new_klass_change() const { return true; }

  InstanceKlass* new_type() { return type(); }
};

// Change in initialization state of a loaded class.
class KlassInitDepChange : public KlassDepChange {
 public:
  KlassInitDepChange(InstanceKlass* type) : KlassDepChange(type) {}

  // What kind of DepChange is this?
  virtual bool is_klass_init_change() const { return true; }
};

// A CallSite has changed its target.
class CallSiteDepChange : public DepChange {
 private:
  Handle _call_site;
  Handle _method_handle;

 public:
  CallSiteDepChange(Handle call_site, Handle method_handle);

  // What kind of DepChange is this?
  virtual bool is_call_site_change() const { return true; }

  virtual void mark_for_deoptimization(nmethod* nm) {
    nm->mark_for_deoptimization(/*inc_recompile_counts=*/false);
  }

  oop call_site()     const { return _call_site();     }
  oop method_handle() const { return _method_handle(); }
};

#endif // SHARE_CODE_DEPENDENCIES_HPP
