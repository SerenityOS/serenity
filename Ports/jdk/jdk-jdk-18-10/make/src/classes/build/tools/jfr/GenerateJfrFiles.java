package build.tools.jfr;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Purpose of this program is twofold:
 *
 * 1) Generate C++ classes to be used when writing native events for HotSpot.
 *
 * 2) Generate metadata (label, descriptions, field layout etc.) from XML
 * (metadata.xml) into a binary format (metadata.bin) that can be read quickly
 * during startup by the jdk.jfr module.
 *
 * INPUT FILES:
 *
 * -  metadata.xml  File that contains descriptions of events and types
 * -  metadata.xsd  Schema that verifies that metadata.xml is legit XML
 *
 * OUTPUT FILES:
 *
 * MODE: headers
 *
 * - jfrEventIds.hpp      List of IDs so events can be identified from native
 * - jfrTypes.hpp         List of IDs so types can be identified from native
 * - jfrPeriodic.hpp      Dispatch mechanism so Java can emit native periodic events
 * - jfrEventControl.hpp  Data structure for native event settings.
 * - jfrEventClasses.hpp  C++ event classes that can write data into native buffers
 *
 * MODE: metadata
 *
 *  - metadata.bin        Binary representation of the information in metadata.xml
 *
 */
public class GenerateJfrFiles {

    enum OutputMode {
        headers, metadata
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: java GenerateJfrFiles[.java]");
        out.println(" --mode <headers|metadata>");
        out.println(" --xml <path-to-metadata.xml> ");
        out.println(" --xsd <path-to-metadata.xsd>");
        out.println(" --output <output-file-or-directory>");
    }

    private static String consumeOption(String option, List<String> argList) throws Exception {
        int index = argList.indexOf(option);
        if (index >= 0 && index <= argList.size() - 2) {
            String result = argList.get(index + 1);
            argList.remove(index);
            argList.remove(index);
            return result;
        }
        throw new IllegalArgumentException("missing option " + option);
    }

