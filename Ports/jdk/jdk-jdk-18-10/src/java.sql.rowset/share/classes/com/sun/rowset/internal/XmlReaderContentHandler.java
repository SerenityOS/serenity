/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.rowset.internal;

import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.sql.*;
import javax.sql.*;

import javax.sql.rowset.*;
import com.sun.rowset.*;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * The document handler that receives parse events that an XML parser sends while it
 * is parsing an XML document representing a <code>WebRowSet</code> object. The
 * parser sends strings to this <code>XmlReaderContentHandler</code> and then uses
 * these strings as arguments for the <code>XmlReaderContentHandler</code> methods
 * it invokes. The final goal of the SAX parser working with an
 * <code>XmlReaderContentHandler</code> object is to read an XML document that represents
 * a <code>RowSet</code> object.
 * <P>
 * A rowset consists of its properties, metadata, and data values. An XML document
 * representating a rowset includes the values in these three categories along with
 * appropriate XML tags to identify them.  It also includes a top-level XML tag for
 * the rowset and three section tags identifying the three categories of values.
 * <P>
 * The tags in an XML document are hierarchical.
 * This means that the top-level tag, <code>RowSet</code>, is
 * followed by the three sections with appropriate tags, which are in turn each
 * followed by their constituent elements. For example, the <code>properties</code>
 * element will be followed by an element for each of the properties listed in
 * in this <code>XmlReaderContentHandler</code> object's <code>properties</code>
 * field.  The content of the other two fields, <code>colDef</code>, which lists
 * the rowset's metadata elements, and <code>data</code>, which lists the rowset's data
 * elements, are handled similarly .
 * <P>
 * This implementation of <code>XmlReaderContentHandler</code> provides the means for the
 * parser to determine which elements need to have a value set and then to set
 * those values. The methods in this class are all called by the parser; an
 * application programmer never calls them directly.
 *
 */

public class XmlReaderContentHandler extends DefaultHandler {

    private HashMap <String, Integer> propMap;
    private HashMap <String, Integer> colDefMap;
    private HashMap <String, Integer> dataMap;

    private HashMap<String,Class<?>> typeMap;

    private Vector<Object[]> updates;
    private Vector<String> keyCols;

    private String columnValue;
    private String propertyValue;
    private String metaDataValue;

    private int tag;
    private int state;

    private WebRowSetImpl rs;
    private boolean nullVal;
    private boolean emptyStringVal;
    private RowSetMetaData md;
    private int idx;
    private String lastval;
    private String Key_map;
    private String Value_map;
    private String tempStr;
    private String tempUpdate;
    private String tempCommand;
    private Object [] upd;

    /**
     * A list of the properties for a rowset. There is a constant defined to
     * correspond to each of these properties so that a <code>HashMap</code>
     * object can be created to map the properties, which are strings, to
     * the constants, which are integers.
     */
    private String [] properties = {"command", "concurrency", "datasource",
                            "escape-processing", "fetch-direction", "fetch-size",
                            "isolation-level", "key-columns", "map",
                            "max-field-size", "max-rows", "query-timeout",
                            "read-only", "rowset-type", "show-deleted",
                            "table-name", "url", "null", "column", "type",
                            "class", "sync-provider", "sync-provider-name",
                             "sync-provider-vendor", "sync-provider-version",
                             "sync-provider-grade","data-source-lock"};

    /**
     * A constant representing the tag for the command property.
     */
    private static final int CommandTag = 0;

    /**
     * A constant representing the tag for the concurrency property.
     */
    private static final int ConcurrencyTag = 1;

    /**
     * A constant representing the tag for the datasource property.
     */
    private static final int DatasourceTag = 2;

    /**
     * A constant representing the tag for the escape-processing property.
     */
    private static final int EscapeProcessingTag = 3;

    /**
     * A constant representing the tag for the fetch-direction property.
     */
    private static final int FetchDirectionTag = 4;

    /**
     * A constant representing the tag for the fetch-size property.
     */
    private static final int FetchSizeTag = 5;

    /**
     * A constant representing the tag for the isolation-level property
     */
    private static final int IsolationLevelTag = 6;

    /**
     * A constant representing the tag for the key-columns property.
     */
    private static final int KeycolsTag = 7;

    /**
     * A constant representing the tag for the map property.
     * This map is the type map that specifies the custom mapping
     * for an SQL user-defined type.
     */
    private static final int MapTag = 8;

    /**
     * A constant representing the tag for the max-field-size property.
     */
    private static final int MaxFieldSizeTag = 9;

    /**
     * A constant representing the tag for the max-rows property.
     */
    private static final int MaxRowsTag = 10;

