/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

/** */
public class Test {
    /**
     * abc{@code def}ghi
     */
    public void no_pre() { }

    /**
     * abc{@code  def  }ghi
     */
    public void no_pre_extra_whitespace() { }

    /**
     * <pre> abc{@code  def  }ghi</pre>
     */
    public void in_pre() { }

    /**
     * xyz <pre> abc{@code  def  }ghi</pre>
     */
    public void pre_after_text() { }

    /**
     * xyz <pre> pqr </pre> abc{@code  def  }ghi
     */
    public void after_pre() { }

    /**
     * xyz <pre> pqr </pre> mno <pre> abc{@code  def  }ghi</pre>
     */
    public void back_in_pre() { }

    /**
     * Lorem ipsum dolor sit amet, consectetur adipiscing elit.
     * Example:  <pre>{@code
     *   line 0 @Override
     *   line 1 <T> void m(T t) {
     *   line 2     // do something with T
     *   line 3 }
     * }</pre>
     * and so it goes.
     */
    public void typical_usage_code() { }

    /**
     * Lorem ipsum dolor sit amet, consectetur adipiscing elit.
     * Example:  <pre>{@literal
     *   line 0 @Override
     *   line 1 <T> void m(T t) {
     *   line 2     // do something with T
     *   line 3 }
     * }</pre>
     * and so it goes.
     */
    public void typical_usage_literal() { }

    /**
     * Lorem ipsum dolor sit amet, consectetur adipiscing elit.
     * Example:  <pre>{@literal
     *   line 0 @Override
     *   line 1 <T> void m(T t) {
     *   line 2     // do something with T
     *   line 3 } }</pre>
     * and so it goes.
     */
    public void recommended_usage_literal() { }

    /**
     * abc {@code
     */
    public void bad_code_no_content() { }

    /**
     * abc {@code abc
     */
    public void bad_code_content() { }

    /**
     * Test for html in pre, note the spaces
     * <PRE>
     * <b   >id           </b   >
     * </PRE>
     */
    public void htmlAttrInPre() {}

    /**
     * More html tag outliers.
     * <pre>
     * {@literal @}Override
     * <code> some.function() </code>
     * </pre>
     *
     *
     */
    public void htmlAttrInPre1() {}
}
