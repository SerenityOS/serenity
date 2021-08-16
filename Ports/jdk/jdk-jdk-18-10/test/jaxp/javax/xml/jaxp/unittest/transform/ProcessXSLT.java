/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: Process.java,v 1.2.4.2 2005/09/15 18:21:57 jeffsuttor Exp $
 */

// This file is a copied and modified version of
// com/sun/org/apache/xalan/internal/xslt/Process.java
// which has been modified to only use public exported APIs.
// The only adherence is with
// com.sun.org.apache.xml.internal.utils.DefaultErrorHandler
// which we try to instantiate using reflection, as that class
// can do a better job at reporting error location.
// We however don't have a hard dependency on it. We will use
// our own ErrorHandler if the default one is not accessible.
//

package transform;

import static jaxp.library.JAXPTestUtilities.getSystemProperty;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * The main() method handles the Xalan command-line interface.
 */
public class ProcessXSLT
{

    /**
     * Prints argument options.
     *
     */
    protected static void printArgOptions() {
        System.out.println("xslproc options: ");
        System.out.println("\n\t\t\t" + "-Common Options-" + "\n");
        System.out.println("   [-XSLTC (use XSLTC for transformation)]");  //"    [-XSLTC (use XSLTC for transformation)]
        System.out.println("   [-IN inputXMLURL]");  //"    [-IN inputXMLURL]");
        System.out.println("   [-XSL XSLTransformationURL]");  //"   [-XSL XSLTransformationURL]");
        System.out.println("   [-OUT outputFileName]");  //"   [-OUT outputFileName]");

        System.out.println("   [-E (Do not expand entity refs)]");  //"   [-V (Version info)]");

        System.out.println("   [-EDUMP {optional filename} (Do stackdump on error.)]");  //"   [-EDUMP {optional filename} (Do stackdump on error.)]");
        System.out.println("   [-XML (Use XML formatter and add XML header.)]");  //"   [-XML (Use XML formatter and add XML header.)]");
        System.out.println("   [-TEXT (Use simple Text formatter.)]");  //"   [-TEXT (Use simple Text formatter.)]");
        System.out.println("   [-HTML (Use HTML formatter.)]");  //"   [-HTML (Use HTML formatter.)]");
        System.out.println( "   [-PARAM name expression (Set a stylesheet parameter)]");  //"   [-PARAM name expression (Set a stylesheet parameter)]");

        System.out.println("   [-MEDIA mediaType (use media attribute to find stylesheet associated with a document.)]");
        System.out.println("   [-FLAVOR flavorName (Explicitly use s2s=SAX or d2d=DOM to do transform.)] ");
        System.out.println("   [-DIAG (Print overall milliseconds transform took.)]");
        System.out.println("   [-URIRESOLVER full class name (URIResolver to be used to resolve URIs)]");  //"   [-URIRESOLVER full class name (URIResolver to be used to resolve URIs)]");
        System.out.println("   [-ENTITYRESOLVER full class name (EntityResolver to be used to resolve entities)]");  //"   [-ENTITYRESOLVER full class name (EntityResolver to be used to resolve entities)]");
        waitForReturnKey();
        System.out.println("   [-CONTENTHANDLER full class name (ContentHandler to be used to serialize output)]");  //"   [-CONTENTHANDLER full class name (ContentHandler to be used to serialize output)]");
        System.out.println("   [-SECURE (set the secure processing feature to true.)]"); //"   [-SECURE (set the secure processing feature to true)]");


        System.out.println("\n\t\t\t"+  "-Options for XSLTC-" + "\n");
        System.out.println("   [-XO [transletName] (assign the name to the generated translet)]");
        waitForReturnKey();
        System.out.println("   [-XD destinationDirectory (specify a destination directory for translet)]");
        System.out.println("   [-XJ jarfile (packages translet classes into a jar file of name <jarfile>)]");
        System.out.println("   [-XP package (specifies a package name prefix for all generated translet classes)]");
        System.out.println("   [-XN (enables template inlining)]");
        System.out.println("   [-XX (turns on additional debugging message output)]");
        System.out.println("   [-XT (use translet to transform if possible)]");
    }

