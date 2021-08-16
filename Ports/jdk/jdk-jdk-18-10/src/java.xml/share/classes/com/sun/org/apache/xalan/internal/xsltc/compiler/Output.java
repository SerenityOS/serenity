/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.transform.OutputKeys;

import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.bcel.internal.generic.PUTFIELD;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import com.sun.org.apache.xml.internal.serializer.Encodings;
import com.sun.org.apache.xml.internal.utils.XML11Char;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 */
final class Output extends TopLevelElement {

    // TODO: use three-value variables for boolean values: true/false/default

    // These attributes are extracted from the xsl:output element. They also
    // appear as fields (with the same type, only public) in the translet
    private String  _version;
    private String  _method;
    private String  _encoding;
    private boolean _omitHeader = false;
    private String  _standalone;
    private String  _doctypePublic;
    private String  _doctypeSystem;
    private String  _cdata;
    private boolean _indent = false;
    private String  _mediaType;
    private String _indentamount;

    // Disables this output element (when other element has higher precedence)
    private boolean _disabled = false;

    // Some global constants
    private final static String STRING_SIG = "Ljava/lang/String;";
    private final static String XML_VERSION = "1.0";
    private final static String HTML_VERSION = "4.0";

    /**
     * Displays the contents of this element (for debugging)
     */
    public void display(int indent) {
        indent(indent);
        Util.println("Output " + _method);
    }

    /**
     * Disables this <xsl:output> element in case where there are some other
     * <xsl:output> element (from a different imported/included stylesheet)
     * with higher precedence.
     */
    public void disable() {
        _disabled = true;
    }

    public boolean enabled() {
        return !_disabled;
    }

    public String getCdata() {
        return _cdata;
    }

    public String getOutputMethod() {
        return _method;
    }

    private void transferAttribute(Output previous, String qname) {
        if (!hasAttribute(qname) && previous.hasAttribute(qname)) {
            addAttribute(qname, previous.getAttribute(qname));
        }
    }

    public void mergeOutput(Output previous) {
        // Transfer attributes from previous xsl:output
        transferAttribute(previous, "version");
        transferAttribute(previous, "method");
        transferAttribute(previous, "encoding");
        transferAttribute(previous, "doctype-system");
        transferAttribute(previous, "doctype-public");
        transferAttribute(previous, "media-type");
        transferAttribute(previous, "indent");
        transferAttribute(previous, "omit-xml-declaration");
        transferAttribute(previous, "standalone");

        // Merge cdata-section-elements
        if (previous.hasAttribute("cdata-section-elements")) {
            // addAttribute works as a setter if it already exists
            addAttribute("cdata-section-elements",
                previous.getAttribute("cdata-section-elements") + ' ' +
                getAttribute("cdata-section-elements"));
        }

        // Transfer non-standard attributes as well
        String prefix = lookupPrefix("http://xml.apache.org/xalan");
        if (prefix != null) {
            transferAttribute(previous, prefix + ':' + "indent-amount");
        }
        prefix = lookupPrefix("http://xml.apache.org/xslt");
        if (prefix != null) {
            transferAttribute(previous, prefix + ':' + "indent-amount");
        }
    }

