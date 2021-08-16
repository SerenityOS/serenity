/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "classfile/bytecodeAssembler.hpp"
#include "classfile/defaultMethods.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/arguments.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/signature.hpp"
#include "runtime/thread.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.hpp"
#include "oops/method.hpp"
#include "utilities/accessFlags.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/ostream.hpp"
#include "utilities/pair.hpp"
#include "utilities/resourceHash.hpp"

typedef enum { QUALIFIED, DISQUALIFIED } QualifiedState;

static void print_slot(outputStream* str, Symbol* name, Symbol* signature) {
  str->print("%s%s", name->as_C_string(), signature->as_C_string());
}

static void print_method(outputStream* str, Method* mo, bool with_class=true) {
  if (with_class) {
    str->print("%s.", mo->klass_name()->as_C_string());
  }
  print_slot(str, mo->name(), mo->signature());
}

/**
 * Perform a depth-first iteration over the class hierarchy, applying
 * algorithmic logic as it goes.
 *
 * This class is one half of the inheritance hierarchy analysis mechanism.
 * It is meant to be used in conjunction with another class, the algorithm,
 * which is indicated by the ALGO template parameter.  This class can be
 * paired with any algorithm class that provides the required methods.
 *
 * This class contains all the mechanics for iterating over the class hierarchy
 * starting at a particular root, without recursing (thus limiting stack growth
 * from this point).  It visits each superclass (if present) and superinterface
 * in a depth-first manner, with callbacks to the ALGO class as each class is
 * encountered (visit()), The algorithm can cut-off further exploration of a
 * particular branch by returning 'false' from a visit() call.
 *
 * The ALGO class, must provide a visit() method, which each of which will be
 * called once for each node in the inheritance tree during the iteration.  In
 * addition, it can provide a memory block via new_node_data(), which it can
 * use for node-specific storage (and access via the current_data() and
 * data_at_depth(int) methods).
 *
 * Bare minimum needed to be an ALGO class:
 * class Algo : public HierarchyVisitor<Algo> {
 *   void* new_node_data() { return NULL; }
 *   void free_node_data(void* data) { return; }
 *   bool visit() { return true; }
 * };
 */
template <class ALGO>
class HierarchyVisitor : StackObj {
 private:

  class Node : public ResourceObj {
   public:
    InstanceKlass* _class;
    bool _super_was_visited;
    int _interface_index;
    void* _algorithm_data;

    Node(InstanceKlass* cls, void* data, bool visit_super)
        : _class(cls), _super_was_visited(!visit_super),
          _interface_index(0), _algorithm_data(data) {}

    void update(InstanceKlass* cls, void* data, bool visit_super) {
      _class = cls;
      _super_was_visited = !visit_super;
      _interface_index = 0;
      _algorithm_data = data;
    }
    int number_of_interfaces() { return _class->local_interfaces()->length(); }
    int interface_index() { return _interface_index; }
    void set_super_visited() { _super_was_visited = true; }
    void increment_visited_interface() { ++_interface_index; }
    void set_all_interfaces_visited() {
      _interface_index = number_of_interfaces();
    }
    bool has_visited_super() { return _super_was_visited; }
    bool has_visited_all_interfaces() {
      return interface_index() >= number_of_interfaces();
    }
    InstanceKlass* interface_at(int index) {
      return _class->local_interfaces()->at(index);
    }
    InstanceKlass* next_super() { return _class->java_super(); }
    InstanceKlass* next_interface() {
      return interface_at(interface_index());
    }
  };

  bool _visited_Object;

  GrowableArray<Node*> _path;
  GrowableArray<Node*> _free_nodes;

  Node* current_top() const { return _path.top(); }
  bool has_more_nodes() const { return _path.length() > 0; }
  void push(InstanceKlass* cls, ALGO* algo) {
    assert(cls != NULL, "Requires a valid instance class");
    if (cls == vmClasses::Object_klass()) {
      _visited_Object = true;
    }
    void* data = algo->new_node_data();
    Node* node;
    if (_free_nodes.is_empty()) { // Add a new node
      node = new Node(cls, data, has_super(cls));
    } else { // Reuse existing node and data
      node = _free_nodes.pop();
      node->update(cls, data, has_super(cls));
    }
    _path.push(node);
  }
  void pop() {
    Node* node = _path.pop();
    // Make the node available for reuse
    _free_nodes.push(node);
  }

  // Since the starting point can be an interface, we must ensure we catch
  // j.l.Object as the super once in those cases. The _visited_Object flag
  // only ensures we don't then repeatedly enqueue Object for each interface
  // in the class hierarchy.
  bool has_super(InstanceKlass* cls) {
    return cls->super() != NULL && (!_visited_Object || !cls->is_interface());
  }

  Node* node_at_depth(int i) const {
    return (i >= _path.length()) ? NULL : _path.at(_path.length() - i - 1);
  }

