/*
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;


/**
 * Represents runtime exceptions thrown by MBean methods in
 * the agent. It "wraps" the actual <CODE>java.lang.RuntimeException</CODE> exception thrown.
 * This exception will be built by the MBeanServer when a call to an
 * MBean method throws a runtime exception.
 *
 * @since 1.5
 */
public class RuntimeMBeanException extends JMRuntimeException   {

    /* Serial version */
    private static final long serialVersionUID = 5274912751982730171L;

    /**
     * @serial The encapsulated {@link RuntimeException}
     */
    private java.lang.RuntimeException runtimeException ;


    /**
     * Creates a <CODE>RuntimeMBeanException</CODE> that wraps the actual <CODE>java.lang.RuntimeException</CODE>.
     *
     * @param e the wrapped exception.
     */
    public RuntimeMBeanException(java.lang.RuntimeException e) {
        super() ;
        runtimeException = e ;
    }

    /**
     * Creates a <CODE>RuntimeMBeanException</CODE> that wraps the actual <CODE>java.lang.RuntimeException</CODE> with
     * a detailed message.
     *
     * @param e the wrapped exception.
     * @param message the detail message.
     */
    public RuntimeMBeanException(java.lang.RuntimeException e, String message) {
        super(message) ;
        runtimeException = e ;
    }

    /**
     * Returns the actual {@link RuntimeException} thrown.
     *
     * @return the wrapped {@link RuntimeException}.
     */
    public java.lang.RuntimeException getTargetException()  {
        return runtimeException ;
    }

    /**
     * Returns the actual {@link RuntimeException} thrown.
     *
     * @return the wrapped {@link RuntimeException}.
     */
    public Throwable getCause() {
        return runtimeException;
    }
}