    /**
     * Scans the attribute list for the xsl:output instruction
     */
    public void parseContents(Parser parser) {
        final Properties outputProperties = new Properties();

        // Ask the parser if it wants this <xsl:output> element
        parser.setOutput(this);

        // Do nothing if other <xsl:output> element has higher precedence
        if (_disabled) return;

        String attrib = null;

        // Get the output version
        _version = getAttribute("version");
        if (_version.equals(Constants.EMPTYSTRING)) {
            _version = null;
        }
        else {
            outputProperties.setProperty(OutputKeys.VERSION, _version);
        }

        // Get the output method - "xml", "html", "text" or <qname> (but not ncname)
        _method = getAttribute("method");
        if (_method.equals(Constants.EMPTYSTRING)) {
            _method = null;
        }
        if (_method != null) {
            _method = _method.toLowerCase();
            if ((_method.equals("xml"))||
                (_method.equals("html"))||
                (_method.equals("text"))||
                ((XML11Char.isXML11ValidQName(_method)&&(_method.indexOf(":") > 0)))) {
               outputProperties.setProperty(OutputKeys.METHOD, _method);
            } else {
                reportError(this, parser, ErrorMsg.INVALID_METHOD_IN_OUTPUT, _method);
            }
        }

        // Get the output encoding - any value accepted here
        _encoding = getAttribute("encoding");
        if (_encoding.equals(Constants.EMPTYSTRING)) {
            _encoding = null;
        }
        else {
            try {
                // Create a write to verify encoding support
                String canonicalEncoding;
                canonicalEncoding = Encodings.convertMime2JavaEncoding(_encoding);
                OutputStreamWriter writer =
                    new OutputStreamWriter(System.out, canonicalEncoding);
            }
            catch (java.io.UnsupportedEncodingException e) {
                ErrorMsg msg = new ErrorMsg(ErrorMsg.UNSUPPORTED_ENCODING,
                                            _encoding, this);
                parser.reportError(Constants.WARNING, msg);
            }
            outputProperties.setProperty(OutputKeys.ENCODING, _encoding);
        }

        // Should the XML header be omitted - translate to true/false
        attrib = getAttribute("omit-xml-declaration");
        if (!attrib.equals(Constants.EMPTYSTRING)) {
            if (attrib.equals("yes")) {
                _omitHeader = true;
            }
            outputProperties.setProperty(OutputKeys.OMIT_XML_DECLARATION, attrib);
        }

        // Add 'standalone' decaration to output - use text as is
        _standalone = getAttribute("standalone");
        if (_standalone.equals(Constants.EMPTYSTRING)) {
            _standalone = null;
        }
        else {
            outputProperties.setProperty(OutputKeys.STANDALONE, _standalone);
        }

        // Get system/public identifiers for output DOCTYPE declaration
        _doctypeSystem = getAttribute("doctype-system");
        if (_doctypeSystem.equals(Constants.EMPTYSTRING)) {
            _doctypeSystem = null;
        }
        else {
            outputProperties.setProperty(OutputKeys.DOCTYPE_SYSTEM, _doctypeSystem);
        }


        _doctypePublic = getAttribute("doctype-public");
        if (_doctypePublic.equals(Constants.EMPTYSTRING)) {
            _doctypePublic = null;
        }
        else {
            outputProperties.setProperty(OutputKeys.DOCTYPE_PUBLIC, _doctypePublic);
        }

        // Names the elements of whose text contents should be output as CDATA
        _cdata = getAttribute("cdata-section-elements");
        if (_cdata.equals(Constants.EMPTYSTRING)) {
            _cdata = null;
        }
        else {
            StringBuffer expandedNames = new StringBuffer();
            StringTokenizer tokens = new StringTokenizer(_cdata);

            // Make sure to store names in expanded form
            while (tokens.hasMoreTokens()) {
                String qname = tokens.nextToken();
                if (!XML11Char.isXML11ValidQName(qname)) {
                    ErrorMsg err = new ErrorMsg(ErrorMsg.INVALID_QNAME_ERR, qname, this);
                    parser.reportError(Constants.ERROR, err);
                }
                expandedNames.append(
                   parser.getQName(qname).toString()).append(' ');
            }
            _cdata = expandedNames.toString();
            outputProperties.setProperty(OutputKeys.CDATA_SECTION_ELEMENTS,
                _cdata);
        }

        // Get the indent setting - only has effect for xml and html output
        attrib = getAttribute("indent");
        if (!attrib.equals(EMPTYSTRING)) {
            if (attrib.equals("yes")) {
                _indent = true;
            }
            outputProperties.setProperty(OutputKeys.INDENT, attrib);
        }
        else if (_method != null && _method.equals("html")) {
            _indent = true;
        }

        // indent-amount: extension attribute of xsl:output
        _indentamount = getAttribute(
            lookupPrefix("http://xml.apache.org/xalan"), "indent-amount");
        //  Hack for supporting Old Namespace URI.
        if (_indentamount.equals(EMPTYSTRING)){
            _indentamount = getAttribute(
                lookupPrefix("http://xml.apache.org/xslt"), "indent-amount");
        }
        if (!_indentamount.equals(EMPTYSTRING)) {
            outputProperties.setProperty("indent_amount", _indentamount);
        }

        // Get the MIME type for the output file
        _mediaType = getAttribute("media-type");
        if (_mediaType.equals(Constants.EMPTYSTRING)) {
            _mediaType = null;
        }
        else {
            outputProperties.setProperty(OutputKeys.MEDIA_TYPE, _mediaType);
        }

        // Implied properties
        if (_method != null) {
            if (_method.equals("html")) {
                if (_version == null) {
                    _version = HTML_VERSION;
                }
                if (_mediaType == null) {
                    _mediaType = "text/html";
                }
            }
            else if (_method.equals("text")) {
                if (_mediaType == null) {
                    _mediaType = "text/plain";
                }
            }
        }

        // Set output properties in current stylesheet
        parser.getCurrentStylesheet().setOutputProperties(outputProperties);
    }

