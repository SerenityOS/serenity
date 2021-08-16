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

#include "precompiled.hpp"
#include "memory/resourceArea.hpp"
#include "opto/chaitin.hpp"
#include "opto/idealGraphPrinter.hpp"
#include "opto/machnode.hpp"
#include "opto/parse.hpp"
#include "runtime/threadCritical.hpp"
#include "runtime/threadSMR.hpp"
#include "utilities/stringUtils.hpp"

#ifndef PRODUCT

// Constants
// Keep consistent with Java constants
const char *IdealGraphPrinter::INDENT = "  ";
const char *IdealGraphPrinter::TOP_ELEMENT = "graphDocument";
const char *IdealGraphPrinter::GROUP_ELEMENT = "group";
const char *IdealGraphPrinter::GRAPH_ELEMENT = "graph";
const char *IdealGraphPrinter::PROPERTIES_ELEMENT = "properties";
const char *IdealGraphPrinter::EDGES_ELEMENT = "edges";
const char *IdealGraphPrinter::PROPERTY_ELEMENT = "p";
const char *IdealGraphPrinter::EDGE_ELEMENT = "edge";
const char *IdealGraphPrinter::NODE_ELEMENT = "node";
const char *IdealGraphPrinter::NODES_ELEMENT = "nodes";
const char *IdealGraphPrinter::REMOVE_EDGE_ELEMENT = "removeEdge";
const char *IdealGraphPrinter::REMOVE_NODE_ELEMENT = "removeNode";
const char *IdealGraphPrinter::METHOD_NAME_PROPERTY = "name";
const char *IdealGraphPrinter::METHOD_IS_PUBLIC_PROPERTY = "public";
const char *IdealGraphPrinter::METHOD_IS_STATIC_PROPERTY = "static";
const char *IdealGraphPrinter::TRUE_VALUE = "true";
const char *IdealGraphPrinter::NODE_NAME_PROPERTY = "name";
const char *IdealGraphPrinter::EDGE_NAME_PROPERTY = "name";
const char *IdealGraphPrinter::NODE_ID_PROPERTY = "id";
const char *IdealGraphPrinter::FROM_PROPERTY = "from";
const char *IdealGraphPrinter::TO_PROPERTY = "to";
const char *IdealGraphPrinter::PROPERTY_NAME_PROPERTY = "name";
const char *IdealGraphPrinter::GRAPH_NAME_PROPERTY = "name";
const char *IdealGraphPrinter::INDEX_PROPERTY = "index";
const char *IdealGraphPrinter::METHOD_ELEMENT = "method";
const char *IdealGraphPrinter::INLINE_ELEMENT = "inlined";
const char *IdealGraphPrinter::BYTECODES_ELEMENT = "bytecodes";
const char *IdealGraphPrinter::METHOD_BCI_PROPERTY = "bci";
const char *IdealGraphPrinter::METHOD_SHORT_NAME_PROPERTY = "shortName";
const char *IdealGraphPrinter::CONTROL_FLOW_ELEMENT = "controlFlow";
const char *IdealGraphPrinter::BLOCK_NAME_PROPERTY = "name";
const char *IdealGraphPrinter::BLOCK_DOMINATOR_PROPERTY = "dom";
const char *IdealGraphPrinter::BLOCK_ELEMENT = "block";
const char *IdealGraphPrinter::SUCCESSORS_ELEMENT = "successors";
const char *IdealGraphPrinter::SUCCESSOR_ELEMENT = "successor";
const char *IdealGraphPrinter::ASSEMBLY_ELEMENT = "assembly";

int IdealGraphPrinter::_file_count = 0;

IdealGraphPrinter *IdealGraphPrinter::printer() {
  JavaThread *thread = JavaThread::current();
  if (!thread->is_Compiler_thread()) return NULL;

  CompilerThread *compiler_thread = (CompilerThread *)thread;
  if (compiler_thread->ideal_graph_printer() == NULL) {
    IdealGraphPrinter *printer = new IdealGraphPrinter();
    compiler_thread->set_ideal_graph_printer(printer);
  }

  return compiler_thread->ideal_graph_printer();
}

