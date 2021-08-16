/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "classfile/classFileParser.hpp"
#include "classfile/fieldLayoutBuilder.hpp"
#include "memory/resourceArea.hpp"
#include "oops/array.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/instanceMirrorKlass.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/klass.inline.hpp"
#include "runtime/fieldDescriptor.inline.hpp"


LayoutRawBlock::LayoutRawBlock(Kind kind, int size) :
  _next_block(NULL),
  _prev_block(NULL),
  _kind(kind),
  _offset(-1),
  _alignment(1),
  _size(size),
  _field_index(-1),
  _is_reference(false) {
  assert(kind == EMPTY || kind == RESERVED || kind == PADDING || kind == INHERITED,
         "Otherwise, should use the constructor with a field index argument");
  assert(size > 0, "Sanity check");
}


LayoutRawBlock::LayoutRawBlock(int index, Kind kind, int size, int alignment, bool is_reference) :
 _next_block(NULL),
 _prev_block(NULL),
 _kind(kind),
 _offset(-1),
 _alignment(alignment),
 _size(size),
 _field_index(index),
 _is_reference(is_reference) {
  assert(kind == REGULAR || kind == FLATTENED || kind == INHERITED,
         "Other kind do not have a field index");
  assert(size > 0, "Sanity check");
  assert(alignment > 0, "Sanity check");
}

bool LayoutRawBlock::fit(int size, int alignment) {
  int adjustment = 0;
  if ((_offset % alignment) != 0) {
    adjustment = alignment - (_offset % alignment);
  }
  return _size >= size + adjustment;
}

FieldGroup::FieldGroup(int contended_group) :
  _next(NULL),
  _primitive_fields(NULL),
  _oop_fields(NULL),
  _contended_group(contended_group),  // -1 means no contended group, 0 means default contended group
  _oop_count(0) {}

void FieldGroup::add_primitive_field(AllFieldStream fs, BasicType type) {
  int size = type2aelembytes(type);
  LayoutRawBlock* block = new LayoutRawBlock(fs.index(), LayoutRawBlock::REGULAR, size, size /* alignment == size for primitive types */, false);
  if (_primitive_fields == NULL) {
    _primitive_fields = new(ResourceObj::RESOURCE_AREA, mtInternal) GrowableArray<LayoutRawBlock*>(INITIAL_LIST_SIZE);
  }
  _primitive_fields->append(block);
}

void FieldGroup::add_oop_field(AllFieldStream fs) {
  int size = type2aelembytes(T_OBJECT);
  LayoutRawBlock* block = new LayoutRawBlock(fs.index(), LayoutRawBlock::REGULAR, size, size /* alignment == size for oops */, true);
  if (_oop_fields == NULL) {
    _oop_fields = new(ResourceObj::RESOURCE_AREA, mtInternal) GrowableArray<LayoutRawBlock*>(INITIAL_LIST_SIZE);
  }
  _oop_fields->append(block);
  _oop_count++;
}

void FieldGroup::sort_by_size() {
  if (_primitive_fields != NULL) {
    _primitive_fields->sort(LayoutRawBlock::compare_size_inverted);
  }
}

FieldLayout::FieldLayout(Array<u2>* fields, ConstantPool* cp) :
  _fields(fields),
  _cp(cp),
  _blocks(NULL),
  _start(_blocks),
  _last(_blocks) {}

void FieldLayout::initialize_static_layout() {
  _blocks = new LayoutRawBlock(LayoutRawBlock::EMPTY, INT_MAX);
  _blocks->set_offset(0);
  _last = _blocks;
  _start = _blocks;
  // Note: at this stage, InstanceMirrorKlass::offset_of_static_fields() could be zero, because
  // during bootstrapping, the size of the java.lang.Class is still not known when layout
  // of static field is computed. Field offsets are fixed later when the size is known
  // (see java_lang_Class::fixup_mirror())
  if (InstanceMirrorKlass::offset_of_static_fields() > 0) {
    insert(first_empty_block(), new LayoutRawBlock(LayoutRawBlock::RESERVED, InstanceMirrorKlass::offset_of_static_fields()));
    _blocks->set_offset(0);
  }
}