 protected:

  // Resets the visitor
  void reset() {
    _visited_Object = false;
  }

  // Accessors available to the algorithm
  int current_depth() const { return _path.length() - 1; }

  InstanceKlass* class_at_depth(int i) {
    Node* n = node_at_depth(i);
    return n == NULL ? NULL : n->_class;
  }
  InstanceKlass* current_class() { return class_at_depth(0); }

  void* data_at_depth(int i) {
    Node* n = node_at_depth(i);
    return n == NULL ? NULL : n->_algorithm_data;
  }
  void* current_data() { return data_at_depth(0); }

 public:
  HierarchyVisitor() : _visited_Object(false), _path() {}

  void run(InstanceKlass* root) {
    ALGO* algo = static_cast<ALGO*>(this);

    push(root, algo);
    bool top_needs_visit = true;
    do {
      Node* top = current_top();
      if (top_needs_visit) {
        if (algo->visit() == false) {
          // algorithm does not want to continue along this path.  Arrange
          // it so that this state is immediately popped off the stack
          top->set_super_visited();
          top->set_all_interfaces_visited();
        }
        top_needs_visit = false;
      }

      if (top->has_visited_super() && top->has_visited_all_interfaces()) {
        algo->free_node_data(top->_algorithm_data);
        pop();
      } else {
        InstanceKlass* next = NULL;
        if (top->has_visited_super() == false) {
          next = top->next_super();
          top->set_super_visited();
        } else {
          next = top->next_interface();
          top->increment_visited_interface();
        }
        assert(next != NULL, "Otherwise we shouldn't be here");
        push(next, algo);
        top_needs_visit = true;
      }
    } while (has_more_nodes());
  }
};

class PrintHierarchy : public HierarchyVisitor<PrintHierarchy> {
 private:
   outputStream* _st;
 public:
  bool visit() {
    InstanceKlass* cls = current_class();
    streamIndentor si(_st, current_depth() * 2);
    _st->indent().print_cr("%s", cls->name()->as_C_string());
    return true;
  }

  void* new_node_data() { return NULL; }
  void free_node_data(void* data) { return; }

  PrintHierarchy(outputStream* st = tty) : _st(st) {}
};

// Used to register InstanceKlass objects and all related metadata structures
// (Methods, ConstantPools) as "in-use" by the current thread so that they can't
// be deallocated by class redefinition while we're using them.  The classes are
// de-registered when this goes out of scope.
//
// Once a class is registered, we need not bother with methodHandles or
// constantPoolHandles for it's associated metadata.
class KeepAliveRegistrar : public StackObj {
 private:
  Thread* _thread;
  GrowableArray<ConstantPool*> _keep_alive;

 public:
  KeepAliveRegistrar(Thread* thread) : _thread(thread), _keep_alive(6) {
    assert(thread == Thread::current(), "Must be current thread");
  }

  ~KeepAliveRegistrar() {
    for (int i = _keep_alive.length() - 1; i >= 0; --i) {
      ConstantPool* cp = _keep_alive.at(i);
      int idx = _thread->metadata_handles()->find_from_end(cp);
      assert(idx > 0, "Must be in the list");
      _thread->metadata_handles()->remove_at(idx);
    }
  }

  // Register a class as 'in-use' by the thread.  It's fine to register a class
  // multiple times (though perhaps inefficient)
  void register_class(InstanceKlass* ik) {
    ConstantPool* cp = ik->constants();
    _keep_alive.push(cp);
    _thread->metadata_handles()->push(cp);
  }
};

class KeepAliveVisitor : public HierarchyVisitor<KeepAliveVisitor> {
 private:
  KeepAliveRegistrar* _registrar;

 public:
  KeepAliveVisitor(KeepAliveRegistrar* registrar) : _registrar(registrar) {}

  void* new_node_data() { return NULL; }
  void free_node_data(void* data) { return; }

  bool visit() {
    _registrar->register_class(current_class());
    return true;
  }
};


// A method family contains a set of all methods that implement a single
// erased method. As members of the set are collected while walking over the
// hierarchy, they are tagged with a qualification state.  The qualification
// state for an erased method is set to disqualified if there exists a path
// from the root of hierarchy to the method that contains an interleaving
// erased method defined in an interface.

class MethodState {
 public:
  Method* _method;
  QualifiedState _state;

  MethodState() : _method(NULL), _state(DISQUALIFIED) {}
  MethodState(Method* method, QualifiedState state) : _method(method), _state(state) {}
};

class MethodFamily : public ResourceObj {
 private:

  GrowableArray<MethodState> _members;

  Method* _selected_target;  // Filled in later, if a unique target exists
  Symbol* _exception_message; // If no unique target is found
  Symbol* _exception_name;    // If no unique target is found

  MethodState* find_method(Method* method) {
    for (int i = 0; i < _members.length(); i++) {
      if (_members.at(i)._method == method) {
        return &_members.at(i);
      }
    }
    return NULL;
  }