void IdealGraphPrinter::clean_up() {
  for (JavaThreadIteratorWithHandle jtiwh; JavaThread* p = jtiwh.next(); ) {
    if (p->is_Compiler_thread()) {
      CompilerThread* c = (CompilerThread*)p;
      IdealGraphPrinter* printer = c->ideal_graph_printer();
      if (printer) {
        delete printer;
      }
      c->set_ideal_graph_printer(NULL);
    }
  }
  IdealGraphPrinter* debug_file_printer = Compile::debug_file_printer();
  if (debug_file_printer != NULL) {
    delete debug_file_printer;
  }
  IdealGraphPrinter* debug_network_printer = Compile::debug_network_printer();
  if (debug_network_printer != NULL) {
    delete debug_network_printer;
  }
}

// Either print methods to file specified with PrintIdealGraphFile or otherwise over the network to the IGV
IdealGraphPrinter::IdealGraphPrinter() {
  init(PrintIdealGraphFile, true, false);
}

// Either print methods to the specified file 'file_name' or if NULL over the network to the IGV. If 'append'
// is set, the next phase is directly appended to the specified file 'file_name'. This is useful when doing
// replay compilation with a tool like rr that cannot alter the current program state but only the file.
IdealGraphPrinter::IdealGraphPrinter(Compile* compile, const char* file_name, bool append) {
  assert(!append || (append && file_name != NULL), "can only use append flag when printing to file");
  init(file_name, false, append);
  C = compile;
  if (append) {
    // When directly appending the next graph, we only need to set _current_method and not set up a new method
    _current_method = C->method();
  } else {
    begin_method();
  }
}

void IdealGraphPrinter::init(const char* file_name, bool use_multiple_files, bool append) {
  // By default dump both ins and outs since dead or unreachable code
  // needs to appear in the graph.  There are also some special cases
  // in the mach where kill projections have no users but should
  // appear in the dump.
  _traverse_outs = true;
  _should_send_method = true;
  _output = NULL;
  buffer[0] = 0;
  _depth = 0;
  _current_method = NULL;
  _network_stream = NULL;

  if (file_name != NULL) {
    init_file_stream(file_name, use_multiple_files, append);
  } else {
    init_network_stream();
  }
  _xml = new (ResourceObj::C_HEAP, mtCompiler) xmlStream(_output);
  if (!append) {
    head(TOP_ELEMENT);
  }
}

// Destructor, close file or network stream
IdealGraphPrinter::~IdealGraphPrinter() {
  tail(TOP_ELEMENT);

  // tty->print_cr("Walk time: %d", (int)_walk_time.milliseconds());
  // tty->print_cr("Output time: %d", (int)_output_time.milliseconds());
  // tty->print_cr("Build blocks time: %d", (int)_build_blocks_time.milliseconds());

  if(_xml) {
    delete _xml;
    _xml = NULL;
  }

  if (_network_stream) {
    delete _network_stream;
    if (_network_stream == _output) {
      _output = NULL;
    }
    _network_stream = NULL;
  }

  if (_output) {
    delete _output;
    _output = NULL;
  }
}

void IdealGraphPrinter::begin_elem(const char *s) {
  _xml->begin_elem("%s", s);
}

void IdealGraphPrinter::end_elem() {
  _xml->end_elem();
}

void IdealGraphPrinter::begin_head(const char *s) {
  _xml->begin_head("%s", s);
}

void IdealGraphPrinter::end_head() {
  _xml->end_head();
}

void IdealGraphPrinter::print_attr(const char *name, intptr_t val) {
  stringStream stream;
  stream.print(INTX_FORMAT, val);
  print_attr(name, stream.as_string());
}

void IdealGraphPrinter::print_attr(const char *name, const char *val) {
  _xml->print(" %s='", name);
  text(val);
  _xml->print("'");
}

void IdealGraphPrinter::head(const char *name) {
  _xml->head("%s", name);
}

void IdealGraphPrinter::tail(const char *name) {
  _xml->tail(name);
}

void IdealGraphPrinter::text(const char *s) {
  _xml->text("%s", s);
}

void IdealGraphPrinter::print_prop(const char *name, int val) {
  stringStream stream;
  stream.print("%d", val);
  print_prop(name, stream.as_string());
}

void IdealGraphPrinter::print_prop(const char *name, const char *val) {
  begin_head(PROPERTY_ELEMENT);
  print_attr(PROPERTY_NAME_PROPERTY, name);
  end_head();
  text(val);
  tail(PROPERTY_ELEMENT);
}

