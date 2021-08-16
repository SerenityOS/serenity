/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_IDEALGRAPHPRINTER_HPP
#define SHARE_OPTO_IDEALGRAPHPRINTER_HPP

#include "libadt/dict.hpp"
#include "libadt/vectset.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/ostream.hpp"
#include "utilities/xmlstream.hpp"

#ifndef PRODUCT

class Compile;
class PhaseIFG;
class PhaseChaitin;
class Matcher;
class Node;
class InlineTree;
class ciMethod;

class IdealGraphPrinter : public CHeapObj<mtCompiler> {
 private:

  static const char *INDENT;
  static const char *TOP_ELEMENT;
  static const char *GROUP_ELEMENT;
  static const char *GRAPH_ELEMENT;
  static const char *PROPERTIES_ELEMENT;
  static const char *EDGES_ELEMENT;
  static const char *PROPERTY_ELEMENT;
  static const char *EDGE_ELEMENT;
  static const char *NODE_ELEMENT;
  static const char *NODES_ELEMENT;
  static const char *CONTROL_FLOW_ELEMENT;
  static const char *REMOVE_EDGE_ELEMENT;
  static const char *REMOVE_NODE_ELEMENT;
  static const char *METHOD_NAME_PROPERTY;
  static const char *BLOCK_NAME_PROPERTY;
  static const char *BLOCK_DOMINATOR_PROPERTY;
  static const char *BLOCK_ELEMENT;
  static const char *SUCCESSORS_ELEMENT;
  static const char *SUCCESSOR_ELEMENT;
  static const char *METHOD_IS_PUBLIC_PROPERTY;
  static const char *METHOD_IS_STATIC_PROPERTY;
  static const char *TRUE_VALUE;
  static const char *NODE_NAME_PROPERTY;
  static const char *EDGE_NAME_PROPERTY;
  static const char *NODE_ID_PROPERTY;
  static const char *FROM_PROPERTY;
  static const char *TO_PROPERTY;
  static const char *PROPERTY_NAME_PROPERTY;
  static const char *GRAPH_NAME_PROPERTY;
  static const char *INDEX_PROPERTY;
  static const char *METHOD_ELEMENT;
  static const char *INLINE_ELEMENT;
  static const char *BYTECODES_ELEMENT;
  static const char *METHOD_BCI_PROPERTY;
  static const char *METHOD_SHORT_NAME_PROPERTY;
  static const char *ASSEMBLY_ELEMENT;

  static int _file_count;
  networkStream *_network_stream;
  xmlStream *_xml;
  outputStream *_output;
  ciMethod *_current_method;
  int _depth;
  char buffer[128];
  bool _should_send_method;
  PhaseChaitin* _chaitin;
  bool _traverse_outs;
  Compile *C;
  double _max_freq;

  void print_method(ciMethod *method, int bci, InlineTree *tree);
  void print_inline_tree(InlineTree *tree);
  void visit_node(Node *n, bool edges, VectorSet* temp_set);
  void walk_nodes(Node *start, bool edges, VectorSet* temp_set);
  void begin_elem(const char *s);
  void end_elem();
  void begin_head(const char *s);
  void end_head();
  void print_attr(const char *name, const char *val);
  void print_attr(const char *name, intptr_t val);
  void print_prop(const char *name, const char *val);
  void print_prop(const char *name, int val);
  void tail(const char *name);
  void head(const char *name);
  void text(const char *s);
  void init(const char* file_name, bool use_multiple_files, bool append);
  void init_file_stream(const char* file_name, bool use_multiple_files, bool append);
  void init_network_stream();
  IdealGraphPrinter();
  ~IdealGraphPrinter();

 public:
  IdealGraphPrinter(Compile* compile, const char* file_name = NULL, bool append = false);
  static void clean_up();
  static IdealGraphPrinter *printer();

  bool traverse_outs();
  void set_traverse_outs(bool b);
  void print_inlining();
  void begin_method();
  void end_method();
  void print_method(const char *name, int level = 0);
  void print(const char *name, Node *root);
  void set_compile(Compile* compile) {C = compile; }
  void update_compiled_method(ciMethod* current_method);
};

#endif

#endif // SHARE_OPTO_IDEALGRAPHPRINTER_HPP