void FieldLayout::initialize_instance_layout(const InstanceKlass* super_klass) {
  if (super_klass == NULL) {
    _blocks = new LayoutRawBlock(LayoutRawBlock::EMPTY, INT_MAX);
    _blocks->set_offset(0);
    _last = _blocks;
    _start = _blocks;
    insert(first_empty_block(), new LayoutRawBlock(LayoutRawBlock::RESERVED, instanceOopDesc::base_offset_in_bytes()));
  } else {
    bool has_fields = reconstruct_layout(super_klass);
    fill_holes(super_klass);
    if ((UseEmptySlotsInSupers && !super_klass->has_contended_annotations()) || !has_fields) {
      _start = _blocks;  // start allocating fields from the first empty block
    } else {
      _start = _last;    // append fields at the end of the reconstructed layout
    }
  }
}

LayoutRawBlock* FieldLayout::first_field_block() {
  LayoutRawBlock* block = _start;
  while (block->kind() != LayoutRawBlock::INHERITED && block->kind() != LayoutRawBlock::REGULAR
      && block->kind() != LayoutRawBlock::FLATTENED && block->kind() != LayoutRawBlock::PADDING) {
    block = block->next_block();
  }
  return block;
}


// Insert a set of fields into a layout using a best-fit strategy.
// For each field, search for the smallest empty slot able to fit the field
// (satisfying both size and alignment requirements), if none is found,
// add the field at the end of the layout.
// Fields cannot be inserted before the block specified in the "start" argument
void FieldLayout::add(GrowableArray<LayoutRawBlock*>* list, LayoutRawBlock* start) {
  if (list == NULL) return;
  if (start == NULL) start = this->_start;
  bool last_search_success = false;
  int last_size = 0;
  int last_alignment = 0;
  for (int i = 0; i < list->length(); i ++) {
    LayoutRawBlock* b = list->at(i);
    LayoutRawBlock* cursor = NULL;
    LayoutRawBlock* candidate = NULL;

    // if start is the last block, just append the field
    if (start == last_block()) {
      candidate = last_block();
    }
    // Before iterating over the layout to find an empty slot fitting the field's requirements,
    // check if the previous field had the same requirements and if the search for a fitting slot
    // was successful. If the requirements were the same but the search failed, a new search will
    // fail the same way, so just append the field at the of the layout.
    else  if (b->size() == last_size && b->alignment() == last_alignment && !last_search_success) {
      candidate = last_block();
    } else {
      // Iterate over the layout to find an empty slot fitting the field's requirements
      last_size = b->size();
      last_alignment = b->alignment();
      cursor = last_block()->prev_block();
      assert(cursor != NULL, "Sanity check");
      last_search_success = true;
      while (cursor != start) {
        if (cursor->kind() == LayoutRawBlock::EMPTY && cursor->fit(b->size(), b->alignment())) {
          if (candidate == NULL || cursor->size() < candidate->size()) {
            candidate = cursor;
          }
        }
        cursor = cursor->prev_block();
      }
      if (candidate == NULL) {
        candidate = last_block();
        last_search_success = false;
      }
      assert(candidate != NULL, "Candidate must not be null");
      assert(candidate->kind() == LayoutRawBlock::EMPTY, "Candidate must be an empty block");
      assert(candidate->fit(b->size(), b->alignment()), "Candidate must be able to store the block");
    }

    insert_field_block(candidate, b);
  }
}