  void add_method(Method* method, QualifiedState state) {
    MethodState method_state(method, state);
    _members.append(method_state);
  }

  Symbol* generate_no_defaults_message() const;
  Symbol* generate_method_message(Symbol *klass_name, Method* method) const;
  Symbol* generate_conflicts_message(GrowableArray<MethodState>* methods) const;

 public:

  MethodFamily()
      : _selected_target(NULL), _exception_message(NULL), _exception_name(NULL) {}

  void set_target_if_empty(Method* m) {
    if (_selected_target == NULL && !m->is_overpass()) {
      _selected_target = m;
    }
  }

  void record_method(Method* m, QualifiedState state) {
    // If not in the set, add it.  If it's already in the set, then leave it
    // as is if state is qualified, or set it to disqualified if state is
    // disqualified.
    MethodState* method_state = find_method(m);
    if (method_state == NULL) {
      add_method(m, state);
    } else if (state == DISQUALIFIED) {
      method_state->_state = DISQUALIFIED;
    }
  }

  bool has_target() const { return _selected_target != NULL; }
  bool throws_exception() { return _exception_message != NULL; }

  Method* get_selected_target() { return _selected_target; }
  Symbol* get_exception_message() { return _exception_message; }
  Symbol* get_exception_name() { return _exception_name; }

  // Either sets the target or the exception error message
  void determine_target_or_set_exception_message(InstanceKlass* root) {
    if (has_target() || throws_exception()) {
      return;
    }

    // Qualified methods are maximally-specific methods
    // These include public, instance concrete (=default) and abstract methods
    int num_defaults = 0;
    int default_index = -1;
    for (int i = 0; i < _members.length(); i++) {
      MethodState &member = _members.at(i);
      if (member._state == QUALIFIED) {
        if (member._method->is_default_method()) {
          num_defaults++;
          default_index = i;
        }
      }
    }

    if (num_defaults == 1) {
      assert(_members.at(default_index)._state == QUALIFIED, "");
      _selected_target = _members.at(default_index)._method;
    } else {
      generate_and_set_exception_message(root, num_defaults, default_index);
    }
  }

  void generate_and_set_exception_message(InstanceKlass* root, int num_defaults, int default_index) {
    assert(num_defaults != 1, "invariant - should've been handled calling method");

    GrowableArray<Method*> qualified_methods;
    for (int i = 0; i < _members.length(); i++) {
      MethodState& member = _members.at(i);
      if (member._state == QUALIFIED) {
        qualified_methods.push(member._method);
      }
    }
    if (num_defaults == 0) {
      // If the root klass has a static method with matching name and signature
      // then do not generate an overpass method because it will hide the
      // static method during resolution.
      if (qualified_methods.length() == 0) {
        _exception_message = generate_no_defaults_message();
      } else {
        assert(root != NULL, "Null root class");
        _exception_message = generate_method_message(root->name(), qualified_methods.at(0));
      }
      _exception_name = vmSymbols::java_lang_AbstractMethodError();
    } else {
      _exception_message = generate_conflicts_message(&_members);
      _exception_name = vmSymbols::java_lang_IncompatibleClassChangeError();
      LogTarget(Debug, defaultmethods) lt;
      if (lt.is_enabled()) {
        LogStream ls(lt);
        _exception_message->print_value_on(&ls);
        ls.cr();
      }
    }
  }

  void print_selected(outputStream* str, int indent) const {
    assert(has_target(), "Should be called otherwise");
    streamIndentor si(str, indent * 2);
    str->indent().print("Selected method: ");
    print_method(str, _selected_target);
    Klass* method_holder = _selected_target->method_holder();
    if (!method_holder->is_interface()) {
      str->print(" : in superclass");
    }
    str->cr();
  }

  void print_exception(outputStream* str, int indent) {
    assert(throws_exception(), "Should be called otherwise");
    assert(_exception_name != NULL, "exception_name should be set");
    streamIndentor si(str, indent * 2);
    str->indent().print_cr("%s: %s", _exception_name->as_C_string(), _exception_message->as_C_string());
  }
};

Symbol* MethodFamily::generate_no_defaults_message() const {
  return SymbolTable::new_symbol("No qualifying defaults found");
}

Symbol* MethodFamily::generate_method_message(Symbol *klass_name, Method* method) const {
  stringStream ss;
  ss.print("Method ");
  Symbol* name = method->name();
  Symbol* signature = method->signature();
  ss.write((const char*)klass_name->bytes(), klass_name->utf8_length());
  ss.print(".");
  ss.write((const char*)name->bytes(), name->utf8_length());
  ss.write((const char*)signature->bytes(), signature->utf8_length());
  ss.print(" is abstract");
  return SymbolTable::new_symbol(ss.base(), (int)ss.size());
}

