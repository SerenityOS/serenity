/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_LINKEDLIST_HPP
#define SHARE_UTILITIES_LINKEDLIST_HPP

#include "memory/allocation.hpp"

/*
 * The implementation of a generic linked list, which uses various
 * backing storages, such as C heap, arena and resource, etc.
 */


// An entry in a linked list. It should use the same backing storage
// as the linked list that contains this entry.
template <class E> class LinkedListNode : public ResourceObj {
 private:
  E                       _data;  // embedded content
  LinkedListNode<E>*      _next;  // next entry

  // Select member function 'bool U::equals(const U&) const' if 'U' is of class
  // type. This works because of the "Substitution Failure Is Not An Error"
  // (SFINAE) rule. Notice that this version of 'equal' will also be chosen for
  // class types which don't define a corresponding 'equals()' method (and will
  // result in a compilation error for them). It is not easily possible to
  // specialize this 'equal()' function exclusively for class types which define
  // the correct 'equals()' function because that function can be in a base
  // class, a dependent base class or have a compatible but slightly different
  // signature.
  template <class U>
  static bool equal(const U& a, const U& b, bool (U::*t)(const U&) const) {
    return a.equals(b);
  }

  template <class U>
  static bool equal(const U& a, const U& b, ...) {
    return a == b;
  }

 protected:
  LinkedListNode() : _next(NULL) { }

 public:
  LinkedListNode(const E& e): _data(e), _next(NULL) { }

  inline void set_next(LinkedListNode<E>* node) { _next = node; }
  inline LinkedListNode<E> * next() const       { return _next; }

  E*  data() { return &_data; }
  const E* peek() const { return &_data; }

  bool equals(const E& t) const {
    return equal<E>(_data, t, NULL);
  }
};

// A linked list interface. It does not specify
// any storage type it uses, so all methods involving
// memory allocation or deallocation are pure virtual
template <class E> class LinkedList : public ResourceObj {
 protected:
  LinkedListNode<E>*    _head;
  NONCOPYABLE(LinkedList<E>);

 public:
  LinkedList() : _head(NULL) { }
  virtual ~LinkedList() {}

  inline void set_head(LinkedListNode<E>* h) { _head = h; }
  inline LinkedListNode<E>* head() const     { return _head; }
  inline bool is_empty()           const     { return head() == NULL; }

  inline size_t size() const {
    LinkedListNode<E>* p;
    size_t count = 0;
    for (p = head(); p != NULL; count++, p = p->next());
    return count;
 }

  // Move all entries from specified linked list to this one
  virtual void move(LinkedList<E>* list) = 0;

  // Add an entry to this linked list
  virtual LinkedListNode<E>* add(const E& e) = 0;
  // Add all entries from specified linked list to this one,
  virtual void add(LinkedListNode<E>* node) = 0;

  // Add a linked list to this linked list
  virtual bool  add(const LinkedList<E>* list) = 0;

  // Search entry in the linked list
  virtual LinkedListNode<E>* find_node(const E& e) = 0;
  virtual E* find(const E& e) = 0;

  // Insert entry to the linked list
  virtual LinkedListNode<E>* insert_before(const E& e, LinkedListNode<E>* ref) = 0;
  virtual LinkedListNode<E>* insert_after (const E& e, LinkedListNode<E>* ref) = 0;

  // Remove entry from the linked list
  virtual bool               remove(const E& e) = 0;
  virtual bool               remove(LinkedListNode<E>* node) = 0;
  virtual bool               remove_before(LinkedListNode<E>* ref) = 0;
  virtual bool               remove_after(LinkedListNode<E>*  ref) = 0;

  LinkedListNode<E>* unlink_head() {
    LinkedListNode<E>* h = this->head();
    if (h != NULL) {
      this->set_head(h->next());
    }
    return h;
  }

  DEBUG_ONLY(virtual ResourceObj::allocation_type storage_type() = 0;)
};

