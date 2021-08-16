/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

public interface MerlinMXBean {

    int PInt = 59;
    SimpleType PIntType = SimpleType.INTEGER;
    int getPInt();
    void setPInt(int x);
    int opPInt(int x, int y);

    long PLong = Long.MAX_VALUE;
    SimpleType PLongType = SimpleType.LONG;
    long getPLong();
    void setPLong(long x);
    long opPLong(long x, long y);

    short PShort = 55;
    SimpleType PShortType = SimpleType.SHORT;
    short getPShort();
    void setPShort(short x);
    short opPShort(short x, short y);

    byte PByte = 13;
    SimpleType PByteType = SimpleType.BYTE;
    byte getPByte();
    void setPByte(byte x);
    byte opPByte(byte x, byte y);

    char PChar = 'x';
    SimpleType PCharType = SimpleType.CHARACTER;
    char getPChar();
    void setPChar(char x);
    char opPChar(char x, char y);

    float PFloat = 1.3f;
    SimpleType PFloatType = SimpleType.FLOAT;
    float getPFloat();
    void setPFloat(float x);
    float opPFloat(float x, float y);

    double PDouble = Double.MAX_VALUE;
    SimpleType PDoubleType = SimpleType.DOUBLE;
    double getPDouble();
    void setPDouble(double x);
    double opPDouble(double x, double y);

    boolean PBoolean = true;
    SimpleType PBooleanType = SimpleType.BOOLEAN;
    boolean getPBoolean();
    void setPBoolean(boolean x);
    boolean opPBoolean(boolean x, boolean y);

    Integer WInteger = new Integer(59);
    SimpleType WIntegerType = SimpleType.INTEGER;
    Integer getWInteger();
    void setWInteger(Integer x);
    Integer opWInteger(Integer x, Integer y);

    Long WLong = new Long(Long.MAX_VALUE);
    SimpleType WLongType = SimpleType.LONG;
    Long getWLong();
    void setWLong(Long x);
    Long opWLong(Long x, Long y);

    Short WShort = new Short(Short.MAX_VALUE);
    SimpleType WShortType = SimpleType.SHORT;
    Short getWShort();
    void setWShort(Short x);
    Short opWShort(Short x, Short y);

    Byte WByte = new Byte(Byte.MAX_VALUE);
    SimpleType WByteType = SimpleType.BYTE;
    Byte getWByte();
    void setWByte(Byte x);
    Byte opWByte(Byte x, Byte y);

    Character WCharacter = new Character('x');
    SimpleType WCharacterType = SimpleType.CHARACTER;
    Character getWCharacter();
    void setWCharacter(Character x);
    Character opWCharacter(Character x, Character y);

    Float WFloat = new Float(1.3f);
    SimpleType WFloatType = SimpleType.FLOAT;
    Float getWFloat();
    void setWFloat(Float x);
    Float opWFloat(Float x, Float y);

    Double WDouble = new Double(Double.MAX_VALUE);
    SimpleType WDoubleType = SimpleType.DOUBLE;
    Double getWDouble();
    void setWDouble(Double x);
    Double opWDouble(Double x, Double y);

    Boolean WBoolean = Boolean.TRUE;
    SimpleType WBooleanType = SimpleType.BOOLEAN;
    Boolean getWBoolean();
    void setWBoolean(Boolean x);
    Boolean opWBoolean(Boolean x, Boolean y);

    int[] PIntA = {2, 3, 5, 7, 11, 13};
    ArrayType PIntAType = ArrayTypeMaker.make(SimpleType.INTEGER, true);
    int[] getPIntA();
    void setPIntA(int[] x);
    int[] opPIntA(int[] x, int[] y);

    int[][] PInt2D = {{1, 2}, {3, 4}};
    ArrayType PInt2DType = ArrayTypeMaker.make(1, PIntAType);
    int[][] getPInt2D();
    void setPInt2D(int[][] x);
    int[][] opPInt2D(int[][] x, int[][] y);

    Integer[] WIntA = {new Integer(3), new Integer(5)};
    ArrayType WIntAType = ArrayTypeMaker.make(1, SimpleType.INTEGER);
    Integer[] getWIntA();
    void setWIntA(Integer[] x);
    Integer[] opWIntA(Integer[] x, Integer[] y);

