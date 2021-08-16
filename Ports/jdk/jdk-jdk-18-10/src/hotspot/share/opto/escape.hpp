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

#ifndef SHARE_OPTO_ESCAPE_HPP
#define SHARE_OPTO_ESCAPE_HPP

#include "opto/addnode.hpp"
#include "opto/node.hpp"
#include "utilities/growableArray.hpp"

//
// Adaptation for C2 of the escape analysis algorithm described in:
//
// [Choi99] Jong-Deok Shoi, Manish Gupta, Mauricio Seffano,
//          Vugranam C. Sreedhar, Sam Midkiff,
//          "Escape Analysis for Java", Procedings of ACM SIGPLAN
//          OOPSLA  Conference, November 1, 1999
//
// The flow-insensitive analysis described in the paper has been implemented.
//
// The analysis requires construction of a "connection graph" (CG) for
// the method being analyzed.  The nodes of the connection graph are:
//
//     -  Java objects (JO)
//     -  Local variables (LV)
//     -  Fields of an object (OF),  these also include array elements
//
// The CG contains 3 types of edges:
//
//   -  PointsTo  (-P>)    {LV, OF} to JO
//   -  Deferred  (-D>)    from {LV, OF} to {LV, OF}
//   -  Field     (-F>)    from JO to OF
//
// The following  utility functions is used by the algorithm:
//
//   PointsTo(n) - n is any CG node, it returns the set of JO that n could
//                 point to.
//
// The algorithm describes how to construct the connection graph
// in the following 4 cases:
//
//          Case                  Edges Created
//
// (1)   p   = new T()              LV -P> JO
// (2)   p   = q                    LV -D> LV
// (3)   p.f = q                    JO -F> OF,  OF -D> LV
// (4)   p   = q.f                  JO -F> OF,  LV -D> OF
//
// In all these cases, p and q are local variables.  For static field
// references, we can construct a local variable containing a reference
// to the static memory.
//
// C2 does not have local variables.  However for the purposes of constructing
// the connection graph, the following IR nodes are treated as local variables:
//     Phi    (pointer values)
//     LoadP, LoadN
//     Proj#5 (value returned from call nodes including allocations)
//     CheckCastPP, CastPP
//
// The LoadP, Proj and CheckCastPP behave like variables assigned to only once.
// Only a Phi can have multiple assignments.  Each input to a Phi is treated
// as an assignment to it.
//
// The following node types are JavaObject:
//
//     phantom_object (general globally escaped object)
//     Allocate
//     AllocateArray
//     Parm  (for incoming arguments)
//     CastX2P ("unsafe" operations)
//     CreateEx
//     ConP
//     LoadKlass
//     ThreadLocal
//     CallStaticJava (which returns Object)
//
// AddP nodes are fields.
//
// After building the graph, a pass is made over the nodes, deleting deferred
// nodes and copying the edges from the target of the deferred edge to the
// source.  This results in a graph with no deferred edges, only:
//
//    LV -P> JO
//    OF -P> JO (the object whose oop is stored in the field)
//    JO -F> OF
//
// Then, for each node which is GlobalEscape, anything it could point to
// is marked GlobalEscape.  Finally, for any node marked ArgEscape, anything
// it could point to is marked ArgEscape.
//

class  Compile;
class  Node;
class  CallNode;
class  PhiNode;
class  PhaseTransform;
class  PointsToNode;
class  Type;
class  TypePtr;
class  VectorSet;

class JavaObjectNode;
class LocalVarNode;
class FieldNode;
class ArraycopyNode;

class ConnectionGraph;

// ConnectionGraph nodes
class PointsToNode : public ResourceObj {
  GrowableArray<PointsToNode*> _edges; // List of nodes this node points to
  GrowableArray<PointsToNode*> _uses;  // List of nodes which point to this node

  const u1           _type;  // NodeType
  u1                _flags;  // NodeFlags
  u1               _escape;  // EscapeState of object
  u1        _fields_escape;  // EscapeState of object's fields