Symbol* MethodFamily::generate_conflicts_message(GrowableArray<MethodState>* methods) const {
  stringStream ss;
  ss.print("Conflicting default methods:");
  for (int i = 0; i < methods->length(); ++i) {
    Method *method = methods->at(i)._method;
    Symbol *klass = method->klass_name();
    Symbol *name = method->name();
    ss.print(" ");
    ss.write((const char*) klass->bytes(), klass->utf8_length());
    ss.print(".");
    ss.write((const char*) name->bytes(), name->utf8_length());
  }
  return SymbolTable::new_symbol(ss.base(), (int)ss.size());
}


class StateRestorerScope;

// StatefulMethodFamily is a wrapper around a MethodFamily that maintains the
// qualification state during hierarchy visitation, and applies that state
// when adding members to the MethodFamily
class StatefulMethodFamily : public ResourceObj {
  friend class StateRestorer;
 private:
  QualifiedState _qualification_state;

  void set_qualification_state(QualifiedState state) {
    _qualification_state = state;
  }

 protected:
  MethodFamily _method_family;

 public:
  StatefulMethodFamily() {
   _qualification_state = QUALIFIED;
  }

  void set_target_if_empty(Method* m) { _method_family.set_target_if_empty(m); }

  MethodFamily* get_method_family() { return &_method_family; }

  void record_method_and_dq_further(StateRestorerScope* scope, Method* mo);
};

// Because we use an iterative algorithm when iterating over the type
// hierarchy, we can't use traditional scoped objects which automatically do
// cleanup in the destructor when the scope is exited.  StateRestorerScope (and
// StateRestorer) provides a similar functionality, but for when you want a
// scoped object in non-stack memory (such as in resource memory, as we do
// here).  You've just got to remember to call 'restore_state()' on the scope when
// leaving it (and marks have to be explicitly added). The scope is reusable after
// 'restore_state()' has been called.
class StateRestorer : public ResourceObj {
 public:
  StatefulMethodFamily* _method;
  QualifiedState _state_to_restore;

  StateRestorer() : _method(NULL), _state_to_restore(DISQUALIFIED) {}

  void restore_state() { _method->set_qualification_state(_state_to_restore); }
};

class StateRestorerScope : public ResourceObj {
 private:
  GrowableArray<StateRestorer*>  _marks;
  GrowableArray<StateRestorer*>* _free_list; // Shared between scopes
 public:
  StateRestorerScope(GrowableArray<StateRestorer*>* free_list) : _marks(), _free_list(free_list) {}

  static StateRestorerScope* cast(void* data) {
    return static_cast<StateRestorerScope*>(data);
  }

  void mark(StatefulMethodFamily* family, QualifiedState qualification_state) {
    StateRestorer* restorer;
    if (!_free_list->is_empty()) {
      restorer = _free_list->pop();
    } else {
      restorer = new StateRestorer();
    }
    restorer->_method = family;
    restorer->_state_to_restore = qualification_state;
    _marks.append(restorer);
  }

#ifdef ASSERT
  bool is_empty() {
    return _marks.is_empty();
  }
#endif

  void restore_state() {
    while(!_marks.is_empty()) {
      StateRestorer* restorer = _marks.pop();
      restorer->restore_state();
      _free_list->push(restorer);
    }
  }
};

void StatefulMethodFamily::record_method_and_dq_further(StateRestorerScope* scope, Method* mo) {
  scope->mark(this, _qualification_state);
  _method_family.record_method(mo, _qualification_state);

  // Everything found "above"??? this method in the hierarchy walk is set to
  // disqualified
  set_qualification_state(DISQUALIFIED);
}

// Represents a location corresponding to a vtable slot for methods that
// neither the class nor any of it's ancestors provide an implementaion.
// Default methods may be present to fill this slot.
class EmptyVtableSlot : public ResourceObj {
 private:
  Symbol* _name;
  Symbol* _signature;
  int _size_of_parameters;
  MethodFamily* _binding;

 public:
  EmptyVtableSlot(Method* method)
      : _name(method->name()), _signature(method->signature()),
        _size_of_parameters(method->size_of_parameters()), _binding(NULL) {}

  Symbol* name() const { return _name; }
  Symbol* signature() const { return _signature; }
  int size_of_parameters() const { return _size_of_parameters; }

  void bind_family(MethodFamily* lm) { _binding = lm; }
  bool is_bound() { return _binding != NULL; }
  MethodFamily* get_binding() { return _binding; }

  void print_on(outputStream* str) const {
    print_slot(str, name(), signature());
  }
};

static bool already_in_vtable_slots(GrowableArray<EmptyVtableSlot*>* slots, Method* m) {
  bool found = false;
  for (int j = 0; j < slots->length(); ++j) {
    if (slots->at(j)->name() == m->name() &&
        slots->at(j)->signature() == m->signature() ) {
      found = true;
      break;
    }
  }
  return found;
}