    Integer[][] WInt2D = {{new Integer(3)}, {new Integer(5)}};
    ArrayType WInt2DType = ArrayTypeMaker.make(2, SimpleType.INTEGER);
    Integer[][] getWInt2D();
    void setWInt2D(Integer[][] x);
    Integer[][] opWInt2D(Integer[][] x, Integer[][] y);

    String XString = "yo!";
    SimpleType XStringType = SimpleType.STRING;
    String getXString();
    void setXString(String x);
    String opXString(String x, String y);

    String[] XStringA = {"hello", "world"};
    ArrayType XStringAType = ArrayTypeMaker.make(1, SimpleType.STRING);
    String[] getXStringA();
    void setXStringA(String[] x);
    String[] opXStringA(String[] x, String[] y);

    int[] NoInts = {};
    ArrayType NoIntsType = ArrayTypeMaker.make(SimpleType.INTEGER, true);
    int[] getNoInts();
    void setNoInts(int[] x);
    int[] opNoInts(int[] x, int[] y);

    GetSetBean GetSet = GetSetBean.make(5, "x", new String[] {"a", "b"});
    CompositeType GetSetType =
        CompositeTypeMaker.make(GetSetBean.class.getName(),
                                GetSetBean.class.getName(),
                                new String[] {"int", "string", "stringArray"},
                                new String[] {"int", "string", "stringArray"},
                                new OpenType[] {
                                    SimpleType.INTEGER,
                                    SimpleType.STRING,
                                    ArrayTypeMaker.make(1, SimpleType.STRING),
                                });
    GetSetBean getGetSet();
    void setGetSet(GetSetBean bean);
    GetSetBean opGetSet(GetSetBean x, GetSetBean y);

    GetterInterface Interface = new GetterInterface() {
        public boolean isWhatsit() {
            return true;
        }

        public int[] getInts() {
            return new int[] {1};
        }

        public String[] getStrings() {
            return new String[] {"x"};
        }

        public GetSetBean getGetSet() {
            return GetSetBean.make(3, "a", new String[] {"b"});
        }

        public boolean equals(Object o) {
            if (!(o instanceof GetterInterface))
                return false;
            GetterInterface i = (GetterInterface) o;
            return isWhatsit() == i.isWhatsit() &&
                   Arrays.equals(getInts(), i.getInts()) &&
                   Arrays.equals(getStrings(), i.getStrings()) &&
                   getGetSet().equals(i.getGetSet());
        }
     };
     CompositeType InterfaceType =
        CompositeTypeMaker.make(GetterInterface.class.getName(),
                                GetterInterface.class.getName(),
                                new String[] {
                                    "ints", "getSet", "strings", "whatsit",
                                },
                                new String[] {
                                    "ints", "getSet", "strings", "whatsit",
                                },
                                new OpenType[] {
                                    ArrayTypeMaker.make(SimpleType.INTEGER, true),
                                    GetSetType,
                                    ArrayTypeMaker.make(1, SimpleType.STRING),
                                    SimpleType.BOOLEAN,
                                });
     GetterInterface getInterface();
     void setInterface(GetterInterface i);
     GetterInterface opInterface(GetterInterface x, GetterInterface y);

    /* Testing that we can use a public no-arg constructor plus a setter
     * for every getter to reconstruct this object.  Note that the
     * constructor-guessing logic is no longer valid for this class,
     * so if we can reconstruct it it must be because of the setters.
     */
    public static class GetSetBean {
        public GetSetBean() {
            this(0, null, null);
        }

        private GetSetBean(int Int, String string, String[] stringArray) {
            this.Int = Int;
            this.string = string;
            this.stringArray = stringArray;
        }

        public static GetSetBean
                make(int Int, String string, String[] stringArray) {
            GetSetBean b = new GetSetBean(Int, string, stringArray);
            return b;
        }

        public int getInt() {
            return Int;
        }

        public String getString() {
            return this.string;
        }

        public String[] getStringArray() {
            return this.stringArray;
        }

        public void setInt(int x) {
            this.Int = x;
        }

        public void setString(String string) {
            this.string = string;
        }

        public void setStringArray(String[] stringArray) {
            this.stringArray = stringArray;
        }

        public boolean equals(Object o) {
            if (!(o instanceof GetSetBean))
                return false;
            GetSetBean b = (GetSetBean) o;
            return (b.Int == Int &&
                    b.string.equals(string) &&
                    Arrays.equals(b.stringArray, stringArray));
        }