    /**
     * A constant representing the tag for the query-timeout property.
     */
    private static final int QueryTimeoutTag = 11;

    /**
     * A constant representing the tag for the read-only property.
     */
    private static final int ReadOnlyTag = 12;

    /**
     * A constant representing the tag for the rowset-type property.
     */
    private static final int RowsetTypeTag = 13;

    /**
     * A constant representing the tag for the show-deleted property.
     */
    private static final int ShowDeletedTag = 14;

    /**
     * A constant representing the tag for the table-name property.
     */
    private static final int TableNameTag = 15;

    /**
     * A constant representing the tag for the URL property.
     */
    private static final int UrlTag = 16;

    /**
     * A constant representing the tag for the null property.
     */
    private static final int PropNullTag = 17;

    /**
     * A constant representing the tag for the column property.
     */
    private static final int PropColumnTag = 18;

    /**
     * A constant representing the tag for the type property.
     */
    private static final int PropTypeTag = 19;

    /**
     * A constant representing the tag for the class property.
     */
    private static final int PropClassTag = 20;

    /**
     * A constant representing the tag for the sync-provider.
     */
    private static final int SyncProviderTag = 21;

    /**
     * A constant representing the tag for the sync-provider
     * name
     */
    private static final int SyncProviderNameTag = 22;

    /**
     * A constant representing the tag for the sync-provider
     * vendor tag.
     */
    private static final int SyncProviderVendorTag = 23;

    /**
     * A constant representing the tag for the sync-provider
     * version tag.
     */
    private static final int SyncProviderVersionTag = 24;

    /**
     * A constant representing the tag for the sync-provider
     * grade tag.
     */
    private static final int SyncProviderGradeTag = 25;

    /**
     * A constant representing the tag for the data source lock.
     */
    private static final int DataSourceLock = 26;

    /**
     * A listing of the kinds of metadata information available about
     * the columns in a <code>WebRowSet</code> object.
     */
    private String [] colDef = {"column-count", "column-definition", "column-index",
                        "auto-increment", "case-sensitive", "currency",
                        "nullable", "signed", "searchable",
                        "column-display-size", "column-label", "column-name",
                        "schema-name", "column-precision", "column-scale",
                        "table-name", "catalog-name", "column-type",
                        "column-type-name", "null"};


    /**
     * A constant representing the tag for column-count.
     */
    private static final int ColumnCountTag = 0;

    /**
     * A constant representing the tag for column-definition.
     */
    private static final int ColumnDefinitionTag = 1;

    /**
     * A constant representing the tag for column-index.
     */
    private static final int ColumnIndexTag = 2;

    /**
     * A constant representing the tag for auto-increment.
     */
    private static final int AutoIncrementTag = 3;

    /**
     * A constant representing the tag for case-sensitive.
     */
    private static final int CaseSensitiveTag = 4;

    /**
     * A constant representing the tag for currency.
     */
    private static final int CurrencyTag = 5;

    /**
     * A constant representing the tag for nullable.
     */
    private static final int NullableTag = 6;

    /**
     * A constant representing the tag for signed.
     */
    private static final int SignedTag = 7;

    /**
     * A constant representing the tag for searchable.
     */
    private static final int SearchableTag = 8;

    /**
     * A constant representing the tag for column-display-size.
     */
    private static final int ColumnDisplaySizeTag = 9;

    /**
     * A constant representing the tag for column-label.
     */
    private static final int ColumnLabelTag = 10;

    /**
     * A constant representing the tag for column-name.
     */
    private static final int ColumnNameTag = 11;

    /**
     * A constant representing the tag for schema-name.
     */
    private static final int SchemaNameTag = 12;

    /**
     * A constant representing the tag for column-precision.
     */
    private static final int ColumnPrecisionTag = 13;

    /**
     * A constant representing the tag for column-scale.
     */
    private static final int ColumnScaleTag = 14;

    /**
     * A constant representing the tag for table-name.
     */
    private static final int MetaTableNameTag = 15;

    /**
     * A constant representing the tag for catalog-name.
     */
    private static final int CatalogNameTag = 16;

    /**
     * A constant representing the tag for column-type.
     */
    private static final int ColumnTypeTag = 17;

    /**
     * A constant representing the tag for column-type-name.
     */
    private static final int ColumnTypeNameTag = 18;

    /**
     * A constant representing the tag for null.
     */
    private static final int MetaNullTag = 19;

    private String [] data = {"currentRow", "columnValue", "insertRow", "deleteRow", "insdel", "updateRow", "null" , "emptyString"};