// Used for classes with hard coded field offsets, insert a field at the specified offset */
void FieldLayout::add_field_at_offset(LayoutRawBlock* block, int offset, LayoutRawBlock* start) {
  assert(block != NULL, "Sanity check");
  block->set_offset(offset);
  if (start == NULL) {
    start = this->_start;
  }
  LayoutRawBlock* slot = start;
  while (slot != NULL) {
    if ((slot->offset() <= block->offset() && (slot->offset() + slot->size()) > block->offset()) ||
        slot == _last){
      assert(slot->kind() == LayoutRawBlock::EMPTY, "Matching slot must be an empty slot");
      assert(slot->size() >= block->offset() + block->size() ,"Matching slot must be big enough");
      if (slot->offset() < block->offset()) {
        int adjustment = block->offset() - slot->offset();
        LayoutRawBlock* adj = new LayoutRawBlock(LayoutRawBlock::EMPTY, adjustment);
        insert(slot, adj);
      }
      insert(slot, block);
      if (slot->size() == 0) {
        remove(slot);
      }
      FieldInfo::from_field_array(_fields, block->field_index())->set_offset(block->offset());
      return;
    }
    slot = slot->next_block();
  }
  fatal("Should have found a matching slot above, corrupted layout or invalid offset");
}

// The allocation logic uses a best fit strategy: the set of fields is allocated
// in the first empty slot big enough to contain the whole set ((including padding
// to fit alignment constraints).
void FieldLayout::add_contiguously(GrowableArray<LayoutRawBlock*>* list, LayoutRawBlock* start) {
  if (list == NULL) return;
  if (start == NULL) {
    start = _start;
  }
  // This code assumes that if the first block is well aligned, the following
  // blocks would naturally be well aligned (no need for adjustment)
  int size = 0;
  for (int i = 0; i < list->length(); i++) {
    size += list->at(i)->size();
  }

  LayoutRawBlock* candidate = NULL;
  if (start == last_block()) {
    candidate = last_block();
  } else {
    LayoutRawBlock* first = list->at(0);
    candidate = last_block()->prev_block();
    while (candidate->kind() != LayoutRawBlock::EMPTY || !candidate->fit(size, first->alignment())) {
      if (candidate == start) {
        candidate = last_block();
        break;
      }
      candidate = candidate->prev_block();
    }
    assert(candidate != NULL, "Candidate must not be null");
    assert(candidate->kind() == LayoutRawBlock::EMPTY, "Candidate must be an empty block");
    assert(candidate->fit(size, first->alignment()), "Candidate must be able to store the whole contiguous block");
  }

  for (int i = 0; i < list->length(); i++) {
    LayoutRawBlock* b = list->at(i);
    insert_field_block(candidate, b);
    assert((candidate->offset() % b->alignment() == 0), "Contiguous blocks must be naturally well aligned");
  }
}

LayoutRawBlock* FieldLayout::insert_field_block(LayoutRawBlock* slot, LayoutRawBlock* block) {
  assert(slot->kind() == LayoutRawBlock::EMPTY, "Blocks can only be inserted in empty blocks");
  if (slot->offset() % block->alignment() != 0) {
    int adjustment = block->alignment() - (slot->offset() % block->alignment());
    LayoutRawBlock* adj = new LayoutRawBlock(LayoutRawBlock::EMPTY, adjustment);
    insert(slot, adj);
  }
  insert(slot, block);
  if (slot->size() == 0) {
    remove(slot);
  }
  FieldInfo::from_field_array(_fields, block->field_index())->set_offset(block->offset());
  return block;
}

