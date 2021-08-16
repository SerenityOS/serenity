/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.oops;

import java.io.*;

import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

// Super class for all fields in an object
public class Field {

  Field(FieldIdentifier id, long offset, boolean isVMField) {
    this.offset    = offset;
    this.id        = id;
    this.isVMField = isVMField;
  }

  /** Constructor for fields that are named in an InstanceKlass's
      fields array (i.e., named, non-VM fields) */
  Field(InstanceKlass holder, int fieldIndex) {
    this.holder = holder;
    this.fieldIndex = fieldIndex;

    offset               = holder.getFieldOffset(fieldIndex);
    genericSignature     = holder.getFieldGenericSignature(fieldIndex);

    name                 = holder.getFieldName(fieldIndex);
    id          = new NamedFieldIdentifier(name.asString());

    signature            = holder.getFieldSignature(fieldIndex);
    fieldType   = new FieldType(signature);

    short access         = holder.getFieldAccessFlags(fieldIndex);
    accessFlags = new AccessFlags(access);
  }

  private Symbol          name;
  private long            offset;
  private FieldIdentifier id;
  private boolean         isVMField;
  // Java fields only
  private InstanceKlass   holder;
  private FieldType       fieldType;
  private Symbol          signature;
  private Symbol          genericSignature;
  private AccessFlags     accessFlags;
  private int             fieldIndex;

  /** Returns the byte offset of the field within the object or klass */
  public long getOffset() { return offset; }

  /** Returns the identifier of the field */
  public FieldIdentifier getID() { return id; }

  public Symbol getName() { return name; }

  /** Indicates whether this is a VM field */
  public boolean isVMField() { return isVMField; }

  /** Indicates whether this is a named field */
  public boolean isNamedField() { return (id instanceof NamedFieldIdentifier); }

  public void printOn(PrintStream tty) {
    getID().printOn(tty);
    tty.print(" {" + getOffset() + "} :");
  }

  /** (Named, non-VM fields only) Returns the InstanceKlass containing
      this (static or non-static) field. */
  public InstanceKlass getFieldHolder() {
    return holder;
  }

  /** (Named, non-VM fields only) Returns the index in the fields
      TypeArray for this field. Equivalent to the "index" in the VM's
      fieldDescriptors. */
  public int getFieldIndex() {
    return fieldIndex;
  }

  /** (Named, non-VM fields only) Retrieves the access flags. */
  public long getAccessFlags() { return accessFlags.getValue(); }
  public AccessFlags getAccessFlagsObj() { return accessFlags; }

  /** (Named, non-VM fields only) Returns the type of this field. */
  public FieldType getFieldType() { return fieldType; }

  /** (Named, non-VM fields only) Returns the signature of this
      field. */
  public Symbol getSignature() { return signature; }
  public Symbol getGenericSignature() { return genericSignature; }

  public boolean hasInitialValue()           { return holder.getFieldInitialValueIndex(fieldIndex) != 0;    }

  //
  // Following acccessors are for named, non-VM fields only
  //
  public boolean isPublic()                  { return accessFlags.isPublic(); }
  public boolean isPrivate()                 { return accessFlags.isPrivate(); }
  public boolean isProtected()               { return accessFlags.isProtected(); }
  public boolean isPackagePrivate()          { return !isPublic() && !isPrivate() && !isProtected(); }

  public boolean isStatic()                  { return accessFlags.isStatic(); }
  public boolean isFinal()                   { return accessFlags.isFinal(); }
  public boolean isVolatile()                { return accessFlags.isVolatile(); }
  public boolean isTransient()               { return accessFlags.isTransient(); }

  public boolean isSynthetic()               { return accessFlags.isSynthetic(); }
  public boolean isEnumConstant()            { return accessFlags.isEnum();      }

  public boolean equals(Object obj) {
     if (obj == null) {
        return false;
     }

     if (! (obj instanceof Field)) {
        return false;
     }

     Field other = (Field) obj;
     return this.getFieldHolder().equals(other.getFieldHolder()) &&
            this.getID().equals(other.getID());
  }

  public int hashCode() {
     return getFieldHolder().hashCode() ^ getID().hashCode();
  }
}