    public static void main(String... args) throws Exception {
        try {
            List<String> argList = new ArrayList<>();
            argList.addAll(Arrays.asList(args));
            String mode = consumeOption("--mode", argList);
            String output = consumeOption("--output", argList);
            String xml = consumeOption("--xml", argList);
            String xsd = consumeOption("--xsd", argList);
            if (!argList.isEmpty()) {
                throw new IllegalArgumentException("unknown option " + argList);
            }
            OutputMode outputMode = OutputMode.valueOf(mode);
            File xmlFile = new File(xml);
            File xsdFile = new File(xsd);

            Metadata metadata = new Metadata(xmlFile, xsdFile);
            metadata.verify();
            metadata.wireUpTypes();

            if (outputMode == OutputMode.headers) {
                File outputDir = new File(output);
                printJfrEventIdsHpp(metadata, new File(outputDir, "jfrEventIds.hpp"));
                printJfrTypesHpp(metadata, new File(outputDir, "jfrTypes.hpp"));
                printJfrPeriodicHpp(metadata, new File(outputDir, "jfrPeriodic.hpp"));
                printJfrEventControlHpp(metadata, new File(outputDir, "jfrEventControl.hpp"));
                printJfrEventClassesHpp(metadata, new File(outputDir, "jfrEventClasses.hpp"));
            }

            if (outputMode == OutputMode.metadata) {
                File outputFile  = new File(output);
                try (var b = new DataOutputStream(
                        new BufferedOutputStream(
                            new FileOutputStream(outputFile)))) {
                    metadata.persist(b);
                }
            }
            System.exit(0);
        } catch (IllegalArgumentException iae) {
            System.err.println();
            System.err.println("GenerateJfrFiles: " + iae.getMessage());
            System.err.println();
            printUsage(System.err);
            System.err.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    static class XmlType {
        final String name;
        final String fieldType;
        final String parameterType;
        final String javaType;
        final boolean unsigned;
        final String contentType;

        XmlType(String name, String fieldType, String parameterType, String javaType, String contentType,
                boolean unsigned) {
            this.name = name;
            this.fieldType = fieldType;
            this.parameterType = parameterType;
            this.javaType = javaType;
            this.unsigned = unsigned;
            this.contentType = contentType;
        }
    }

    static class XmlContentType {
        final String name;
        final String annotation;

        XmlContentType(String name, String annotation) {
            this.name = name;
            this.annotation = annotation;
        }
    }

    static class TypeElement {
        List<FieldElement> fields = new ArrayList<>();
        String name;
        String javaType;
        String label = "";
        String description = "";
        String category = "";
        boolean thread;
        boolean stackTrace;
        boolean startTime;
        String period = "";
        boolean cutoff;
        boolean throttle;
        boolean experimental;
        long id;
        boolean isEvent;
        boolean isRelation;
        boolean supportStruct = false;
        String commitState;
        public boolean primitive;

        public void persist(DataOutputStream pos) throws IOException {
            pos.writeInt(fields.size());
            for (FieldElement field : fields) {
                field.persist(pos);
            }
            pos.writeUTF(javaType);
            pos.writeUTF(label);
            pos.writeUTF(description);
            pos.writeUTF(category);
            pos.writeBoolean(thread);
            pos.writeBoolean(stackTrace);
            pos.writeBoolean(startTime);
            pos.writeUTF(period);
            pos.writeBoolean(cutoff);
            pos.writeBoolean(throttle);
            pos.writeBoolean(experimental);
            pos.writeLong(id);
            pos.writeBoolean(isEvent);
            pos.writeBoolean(isRelation);
        }
    }

    static class Metadata {
        static class TypeCounter {
            final long first;
            long last = -1;
            long count = 0;
            long id = -1;

            TypeCounter(long startId) {
                this.first = startId;
            }

            long next() {
                id = (id == -1) ? first : id + 1;
                count++;
                last = id;
                return id;
            }
        }

        static int RESERVED_EVENT_COUNT = 2;
        final Map<String, TypeElement> types = new LinkedHashMap<>();
        final Map<String, XmlType> xmlTypes = new LinkedHashMap<>();
        final Map<String, XmlContentType> xmlContentTypes = new LinkedHashMap<>();
        int lastEventId;
        private TypeCounter eventCounter;
        private TypeCounter typeCounter;

        Metadata(File metadataXml, File metadataSchema)
                throws ParserConfigurationException, SAXException, FileNotFoundException, IOException {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setSchema(schemaFactory.newSchema(metadataSchema));
            SAXParser sp = factory.newSAXParser();
            sp.parse(metadataXml, new MetadataHandler(this));
        }

        public void persist(DataOutputStream pos) throws IOException {
            pos.writeInt(types.values().size());
            for (TypeElement t : types.values()) {
                t.persist(pos);
            }
        }

        List<TypeElement> getEvents() {
            return getList(t -> t.isEvent);
        }

        List<TypeElement> getEventsAndStructs() {
            return getList(t -> t.isEvent || t.supportStruct);
        }

        @SuppressWarnings("unchecked")
        <T> List<T> getList(Predicate<? super TypeElement> pred) {
            List<T> result = new ArrayList<>(types.size());
            for (TypeElement t : types.values()) {
                if (pred.test(t)) {
                    result.add((T) t);
                }
            }
            return result;
        }

        List<TypeElement> getPeriodicEvents() {
            return getList(t -> t.isEvent && !t.period.isEmpty());
        }

        List<TypeElement> getTypes() {
            return getList(t -> !t.isEvent);
        }

        List<TypeElement> getStructs() {
            return getList(t -> !t.isEvent && t.supportStruct);
        }

        void verify() {
            for (TypeElement t : types.values()) {
                for (FieldElement f : t.fields) {
                    if (!xmlTypes.containsKey(f.typeName)) { // ignore primitives
                        if (!types.containsKey(f.typeName)) {
                            throw new IllegalStateException("Could not find definition of type '" + f.typeName
                                    + "' used by " + t.name + "#" + f.name);
                        }
                    }
                }
            }
        }

        void wireUpTypes() {
            // Add Java primitives
            for (var t : xmlTypes.entrySet()) {
                String name = t.getKey();
                XmlType xmlType = t.getValue();
                // Excludes Thread and Class
                if (!types.containsKey(name)) {
                    // Excludes u8, u4, u2, u1, Ticks and Ticksspan
                    if (!xmlType.javaType.isEmpty() && !xmlType.unsigned) {
                        TypeElement te = new TypeElement();
                        te.name = name;
                        te.javaType = xmlType.javaType;
                        te.primitive = true;
                        types.put(te.name, te);
                    }
                }
            }
            // Setup Java fully qualified names
            for (TypeElement t : types.values()) {
                if (t.isEvent) {
                    t.javaType = "jdk." + t.name;
                } else {
                    XmlType xmlType = xmlTypes.get(t.name);
                    if (xmlType != null && !xmlType.javaType.isEmpty()) {
                        t.javaType = xmlType.javaType;
                    } else {
                        t.javaType = "jdk.types." + t.name;
                    }
                }
            }
            // Setup content type, annotation, constant pool etc. for fields.
            for (TypeElement t : types.values()) {
                for (FieldElement f : t.fields) {
                    TypeElement type = types.get(f.typeName);
                    XmlType xmlType = xmlTypes.get(f.typeName);
                    if (type == null) {
                        if (xmlType == null) {
                            throw new IllegalStateException("Unknown type");
                        }
                        if (f.contentType.isEmpty()) {
                            f.contentType = xmlType.contentType;
                        }
                        String javaType = xmlType.javaType;
                        type = types.get(javaType);
                        Objects.requireNonNull(type);
                    }
                    if (type.primitive) {
                        f.constantPool = false;
                    }

                    if (xmlType != null) {
                        f.unsigned = xmlType.unsigned;
                    }

                    if (f.struct) {
                        f.constantPool = false;
                        type.supportStruct = true;
                    }
                    f.type = type;
                    XmlContentType xmlContentType = xmlContentTypes.get(f.contentType);
                    if (xmlContentType == null) {
                        f.annotations = "";
                    } else {
                        f.annotations = xmlContentType.annotation;
                    }
                    if (!f.relation.isEmpty()) {
                        f.relation = "jdk.types." + f.relation;
                    }
                }
            }

            // Low numbers for event so most of them
            // can fit in one byte with compressed integers
            eventCounter = new TypeCounter(RESERVED_EVENT_COUNT);
            for (TypeElement t : getEvents()) {
                t.id = eventCounter.next();
            }
            typeCounter = new TypeCounter(eventCounter.last + 1);
            for (TypeElement t : getTypes()) {
                t.id = typeCounter.next();
            }
        }

        public String getName(long id) {
            for (TypeElement t : types.values()) {
                if (t.id == id) {
                    return t.name;
                }
            }
            throw new IllegalStateException("Unexpected id " + id );
        }
    }

    static class FieldElement {
        final Metadata metadata;
        TypeElement type;
        String name;
        String typeName;
        boolean constantPool = true;
        public String transition;
        public String contentType;
        private String label;
        private String description;
        private String relation;
        private boolean experimental;
        private boolean unsigned;
        private boolean array;
        private String annotations;
        public boolean struct;

        FieldElement(Metadata metadata) {
            this.metadata = metadata;
        }

        public void persist(DataOutputStream pos) throws IOException {
            pos.writeUTF(name);
            pos.writeUTF(type.javaType);
            pos.writeUTF(label);
            pos.writeUTF(description);
            pos.writeBoolean(constantPool);
            pos.writeBoolean(array);
            pos.writeBoolean(unsigned);
            pos.writeUTF(annotations);
            pos.writeUTF(transition);
            pos.writeUTF(relation);
            pos.writeBoolean(experimental);
        }

        String getParameterType() {
            if (struct) {
                return "const JfrStruct" + typeName + "&";
            }
            XmlType xmlType = metadata.xmlTypes.get(typeName);
            if (xmlType != null) {
                return xmlType.parameterType;
            }
            return type != null ? "u8" : typeName;
        }

        String getParameterName() {
            return struct ? "value" : "new_value";
        }

        String getFieldType() {
            if (struct) {
                return "JfrStruct" + typeName;
            }
            XmlType xmlType = metadata.xmlTypes.get(typeName);
            if (xmlType != null) {
                return xmlType.fieldType;
            }
            return type != null ? "u8" : typeName;
        }
    }

    static class MetadataHandler extends DefaultHandler {
        final Metadata metadata;
        FieldElement currentField;
        TypeElement currentType;

        MetadataHandler(Metadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            switch (qName) {
            case "XmlContentType":
                String n = attributes.getValue("name"); // mandatory
                String a = attributes.getValue("annotation"); // mandatory
                metadata.xmlContentTypes.put(n, new XmlContentType(n, a));
                break;
            case "XmlType":
                String name = attributes.getValue("name"); // mandatory
                String parameterType = attributes.getValue("parameterType"); // mandatory
                String fieldType = attributes.getValue("fieldType"); // mandatory
                String javaType = getString(attributes, "javaType");
                String contentType = getString(attributes, "contentType");
                boolean unsigned = getBoolean(attributes, "unsigned", false);
                metadata.xmlTypes.put(name,
                        new XmlType(name, fieldType, parameterType, javaType, contentType, unsigned));
                break;
            case "Relation":
            case "Type":
            case "Event":
                currentType = new TypeElement();
                currentType.name = attributes.getValue("name"); // mandatory
                currentType.label = getString(attributes, "label");
                currentType.description = getString(attributes, "description");
                currentType.category = getString(attributes, "category");
                currentType.experimental = getBoolean(attributes, "experimental", false);
                currentType.thread = getBoolean(attributes, "thread", false);
                currentType.stackTrace = getBoolean(attributes, "stackTrace", false);
                currentType.startTime = getBoolean(attributes, "startTime", true);
                currentType.period = getString(attributes, "period");
                currentType.cutoff = getBoolean(attributes, "cutoff", false);
                currentType.throttle = getBoolean(attributes, "throttle", false);
                currentType.commitState = getString(attributes, "commitState");
                currentType.isEvent = "Event".equals(qName);
                currentType.isRelation = "Relation".equals(qName);
                break;
            case "Field":
                currentField = new FieldElement(metadata);
                currentField.name = attributes.getValue("name"); // mandatory
                currentField.typeName = attributes.getValue("type"); // mandatory
                currentField.label = getString(attributes, "label");
                currentField.description = getString(attributes, "description");
                currentField.contentType = getString(attributes, "contentType");
                currentField.struct = getBoolean(attributes, "struct", false);
                currentField.array = getBoolean(attributes, "array", false);
                currentField.transition = getString(attributes, "transition");
                currentField.relation = getString(attributes, "relation");
                currentField.experimental = getBoolean(attributes, "experimental", false);
                break;
            }
        }

        private static String getString(Attributes attributes, String name) {
            String value = attributes.getValue(name);
            return value != null ? value : "";
        }

        private static boolean getBoolean(Attributes attributes, String name, boolean defaultValue) {
            String value = attributes.getValue(name);
            return value == null ? defaultValue : Boolean.valueOf(value);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName) {
            case "Relation":
            case "Type":
            case "Event":
                metadata.types.put(currentType.name, currentType);
                currentType = null;
                break;
            case "Field":
                currentType.fields.add(currentField);
                currentField = null;
                break;
            }
        }
    }

    static class Printer implements Closeable {
        final PrintStream out;

        Printer(File outputFile) throws FileNotFoundException {
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            write("/* AUTOMATICALLY GENERATED FILE - DO NOT EDIT */");
            write("");
        }

        void write(String text) {
            out.print(text);
            out.print("\n"); // Don't use Windows line endings
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }

    private static void printJfrPeriodicHpp(Metadata metadata, File outputFile) throws Exception {
        try (var out = new Printer(outputFile)) {
            out.write("#ifndef JFRFILES_JFRPERIODICEVENTSET_HPP");
            out.write("#define JFRFILES_JFRPERIODICEVENTSET_HPP");
            out.write("");
            out.write("#include \"utilities/macros.hpp\"");
            out.write("#if INCLUDE_JFR");
            out.write("#include \"jfrfiles/jfrEventIds.hpp\"");
            out.write("#include \"memory/allocation.hpp\"");
            out.write("");
            out.write("class JfrPeriodicEventSet : public AllStatic {");
            out.write(" public:");
            out.write("  static void requestEvent(JfrEventId id) {");
            out.write("    switch(id) {");
            out.write("  ");
            for (TypeElement e : metadata.getPeriodicEvents()) {
                out.write("      case Jfr" + e.name + "Event:");
                out.write("        request" + e.name + "();");
                out.write("        break;");
                out.write("  ");
            }
            out.write("      default:");
            out.write("        break;");
            out.write("      }");
            out.write("    }");
            out.write("");
            out.write(" private:");
            out.write("");
            for (TypeElement e : metadata.getPeriodicEvents()) {
                out.write("  static void request" + e.name + "(void);");
                out.write("");
            }
            out.write("};");
            out.write("");
            out.write("#endif // INCLUDE_JFR");
            out.write("#endif // JFRFILES_JFRPERIODICEVENTSET_HPP");
        }
    }

    private static void printJfrEventControlHpp(Metadata metadata, File outputFile) throws Exception {
        try (var out = new Printer(outputFile)) {
            out.write("#ifndef JFRFILES_JFR_NATIVE_EVENTSETTING_HPP");
            out.write("#define JFRFILES_JFR_NATIVE_EVENTSETTING_HPP");
            out.write("");
            out.write("#include \"utilities/macros.hpp\"");
            out.write("#if INCLUDE_JFR");
            out.write("#include \"jfrfiles/jfrEventIds.hpp\"");
            out.write("");
            out.write("/**");
            out.write(" * Event setting. We add some padding so we can use our");
            out.write(" * event IDs as indexes into this.");
            out.write(" */");
            out.write("");
            out.write("struct jfrNativeEventSetting {");
            out.write("  jlong  threshold_ticks;");
            out.write("  jlong  cutoff_ticks;");
            out.write("  u1     stacktrace;");
            out.write("  u1     enabled;");
            out.write("  u1     large;");
            out.write("  u1     pad[5]; // Because GCC on linux ia32 at least tries to pack this.");
            out.write("};");
            out.write("");
            out.write("union JfrNativeSettings {");
            out.write("  // Array version.");
            out.write("  jfrNativeEventSetting bits[NUMBER_OF_EVENTS];");
            out.write("  // Then, to make it easy to debug,");
            out.write("  // add named struct members also.");
            out.write("  struct {");
            out.write("    jfrNativeEventSetting pad[NUMBER_OF_RESERVED_EVENTS];");
            for (TypeElement t : metadata.getEventsAndStructs()) {
                out.write("    jfrNativeEventSetting " + t.name + ";");
            }
            out.write("  } ev;");
            out.write("};");
            out.write("");
            out.write("#endif // INCLUDE_JFR");
            out.write("#endif // JFRFILES_JFR_NATIVE_EVENTSETTING_HPP");
        }
    }

    private static void printJfrEventIdsHpp(Metadata metadata, File outputFile) throws Exception {
        try (var out = new Printer(outputFile)) {
            out.write("#ifndef JFRFILES_JFREVENTIDS_HPP");
            out.write("#define JFRFILES_JFREVENTIDS_HPP");
            out.write("");
            out.write("#include \"utilities/macros.hpp\"");
            out.write("#if INCLUDE_JFR");
            out.write("");
            out.write("enum JfrEventId {");
            out.write("  JfrMetadataEvent = 0,");
            out.write("  JfrCheckpointEvent = 1,");
            for (TypeElement t : metadata.getEvents()) {
                out.write("  " + jfrEventId(t.name) + " = " + t.id + ",");
            }
            out.write("};");
            out.write("typedef enum JfrEventId JfrEventId;");
            out.write("");
            String first = metadata.getName(metadata.eventCounter.first);
            String last = metadata.getName(metadata.eventCounter.last);
            out.write("static const JfrEventId FIRST_EVENT_ID = " + jfrEventId(first) + ";");
            out.write("static const JfrEventId LAST_EVENT_ID = " + jfrEventId(last) + ";");
            out.write("static const int NUMBER_OF_EVENTS = " + metadata.eventCounter.count + ";");
            out.write("static const int NUMBER_OF_RESERVED_EVENTS = " + Metadata.RESERVED_EVENT_COUNT + ";");
            out.write("#endif // INCLUDE_JFR");
            out.write("#endif // JFRFILES_JFREVENTIDS_HPP");
        }
    }

    private static String jfrEventId(String name) {
        return "Jfr" + name + "Event";
    }

    private static void printJfrTypesHpp(Metadata metadata, File outputFile) throws Exception {
        try (var out = new Printer(outputFile)) {
            out.write("#ifndef JFRFILES_JFRTYPES_HPP");
            out.write("#define JFRFILES_JFRTYPES_HPP");
            out.write("");
            out.write("#include \"utilities/macros.hpp\"");
            out.write("#if INCLUDE_JFR");
            out.write("");
            out.write("#include <string.h>");
            out.write("#include \"memory/allocation.hpp\"");
            out.write("");
            out.write("enum JfrTypeId {");
            for (TypeElement type : metadata.getTypes()) {
                out.write("  " + jfrTypeId(type.name) + " = " + type.id + ",");
            }
            out.write("};");
            out.write("");
            String first = metadata.getName(metadata.typeCounter.first);
            String last = metadata.getName(metadata.typeCounter.last);
            out.write("static const JfrTypeId FIRST_TYPE_ID = " + jfrTypeId(first) + ";");
            out.write("static const JfrTypeId LAST_TYPE_ID = " + jfrTypeId(last) + ";");
            out.write("");
            out.write("class JfrType : public AllStatic {");
            out.write(" public:");
            out.write("  static jlong name_to_id(const char* type_name) {");

            Map<String, XmlType> javaTypes = new LinkedHashMap<>();
            for (XmlType xmlType : metadata.xmlTypes.values()) {
                if (!xmlType.javaType.isEmpty()) {
                    javaTypes.put(xmlType.javaType, xmlType);
                }
            }
            for (XmlType xmlType : javaTypes.values()) {
                String javaName = xmlType.javaType;
                String typeName = xmlType.name.toUpperCase();
                out.write("    if (strcmp(type_name, \"" + javaName + "\") == 0) {");
                out.write("      return TYPE_" + typeName + ";");
                out.write("    }");
            }
            out.write("    return -1;");
            out.write("  }");
            out.write("};");
            out.write("");
            out.write("#endif // INCLUDE_JFR");
            out.write("#endif // JFRFILES_JFRTYPES_HPP");
        }
    }

    private static String jfrTypeId(String name) {
        return  "TYPE_" + name.toUpperCase();
    }

    private static void printJfrEventClassesHpp(Metadata metadata, File outputFile) throws Exception {
        try (var out = new Printer(outputFile)) {
            out.write("#ifndef JFRFILES_JFREVENTCLASSES_HPP");
            out.write("#define JFRFILES_JFREVENTCLASSES_HPP");
            out.write("");
            out.write("#include \"oops/klass.hpp\"");
            out.write("#include \"jfrfiles/jfrTypes.hpp\"");
            out.write("#include \"jfr/utilities/jfrTypes.hpp\"");
            out.write("#include \"utilities/macros.hpp\"");
            out.write("#include \"utilities/ticks.hpp\"");
            out.write("#if INCLUDE_JFR");
            out.write("#include \"jfr/recorder/service/jfrEvent.hpp\"");
            out.write("#include \"jfr/support/jfrEpochSynchronization.hpp\"");
            out.write("/*");
            out.write(" * Each event class has an assert member function verify() which is invoked");
            out.write(" * just before the engine writes the event and its fields to the data stream.");
            out.write(" * The purpose of verify() is to ensure that all fields in the event are initialized");
            out.write(" * and set before attempting to commit.");
            out.write(" *");
            out.write(" * We enforce this requirement because events are generally stack allocated and therefore");
            out.write(" * *not* initialized to default values. This prevents us from inadvertently committing");
            out.write(" * uninitialized values to the data stream.");
            out.write(" *");
            out.write(" * The assert message contains both the index (zero based) as well as the name of the field.");
            out.write(" */");
            out.write("");
            printTypes(out, metadata, false);
            out.write("");
            out.write("");
            out.write("#else // !INCLUDE_JFR");
            out.write("");
            out.write("template <typename T>");
            out.write("class JfrEvent {");
            out.write(" public:");
            out.write("  JfrEvent() {}");
            out.write("  void set_starttime(const Ticks&) const {}");
            out.write("  void set_endtime(const Ticks&) const {}");
            out.write("  bool should_commit() const { return false; }");
            out.write("  bool is_started() const { return false; }");
            out.write("  static bool is_enabled() { return false; }");
            out.write("  void commit() {}");
            out.write("};");
            out.write("");
            printTypes(out, metadata, true);
            out.write("");
            out.write("");
            out.write("#endif // INCLUDE_JFR");
            out.write("#endif // JFRFILES_JFREVENTCLASSES_HPP");
        }
    }

    private static void printTypes(Printer out, Metadata metadata, boolean empty) {
        for (TypeElement t : metadata.getStructs()) {
            printType(out, t, empty);
            out.write("");
        }
        for (TypeElement e : metadata.getEvents()) {
            printEvent(out, e, empty);
            out.write("");
        }
    }

    private static void printType(Printer out, TypeElement t, boolean empty) {
        out.write("struct JfrStruct" + t.name);
        out.write("{");
        if (!empty) {
            out.write(" private:");
            for (FieldElement f : t.fields) {
                printField(out, f);
            }
            out.write("");
        }
        out.write(" public:");
        for (FieldElement f : t.fields) {
            printTypeSetter(out, f, empty);
        }
        out.write("");
        if (!empty) {
            printWriteData(out, t);
        }
        out.write("};");
        out.write("");
    }

    private static void printEvent(Printer out, TypeElement event, boolean empty) {
        out.write("class Event" + event.name + " : public JfrEvent<Event" + event.name + ">");
        out.write("{");
        if (!empty) {
            out.write(" private:");
            for (FieldElement f : event.fields) {
                printField(out, f);
            }
            out.write("");
        }
        out.write(" public:");
        if (!empty) {
            out.write("  static const bool hasThread = " + event.thread + ";");
            out.write("  static const bool hasStackTrace = " + event.stackTrace + ";");
            out.write("  static const bool isInstant = " + !event.startTime + ";");
            out.write("  static const bool hasCutoff = " + event.cutoff + ";");
            out.write("  static const bool hasThrottle = " + event.throttle + ";");
            out.write("  static const bool isRequestable = " + !event.period.isEmpty() + ";");
            out.write("  static const JfrEventId eventId = Jfr" + event.name + "Event;");
            out.write("");
        }
        if (!empty) {
            out.write("  Event" + event.name + "(EventStartTime timing=TIMED) : JfrEvent<Event" + event.name
                    + ">(timing) {}");
        } else {
            out.write("  Event" + event.name + "(EventStartTime timing=TIMED) {}");
        }
        out.write("");
        int index = 0;
        for (FieldElement f : event.fields) {
            out.write("  void set_" + f.name + "(" + f.getParameterType() + " " + f.getParameterName() + ") {");
            if (!empty) {
                out.write("    this->_" + f.name + " = " + f.getParameterName() + ";");
                out.write("    DEBUG_ONLY(set_field_bit(" + index++ + "));");
            }
            out.write("  }");
        }
        out.write("");
        if (!empty) {
            printWriteData(out, event);
            out.write("");
        }
        out.write("  using JfrEvent<Event" + event.name
                + ">::commit; // else commit() is hidden by overloaded versions in this class");
        printConstructor2(out, event, empty);
        printCommitMethod(out, event, empty);
        if (!empty) {
            printVerify(out, event.fields);
        }
        out.write("};");
    }

    private static void printWriteData(Printer out, TypeElement type) {
        out.write("  template <typename Writer>");
        out.write("  void writeData(Writer& w) {");
        if (("_thread_in_native").equals(type.commitState)) {
            out.write("    // explicit epoch synchronization check");
            out.write("    JfrEpochSynchronization sync;");
        }
        for (FieldElement field : type.fields) {
            if (field.struct) {
                out.write("    _" + field.name + ".writeData(w);");
            } else {
                out.write("    w.write(_" + field.name + ");");
            }
        }
        out.write("  }");
    }

    private static void printTypeSetter(Printer out, FieldElement field, boolean empty) {
        if (!empty) {
            out.write("  void set_" + field.name + "(" + field.getParameterType() + " new_value) { this->_" + field.name
                    + " = new_value; }");
        } else {
            out.write("  void set_" + field.name + "(" + field.getParameterType() + " new_value) { }");
        }
    }

    private static void printVerify(Printer out, List<FieldElement> fields) {
        out.write("");
        out.write("#ifdef ASSERT");
        out.write("  void verify() const {");
        int index = 0;
        for (FieldElement f : fields) {
            out.write("    assert(verify_field_bit(" + index++
                    + "), \"Attempting to write an uninitialized event field: %s\", \"_" + f.name + "\");");
        }
        out.write("  }");
        out.write("#endif");
    }

    private static void printCommitMethod(Printer out, TypeElement event, boolean empty) {
        if (event.startTime) {
            StringJoiner sj = new StringJoiner(",\n              ");
            for (FieldElement f : event.fields) {
                sj.add(f.getParameterType() + " " + f.name);
            }
            out.write("");
            out.write("  void commit(" + sj.toString() + ") {");
            if (!empty) {
                out.write("    if (should_commit()) {");
                for (FieldElement f : event.fields) {
                    out.write("      set_" + f.name + "(" + f.name + ");");
                }
                out.write("      commit();");
                out.write("    }");
            }
            out.write("  }");
        }

        // Avoid clash with static commit() method
        if (event.fields.isEmpty()) {
            return;
        }

        out.write("");
        StringJoiner sj = new StringJoiner(",\n                     ");
        if (event.startTime) {
            sj.add("const Ticks& startTicks");
            sj.add("const Ticks& endTicks");
        }
        for (FieldElement f : event.fields) {
            sj.add(f.getParameterType() + " " + f.name);
        }
        out.write("  static void commit(" + sj.toString() + ") {");
        if (!empty) {
            out.write("    Event" + event.name + " me(UNTIMED);");
            out.write("");
            out.write("    if (me.should_commit()) {");
            if (event.startTime) {
                out.write("      me.set_starttime(startTicks);");
                out.write("      me.set_endtime(endTicks);");
            }
            for (FieldElement f : event.fields) {
                out.write("      me.set_" + f.name + "(" + f.name + ");");
            }
            out.write("      me.commit();");
            out.write("    }");
        }
        out.write("  }");
    }

    private static void printConstructor2(Printer out, TypeElement event, boolean empty) {
        if (!event.startTime) {
            out.write("");
            out.write("");
        }
        if (event.startTime) {
            out.write("");
            out.write("  Event" + event.name + "(");
            StringJoiner sj = new StringJoiner(",\n    ");
            for (FieldElement f : event.fields) {
                sj.add(f.getParameterType() + " " + f.name);
            }
            if (!empty) {
                out.write("    " + sj.toString() + ") : JfrEvent<Event" + event.name + ">(TIMED) {");
                out.write("    if (should_commit()) {");
                for (FieldElement f : event.fields) {
                    out.write("      set_" + f.name + "(" + f.name + ");");
                }
                out.write("    }");
            } else {
                out.write("    " + sj.toString() + ") {");
            }
            out.write("  }");
        }
    }

    private static void printField(Printer out, FieldElement field) {
        out.write("  " + field.getFieldType() + " _" + field.name + ";");
    }
}
