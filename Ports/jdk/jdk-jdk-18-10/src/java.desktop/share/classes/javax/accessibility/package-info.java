/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines a contract between user-interface components and an assistive
 * technology that provides access to those components. If a Java application
 * fully supports the Java Accessibility API, then it should be compatible with,
 * and friendly toward, assistive technologies such as screen readers, screen
 * magnifiers, etc. With a Java application that fully supports the Java
 * Accessibility API, no screen reader off screen model would be necessary
 * because the API provides all of the information normally contained in an off
 * screen model.
 * <p>
 * The Java Accessibility API package consists of 8 Java programming language
 * interfaces, and 6 Java programming language classes. These are described
 * below.
 *
 * <h2><a id="Accessible"></a><a href="Accessible.html">Interface
 * Accessible</a></h2>
 * <a href="Accessible.html">Interface Accessible</a> is the main interface of
 * the Java Accessibility API. All components that support the Java
 * Accessibility API must implement this interface. It contains a single method,
 * {@code getAccessibleContext}, that returns an instance of the class
 * <a href="#AccessibleContext">AccessibleContext</a>. Sun thinks that
 * implementing this interface is the absolute minimum requirement of every
 * object that is part of the user interface of a Java application, if that
 * program is to be compatible with assistive technologies.
 *
 * <h2><a id="AccessibleContext"></a><a href="AccessibleContext.html">Class
 * AccessibleContext</a></h2>
 * <a href="AccessibleContext.html">AccessibleContext</a> represents the minimum
 * information all accessible objects return and is obtained by calling the
 * {@code getAccessibleContext} method on an object that implements the
 * <a href="#Accessible">Accessible</a> interface. This information includes the
 * accessible name, description, <a href="#AccessibleRole">role</a>, and
 * <a href="#AccessibleState">state</a> of the object, as well as information
 * about the parent and children of the object.&nbsp; In addition,
 * JavaBeans property change support is also included to allow assistive
 * technologies learn when the values of the accessible properties change.
 * AccessibleContext also contains methods for obtaining more specific
 * accessibility information about a component. If the component supports it,
 * these methods will return an object that implements one or more of the
 * following interfaces:
 * <ul>
 *     <li><b><a href="#AccessibleAction">AccessibleAction</a></b> - the object
 *     can perform one or more actions. This interface provides the standard
 *     mechanism for an assistive technology to determine what those actions are
 *     and tell the object to perform those actions. Any object that can be
 *     manipulated should return an object that implements this interface when
 *     the {@code getAccessibleAction} method is called on an AccessibleContext.
 *     </li>
 *     <li><b><a href="#AccessibleComponent">AccessibleComponent</a></b> - the
 *     object has a graphical representation. This interface provides the
 *     standard mechanism for an assistive technology to determine and set the
 *     graphical representation of the object. Any object that is rendered on
 *     the screen should return an object that implements this interface when
 *     the {@code getAccessibleComponent} method is called on an
 *     AccessibleContext.</li>
 *     <li><b><a href="#AccessibleSelection">AccessibleSelection</a></b> - the
 *     object allows its children to be selected. This interface provides the
 *     standard mechanism for an assistive technology to determine the currently
 *     selected children as well as modify the selection set. Any object that
 *     has children that can be selected should return an object that implements
 *     this interface when the {@code getAccessibleSelection} method is called
 *     on an AccessibleContext.</li>
 *     <li><b><a href="#AccessibleText">AccessibleText</a></b> - the object
 *     presents editable textual information on the display. This interface
 *     provides the standard mechanism for an assistive technology to access
 *     that text via its content, attributes, and spatial location. Any object
 *     that contains editable text should return an object that implements this
 *     interface when the {@code getAccessibleText} method is called on an
 *     AccessibleContext.</li>
 *     <li><b><a href="#AccessibleHypertext">AccessibleHypertext</a></b> - the
 *     object presents hypertext information on the display. This interface
 *     provides the standard mechanism for an assistive technology to access that
 *     hypertext via its content, attributes, and spatial location. Any object
 *     that contains hypertext should return an object that implements this
 *     interface when the {@code getAccessibleText} method is called on an
 *     AccessibleContext.</li>
 *     <li><b><a href="#AccessibleValue">AccessibleValue</a></b> - the object
 *     supports a numerical value. This interface provides the standard
 *     mechanism for an assistive technology to determine and set the current
 *     value of the object, as well as the minimum and maximum values. Any
 *     object that supports a numerical value should return an object that
 *     implements this interface when the {@code getAccessibleValue} method is
 *     called on an AccessibleContext.</li>
 * </ul>
 *
 * <h2><a id="AccessibleRole"></a><a href="AccessibleRole.html">Class
 * AccessibleRole</a></h2>
 * This class encapsulates the Accessible object's role in the user interface
 * and is obtained by calling the {@code getAccessibleRole} method on an
 * <a href="#AccessibleContext">AccessibleContext</a>. Accessible roles include
 * "Check box", "Menu Item", "Panel", etc. These roles are identified by the
 * constants in this class such as {@code AccessibleRole.CHECK_BOX,
 * AccessibleRole.MENU_ITEM,} and {@code AccessibleRole.PANEL}. The constants in
 * this class present a strongly typed enumeration of common object roles. A
 * public constructor for this class has been purposely omitted and applications
 * should use one of the constants from this class. Although this class
 * pre-defines a large list of standard roles, it is extensible so additional
 * programmer-defined roles can be added in the future without needing to modify
 * the base class.
 *
 * <h2><a id="AccessibleState"></a><a href="AccessibleState.html">Class
 * AccessibleState</a></h2>
 * This class encapsulates a particular state of the Accessible object.
 * Accessible states include things like "Armed", "Busy", "Checked", "Focused",
 * etc. These roles are identified by the constants in this class such as
 * {@code AccessibleState.ARMED, AccessibleState.BUSY, AccessibleState.CHECKED,}
 * and {@code AccessibleState.FOCUSED}. The sum of all the states of an
 * Accessible object is called the
 * <a href="#AccessibleStateSet">AccessibleStateSet</a>, and can be obtained by
 * calling the {@code getAccessibleStateSet} method on an
 * <a href="#AccessibleContext">AccessibleContext</a>.
 * <p>
 * The constants in this class present a strongly typed enumeration of common
 * object roles. A public constructor for this class has been purposely omitted
 * and applications should use one of the constants from this class. Although
 * this class pre-defines a large list of standard roles, it is extensible so
 * additional, programmer-defined roles can be added in the future without
 * needing to modify the base class.
 *
 * <h2><a id="AccessibleStateSet"></a><a href="AccessibleStateSet.html">Class
 * AccessibleStateSet</a></h2>
 * This class encapsulates a collection of states of the Accessible object and
 * is obtained by calling the {@code getAccessibleStateSet} method on an
 * <a href="#AccessibleContext">AccessibleContext</a>. Since an object might
 * have multiple states (e.g. it might be both "Checked" and "Focused"), this
 * class is needed to encapsulate a collection of these states. Methods in the
 * class provide for retrieving the individual
 * <a href="#AccessibleState">AccessibleStates</a> on the state set.
 *
 * <h2><a id="AccessibleBundle"></a><a href="AccessibleBundle.html">Class
 * AccessibleBundle</a></h2>
 * This class is used to maintain a strongly typed enumeration. It is the super
 * class of both the <a href="#AccessibleRole">AccessibleRole</a> and
 * <a href="#AccessibleState">AccessibleState</a> classes. Programmers normally
 * do not interact with this class directly, but will instead use the
 * <a href="#AccessibleRole">AccessibleRole</a> and
 * <a href="#AccessibleState">AccessibleState</a> classes.
 *
 * <h2><a id="AccessibleAction"></a><a href="AccessibleAction.html">Interface
 * AccessibleAction</a></h2>
 * The <a href="AccessibleAction.html">AccessibleAction</a> interface should be
 * supported by any object that can perform one or more actions. This interface
 * provides the standard mechanism for an assistive technology to determine what
 * those actions are as well as tell the object to perform those actions. Any
 * object that can be manipulated should support this interface.
 * <p>
 * Applications can determine if an object supports the AccessibleAction
 * interface by first obtaining its
 * <a href="#AccessibleContext">AccessibleContext</a> (see
 * <a href="#Accessible">Accessible</a>) and then calling the
 * {@code getAccessibleAction} method of
 * <a href="#AccessibleContext">AccessibleContext</a>. If the return value is
 * not {@code null}, the object supports this interface.
 *
 * <h2> <a id="AccessibleComponent"></a><a href="AccessibleComponent.html">
 * Interface AccessibleComponent</a></h2>
 * The <a href="AccessibleComponent.html">AccessibleComponent</a> interface
 * should be supported by any object that is rendered on the screen. This
 * interface provides the standard mechanism for an assistive technology to
 * determine and set the graphical representation of an object. <p>Applications
 * can determine if an object supports the AccessibleComponent interface by
 * first obtaining its <a href="#AccessibleContext">AccessibleContext</a> (see
 * <a href="#Accessible">Accessible</a>) and then calling the
 * {@code getAccessibleComponent} method of
 * <a href="#AccessibleContext">AccessibleContext</a>. If the return value is
 * not {@code null}, the object supports this interface.
 *
 * <h2><a id="AccessibleSelection"></a><a href="AccessibleSelection.html">
 * Interface AccessibleSelection</a></h2>
 * The <a href="AccessibleSelection.html">AccessibleSelection</a> interface
 * provides the standard mechanism for an assistive technology to determine what
 * the current selected children are, as well as modify the selection set. Any
 * object that has children that can be selected should support this the
 * AccessibleSelection interface.
 * <p>
 * Applications can determine if an object supports the AccessibleSelection
 * interface by first obtaining its
 * <a href="#AccessibleContext">AccessibleContext</a> (see
 * <a href="#Accessible">Accessible</a>) and then calling the
 * {@code getAccessibleSelection} method of
 * <a href="#AccessibleContext">AccessibleContext</a>. If the return value is
 * not {@code null}, the object supports this interface.
 *
 * <h2><a id="AccessibleText"></a><a href="AccessibleText.html">Interface
 * AccessibleText</a></h2>
 * Interface <a href="AccessibleText.html">AccessibleText</a> is the contract
 * for making rich, editable text Accessible. Not all text displayed on the
 * screen is rich and editable (e.g. text contained in buttons, labels, menus,
 * etc., which users aren't expected to manipulate). However, objects containing
 * editable text must implement interface AccessibleText if they are to
 * interoperate with assistive technologies.
 * <p>
 * This interface provides support for going between pixel coordinates and the
 * text at a given pixel coordinate, for retrieving the letter, word, and
 * sentence at, before, or after a given position in the text. This interface
 * provides support for retrieving the attributes of the character at a given
 * position in the text (font, font size, style, etc.), as well as getting the
 * selected text (if any), the length of the text, and the location of the text
 * caret.
 * <p>
 * Applications can determine if an object supports the AccessibleText interface
 * by first obtaining its <a href="#AccessibleContext">AccessibleContext</a>
 * (see <a href="#Accessible">Accessible</a>) and then calling the
 * {@code getAccessibleText} method of
 * <a href="#AccessibleContext">AccessibleContext</a>. If the return value is
 * not {@code null}, the object supports this interface.
 *
 * <h2><a id="AccessibleHypertext"></a> <a href="AccessibleHypertext.html">
 * Interface AccessibleHypertext</a></h2>
 * The <a href="AccessibleHypertext.html">AccessibleHypertext</a> interface
 * should be supported by any object that presents hypertext information on the
 * display. This interface provides the standard mechanism for an assistive
 * technology to access that text via its content, attributes, and spatial
 * location. It also provides standard mechanisms for manipulating
 * <a href="#AccessibleHyperlink">hyperlinks</a>. Applications can determine if
 * an object supports the AccessibleHypertext interface by first obtaining its
 * <a href="#AccessibleContext">AccessibleContext</a> (see
 * <a href="#Accessible">Accessible</a>) and then calling the
 * AccessibleContext.getAccessibleText() method of
 * <a href="#AccessibleContext">AccessibleContext</a>. If the return value is a
 * class which extends AccessibleHypertext, then that object supports
 * AccessibleHypertext.
 *
 * <h2><a id="AccessibleHyperlink"></a><a href="AccessibleHyperlink.html">
 * Interface AccessibleHyperlink</a></h2>
 * An object that is a hyperlink should support the
 * <a href="AccessibleHyperlink.html">AccessibleHyperlink</a> interface.&nbsp;
 * An object that implements this interface will be returned by calling the
 * getLink method on an <a href="#AccessibleHypertext">AccessibleHypertext</a>
 * object.
 *
 * <h2><a id="AccessibleValue"></a><a href="AccessibleValue.html">Interface
 * AccessibleValue</a></h2>
 * The <a href="AccessibleValue.html">AccessibleValue</a> interface should be
 * supported by any object that supports a numerical value (e.g., a scroll bar).
 * This interface provides the standard mechanism for an assistive technology to
 * determine and set the numerical value as well as get the minimum and maximum
 * values.
 * <p>
 * Applications can determine if an object supports the AccessibleValue
 * interface by first obtaining its
 * <a href="#AccessibleContext">AccessibleContext</a> (see
 * <a href="#Accessible">Accessible</a>) and then calling the
 * {@code getAccessibleValue} method of
 * <a href="#AccessibleContext">AccessibleContext</a>. If the return value is
 * not {@code null}, the object supports this interface.
 *
 * @since 1.2
 */
package javax.accessibility;
