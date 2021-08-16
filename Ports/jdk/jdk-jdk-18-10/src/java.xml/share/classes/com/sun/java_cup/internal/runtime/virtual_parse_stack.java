/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */


package com.sun.java_cup.internal.runtime;

import java.util.Stack;

/** This class implements a temporary or "virtual" parse stack that
 *  replaces the top portion of the actual parse stack (the part that
 *  has been changed by some set of operations) while maintaining its
 *  original contents.  This data structure is used when the parse needs
 *  to "parse ahead" to determine if a given error recovery attempt will
 *  allow the parse to continue far enough to consider it successful.  Once
 *  success or failure of parse ahead is determined the system then
 *  reverts to the original parse stack (which has not actually been
 *  modified).  Since parse ahead does not execute actions, only parse
 *  state is maintained on the virtual stack, not full Symbol objects.
 *
 * @see     com.sun.java_cup.internal.runtime.lr_parser
 * @author  Frank Flannery
 */

public class virtual_parse_stack {
  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Constructor to build a virtual stack out of a real stack. */
  public virtual_parse_stack(Stack<Symbol> shadowing_stack) throws java.lang.Exception
    {
      /* sanity check */
      if (shadowing_stack == null)
        throw new Exception(
          "Internal parser error: attempt to create null virtual stack");

      /* set up our internals */
      real_stack = shadowing_stack;
      vstack     = new Stack<>();
      real_next  = 0;

      /* get one element onto the virtual portion of the stack */
      get_from_real();
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The real stack that we shadow.  This is accessed when we move off
   *  the bottom of the virtual portion of the stack, but is always left
   *  unmodified.
   */
  protected Stack<Symbol> real_stack;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Top of stack indicator for where we leave off in the real stack.
   *  This is measured from top of stack, so 0 would indicate that no
   *  elements have been "moved" from the real to virtual stack.
   */
  protected int real_next;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The virtual top portion of the stack.  This stack contains Integer
   *  objects with state numbers.  This stack shadows the top portion
   *  of the real stack within the area that has been modified (via operations
   *  on the virtual stack).  When this portion of the stack becomes empty we
   *  transfer elements from the underlying stack onto this stack.
   */
  protected Stack<Integer> vstack;

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Transfer an element from the real to the virtual stack.  This assumes
   *  that the virtual stack is currently empty.
   */
  protected void get_from_real()
    {
      Symbol stack_sym;

      /* don't transfer if the real stack is empty */
      if (real_next >= real_stack.size()) return;

      /* get a copy of the first Symbol we have not transfered */
      stack_sym = real_stack.get(real_stack.size()-1-real_next);

      /* record the transfer */
      real_next++;

      /* put the state number from the Symbol onto the virtual stack */
      vstack.push(stack_sym.parse_state);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Indicate whether the stack is empty. */
  public boolean empty()
    {
      /* if vstack is empty then we were unable to transfer onto it and
         the whole thing is empty. */
      return vstack.empty();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Return value on the top of the stack (without popping it). */
  public int top() throws java.lang.Exception
    {
      if (vstack.empty())
        throw new Exception(
                  "Internal parser error: top() called on empty virtual stack");

      return (vstack.peek());
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Pop the stack. */
  public void pop() throws java.lang.Exception
    {
      if (vstack.empty())
        throw new Exception(
                  "Internal parser error: pop from empty virtual stack");

      /* pop it */
      vstack.pop();

      /* if we are now empty transfer an element (if there is one) */
      if (vstack.empty())
        get_from_real();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Push a state number onto the stack. */
  public void push(int state_num)
    {
      vstack.push(state_num);
    }

  /*-----------------------------------------------------------*/

}