  Node* const        _node;  // Ideal node corresponding to this PointsTo node.
  const int           _idx;  // Cached ideal node's _idx
  const uint         _pidx;  // Index of this node

public:
  typedef enum {
    UnknownType = 0,
    JavaObject  = 1,
    LocalVar    = 2,
    Field       = 3,
    Arraycopy   = 4
  } NodeType;

  typedef enum {
    UnknownEscape = 0,
    NoEscape      = 1, // An object does not escape method or thread and it is
                       // not passed to call. It could be replaced with scalar.
    ArgEscape     = 2, // An object does not escape method or thread but it is
                       // passed as argument to call or referenced by argument
                       // and it does not escape during call.
    GlobalEscape  = 3  // An object escapes the method or thread.
  } EscapeState;

  typedef enum {
    ScalarReplaceable = 1,  // Not escaped object could be replaced with scalar
    PointsToUnknown   = 2,  // Has edge to phantom_object
    ArraycopySrc      = 4,  // Has edge from Arraycopy node
    ArraycopyDst      = 8   // Has edge to Arraycopy node
  } NodeFlags;


  inline PointsToNode(ConnectionGraph* CG, Node* n, EscapeState es, NodeType type);

  uint        pidx()   const { return _pidx; }

  Node* ideal_node()   const { return _node; }
  int          idx()   const { return _idx; }

  bool is_JavaObject() const { return _type == (u1)JavaObject; }
  bool is_LocalVar()   const { return _type == (u1)LocalVar; }
  bool is_Field()      const { return _type == (u1)Field; }
  bool is_Arraycopy()  const { return _type == (u1)Arraycopy; }

  JavaObjectNode* as_JavaObject() { assert(is_JavaObject(),""); return (JavaObjectNode*)this; }
  LocalVarNode*   as_LocalVar()   { assert(is_LocalVar(),"");   return (LocalVarNode*)this; }
  FieldNode*      as_Field()      { assert(is_Field(),"");      return (FieldNode*)this; }
  ArraycopyNode*  as_Arraycopy()  { assert(is_Arraycopy(),"");  return (ArraycopyNode*)this; }

  EscapeState escape_state() const { return (EscapeState)_escape; }
  void    set_escape_state(EscapeState state) { _escape = (u1)state; }

  EscapeState fields_escape_state() const { return (EscapeState)_fields_escape; }
  void    set_fields_escape_state(EscapeState state) { _fields_escape = (u1)state; }

  bool     has_unknown_ptr() const { return (_flags & PointsToUnknown) != 0; }
  void set_has_unknown_ptr()       { _flags |= PointsToUnknown; }

  bool     arraycopy_src() const { return (_flags & ArraycopySrc) != 0; }
  void set_arraycopy_src()       { _flags |= ArraycopySrc; }
  bool     arraycopy_dst() const { return (_flags & ArraycopyDst) != 0; }
  void set_arraycopy_dst()       { _flags |= ArraycopyDst; }

  bool     scalar_replaceable() const { return (_flags & ScalarReplaceable) != 0;}
  void set_scalar_replaceable(bool set) {
    if (set) {
      _flags |= ScalarReplaceable;
    } else {
      _flags &= ~ScalarReplaceable;
    }
  }

  int edge_count()              const { return _edges.length(); }
  PointsToNode* edge(int e)     const { return _edges.at(e); }
  bool add_edge(PointsToNode* edge)   { return _edges.append_if_missing(edge); }

  int use_count()             const { return _uses.length(); }
  PointsToNode* use(int e)    const { return _uses.at(e); }
  bool add_use(PointsToNode* use)   { return _uses.append_if_missing(use); }

  // Mark base edge use to distinguish from stored value edge.
  bool add_base_use(FieldNode* use) { return _uses.append_if_missing((PointsToNode*)((intptr_t)use + 1)); }
  static bool is_base_use(PointsToNode* use) { return (((intptr_t)use) & 1); }
  static PointsToNode* get_use_node(PointsToNode* use) { return (PointsToNode*)(((intptr_t)use) & ~1); }

