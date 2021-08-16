/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2000 World Wide Web Consortium,
 * (Massachusetts Institute of Technology, Institut National de
 * Recherche en Informatique et en Automatique, Keio University). All
 * Rights Reserved. This program is distributed under the W3C's Software
 * Intellectual Property License. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See W3C License http://www.w3.org/Consortium/Legal/ for more
 * details.
 */

package org.w3c.dom.html;

/**
 *  Form control.  Note. Depending upon the environment in which the page is
 * being viewed, the value property may be read-only for the file upload
 * input type. For the "password" input type, the actual value returned may
 * be masked to prevent unauthorized use. See the  INPUT element definition
 * in HTML 4.0.
 * <p>See also the <a href='http://www.w3.org/TR/2000/CR-DOM-Level-2-20000510'>Document Object Model (DOM) Level 2 Specification</a>.
 *
 * @since 1.4, DOM Level 2
 */
public interface HTMLInputElement extends HTMLElement {
    /**
     *  When the <code>type</code> attribute of the element has the value
     * "Text", "File" or "Password", this represents the HTML value attribute
     * of the element. The value of this attribute does not change if the
     * contents of the corresponding form control, in an interactive user
     * agent, changes. Changing this attribute, however, resets the contents
     * of the form control. See the  value attribute definition in HTML 4.0.
     */
    public String getDefaultValue();
    public void setDefaultValue(String defaultValue);

    /**
     *  When <code>type</code> has the value "Radio" or "Checkbox", this
     * represents the HTML checked attribute of the element. The value of
     * this attribute does not change if the state of the corresponding form
     * control, in an interactive user agent, changes. Changes to this
     * attribute, however, resets the state of the form control. See the
     * checked attribute definition in HTML 4.0.
     */
    public boolean getDefaultChecked();
    public void setDefaultChecked(boolean defaultChecked);

    /**
     *  Returns the <code>FORM</code> element containing this control. Returns
     * <code>null</code> if this control is not within the context of a form.
     */
    public HTMLFormElement getForm();

    /**
     *  A comma-separated list of content types that a server processing this
     * form will handle correctly. See the  accept attribute definition in
     * HTML 4.0.
     */
    public String getAccept();
    public void setAccept(String accept);

    /**
     *  A single character access key to give access to the form control. See
     * the  accesskey attribute definition in HTML 4.0.
     */
    public String getAccessKey();
    public void setAccessKey(String accessKey);

    /**
     *  Aligns this object (vertically or horizontally)  with respect to its
     * surrounding text. See the  align attribute definition in HTML 4.0.
     * This attribute is deprecated in HTML 4.0.
     */
    public String getAlign();
    public void setAlign(String align);

    /**
     *  Alternate text for user agents not rendering the normal content of
     * this element. See the  alt attribute definition in HTML 4.0.
     */
    public String getAlt();
    public void setAlt(String alt);

    /**
     *  When the <code>type</code> attribute of the element has the value
     * "Radio" or "Checkbox", this represents the current state of the form
     * control, in an interactive user agent. Changes to this attribute
     * change the state of the form control, but do not change the value of
     * the HTML value attribute of the element.
     */
    public boolean getChecked();
    public void setChecked(boolean checked);

    /**
     *  The control is unavailable in this context. See the  disabled
     * attribute definition in HTML 4.0.
     */
    public boolean getDisabled();
    public void setDisabled(boolean disabled);

    /**
     *  Maximum number of characters for text fields, when <code>type</code>
     * has the value "Text" or "Password". See the  maxlength attribute
     * definition in HTML 4.0.
     */
    public int getMaxLength();
    public void setMaxLength(int maxLength);

    /**
     *  Form control or object name when submitted with a form. See the  name
     * attribute definition in HTML 4.0.
     */
    public String getName();
    public void setName(String name);

    /**
     *  This control is read-only. Relevant only when <code>type</code> has
     * the value "Text" or "Password". See the  readonly attribute definition
     * in HTML 4.0.
     */
    public boolean getReadOnly();
    public void setReadOnly(boolean readOnly);

    /**
     *  Size information. The precise meaning is specific to each type of
     * field.  See the  size attribute definition in HTML 4.0.
     */
    public String getSize();
    public void setSize(String size);

    /**
     *  When the <code>type</code> attribute has the value "Image", this
     * attribute specifies the location of the image to be used to decorate
     * the graphical submit button. See the  src attribute definition in HTML
     * 4.0.
     */
    public String getSrc();
    public void setSrc(String src);

    /**
     *  Index that represents the element's position in the tabbing order. See
     * the  tabindex attribute definition in HTML 4.0.
     */
    public int getTabIndex();
    public void setTabIndex(int tabIndex);

    /**
     *  The type of control created. See the  type attribute definition in
     * HTML 4.0.
     */
    public String getType();

    /**
     *  Use client-side image map. See the  usemap attribute definition in
     * HTML 4.0.
     */
    public String getUseMap();
    public void setUseMap(String useMap);

    /**
     *  When the <code>type</code> attribute of the element has the value
     * "Text", "File" or "Password", this represents the current contents of
     * the corresponding form control, in an interactive user agent. Changing
     * this attribute changes the contents of the form control, but does not
     * change the value of the HTML value attribute of the element. When the
     * <code>type</code> attribute of the element has the value "Button",
     * "Hidden", "Submit", "Reset", "Image", "Checkbox" or "Radio", this
     * represents the HTML value attribute of the element. See the  value
     * attribute definition in HTML 4.0.
     */
    public String getValue();
    public void setValue(String value);

    /**
     *  Removes keyboard focus from this element.
     */
    public void blur();

    /**
     *  Gives keyboard focus to this element.
     */
    public void focus();

    /**
     *  Select the contents of the text area. For <code>INPUT</code> elements
     * whose <code>type</code> attribute has one of the following values:
     * "Text", "File", or "Password".
     */
    public void select();

    /**
     *  Simulate a mouse-click. For <code>INPUT</code> elements whose
     * <code>type</code> attribute has one of the following values: "Button",
     * "Checkbox", "Radio", "Reset", or "Submit".
     */
    public void click();

}