// A linked list implementation.
// The linked list can be allocated in various type of memory: C heap, arena and resource area, etc.
template <class E, ResourceObj::allocation_type T = ResourceObj::C_HEAP,
  MEMFLAGS F = mtNMT, AllocFailType alloc_failmode = AllocFailStrategy::RETURN_NULL>
  class LinkedListImpl : public LinkedList<E> {
 protected:
  Arena*                 _arena;
 public:
  LinkedListImpl() :  _arena(NULL) { }
  LinkedListImpl(Arena* a) : _arena(a) { }

  virtual ~LinkedListImpl() {
    clear();
  }

  virtual void clear() {
    LinkedListNode<E>* p = this->head();
    this->set_head(NULL);
    while (p != NULL) {
      LinkedListNode<E>* to_delete = p;
      p = p->next();
      delete_node(to_delete);
    }
  }

  // Add an entry to the linked list
  virtual LinkedListNode<E>* add(const E& e)  {
    LinkedListNode<E>* node = this->new_node(e);
    if (node != NULL) {
      this->add(node);
    }

    return node;
  }

  virtual void add(LinkedListNode<E>* node) {
    assert(node != NULL, "NULL pointer");
    node->set_next(this->head());
    this->set_head(node);
  }

  // Move a linked list to this linked list, both have to be allocated on the same
  // storage type.
  virtual void move(LinkedList<E>* list) {
    assert(list->storage_type() == this->storage_type(), "Different storage type");
    LinkedListNode<E>* node = this->head();
    while (node != NULL && node->next() != NULL) {
      node = node->next();
    }
    if (node == NULL) {
      this->set_head(list->head());
    } else {
      node->set_next(list->head());
    }
    // All entries are moved
    list->set_head(NULL);
  }

  virtual bool add(const LinkedList<E>* list) {
    LinkedListNode<E>* node = list->head();
    while (node != NULL) {
      if (this->add(*node->peek()) == NULL) {
        return false;
      }
      node = node->next();
    }
    return true;
  }


  virtual LinkedListNode<E>* find_node(const E& e) {
    LinkedListNode<E>* p = this->head();
    while (p != NULL && !p->equals(e)) {
      p = p->next();
    }
    return p;
  }

  E* find(const E& e) {
    LinkedListNode<E>* node = find_node(e);
    return (node == NULL) ? NULL : node->data();
  }


  // Add an entry in front of the reference entry
  LinkedListNode<E>* insert_before(const E& e, LinkedListNode<E>* ref_node) {
    LinkedListNode<E>* node = this->new_node(e);
    if (node == NULL) return NULL;
    if (ref_node == this->head()) {
      node->set_next(ref_node);
      this->set_head(node);
    } else {
      LinkedListNode<E>* p = this->head();
      while (p != NULL && p->next() != ref_node) {
        p = p->next();
      }
      assert(p != NULL, "ref_node not in the list");
      node->set_next(ref_node);
      p->set_next(node);
    }
    return node;
  }

   // Add an entry behind the reference entry
   LinkedListNode<E>* insert_after(const E& e, LinkedListNode<E>* ref_node) {
     LinkedListNode<E>* node = this->new_node(e);
     if (node == NULL) return NULL;
     node->set_next(ref_node->next());
     ref_node->set_next(node);
     return node;
   }

   // Remove an entry from the linked list.
   // Return true if the entry is successfully removed
   virtual bool remove(const E& e) {
     LinkedListNode<E>* tmp = this->head();
     LinkedListNode<E>* prev = NULL;

     while (tmp != NULL) {
       if (tmp->equals(e)) {
         return remove_after(prev);
       }
       prev = tmp;
       tmp = tmp->next();
     }
     return false;
  }

  // Remove the node after the reference entry
  virtual bool remove_after(LinkedListNode<E>* prev) {
    LinkedListNode<E>* to_delete;
    if (prev == NULL) {
      to_delete = this->unlink_head();
    } else {
      to_delete = prev->next();
      if (to_delete != NULL) {
        prev->set_next(to_delete->next());
      }
    }

    if (to_delete != NULL) {
      delete_node(to_delete);
      return true;
    }
    return false;
  }

  virtual bool remove(LinkedListNode<E>* node) {
    LinkedListNode<E>* p = this->head();
    if (p == node) {
      this->set_head(p->next());
      delete_node(node);
      return true;
    }
    while (p != NULL && p->next() != node) {
      p = p->next();
    }
    if (p != NULL) {
      p->set_next(node->next());
      delete_node(node);
      return true;
    } else {
      return false;
    }
  }

  virtual bool remove_before(LinkedListNode<E>* ref) {
    assert(ref != NULL, "NULL pointer");
    LinkedListNode<E>* p = this->head();
    LinkedListNode<E>* to_delete = NULL; // to be deleted
    LinkedListNode<E>* prev = NULL;      // node before the node to be deleted
    while (p != NULL && p != ref) {
      prev = to_delete;
      to_delete = p;
      p = p->next();
    }
    if (p == NULL || to_delete == NULL) return false;
    assert(to_delete->next() == ref, "Wrong node to delete");
    assert(prev == NULL || prev->next() == to_delete,
      "Sanity check");
    if (prev == NULL) {
      assert(to_delete == this->head(), "Must be head");
      this->set_head(to_delete->next());
    } else {
      prev->set_next(to_delete->next());
    }
    delete_node(to_delete);
    return true;
  }

  DEBUG_ONLY(ResourceObj::allocation_type storage_type() { return T; })
 protected:
  // Create new linked list node object in specified storage
  LinkedListNode<E>* new_node(const E& e) const {
     switch(T) {
       case ResourceObj::ARENA: {
         assert(_arena != NULL, "Arena not set");
         return new(_arena) LinkedListNode<E>(e);
       }
       case ResourceObj::RESOURCE_AREA:
       case ResourceObj::C_HEAP: {
         if (alloc_failmode == AllocFailStrategy::RETURN_NULL) {
           return new(std::nothrow, T, F) LinkedListNode<E>(e);
         } else {
           return new(T, F) LinkedListNode<E>(e);
         }
       }
       default:
         ShouldNotReachHere();
     }
     return NULL;
  }

  // Delete linked list node object
  void delete_node(LinkedListNode<E>* node) {
    if (T == ResourceObj::C_HEAP) {
      delete node;
    }
  }
};