static void find_empty_vtable_slots(GrowableArray<EmptyVtableSlot*>* slots,
    InstanceKlass* klass, const GrowableArray<Method*>* mirandas) {

  assert(klass != NULL, "Must be valid class");

  // All miranda methods are obvious candidates
  for (int i = 0; i < mirandas->length(); ++i) {
    Method* m = mirandas->at(i);
    if (!already_in_vtable_slots(slots, m)) {
      slots->append(new EmptyVtableSlot(m));
    }
  }

  // Also any overpasses in our superclasses, that we haven't implemented.
  // (can't use the vtable because it is not guaranteed to be initialized yet)
  InstanceKlass* super = klass->java_super();
  while (super != NULL) {
    for (int i = 0; i < super->methods()->length(); ++i) {
      Method* m = super->methods()->at(i);
      if (m->is_overpass() || m->is_static()) {
        // m is a method that would have been a miranda if not for the
        // default method processing that occurred on behalf of our superclass,
        // so it's a method we want to re-examine in this new context.  That is,
        // unless we have a real implementation of it in the current class.
        if (!already_in_vtable_slots(slots, m)) {
          Method *impl = klass->lookup_method(m->name(), m->signature());
          if (impl == NULL || impl->is_overpass() || impl->is_static()) {
            slots->append(new EmptyVtableSlot(m));
          }
        }
      }
    }

    // also any default methods in our superclasses
    if (super->default_methods() != NULL) {
      for (int i = 0; i < super->default_methods()->length(); ++i) {
        Method* m = super->default_methods()->at(i);
        // m is a method that would have been a miranda if not for the
        // default method processing that occurred on behalf of our superclass,
        // so it's a method we want to re-examine in this new context.  That is,
        // unless we have a real implementation of it in the current class.
        if (!already_in_vtable_slots(slots, m)) {
          Method* impl = klass->lookup_method(m->name(), m->signature());
          if (impl == NULL || impl->is_overpass() || impl->is_static()) {
            slots->append(new EmptyVtableSlot(m));
          }
        }
      }
    }
    super = super->java_super();
  }

  LogTarget(Debug, defaultmethods) lt;
  if (lt.is_enabled()) {
    lt.print("Slots that need filling:");
    ResourceMark rm;
    LogStream ls(lt);
    streamIndentor si(&ls);
    for (int i = 0; i < slots->length(); ++i) {
      ls.indent();
      slots->at(i)->print_on(&ls);
      ls.cr();
    }
  }
}

// Iterates over the superinterface type hierarchy looking for all methods
// with a specific erased signature.
class FindMethodsByErasedSig : public HierarchyVisitor<FindMethodsByErasedSig> {
 private:
  // Context data
  Symbol* _method_name;
  Symbol* _method_signature;
  StatefulMethodFamily*  _family;
  bool _cur_class_is_interface;
  // Free lists, used as an optimization
  GrowableArray<StateRestorerScope*> _free_scopes;
  GrowableArray<StateRestorer*> _free_restorers;
 public:
  FindMethodsByErasedSig() : _free_scopes(6), _free_restorers(6) {};

  void prepare(Symbol* name, Symbol* signature, bool is_interf) {
    reset();
    _method_name = name;
    _method_signature = signature;
    _family = NULL;
    _cur_class_is_interface = is_interf;
  }

  void get_discovered_family(MethodFamily** family) {
      if (_family != NULL) {
        *family = _family->get_method_family();
      } else {
        *family = NULL;
      }
  }

  void* new_node_data() {
    if (!_free_scopes.is_empty()) {
      StateRestorerScope* free_scope = _free_scopes.pop();
      assert(free_scope->is_empty(), "StateRestorerScope::_marks array not empty");
      return free_scope;
    }
    return new StateRestorerScope(&_free_restorers);
  }
  void free_node_data(void* node_data) {
    StateRestorerScope* scope =  StateRestorerScope::cast(node_data);
    scope->restore_state();
    // Reuse scopes
    _free_scopes.push(scope);
  }

  // Find all methods on this hierarchy that match this
  // method's erased (name, signature)
  bool visit() {
    StateRestorerScope* scope = StateRestorerScope::cast(current_data());
    InstanceKlass* iklass = current_class();

    Method* m = iklass->find_method(_method_name, _method_signature);
    // Private interface methods are not candidates for default methods.
    // invokespecial to private interface methods doesn't use default method logic.
    // Private class methods are not candidates for default methods.
    // Private methods do not override default methods, so need to perform
    // default method inheritance without including private methods.
    // The overpasses are your supertypes' errors, we do not include them.
    // Non-public methods in java.lang.Object are not candidates for default
    // methods.
    // Future: take access controls into account for superclass methods
    if (m != NULL && !m->is_static() && !m->is_overpass() && !m->is_private() &&
     (!_cur_class_is_interface || !SystemDictionary::is_nonpublic_Object_method(m))) {
      if (_family == NULL) {
        _family = new StatefulMethodFamily();
      }

      if (iklass->is_interface()) {
        _family->record_method_and_dq_further(scope, m);
      } else {
        // This is the rule that methods in classes "win" (bad word) over
        // methods in interfaces. This works because of single inheritance.
        // Private methods in classes do not "win", they will be found
        // first on searching, but overriding for invokevirtual needs
        // to find default method candidates for the same signature
        _family->set_target_if_empty(m);
      }
    }
    return true;
  }

};