    /**
     * Compile code that passes the information in this <xsl:output> element
     * to the appropriate fields in the translet
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {

        // Do nothing if other <xsl:output> element has higher precedence
        if (_disabled) return;

        ConstantPoolGen cpg = classGen.getConstantPool();
        InstructionList il = methodGen.getInstructionList();

        int field = 0;
        il.append(classGen.loadTranslet());

        // Only update _version field if set and different from default
        if ((_version != null) && (!_version.equals(XML_VERSION))) {
            field = cpg.addFieldref(TRANSLET_CLASS, "_version", STRING_SIG);
            il.append(DUP);
            il.append(new PUSH(cpg, _version));
            il.append(new PUTFIELD(field));
        }

        // Only update _method field if "method" attribute used
        if (_method != null) {
            field = cpg.addFieldref(TRANSLET_CLASS, "_method", STRING_SIG);
            il.append(DUP);
            il.append(new PUSH(cpg, _method));
            il.append(new PUTFIELD(field));
        }

        // Only update if _encoding field is "encoding" attribute used
        if (_encoding != null) {
            field = cpg.addFieldref(TRANSLET_CLASS, "_encoding", STRING_SIG);
            il.append(DUP);
            il.append(new PUSH(cpg, _encoding));
            il.append(new PUTFIELD(field));
        }

        // Only update if "omit-xml-declaration" used and set to 'yes'
        if (_omitHeader) {
            field = cpg.addFieldref(TRANSLET_CLASS, "_omitHeader", "Z");
            il.append(DUP);
            il.append(new PUSH(cpg, _omitHeader));
            il.append(new PUTFIELD(field));
        }

        // Add 'standalone' decaration to output - use text as is
        if (_standalone != null) {
            field = cpg.addFieldref(TRANSLET_CLASS, "_standalone", STRING_SIG);
            il.append(DUP);
            il.append(new PUSH(cpg, _standalone));
            il.append(new PUTFIELD(field));
        }

        // Set system/public doctype only if both are set
        field = cpg.addFieldref(TRANSLET_CLASS,"_doctypeSystem",STRING_SIG);
        il.append(DUP);
        il.append(new PUSH(cpg, _doctypeSystem));
        il.append(new PUTFIELD(field));
        field = cpg.addFieldref(TRANSLET_CLASS,"_doctypePublic",STRING_SIG);
        il.append(DUP);
        il.append(new PUSH(cpg, _doctypePublic));
        il.append(new PUTFIELD(field));

        // Add 'medye-type' decaration to output - if used
        if (_mediaType != null) {
            field = cpg.addFieldref(TRANSLET_CLASS, "_mediaType", STRING_SIG);
            il.append(DUP);
            il.append(new PUSH(cpg, _mediaType));
            il.append(new PUTFIELD(field));
        }

        // Compile code to set output indentation on/off
        if (_indent) {
            field = cpg.addFieldref(TRANSLET_CLASS, "_indent", "Z");
            il.append(DUP);
            il.append(new PUSH(cpg, _indent));
            il.append(new PUTFIELD(field));
        }

        //Compile code to set indent amount.
        if(_indentamount != null && !_indentamount.equals(EMPTYSTRING)){
            field = cpg.addFieldref(TRANSLET_CLASS, "_indentamount", "I");
            il.append(DUP);
            il.append(new PUSH(cpg, Integer.parseInt(_indentamount)));
            il.append(new PUTFIELD(field));
        }

        // Forward to the translet any elements that should be output as CDATA
        if (_cdata != null) {
            int index = cpg.addMethodref(TRANSLET_CLASS,
                                         "addCdataElement",
                                         "(Ljava/lang/String;)V");

            StringTokenizer tokens = new StringTokenizer(_cdata);
            while (tokens.hasMoreTokens()) {
                il.append(DUP);
                il.append(new PUSH(cpg, tokens.nextToken()));
                il.append(new INVOKEVIRTUAL(index));
            }
        }
        il.append(POP); // Cleanup - pop last translet reference off stack
    }

}