void IdealGraphPrinter::print_method(ciMethod *method, int bci, InlineTree *tree) {
  begin_head(METHOD_ELEMENT);

  stringStream str;
  method->print_name(&str);

  stringStream shortStr;
  method->print_short_name(&shortStr);

  print_attr(METHOD_NAME_PROPERTY, str.as_string());
  print_attr(METHOD_SHORT_NAME_PROPERTY, shortStr.as_string());
  print_attr(METHOD_BCI_PROPERTY, bci);

  end_head();

  head(BYTECODES_ELEMENT);
  _xml->print_cr("<![CDATA[");
  method->print_codes_on(_xml);
  _xml->print_cr("]]>");
  tail(BYTECODES_ELEMENT);

  if (tree != NULL && tree->subtrees().length() > 0) {
    head(INLINE_ELEMENT);
    GrowableArray<InlineTree *> subtrees = tree->subtrees();
    for (int i = 0; i < subtrees.length(); i++) {
      print_inline_tree(subtrees.at(i));
    }
    tail(INLINE_ELEMENT);
  }

  tail(METHOD_ELEMENT);
  _xml->flush();
}

void IdealGraphPrinter::print_inline_tree(InlineTree *tree) {
  if (tree != NULL) {
    print_method(tree->method(), tree->caller_bci(), tree);
  }
}

void IdealGraphPrinter::print_inlining() {

  // Print inline tree
  if (_should_send_method) {
    InlineTree *inlineTree = C->ilt();
    if (inlineTree != NULL) {
      print_inline_tree(inlineTree);
    } else {
      // print this method only
    }
  }
}

// Has to be called whenever a method is compiled
void IdealGraphPrinter::begin_method() {

  ciMethod *method = C->method();
  assert(_output, "output stream must exist!");
  assert(method, "null methods are not allowed!");
  assert(!_current_method, "current method must be null!");

  head(GROUP_ELEMENT);

  head(PROPERTIES_ELEMENT);

  // Print properties
  // Add method name
  stringStream strStream;
  method->print_name(&strStream);
  print_prop(METHOD_NAME_PROPERTY, strStream.as_string());

  if (method->flags().is_public()) {
    print_prop(METHOD_IS_PUBLIC_PROPERTY, TRUE_VALUE);
  }

  if (method->flags().is_static()) {
    print_prop(METHOD_IS_STATIC_PROPERTY, TRUE_VALUE);
  }

  tail(PROPERTIES_ELEMENT);

  _should_send_method = true;
  this->_current_method = method;

  _xml->flush();
}

// Has to be called whenever a method has finished compilation
void IdealGraphPrinter::end_method() {
  tail(GROUP_ELEMENT);
  _current_method = NULL;
  _xml->flush();
}

bool IdealGraphPrinter::traverse_outs() {
  return _traverse_outs;
}

void IdealGraphPrinter::set_traverse_outs(bool b) {
  _traverse_outs = b;
}