static void create_defaults_and_exceptions(
    GrowableArray<EmptyVtableSlot*>* slots, InstanceKlass* klass, TRAPS);

static void generate_erased_defaults(
    FindMethodsByErasedSig* visitor,
    InstanceKlass* klass, EmptyVtableSlot* slot, bool is_intf) {

  // the visitor needs to be initialized or re-initialized before use
  // - this facilitates reusing the same visitor instance on multiple
  // generation passes as an optimization
  visitor->prepare(slot->name(), slot->signature(), is_intf);
  // sets up a set of methods with the same exact erased signature
  visitor->run(klass);

  MethodFamily* family;
  visitor->get_discovered_family(&family);
  if (family != NULL) {
    family->determine_target_or_set_exception_message(klass);
    slot->bind_family(family);
  }
}

static void merge_in_new_methods(InstanceKlass* klass,
    GrowableArray<Method*>* new_methods, TRAPS);
static void create_default_methods( InstanceKlass* klass,
    GrowableArray<Method*>* new_methods, TRAPS);

// This is the guts of the default methods implementation.  This is called just
// after the classfile has been parsed if some ancestor has default methods.
//
// First it finds any name/signature slots that need any implementation (either
// because they are miranda or a superclass's implementation is an overpass
// itself).  For each slot, iterate over the hierarchy, to see if they contain a
// signature that matches the slot we are looking at.
//
// For each slot filled, we either record the default method candidate in the
// klass default_methods list or, only to handle exception cases, we create an
// overpass method that throws an exception and add it to the klass methods list.
// The JVM does not create bridges nor handle generic signatures here.
void DefaultMethods::generate_default_methods(
    InstanceKlass* klass, const GrowableArray<Method*>* mirandas, TRAPS) {
  assert(klass != NULL, "invariant");
  assert(klass != vmClasses::Object_klass(), "Shouldn't be called for Object");

  // This resource mark is the bound for all memory allocation that takes
  // place during default method processing.  After this goes out of scope,
  // all (Resource) objects' memory will be reclaimed.  Be careful if adding an
  // embedded resource mark under here as that memory can't be used outside
  // whatever scope it's in.
  ResourceMark rm(THREAD);

  // Keep entire hierarchy alive for the duration of the computation
  constantPoolHandle cp(THREAD, klass->constants());
  KeepAliveRegistrar keepAlive(THREAD);
  KeepAliveVisitor loadKeepAlive(&keepAlive);
  loadKeepAlive.run(klass);

  LogTarget(Debug, defaultmethods) lt;
  if (lt.is_enabled()) {
    ResourceMark rm(THREAD);
    lt.print("%s %s requires default method processing",
             klass->is_interface() ? "Interface" : "Class",
             klass->name()->as_klass_external_name());
    LogStream ls(lt);
    PrintHierarchy printer(&ls);
    printer.run(klass);
  }

  GrowableArray<EmptyVtableSlot*> empty_slots;
  find_empty_vtable_slots(&empty_slots, klass, mirandas);

  if (empty_slots.length() > 0) {
    FindMethodsByErasedSig findMethodsByErasedSig;
    for (int i = 0; i < empty_slots.length(); ++i) {
      EmptyVtableSlot* slot = empty_slots.at(i);
      LogTarget(Debug, defaultmethods) lt;
      if (lt.is_enabled()) {
        LogStream ls(lt);
        streamIndentor si(&ls, 2);
        ls.indent().print("Looking for default methods for slot ");
        slot->print_on(&ls);
        ls.cr();
      }
      generate_erased_defaults(&findMethodsByErasedSig, klass, slot, klass->is_interface());
    }
    log_debug(defaultmethods)("Creating defaults and overpasses...");
    create_defaults_and_exceptions(&empty_slots, klass, CHECK);
  }
  log_debug(defaultmethods)("Default method processing complete");
}

static int assemble_method_error(
    BytecodeConstantPool* cp, BytecodeBuffer* buffer, Symbol* errorName, Symbol* message) {

  Symbol* init = vmSymbols::object_initializer_name();
  Symbol* sig = vmSymbols::string_void_signature();

  BytecodeAssembler assem(buffer, cp);

  assem._new(errorName);
  assem.dup();
  assem.load_string(message);
  assem.invokespecial(errorName, init, sig);
  assem.athrow();

  return 3; // max stack size: [ exception, exception, string ]
}

