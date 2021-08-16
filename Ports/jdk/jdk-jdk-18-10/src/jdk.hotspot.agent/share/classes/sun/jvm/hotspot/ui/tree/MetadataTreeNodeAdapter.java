/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.ui.tree;

import java.io.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;

/** An adapter class which allows oops to be displayed in a tree via
    the SimpleTreeNode interface. FIXME: must attach this to some sort
    of policy object which determines how to display names and whether
    VM fields should be shown. (Must also fix oop visitation mechanism
    in oops package.) */

public class MetadataTreeNodeAdapter extends FieldTreeNodeAdapter {
  private Metadata metadata;

  /** The metadata may be null (for metadata fields of metadatas which are null); the
      FieldIdentifier may also be null (for the root node).
      treeTableMode defaults to false. */
  public MetadataTreeNodeAdapter(Metadata metadata, FieldIdentifier id) {
    this(metadata, id, false);
  }

  /** The metadata may be null (for metadata fields of metadatas which are null); the
      FieldIdentifier may also be null (for the root node). */
  public MetadataTreeNodeAdapter(Metadata metadata, FieldIdentifier id, boolean treeTableMode) {
    super(id, treeTableMode);
    this.metadata = metadata;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public int getChildCount() {
    if (metadata == null) {
      return 0;
    }

    Counter c = new Counter();
    metadata.iterate(c);
    return c.getNumFields() + (VM.getVM().getRevPtrs() == null ? 0 : 1);
  }

  public SimpleTreeNode getChild(int index) {
    if (metadata == null) {
      return null;
    }

    Fetcher f = new Fetcher(index);
    metadata.iterate(f);
    return f.getChild();
  }

  public boolean isLeaf() {
    return (metadata == null);
  }

  public int getIndexOfChild(SimpleTreeNode child) {
    if (child instanceof RevPtrsTreeNodeAdapter) {
      // assert(VM.getVM().getRevPtrs() != null, "Only created from revptrs");
      return 0;
    }
    FieldIdentifier id = ((FieldTreeNodeAdapter) child).getID();
    Finder f = new Finder(id);
    metadata.iterate(f);
    return f.getIndex() + (VM.getVM().getRevPtrs() == null ? 0 : 1);
  }

  public String getValue() {
    if (metadata != null) {
      // FIXME: choose style of printing depending on whether we're
      // displaying VM fields? Want to make Java objects look like
      // Java objects.
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      metadata.printValueOn(new PrintStream(bos));
      return bos.toString();
    }
    return "null";
  }

  /** Should be applied to one metadata at a time, then have the number of
      fields fetched. FIXME: want this to distinguish between VM and
      non-VM fields. */
  static class Counter extends DefaultMetadataVisitor {
    private int numFields;

    public int getNumFields() {
      return numFields;
    }

    public void prologue() {
      numFields = 0;
    }

    public void doMetadata(MetadataField field, boolean isVMField) { ++numFields; }
    public void doOop(OopField field, boolean isVMField)         { ++numFields; }
    public void doByte(ByteField field, boolean isVMField)       { ++numFields; }
    public void doChar(CharField field, boolean isVMField)       { ++numFields; }
    public void doBoolean(BooleanField field, boolean isVMField) { ++numFields; }
    public void doShort(ShortField field, boolean isVMField)     { ++numFields; }
    public void doInt(IntField field, boolean isVMField)         { ++numFields; }
    public void doLong(LongField field, boolean isVMField)       { ++numFields; }
    public void doFloat(FloatField field, boolean isVMField)     { ++numFields; }
    public void doDouble(DoubleField field, boolean isVMField)   { ++numFields; }
    public void doCInt(CIntField field, boolean isVMField)       { ++numFields; }
  }

  /** Creates a new SimpleTreeNode for the given field. FIXME: want
      this to distinguish between VM and non-VM fields. */
  class Fetcher extends DefaultMetadataVisitor {
    private int index;
    private int curField;
    private SimpleTreeNode child;

    public Fetcher(int index) {
      this.index = index;
    }

    public SimpleTreeNode getChild() {
      return child;
    }

    public void prologue() {
      curField = 0;
    }

    public void doMetadata(MetadataField field, boolean isVMField) {
      if (curField == index) {
        try {
          child = new MetadataTreeNodeAdapter(field.getValue(getObj()), field.getID(), getTreeTableMode());
        } catch (AddressException e) {
          child = new BadAddressTreeNodeAdapter(getObj().getAddress().getAddressAt(field.getOffset()), field, getTreeTableMode());
        } catch (UnknownOopException e) {
          child = new BadAddressTreeNodeAdapter(getObj().getAddress().getAddressAt(field.getOffset()), field, getTreeTableMode());
        }
      }
      ++curField;
    }

    public void doOop(OopField field, boolean isVMField) {
      if (curField == index) {
        try {
          child = new OopTreeNodeAdapter(field.getValue(getObj()), field.getID(), getTreeTableMode());
        } catch (AddressException e) {
          child = new BadAddressTreeNodeAdapter(field.getValueAsOopHandle(getObj()), field, getTreeTableMode());
        } catch (UnknownOopException e) {
          child = new BadAddressTreeNodeAdapter(field.getValueAsOopHandle(getObj()), field, getTreeTableMode());
        }
      }
      ++curField;
    }

    public void doByte(ByteField field, boolean isVMField) {
      if (curField == index) {
        child = new LongTreeNodeAdapter(field.getValue(getObj()) & 0xFF, field.getID(), getTreeTableMode());
      }
      ++curField;
    }

    public void doChar(CharField field, boolean isVMField) {
      if (curField == index) {
        child = new CharTreeNodeAdapter(field.getValue(getObj()), field.getID(), getTreeTableMode());
      }
      ++curField;
    }

    public void doBoolean(BooleanField field, boolean isVMField) {
      if (curField == index) {
        child = new BooleanTreeNodeAdapter(field.getValue(getObj()), field.getID(), getTreeTableMode());
      }
      ++curField;
    }

    public void doShort(ShortField field, boolean isVMField) {
      if (curField == index) {
        child = new LongTreeNodeAdapter(field.getValue(getObj()) & 0xFFFF, field.getID(), getTreeTableMode());
      }
      ++curField;
    }

    public void doInt(IntField field, boolean isVMField) {
      if (curField == index) {
        child = new LongTreeNodeAdapter(field.getValue(getObj()) & 0xFFFFFFFF, field.getID(), getTreeTableMode());
      }
      ++curField;
    }

    public void doLong(LongField field, boolean isVMField) {
      if (curField == index) {
        child = new LongTreeNodeAdapter(field.getValue(getObj()), field.getID(), getTreeTableMode());
      }
      ++curField;
    }

    public void doFloat(FloatField field, boolean isVMField) {
      if (curField == index) {
        child = new FloatTreeNodeAdapter(field.getValue(getObj()), field.getID(), getTreeTableMode());
      }
      ++curField;
    }

    public void doDouble(DoubleField field, boolean isVMField) {
      if (curField == index) {
        child = new DoubleTreeNodeAdapter(field.getValue(getObj()), field.getID(), getTreeTableMode());
      }
      ++curField;
    }

    public void doCInt(CIntField field, boolean isVMField) {
      if (curField == index) {
        child = new LongTreeNodeAdapter(field.getValue(getObj()), field.getID(), getTreeTableMode());
      }
      ++curField;
    }
  }

  /** Finds the index of the given FieldIdentifier. */
  static class Finder extends DefaultMetadataVisitor {
    private FieldIdentifier id;
    private int curField;
    private int index;

    public Finder(FieldIdentifier id) {
      this.id = id;
    }

    /** Returns -1 if not found */
    public int getIndex() {
      return index;
    }

    public void prologue() {
      curField = 0;
      index = -1;
    }

    public void doMetadata(MetadataField field, boolean isVMField) { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doOop(OopField field, boolean isVMField)         { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doByte(ByteField field, boolean isVMField)       { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doChar(CharField field, boolean isVMField)       { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doBoolean(BooleanField field, boolean isVMField) { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doShort(ShortField field, boolean isVMField)     { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doInt(IntField field, boolean isVMField)         { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doLong(LongField field, boolean isVMField)       { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doFloat(FloatField field, boolean isVMField)     { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doDouble(DoubleField field, boolean isVMField)   { if (field.getID().equals(id)) { index = curField; } ++curField; }
    public void doCInt(CIntField field, boolean isVMField)       { if (field.getID().equals(id)) { index = curField; } ++curField; }
  }
}
