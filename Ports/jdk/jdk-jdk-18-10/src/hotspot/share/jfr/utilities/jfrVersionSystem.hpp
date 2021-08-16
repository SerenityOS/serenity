/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRVERSIONSYSTEM_HPP
#define SHARE_JFR_UTILITIES_JFRVERSIONSYSTEM_HPP

/*
 * A lock-free data structure usually require support for tracking references
 * in the service of Safe Memory Reclamation (SMR). JfrVersionSystem provides local,
 * compared to global, reference tracking for an assoicated data structure.
 *
 * A client, before accessing a structure, will perform a "checkout" from the JfrVersionSystem.
 * This checkout is associated with the current, or latest, version, analogous to the "tip" in a version control system.
 * When a client is done it releases its checkout, by which the JfrVersionSystem is notified that the client
 * is no longer an active user of the associated structure.
 *
 * If a client performs a modification, it will register this with the JfrVersionSystem, by means of incrementing the current version.
 *
 * To guarantee safe memory reclamation, say before attempting a delete, a client will use the JfrVersionSystem to
 * check for potential active uses, i.e. checkouts, with versions earlier than the version associated with a specific modification.
 *
 * Let's exemplify this with the removal of a node from a linked-list:
 *
 * 1. Before accessing the list, the client will check out the latest version from the JfrVersionSystem. This access is now tracked.
 * 2. The client finds a node it wants to use, and excises the node from the list.
 * 3. This excision is a modification so the client will increment the current version.
 * 4. Before it can start to use proper the node just excised, the client must ensure no possible references exist.
 * 5. To do this, the client inspects the JfrVersionSystem to possibly await the release of existing checkouts,
 *    i.e. for versions less than the version associated with the modification.
 * 6. On return, the client is guaranteed exclusive access to the excised node.
 *
 * Tracking the version of a structure is conceptually similar to tracking a representative pointer using Hazard Pointers,
 * or by using a global counter or some kind of ticket system.
 *
 * The implementation was inspired by Andrei Alexandrescu and Maged Michael, Lock-Free Data Structures with Hazard Pointers
 * https ://www.drdobbs.com/lock-free-data-structures-with-hazard-po/184401890
 */

#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrRefCountPointer.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "memory/padded.hpp"

class JfrVersionSystem : public JfrCHeapObj {
 public:
  typedef traceid Type;
 private:
  class Node : public JfrCHeapObj {
    friend class JfrVersionSystem;
    template <typename>
    friend class RefCountHandle;
   private:
    JfrVersionSystem* const _system;
    Node* _next;
    mutable Type _version;
    SingleThreadedRefCounter _ref_counter;
    mutable bool _live;
    Node(JfrVersionSystem* system);
    void add_ref() const;
    void remove_ref() const;
    Type version() const;
    void set(Type version) const;
   public:
    void checkout();
    void commit();
    const Node* operator->() const { return this; }
    Node* operator->() { return this; }
  };
  typedef Node* NodePtr;

 public:
  JfrVersionSystem();
  ~JfrVersionSystem();
  void reset();

  typedef RefCountHandle<Node> Handle;
  Handle get();

 private:
  NodePtr acquire();
  void await(Type version);
  Type tip() const;
  Type inc_tip();
  NodePtr synchronize_with(Type version, NodePtr last) const;
  DEBUG_ONLY(void assert_state(const Node* node) const;)
  struct PaddedTip {
    DEFINE_PAD_MINUS_SIZE(0, DEFAULT_CACHE_LINE_SIZE, 0);
    volatile Type _value;
    DEFINE_PAD_MINUS_SIZE(1, DEFAULT_CACHE_LINE_SIZE, sizeof(volatile Type));
  };
  PaddedTip _tip;
  NodePtr _head;
};

#endif // SHARE_JFR_UTILITIES_JFRVERSIONSYSTEM_HPP