static Method* new_method(
    BytecodeConstantPool* cp, BytecodeBuffer* bytecodes, Symbol* name,
    Symbol* sig, AccessFlags flags, int max_stack, int params,
    ConstMethod::MethodType mt, TRAPS) {

  address code_start = 0;
  int code_length = 0;
  InlineTableSizes sizes;

  if (bytecodes != NULL && bytecodes->length() > 0) {
    code_start = static_cast<address>(bytecodes->adr_at(0));
    code_length = bytecodes->length();
  }

  Method* m = Method::allocate(cp->pool_holder()->class_loader_data(),
                               code_length, flags, &sizes,
                               mt, CHECK_NULL);

  m->set_constants(NULL); // This will get filled in later
  m->set_name_index(cp->utf8(name));
  m->set_signature_index(cp->utf8(sig));
  m->compute_from_signature(sig);
  m->set_size_of_parameters(params);
  m->set_max_stack(max_stack);
  m->set_max_locals(params);
  m->constMethod()->set_stackmap_data(NULL);
  m->set_code(code_start);

  return m;
}

static void switchover_constant_pool(BytecodeConstantPool* bpool,
    InstanceKlass* klass, GrowableArray<Method*>* new_methods, TRAPS) {

  if (new_methods->length() > 0) {
    ConstantPool* cp = bpool->create_constant_pool(CHECK);
    if (cp != klass->constants()) {
      // Copy resolved hidden class into new constant pool.
      if (klass->is_hidden()) {
        cp->klass_at_put(klass->this_class_index(), klass);
      }
      klass->class_loader_data()->add_to_deallocate_list(klass->constants());
      klass->set_constants(cp);
      cp->set_pool_holder(klass);

      for (int i = 0; i < new_methods->length(); ++i) {
        new_methods->at(i)->set_constants(cp);
      }
      for (int i = 0; i < klass->methods()->length(); ++i) {
        Method* mo = klass->methods()->at(i);
        mo->set_constants(cp);
      }
    }
  }
}

// Create default_methods list for the current class.
// With the VM only processing erased signatures, the VM only
// creates an overpass in a conflict case or a case with no candidates.
// This allows virtual methods to override the overpass, but ensures
// that a local method search will find the exception rather than an abstract
// or default method that is not a valid candidate.
//
// Note that if overpass method are ever created that are not exception
// throwing methods then the loader constraint checking logic for vtable and
// itable creation needs to be changed to check loader constraints for the
// overpass methods that do not throw exceptions.
static void create_defaults_and_exceptions(GrowableArray<EmptyVtableSlot*>* slots,
    InstanceKlass* klass, TRAPS) {

  GrowableArray<Method*> overpasses;
  GrowableArray<Method*> defaults;
  BytecodeConstantPool bpool(klass->constants());

  BytecodeBuffer* buffer = NULL; // Lazily create a reusable buffer
  for (int i = 0; i < slots->length(); ++i) {
    EmptyVtableSlot* slot = slots->at(i);

    if (slot->is_bound()) {
      MethodFamily* method = slot->get_binding();

      LogTarget(Debug, defaultmethods) lt;
      if (lt.is_enabled()) {
        ResourceMark rm(THREAD);
        LogStream ls(lt);
        ls.print("for slot: ");
        slot->print_on(&ls);
        ls.cr();
        if (method->has_target()) {
          method->print_selected(&ls, 1);
        } else if (method->throws_exception()) {
          method->print_exception(&ls, 1);
        }
      }

      if (method->has_target()) {
        Method* selected = method->get_selected_target();
        if (selected->method_holder()->is_interface()) {
          assert(!selected->is_private(), "pushing private interface method as default");
          defaults.push(selected);
        }
      } else if (method->throws_exception()) {
        if (buffer == NULL) {
          buffer = new BytecodeBuffer();
        } else {
          buffer->clear();
        }
        int max_stack = assemble_method_error(&bpool, buffer,
           method->get_exception_name(), method->get_exception_message());
        AccessFlags flags = accessFlags_from(
          JVM_ACC_PUBLIC | JVM_ACC_SYNTHETIC | JVM_ACC_BRIDGE);
        Method* m = new_method(&bpool, buffer, slot->name(), slot->signature(),
          flags, max_stack, slot->size_of_parameters(),
          ConstMethod::OVERPASS, CHECK);
        // We push to the methods list:
        // overpass methods which are exception throwing methods
        if (m != NULL) {
          overpasses.push(m);
        }
      }
    }
  }


  log_debug(defaultmethods)("Created %d overpass methods", overpasses.length());
  log_debug(defaultmethods)("Created %d default  methods", defaults.length());

  if (overpasses.length() > 0) {
    switchover_constant_pool(&bpool, klass, &overpasses, CHECK);
    merge_in_new_methods(klass, &overpasses, CHECK);
  }
  if (defaults.length() > 0) {
    create_default_methods(klass, &defaults, CHECK);
  }
}