        String string;
        String[] stringArray;
        int Int;
    }

    public static interface GetterInterface {
        public String[] getStrings();
        public int[] getInts();
        public boolean isWhatsit();
        public GetSetBean getGetSet();

        // We uselessly mention the public methods inherited from Object because
        // they should not prevent the interface from being translatable.
        // We could consider encoding the result of hashCode() and toString()
        // on the original object that implements this interface into the
        // generated CompositeData and referencing that in the proxy, but
        // that seems ambitious for now.  Doing it only if hashCode() and/or
        // toString() are mentioned in the interface is a possibility but
        // a rather abstruse one.
        public boolean equals(Object o);
        public int hashCode();
        public String toString();
    }

    public static class ArrayTypeMaker {
        static ArrayType make(int dims, OpenType baseType) {
            try {
                return new ArrayType(dims, baseType);
            } catch (OpenDataException e) {
                throw new Error(e);
            }
        }

        static ArrayType make(SimpleType baseType, boolean primitiveArray) {
            try {
                return new ArrayType(baseType, primitiveArray);
            } catch (OpenDataException e) {
                throw new Error(e);
            }
        }
    }

    public static class CompositeTypeMaker {
        static CompositeType make(String className,
                                  String description,
                                  String[] itemNames,
                                  String[] itemDescriptions,
                                  OpenType[] itemTypes) {
            try {
                return new CompositeType(className,
                                         description,
                                         itemNames,
                                         itemDescriptions,
                                         itemTypes);
            } catch (OpenDataException e) {
                throw new Error(e);
            }
        }
    }

    public static interface GraphMXBean {
        public NodeMXBean[] getNodes();
    }

    public static class Graph implements GraphMXBean {
        public Graph(Node... nodes) {
            for (Node node : nodes)
                node.setGraph(this);
            this.nodes = nodes;
        }

        public NodeMXBean[] getNodes() {
            return nodes;
        }

        private final Node[] nodes;
    }

    public static interface NodeMXBean {
        public String getName();
        public GraphMXBean getGraph();
    }

    public static class Node implements NodeMXBean {
        public Node(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public GraphMXBean getGraph() {
            return graph;
        }

        public void setGraph(Graph graph) {
            this.graph = graph;
        }

        private final String name;
        private Graph graph;
    }

    SimpleType GraphType = SimpleType.OBJECTNAME;
    GraphMXBean getGraph();
    void setGraph(GraphMXBean g);
    GraphMXBean opGraph(GraphMXBean x, GraphMXBean y);
    String GraphObjectName = "test:type=GraphMXBean";
    String NodeAObjectName = "test:type=NodeMXBean,name=a";
    String NodeBObjectName = "test:type=NodeMXBean,name=b";
    Node NodeA = new Node("a");
    Node NodeB = new Node("b");
    GraphMXBean Graph = new Graph(NodeA, NodeB);

    public static class ExoticCompositeData implements CompositeDataView {
        private ExoticCompositeData(String whatsit) {
            this.whatsit = whatsit;
        }

        public static ExoticCompositeData from(CompositeData cd) {
            String whatsit = (String) cd.get("whatsit");
            if (!whatsit.startsWith("!"))
                throw new IllegalArgumentException(whatsit);
            return new ExoticCompositeData(whatsit.substring(1));
        }

        public CompositeData toCompositeData(CompositeType ct) {
            try {
                return new CompositeDataSupport(ct, new String[] {"whatsit"},
                                                new String[] {"!" + whatsit});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String getWhatsit() {
            return whatsit;
        }

        public boolean equals(Object o) {
            return ((o instanceof ExoticCompositeData) &&
                    ((ExoticCompositeData) o).whatsit.equals(whatsit));
        }

        private final String whatsit;

        public static final CompositeType type;
        static {
            try {
                type =
                    new CompositeType(ExoticCompositeData.class.getName(),
                                      ExoticCompositeData.class.getName(),
                                      new String[] {"whatsit"},
                                      new String[] {"whatsit"},
                                      new OpenType[] {SimpleType.STRING});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    CompositeType ExoticType = ExoticCompositeData.type;
    ExoticCompositeData getExotic();
    void setExotic(ExoticCompositeData ecd);
    ExoticCompositeData opExotic(ExoticCompositeData ecd1,
                                 ExoticCompositeData ecd2);
    ExoticCompositeData Exotic = new ExoticCompositeData("foo");
}
