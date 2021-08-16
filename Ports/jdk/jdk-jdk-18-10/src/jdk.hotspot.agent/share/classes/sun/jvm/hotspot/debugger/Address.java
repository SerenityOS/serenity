/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger;

/** <P> This is the bottom-most interface which abstracts address
    access for both debugging and introspection. In the situation of
    debugging a target VM, these routines can throw the specified
    RuntimeExceptions to indicate failure and allow recovery of the
    debugging system. If these are used for introspecting the current
    VM and implementing functionality in it, however, it is expected
    that these kinds of failures will not occur and, in fact, a crash
    will occur if the situation arises where they would have been
    thrown. </P>

    <P> Addresses are immutable. Further, it was decided not to expose
    the representation of the Address (and provide a corresponding
    factory method from, for example, long to Address). Unfortunately,
    because of the existence of C and "reuse" of low bits of pointers,
    it is occasionally necessary to perform logical operations like
    masking off the low bits of an "address". While these operations
    could be used to generate arbitrary Address objects, allowing this
    is not the intent of providing these operations. </P>

    <P> This interface is able to fetch all Java primitive types,
    addresses, oops, and C integers of arbitrary size (see @see
    sun.jvm.hotspot.types.CIntegerType for further discussion). Note
    that the result of the latter is restricted to fitting into a
    64-bit value and the high-order bytes will be silently discarded
    if too many bytes are requested. </P>

    <P> Implementations may have restrictions, for example that the
    Java-related routines may not be called until a certain point in
    the bootstrapping process once the sizes of the Java primitive
    types are known. (The current implementation has that property.)
    </P>

    <P> A note of warning: in C addresses, when represented as
    integers, are usually represented with unsigned types.
    Unfortunately, there are no unsigned primitive types in Java, so
    care will have to be taken in the implementation of this interface
    if using longs as the representation for 64-bit correctness. This
    is not so simple for the comparison operators. </P> */

public interface Address {

  /** This is stated explicitly here because it is important for
      implementations to understand that equals() and hashCode() must
      absolutely, positively work properly -- i.e., two Address
      objects representing the same address are both equal (via
      equals()) and have the same hash code. */
  public boolean equals(Object arg);

  /** This is stated explicitly here because it is important for
      implementations to understand that equals() and hashCode() must
      absolutely, positively work properly -- i.e., two Address
      objects representing the same address are both equal (via
      equals()) and have the same hash code. */
  public int hashCode();

  //
  // C/C++-related routines
  //

  public long       getCIntegerAt      (long offset, long numBytes, boolean isUnsigned)
    throws UnmappedAddressException, UnalignedAddressException;
  /** This returns null if the address at the given offset is NULL. */
  public Address    getAddressAt       (long offset) throws UnmappedAddressException, UnalignedAddressException;
  /** Returns the decoded address at the given offset */
  public Address    getCompOopAddressAt (long offset) throws UnmappedAddressException, UnalignedAddressException;
  public Address    getCompKlassAddressAt (long offset) throws UnmappedAddressException, UnalignedAddressException;

  //
  // Java-related routines
  //

  public boolean    getJBooleanAt      (long offset) throws UnmappedAddressException, UnalignedAddressException;
  public byte       getJByteAt         (long offset) throws UnmappedAddressException, UnalignedAddressException;
  public char       getJCharAt         (long offset) throws UnmappedAddressException, UnalignedAddressException;
  public double     getJDoubleAt       (long offset) throws UnmappedAddressException, UnalignedAddressException;
  public float      getJFloatAt        (long offset) throws UnmappedAddressException, UnalignedAddressException;
  public int        getJIntAt          (long offset) throws UnmappedAddressException, UnalignedAddressException;
  public long       getJLongAt         (long offset) throws UnmappedAddressException, UnalignedAddressException;
  public short      getJShortAt        (long offset) throws UnmappedAddressException, UnalignedAddressException;
  /** This returns null if the address at the given offset is NULL. */
  public OopHandle  getOopHandleAt     (long offset)
    throws UnmappedAddressException, UnalignedAddressException, NotInHeapException;
  public OopHandle  getCompOopHandleAt (long offset)
    throws UnmappedAddressException, UnalignedAddressException, NotInHeapException;

  //
  // C/C++-related mutators. These throw UnmappedAddressException if
  // the target is read-only (for example, a core file rather than an
  // active process), if the target address is unmapped, or if it is
  // read-only. The implementation may supply extra detail messages.
  //