// Sorted linked list. The linked list maintains sorting order specified by the comparison
// function
template <class E, int (*FUNC)(const E&, const E&),
  ResourceObj::allocation_type T = ResourceObj::C_HEAP,
  MEMFLAGS F = mtNMT, AllocFailType alloc_failmode = AllocFailStrategy::RETURN_NULL>
  class SortedLinkedList : public LinkedListImpl<E, T, F, alloc_failmode> {
 public:
  SortedLinkedList() { }
  SortedLinkedList(Arena* a) : LinkedListImpl<E, T, F, alloc_failmode>(a) { }

  virtual LinkedListNode<E>* add(const E& e) {
    return LinkedListImpl<E, T, F, alloc_failmode>::add(e);
  }

  virtual void move(LinkedList<E>* list) {
    assert(list->storage_type() == this->storage_type(), "Different storage type");
    LinkedListNode<E>* node;
    while ((node = list->unlink_head()) != NULL) {
      this->add(node);
    }
    assert(list->is_empty(), "All entries are moved");
  }

  virtual void add(LinkedListNode<E>* node) {
    assert(node != NULL, "NULL pointer");
    LinkedListNode<E>* tmp = this->head();
    LinkedListNode<E>* prev = NULL;

    int cmp_val;
    while (tmp != NULL) {
      cmp_val = FUNC(*tmp->peek(), *node->peek());
      if (cmp_val >= 0) {
        break;
      }
      prev = tmp;
      tmp = tmp->next();
    }

    if (prev != NULL) {
      node->set_next(prev->next());
      prev->set_next(node);
    } else {
      node->set_next(this->head());
      this->set_head(node);
    }
  }

  virtual bool add(const LinkedList<E>* list) {
    return LinkedListImpl<E, T, F, alloc_failmode>::add(list);
  }

  virtual LinkedListNode<E>* find_node(const E& e) {
    LinkedListNode<E>* p = this->head();

    while (p != NULL) {
      int comp_val = FUNC(*p->peek(), e);
      if (comp_val == 0) {
        return p;
      } else if (comp_val > 0) {
        return NULL;
      }
      p = p->next();
    }
    return NULL;
  }
};

// Iterates all entries in the list
template <class E> class LinkedListIterator : public StackObj {
 private:
  mutable LinkedListNode<E>* _p;

 public:
  LinkedListIterator(LinkedListNode<E>* head) : _p(head) {}

  bool is_empty() const { return _p == NULL; }

  E* next() {
    if (_p == NULL) return NULL;
    E* e = _p->data();
    _p = _p->next();
    return e;
  }

  const E* next() const {
    if (_p == NULL) return NULL;
    const E* e = _p->peek();
    _p = _p->next();
    return e;
  }
};

#endif // SHARE_UTILITIES_LINKEDLIST_HPP
