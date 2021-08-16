/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.dtdbuilder;

import javax.swing.text.html.parser.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.BitSet;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Deflater;
import java.net.URL;

/**
 * The representation of an SGML DTD. This is produced by the DTDParser.
 * The resulting DTD object describes a document syntax and is needed
 * to parse HTML documents using the Parser. It contains a list of
 * elements and their attributes as well as a list of entities defined
 * in the DTD.
 *
 * @see Element
 * @see AttributeList
 * @see ContentModel
 * @see DTDParser
 * @see Parser
 * @author Arthur van Hoff
 * @author Guy Abossolo Foh
 */
public
class DTDBuilder extends DTD {

    static PublicMapping mapping = null;

    // Hash from name to Integer
    private Hashtable<String, Integer> namesHash = new Hashtable<>();
    // Vector of all names
    private Vector<String> namesVector = new Vector<>();

    /**
     * Create a new DTD.
     */
    protected DTDBuilder(String name) {
        super(name);
    }


    /**
     * Save to a stream as a Java class. Instantiating this class will
     * reproduce a (virtually) identical DTD.
     */
    void save(DataOutputStream out, String className) throws IOException {

        out.writeInt(DTD.FILE_VERSION);

        buildNamesTable();
        int numNames = namesVector.size();
        out.writeShort((short) (namesVector.size()));
        for (int i = 0; i < namesVector.size(); i++) {
            String nm = namesVector.elementAt(i);
            out.writeUTF(nm);
        }

        saveEntities(out);

        out.writeShort((short) (elements.size()));
        for (Enumeration<Element> e = elements.elements() ; e.hasMoreElements() ; ) {
            saveElement(out, e.nextElement());
        }

        if (namesVector.size() != numNames) {
            System.err.println("!!! ERROR!  Names were added to the list!");
            Thread.dumpStack();
            System.exit(1);
        }
    }

    private void buildNamesTable() {
        for (Enumeration<Entity> e = entityHash.elements() ; e.hasMoreElements() ; ) {
            Entity ent = e.nextElement();
            // Do even if not isGeneral().  That way, exclusions and inclusions
            // will definitely have their element.
            getNameId(ent.getName());
        }
        for (Enumeration<Element> e = elements.elements() ; e.hasMoreElements() ; ) {
            Element el = e.nextElement();
            getNameId(el.getName());
            for (AttributeList atts = el.getAttributes() ; atts != null ; atts = atts.getNext()) {
                getNameId(atts.getName());
                if (atts.getValue() != null) {
                    getNameId(atts.getValue());
                }
                Enumeration<?> vals = atts.getValues();
                while (vals != null && vals.hasMoreElements()) {
                    String s = (String) vals.nextElement();
                    getNameId(s);
                }
            }
        }
    }

    //
    // The the id of a name from the list of names
    //
    private short getNameId(String name)  {
        Integer o = namesHash.get(name);
        if (o != null) {
            return (short) o.intValue();
        }
        int i = namesVector.size();
        namesVector.addElement(name);
        namesHash.put(name, i);
        return (short) i;
    }


    /**
     * Save an entity to a stream.
     */
    void saveEntities(DataOutputStream out) throws IOException {
        int num = 0;
        for (Enumeration<Entity> e = entityHash.elements() ; e.hasMoreElements() ; ) {
            Entity ent = e.nextElement();
            if (ent.isGeneral()) {
                num++;
            }
        }

        out.writeShort((short) num);
        for (Enumeration<Entity> e = entityHash.elements() ; e.hasMoreElements() ; ) {
            Entity ent = e.nextElement();
            if (ent.isGeneral()) {
                out.writeShort(getNameId(ent.getName()));
                out.writeByte(ent.getType() & ~GENERAL);
                out.writeUTF(ent.getString());
            }
        }
    }


    /**
     * Save an element to a stream.
     */

    public void saveElement(DataOutputStream out, Element elem) throws IOException {

        out.writeShort(getNameId(elem.getName()));
        out.writeByte(elem.getType());

        byte flags = 0;
        if (elem.omitStart()) {
            flags |= 0x01;
        }
        if (elem.omitEnd()) {
            flags |= 0x02;
        }
        out.writeByte(flags);
        saveContentModel(out, elem.getContent());

        // Exclusions
        if (elem.exclusions == null) {
            out.writeShort(0);
        } else {
            short num = 0;
            for (int i = 0 ; i < elem.exclusions.size() ; i++) {
                if (elem.exclusions.get(i)) {
                    num++;
                }
            }
            out.writeShort(num);
            for (int i = 0 ; i < elem.exclusions.size() ; i++) {
                if (elem.exclusions.get(i)) {
                    out.writeShort(getNameId(getElement(i).getName()));
                }
            }
        }

        // Inclusions
        if (elem.inclusions == null) {
            out.writeShort(0);
        } else {
            short num = 0;
            for (int i = 0 ; i < elem.inclusions.size() ; i++) {
                if (elem.inclusions.get(i)) {
                    num++;
                }
            }
            out.writeShort(num);
            for (int i = 0 ; i < elem.inclusions.size() ; i++) {
                if (elem.inclusions.get(i)) {
                    out.writeShort(getNameId(getElement(i).getName()));
                }
            }
        }

        // Attributes
        {
            short numAtts = 0;
            for (AttributeList atts = elem.getAttributes() ; atts != null ; atts = atts.getNext()) {
                numAtts++;
            }
            out.writeByte(numAtts);
            for (AttributeList atts = elem.getAttributes() ; atts != null ; atts = atts.getNext()) {
                out.writeShort(getNameId(atts.getName()));
                out.writeByte(atts.getType());
                out.writeByte(atts.getModifier());
                if (atts.getValue() == null) {
                    out.writeShort(-1);
                } else {
                    out.writeShort(getNameId(atts.getValue()));
                }
                if (atts.values == null) {
                    out.writeShort(0);
                } else {
                    out.writeShort((short) atts.values.size());
                    for (int i = 0; i < atts.values.size(); i++) {
                        String s = (String) atts.values.elementAt(i);
                        out.writeShort(getNameId(s));
                    }
                }
            }
        }
    }


    /**
     * Save a content model to a stream. This does a
     * recursive decent of the entire model.
     */
    public void saveContentModel(DataOutputStream out, ContentModel model) throws IOException {
        if (model == null) {
            out.writeByte(0);
        } else if (model.content instanceof ContentModel) {
            out.writeByte(1);
            out.writeByte(model.type);
            saveContentModel(out, (ContentModel)model.content);

            saveContentModel(out, model.next);
        } else if (model.content instanceof Element) {
            out.writeByte(2);
            out.writeByte(model.type);
            out.writeShort(getNameId(((Element) model.content).getName()));

            saveContentModel(out, model.next);
        }
    }


    /**
     * Generate a class representing this DTD.
     */

    public static void main(String argv[]) {

        String dtd_home = System.getProperty("dtd_home");
        if (dtd_home == null) {
            System.err.println("Must set property 'dtd_home'");
            return;
        }

        DTDBuilder dtd = null;
        try {
            dtd = new DTDBuilder(argv[0]);
            mapping = new PublicMapping(dtd_home + File.separator, "public.map");
            String path = mapping.get(argv[0]);
            new DTDParser().parse(new FileInputStream(path), dtd);

        } catch (IOException e) {
            System.err.println("Could not open DTD file " + argv[0]);
            e.printStackTrace(System.err);
            System.exit(1);
        }
        try {
            DataOutputStream str = new DataOutputStream(System.out);
            dtd.save(str, argv[0]);
            str.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