    private static final int RowTag = 0;
    private static final int ColTag = 1;
    private static final int InsTag = 2;
    private static final int DelTag = 3;
    private static final int InsDelTag = 4;
    private static final int UpdTag = 5;
    private static final int NullTag = 6;
    private static final int EmptyStringTag = 7;

    /**
     * A constant indicating the state of this <code>XmlReaderContentHandler</code>
     * object in which it has not yet been called by the SAX parser and therefore
     * has no indication of what type of input to expect from the parser next.
     * <P>
     * The state is set to <code>INITIAL</code> at the end of each
     * section, which allows the sections to appear in any order and
     * still be parsed correctly (except that metadata must be
     * set before data values can be set).
     */
    private static final int INITIAL = 0;

    /**
     * A constant indicating the state in which this <code>XmlReaderContentHandler</code>
     * object expects the next input received from the
     * SAX parser to be a string corresponding to one of the elements in
     * <code>properties</code>.
     */
    private static final int PROPERTIES = 1;

    /**
     * A constant indicating the state in which this <code>XmlReaderContentHandler</code>
     * object expects the next input received from the
     * SAX parser to be a string corresponding to one of the elements in
     * <code>colDef</code>.
     */
    private static final int METADATA = 2;

    /**
     * A constant indicating the state in which this <code>XmlReaderContentHandler</code>
     * object expects the next input received from the
     * SAX parser to be a string corresponding to one of the elements in
     * <code>data</code>.
     */
    private static final int DATA = 3;

    private  JdbcRowSetResourceBundle resBundle;

