/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.rowset.JdbcRowSetResourceBundle;
import java.sql.*;
import javax.sql.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;

import javax.sql.rowset.*;
import javax.sql.rowset.spi.*;

/**
 * An implementation of the {@code XmlWriter}  interface, which writes a
 * {@code WebRowSet}  object to an output stream as an XML document.
 */

public class WebRowSetXmlWriter implements XmlWriter, Serializable {

    /**
     * The {@code java.io.Writer}  object to which this {@code WebRowSetXmlWriter}
     * object will write when its {@code writeXML}  method is called. The value
     * for this field is set with the {@code java.io.Writer}  object given
     * as the second argument to the {@code writeXML}  method.
     */
    private transient java.io.Writer writer;

    /**
     * The {@code java.util.Stack}  object that this {@code WebRowSetXmlWriter}
     * object will use for storing the tags to be used for writing the calling
     * {@code WebRowSet}  object as an XML document.
     */
    private java.util.Stack<String> stack;

    private  JdbcRowSetResourceBundle resBundle;

    public WebRowSetXmlWriter() {

        try {
           resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Writes the given {@code WebRowSet}  object as an XML document
     * using the given {@code java.io.Writer}  object. The XML document
     * will include the {@code WebRowSet}  object's data, metadata, and
     * properties.  If a data value has been updated, that information is also
     * included.
     * <P>
     * This method is called by the {@code XmlWriter}  object that is
     * referenced in the calling {@code WebRowSet}  object's
     * {@code xmlWriter}  field.  The {@code XmlWriter.writeXML}
     * method passes to this method the arguments that were supplied to it.
     *
     * @param caller the {@code WebRowSet}  object to be written; must
     *        be a rowset for which this {@code WebRowSetXmlWriter}  object
     *        is the writer
     * @param wrt the {@code java.io.Writer}  object to which
     *        {@code caller}  will be written
     * @exception SQLException if a database access error occurs or
     *            this {@code WebRowSetXmlWriter}  object is not the writer
     *            for the given rowset
     * @see XmlWriter#writeXML
     */
    public void writeXML(WebRowSet caller, java.io.Writer wrt)
    throws SQLException {

        // create a new stack for tag checking.
        stack = new java.util.Stack<>();
        writer = wrt;
        writeRowSet(caller);
    }

    /**
     * Writes the given {@code WebRowSet}  object as an XML document
     * using the given {@code java.io.OutputStream}  object. The XML document
     * will include the {@code WebRowSet}  object's data, metadata, and
     * properties.  If a data value has been updated, that information is also
     * included.
     * <P>
     * Using stream is a faster way than using {@code java.io.Writer}
     *
     * This method is called by the {@code XmlWriter}  object that is
     * referenced in the calling {@code WebRowSet}  object's
     * {@code xmlWriter}  field.  The {@code XmlWriter.writeXML}
     * method passes to this method the arguments that were supplied to it.
     *
     * @param caller the {@code WebRowSet}  object to be written; must
     *        be a rowset for which this {@code WebRowSetXmlWriter}  object
     *        is the writer
     * @param oStream the {@code java.io.OutputStream}  object to which
     *        {@code caller}  will be written
     * @throws SQLException if a database access error occurs or
     *            this {@code WebRowSetXmlWriter}  object is not the writer
     *            for the given rowset
     * @see XmlWriter#writeXML
     */
    public void writeXML(WebRowSet caller, java.io.OutputStream oStream)
    throws SQLException {

        // create a new stack for tag checking.
        stack = new java.util.Stack<>();
        writer = new OutputStreamWriter(oStream);
        writeRowSet(caller);
    }

    /**
     *
     *
     * @exception SQLException if a database access error occurs
     */
    private void writeRowSet(WebRowSet caller) throws SQLException {

        try {

            startHeader();

            writeProperties(caller);
            writeMetaData(caller);
            writeData(caller);

            endHeader();

        } catch (java.io.IOException ex) {
            throw new SQLException(MessageFormat.format(resBundle.handleGetObject("wrsxmlwriter.ioex").toString(), ex.getMessage()));
        }
    }

    private void startHeader() throws java.io.IOException {

        setTag("webRowSet");
        writer.write("<?xml version=\"1.0\"?>\n");
        writer.write("<webRowSet xmlns=\"http://java.sun.com/xml/ns/jdbc\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        writer.write("xsi:schemaLocation=\"http://java.sun.com/xml/ns/jdbc http://java.sun.com/xml/ns/jdbc/webrowset.xsd\">\n");
    }

    private void endHeader() throws java.io.IOException {
        endTag("webRowSet");
    }

    /**
     *
     *
     * @exception SQLException if a database access error occurs
     */
    private void writeProperties(WebRowSet caller) throws java.io.IOException {

        beginSection("properties");

        try {
            propString("command", processSpecialCharacters(caller.getCommand()));
            propInteger("concurrency", caller.getConcurrency());
            propString("datasource", caller.getDataSourceName());
            propBoolean("escape-processing",
                    caller.getEscapeProcessing());

            try {
                propInteger("fetch-direction", caller.getFetchDirection());
            } catch(SQLException sqle) {
                // it may be the case that fetch direction has not been set
                // fetchDir  == 0
                // in that case it will throw a SQLException.
                // To avoid that catch it here
            }

            propInteger("fetch-size", caller.getFetchSize());
            propInteger("isolation-level",
                    caller.getTransactionIsolation());

            beginSection("key-columns");

            int[] kc = caller.getKeyColumns();
            for (int i = 0; kc != null && i < kc.length; i++)
                propInteger("column", kc[i]);

            endSection("key-columns");

            //Changed to beginSection and endSection for maps for proper indentation
            beginSection("map");
            Map<String, Class<?>> typeMap = caller.getTypeMap();
            if(typeMap != null) {
                for(Map.Entry<String, Class<?>> mm : typeMap.entrySet()) {
                    propString("type", mm.getKey());
                    propString("class", mm.getValue().getName());
                }
            }
            endSection("map");

            propInteger("max-field-size", caller.getMaxFieldSize());
            propInteger("max-rows", caller.getMaxRows());
            propInteger("query-timeout", caller.getQueryTimeout());
            propBoolean("read-only", caller.isReadOnly());

            int itype = caller.getType();
            String strType = "";

            if(itype == 1003) {
                strType = "ResultSet.TYPE_FORWARD_ONLY";
            } else if(itype == 1004) {
                strType = "ResultSet.TYPE_SCROLL_INSENSITIVE";
            } else if(itype == 1005) {
                strType = "ResultSet.TYPE_SCROLL_SENSITIVE";
            }

            propString("rowset-type", strType);

            propBoolean("show-deleted", caller.getShowDeleted());
            propString("table-name", caller.getTableName());
            propString("url", caller.getUrl());

            beginSection("sync-provider");
            // Remove the string after "@xxxx"
            // before writing it to the xml file.
            String strProviderInstance = (caller.getSyncProvider()).toString();
            String strProvider = strProviderInstance.substring(0, (caller.getSyncProvider()).toString().indexOf('@'));

            propString("sync-provider-name", strProvider);
            propString("sync-provider-vendor", "Oracle Corporation");
            propString("sync-provider-version", "1.0");
            propInteger("sync-provider-grade", caller.getSyncProvider().getProviderGrade());
            propInteger("data-source-lock", caller.getSyncProvider().getDataSourceLock());

            endSection("sync-provider");

        } catch (SQLException ex) {
            throw new java.io.IOException(MessageFormat.format(resBundle.handleGetObject("wrsxmlwriter.sqlex").toString(), ex.getMessage()));
        }

        endSection("properties");
    }

    /**
     *
     *
     * @exception SQLException if a database access error occurs
     */
    private void writeMetaData(WebRowSet caller) throws java.io.IOException {
        int columnCount;

        beginSection("metadata");

        try {

            ResultSetMetaData rsmd = caller.getMetaData();
            columnCount = rsmd.getColumnCount();
            propInteger("column-count", columnCount);

            for (int colIndex = 1; colIndex <= columnCount; colIndex++) {
                beginSection("column-definition");

                propInteger("column-index", colIndex);
                propBoolean("auto-increment", rsmd.isAutoIncrement(colIndex));
                propBoolean("case-sensitive", rsmd.isCaseSensitive(colIndex));
                propBoolean("currency", rsmd.isCurrency(colIndex));
                propInteger("nullable", rsmd.isNullable(colIndex));
                propBoolean("signed", rsmd.isSigned(colIndex));
                propBoolean("searchable", rsmd.isSearchable(colIndex));
                propInteger("column-display-size",rsmd.getColumnDisplaySize(colIndex));
                propString("column-label", rsmd.getColumnLabel(colIndex));
                propString("column-name", rsmd.getColumnName(colIndex));
                propString("schema-name", rsmd.getSchemaName(colIndex));
                propInteger("column-precision", rsmd.getPrecision(colIndex));
                propInteger("column-scale", rsmd.getScale(colIndex));
                propString("table-name", rsmd.getTableName(colIndex));
                propString("catalog-name", rsmd.getCatalogName(colIndex));
                propInteger("column-type", rsmd.getColumnType(colIndex));
                propString("column-type-name", rsmd.getColumnTypeName(colIndex));

                endSection("column-definition");
            }
        } catch (SQLException ex) {
            throw new java.io.IOException(MessageFormat.format(resBundle.handleGetObject("wrsxmlwriter.sqlex").toString(), ex.getMessage()));
        }

        endSection("metadata");
    }

    /**
     *
     *
     * @exception SQLException if a database access error occurs
     */
    private void writeData(WebRowSet caller) throws java.io.IOException {
        ResultSet rs;

        try {
            ResultSetMetaData rsmd = caller.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int i;

            beginSection("data");

            caller.beforeFirst();
            caller.setShowDeleted(true);
            while (caller.next()) {
                if (caller.rowDeleted() && caller.rowInserted()) {
                    beginSection("modifyRow");
                } else if (caller.rowDeleted()) {
                    beginSection("deleteRow");
                } else if (caller.rowInserted()) {
                    beginSection("insertRow");
                } else {
                    beginSection("currentRow");
                }

                for (i = 1; i <= columnCount; i++) {
                    if (caller.columnUpdated(i)) {
                        rs = caller.getOriginalRow();
                        rs.next();
                        beginTag("columnValue");
                        writeValue(i, (RowSet)rs);
                        endTag("columnValue");
                        beginTag("updateRow");
                        writeValue(i, caller);
                        endTag("updateRow");
                    } else {
                        beginTag("columnValue");
                        writeValue(i, caller);
                        endTag("columnValue");
                    }
                }

                endSection(); // this is unchecked
            }
            endSection("data");
        } catch (SQLException ex) {
            throw new java.io.IOException(MessageFormat.format(resBundle.handleGetObject("wrsxmlwriter.sqlex").toString(), ex.getMessage()));
        }
    }

    private void writeValue(int idx, RowSet caller) throws java.io.IOException {
        try {
            int type = caller.getMetaData().getColumnType(idx);

            switch (type) {
                case java.sql.Types.BIT:
                case java.sql.Types.BOOLEAN:
                    boolean b = caller.getBoolean(idx);
                    if (caller.wasNull())
                        writeNull();
                    else
                        writeBoolean(b);
                    break;
                case java.sql.Types.TINYINT:
                case java.sql.Types.SMALLINT:
                    short s = caller.getShort(idx);
                    if (caller.wasNull())
                        writeNull();
                    else
                        writeShort(s);
                    break;
                case java.sql.Types.INTEGER:
                    int i = caller.getInt(idx);
                    if (caller.wasNull())
                        writeNull();
                    else
                        writeInteger(i);
                    break;
                case java.sql.Types.BIGINT:
                    long l = caller.getLong(idx);
                    if (caller.wasNull())
                        writeNull();
                    else
                        writeLong(l);
                    break;
                case java.sql.Types.REAL:
                case java.sql.Types.FLOAT:
                    float f = caller.getFloat(idx);
                    if (caller.wasNull())
                        writeNull();
                    else
                        writeFloat(f);
                    break;
                case java.sql.Types.DOUBLE:
                    double d = caller.getDouble(idx);
                    if (caller.wasNull())
                        writeNull();
                    else
                        writeDouble(d);
                    break;
                case java.sql.Types.NUMERIC:
                case java.sql.Types.DECIMAL:
                    writeBigDecimal(caller.getBigDecimal(idx));
                    break;
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    break;
                case java.sql.Types.DATE:
                    java.sql.Date date = caller.getDate(idx);
                    if (caller.wasNull())
                        writeNull();
                    else
                        writeLong(date.getTime());
                    break;
                case java.sql.Types.TIME:
                    java.sql.Time time = caller.getTime(idx);
                    if (caller.wasNull())
                        writeNull();
                    else
                        writeLong(time.getTime());
                    break;
                case java.sql.Types.TIMESTAMP:
                    java.sql.Timestamp ts = caller.getTimestamp(idx);
                    if (caller.wasNull())
                        writeNull();
                    else
                        writeLong(ts.getTime());
                    break;
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.LONGVARCHAR:
                    writeStringData(caller.getString(idx));
                    break;
                default:
                    System.out.println(resBundle.handleGetObject("wsrxmlwriter.notproper").toString());
                    //Need to take care of BLOB, CLOB, Array, Ref here
            }
        } catch (SQLException ex) {
            throw new java.io.IOException(resBundle.handleGetObject("wrsxmlwriter.failedwrite").toString()+ ex.getMessage());
        }
    }

    /*
     * This begins a new tag with a indent
     *
     */
    private void beginSection(String tag) throws java.io.IOException {
        // store the current tag
        setTag(tag);

        writeIndent(stack.size());

        // write it out
        writer.write("<" + tag + ">\n");
    }

    /*
     * This closes a tag started by beginTag with a indent
     *
     */
    private void endSection(String tag) throws java.io.IOException {
        writeIndent(stack.size());

        String beginTag = getTag();

        if(beginTag.indexOf("webRowSet") != -1) {
            beginTag ="webRowSet";
        }

        if (tag.equals(beginTag) ) {
            // get the current tag and write it out
            writer.write("</" + beginTag + ">\n");
        } else {
            ;
        }
        writer.flush();
    }

    private void endSection() throws java.io.IOException {
        writeIndent(stack.size());

        // get the current tag and write it out
        String beginTag = getTag();
        writer.write("</" + beginTag + ">\n");

        writer.flush();
    }

    private void beginTag(String tag) throws java.io.IOException {
        // store the current tag
        setTag(tag);

        writeIndent(stack.size());

        // write tag out
        writer.write("<" + tag + ">");
    }

    private void endTag(String tag) throws java.io.IOException {
        String beginTag = getTag();
        if (tag.equals(beginTag)) {
            // get the current tag and write it out
            writer.write("</" + beginTag + ">\n");
        } else {
            ;
        }
        writer.flush();
    }

    private void emptyTag(String tag) throws java.io.IOException {
        // write an emptyTag
        writer.write("<" + tag + "/>");
    }

    private void setTag(String tag) {
        // add the tag to stack
        stack.push(tag);
    }

    private String getTag() {
        return stack.pop();
    }

    private void writeNull() throws java.io.IOException {
        emptyTag("null");
    }

    private void writeStringData(String s) throws java.io.IOException {
        if (s == null) {
            writeNull();
        } else if (s.isEmpty()) {
            writeEmptyString();
        } else {

            s = processSpecialCharacters(s);

            writer.write(s);
        }
    }

    private void writeString(String s) throws java.io.IOException {
        if (s != null) {
            writer.write(s);
        } else  {
            writeNull();
        }
    }


    private void writeShort(short s) throws java.io.IOException {
        writer.write(Short.toString(s));
    }

    private void writeLong(long l) throws java.io.IOException {
        writer.write(Long.toString(l));
    }

    private void writeInteger(int i) throws java.io.IOException {
        writer.write(Integer.toString(i));
    }

    private void writeBoolean(boolean b) throws java.io.IOException {
        writer.write(Boolean.valueOf(b).toString());
    }

    private void writeFloat(float f) throws java.io.IOException {
        writer.write(Float.toString(f));
    }

    private void writeDouble(double d) throws java.io.IOException {
        writer.write(Double.toString(d));
    }

    private void writeBigDecimal(java.math.BigDecimal bd) throws java.io.IOException {
        if (bd != null)
            writer.write(bd.toString());
        else
            emptyTag("null");
    }

    private void writeIndent(int tabs) throws java.io.IOException {
        // indent...
        for (int i = 1; i < tabs; i++) {
            writer.write("  ");
        }
    }

    private void propString(String tag, String s) throws java.io.IOException {
        beginTag(tag);
        writeString(s);
        endTag(tag);
    }

    private void propInteger(String tag, int i) throws java.io.IOException {
        beginTag(tag);
        writeInteger(i);
        endTag(tag);
    }

    private void propBoolean(String tag, boolean b) throws java.io.IOException {
        beginTag(tag);
        writeBoolean(b);
        endTag(tag);
    }

    private void writeEmptyString() throws java.io.IOException {
        emptyTag("emptyString");
    }
    /**
     * Purely for code coverage purposes..
     */
    public boolean writeData(RowSetInternal caller) {
        return false;
    }


    /**
     * This function has been added for the processing of special characters
     * lik <,>,'," and & in the data to be serialized. These have to be taken
     * of specifically or else there will be parsing error while trying to read
     * the contents of the XML file.
     **/

    private String processSpecialCharacters(String s) {

        if(s == null) {
            return null;
        }
        char []charStr = s.toCharArray();
        String specialStr = "";

        for(int i = 0; i < charStr.length; i++) {
            if(charStr[i] == '&') {
                specialStr = specialStr.concat("&amp;");
            } else if(charStr[i] == '<') {
                specialStr = specialStr.concat("&lt;");
            } else if(charStr[i] == '>') {
                specialStr = specialStr.concat("&gt;");
            } else if(charStr[i] == '\'') {
                specialStr = specialStr.concat("&apos;");
            } else if(charStr[i] == '\"') {
                specialStr = specialStr.concat("&quot;");
            } else {
                specialStr = specialStr.concat(String.valueOf(charStr[i]));
            }
        }

        s = specialStr;
        return s;
    }


    /**
     * This method re populates the resBundle
     * during the deserialization process
     *
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Default state initialization happens here
        ois.defaultReadObject();
        // Initialization of transient Res Bundle happens here .
        try {
           resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    static final long serialVersionUID = 7163134986189677641L;
}