void IdealGraphPrinter::visit_node(Node *n, bool edges, VectorSet* temp_set) {

  if (edges) {

    for (uint i = 0; i < n->len(); i++) {
      if (n->in(i)) {
        Node *source = n->in(i);
        begin_elem(EDGE_ELEMENT);
        print_attr(FROM_PROPERTY, source->_igv_idx);
        print_attr(TO_PROPERTY, n->_igv_idx);
        print_attr(INDEX_PROPERTY, i);
        end_elem();
      }
    }

  } else {

    // Output node
    begin_head(NODE_ELEMENT);
    print_attr(NODE_ID_PROPERTY, n->_igv_idx);
    end_head();

    head(PROPERTIES_ELEMENT);

    Node *node = n;
#ifndef PRODUCT
    Compile::current()->_in_dump_cnt++;
    print_prop(NODE_NAME_PROPERTY, (const char *)node->Name());
    const Type *t = node->bottom_type();
    print_prop("type", t->msg());
    print_prop("idx", node->_idx);
#ifdef ASSERT
    print_prop("debug_idx", node->_debug_idx);
#endif

    if (C->cfg() != NULL) {
      Block* block = C->cfg()->get_block_for_node(node);
      if (block == NULL) {
        print_prop("block", C->cfg()->get_block(0)->_pre_order);
      } else {
        print_prop("block", block->_pre_order);
        // Print estimated execution frequency, normalized within a [0,1] range.
        buffer[0] = 0;
        stringStream freq(buffer, sizeof(buffer) - 1);
        // Higher precision has no practical effect in visualizations.
        freq.print("%.8f", block->_freq / _max_freq);
        assert(freq.size() < sizeof(buffer), "size in range");
        // Enforce dots as decimal separators, as required by IGV.
        StringUtils::replace_no_expand(buffer, ",", ".");
        print_prop("frequency", buffer);
      }
    }

    switch (t->category()) {
      case Type::Category::Data:
        print_prop("category", "data");
        break;
      case Type::Category::Memory:
        print_prop("category", "memory");
        break;
      case Type::Category::Mixed:
        print_prop("category", "mixed");
        break;
      case Type::Category::Control:
        print_prop("category", "control");
        break;
      case Type::Category::Other:
        print_prop("category", "other");
        break;
      case Type::Category::Undef:
        print_prop("category", "undef");
        break;
    }

    const jushort flags = node->flags();
    if (flags & Node::Flag_is_Copy) {
      print_prop("is_copy", "true");
    }
    if (flags & Node::Flag_rematerialize) {
      print_prop("rematerialize", "true");
    }
    if (flags & Node::Flag_needs_anti_dependence_check) {
      print_prop("needs_anti_dependence_check", "true");
    }
    if (flags & Node::Flag_is_macro) {
      print_prop("is_macro", "true");
    }
    if (flags & Node::Flag_is_Con) {
      print_prop("is_con", "true");
    }
    if (flags & Node::Flag_is_cisc_alternate) {
      print_prop("is_cisc_alternate", "true");
    }
    if (flags & Node::Flag_is_dead_loop_safe) {
      print_prop("is_dead_loop_safe", "true");
    }
    if (flags & Node::Flag_may_be_short_branch) {
      print_prop("may_be_short_branch", "true");
    }
    if (flags & Node::Flag_has_call) {
      print_prop("has_call", "true");
    }

    if (C->matcher() != NULL) {
      if (C->matcher()->is_shared(node)) {
        print_prop("is_shared", "true");
      } else {
        print_prop("is_shared", "false");
      }
      if (C->matcher()->is_dontcare(node)) {
        print_prop("is_dontcare", "true");
      } else {
        print_prop("is_dontcare", "false");
      }
      Node* old = C->matcher()->find_old_node(node);
      if (old != NULL) {
        print_prop("old_node_idx", old->_idx);
      }
    }

    if (node->is_Proj()) {
      print_prop("con", (int)node->as_Proj()->_con);
    }

    if (node->is_Mach()) {
      print_prop("idealOpcode", (const char *)NodeClassNames[node->as_Mach()->ideal_Opcode()]);
    }

    buffer[0] = 0;
    stringStream s2(buffer, sizeof(buffer) - 1);

    node->dump_spec(&s2);
    if (t != NULL && (t->isa_instptr() || t->isa_klassptr())) {
      const TypeInstPtr  *toop = t->isa_instptr();
      const TypeKlassPtr *tkls = t->isa_klassptr();
      ciKlass*           klass = toop ? toop->klass() : (tkls ? tkls->klass() : NULL );
      if( klass && klass->is_loaded() && klass->is_interface() ) {
        s2.print("  Interface:");
      } else if( toop ) {
        s2.print("  Oop:");
      } else if( tkls ) {
        s2.print("  Klass:");
      }
      t->dump_on(&s2);
    } else if( t == Type::MEMORY ) {
      s2.print("  Memory:");
      MemNode::dump_adr_type(node, node->adr_type(), &s2);
    }

    assert(s2.size() < sizeof(buffer), "size in range");
    print_prop("dump_spec", buffer);

    if (node->is_block_proj()) {
      print_prop("is_block_proj", "true");
    }

    if (node->is_block_start()) {
      print_prop("is_block_start", "true");
    }

    const char *short_name = "short_name";
    if (strcmp(node->Name(), "Parm") == 0 && node->as_Proj()->_con >= TypeFunc::Parms) {
      int index = node->as_Proj()->_con - TypeFunc::Parms;
      if (index >= 10) {
        print_prop(short_name, "PA");
      } else {
        sprintf(buffer, "P%d", index);
        print_prop(short_name, buffer);
      }
    } else if (strcmp(node->Name(), "IfTrue") == 0) {
      print_prop(short_name, "T");
    } else if (strcmp(node->Name(), "IfFalse") == 0) {
      print_prop(short_name, "F");
    } else if ((node->is_Con() && node->is_Type()) || node->is_Proj()) {

      if (t->base() == Type::Int && t->is_int()->is_con()) {
        const TypeInt *typeInt = t->is_int();
        assert(typeInt->is_con(), "must be constant");
        jint value = typeInt->get_con();

        // max. 2 chars allowed
        if (value >= -9 && value <= 99) {
          sprintf(buffer, "%d", value);
          print_prop(short_name, buffer);
        } else {
          print_prop(short_name, "I");
        }
      } else if (t == Type::TOP) {
        print_prop(short_name, "^");
      } else if (t->base() == Type::Long && t->is_long()->is_con()) {
        const TypeLong *typeLong = t->is_long();
        assert(typeLong->is_con(), "must be constant");
        jlong value = typeLong->get_con();

        // max. 2 chars allowed
        if (value >= -9 && value <= 99) {
          sprintf(buffer, JLONG_FORMAT, value);
          print_prop(short_name, buffer);
        } else {
          print_prop(short_name, "L");
        }
      } else if (t->base() == Type::KlassPtr) {
        const TypeKlassPtr *typeKlass = t->is_klassptr();
        print_prop(short_name, "CP");
      } else if (t->base() == Type::Control) {
        print_prop(short_name, "C");
      } else if (t->base() == Type::Memory) {
        print_prop(short_name, "M");
      } else if (t->base() == Type::Abio) {
        print_prop(short_name, "IO");
      } else if (t->base() == Type::Return_Address) {
        print_prop(short_name, "RA");
      } else if (t->base() == Type::AnyPtr) {
        print_prop(short_name, "P");
      } else if (t->base() == Type::RawPtr) {
        print_prop(short_name, "RP");
      } else if (t->base() == Type::AryPtr) {
        print_prop(short_name, "AP");
      }
    }

    JVMState* caller = NULL;
    if (node->is_SafePoint()) {
      caller = node->as_SafePoint()->jvms();
    } else {
      Node_Notes* notes = C->node_notes_at(node->_idx);
      if (notes != NULL) {
        caller = notes->jvms();
      }
    }

    if (caller != NULL) {
      stringStream bciStream;
      ciMethod* last = NULL;
      int last_bci;
      while(caller) {
        if (caller->has_method()) {
          last = caller->method();
          last_bci = caller->bci();
        }
        bciStream.print("%d ", caller->bci());
        caller = caller->caller();
      }
      print_prop("bci", bciStream.as_string());
      if (last != NULL && last->has_linenumber_table() && last_bci >= 0) {
        print_prop("line", last->line_number_from_bci(last_bci));
      }
    }

#ifdef ASSERT
    if (node->debug_orig() != NULL) {
      stringStream dorigStream;
      node->dump_orig(&dorigStream, false);
      print_prop("debug_orig", dorigStream.as_string());
    }
#endif

    if (_chaitin && _chaitin != (PhaseChaitin *)((intptr_t)0xdeadbeef)) {
      buffer[0] = 0;
      _chaitin->dump_register(node, buffer);
      print_prop("reg", buffer);
      uint lrg_id = 0;
      if (node->_idx < _chaitin->_lrg_map.size()) {
        lrg_id = _chaitin->_lrg_map.live_range_id(node);
      }
      print_prop("lrg", lrg_id);
    }

    Compile::current()->_in_dump_cnt--;
#endif

    tail(PROPERTIES_ELEMENT);
    tail(NODE_ELEMENT);
  }
}