bool FieldLayout::reconstruct_layout(const InstanceKlass* ik) {
  bool has_instance_fields = false;
  GrowableArray<LayoutRawBlock*>* all_fields = new GrowableArray<LayoutRawBlock*>(32);
  while (ik != NULL) {
    for (AllFieldStream fs(ik->fields(), ik->constants()); !fs.done(); fs.next()) {
      BasicType type = Signature::basic_type(fs.signature());
      // distinction between static and non-static fields is missing
      if (fs.access_flags().is_static()) continue;
      has_instance_fields = true;
      int size = type2aelembytes(type);
      // INHERITED blocks are marked as non-reference because oop_maps are handled by their holder class
      LayoutRawBlock* block = new LayoutRawBlock(fs.index(), LayoutRawBlock::INHERITED, size, size, false);
      block->set_offset(fs.offset());
      all_fields->append(block);
    }
    ik = ik->super() == NULL ? NULL : InstanceKlass::cast(ik->super());
  }

  all_fields->sort(LayoutRawBlock::compare_offset);
  _blocks = new LayoutRawBlock(LayoutRawBlock::RESERVED, instanceOopDesc::base_offset_in_bytes());
  _blocks->set_offset(0);
  _last = _blocks;

  for(int i = 0; i < all_fields->length(); i++) {
    LayoutRawBlock* b = all_fields->at(i);
    _last->set_next_block(b);
    b->set_prev_block(_last);
    _last = b;
  }
  _start = _blocks;
  return has_instance_fields;
}

// Called during the reconstruction of a layout, after fields from super
// classes have been inserted. It fills unused slots between inserted fields
// with EMPTY blocks, so the regular field insertion methods would work.
// This method handles classes with @Contended annotations differently
// by inserting PADDING blocks instead of EMPTY block to prevent subclasses'
// fields to interfere with contended fields/classes.
void FieldLayout::fill_holes(const InstanceKlass* super_klass) {
  assert(_blocks != NULL, "Sanity check");
  assert(_blocks->offset() == 0, "first block must be at offset zero");
  LayoutRawBlock::Kind filling_type = super_klass->has_contended_annotations() ? LayoutRawBlock::PADDING: LayoutRawBlock::EMPTY;
  LayoutRawBlock* b = _blocks;
  while (b->next_block() != NULL) {
    if (b->next_block()->offset() > (b->offset() + b->size())) {
      int size = b->next_block()->offset() - (b->offset() + b->size());
      LayoutRawBlock* empty = new LayoutRawBlock(filling_type, size);
      empty->set_offset(b->offset() + b->size());
      empty->set_next_block(b->next_block());
      b->next_block()->set_prev_block(empty);
      b->set_next_block(empty);
      empty->set_prev_block(b);
    }
    b = b->next_block();
  }
  assert(b->next_block() == NULL, "Invariant at this point");
  assert(b->kind() != LayoutRawBlock::EMPTY, "Sanity check");

  // If the super class has @Contended annotation, a padding block is
  // inserted at the end to ensure that fields from the subclasses won't share
  // the cache line of the last field of the contended class
  if (super_klass->has_contended_annotations() && ContendedPaddingWidth > 0) {
    LayoutRawBlock* p = new LayoutRawBlock(LayoutRawBlock::PADDING, ContendedPaddingWidth);
    p->set_offset(b->offset() + b->size());
    b->set_next_block(p);
    p->set_prev_block(b);
    b = p;
  }

  if (!UseEmptySlotsInSupers) {
    // Add an empty slots to align fields of the subclass on a heapOopSize boundary
    // in order to emulate the behavior of the previous algorithm
    int align = (b->offset() + b->size()) % heapOopSize;
    if (align != 0) {
      int sz = heapOopSize - align;
      LayoutRawBlock* p = new LayoutRawBlock(LayoutRawBlock::EMPTY, sz);
      p->set_offset(b->offset() + b->size());
      b->set_next_block(p);
      p->set_prev_block(b);
      b = p;
    }
  }

  LayoutRawBlock* last = new LayoutRawBlock(LayoutRawBlock::EMPTY, INT_MAX);
  last->set_offset(b->offset() + b->size());
  assert(last->offset() > 0, "Sanity check");
  b->set_next_block(last);
  last->set_prev_block(b);
  _last = last;
}

