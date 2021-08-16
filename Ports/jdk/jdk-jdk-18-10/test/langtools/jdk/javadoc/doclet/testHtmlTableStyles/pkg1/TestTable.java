/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 */

package pkg1;

/**
 * Testing table in documentation comment. In the generated documentation, it
 * should be displayed as a regular table and not have any styles that the javadoc
 * generated tables have.
 *
 * <table summary="Summary" border cellpadding=3 cellspacing=1>
 * <caption>Summary of test table</caption>
 *  <tr>
 *    <td></td>
 *    <td align=center colspan = 2> First Element (Head)</td>
 *    <td align=center colspan = 2> Last Element (Tail)</td>
 *  </tr>
 *  <tr>
 *    <td></td>
 *    <td align=center><em>Throws exception</em></td>
 *    <td align=center><em>Special value</em></td>
 *    <td align=center><em>Throws exception</em></td>
 *    <td align=center><em>Special value</em></td>
 *  </tr>
 *  <tr>
 *    <td>Insert</td>
 *    <td>addFirst(e)</td>
 *    <td>offerFirst(e)</td>
 *    <td>addLast(e)</td>
 *    <td>offerLast(e)</td>
 *  </tr>
 *  <tr>
 *    <td>Remove</td>
 *    <td>removeFirst()</td>
 *    <td>pollFirst()</td>
 *    <td>removeLast()</td>
 *    <td>pollLast()</td>
 *  </tr>
 * </table>
 */
public class TestTable
{
    /**
     * Field in Class.
     */
    public String field;

    /**
     * Field constant in Class.
     */
    public static final int fieldCnst = 0;

    /**
     * Method in Class.
     */
    public void methodInClass(int i) {}

    /**
     * Deprecated method in Class.
     * @deprecated Do not use it.
     */
    public void deprMethod() {}

}