void IdealGraphPrinter::walk_nodes(Node *start, bool edges, VectorSet* temp_set) {


  VectorSet visited;
  GrowableArray<Node *> nodeStack(Thread::current()->resource_area(), 0, 0, NULL);
  nodeStack.push(start);
  visited.test_set(start->_idx);
  if (C->cfg() != NULL) {
    // once we have a CFG there are some nodes that aren't really
    // reachable but are in the CFG so add them here.
    for (uint i = 0; i < C->cfg()->number_of_blocks(); i++) {
      Block* block = C->cfg()->get_block(i);
      for (uint s = 0; s < block->number_of_nodes(); s++) {
        nodeStack.push(block->get_node(s));
      }
    }
  }

  while(nodeStack.length() > 0) {

    Node *n = nodeStack.pop();
    visit_node(n, edges, temp_set);

    if (_traverse_outs) {
      for (DUIterator i = n->outs(); n->has_out(i); i++) {
        Node* p = n->out(i);
        if (!visited.test_set(p->_idx)) {
          nodeStack.push(p);
        }
      }
    }

    for ( uint i = 0; i < n->len(); i++ ) {
      if ( n->in(i) ) {
        if (!visited.test_set(n->in(i)->_idx)) {
          nodeStack.push(n->in(i));
        }
      }
    }
  }
}