LayoutRawBlock* FieldLayout::insert(LayoutRawBlock* slot, LayoutRawBlock* block) {
  assert(slot->kind() == LayoutRawBlock::EMPTY, "Blocks can only be inserted in empty blocks");
  assert(slot->offset() % block->alignment() == 0, "Incompatible alignment");
  block->set_offset(slot->offset());
  slot->set_offset(slot->offset() + block->size());
  assert((slot->size() - block->size()) < slot->size(), "underflow checking");
  assert(slot->size() - block->size() >= 0, "no negative size allowed");
  slot->set_size(slot->size() - block->size());
  block->set_prev_block(slot->prev_block());
  block->set_next_block(slot);
  slot->set_prev_block(block);
  if (block->prev_block() != NULL) {
    block->prev_block()->set_next_block(block);
  }
  if (_blocks == slot) {
    _blocks = block;
  }
  return block;
}

void FieldLayout::remove(LayoutRawBlock* block) {
  assert(block != NULL, "Sanity check");
  assert(block != _last, "Sanity check");
  if (_blocks == block) {
    _blocks = block->next_block();
    if (_blocks != NULL) {
      _blocks->set_prev_block(NULL);
    }
  } else {
    assert(block->prev_block() != NULL, "_prev should be set for non-head blocks");
    block->prev_block()->set_next_block(block->next_block());
    block->next_block()->set_prev_block(block->prev_block());
  }
  if (block == _start) {
    _start = block->prev_block();
  }
}

void FieldLayout::print(outputStream* output, bool is_static, const InstanceKlass* super) {
  ResourceMark rm;
  LayoutRawBlock* b = _blocks;
  while(b != _last) {
    switch(b->kind()) {
      case LayoutRawBlock::REGULAR: {
        FieldInfo* fi = FieldInfo::from_field_array(_fields, b->field_index());
        output->print_cr(" @%d \"%s\" %s %d/%d %s",
                         b->offset(),
                         fi->name(_cp)->as_C_string(),
                         fi->signature(_cp)->as_C_string(),
                         b->size(),
                         b->alignment(),
                         "REGULAR");
        break;
      }
      case LayoutRawBlock::FLATTENED: {
        FieldInfo* fi = FieldInfo::from_field_array(_fields, b->field_index());
        output->print_cr(" @%d \"%s\" %s %d/%d %s",
                         b->offset(),
                         fi->name(_cp)->as_C_string(),
                         fi->signature(_cp)->as_C_string(),
                         b->size(),
                         b->alignment(),
                         "FLATTENED");
        break;
      }
      case LayoutRawBlock::RESERVED: {
        output->print_cr(" @%d %d/- %s",
                         b->offset(),
                         b->size(),
                         "RESERVED");
        break;
      }
      case LayoutRawBlock::INHERITED: {
        assert(!is_static, "Static fields are not inherited in layouts");
        assert(super != NULL, "super klass must be provided to retrieve inherited fields info");
        bool found = false;
        const InstanceKlass* ik = super;
        while (!found && ik != NULL) {
          for (AllFieldStream fs(ik->fields(), ik->constants()); !fs.done(); fs.next()) {
            if (fs.offset() == b->offset()) {
              output->print_cr(" @%d \"%s\" %s %d/%d %s",
                  b->offset(),
                  fs.name()->as_C_string(),
                  fs.signature()->as_C_string(),
                  b->size(),
                  b->size(), // so far, alignment constraint == size, will change with Valhalla
                  "INHERITED");
              found = true;
              break;
            }
          }
          ik = ik->java_super();
        }
        break;
      }
      case LayoutRawBlock::EMPTY:
        output->print_cr(" @%d %d/1 %s",
                         b->offset(),
                         b->size(),
                        "EMPTY");
        break;
      case LayoutRawBlock::PADDING:
        output->print_cr(" @%d %d/1 %s",
                         b->offset(),
                         b->size(),
                        "PADDING");
        break;
    }
    b = b->next_block();
  }
}