    /**
     * Constructs a new <code>XmlReaderContentHandler</code> object that will
     * assist the SAX parser in reading a <code>WebRowSet</code> object in the
     * format of an XML document. In addition to setting some default values,
     * this constructor creates three <code>HashMap</code> objects, one for
     * properties, one for metadata, and one for data.  These hash maps map the
     * strings sent by the SAX parser to integer constants so that they can be
     * compared more efficiently in <code>switch</code> statements.
     *
     * @param r the <code>RowSet</code> object in XML format that will be read
     */
    public XmlReaderContentHandler(RowSet r) {
        // keep the rowset we've been given
        rs = (WebRowSetImpl)r;

        // set-up the token maps
        initMaps();

        // allocate the collection for the updates
        updates = new Vector<>();

        // start out with the empty string
        columnValue = "";
        propertyValue = "";
        metaDataValue = "";

        nullVal = false;
        idx = 0;
        tempStr = "";
        tempUpdate = "";
        tempCommand = "";

        try {
           resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Creates and initializes three new <code>HashMap</code> objects that map
     * the strings returned by the SAX parser to <code>Integer</code>
     * objects.  The strings returned by the parser will match the strings that
     * are array elements in this <code>XmlReaderContentHandler</code> object's
     * <code>properties</code>, <code>colDef</code>, or <code>data</code>
     * fields. For each array element in these fields, there is a corresponding
     * constant defined. It is to these constants that the strings are mapped.
     * In the <code>HashMap</code> objects, the string is the key, and the
     * integer is the value.
     * <P>
     * The purpose of the mapping is to make comparisons faster.  Because comparing
     * numbers is more efficient than comparing strings, the strings returned
     * by the parser are mapped to integers, which can then be used in a
     * <code>switch</code> statement.
     */
    private void initMaps() {
        int items, i;

        propMap = new HashMap<>();
        items = properties.length;

        for (i=0;i<items;i++) {
            propMap.put(properties[i], Integer.valueOf(i));
        }

        colDefMap = new HashMap<>();
        items = colDef.length;

        for (i=0;i<items;i++) {
            colDefMap.put(colDef[i], Integer.valueOf(i));
        }

        dataMap = new HashMap<>();
        items = data.length;

        for (i=0;i<items;i++) {
            dataMap.put(data[i], Integer.valueOf(i));
        }

        //Initialize connection map here
        typeMap = new HashMap<>();
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }


    /**
     * Sets this <code>XmlReaderContentHandler</code> object's <code>tag</code>
     * field if the given name is the key for a tag and this object's state
     * is not <code>INITIAL</code>.  The field is set
     * to the constant that corresponds to the given element name.
     * If the state is <code>INITIAL</code>, the state is set to the given
     * name, which will be one of the sections <code>PROPERTIES</code>,
     * <code>METADATA</code>, or <code>DATA</code>.  In either case, this
     * method puts this document handler in the proper state for calling
     * the method <code>endElement</code>.
     * <P>
     * If the state is <code>DATA</code> and the tag is <code>RowTag</code>,
     * <code>DelTag</code>, or <code>InsTag</code>, this method moves the
     * rowset's cursor to the insert row and sets this
     * <code>XmlReaderContentHandler</code> object's <code>idx</code>
     * field to <code>0</code> so that it will be in the proper
     * state when the parser calls the method <code>endElement</code>.
     *
     * @param lName the name of the element; either (1) one of the array
     *        elements in the fields <code>properties</code>,
     *        <code>colDef</code>, or <code>data</code> or
     *        (2) one of the <code>RowSet</code> elements
     *        <code>"properties"</code>, <code>"metadata"</code>, or
     *        <code>"data"</code>
     * @param attributes <code>org.xml.sax.AttributeList</code> objects that are
     *             attributes of the named section element; may be <code>null</code>
     *             if there are no attributes, which is the case for
     *             <code>WebRowSet</code> objects
     * @exception SAXException if a general SAX error occurs
     */
    public void startElement(String uri, String lName, String qName, Attributes attributes) throws SAXException {
        int tag;
        String name = "";

        name = lName;

        switch (getState()) {
        case PROPERTIES:

            tempCommand = "";
            tag = propMap.get(name);
            if (tag == PropNullTag)
               setNullValue(true);
            else
                setTag(tag);
            break;
        case METADATA:
            tag = colDefMap.get(name);

            if (tag == MetaNullTag)
                setNullValue(true);
            else
                setTag(tag);
            break;
        case DATA:

            /**
              * This has been added to clear out the values of the previous read
              * so that we should not add up values of data between different tags
              */
            tempStr = "";
            tempUpdate = "";
            if(dataMap.get(name) == null) {
                tag = NullTag;
            } else if(dataMap.get(name) == EmptyStringTag) {
                tag = EmptyStringTag;
            } else {
                 tag = dataMap.get(name);
            }

            if (tag == NullTag) {
                setNullValue(true);
            } else if(tag == EmptyStringTag) {
                setEmptyStringValue(true);
            } else {
                setTag(tag);

                if (tag == RowTag || tag == DelTag || tag == InsTag) {
                    idx = 0;
                    try {
                        rs.moveToInsertRow();
                    } catch (SQLException ex) {
                        ;
                    }
                }
            }

            break;
        default:
            setState(name);
        }

    }

    /**
     * Sets the value for the given element if <code>name</code> is one of
     * the array elements in the fields <code>properties</code>,
     * <code>colDef</code>, or <code>data</code> and this
     * <code>XmlReaderContentHandler</code> object's state is not
     * <code>INITIAL</code>. If the state is <code>INITIAL</code>,
     * this method does nothing.
     * <P>
     * If the state is <code>METADATA</code> and
     * the argument supplied is <code>"metadata"</code>, the rowset's
     * metadata is set. If the state is <code>PROPERTIES</code>, the
     * appropriate property is set using the given name to determine
     * the appropriate value. If the state is <code>DATA</code> and
     * the argument supplied is <code>"data"</code>, this method sets
     * the state to <code>INITIAL</code> and returns.  If the argument
     * supplied is one of the elements in the field <code>data</code>,
     * this method makes the appropriate changes to the rowset's data.
     *
     * @param lName the name of the element; either (1) one of the array
     *        elements in the fields <code>properties</code>,
     *        <code>colDef</code>, or <code>data</code> or
     *        (2) one of the <code>RowSet</code> elements
     *        <code>"properties"</code>, <code>"metadata"</code>, or
     *        <code>"data"</code>
     *
     * @exception SAXException if a general SAX error occurs
     */
    @SuppressWarnings("fallthrough")
    public void endElement(String uri, String lName, String qName) throws SAXException {
        int tag;

        String name = "";
        name = lName;

        switch (getState()) {
        case PROPERTIES:
            if (name.equals("properties")) {
                state = INITIAL;
                break;
            }

            try {
                tag = propMap.get(name);
                switch (tag) {
                case KeycolsTag:
                    if (keyCols != null) {
                        int i[] = new int[keyCols.size()];
                        for (int j = 0; j < i.length; j++)
                            i[j] = Integer.parseInt(keyCols.elementAt(j));
                        rs.setKeyColumns(i);
                    }
                    break;

                 case PropClassTag:
                     //Added the handling for Class tags to take care of maps
                     //Makes an entry into the map upon end of class tag
                     try{
                          typeMap.put(Key_map,sun.reflect.misc.ReflectUtil.forName(Value_map));

                        }catch(ClassNotFoundException ex) {
                          throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errmap").toString(), ex.getMessage()));
                        }
                      break;

                 case MapTag:
                      //Added the handling for Map to take set the typeMap
                      rs.setTypeMap(typeMap);
                      break;

                default:
                    break;
                }

                if (getNullValue()) {
                    setPropertyValue(null);
                    setNullValue(false);
                } else {
                    setPropertyValue(propertyValue);
                }
            } catch (SQLException ex) {
                throw new SAXException(ex.getMessage());
            }

            // propertyValue need to be reset to an empty string
            propertyValue = "";
            setTag(-1);
            break;
        case METADATA:
            if (name.equals("metadata")) {
                try {
                    rs.setMetaData(md);
                    state = INITIAL;
                } catch (SQLException ex) {
                    throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errmetadata").toString(), ex.getMessage()));
                }
            } else {
                try {
                    if (getNullValue()) {
                        setMetaDataValue(null);
                        setNullValue(false);
                    } else {
                        setMetaDataValue(metaDataValue);
                    }
                } catch (SQLException ex) {
                    throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errmetadata").toString(), ex.getMessage()));

                }
                // metaDataValue needs to be reset to an empty string
                metaDataValue = "";
            }
            setTag(-1);
            break;
        case DATA:
            if (name.equals("data")) {
                state = INITIAL;
                return;
            }

            if(dataMap.get(name) == null) {
                tag = NullTag;
            } else {
                 tag = dataMap.get(name);
            }
            switch (tag) {
            case ColTag:
                try {
                    idx++;
                    if (getNullValue()) {
                        insertValue(null);
                        setNullValue(false);
                    } else {
                        insertValue(tempStr);
                    }
                    // columnValue now need to be reset to the empty string
                    columnValue = "";
                } catch (SQLException ex) {
                    throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errinsertval").toString(), ex.getMessage()));
                }
                break;
            case RowTag:
                try {
                    rs.insertRow();
                    rs.moveToCurrentRow();
                    rs.next();

                    // Making this as the original to turn off the
                    // rowInserted flagging
                    rs.setOriginalRow();

                    applyUpdates();
                } catch (SQLException ex) {
                    throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errconstr").toString(), ex.getMessage()));
                }
                break;
            case DelTag:
                try {
                    rs.insertRow();
                    rs.moveToCurrentRow();
                    rs.next();
                    rs.setOriginalRow();
                    applyUpdates();
                    rs.deleteRow();
                } catch (SQLException ex) {
                    throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errdel").toString() , ex.getMessage()));
                }
                break;
            case InsTag:
                try {
                    rs.insertRow();
                    rs.moveToCurrentRow();
                    rs.next();
                    applyUpdates();
                } catch (SQLException ex) {
                    throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errinsert").toString() , ex.getMessage()));
                }
                break;

            case InsDelTag:
                try {
                    rs.insertRow();
                    rs.moveToCurrentRow();
                    rs.next();
                    rs.setOriginalRow();
                    applyUpdates();
                } catch (SQLException ex) {
                    throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errinsdel").toString() , ex.getMessage()));
                }
                break;

             case UpdTag:
                 try {
                        if(getNullValue())
                         {
                          insertValue(null);
                          setNullValue(false);
                         } else if(getEmptyStringValue()) {
                               insertValue("");
                               setEmptyStringValue(false);
                         } else {
                            updates.add(upd);
                         }
                 }  catch(SQLException ex) {
                        throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errupdate").toString() , ex.getMessage()));
                 }
                break;

            default:
                break;
            }
        default:
            break;
        }
    }

    private void applyUpdates() throws SAXException {
        // now handle any updates
        if (updates.size() > 0) {
            try {
                Object upd[];
                Iterator<?> i = updates.iterator();
                while (i.hasNext()) {
                    upd = (Object [])i.next();
                    idx = ((Integer)upd[0]).intValue();

                   if(!(lastval.equals(upd[1]))){
                       insertValue((String)(upd[1]));
                    }
                }

                rs.updateRow();
                } catch (SQLException ex) {
                    throw new SAXException(MessageFormat.format(resBundle.handleGetObject("xmlrch.errupdrow").toString() , ex.getMessage()));
                }
            updates.removeAllElements();
        }


    }

    /**
     * Sets a property, metadata, or data value with the characters in
     * the given array of characters, starting with the array element
     * indicated by <code>start</code> and continuing for <code>length</code>
     * number of characters.
     * <P>
     * The SAX parser invokes this method and supplies
     * the character array, start position, and length parameter values it
     * got from parsing the XML document.  An application programmer never
     * invokes this method directly.
     *
     * @param ch an array of characters supplied by the SAX parser, all or part of
     *         which will be used to set a value
     * @param start the position in the given array at which to start
     * @param length the number of consecutive characters to use
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            switch (getState()) {
            case PROPERTIES:
                propertyValue = new String(ch, start, length);

                /**
                  * This has been added for handling of special characters. When special
                  * characters are encountered the characters function gets called for
                  * each of the characters so we need to append the value got in the
                  * previous call as it is the same data present between the start and
                  * the end tag.
                  **/
                tempCommand = tempCommand.concat(propertyValue);
                propertyValue = tempCommand;

                // Added the following check for handling of type tags in maps
                if(tag == PropTypeTag)
                {
                        Key_map = propertyValue;
                }

                // Added the following check for handling of class tags in maps
                else if(tag == PropClassTag)
                {
                        Value_map = propertyValue;
                }
                break;

            case METADATA:

                // The parser will come here after the endElement as there is
                // "\n" in the after endTag is printed. This will cause a problem
                // when the data between the tags is an empty string so adding
                // below condition to take care of that situation.

                if (tag == -1)
                {
                        break;
                }

                metaDataValue = new String(ch, start, length);
                break;
            case DATA:
                setDataValue(ch, start, length);
                break;
            default:
                ;
            }
        } catch (SQLException ex) {
            throw new SAXException(resBundle.handleGetObject("xmlrch.chars").toString() + ex.getMessage());
        }
    }

    private void setState(String s) throws SAXException {
        if (s.equals("webRowSet")) {
            state = INITIAL;
        } else if (s.equals("properties")) {
            if (state != PROPERTIES)
                state = PROPERTIES;
            else
                state = INITIAL;
        } else if (s.equals("metadata")) {
            if (state != METADATA)
                state = METADATA;
            else
                state = INITIAL;
        } else if (s.equals("data")) {
            if (state != DATA)
                state = DATA;
            else
                state = INITIAL;
        }

    }

    /**
     * Retrieves the current state of this <code>XmlReaderContentHandler</code>
     * object's rowset, which is stored in the document handler's
     * <code>state</code> field.
     *
     * @return one of the following constants:
     *         <code>XmlReaderContentHandler.PROPERTIES</code>
     *         <code>XmlReaderContentHandler.METADATA</code>
     *         <code>XmlReaderContentHandler.DATA</code>
     *         <code>XmlReaderContentHandler.INITIAL</code>
     */
    private int getState() {
        return state;
    }

    private void setTag(int t) {
        tag = t;
    }

    private int getTag() {
        return tag;
    }

    private void setNullValue(boolean n) {
        nullVal = n;
    }

    private boolean getNullValue() {
        return nullVal;
    }

    private void setEmptyStringValue(boolean e) {
        emptyStringVal = e;
    }

    private boolean getEmptyStringValue() {
        return emptyStringVal;
    }

    private String getStringValue(String s) {
         return s;
    }

    private int getIntegerValue(String s) {
        return Integer.parseInt(s);
    }

    private boolean getBooleanValue(String s) {

        return Boolean.valueOf(s).booleanValue();
    }

    private java.math.BigDecimal getBigDecimalValue(String s) {
        return new java.math.BigDecimal(s);
    }

    private byte getByteValue(String s) {
        return Byte.parseByte(s);
    }

    private short getShortValue(String s) {
        return Short.parseShort(s);
    }

    private long getLongValue(String s) {
        return Long.parseLong(s);
    }

    private float getFloatValue(String s) {
        return Float.parseFloat(s);
    }

    private double getDoubleValue(String s) {
        return Double.parseDouble(s);
    }

    private byte[] getBinaryValue(String s) {
        return s.getBytes();
    }

    private java.sql.Date getDateValue(String s) {
        return new java.sql.Date(getLongValue(s));
    }

    private java.sql.Time getTimeValue(String s) {
        return new java.sql.Time(getLongValue(s));
    }

    private java.sql.Timestamp getTimestampValue(String s) {
        return new java.sql.Timestamp(getLongValue(s));
    }

    private void setPropertyValue(String s) throws SQLException {
        // find out if we are going to be dealing with a null
        boolean nullValue = getNullValue();

        switch(getTag()) {
        case CommandTag:
            if (nullValue)
               ; //rs.setCommand(null);
            else
                rs.setCommand(s);
            break;
        case ConcurrencyTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setConcurrency(getIntegerValue(s));
            break;
        case DatasourceTag:
            if (nullValue)
                rs.setDataSourceName(null);
            else
                rs.setDataSourceName(s);
            break;
        case EscapeProcessingTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setEscapeProcessing(getBooleanValue(s));
            break;
        case FetchDirectionTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setFetchDirection(getIntegerValue(s));
            break;
        case FetchSizeTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setFetchSize(getIntegerValue(s));
            break;
        case IsolationLevelTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setTransactionIsolation(getIntegerValue(s));
            break;
        case KeycolsTag:
            break;
        case PropColumnTag:
            if (keyCols == null)
                keyCols = new Vector<>();
            keyCols.add(s);
            break;
        case MapTag:
            break;
        case MaxFieldSizeTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setMaxFieldSize(getIntegerValue(s));
            break;
        case MaxRowsTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setMaxRows(getIntegerValue(s));
            break;
        case QueryTimeoutTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setQueryTimeout(getIntegerValue(s));
            break;
        case ReadOnlyTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setReadOnly(getBooleanValue(s));
            break;
        case RowsetTypeTag:
            if (nullValue) {
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            } else {
                //rs.setType(getIntegerValue(s));
                String strType = getStringValue(s);
                int iType = 0;

                if(strType.trim().equals("ResultSet.TYPE_SCROLL_INSENSITIVE")) {
                   iType = 1004;
                } else if(strType.trim().equals("ResultSet.TYPE_SCROLL_SENSITIVE"))   {
                   iType = 1005;
                } else if(strType.trim().equals("ResultSet.TYPE_FORWARD_ONLY")) {
                   iType = 1003;
                }
                rs.setType(iType);
            }
            break;
        case ShowDeletedTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue").toString());
            else
                rs.setShowDeleted(getBooleanValue(s));
            break;
        case TableNameTag:
            if (nullValue)
                //rs.setTableName(null);
                ;
            else
                rs.setTableName(s);
            break;
        case UrlTag:
            if (nullValue)
                rs.setUrl(null);
            else
                rs.setUrl(s);
            break;
        case SyncProviderNameTag:
            if (nullValue) {
                rs.setSyncProvider(null);
            } else {
                String str = s.substring(0,s.indexOf('@')+1);
                rs.setSyncProvider(str);
            }
            break;
        case SyncProviderVendorTag:
            // to be implemented
            break;
        case SyncProviderVersionTag:
            // to be implemented
            break;
        case SyncProviderGradeTag:
            // to be implemented
            break;
        case DataSourceLock:
            // to be implemented
            break;
        default:
            break;
        }

    }

    private void setMetaDataValue(String s) throws SQLException {
        // find out if we are going to be dealing with a null
        boolean nullValue = getNullValue();

        switch (getTag()) {
        case ColumnCountTag:
            md = new RowSetMetaDataImpl();
            idx = 0;

            if (nullValue) {
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            } else {
                md.setColumnCount(getIntegerValue(s));
            }
            break;
        case ColumnDefinitionTag:
            break;
        case ColumnIndexTag:
            idx++;
            break;
        case AutoIncrementTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setAutoIncrement(idx, getBooleanValue(s));
            break;
        case CaseSensitiveTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setCaseSensitive(idx, getBooleanValue(s));
            break;
        case CurrencyTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setCurrency(idx, getBooleanValue(s));
            break;
        case NullableTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setNullable(idx, getIntegerValue(s));
            break;
        case SignedTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setSigned(idx, getBooleanValue(s));
            break;
        case SearchableTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setSearchable(idx, getBooleanValue(s));
            break;
        case ColumnDisplaySizeTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setColumnDisplaySize(idx, getIntegerValue(s));
            break;
        case ColumnLabelTag:
            if (nullValue)
                md.setColumnLabel(idx, null);
            else
                md.setColumnLabel(idx, s);
            break;
        case ColumnNameTag:
            if (nullValue)
                md.setColumnName(idx, null);
            else
                md.setColumnName(idx, s);

            break;
        case SchemaNameTag:
            if (nullValue) {
                md.setSchemaName(idx, null); }
            else
                md.setSchemaName(idx, s);
            break;
        case ColumnPrecisionTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setPrecision(idx, getIntegerValue(s));
            break;
        case ColumnScaleTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setScale(idx, getIntegerValue(s));
            break;
        case MetaTableNameTag:
            if (nullValue)
                md.setTableName(idx, null);
            else
                md.setTableName(idx, s);
            break;
        case CatalogNameTag:
            if (nullValue)
                md.setCatalogName(idx, null);
            else
                md.setCatalogName(idx, s);
            break;
        case ColumnTypeTag:
            if (nullValue)
                throw new SQLException(resBundle.handleGetObject("xmlrch.badvalue1").toString());
            else
                md.setColumnType(idx, getIntegerValue(s));
            break;
        case ColumnTypeNameTag:
            if (nullValue)
                md.setColumnTypeName(idx, null);
            else
                md.setColumnTypeName(idx, s);
            break;
        default:
            //System.out.println("MetaData: Unknown Tag: (" + getTag() + ")");
            break;

        }
    }

    private void setDataValue(char[] ch, int start, int len) throws SQLException {
        switch (getTag()) {
        case ColTag:
            columnValue = new String(ch, start, len);
            /**
              * This has been added for handling of special characters. When special
              * characters are encountered the characters function gets called for
              * each of the characters so we need to append the value got in the
              * previous call as it is the same data present between the start and
              * the end tag.
              **/
            tempStr = tempStr.concat(columnValue);
            break;
        case UpdTag:
            upd = new Object[2];

            /**
              * This has been added for handling of special characters. When special
              * characters are encountered the characters function gets called for
              * each of the characters so we need to append the value got in the
              * previous call as it is the same data present between the start and
              * the end tag.
              **/

            tempUpdate = tempUpdate.concat(new String(ch,start,len));
            upd[0] = Integer.valueOf(idx);
            upd[1] = tempUpdate;
            //updates.add(upd);

            lastval = (String)upd[1];
            //insertValue(ch, start, len);
            break;
        case InsTag:

        }
    }

    private void insertValue(String s) throws SQLException {

        if (getNullValue()) {
            rs.updateNull(idx);
            return;
        }

        // no longer have to deal with those pesky nulls.
        int type = rs.getMetaData().getColumnType(idx);
        switch (type) {
        case java.sql.Types.BIT:
            rs.updateBoolean(idx, getBooleanValue(s));
            break;
        case java.sql.Types.BOOLEAN:
            rs.updateBoolean(idx, getBooleanValue(s));
            break;
        case java.sql.Types.SMALLINT:
        case java.sql.Types.TINYINT:
            rs.updateShort(idx, getShortValue(s));
            break;
        case java.sql.Types.INTEGER:
            rs.updateInt(idx, getIntegerValue(s));
            break;
        case java.sql.Types.BIGINT:
            rs.updateLong(idx, getLongValue(s));
            break;
        case java.sql.Types.REAL:
        case java.sql.Types.FLOAT:
            rs.updateFloat(idx, getFloatValue(s));
            break;
        case java.sql.Types.DOUBLE:
            rs.updateDouble(idx, getDoubleValue(s));
            break;
        case java.sql.Types.NUMERIC:
        case java.sql.Types.DECIMAL:
            rs.updateObject(idx, getBigDecimalValue(s));
            break;
        case java.sql.Types.BINARY:
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
            rs.updateBytes(idx, getBinaryValue(s));
            break;
        case java.sql.Types.DATE:
            rs.updateDate(idx,  getDateValue(s));
            break;
        case java.sql.Types.TIME:
            rs.updateTime(idx, getTimeValue(s));
            break;
        case java.sql.Types.TIMESTAMP:
            rs.updateTimestamp(idx, getTimestampValue(s));
            break;
        case java.sql.Types.CHAR:
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            rs.updateString(idx, getStringValue(s));
            break;
        default:

        }

    }

    /**
     * Throws the given <code>SAXParseException</code> object. This
     * exception was originally thrown by the SAX parser and is passed
     * to the method <code>error</code> when the SAX parser invokes it.
     *
     * @param e the <code>SAXParseException</code> object to throw
     */
    public void error (SAXParseException e) throws SAXParseException {
            throw e;
    }

    // dump warnings too
    /**
     * Prints a warning message to <code>System.out</code> giving the line
     * number and uri for what caused the warning plus a message explaining
     * the reason for the warning. This method is invoked by the SAX parser.
     *
     * @param err a warning generated by the SAX parser
     */
    public void warning (SAXParseException err) throws SAXParseException {
        System.out.println (MessageFormat.format(resBundle.handleGetObject("xmlrch.warning").toString(), new Object[] { err.getMessage(), err.getLineNumber(), err.getSystemId() }));
    }

    /**
     *
     */
    public void notationDecl(String name, String publicId, String systemId) {

    }

    /**
     *
     */
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) {

    }

   /**
    * Returns the current row of this <code>Rowset</code>object.
    * The ResultSet's cursor is positioned at the Row which is needed
    *
    * @return the <code>Row</code> object on which the <code>RowSet</code>
    *           implementation objects's cursor is positioned
    */
    private Row getPresentRow(WebRowSetImpl rs) throws SQLException {
         //rs.setOriginalRow();
         // ResultSetMetaData rsmd = rs.getMetaData();
         // int numCols = rsmd.getColumnCount();
         // Object vals[] = new Object[numCols];
         // for(int j = 1; j<= numCols ; j++){
         //     vals[j-1] = rs.getObject(j);
         // }
         // return(new Row(numCols, vals));
         return null;
   }




}