  /**
   * Command line interface to transform an XML document according to
   * the instructions found in an XSL stylesheet.
   * <p>The Process class provides basic functionality for
   * performing transformations from the command line.  To see a
   * list of arguments supported, call with zero arguments.</p>
   * <p>To set stylesheet parameters from the command line, use
   * <code>-PARAM name expression</code>. If you want to set the
   * parameter to a string value, simply pass the string value
   * as-is, and it will be interpreted as a string.  (Note: if
   * the value has spaces in it, you may need to quote it depending
   * on your shell environment).</p>
   *
   * @param argv Input parameters from command line
   */
    public static void main(String argv[]) {

        boolean doStackDumpOnError = false;
        boolean doDiag = false;
        boolean setQuietMode = false;
        String msg = null;
        boolean isSecureProcessing = false;

        /**
         * The default diagnostic writer...
         */
        java.io.PrintWriter diagnosticsWriter = new PrintWriter(System.err, true);
        java.io.PrintWriter dumpWriter = diagnosticsWriter;
        String flavor = "s2s";

        if (argv.length < 1) {
            printArgOptions();
        } else {
             // J2SE does not support Xalan interpretive
            // false -> true
            boolean useXSLTC = true;
            for (int i = 0; i < argv.length; i++) {
                if ("-XSLTC".equalsIgnoreCase(argv[i])) {
                    useXSLTC = true;
                }
            }

            TransformerFactory tfactory;
            if (useXSLTC) {
                String key = "javax.xml.transform.TransformerFactory";
                String value = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
                Properties props = System.getProperties();
                props.put(key, value);
                System.setProperties(props);
            }

            try {
                tfactory = TransformerFactory.newInstance();
                tfactory.setErrorListener(createDefaultErrorListener());
            } catch (TransformerFactoryConfigurationError pfe) {
                pfe.printStackTrace(dumpWriter);
                //      "XSL Process was not successful.");
                msg = "XSL Process was not successful.";
                diagnosticsWriter.println(msg);

                tfactory = null;  // shut up compiler

                doExit(msg);
            }

            boolean formatOutput = false;
            boolean useSourceLocation = false;
            String inFileName = null;
            String outFileName = null;
            String dumpFileName = null;
            String xslFileName = null;
            String treedumpFileName = null;
            String outputType = null;
            String media = null;
            List<String> params = new ArrayList<>();
            boolean quietConflictWarnings = false;
            URIResolver uriResolver = null;
            EntityResolver entityResolver = null;
            ContentHandler contentHandler = null;
            int recursionLimit = -1;

            for (int i = 0; i < argv.length; i++) {
                if ("-XSLTC".equalsIgnoreCase(argv[i])) {
                    // The -XSLTC option has been processed.
                } // J2SE does not support Xalan interpretive
                else if ("-INDENT".equalsIgnoreCase(argv[i])) {
                    int indentAmount;

                    if (((i + 1) < argv.length) && (argv[i + 1].charAt(0) != '-')) {
                        indentAmount = Integer.parseInt(argv[++i]);
                    } else {
                        indentAmount = 0;
                    }

                } else if ("-IN".equalsIgnoreCase(argv[i])) {
                    if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                        inFileName = argv[++i];
                    } else {
                        System.err.println("Missing argument for -IN");
                    }
                } else if ("-MEDIA".equalsIgnoreCase(argv[i])) {
                    if (i + 1 < argv.length) {
                        media = argv[++i];
                    } else {
                        System.err.println("Missing argument for -MEDIA");  //"Missing argument for);
                    }
                } else if ("-OUT".equalsIgnoreCase(argv[i])) {
                    if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                        outFileName = argv[++i];
                    } else {
                        System.err.println("Missing argument for -OUT");  //"Missing argument for);
                    }
                } else if ("-XSL".equalsIgnoreCase(argv[i])) {
                    if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                        xslFileName = argv[++i];
                    } else {
                        System.err.println("Missing argument for -XSL");  //"Missing argument for);
                    }
                } else if ("-FLAVOR".equalsIgnoreCase(argv[i])) {
                    if (i + 1 < argv.length) {
                        flavor = argv[++i];
                    } else {
                        System.err.println("Missing argument for -FLAVOR");  //"Missing argument for);
                    }
                } else if ("-PARAM".equalsIgnoreCase(argv[i])) {
                    if (i + 2 < argv.length) {
                        String name = argv[++i];

                        params.add(name);

                        String expression = argv[++i];

                        params.add(expression);
                    } else {
                        System.err.println("Missing argument for -PARAM");  //"Missing argument for);
                    }
                } else if ("-E".equalsIgnoreCase(argv[i])) {

                } else if ("-V".equalsIgnoreCase(argv[i])) {
                    diagnosticsWriter.println(">>>>>>> Java Version "
                            + getSystemProperty("java.version") + ", "
                            + /* xmlProcessorLiaison.getParserDescription()+ */ "<<<<<<<");
                } // J2SE does not support Xalan interpretive
                /*
                 else if ("-QC".equalsIgnoreCase(argv[i]))
                 {
                 if (!useXSLTC)
                 quietConflictWarnings = true;
                 else
                 printInvalidXSLTCOption("-QC");
                 }
                 */ else if ("-Q".equalsIgnoreCase(argv[i])) {
                    setQuietMode = true;
                } else if ("-DIAG".equalsIgnoreCase(argv[i])) {
                    doDiag = true;
                } else if ("-XML".equalsIgnoreCase(argv[i])) {
                    outputType = "xml";
                } else if ("-TEXT".equalsIgnoreCase(argv[i])) {
                    outputType = "text";
                } else if ("-HTML".equalsIgnoreCase(argv[i])) {
                    outputType = "html";
                } else if ("-EDUMP".equalsIgnoreCase(argv[i])) {
                    doStackDumpOnError = true;

                    if (((i + 1) < argv.length) && (argv[i + 1].charAt(0) != '-')) {
                        dumpFileName = argv[++i];
                    }
                } else if ("-URIRESOLVER".equalsIgnoreCase(argv[i])) {
                    if (i + 1 < argv.length) {
                        try {
                            Class<?> uriResolverClass = Class.forName(argv[++i]);
                            Constructor<?> ctor = uriResolverClass.getConstructor();
                            ctor.setAccessible(true);
                            uriResolver = (URIResolver) ctor.newInstance();

                            tfactory.setURIResolver(uriResolver);
                        } catch (Throwable cnfe) {
                            msg = "Class not found for option -URIResolver";
                            System.err.println(msg);
                            doExit(msg);
                        }
                    } else {
                        msg = "Missing argument for -URIResolver";
                        System.err.println(msg);  //"Missing argument for);
                        doExit(msg);
                    }
                } else if ("-ENTITYRESOLVER".equalsIgnoreCase(argv[i])) {
                    if (i + 1 < argv.length) {
                        try {
                            Class<?> entityResolverClass = Class.forName(argv[++i]);
                            Constructor<?> ctor = entityResolverClass.getConstructor();
                            ctor.setAccessible(true);
                            entityResolver = (EntityResolver) ctor.newInstance();
                        } catch (Throwable cnfe) {
                            msg = "Class not found for option -EntityResolver";
                            System.err.println(msg);
                            doExit(msg);
                        }
                    } else {
                        //            "Missing argument for);
                        msg = "Missing argument for -EntityResolver";
                        System.err.println(msg);
                        doExit(msg);
                    }
                } else if ("-CONTENTHANDLER".equalsIgnoreCase(argv[i])) {
                    if (i + 1 < argv.length) {
                        try {
                            Class<?> contentHandlerClass = Class.forName(argv[++i]);
                            Constructor<?> ctor = contentHandlerClass.getConstructor();
                            ctor.setAccessible(true);
                            contentHandler = (ContentHandler) ctor.newInstance();
                        } catch (Throwable cnfe) {
                            msg = "Class not found for option -ContentHandler";
                            System.err.println(msg);
                            doExit(msg);
                        }
                    } else {
                        //            "Missing argument for);
                        msg = "Missing argument for -ContentHandler";
                        System.err.println(msg);
                        doExit(msg);
                    }
                } else if ("-XO".equalsIgnoreCase(argv[i])) {
                    if (useXSLTC) {
                        if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                            tfactory.setAttribute("generate-translet", "true");
                            tfactory.setAttribute("translet-name", argv[++i]);
                        } else {
                            tfactory.setAttribute("generate-translet", "true");
                        }
                    } else {
                        if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                            i++;
                        }
                        printInvalidXalanOption("-XO");
                    }
                } // Specify the destination directory for the translet classes.
                else if ("-XD".equalsIgnoreCase(argv[i])) {
                    if (useXSLTC) {
                        if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                            tfactory.setAttribute("destination-directory", argv[++i]);
                        } else {
                            System.err.println("Missing argument for -XD");  //"Missing argument for);
                        }
                    } else {
                        if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                            i++;
                        }

                        printInvalidXalanOption("-XD");
                    }
                } // Specify the jar file name which the translet classes are packaged into.
                else if ("-XJ".equalsIgnoreCase(argv[i])) {
                    if (useXSLTC) {
                        if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                            tfactory.setAttribute("generate-translet", "true");
                            tfactory.setAttribute("jar-name", argv[++i]);
                        } else {
                            System.err.println("Missing argument for -XJ");  //"Missing argument for);
                        }
                    } else {
                        if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                            i++;
                        }

                        printInvalidXalanOption("-XJ");
                    }

                } // Specify the package name prefix for the generated translet classes.
                else if ("-XP".equalsIgnoreCase(argv[i])) {
                    if (useXSLTC) {
                        if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                            tfactory.setAttribute("package-name", argv[++i]);
                        } else {
                            System.err.println("Missing argument for -XP");  //"Missing argument for);
                        }
                    } else {
                        if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-') {
                            i++;
                        }

                        printInvalidXalanOption("-XP");
                    }

                } // Enable template inlining.
                else if ("-XN".equalsIgnoreCase(argv[i])) {
                    if (useXSLTC) {
                        tfactory.setAttribute("enable-inlining", "true");
                    } else {
                        printInvalidXalanOption("-XN");
                    }
                } // Turns on additional debugging message output
                else if ("-XX".equalsIgnoreCase(argv[i])) {
                    if (useXSLTC) {
                        tfactory.setAttribute("debug", "true");
                    } else {
                        printInvalidXalanOption("-XX");
                    }
                } // Create the Transformer from the translet if the translet class is newer
                // than the stylesheet.
                else if ("-XT".equalsIgnoreCase(argv[i])) {
                    if (useXSLTC) {
                        tfactory.setAttribute("auto-translet", "true");
                    } else {
                        printInvalidXalanOption("-XT");
                    }
                } else if ("-SECURE".equalsIgnoreCase(argv[i])) {
                    isSecureProcessing = true;
                    try {
                        tfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    } catch (TransformerConfigurationException e) {
                    }
                } else {
                    System.err.println("Invalid argument: " + argv[i]);  //"Invalid argument:);
                }
            }

            // Print usage instructions if no xml and xsl file is specified in the command line
            if (inFileName == null && xslFileName == null) {
                msg = "Error: No stylesheet or input xml is specified. Run this command without any option for usage instructions.";
                System.err.println(msg);
                doExit(msg);
            }

      // Note that there are usage cases for calling us without a -IN arg
            // The main XSL transformation occurs here!
            try {
                long start = System.currentTimeMillis();

                if (null != dumpFileName) {
                    dumpWriter = new PrintWriter(new FileWriter(dumpFileName));
                }

                Templates stylesheet = null;

                if (null != xslFileName) {
                    if (flavor.equals("d2d")) {

                        // Parse in the xml data into a DOM
                        DocumentBuilderFactory dfactory
                                = DocumentBuilderFactory.newInstance();

                        dfactory.setNamespaceAware(true);

                        if (isSecureProcessing) {
                            try {
                                dfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                            } catch (ParserConfigurationException pce) {
                            }
                        }

                        DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
                        Node xslDOM = docBuilder.parse(new InputSource(xslFileName));

                        stylesheet = tfactory.newTemplates(new DOMSource(xslDOM,
                                xslFileName));
                    } else {
                        // System.out.println("Calling newTemplates: "+xslFileName);
                        stylesheet = tfactory.newTemplates(new StreamSource(xslFileName));
                        // System.out.println("Done calling newTemplates: "+xslFileName);
                    }
                }

                PrintWriter resultWriter;
                StreamResult strResult;

                if (null != outFileName) {
                    strResult = new StreamResult(new FileOutputStream(outFileName));
                    // One possible improvement might be to ensure this is
                    //  a valid URI before setting the systemId, but that
                    //  might have subtle changes that pre-existing users
                    //  might notice; we can think about that later -sc r1.46
                    strResult.setSystemId(outFileName);
                } else {
                    strResult = new StreamResult(System.out);
                    // We used to default to incremental mode in this case.
                    // We've since decided that since the -INCREMENTAL switch is
                    // available, that default is probably not necessary nor
                    // necessarily a good idea.
                }

                SAXTransformerFactory stf = (SAXTransformerFactory) tfactory;

                // Did they pass in a stylesheet, or should we get it from the
                // document?
                if (null == stylesheet) {
                    Source source
                            = stf.getAssociatedStylesheet(new StreamSource(inFileName), media,
                                    null, null);

                    if (null != source) {
                        stylesheet = tfactory.newTemplates(source);
                    } else {
                        if (null != media) {
                            throw new TransformerException("No stylesheet found in:  "
                                    + inFileName + ", media=" + media); //"No stylesheet found in: "
                        } // + inFileName + ", media="
                        // + media);
                        else {
                            throw new TransformerException("No xml-stylesheet PI found in: " + inFileName); //"No xml-stylesheet PI found in: "
                        }                                             //+ inFileName);
                    }
                }

                if (null != stylesheet) {
                    Transformer transformer = flavor.equals("th") ? null : stylesheet.newTransformer();
                    transformer.setErrorListener(createDefaultErrorListener());

                    // Override the output format?
                    if (null != outputType) {
                        transformer.setOutputProperty(OutputKeys.METHOD, outputType);
                    }

                    int nParams = params.size();

                    for (int i = 0; i < nParams; i += 2) {
                        transformer.setParameter((String) params.get(i),
                                (String) params.get(i + 1));
                    }

                    if (uriResolver != null) {
                        transformer.setURIResolver(uriResolver);
                    }

                    if (null != inFileName) {
                        if (flavor.equals("d2d")) {

                            // Parse in the xml data into a DOM
                            DocumentBuilderFactory dfactory
                                    = DocumentBuilderFactory.newInstance();

                            dfactory.setCoalescing(true);
                            dfactory.setNamespaceAware(true);

                            if (isSecureProcessing) {
                                try {
                                    dfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                                } catch (ParserConfigurationException pce) {
                                }
                            }

                            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

                            if (entityResolver != null) {
                                docBuilder.setEntityResolver(entityResolver);
                            }

                            Node xmlDoc = docBuilder.parse(new InputSource(inFileName));
                            Document doc = docBuilder.newDocument();
                            org.w3c.dom.DocumentFragment outNode
                                    = doc.createDocumentFragment();

                            transformer.transform(new DOMSource(xmlDoc, inFileName),
                                    new DOMResult(outNode));

                            // Now serialize output to disk with identity transformer
                            Transformer serializer = stf.newTransformer();
                            serializer.setErrorListener(createDefaultErrorListener());

                            Properties serializationProps
                                    = stylesheet.getOutputProperties();

                            serializer.setOutputProperties(serializationProps);

                            if (contentHandler != null) {
                                SAXResult result = new SAXResult(contentHandler);

                                serializer.transform(new DOMSource(outNode), result);
                            } else {
                                serializer.transform(new DOMSource(outNode), strResult);
                            }
                        } else if (flavor.equals("th")) {
                            for (int i = 0; i < 1; i++) // Loop for diagnosing bugs with inconsistent behavior
                            {
                                // System.out.println("Testing the TransformerHandler...");

                                XMLReader reader = null;

                                // Use JAXP1.1 ( if possible )
                                try {
                                    javax.xml.parsers.SAXParserFactory factory
                                            = javax.xml.parsers.SAXParserFactory.newInstance();

                                    factory.setNamespaceAware(true);

                                    if (isSecureProcessing) {
                                        try {
                                            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                                        } catch (org.xml.sax.SAXException se) {
                                        }
                                    }

                                    javax.xml.parsers.SAXParser jaxpParser
                                            = factory.newSAXParser();

                                    reader = jaxpParser.getXMLReader();
                                } catch (javax.xml.parsers.ParserConfigurationException ex) {
                                    throw new org.xml.sax.SAXException(ex);
                                } catch (javax.xml.parsers.FactoryConfigurationError ex1) {
                                    throw new org.xml.sax.SAXException(ex1.toString());
                                } catch (NoSuchMethodError ex2) {
                                } catch (AbstractMethodError ame) {
                                }

                                if (null == reader) {
                                    reader = XMLReaderFactory.createXMLReader();
                                }

                                TransformerHandler th = stf.newTransformerHandler(stylesheet);

                                reader.setContentHandler(th);
                                reader.setDTDHandler(th);

                                if (th instanceof org.xml.sax.ErrorHandler) {
                                    reader.setErrorHandler((org.xml.sax.ErrorHandler) th);
                                }

                                try {
                                    reader.setProperty(
                                            "http://xml.org/sax/properties/lexical-handler", th);
                                } catch (org.xml.sax.SAXNotRecognizedException e) {
                                } catch (org.xml.sax.SAXNotSupportedException e) {
                                }
                                try {
                                    reader.setFeature("http://xml.org/sax/features/namespace-prefixes",
                                            true);
                                } catch (org.xml.sax.SAXException se) {
                                }

                                th.setResult(strResult);

                                reader.parse(new InputSource(inFileName));
                            }
                        } else {
                            if (entityResolver != null) {
                                XMLReader reader = null;

                                // Use JAXP1.1 ( if possible )
                                try {
                                    javax.xml.parsers.SAXParserFactory factory
                                            = javax.xml.parsers.SAXParserFactory.newInstance();

                                    factory.setNamespaceAware(true);

                                    if (isSecureProcessing) {
                                        try {
                                            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                                        } catch (org.xml.sax.SAXException se) {
                                        }
                                    }

                                    javax.xml.parsers.SAXParser jaxpParser
                                            = factory.newSAXParser();

                                    reader = jaxpParser.getXMLReader();
                                } catch (javax.xml.parsers.ParserConfigurationException ex) {
                                    throw new org.xml.sax.SAXException(ex);
                                } catch (javax.xml.parsers.FactoryConfigurationError ex1) {
                                    throw new org.xml.sax.SAXException(ex1.toString());
                                } catch (NoSuchMethodError ex2) {
                                } catch (AbstractMethodError ame) {
                                }

                                if (null == reader) {
                                    reader = XMLReaderFactory.createXMLReader();
                                }

                                reader.setEntityResolver(entityResolver);

                                if (contentHandler != null) {
                                    SAXResult result = new SAXResult(contentHandler);

                                    transformer.transform(
                                            new SAXSource(reader, new InputSource(inFileName)),
                                            result);
                                } else {
                                    transformer.transform(
                                            new SAXSource(reader, new InputSource(inFileName)),
                                            strResult);
                                }
                            } else if (contentHandler != null) {
                                SAXResult result = new SAXResult(contentHandler);

                                transformer.transform(new StreamSource(inFileName), result);
                            } else {
                                // System.out.println("Starting transform");
                                transformer.transform(new StreamSource(inFileName),
                                        strResult);
                                // System.out.println("Done with transform");
                            }
                        }
                    } else {
                        StringReader reader
                                = new StringReader("<?xml version=\"1.0\"?> <doc/>");

                        transformer.transform(new StreamSource(reader), strResult);
                    }
                } else {
                    //          "XSL Process was not successful.");
                    msg = "XSL Process was not successful.";
                    diagnosticsWriter.println(msg);
                    doExit(msg);
                }

                // close output streams
                if (null != outFileName && strResult != null) {
                    java.io.OutputStream out = strResult.getOutputStream();
                    java.io.Writer writer = strResult.getWriter();
                    try {
                        if (out != null) {
                            out.close();
                        }
                        if (writer != null) {
                            writer.close();
                        }
                    } catch (java.io.IOException ie) {
                    }
                }

                long stop = System.currentTimeMillis();
                long millisecondsDuration = stop - start;

                if (doDiag) {
                    msg = " --------- Transform of " + inFileName + " via "
                            + xslFileName + " took " + millisecondsDuration + " ms";
                    diagnosticsWriter.println('\n');
                    diagnosticsWriter.println(msg);
                }

            } catch (Throwable throwable) {
                doStackDumpOnError = true;

                diagnosticsWriter.println();

                if (doStackDumpOnError) {
                    throwable.printStackTrace(dumpWriter);
                } else {
                    printLocation(diagnosticsWriter, throwable);
                    diagnosticsWriter.println("Unexpected exception: " + throwable);
                }

                // diagnosticsWriter.println(XSLMessages.createMessage(XSLTErrorResources.ER_NOT_SUCCESSFUL, null)); //"XSL Process was not successful.");
                if (null != dumpFileName) {
                    dumpWriter.close();
                }

                doExit(throwable.getMessage());
            }

            if (null != dumpFileName) {
                dumpWriter.close();
            }

            if (null != diagnosticsWriter) {

                // diagnosticsWriter.close();
            }

            // if(!setQuietMode)
            //  diagnosticsWriter.println(resbundle.getString("xsldone")); //"Xalan: done");
            // else
            // diagnosticsWriter.println("");  //"Xalan: done");
        }
    }

    /**
     * It is _much_ easier to debug under VJ++ if I can set a single breakpoint
     * before this blows itself out of the water... (I keep checking this in, it
     * keeps vanishing. Grr!)
     *
     */
    static void doExit(String msg) {
        throw new RuntimeException(msg);
    }

    /**
     * Wait for a return key to continue
     *
     * @param resbundle The resource bundle
     */
    private static void waitForReturnKey() {
        System.out.println("(press <return> to continue)");
        try {
            while (System.in.read() != '\n');
        } catch (java.io.IOException e) {
        }
    }

    /**
     * Print a message if an option cannot be used with -XSLTC.
     *
     * @param option The option String
     */
    private static void printInvalidXSLTCOption(String option) {
        System.err.println("The option " + option + " is not supported in XSLTC mode.");
    }

    /**
     * Print a message if an option can only be used with -XSLTC.
     *
     * @param option The option String
     */
    private static void printInvalidXalanOption(String option) {
        System.err.println("The option " + option + " can only be used with -XSLTC.");
    }

    static class DummyErrorListenerHandler implements ErrorHandler, ErrorListener {
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            System.err.println("WARNING: " + exception);
        }
        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
        @Override
        public void warning(TransformerException exception) throws TransformerException {
            System.err.println("WARNING: " + exception);
        }
        @Override
        public void error(TransformerException exception) throws TransformerException {
            throw exception;
        }
        @Override
        public void fatalError(TransformerException exception) throws TransformerException {
            throw exception;
        }
    }

    static ErrorListener createDefaultErrorListener() {
        try {
            Class<?> errorHandler =
                    Class.forName("com.sun.org.apache.xml.internal.utils.DefaultErrorHandler");
            Constructor<?> ctor = errorHandler.getConstructor();
            return (ErrorListener) ctor.newInstance();
        } catch (Throwable r) {
            return new DummyErrorListenerHandler();
        }
    }

    private static void printLocation(PrintWriter diagnosticsWriter, Throwable throwable) {
        try {
            Class<?> errorHandler =
                    Class.forName("com.sun.org.apache.xml.internal.utils.DefaultErrorHandler");
            Method m = errorHandler.getMethod("printLocation", PrintWriter.class, Throwable.class);
            m.invoke(null, diagnosticsWriter, throwable);
        } catch (Throwable t) {
            SourceLocator locator = null;
            Throwable cause = throwable;

            // Try to find the locator closest to the cause.
            do {
                if (cause instanceof TransformerException) {
                    SourceLocator causeLocator = ((TransformerException) cause).getLocator();
                    if (null != causeLocator) {
                        locator = causeLocator;
                    }
                    cause = ((TransformerException) cause).getCause();
                } else if (cause instanceof SAXException) {
                    cause = ((SAXException) cause).getException();
                } else {
                    cause = cause.getCause();
                }
            } while (null != cause);

            if (null != locator) {
                // m_pw.println("Parser fatal error: "+exception.getMessage());
                String id = (null != locator.getPublicId())
                        ? locator.getPublicId()
                        : (null != locator.getSystemId())
                                ? locator.getSystemId() : "SystemId Unknown"; //"SystemId Unknown";

                diagnosticsWriter.print(id + "; " + "line: " + locator.getLineNumber()
                        + "; column: " + locator.getColumnNumber() + "; ");
            }
            diagnosticsWriter.print("(" + throwable + ": unknown location)");
        }
    }

}