  // Return true if this node points to specified node or nodes it points to.
  bool points_to(JavaObjectNode* ptn) const;

  // Return true if this node points only to non-escaping allocations.
  bool non_escaping_allocation();

  // Return true if one node points to an other.
  bool meet(PointsToNode* ptn);

#ifndef PRODUCT
  NodeType node_type() const { return (NodeType)_type;}
  void dump(bool print_state=true) const;
#endif

};

class LocalVarNode: public PointsToNode {
public:
  LocalVarNode(ConnectionGraph *CG, Node* n, EscapeState es):
    PointsToNode(CG, n, es, LocalVar) {}
};

class JavaObjectNode: public PointsToNode {
public:
  JavaObjectNode(ConnectionGraph *CG, Node* n, EscapeState es):
    PointsToNode(CG, n, es, JavaObject) {
      if (es > NoEscape) {
        set_scalar_replaceable(false);
      }
    }
};

class FieldNode: public PointsToNode {
  GrowableArray<PointsToNode*> _bases; // List of JavaObject nodes which point to this node
  const int   _offset; // Field's offset.
  const bool  _is_oop; // Field points to object
        bool  _has_unknown_base; // Has phantom_object base
public:
  inline FieldNode(ConnectionGraph *CG, Node* n, EscapeState es, int offs, bool is_oop);

  int      offset()              const { return _offset;}
  bool     is_oop()              const { return _is_oop;}
  bool     has_unknown_base()    const { return _has_unknown_base; }
  void set_has_unknown_base()          { _has_unknown_base = true; }

  int base_count()              const { return _bases.length(); }
  PointsToNode* base(int e)     const { return _bases.at(e); }
  bool add_base(PointsToNode* base)    { return _bases.append_if_missing(base); }
#ifdef ASSERT
  // Return true if bases points to this java object.
  bool has_base(JavaObjectNode* ptn) const;
#endif

};

class ArraycopyNode: public PointsToNode {
public:
  ArraycopyNode(ConnectionGraph *CG, Node* n, EscapeState es):
    PointsToNode(CG, n, es, Arraycopy) {}
};

// Iterators for PointsTo node's edges:
//   for (EdgeIterator i(n); i.has_next(); i.next()) {
//     PointsToNode* u = i.get();
class PointsToIterator: public StackObj {
protected:
  const PointsToNode* node;
  const int cnt;
  int i;
public:
  inline PointsToIterator(const PointsToNode* n, int cnt) : node(n), cnt(cnt), i(0) { }
  inline bool has_next() const { return i < cnt; }
  inline void next() { i++; }
  PointsToNode* get() const { ShouldNotCallThis(); return NULL; }
};

class EdgeIterator: public PointsToIterator {
public:
  inline EdgeIterator(const PointsToNode* n) : PointsToIterator(n, n->edge_count()) { }
  inline PointsToNode* get() const { return node->edge(i); }
};

class UseIterator: public PointsToIterator {
public:
  inline UseIterator(const PointsToNode* n) : PointsToIterator(n, n->use_count()) { }
  inline PointsToNode* get() const { return node->use(i); }
};

class BaseIterator: public PointsToIterator {
public:
  inline BaseIterator(const FieldNode* n) : PointsToIterator(n, n->base_count()) { }
  inline PointsToNode* get() const { return ((PointsToNode*)node)->as_Field()->base(i); }
};


class ConnectionGraph: public ResourceObj {
  friend class PointsToNode; // to access _compile
  friend class FieldNode;
private:
  GrowableArray<PointsToNode*>  _nodes; // Map from ideal nodes to
                                        // ConnectionGraph nodes.

  GrowableArray<PointsToNode*>  _worklist; // Nodes to be processed
  VectorSet                  _in_worklist;
  uint                         _next_pidx;

  bool            _collecting; // Indicates whether escape information
                               // is still being collected. If false,
                               // no new nodes will be processed.

  bool               _verify;  // verify graph

  JavaObjectNode*    null_obj;

