/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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

#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/classLoaderHierarchyDCmd.hpp"
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/safepoint.hpp"
#include "oops/reflectionAccessorImplKlassHelper.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"


ClassLoaderHierarchyDCmd::ClassLoaderHierarchyDCmd(outputStream* output, bool heap)
  : DCmdWithParser(output, heap),
   _show_classes("show-classes", "Print loaded classes.", "BOOLEAN", false, "false"),
  _verbose("verbose", "Print detailed information.", "BOOLEAN", false, "false"),
  _fold("fold", "Show loaders of the same name and class as one.", "BOOLEAN", true, "true") {
  _dcmdparser.add_dcmd_option(&_show_classes);
  _dcmdparser.add_dcmd_option(&_verbose);
  _dcmdparser.add_dcmd_option(&_fold);
}


int ClassLoaderHierarchyDCmd::num_arguments() {
  ResourceMark rm;
  ClassLoaderHierarchyDCmd* dcmd = new ClassLoaderHierarchyDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

// Helper class for drawing the branches to the left of a node.
class BranchTracker : public StackObj {
  //       "<x>"
  //       " |---<y>"
  //       " |    |
  //       " |   <z>"
  //       " |    |---<z1>
  //       " |    |---<z2>
  //       ^^^^^^^ ^^^
  //        A       B

  // Some terms for the graphics:
  // - branch: vertical connection between a node's ancestor to a later sibling.
  // - branchwork: (A) the string to print as a prefix at the start of each line, contains all branches.
  // - twig (B): Length of the dashed line connecting a node to its branch.
  // - branch spacing: how many spaces between branches are printed.

public:

  enum { max_depth = 64, twig_len = 2, branch_spacing = 5 };

private:

  char _branches[max_depth];
  int _pos;

public:
  BranchTracker()
    : _pos(0) {}

  void push(bool has_branch) {
    if (_pos < max_depth) {
      _branches[_pos] = has_branch ? '|' : ' ';
    }
    _pos ++; // beyond max depth, omit branch drawing but do count on.
  }

  void pop() {
    assert(_pos > 0, "must be");
    _pos --;
  }

  void print(outputStream* st) {
    for (int i = 0; i < _pos; i ++) {
      st->print("%c%.*s", _branches[i], branch_spacing, "          ");
    }
  }

  class Mark {
    BranchTracker& _tr;
  public:
    Mark(BranchTracker& tr, bool has_branch_here)
      : _tr(tr)  { _tr.push(has_branch_here); }
    ~Mark() { _tr.pop(); }
  };

}; // end: BranchTracker

struct LoadedClassInfo : public ResourceObj {
public:
  LoadedClassInfo* _next;
  Klass* const _klass;
  const ClassLoaderData* const _cld;

  LoadedClassInfo(Klass* klass, const ClassLoaderData* cld)
    : _klass(klass), _cld(cld) {}

};

class LoaderTreeNode : public ResourceObj {

  // We walk the CLDG and, for each CLD which is findable, add
  // a tree node.
  // To add a node we need its parent node; if the parent node does not yet
  // exist - because we have not yet encountered the CLD for the parent loader -
  // we add a preliminary empty LoaderTreeNode for it. This preliminary node
  // just contains the loader oop and nothing else. Once we encounter the CLD of
  // this parent loader, we fill in all the other details.

  const oop _loader_oop;
  const ClassLoaderData* _cld;

  LoaderTreeNode* _child;
  LoaderTreeNode* _next;

  LoadedClassInfo* _classes;
  int _num_classes;

  LoadedClassInfo* _hidden_classes;
  int _num_hidden_classes;

  // In default view, similar tree nodes (same loader class, same name or no name)
  // are folded into each other to make the output more readable.
  // _num_folded contains the number of nodes which have been folded into this
  // one.
  int _num_folded;

  void print_with_childs(outputStream* st, BranchTracker& branchtracker,
      bool print_classes, bool verbose) const {

    ResourceMark rm;

    if (_cld == NULL) {
      // Not sure how this could happen: we added a preliminary node for a parent but then never encountered
      // its CLD?
      return;
    }

    // Retrieve information.
    const Klass* const loader_klass = _cld->class_loader_klass();
    const Symbol* const loader_name = _cld->name();

    branchtracker.print(st);

    // e.g. "+--- jdk.internal.reflect.DelegatingClassLoader"
    st->print("+%.*s", BranchTracker::twig_len, "----------");
    if (_cld->is_the_null_class_loader_data()) {
      st->print(" <bootstrap>");
    } else {
      assert(!_cld->has_class_mirror_holder(), "_cld must be the primary cld");
      if (loader_name != NULL) {
        st->print(" \"%s\",", loader_name->as_C_string());
      }
      st->print(" %s", loader_klass != NULL ? loader_klass->external_name() : "??");
      if (_num_folded > 0) {
        st->print(" (+ %d more)", _num_folded);
      }
    }
    st->cr();

    // Output following this node (node details and child nodes) - up to the next sibling node
    // needs to be prefixed with "|" if there is a follow up sibling.
    const bool have_sibling = _next != NULL;
    BranchTracker::Mark trm(branchtracker, have_sibling);

    {
      // optional node details following this node needs to be prefixed with "|"
      // if there are follow up child nodes.
      const bool have_child = _child != NULL;
      BranchTracker::Mark trm(branchtracker, have_child);

      // Empty line
      branchtracker.print(st);
      st->cr();

      const int indentation = 18;

      if (verbose) {
        branchtracker.print(st);
        st->print_cr("%*s " PTR_FORMAT, indentation, "Loader Oop:", p2i(_loader_oop));
        branchtracker.print(st);
        st->print_cr("%*s " PTR_FORMAT, indentation, "Loader Data:", p2i(_cld));
        branchtracker.print(st);
        st->print_cr("%*s " PTR_FORMAT, indentation, "Loader Klass:", p2i(loader_klass));

        // Empty line
        branchtracker.print(st);
        st->cr();
      }

      if (print_classes) {
        if (_classes != NULL) {
          for (LoadedClassInfo* lci = _classes; lci; lci = lci->_next) {
            // non-strong hidden classes should not live in
            // the primary CLD of their loaders.
            assert(lci->_cld == _cld, "must be");

            branchtracker.print(st);
            if (lci == _classes) { // first iteration
              st->print("%*s ", indentation, "Classes:");
            } else {
              st->print("%*s ", indentation, "");
            }
            st->print("%s", lci->_klass->external_name());

            // Special treatment for generated core reflection accessor classes: print invocation target.
            if (ReflectionAccessorImplKlassHelper::is_generated_accessor(lci->_klass)) {
              st->print(" (invokes: ");
              ReflectionAccessorImplKlassHelper::print_invocation_target(st, lci->_klass);
              st->print(")");
            }

            st->cr();
          }
          branchtracker.print(st);
          st->print("%*s ", indentation, "");
          st->print_cr("(%u class%s)", _num_classes, (_num_classes == 1) ? "" : "es");

          // Empty line
          branchtracker.print(st);
          st->cr();
        }

        if (_hidden_classes != NULL) {
          for (LoadedClassInfo* lci = _hidden_classes; lci; lci = lci->_next) {
            branchtracker.print(st);
            if (lci == _hidden_classes) { // first iteration
              st->print("%*s ", indentation, "Hidden Classes:");
            } else {
              st->print("%*s ", indentation, "");
            }
            st->print("%s", lci->_klass->external_name());
            // For non-strong hidden classes, also print CLD if verbose. Should be a
            // different one than the primary CLD.
            assert(lci->_cld != _cld, "must be");
            if (verbose) {
              st->print("  (Loader Data: " PTR_FORMAT ")", p2i(lci->_cld));
            }
            st->cr();
          }
          branchtracker.print(st);
          st->print("%*s ", indentation, "");
          st->print_cr("(%u hidden class%s)", _num_hidden_classes,
                       (_num_hidden_classes == 1) ? "" : "es");

          // Empty line
          branchtracker.print(st);
          st->cr();
        }

      } // end: print_classes

    } // Pop branchtracker mark

    // Print children, recursively
    LoaderTreeNode* c = _child;
    while (c != NULL) {
      c->print_with_childs(st, branchtracker, print_classes, verbose);
      c = c->_next;
    }

  }

  // Helper: Attempt to fold this node into the target node. If success, returns true.
  // Folding can be done if both nodes are leaf nodes and they refer to the same loader class
  // and they have the same name or no name (note: leaf check is done by caller).
  bool can_fold_into(LoaderTreeNode* target_node) const {
    assert(is_leaf() && target_node->is_leaf(), "must be leaf");
    return _cld->class_loader_klass() == target_node->_cld->class_loader_klass() &&
           _cld->name() == target_node->_cld->name();
  }

public:

  LoaderTreeNode(const oop loader_oop)
    : _loader_oop(loader_oop), _cld(NULL), _child(NULL), _next(NULL),
      _classes(NULL), _num_classes(0), _hidden_classes(NULL),
      _num_hidden_classes(0), _num_folded(0)
    {}

  void set_cld(const ClassLoaderData* cld) {
    _cld = cld;
  }

  void add_child(LoaderTreeNode* info) {
    info->_next = _child;
    _child = info;
  }

  void add_sibling(LoaderTreeNode* info) {
    assert(info->_next == NULL, "must be");
    info->_next = _next;
    _next = info;
  }

  void add_classes(LoadedClassInfo* first_class, int num_classes, bool has_class_mirror_holder) {
    LoadedClassInfo** p_list_to_add_to;
    bool is_hidden = first_class->_klass->is_hidden();
    if (has_class_mirror_holder) {
      p_list_to_add_to = &_hidden_classes;
    } else {
      p_list_to_add_to = &_classes;
    }
    // Search tail.
    while ((*p_list_to_add_to) != NULL) {
      p_list_to_add_to = &(*p_list_to_add_to)->_next;
    }
    *p_list_to_add_to = first_class;
    if (has_class_mirror_holder) {
      _num_hidden_classes += num_classes;
    } else {
      _num_classes += num_classes;
    }
  }

  const ClassLoaderData* cld() const {
    return _cld;
  }

  const oop loader_oop() const {
    return _loader_oop;
  }

  LoaderTreeNode* find(const oop loader_oop) {
    LoaderTreeNode* result = NULL;
    if (_loader_oop == loader_oop) {
      result = this;
    } else {
      LoaderTreeNode* c = _child;
      while (c != NULL && result == NULL) {
        result = c->find(loader_oop);
        c = c->_next;
      }
    }
    return result;
  }

  bool is_leaf() const { return _child == NULL; }

  // Attempt to fold similar nodes among this node's children. We only fold leaf nodes
  // (no child class loaders).
  // For non-leaf nodes (class loaders with child class loaders), do this recursivly.
  void fold_children() {
    LoaderTreeNode* node = _child;
    LoaderTreeNode* prev = NULL;
    while (node != NULL) {
      LoaderTreeNode* matching_node = NULL;
      if (node->is_leaf()) {
        // Look among the preceeding node siblings for a match.
        for (LoaderTreeNode* node2 = _child; node2 != node && matching_node == NULL;
            node2 = node2->_next) {
          if (node2->is_leaf() && node->can_fold_into(node2)) {
            matching_node = node2;
          }
        }
      } else {
        node->fold_children();
      }
      if (matching_node != NULL) {
        // Increase fold count for the matching node and remove folded node from the child list.
        matching_node->_num_folded ++;
        assert(prev != NULL, "Sanity"); // can never happen since we do not fold the first node.
        prev->_next = node->_next;
      } else {
        prev = node;
      }
      node = node->_next;
    }
  }

  void print_with_childs(outputStream* st, bool print_classes, bool print_add_info) const {
    BranchTracker bwt;
    print_with_childs(st, bwt, print_classes, print_add_info);
  }

};

class LoadedClassCollectClosure : public KlassClosure {
public:
  LoadedClassInfo* _list;
  const ClassLoaderData* _cld;
  int _num_classes;
  LoadedClassCollectClosure(const ClassLoaderData* cld)
    : _list(NULL), _cld(cld), _num_classes(0) {}
  void do_klass(Klass* k) {
    LoadedClassInfo* lki = new LoadedClassInfo(k, _cld);
    lki->_next = _list;
    _list = lki;
    _num_classes ++;
  }
};

class LoaderInfoScanClosure : public CLDClosure {

  const bool _print_classes;
  const bool _verbose;
  LoaderTreeNode* _root;

  static void fill_in_classes(LoaderTreeNode* info, const ClassLoaderData* cld) {
    assert(info != NULL && cld != NULL, "must be");
    LoadedClassCollectClosure lccc(cld);
    const_cast<ClassLoaderData*>(cld)->classes_do(&lccc);
    if (lccc._num_classes > 0) {
      info->add_classes(lccc._list, lccc._num_classes, cld->has_class_mirror_holder());
    }
  }

  LoaderTreeNode* find_node_or_add_empty_node(oop loader_oop) {

    assert(_root != NULL, "root node must exist");

    if (loader_oop == NULL) {
      return _root;
    }

    // Check if a node for this oop already exists.
    LoaderTreeNode* info = _root->find(loader_oop);

    if (info == NULL) {
      // It does not. Create a node.
      info = new LoaderTreeNode(loader_oop);

      // Add it to tree.
      LoaderTreeNode* parent_info = NULL;

      // Recursively add parent nodes if needed.
      const oop parent_oop = java_lang_ClassLoader::parent(loader_oop);
      if (parent_oop == NULL) {
        parent_info = _root;
      } else {
        parent_info = find_node_or_add_empty_node(parent_oop);
      }
      assert(parent_info != NULL, "must be");

      parent_info->add_child(info);
    }
    return info;
  }


public:
  LoaderInfoScanClosure(bool print_classes, bool verbose)
    : _print_classes(print_classes), _verbose(verbose), _root(NULL) {
    _root = new LoaderTreeNode(NULL);
  }

  void print_results(outputStream* st) const {
    _root->print_with_childs(st, _print_classes, _verbose);
  }

  void do_cld (ClassLoaderData* cld) {

    // We do not display unloading loaders, for now.
    if (!cld->is_alive()) {
      return;
    }

    const oop loader_oop = cld->class_loader();

    LoaderTreeNode* info = find_node_or_add_empty_node(loader_oop);
    assert(info != NULL, "must be");

    // Update CLD in node, but only if this is the primary CLD for this loader.
    if (cld->has_class_mirror_holder() == false) {
      assert(info->cld() == NULL, "there should be only one primary CLD per loader");
      info->set_cld(cld);
    }

    // Add classes.
    fill_in_classes(info, cld);
  }

  void fold() {
    _root->fold_children();
  }

};


class ClassLoaderHierarchyVMOperation : public VM_Operation {
  outputStream* const _out;
  const bool _show_classes;
  const bool _verbose;
  const bool _fold;
public:
  ClassLoaderHierarchyVMOperation(outputStream* out, bool show_classes, bool verbose, bool fold) :
    _out(out), _show_classes(show_classes), _verbose(verbose), _fold(fold)
  {}

  VMOp_Type type() const {
    return VMOp_ClassLoaderHierarchyOperation;
  }

  void doit() {
    assert(SafepointSynchronize::is_at_safepoint(), "must be a safepoint");
    ResourceMark rm;
    LoaderInfoScanClosure cl (_show_classes, _verbose);
    ClassLoaderDataGraph::loaded_cld_do(&cl);
    // In non-verbose and non-show-classes mode, attempt to fold the tree.
    if (_fold) {
      if (!_verbose && !_show_classes) {
        cl.fold();
      }
    }
    cl.print_results(_out);
  }
};

// This command needs to be executed at a safepoint.
void ClassLoaderHierarchyDCmd::execute(DCmdSource source, TRAPS) {
  ClassLoaderHierarchyVMOperation op(output(), _show_classes.value(), _verbose.value(), _fold.value());
  VMThread::execute(&op);
}