  /** Sets a C integer numBytes in size at the specified offset. Note
      that there is no "unsigned" flag for the accessor since the
      value will not be sign-extended; the number of bytes are simply
      copied from the value into the target address space. */
  public void setCIntegerAt(long offset, long numBytes, long value);

  /** Sets an Address at the specified location. */
  public void setAddressAt(long offset, Address value);

  //
  // Java-related mutators -- see above.
  //

  public void       setJBooleanAt      (long offset, boolean value)
    throws UnmappedAddressException, UnalignedAddressException;
  public void       setJByteAt         (long offset, byte value)
    throws UnmappedAddressException, UnalignedAddressException;
  public void       setJCharAt         (long offset, char value)
    throws UnmappedAddressException, UnalignedAddressException;
  public void       setJDoubleAt       (long offset, double value)
    throws UnmappedAddressException, UnalignedAddressException;
  public void       setJFloatAt        (long offset, float value)
    throws UnmappedAddressException, UnalignedAddressException;
  public void       setJIntAt          (long offset, int value)
    throws UnmappedAddressException, UnalignedAddressException;
  public void       setJLongAt         (long offset, long value)
    throws UnmappedAddressException, UnalignedAddressException;
  public void       setJShortAt        (long offset, short value)
    throws UnmappedAddressException, UnalignedAddressException;
  public void       setOopHandleAt     (long offset, OopHandle value)
    throws UnmappedAddressException, UnalignedAddressException;

  //
  // Arithmetic operations -- necessary evil.
  //

  /** This throws an UnsupportedOperationException if this address happens
      to actually be an OopHandle, because interior object pointers
      are not allowed. Negative offsets are allowed and handle the
      subtraction case. */
  public Address    addOffsetTo        (long offset) throws UnsupportedOperationException;

  /** This method had to be added in order to support heap iteration
      in the debugging system, and is effectively the dangerous
      operation of allowing interior object pointers. For this reason
      it was kept as a separate API and its use is forbidden in the
      non-debugging (i.e., reflective system) case. It is strongly
      recommended that this not be called by clients: it is currently
      wrapped up in the Space's iteration mechanism. */
  public OopHandle  addOffsetToAsOopHandle(long offset) throws UnsupportedOperationException;

  /** Performs the subtraction "this - arg", returning the resulting
      offset in bytes. Note that this must handle a null argument
      properly, and can be used to convert an Address into a long for
      further manipulation, but that the reverse conversion is not
      possible. (FIXME: any signed/unsigned issues? Should this work
      for OopHandles?) */
  public long       minus(Address arg);

  /** Performs unsigned comparison "this < arg".
      (FIXME: should this work for OopHandles?) */
  public boolean    lessThan          (Address arg);
  /** Performs unsigned comparison "this <= arg".
      (FIXME: should this work for OopHandles?) */
  public boolean    lessThanOrEqual   (Address arg);
  /** Performs unsigned comparison "this > arg".
      (FIXME: should this work for OopHandles?) */
  public boolean    greaterThan       (Address arg);
  /** Performs unsigned comparison "this >= arg".
      (FIXME: should this work for OopHandles?) */
  public boolean    greaterThanOrEqual(Address arg);

  /** This throws an UnsupportedOperationException if this address happens
      to actually be an OopHandle. Performs a logical "and" operation
      of the bits of the address and the mask (least significant bits
      of the Address and the mask are aligned) and returns the result
      as an Address. Returns null if the result was zero. */
  public Address    andWithMask(long mask) throws UnsupportedOperationException;

  /** This throws an UnsupportedOperationException if this address happens
      to actually be an OopHandle. Performs a logical "or" operation
      of the bits of the address and the mask (least significant bits
      of the Address and the mask are aligned) and returns the result
      as an Address. Returns null if the result was zero. */
  public Address    orWithMask(long mask) throws UnsupportedOperationException;

  /** This throws an UnsupportedOperationException if this address happens
      to actually be an OopHandle. Performs a logical "exclusive or"
      operation of the bits of the address and the mask (least
      significant bits of the Address and the mask are aligned) and
      returns the result as an Address. Returns null if the result was
      zero. */
  public Address    xorWithMask(long mask) throws UnsupportedOperationException;

  // return address as long integer.
  public long asLongValue();
}