  Compile*           _compile; // Compile object for current compilation
  PhaseIterGVN*         _igvn; // Value numbering

  Unique_Node_List ideal_nodes; // Used by CG construction and types splitting.

  int        _build_iterations; // Number of iterations took to build graph
  double           _build_time; // Time (sec) took to build graph

public:
  JavaObjectNode* phantom_obj; // Unknown object

private:
  // Address of an element in _nodes.  Used when the element is to be modified
  PointsToNode* ptnode_adr(int idx) const {
    // There should be no new ideal nodes during ConnectionGraph build,
    // growableArray::at() will throw assert otherwise.
    return _nodes.at(idx);
  }
  uint nodes_size() const { return _nodes.length(); }

  uint next_pidx() { return _next_pidx++; }

  // Add nodes to ConnectionGraph.
  void add_local_var(Node* n, PointsToNode::EscapeState es);
  void add_java_object(Node* n, PointsToNode::EscapeState es);
  void add_field(Node* n, PointsToNode::EscapeState es, int offset);
  void add_arraycopy(Node* n, PointsToNode::EscapeState es, PointsToNode* src, PointsToNode* dst);

  // Compute the escape state for arguments to a call.
  void process_call_arguments(CallNode *call);

  // Add PointsToNode node corresponding to a call
  void add_call_node(CallNode* call);

  // Create PointsToNode node and add it to Connection Graph.
  void add_node_to_connection_graph(Node *n, Unique_Node_List *delayed_worklist);

  // Add final simple edges to graph.
  void add_final_edges(Node *n);

  // Finish Graph construction.
  bool complete_connection_graph(GrowableArray<PointsToNode*>&   ptnodes_worklist,
                                 GrowableArray<JavaObjectNode*>& non_escaped_worklist,
                                 GrowableArray<JavaObjectNode*>& java_objects_worklist,
                                 GrowableArray<FieldNode*>&      oop_fields_worklist);

#ifdef ASSERT
  void verify_connection_graph(GrowableArray<PointsToNode*>&   ptnodes_worklist,
                               GrowableArray<JavaObjectNode*>& non_escaped_worklist,
                               GrowableArray<JavaObjectNode*>& java_objects_worklist,
                               GrowableArray<Node*>& addp_worklist);
#endif

  // Add all references to this JavaObject node.
  int add_java_object_edges(JavaObjectNode* jobj, bool populate_worklist);

  // Put node on worklist if it is (or was) not there.
  inline void add_to_worklist(PointsToNode* pt) {
    PointsToNode* ptf = pt;
    uint pidx_bias = 0;
    if (PointsToNode::is_base_use(pt)) {
      // Create a separate entry in _in_worklist for a marked base edge
      // because _worklist may have an entry for a normal edge pointing
      // to the same node. To separate them use _next_pidx as bias.
      ptf = PointsToNode::get_use_node(pt)->as_Field();
      pidx_bias = _next_pidx;
    }
    if (!_in_worklist.test_set(ptf->pidx() + pidx_bias)) {
      _worklist.append(pt);
    }
  }

  // Put on worklist all uses of this node.
  inline void add_uses_to_worklist(PointsToNode* pt) {
    for (UseIterator i(pt); i.has_next(); i.next()) {
      add_to_worklist(i.get());
    }
  }

  // Put on worklist all field's uses and related field nodes.
  void add_field_uses_to_worklist(FieldNode* field);

  // Put on worklist all related field nodes.
  void add_fields_to_worklist(FieldNode* field, PointsToNode* base);

  // Find fields which have unknown value.
  int find_field_value(FieldNode* field);

  // Find fields initializing values for allocations.
  int find_init_values_null   (JavaObjectNode* ptn, PhaseTransform* phase);
  int find_init_values_phantom(JavaObjectNode* ptn);