FieldLayoutBuilder::FieldLayoutBuilder(const Symbol* classname, const InstanceKlass* super_klass, ConstantPool* constant_pool,
      Array<u2>* fields, bool is_contended, FieldLayoutInfo* info) :
  _classname(classname),
  _super_klass(super_klass),
  _constant_pool(constant_pool),
  _fields(fields),
  _info(info),
  _root_group(NULL),
  _contended_groups(GrowableArray<FieldGroup*>(8)),
  _static_fields(NULL),
  _layout(NULL),
  _static_layout(NULL),
  _nonstatic_oopmap_count(0),
  _alignment(-1),
  _has_nonstatic_fields(false),
  _is_contended(is_contended) {}


FieldGroup* FieldLayoutBuilder::get_or_create_contended_group(int g) {
  assert(g > 0, "must only be called for named contended groups");
  FieldGroup* fg = NULL;
  for (int i = 0; i < _contended_groups.length(); i++) {
    fg = _contended_groups.at(i);
    if (fg->contended_group() == g) return fg;
  }
  fg = new FieldGroup(g);
  _contended_groups.append(fg);
  return fg;
}

void FieldLayoutBuilder::prologue() {
  _layout = new FieldLayout(_fields, _constant_pool);
  const InstanceKlass* super_klass = _super_klass;
  _layout->initialize_instance_layout(super_klass);
  if (super_klass != NULL) {
    _has_nonstatic_fields = super_klass->has_nonstatic_fields();
  }
  _static_layout = new FieldLayout(_fields, _constant_pool);
  _static_layout->initialize_static_layout();
  _static_fields = new FieldGroup();
  _root_group = new FieldGroup();
}

// Field sorting for regular classes:
//   - fields are sorted in static and non-static fields
//   - non-static fields are also sorted according to their contention group
//     (support of the @Contended annotation)
//   - @Contended annotation is ignored for static fields
void FieldLayoutBuilder::regular_field_sorting() {
  for (AllFieldStream fs(_fields, _constant_pool); !fs.done(); fs.next()) {
    FieldGroup* group = NULL;
    if (fs.access_flags().is_static()) {
      group = _static_fields;
    } else {
      _has_nonstatic_fields = true;
      if (fs.is_contended()) {
        int g = fs.contended_group();
        if (g == 0) {
          group = new FieldGroup(true);
          _contended_groups.append(group);
        } else {
          group = get_or_create_contended_group(g);
        }
      } else {
        group = _root_group;
      }
    }
    assert(group != NULL, "invariant");
    BasicType type = Signature::basic_type(fs.signature());
    switch(type) {
      case T_BYTE:
      case T_CHAR:
      case T_DOUBLE:
      case T_FLOAT:
      case T_INT:
      case T_LONG:
      case T_SHORT:
      case T_BOOLEAN:
        group->add_primitive_field(fs, type);
        break;
      case T_OBJECT:
      case T_ARRAY:
        if (group != _static_fields) _nonstatic_oopmap_count++;
        group->add_oop_field(fs);
        break;
      default:
        fatal("Something wrong?");
    }
  }
  _root_group->sort_by_size();
  _static_fields->sort_by_size();
  if (!_contended_groups.is_empty()) {
    for (int i = 0; i < _contended_groups.length(); i++) {
      _contended_groups.at(i)->sort_by_size();
    }
  }
}

void FieldLayoutBuilder::insert_contended_padding(LayoutRawBlock* slot) {
  if (ContendedPaddingWidth > 0) {
    LayoutRawBlock* padding = new LayoutRawBlock(LayoutRawBlock::PADDING, ContendedPaddingWidth);
    _layout->insert(slot, padding);
  }
}