static void create_default_methods(InstanceKlass* klass,
    GrowableArray<Method*>* new_methods, TRAPS) {

  int new_size = new_methods->length();
  Array<Method*>* total_default_methods = MetadataFactory::new_array<Method*>(
      klass->class_loader_data(), new_size, NULL, CHECK);
  for (int index = 0; index < new_size; index++ ) {
    total_default_methods->at_put(index, new_methods->at(index));
  }
  Method::sort_methods(total_default_methods, /*set_idnums=*/false);

  klass->set_default_methods(total_default_methods);
  // Create an array for mapping default methods to their vtable indices in
  // this class, since default methods vtable indices are the indices for
  // the defining class.
  klass->create_new_default_vtable_indices(new_size, CHECK);
}

static void sort_methods(GrowableArray<Method*>* methods) {
  // Note that this must sort using the same key as is used for sorting
  // methods in InstanceKlass.
  bool sorted = true;
  for (int i = methods->length() - 1; i > 0; --i) {
    for (int j = 0; j < i; ++j) {
      Method* m1 = methods->at(j);
      Method* m2 = methods->at(j + 1);
      if ((uintptr_t)m1->name() > (uintptr_t)m2->name()) {
        methods->at_put(j, m2);
        methods->at_put(j + 1, m1);
        sorted = false;
      }
    }
    if (sorted) break;
    sorted = true;
  }
#ifdef ASSERT
  uintptr_t prev = 0;
  for (int i = 0; i < methods->length(); ++i) {
    Method* mh = methods->at(i);
    uintptr_t nv = (uintptr_t)mh->name();
    assert(nv >= prev, "Incorrect overpass method ordering");
    prev = nv;
  }
#endif
}

static void merge_in_new_methods(InstanceKlass* klass,
    GrowableArray<Method*>* new_methods, TRAPS) {

  enum { ANNOTATIONS, PARAMETERS, DEFAULTS, NUM_ARRAYS };

  Array<Method*>* original_methods = klass->methods();
  Array<int>* original_ordering = klass->method_ordering();
  Array<int>* merged_ordering = Universe::the_empty_int_array();

  int new_size = klass->methods()->length() + new_methods->length();

  Array<Method*>* merged_methods = MetadataFactory::new_array<Method*>(
      klass->class_loader_data(), new_size, NULL, CHECK);

  // original_ordering might be empty if this class has no methods of its own
  if (JvmtiExport::can_maintain_original_method_order() || Arguments::is_dumping_archive()) {
    merged_ordering = MetadataFactory::new_array<int>(
        klass->class_loader_data(), new_size, CHECK);
  }
  int method_order_index = klass->methods()->length();

  sort_methods(new_methods);

  // Perform grand merge of existing methods and new methods
  int orig_idx = 0;
  int new_idx = 0;

  for (int i = 0; i < new_size; ++i) {
    Method* orig_method = NULL;
    Method* new_method = NULL;
    if (orig_idx < original_methods->length()) {
      orig_method = original_methods->at(orig_idx);
    }
    if (new_idx < new_methods->length()) {
      new_method = new_methods->at(new_idx);
    }

    if (orig_method != NULL &&
        (new_method == NULL || orig_method->name() < new_method->name())) {
      merged_methods->at_put(i, orig_method);
      original_methods->at_put(orig_idx, NULL);
      if (merged_ordering->length() > 0) {
        assert(original_ordering != NULL && original_ordering->length() > 0,
               "should have original order information for this method");
        merged_ordering->at_put(i, original_ordering->at(orig_idx));
      }
      ++orig_idx;
    } else {
      merged_methods->at_put(i, new_method);
      if (merged_ordering->length() > 0) {
        merged_ordering->at_put(i, method_order_index++);
      }
      ++new_idx;
    }
    // update idnum for new location
    merged_methods->at(i)->set_method_idnum(i);
    merged_methods->at(i)->set_orig_method_idnum(i);
  }

  // Verify correct order
#ifdef ASSERT
  uintptr_t prev = 0;
  for (int i = 0; i < merged_methods->length(); ++i) {
    Method* mo = merged_methods->at(i);
    uintptr_t nv = (uintptr_t)mo->name();
    assert(nv >= prev, "Incorrect method ordering");
    prev = nv;
  }
#endif

  // Replace klass methods with new merged lists
  klass->set_methods(merged_methods);
  klass->set_initial_method_idnum(new_size);
  klass->set_method_ordering(merged_ordering);

  // Free metadata
  ClassLoaderData* cld = klass->class_loader_data();
  if (original_methods->length() > 0) {
    MetadataFactory::free_array(cld, original_methods);
  }
  if (original_ordering != NULL && original_ordering->length() > 0) {
    MetadataFactory::free_array(cld, original_ordering);
  }
}