  // Set the escape state of an object and its fields.
  void set_escape_state(PointsToNode* ptn, PointsToNode::EscapeState esc) {
    // Don't change non-escaping state of NULL pointer.
    if (ptn != null_obj) {
      if (ptn->escape_state() < esc) {
        ptn->set_escape_state(esc);
      }
      if (ptn->fields_escape_state() < esc) {
        ptn->set_fields_escape_state(esc);
      }
    }
  }
  void set_fields_escape_state(PointsToNode* ptn, PointsToNode::EscapeState esc) {
    // Don't change non-escaping state of NULL pointer.
    if (ptn != null_obj) {
      if (ptn->fields_escape_state() < esc) {
        ptn->set_fields_escape_state(esc);
      }
    }
  }

  // Propagate GlobalEscape and ArgEscape escape states to all nodes
  // and check that we still have non-escaping java objects.
  bool find_non_escaped_objects(GrowableArray<PointsToNode*>& ptnodes_worklist,
                                GrowableArray<JavaObjectNode*>& non_escaped_worklist);

  // Adjust scalar_replaceable state after Connection Graph is built.
  void adjust_scalar_replaceable_state(JavaObjectNode* jobj);

  // Optimize ideal graph.
  void optimize_ideal_graph(GrowableArray<Node*>& ptr_cmp_worklist,
                            GrowableArray<Node*>& storestore_worklist);
  // Optimize objects compare.
  const TypeInt* optimize_ptr_compare(Node* n);

  // Returns unique corresponding java object or NULL.
  JavaObjectNode* unique_java_object(Node *n);

  // Add an edge of the specified type pointing to the specified target.
  bool add_edge(PointsToNode* from, PointsToNode* to) {
    assert(!from->is_Field() || from->as_Field()->is_oop(), "sanity");

    if (to == phantom_obj) {
      if (from->has_unknown_ptr()) {
        return false; // already points to phantom_obj
      }
      from->set_has_unknown_ptr();
    }

    bool is_new = from->add_edge(to);
    assert(to != phantom_obj || is_new, "sanity");
    if (is_new) { // New edge?
      assert(!_verify, "graph is incomplete");
      is_new = to->add_use(from);
      assert(is_new, "use should be also new");
    }
    return is_new;
  }

  // Add an edge from Field node to its base and back.
  bool add_base(FieldNode* from, PointsToNode* to) {
    assert(!to->is_Arraycopy(), "sanity");
    if (to == phantom_obj) {
      if (from->has_unknown_base()) {
        return false; // already has phantom_obj base
      }
      from->set_has_unknown_base();
    }
    bool is_new = from->add_base(to);
    assert(to != phantom_obj || is_new, "sanity");
    if (is_new) {      // New edge?
      assert(!_verify, "graph is incomplete");
      if (to == null_obj) {
        return is_new; // Don't add fields to NULL pointer.
      }
      if (to->is_JavaObject()) {
        is_new = to->add_edge(from);
      } else {
        is_new = to->add_base_use(from);
      }
      assert(is_new, "use should be also new");
    }
    return is_new;
  }

  // Helper functions
  bool   is_oop_field(Node* n, int offset, bool* unsafe);
  static Node* find_second_addp(Node* addp, Node* n);
  // offset of a field reference
  int address_offset(Node* adr, PhaseTransform *phase);

  bool is_captured_store_address(Node* addp);

  // Propagate unique types created for non-escaped allocated objects through the graph
  void split_unique_types(GrowableArray<Node *>  &alloc_worklist, GrowableArray<ArrayCopyNode*> &arraycopy_worklist);

  // Helper methods for unique types split.
  bool split_AddP(Node *addp, Node *base);

  PhiNode *create_split_phi(PhiNode *orig_phi, int alias_idx, GrowableArray<PhiNode *>  &orig_phi_worklist, bool &new_created);
  PhiNode *split_memory_phi(PhiNode *orig_phi, int alias_idx, GrowableArray<PhiNode *>  &orig_phi_worklist);

  void  move_inst_mem(Node* n, GrowableArray<PhiNode *>  &orig_phis);
  Node* find_inst_mem(Node* mem, int alias_idx,GrowableArray<PhiNode *>  &orig_phi_worklist);
  Node* step_through_mergemem(MergeMemNode *mmem, int alias_idx, const TypeOopPtr *toop);