void IdealGraphPrinter::print_method(const char *name, int level) {
  if (C->should_print(level)) {
    print(name, (Node *) C->root());
  }
}

// Print current ideal graph
void IdealGraphPrinter::print(const char *name, Node *node) {

  if (!_current_method || !_should_send_method || node == NULL) return;

  // Warning, unsafe cast?
  _chaitin = (PhaseChaitin *)C->regalloc();

  begin_head(GRAPH_ELEMENT);
  print_attr(GRAPH_NAME_PROPERTY, (const char *)name);
  end_head();

  VectorSet temp_set;

  head(NODES_ELEMENT);
  if (C->cfg() != NULL) {
    // Compute the maximum estimated frequency in the current graph.
    _max_freq = 1.0e-6;
    for (uint i = 0; i < C->cfg()->number_of_blocks(); i++) {
      Block* block = C->cfg()->get_block(i);
      if (block->_freq > _max_freq) {
        _max_freq = block->_freq;
      }
    }
  }
  walk_nodes(node, false, &temp_set);
  tail(NODES_ELEMENT);

  head(EDGES_ELEMENT);
  walk_nodes(node, true, &temp_set);
  tail(EDGES_ELEMENT);
  if (C->cfg() != NULL) {
    head(CONTROL_FLOW_ELEMENT);
    for (uint i = 0; i < C->cfg()->number_of_blocks(); i++) {
      Block* block = C->cfg()->get_block(i);
      begin_head(BLOCK_ELEMENT);
      print_attr(BLOCK_NAME_PROPERTY, block->_pre_order);
      end_head();

      head(SUCCESSORS_ELEMENT);
      for (uint s = 0; s < block->_num_succs; s++) {
        begin_elem(SUCCESSOR_ELEMENT);
        print_attr(BLOCK_NAME_PROPERTY, block->_succs[s]->_pre_order);
        end_elem();
      }
      tail(SUCCESSORS_ELEMENT);

      head(NODES_ELEMENT);
      for (uint s = 0; s < block->number_of_nodes(); s++) {
        begin_elem(NODE_ELEMENT);
        print_attr(NODE_ID_PROPERTY, block->get_node(s)->_igv_idx);
        end_elem();
      }
      tail(NODES_ELEMENT);

      tail(BLOCK_ELEMENT);
    }
    tail(CONTROL_FLOW_ELEMENT);
  }
  tail(GRAPH_ELEMENT);
  _xml->flush();
}

void IdealGraphPrinter::init_file_stream(const char* file_name, bool use_multiple_files, bool append) {
  ThreadCritical tc;
  if (use_multiple_files && _file_count != 0) {
    assert(!append, "append should only be used for debugging with a single file");
    ResourceMark rm;
    stringStream st;
    const char* dot = strrchr(file_name, '.');
    if (dot) {
      st.write(file_name, dot - file_name);
      st.print("%d%s", _file_count, dot);
    } else {
      st.print("%s%d", file_name, _file_count);
    }
    _output = new (ResourceObj::C_HEAP, mtCompiler) fileStream(st.as_string(), "w");
  } else {
    _output = new (ResourceObj::C_HEAP, mtCompiler) fileStream(file_name, append ? "a" : "w");
  }
  if (use_multiple_files) {
    assert(!append, "append should only be used for debugging with a single file");
    _file_count++;
  }
}

void IdealGraphPrinter::init_network_stream() {
  _network_stream = new (ResourceObj::C_HEAP, mtCompiler) networkStream();
  // Try to connect to visualizer
  if (_network_stream->connect(PrintIdealGraphAddress, PrintIdealGraphPort)) {
    char c = 0;
    _network_stream->read(&c, 1);
    if (c != 'y') {
      tty->print_cr("Client available, but does not want to receive data!");
      _network_stream->close();
      delete _network_stream;
      _network_stream = NULL;
      return;
    }
    _output = _network_stream;
  } else {
    // It would be nice if we could shut down cleanly but it should
    // be an error if we can't connect to the visualizer.
    fatal("Couldn't connect to visualizer at %s:" INTX_FORMAT,
          PrintIdealGraphAddress, PrintIdealGraphPort);
  }
}

void IdealGraphPrinter::update_compiled_method(ciMethod* current_method) {
  assert(C != NULL, "must already be set");
  if (current_method != _current_method) {
    // If a different method, end the old and begin with the new one.
    end_method();
    _current_method = NULL;
    begin_method();
  }
}

extern const char *NodeClassNames[];

#endif
