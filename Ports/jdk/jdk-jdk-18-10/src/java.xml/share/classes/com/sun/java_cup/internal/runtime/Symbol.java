/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the Symbol class, which is used to represent all terminals
 * and nonterminals while parsing.  The lexer should pass CUP Symbols
 * and CUP returns a Symbol.
 *
 * @author  Frank Flannery
 */

/* ****************************************************************
  Class Symbol
  what the parser expects to receive from the lexer.
  the token is identified as follows:
  sym:    the symbol type
  parse_state: the parse state.
  value:  is the lexical value of type Object
  left :  is the left position in the original input file
  right:  is the right position in the original input file
******************************************************************/

public class Symbol {

/*******************************
  Constructor for l,r values
 *******************************/

  public Symbol(int id, int l, int r, Object o) {
    this(id);
    left = l;
    right = r;
    value = o;
  }

/*******************************
  Constructor for no l,r values
********************************/

  public Symbol(int id, Object o) {
    this(id);
    left = -1;
    right = -1;
    value = o;
  }

/*****************************
  Constructor for no value
  ***************************/

  public Symbol(int sym_num, int l, int r) {
    sym = sym_num;
    left = l;
    right = r;
    value = null;
  }

/***********************************
  Constructor for no value or l,r
***********************************/

  public Symbol(int sym_num) {
    this(sym_num, -1);
    left = -1;
    right = -1;
    value = null;
  }

/***********************************
  Constructor to give a start state
***********************************/
  public Symbol(int sym_num, int state)
    {
      sym = sym_num;
      parse_state = state;
    }

/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The symbol number of the terminal or non terminal being represented */
  public int sym;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The parse state to be recorded on the parse stack with this symbol.
   *  This field is for the convenience of the parser and shouldn't be
   *  modified except by the parser.
   */
  public int parse_state;
  /** This allows us to catch some errors caused by scanners recycling
   *  symbols.  For the use of the parser only. [CSA, 23-Jul-1999] */
  boolean used_by_parser = false;

/*******************************
  The data passed to parser
 *******************************/

  public int left, right;
  public Object value;

  /*****************************
    Printing this token out. (Override for pretty-print).
    ****************************/
  public String toString() { return "#"+sym; }
}