  GrowableArray<MergeMemNode*>  _mergemem_worklist; // List of all MergeMem nodes

  Node_Array _node_map; // used for bookkeeping during type splitting
                        // Used for the following purposes:
                        // Memory Phi    - most recent unique Phi split out
                        //                 from this Phi
                        // MemNode       - new memory input for this node
                        // ChecCastPP    - allocation that this is a cast of
                        // allocation    - CheckCastPP of the allocation

  // manage entries in _node_map

  void  set_map(Node* from, Node* to)  {
    ideal_nodes.push(from);
    _node_map.map(from->_idx, to);
  }

  Node* get_map(int idx) { return _node_map[idx]; }

  PhiNode* get_map_phi(int idx) {
    Node* phi = _node_map[idx];
    return (phi == NULL) ? NULL : phi->as_Phi();
  }

  // Returns true if there is an object in the scope of sfn that does not escape globally.
  bool has_ea_local_in_scope(SafePointNode* sfn);

  bool has_arg_escape(CallJavaNode* call);

  // Notify optimizer that a node has been modified
  void record_for_optimizer(Node *n);

  // Compute the escape information
  bool compute_escape();

public:
  ConnectionGraph(Compile *C, PhaseIterGVN *igvn);

  // Check for non-escaping candidates
  static bool has_candidates(Compile *C);

  // Perform escape analysis
  static void do_analysis(Compile *C, PhaseIterGVN *igvn);

  bool not_global_escape(Node *n);

  // To be used by, e.g., BarrierSetC2 impls
  Node* get_addp_base(Node* addp);

  // Utility function for nodes that load an object
  void add_objload_to_connection_graph(Node* n, Unique_Node_List* delayed_worklist);

  // Add LocalVar node and edge if possible
  void add_local_var_and_edge(Node* n, PointsToNode::EscapeState es, Node* to,
                              Unique_Node_List *delayed_worklist) {
    PointsToNode* ptn = ptnode_adr(to->_idx);
    if (delayed_worklist != NULL) { // First iteration of CG construction
      add_local_var(n, es);
      if (ptn == NULL) {
        delayed_worklist->push(n);
        return; // Process it later.
      }
    } else {
      assert(ptn != NULL, "node should be registered");
    }
    add_edge(ptnode_adr(n->_idx), ptn);
  }

  // Map ideal node to existing PointsTo node (usually phantom_object).
  void map_ideal_node(Node *n, PointsToNode* ptn) {
    assert(ptn != NULL, "only existing PointsTo node");
    _nodes.at_put(n->_idx, ptn);
  }

  void add_to_congraph_unsafe_access(Node* n, uint opcode, Unique_Node_List* delayed_worklist);
  bool add_final_edges_unsafe_access(Node* n, uint opcode);

#ifndef PRODUCT
  void dump(GrowableArray<PointsToNode*>& ptnodes_worklist);
#endif
};

inline PointsToNode::PointsToNode(ConnectionGraph *CG, Node* n, EscapeState es, NodeType type):
  _edges(CG->_compile->comp_arena(), 2, 0, NULL),
  _uses (CG->_compile->comp_arena(), 2, 0, NULL),
  _type((u1)type),
  _flags(ScalarReplaceable),
  _escape((u1)es),
  _fields_escape((u1)es),
  _node(n),
  _idx(n->_idx),
  _pidx(CG->next_pidx()) {
  assert(n != NULL && es != UnknownEscape, "sanity");
}

inline FieldNode::FieldNode(ConnectionGraph *CG, Node* n, EscapeState es, int offs, bool is_oop):
  PointsToNode(CG, n, es, Field),
  _bases(CG->_compile->comp_arena(), 2, 0, NULL),
  _offset(offs), _is_oop(is_oop),
  _has_unknown_base(false) {
}

#endif // SHARE_OPTO_ESCAPE_HPP