// Computation of regular classes layout is an evolution of the previous default layout
// (FieldAllocationStyle 1):
//   - primitive fields are allocated first (from the biggest to the smallest)
//   - then oop fields are allocated, either in existing gaps or at the end of
//     the layout
void FieldLayoutBuilder::compute_regular_layout() {
  bool need_tail_padding = false;
  prologue();
  regular_field_sorting();

  if (_is_contended) {
    _layout->set_start(_layout->last_block());
    // insertion is currently easy because the current strategy doesn't try to fill holes
    // in super classes layouts => the _start block is by consequence the _last_block
    insert_contended_padding(_layout->start());
    need_tail_padding = true;
  }
  _layout->add(_root_group->primitive_fields());
  _layout->add(_root_group->oop_fields());

  if (!_contended_groups.is_empty()) {
    for (int i = 0; i < _contended_groups.length(); i++) {
      FieldGroup* cg = _contended_groups.at(i);
      LayoutRawBlock* start = _layout->last_block();
      insert_contended_padding(start);
      _layout->add(cg->primitive_fields(), start);
      _layout->add(cg->oop_fields(), start);
      need_tail_padding = true;
    }
  }

  if (need_tail_padding) {
    insert_contended_padding(_layout->last_block());
  }

  _static_layout->add_contiguously(this->_static_fields->oop_fields());
  _static_layout->add(this->_static_fields->primitive_fields());

  epilogue();
}

void FieldLayoutBuilder::epilogue() {
  // Computing oopmaps
  int super_oop_map_count = (_super_klass == NULL) ? 0 :_super_klass->nonstatic_oop_map_count();
  int max_oop_map_count = super_oop_map_count + _nonstatic_oopmap_count;

  OopMapBlocksBuilder* nonstatic_oop_maps =
      new OopMapBlocksBuilder(max_oop_map_count);
  if (super_oop_map_count > 0) {
    nonstatic_oop_maps->initialize_inherited_blocks(_super_klass->start_of_nonstatic_oop_maps(),
    _super_klass->nonstatic_oop_map_count());
  }

  if (_root_group->oop_fields() != NULL) {
    for (int i = 0; i < _root_group->oop_fields()->length(); i++) {
      LayoutRawBlock* b = _root_group->oop_fields()->at(i);
      nonstatic_oop_maps->add(b->offset(), 1);
    }
  }

  if (!_contended_groups.is_empty()) {
    for (int i = 0; i < _contended_groups.length(); i++) {
      FieldGroup* cg = _contended_groups.at(i);
      if (cg->oop_count() > 0) {
        assert(cg->oop_fields() != NULL && cg->oop_fields()->at(0) != NULL, "oop_count > 0 but no oop fields found");
        nonstatic_oop_maps->add(cg->oop_fields()->at(0)->offset(), cg->oop_count());
      }
    }
  }

  nonstatic_oop_maps->compact();

  int instance_end = align_up(_layout->last_block()->offset(), wordSize);
  int static_fields_end = align_up(_static_layout->last_block()->offset(), wordSize);
  int static_fields_size = (static_fields_end -
      InstanceMirrorKlass::offset_of_static_fields()) / wordSize;
  int nonstatic_field_end = align_up(_layout->last_block()->offset(), heapOopSize);

  // Pass back information needed for InstanceKlass creation

  _info->oop_map_blocks = nonstatic_oop_maps;
  _info->_instance_size = align_object_size(instance_end / wordSize);
  _info->_static_field_size = static_fields_size;
  _info->_nonstatic_field_size = (nonstatic_field_end - instanceOopDesc::base_offset_in_bytes()) / heapOopSize;
  _info->_has_nonstatic_fields = _has_nonstatic_fields;

  if (PrintFieldLayout) {
    ResourceMark rm;
    tty->print_cr("Layout of class %s", _classname->as_C_string());
    tty->print_cr("Instance fields:");
    _layout->print(tty, false, _super_klass);
    tty->print_cr("Static fields:");
    _static_layout->print(tty, true, NULL);
    tty->print_cr("Instance size = %d bytes", _info->_instance_size * wordSize);
    tty->print_cr("---");
  }
}

void FieldLayoutBuilder::build_layout() {
  compute_regular_layout();
}
