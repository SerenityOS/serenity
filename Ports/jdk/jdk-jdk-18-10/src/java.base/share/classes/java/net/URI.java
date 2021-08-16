/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Path;
import java.text.Normalizer;
import jdk.internal.access.JavaNetUriAccess;
import jdk.internal.access.SharedSecrets;
import sun.nio.cs.UTF_8;

import java.lang.Character;             // for javadoc
import java.lang.NullPointerException;  // for javadoc

/**
 * Represents a Uniform Resource Identifier (URI) reference.
 *
 * <p> Aside from some minor deviations noted below, an instance of this
 * class represents a URI reference as defined by
 * <a href="http://www.ietf.org/rfc/rfc2396.txt"><i>RFC&nbsp;2396: Uniform
 * Resource Identifiers (URI): Generic Syntax</i></a>, amended by <a
 * href="http://www.ietf.org/rfc/rfc2732.txt"><i>RFC&nbsp;2732: Format for
 * Literal IPv6 Addresses in URLs</i></a>. The Literal IPv6 address format
 * also supports scope_ids. The syntax and usage of scope_ids is described
 * <a href="Inet6Address.html#scoped">here</a>.
 * This class provides constructors for creating URI instances from
 * their components or by parsing their string forms, methods for accessing the
 * various components of an instance, and methods for normalizing, resolving,
 * and relativizing URI instances.  Instances of this class are immutable.
 *
 *
 * <h2> URI syntax and components </h2>
 *
 * At the highest level a URI reference (hereinafter simply "URI") in string
 * form has the syntax
 *
 * <blockquote>
 * [<i>scheme</i><b>{@code :}</b>]<i>scheme-specific-part</i>[<b>{@code #}</b><i>fragment</i>]
 * </blockquote>
 *
 * where square brackets [...] delineate optional components and the characters
 * <b>{@code :}</b> and <b>{@code #}</b> stand for themselves.
 *
 * <p> An <i>absolute</i> URI specifies a scheme; a URI that is not absolute is
 * said to be <i>relative</i>.  URIs are also classified according to whether
 * they are <i>opaque</i> or <i>hierarchical</i>.
 *
 * <p> An <i>opaque</i> URI is an absolute URI whose scheme-specific part does
 * not begin with a slash character ({@code '/'}).  Opaque URIs are not
 * subject to further parsing.  Some examples of opaque URIs are:
 *
 * <blockquote><ul style="list-style-type:none">
 * <li>{@code mailto:java-net@www.example.com}</li>
 * <li>{@code news:comp.lang.java}</li>
 * <li>{@code urn:isbn:096139210x}</li>
 * </ul></blockquote>
 *
 * <p> A <i>hierarchical</i> URI is either an absolute URI whose
 * scheme-specific part begins with a slash character, or a relative URI, that
 * is, a URI that does not specify a scheme.  Some examples of hierarchical
 * URIs are:
 *
 * <blockquote>
 * {@code http://example.com/languages/java/}<br>
 * {@code sample/a/index.html#28}<br>
 * {@code ../../demo/b/index.html}<br>
 * {@code file:///~/calendar}
 * </blockquote>
 *
 * <p> A hierarchical URI is subject to further parsing according to the syntax
 *
 * <blockquote>
 * [<i>scheme</i><b>{@code :}</b>][<b>{@code //}</b><i>authority</i>][<i>path</i>][<b>{@code ?}</b><i>query</i>][<b>{@code #}</b><i>fragment</i>]
 * </blockquote>
 *
 * where the characters <b>{@code :}</b>, <b>{@code /}</b>,
 * <b>{@code ?}</b>, and <b>{@code #}</b> stand for themselves.  The
 * scheme-specific part of a hierarchical URI consists of the characters
 * between the scheme and fragment components.
 *
 * <p> The authority component of a hierarchical URI is, if specified, either
 * <i>server-based</i> or <i>registry-based</i>.  A server-based authority
 * parses according to the familiar syntax
 *
 * <blockquote>
 * [<i>user-info</i><b>{@code @}</b>]<i>host</i>[<b>{@code :}</b><i>port</i>]
 * </blockquote>
 *
 * where the characters <b>{@code @}</b> and <b>{@code :}</b> stand for
 * themselves.  Nearly all URI schemes currently in use are server-based.  An
 * authority component that does not parse in this way is considered to be
 * registry-based.
 *
 * <p> The path component of a hierarchical URI is itself said to be absolute
 * if it begins with a slash character ({@code '/'}); otherwise it is
 * relative.  The path of a hierarchical URI that is either absolute or
 * specifies an authority is always absolute.
 *
 * <p> All told, then, a URI instance has the following nine components:
 *
 * <table class="striped" style="margin-left:2em">
 * <caption style="display:none">Describes the components of a URI:scheme,scheme-specific-part,authority,user-info,host,port,path,query,fragment</caption>
 * <thead>
 * <tr><th scope="col">Component</th><th scope="col">Type</th></tr>
 * </thead>
 * <tbody style="text-align:left">
 * <tr><th scope="row">scheme</th><td>{@code String}</td></tr>
 * <tr><th scope="row">scheme-specific-part</th><td>{@code String}</td></tr>
 * <tr><th scope="row">authority</th><td>{@code String}</td></tr>
 * <tr><th scope="row">user-info</th><td>{@code String}</td></tr>
 * <tr><th scope="row">host</th><td>{@code String}</td></tr>
 * <tr><th scope="row">port</th><td>{@code int}</td></tr>
 * <tr><th scope="row">path</th><td>{@code String}</td></tr>
 * <tr><th scope="row">query</th><td>{@code String}</td></tr>
 * <tr><th scope="row">fragment</th><td>{@code String}</td></tr>
 * </tbody>
 * </table>
 *
 * In a given instance any particular component is either <i>undefined</i> or
 * <i>defined</i> with a distinct value.  Undefined string components are
 * represented by {@code null}, while undefined integer components are
 * represented by {@code -1}.  A string component may be defined to have the
 * empty string as its value; this is not equivalent to that component being
 * undefined.
 *
 * <p> Whether a particular component is or is not defined in an instance
 * depends upon the type of the URI being represented.  An absolute URI has a
 * scheme component.  An opaque URI has a scheme, a scheme-specific part, and
 * possibly a fragment, but has no other components.  A hierarchical URI always
 * has a path (though it may be empty) and a scheme-specific-part (which at
 * least contains the path), and may have any of the other components.  If the
 * authority component is present and is server-based then the host component
 * will be defined and the user-information and port components may be defined.
 *
 *
 * <h3> Operations on URI instances </h3>
 *
 * The key operations supported by this class are those of
 * <i>normalization</i>, <i>resolution</i>, and <i>relativization</i>.
 *
 * <p> <i>Normalization</i> is the process of removing unnecessary {@code "."}
 * and {@code ".."} segments from the path component of a hierarchical URI.
 * Each {@code "."} segment is simply removed.  A {@code ".."} segment is
 * removed only if it is preceded by a non-{@code ".."} segment.
 * Normalization has no effect upon opaque URIs.
 *
 * <p> <i>Resolution</i> is the process of resolving one URI against another,
 * <i>base</i> URI.  The resulting URI is constructed from components of both
 * URIs in the manner specified by RFC&nbsp;2396, taking components from the
 * base URI for those not specified in the original.  For hierarchical URIs,
 * the path of the original is resolved against the path of the base and then
 * normalized.  The result, for example, of resolving
 *
 * <blockquote>
 * {@code sample/a/index.html#28}
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 * &nbsp;&nbsp;&nbsp;&nbsp;(1)
 * </blockquote>
 *
 * against the base URI {@code http://example.com/languages/java/} is the result
 * URI
 *
 * <blockquote>
 * {@code http://example.com/languages/java/sample/a/index.html#28}
 * </blockquote>
 *
 * Resolving the relative URI
 *
 * <blockquote>
 * {@code ../../demo/b/index.html}&nbsp;&nbsp;&nbsp;&nbsp;(2)
 * </blockquote>
 *
 * against this result yields, in turn,
 *
 * <blockquote>
 * {@code http://example.com/languages/java/demo/b/index.html}
 * </blockquote>
 *
 * Resolution of both absolute and relative URIs, and of both absolute and
 * relative paths in the case of hierarchical URIs, is supported.  Resolving
 * the URI {@code file:///~calendar} against any other URI simply yields the
 * original URI, since it is absolute.  Resolving the relative URI (2) above
 * against the relative base URI (1) yields the normalized, but still relative,
 * URI
 *
 * <blockquote>
 * {@code demo/b/index.html}
 * </blockquote>
 *
 * <p> <i>Relativization</i>, finally, is the inverse of resolution: For any
 * two normalized URIs <i>u</i> and&nbsp;<i>v</i>,
 *
 * <blockquote>
 *   <i>u</i>{@code .relativize(}<i>u</i>{@code .resolve(}<i>v</i>{@code )).equals(}<i>v</i>{@code )}&nbsp;&nbsp;and<br>
 *   <i>u</i>{@code .resolve(}<i>u</i>{@code .relativize(}<i>v</i>{@code )).equals(}<i>v</i>{@code )}&nbsp;&nbsp;.<br>
 * </blockquote>
 *
 * This operation is often useful when constructing a document containing URIs
 * that must be made relative to the base URI of the document wherever
 * possible.  For example, relativizing the URI
 *
 * <blockquote>
 * {@code http://example.com/languages/java/sample/a/index.html#28}
 * </blockquote>
 *
 * against the base URI
 *
 * <blockquote>
 * {@code http://example.com/languages/java/}
 * </blockquote>
 *
 * yields the relative URI {@code sample/a/index.html#28}.
 *
 *
 * <h3> Character categories </h3>
 *
 * RFC&nbsp;2396 specifies precisely which characters are permitted in the
 * various components of a URI reference.  The following categories, most of
 * which are taken from that specification, are used below to describe these
 * constraints:
 *
 * <table class="striped" style="margin-left:2em">
 * <caption style="display:none">Describes categories alpha,digit,alphanum,unreserved,punct,reserved,escaped,and other</caption>
 *   <thead>
 *   <tr><th scope="col">Category</th><th scope="col">Description</th></tr>
 *   </thead>
 *   <tbody style="text-align:left">
 *   <tr><th scope="row" style="vertical-align:top">alpha</th>
 *       <td>The US-ASCII alphabetic characters,
 *        {@code 'A'}&nbsp;through&nbsp;{@code 'Z'}
 *        and {@code 'a'}&nbsp;through&nbsp;{@code 'z'}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">digit</th>
 *       <td>The US-ASCII decimal digit characters,
 *       {@code '0'}&nbsp;through&nbsp;{@code '9'}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">alphanum</th>
 *       <td>All <i>alpha</i> and <i>digit</i> characters</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">unreserved</th>
 *       <td>All <i>alphanum</i> characters together with those in the string
 *        {@code "_-!.~'()*"}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">punct</th>
 *       <td>The characters in the string {@code ",;:$&+="}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">reserved</th>
 *       <td>All <i>punct</i> characters together with those in the string
 *        {@code "?/[]@"}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">escaped</th>
 *       <td>Escaped octets, that is, triplets consisting of the percent
 *           character ({@code '%'}) followed by two hexadecimal digits
 *           ({@code '0'}-{@code '9'}, {@code 'A'}-{@code 'F'}, and
 *           {@code 'a'}-{@code 'f'})</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">other</th>
 *       <td>The Unicode characters that are not in the US-ASCII character set,
 *           are not control characters (according to the {@link
 *           java.lang.Character#isISOControl(char) Character.isISOControl}
 *           method), and are not space characters (according to the {@link
 *           java.lang.Character#isSpaceChar(char) Character.isSpaceChar}
 *           method)&nbsp;&nbsp;<i>(<b>Deviation from RFC 2396</b>, which is
 *           limited to US-ASCII)</i></td></tr>
 * </tbody>
 * </table>
 *
 * <p><a id="legal-chars"></a> The set of all legal URI characters consists of
 * the <i>unreserved</i>, <i>reserved</i>, <i>escaped</i>, and <i>other</i>
 * characters.
 *
 *
 * <h3> Escaped octets, quotation, encoding, and decoding </h3>
 *
 * RFC 2396 allows escaped octets to appear in the user-info, path, query, and
 * fragment components.  Escaping serves two purposes in URIs:
 *
 * <ul>
 *
 *   <li><p> To <i>encode</i> non-US-ASCII characters when a URI is required to
 *   conform strictly to RFC&nbsp;2396 by not containing any <i>other</i>
 *   characters.  </p></li>
 *
 *   <li><p> To <i>quote</i> characters that are otherwise illegal in a
 *   component.  The user-info, path, query, and fragment components differ
 *   slightly in terms of which characters are considered legal and illegal.
 *   </p></li>
 *
 * </ul>
 *
 * These purposes are served in this class by three related operations:
 *
 * <ul>
 *
 *   <li><p><a id="encode"></a> A character is <i>encoded</i> by replacing it
 *   with the sequence of escaped octets that represent that character in the
 *   UTF-8 character set.  The Euro currency symbol ({@code '\u005Cu20AC'}),
 *   for example, is encoded as {@code "%E2%82%AC"}.  <i>(<b>Deviation from
 *   RFC&nbsp;2396</b>, which does not specify any particular character
 *   set.)</i> </p></li>
 *
 *   <li><p><a id="quote"></a> An illegal character is <i>quoted</i> simply by
 *   encoding it.  The space character, for example, is quoted by replacing it
 *   with {@code "%20"}.  UTF-8 contains US-ASCII, hence for US-ASCII
 *   characters this transformation has exactly the effect required by
 *   RFC&nbsp;2396. </p></li>
 *
 *   <li><p><a id="decode"></a>
 *   A sequence of escaped octets is <i>decoded</i> by
 *   replacing it with the sequence of characters that it represents in the
 *   UTF-8 character set.  UTF-8 contains US-ASCII, hence decoding has the
 *   effect of de-quoting any quoted US-ASCII characters as well as that of
 *   decoding any encoded non-US-ASCII characters.  If a <a
 *   href="../nio/charset/CharsetDecoder.html#ce">decoding error</a> occurs
 *   when decoding the escaped octets then the erroneous octets are replaced by
 *   {@code '\u005CuFFFD'}, the Unicode replacement character.  </p></li>
 *
 * </ul>
 *
 * These operations are exposed in the constructors and methods of this class
 * as follows:
 *
 * <ul>
 *
 *   <li><p> The {@linkplain #URI(java.lang.String) single-argument
 *   constructor} requires any illegal characters in its argument to be
 *   quoted and preserves any escaped octets and <i>other</i> characters that
 *   are present.  </p></li>
 *
 *   <li><p> The {@linkplain
 *   #URI(java.lang.String,java.lang.String,java.lang.String,int,java.lang.String,java.lang.String,java.lang.String)
 *   multi-argument constructors} quote illegal characters as
 *   required by the components in which they appear.  The percent character
 *   ({@code '%'}) is always quoted by these constructors.  Any <i>other</i>
 *   characters are preserved.  </p></li>
 *
 *   <li><p> The {@link #getRawUserInfo() getRawUserInfo}, {@link #getRawPath()
 *   getRawPath}, {@link #getRawQuery() getRawQuery}, {@link #getRawFragment()
 *   getRawFragment}, {@link #getRawAuthority() getRawAuthority}, and {@link
 *   #getRawSchemeSpecificPart() getRawSchemeSpecificPart} methods return the
 *   values of their corresponding components in raw form, without interpreting
 *   any escaped octets.  The strings returned by these methods may contain
 *   both escaped octets and <i>other</i> characters, and will not contain any
 *   illegal characters.  </p></li>
 *
 *   <li><p> The {@link #getUserInfo() getUserInfo}, {@link #getPath()
 *   getPath}, {@link #getQuery() getQuery}, {@link #getFragment()
 *   getFragment}, {@link #getAuthority() getAuthority}, and {@link
 *   #getSchemeSpecificPart() getSchemeSpecificPart} methods decode any escaped
 *   octets in their corresponding components.  The strings returned by these
 *   methods may contain both <i>other</i> characters and illegal characters,
 *   and will not contain any escaped octets.  </p></li>
 *
 *   <li><p> The {@link #toString() toString} method returns a URI string with
 *   all necessary quotation but which may contain <i>other</i> characters.
 *   </p></li>
 *
 *   <li><p> The {@link #toASCIIString() toASCIIString} method returns a fully
 *   quoted and encoded URI string that does not contain any <i>other</i>
 *   characters.  </p></li>
 *
 * </ul>
 *
 *
 * <h3> Identities </h3>
 *
 * For any URI <i>u</i>, it is always the case that
 *
 * <blockquote>
 * {@code new URI(}<i>u</i>{@code .toString()).equals(}<i>u</i>{@code )}&nbsp;.
 * </blockquote>
 *
 * For any URI <i>u</i> that does not contain redundant syntax such as two
 * slashes before an empty authority (as in {@code file:///tmp/}&nbsp;) or a
 * colon following a host name but no port (as in
 * {@code http://www.example.com:}&nbsp;), and that does not encode characters
 * except those that must be quoted, the following identities also hold:
 * <pre>
 *     new URI(<i>u</i>.getScheme(),
 *             <i>u</i>.getSchemeSpecificPart(),
 *             <i>u</i>.getFragment())
 *     .equals(<i>u</i>)</pre>
 * in all cases,
 * <pre>
 *     new URI(<i>u</i>.getScheme(),
 *             <i>u</i>.getAuthority(),
 *             <i>u</i>.getPath(), <i>u</i>.getQuery(),
 *             <i>u</i>.getFragment())
 *     .equals(<i>u</i>)</pre>
 * if <i>u</i> is hierarchical, and
 * <pre>
 *     new URI(<i>u</i>.getScheme(),
 *             <i>u</i>.getUserInfo(), <i>u</i>.getHost(), <i>u</i>.getPort(),
 *             <i>u</i>.getPath(), <i>u</i>.getQuery(),
 *             <i>u</i>.getFragment())
 *     .equals(<i>u</i>)</pre>
 * if <i>u</i> is hierarchical and has either no authority or a server-based
 * authority.
 *
 *
 * <h3> URIs, URLs, and URNs </h3>
 *
 * A URI is a uniform resource <i>identifier</i> while a URL is a uniform
 * resource <i>locator</i>.  Hence every URL is a URI, abstractly speaking, but
 * not every URI is a URL.  This is because there is another subcategory of
 * URIs, uniform resource <i>names</i> (URNs), which name resources but do not
 * specify how to locate them.  The {@code mailto}, {@code news}, and
 * {@code isbn} URIs shown above are examples of URNs.
 *
 * <p> The conceptual distinction between URIs and URLs is reflected in the
 * differences between this class and the {@link URL} class.
 *
 * <p> An instance of this class represents a URI reference in the syntactic
 * sense defined by RFC&nbsp;2396.  A URI may be either absolute or relative.
 * A URI string is parsed according to the generic syntax without regard to the
 * scheme, if any, that it specifies.  No lookup of the host, if any, is
 * performed, and no scheme-dependent stream handler is constructed.  Equality,
 * hashing, and comparison are defined strictly in terms of the character
 * content of the instance.  In other words, a URI instance is little more than
 * a structured string that supports the syntactic, scheme-independent
 * operations of comparison, normalization, resolution, and relativization.
 *
 * <p> An instance of the {@link URL} class, by contrast, represents the
 * syntactic components of a URL together with some of the information required
 * to access the resource that it describes.  A URL must be absolute, that is,
 * it must always specify a scheme.  A URL string is parsed according to its
 * scheme.  A stream handler is always established for a URL, and in fact it is
 * impossible to create a URL instance for a scheme for which no handler is
 * available.  Equality and hashing depend upon both the scheme and the
 * Internet address of the host, if any; comparison is not defined.  In other
 * words, a URL is a structured string that supports the syntactic operation of
 * resolution as well as the network I/O operations of looking up the host and
 * opening a connection to the specified resource.
 *
 * @apiNote
 *
 * Applications working with file paths and file URIs should take great
 * care to use the appropriate methods to convert between the two.
 * The {@link Path#of(URI)} factory method and the {@link File#File(URI)}
 * constructor can be used to create {@link Path} or {@link File}
 * objects from a file URI. {@link Path#toUri()} and {@link File#toURI()}
 * can be used to create a {@link URI} from a file path.
 * Applications should never try to {@linkplain
 * #URI(String, String, String, int, String, String, String)
 * construct}, {@linkplain #URI(String) parse}, or
 * {@linkplain #resolve(String) resolve} a {@code URI}
 * from the direct string representation of a {@code File} or {@code Path}
 * instance.
 * <p>
 * Some components of a URL or URI, such as <i>userinfo</i>, may
 * be abused to construct misleading URLs or URIs. Applications
 * that deal with URLs or URIs should take into account
 * the recommendations advised in <a
 * href="https://tools.ietf.org/html/rfc3986#section-7">RFC3986,
 * Section 7, Security Considerations</a>.
 *
 * @author Mark Reinhold
 * @since 1.4
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2279.txt"><i>RFC&nbsp;2279: UTF-8, a
 * transformation format of ISO 10646</i></a>
 * @see <a href="http://www.ietf.org/rfc/rfc2373.txt"><i>RFC&nbsp;2373: IPv6 Addressing
 * Architecture</i></a>
 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt"><i>RFC&nbsp;2396: Uniform
 * Resource Identifiers (URI): Generic Syntax</i></a>
 * @see <a href="http://www.ietf.org/rfc/rfc2732.txt"><i>RFC&nbsp;2732: Format for
 * Literal IPv6 Addresses in URLs</i></a>
 * @see <a href="URISyntaxException.html">URISyntaxException</a>
 */

public final class URI
    implements Comparable<URI>, Serializable
{

    // Note: Comments containing the word "ASSERT" indicate places where a
    // throw of an InternalError should be replaced by an appropriate assertion
    // statement once asserts are enabled in the build.
    @java.io.Serial
    static final long serialVersionUID = -6052424284110960213L;


    // -- Properties and components of this instance --

    // Components of all URIs: [<scheme>:]<scheme-specific-part>[#<fragment>]
    private transient String scheme;            // null ==> relative URI
    private transient String fragment;

    // Hierarchical URI components: [//<authority>]<path>[?<query>]
    private transient String authority;         // Registry or server

    // Server-based authority: [<userInfo>@]<host>[:<port>]
    private transient String userInfo;
    private transient String host;              // null ==> registry-based
    private transient int port = -1;            // -1 ==> undefined

    // Remaining components of hierarchical URIs
    private transient String path;              // null ==> opaque
    private transient String query;

    // The remaining fields may be computed on demand, which is safe even in
    // the face of multiple threads racing to initialize them
    private transient String schemeSpecificPart;
    private transient int hash;        // Zero ==> undefined

    private transient String decodedUserInfo;
    private transient String decodedAuthority;
    private transient String decodedPath;
    private transient String decodedQuery;
    private transient String decodedFragment;
    private transient String decodedSchemeSpecificPart;

    /**
     * The string form of this URI.
     *
     * @serial
     */
    private volatile String string;             // The only serializable field



    // -- Constructors and factories --

    private URI() { }                           // Used internally

    /**
     * Constructs a URI by parsing the given string.
     *
     * <p> This constructor parses the given string exactly as specified by the
     * grammar in <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>,
     * Appendix&nbsp;A, <b><i>except for the following deviations:</i></b> </p>
     *
     * <ul>
     *
     *   <li><p> An empty authority component is permitted as long as it is
     *   followed by a non-empty path, a query component, or a fragment
     *   component.  This allows the parsing of URIs such as
     *   {@code "file:///foo/bar"}, which seems to be the intent of
     *   RFC&nbsp;2396 although the grammar does not permit it.  If the
     *   authority component is empty then the user-information, host, and port
     *   components are undefined. </p></li>
     *
     *   <li><p> Empty relative paths are permitted; this seems to be the
     *   intent of RFC&nbsp;2396 although the grammar does not permit it.  The
     *   primary consequence of this deviation is that a standalone fragment
     *   such as {@code "#foo"} parses as a relative URI with an empty path
     *   and the given fragment, and can be usefully <a
     *   href="#resolve-frag">resolved</a> against a base URI.
     *
     *   <li><p> IPv4 addresses in host components are parsed rigorously, as
     *   specified by <a
     *   href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>: Each
     *   element of a dotted-quad address must contain no more than three
     *   decimal digits.  Each element is further constrained to have a value
     *   no greater than 255. </p></li>
     *
     *   <li> <p> Hostnames in host components that comprise only a single
     *   domain label are permitted to start with an <i>alphanum</i>
     *   character. This seems to be the intent of <a
     *   href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     *   section&nbsp;3.2.2 although the grammar does not permit it. The
     *   consequence of this deviation is that the authority component of a
     *   hierarchical URI such as {@code s://123}, will parse as a server-based
     *   authority. </p></li>
     *
     *   <li><p> IPv6 addresses are permitted for the host component.  An IPv6
     *   address must be enclosed in square brackets ({@code '['} and
     *   {@code ']'}) as specified by <a
     *   href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>.  The
     *   IPv6 address itself must parse according to <a
     *   href="http://www.ietf.org/rfc/rfc2373.txt">RFC&nbsp;2373</a>.  IPv6
     *   addresses are further constrained to describe no more than sixteen
     *   bytes of address information, a constraint implicit in RFC&nbsp;2373
     *   but not expressible in the grammar. </p></li>
     *
     *   <li><p> Characters in the <i>other</i> category are permitted wherever
     *   RFC&nbsp;2396 permits <i>escaped</i> octets, that is, in the
     *   user-information, path, query, and fragment components, as well as in
     *   the authority component if the authority is registry-based.  This
     *   allows URIs to contain Unicode characters beyond those in the US-ASCII
     *   character set. </p></li>
     *
     * </ul>
     *
     * @param  str   The string to be parsed into a URI
     *
     * @throws  NullPointerException
     *          If {@code str} is {@code null}
     *
     * @throws  URISyntaxException
     *          If the given string violates RFC&nbsp;2396, as augmented
     *          by the above deviations
     */
    public URI(String str) throws URISyntaxException {
        new Parser(str).parse(false);
    }

    /**
     * Constructs a hierarchical URI from the given components.
     *
     * <p> If a scheme is given then the path, if also given, must either be
     * empty or begin with a slash character ({@code '/'}).  Otherwise a
     * component of the new URI may be left undefined by passing {@code null}
     * for the corresponding parameter or, in the case of the {@code port}
     * parameter, by passing {@code -1}.
     *
     * <p> This constructor first builds a URI string from the given components
     * according to the rules specified in <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>,
     * section&nbsp;5.2, step&nbsp;7: </p>
     *
     * <ol>
     *
     *   <li><p> Initially, the result string is empty. </p></li>
     *
     *   <li><p> If a scheme is given then it is appended to the result,
     *   followed by a colon character ({@code ':'}).  </p></li>
     *
     *   <li><p> If user information, a host, or a port are given then the
     *   string {@code "//"} is appended.  </p></li>
     *
     *   <li><p> If user information is given then it is appended, followed by
     *   a commercial-at character ({@code '@'}).  Any character not in the
     *   <i>unreserved</i>, <i>punct</i>, <i>escaped</i>, or <i>other</i>
     *   categories is <a href="#quote">quoted</a>.  </p></li>
     *
     *   <li><p> If a host is given then it is appended.  If the host is a
     *   literal IPv6 address but is not enclosed in square brackets
     *   ({@code '['} and {@code ']'}) then the square brackets are added.
     *   </p></li>
     *
     *   <li><p> If a port number is given then a colon character
     *   ({@code ':'}) is appended, followed by the port number in decimal.
     *   </p></li>
     *
     *   <li><p> If a path is given then it is appended.  Any character not in
     *   the <i>unreserved</i>, <i>punct</i>, <i>escaped</i>, or <i>other</i>
     *   categories, and not equal to the slash character ({@code '/'}) or the
     *   commercial-at character ({@code '@'}), is quoted.  </p></li>
     *
     *   <li><p> If a query is given then a question-mark character
     *   ({@code '?'}) is appended, followed by the query.  Any character that
     *   is not a <a href="#legal-chars">legal URI character</a> is quoted.
     *   </p></li>
     *
     *   <li><p> Finally, if a fragment is given then a hash character
     *   ({@code '#'}) is appended, followed by the fragment.  Any character
     *   that is not a legal URI character is quoted.  </p></li>
     *
     * </ol>
     *
     * <p> The resulting URI string is then parsed as if by invoking the {@link
     * #URI(String)} constructor and then invoking the {@link
     * #parseServerAuthority()} method upon the result; this may cause a {@link
     * URISyntaxException} to be thrown.  </p>
     *
     * @param   scheme    Scheme name
     * @param   userInfo  User name and authorization information
     * @param   host      Host name
     * @param   port      Port number
     * @param   path      Path
     * @param   query     Query
     * @param   fragment  Fragment
     *
     * @throws URISyntaxException
     *         If both a scheme and a path are given but the path is relative,
     *         if the URI string constructed from the given components violates
     *         RFC&nbsp;2396, or if the authority component of the string is
     *         present but cannot be parsed as a server-based authority
     */
    public URI(String scheme,
               String userInfo, String host, int port,
               String path, String query, String fragment)
        throws URISyntaxException
    {
        String s = toString(scheme, null,
                            null, userInfo, host, port,
                            path, query, fragment);
        checkPath(s, scheme, path);
        new Parser(s).parse(true);
    }

    /**
     * Constructs a hierarchical URI from the given components.
     *
     * <p> If a scheme is given then the path, if also given, must either be
     * empty or begin with a slash character ({@code '/'}).  Otherwise a
     * component of the new URI may be left undefined by passing {@code null}
     * for the corresponding parameter.
     *
     * <p> This constructor first builds a URI string from the given components
     * according to the rules specified in <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>,
     * section&nbsp;5.2, step&nbsp;7: </p>
     *
     * <ol>
     *
     *   <li><p> Initially, the result string is empty.  </p></li>
     *
     *   <li><p> If a scheme is given then it is appended to the result,
     *   followed by a colon character ({@code ':'}).  </p></li>
     *
     *   <li><p> If an authority is given then the string {@code "//"} is
     *   appended, followed by the authority.  If the authority contains a
     *   literal IPv6 address then the address must be enclosed in square
     *   brackets ({@code '['} and {@code ']'}).  Any character not in the
     *   <i>unreserved</i>, <i>punct</i>, <i>escaped</i>, or <i>other</i>
     *   categories, and not equal to the commercial-at character
     *   ({@code '@'}), is <a href="#quote">quoted</a>.  </p></li>
     *
     *   <li><p> If a path is given then it is appended.  Any character not in
     *   the <i>unreserved</i>, <i>punct</i>, <i>escaped</i>, or <i>other</i>
     *   categories, and not equal to the slash character ({@code '/'}) or the
     *   commercial-at character ({@code '@'}), is quoted.  </p></li>
     *
     *   <li><p> If a query is given then a question-mark character
     *   ({@code '?'}) is appended, followed by the query.  Any character that
     *   is not a <a href="#legal-chars">legal URI character</a> is quoted.
     *   </p></li>
     *
     *   <li><p> Finally, if a fragment is given then a hash character
     *   ({@code '#'}) is appended, followed by the fragment.  Any character
     *   that is not a legal URI character is quoted.  </p></li>
     *
     * </ol>
     *
     * <p> The resulting URI string is then parsed as if by invoking the {@link
     * #URI(String)} constructor and then invoking the {@link
     * #parseServerAuthority()} method upon the result; this may cause a {@link
     * URISyntaxException} to be thrown.  </p>
     *
     * @param   scheme     Scheme name
     * @param   authority  Authority
     * @param   path       Path
     * @param   query      Query
     * @param   fragment   Fragment
     *
     * @throws URISyntaxException
     *         If both a scheme and a path are given but the path is relative,
     *         if the URI string constructed from the given components violates
     *         RFC&nbsp;2396, or if the authority component of the string is
     *         present but cannot be parsed as a server-based authority
     */
    public URI(String scheme,
               String authority,
               String path, String query, String fragment)
        throws URISyntaxException
    {
        String s = toString(scheme, null,
                            authority, null, null, -1,
                            path, query, fragment);
        checkPath(s, scheme, path);
        new Parser(s).parse(false);
    }

    /**
     * Constructs a hierarchical URI from the given components.
     *
     * <p> A component may be left undefined by passing {@code null}.
     *
     * <p> This convenience constructor works as if by invoking the
     * seven-argument constructor as follows:
     *
     * <blockquote>
     * {@code new} {@link #URI(String, String, String, int, String, String, String)
     * URI}{@code (scheme, null, host, -1, path, null, fragment);}
     * </blockquote>
     *
     * @param   scheme    Scheme name
     * @param   host      Host name
     * @param   path      Path
     * @param   fragment  Fragment
     *
     * @throws  URISyntaxException
     *          If the URI string constructed from the given components
     *          violates RFC&nbsp;2396
     */
    public URI(String scheme, String host, String path, String fragment)
        throws URISyntaxException
    {
        this(scheme, null, host, -1, path, null, fragment);
    }

    /**
     * Constructs a URI from the given components.
     *
     * <p> A component may be left undefined by passing {@code null}.
     *
     * <p> This constructor first builds a URI in string form using the given
     * components as follows:  </p>
     *
     * <ol>
     *
     *   <li><p> Initially, the result string is empty.  </p></li>
     *
     *   <li><p> If a scheme is given then it is appended to the result,
     *   followed by a colon character ({@code ':'}).  </p></li>
     *
     *   <li><p> If a scheme-specific part is given then it is appended.  Any
     *   character that is not a <a href="#legal-chars">legal URI character</a>
     *   is <a href="#quote">quoted</a>.  </p></li>
     *
     *   <li><p> Finally, if a fragment is given then a hash character
     *   ({@code '#'}) is appended to the string, followed by the fragment.
     *   Any character that is not a legal URI character is quoted.  </p></li>
     *
     * </ol>
     *
     * <p> The resulting URI string is then parsed in order to create the new
     * URI instance as if by invoking the {@link #URI(String)} constructor;
     * this may cause a {@link URISyntaxException} to be thrown.  </p>
     *
     * @param   scheme    Scheme name
     * @param   ssp       Scheme-specific part
     * @param   fragment  Fragment
     *
     * @throws  URISyntaxException
     *          If the URI string constructed from the given components
     *          violates RFC&nbsp;2396
     */
    public URI(String scheme, String ssp, String fragment)
        throws URISyntaxException
    {
        new Parser(toString(scheme, ssp,
                            null, null, null, -1,
                            null, null, fragment))
            .parse(false);
    }

    /**
     * Constructs a simple URI consisting of only a scheme and a pre-validated
     * path. Provides a fast-path for some internal cases.
     */
    URI(String scheme, String path) {
        assert validSchemeAndPath(scheme, path);
        this.scheme = scheme;
        this.path = path;
    }

    private static boolean validSchemeAndPath(String scheme, String path) {
        try {
            URI u = new URI(scheme + ":" + path);
            return scheme.equals(u.scheme) && path.equals(u.path);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Creates a URI by parsing the given string.
     *
     * <p> This convenience factory method works as if by invoking the {@link
     * #URI(String)} constructor; any {@link URISyntaxException} thrown by the
     * constructor is caught and wrapped in a new {@link
     * IllegalArgumentException} object, which is then thrown.
     *
     * <p> This method is provided for use in situations where it is known that
     * the given string is a legal URI, for example for URI constants declared
     * within a program, and so it would be considered a programming error
     * for the string not to parse as such.  The constructors, which throw
     * {@link URISyntaxException} directly, should be used in situations where a
     * URI is being constructed from user input or from some other source that
     * may be prone to errors.  </p>
     *
     * @param  str   The string to be parsed into a URI
     * @return The new URI
     *
     * @throws  NullPointerException
     *          If {@code str} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If the given string violates RFC&nbsp;2396
     */
    public static URI create(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }


    // -- Operations --

    /**
     * Attempts to parse this URI's authority component, if defined, into
     * user-information, host, and port components.
     *
     * <p> If this URI's authority component has already been recognized as
     * being server-based then it will already have been parsed into
     * user-information, host, and port components.  In this case, or if this
     * URI has no authority component, this method simply returns this URI.
     *
     * <p> Otherwise this method attempts once more to parse the authority
     * component into user-information, host, and port components, and throws
     * an exception describing why the authority component could not be parsed
     * in that way.
     *
     * <p> This method is provided because the generic URI syntax specified in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * cannot always distinguish a malformed server-based authority from a
     * legitimate registry-based authority.  It must therefore treat some
     * instances of the former as instances of the latter.  The authority
     * component in the URI string {@code "//foo:bar"}, for example, is not a
     * legal server-based authority but it is legal as a registry-based
     * authority.
     *
     * <p> In many common situations, for example when working URIs that are
     * known to be either URNs or URLs, the hierarchical URIs being used will
     * always be server-based.  They therefore must either be parsed as such or
     * treated as an error.  In these cases a statement such as
     *
     * <blockquote>
     * {@code URI }<i>u</i>{@code  = new URI(str).parseServerAuthority();}
     * </blockquote>
     *
     * <p> can be used to ensure that <i>u</i> always refers to a URI that, if
     * it has an authority component, has a server-based authority with proper
     * user-information, host, and port components.  Invoking this method also
     * ensures that if the authority could not be parsed in that way then an
     * appropriate diagnostic message can be issued based upon the exception
     * that is thrown. </p>
     *
     * @return  A URI whose authority field has been parsed
     *          as a server-based authority
     *
     * @throws  URISyntaxException
     *          If the authority component of this URI is defined
     *          but cannot be parsed as a server-based authority
     *          according to RFC&nbsp;2396
     */
    public URI parseServerAuthority()
        throws URISyntaxException
    {
        // We could be clever and cache the error message and index from the
        // exception thrown during the original parse, but that would require
        // either more fields or a more-obscure representation.
        if ((host != null) || (authority == null))
            return this;
        new Parser(toString()).parse(true);
        return this;
    }

    /**
     * Normalizes this URI's path.
     *
     * <p> If this URI is opaque, or if its path is already in normal form,
     * then this URI is returned.  Otherwise a new URI is constructed that is
     * identical to this URI except that its path is computed by normalizing
     * this URI's path in a manner consistent with <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>,
     * section&nbsp;5.2, step&nbsp;6, sub-steps&nbsp;c through&nbsp;f; that is:
     * </p>
     *
     * <ol>
     *
     *   <li><p> All {@code "."} segments are removed. </p></li>
     *
     *   <li><p> If a {@code ".."} segment is preceded by a non-{@code ".."}
     *   segment then both of these segments are removed.  This step is
     *   repeated until it is no longer applicable. </p></li>
     *
     *   <li><p> If the path is relative, and if its first segment contains a
     *   colon character ({@code ':'}), then a {@code "."} segment is
     *   prepended.  This prevents a relative URI with a path such as
     *   {@code "a:b/c/d"} from later being re-parsed as an opaque URI with a
     *   scheme of {@code "a"} and a scheme-specific part of {@code "b/c/d"}.
     *   <b><i>(Deviation from RFC&nbsp;2396)</i></b> </p></li>
     *
     * </ol>
     *
     * <p> A normalized path will begin with one or more {@code ".."} segments
     * if there were insufficient non-{@code ".."} segments preceding them to
     * allow their removal.  A normalized path will begin with a {@code "."}
     * segment if one was inserted by step 3 above.  Otherwise, a normalized
     * path will not contain any {@code "."} or {@code ".."} segments. </p>
     *
     * @return  A URI equivalent to this URI,
     *          but whose path is in normal form
     */
    public URI normalize() {
        return normalize(this);
    }

    /**
     * Resolves the given URI against this URI.
     *
     * <p> If the given URI is already absolute, or if this URI is opaque, then
     * the given URI is returned.
     *
     * <p><a id="resolve-frag"></a> If the given URI's fragment component is
     * defined, its path component is empty, and its scheme, authority, and
     * query components are undefined, then a URI with the given fragment but
     * with all other components equal to those of this URI is returned.  This
     * allows a URI representing a standalone fragment reference, such as
     * {@code "#foo"}, to be usefully resolved against a base URI.
     *
     * <p> Otherwise this method constructs a new hierarchical URI in a manner
     * consistent with <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>,
     * section&nbsp;5.2; that is: </p>
     *
     * <ol>
     *
     *   <li><p> A new URI is constructed with this URI's scheme and the given
     *   URI's query and fragment components. </p></li>
     *
     *   <li><p> If the given URI has an authority component then the new URI's
     *   authority and path are taken from the given URI. </p></li>
     *
     *   <li><p> Otherwise the new URI's authority component is copied from
     *   this URI, and its path is computed as follows: </p>
     *
     *   <ol>
     *
     *     <li><p> If the given URI's path is absolute then the new URI's path
     *     is taken from the given URI. </p></li>
     *
     *     <li><p> Otherwise the given URI's path is relative, and so the new
     *     URI's path is computed by resolving the path of the given URI
     *     against the path of this URI.  This is done by concatenating all but
     *     the last segment of this URI's path, if any, with the given URI's
     *     path and then normalizing the result as if by invoking the {@link
     *     #normalize() normalize} method. </p></li>
     *
     *   </ol></li>
     *
     * </ol>
     *
     * <p> The result of this method is absolute if, and only if, either this
     * URI is absolute or the given URI is absolute.  </p>
     *
     * @param  uri  The URI to be resolved against this URI
     * @return The resulting URI
     *
     * @throws  NullPointerException
     *          If {@code uri} is {@code null}
     */
    public URI resolve(URI uri) {
        return resolve(this, uri);
    }

    /**
     * Constructs a new URI by parsing the given string and then resolving it
     * against this URI.
     *
     * <p> This convenience method works as if invoking it were equivalent to
     * evaluating the expression {@link #resolve(java.net.URI)
     * resolve}{@code (URI.}{@link #create(String) create}{@code (str))}. </p>
     *
     * @param  str   The string to be parsed into a URI
     * @return The resulting URI
     *
     * @throws  NullPointerException
     *          If {@code str} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If the given string violates RFC&nbsp;2396
     */
    public URI resolve(String str) {
        return resolve(URI.create(str));
    }

    /**
     * Relativizes the given URI against this URI.
     *
     * <p> The relativization of the given URI against this URI is computed as
     * follows: </p>
     *
     * <ol>
     *
     *   <li><p> If either this URI or the given URI are opaque, or if the
     *   scheme and authority components of the two URIs are not identical, or
     *   if the path of this URI is not a prefix of the path of the given URI,
     *   then the given URI is returned. </p></li>
     *
     *   <li><p> Otherwise a new relative hierarchical URI is constructed with
     *   query and fragment components taken from the given URI and with a path
     *   component computed by removing this URI's path from the beginning of
     *   the given URI's path. </p></li>
     *
     * </ol>
     *
     * @param  uri  The URI to be relativized against this URI
     * @return The resulting URI
     *
     * @throws  NullPointerException
     *          If {@code uri} is {@code null}
     */
    public URI relativize(URI uri) {
        return relativize(this, uri);
    }

    /**
     * Constructs a URL from this URI.
     *
     * <p> This convenience method works as if invoking it were equivalent to
     * evaluating the expression {@code new URL(this.toString())} after
     * first checking that this URI is absolute. </p>
     *
     * @return  A URL constructed from this URI
     *
     * @throws  IllegalArgumentException
     *          If this URL is not absolute
     *
     * @throws  MalformedURLException
     *          If a protocol handler for the URL could not be found,
     *          or if some other error occurred while constructing the URL
     */
    public URL toURL() throws MalformedURLException {
        return URL.fromURI(this);
    }

    // -- Component access methods --

    /**
     * Returns the scheme component of this URI.
     *
     * <p> The scheme component of a URI, if defined, only contains characters
     * in the <i>alphanum</i> category and in the string {@code "-.+"}.  A
     * scheme always starts with an <i>alpha</i> character. <p>
     *
     * The scheme component of a URI cannot contain escaped octets, hence this
     * method does not perform any decoding.
     *
     * @return  The scheme component of this URI,
     *          or {@code null} if the scheme is undefined
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Tells whether or not this URI is absolute.
     *
     * <p> A URI is absolute if, and only if, it has a scheme component. </p>
     *
     * @return  {@code true} if, and only if, this URI is absolute
     */
    public boolean isAbsolute() {
        return scheme != null;
    }

    /**
     * Tells whether or not this URI is opaque.
     *
     * <p> A URI is opaque if, and only if, it is absolute and its
     * scheme-specific part does not begin with a slash character ('/').
     * An opaque URI has a scheme, a scheme-specific part, and possibly
     * a fragment; all other components are undefined. </p>
     *
     * @return  {@code true} if, and only if, this URI is opaque
     */
    public boolean isOpaque() {
        return path == null;
    }

    /**
     * Returns the raw scheme-specific part of this URI.  The scheme-specific
     * part is never undefined, though it may be empty.
     *
     * <p> The scheme-specific part of a URI only contains legal URI
     * characters. </p>
     *
     * @return  The raw scheme-specific part of this URI
     *          (never {@code null})
     */
    public String getRawSchemeSpecificPart() {
        String part = schemeSpecificPart;
        if (part != null) {
            return part;
        }

        String s = string;
        if (s != null) {
            // if string is defined, components will have been parsed
            int start = 0;
            int end = s.length();
            if (scheme != null) {
                start = scheme.length() + 1;
            }
            if (fragment != null) {
                end -= fragment.length() + 1;
            }
            if (path != null && path.length() == end - start) {
                part = path;
            } else {
                part = s.substring(start, end);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            appendSchemeSpecificPart(sb, null, getAuthority(), getUserInfo(),
                                 host, port, getPath(), getQuery());
            part = sb.toString();
        }
        return schemeSpecificPart = part;
    }

    /**
     * Returns the decoded scheme-specific part of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawSchemeSpecificPart() getRawSchemeSpecificPart} method
     * except that all sequences of escaped octets are <a
     * href="#decode">decoded</a>.  </p>
     *
     * @return  The decoded scheme-specific part of this URI
     *          (never {@code null})
     */
    public String getSchemeSpecificPart() {
        String part = decodedSchemeSpecificPart;
        if (part == null) {
            decodedSchemeSpecificPart = part = decode(getRawSchemeSpecificPart());
        }
        return part;
    }

    /**
     * Returns the raw authority component of this URI.
     *
     * <p> The authority component of a URI, if defined, only contains the
     * commercial-at character ({@code '@'}) and characters in the
     * <i>unreserved</i>, <i>punct</i>, <i>escaped</i>, and <i>other</i>
     * categories.  If the authority is server-based then it is further
     * constrained to have valid user-information, host, and port
     * components. </p>
     *
     * @return  The raw authority component of this URI,
     *          or {@code null} if the authority is undefined
     */
    public String getRawAuthority() {
        return authority;
    }

    /**
     * Returns the decoded authority component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawAuthority() getRawAuthority} method except that all
     * sequences of escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return  The decoded authority component of this URI,
     *          or {@code null} if the authority is undefined
     */
    public String getAuthority() {
        String auth = decodedAuthority;
        if ((auth == null) && (authority != null)) {
            decodedAuthority = auth = decode(authority);
        }
        return auth;
    }

    /**
     * Returns the raw user-information component of this URI.
     *
     * <p> The user-information component of a URI, if defined, only contains
     * characters in the <i>unreserved</i>, <i>punct</i>, <i>escaped</i>, and
     * <i>other</i> categories. </p>
     *
     * @return  The raw user-information component of this URI,
     *          or {@code null} if the user information is undefined
     */
    public String getRawUserInfo() {
        return userInfo;
    }

    /**
     * Returns the decoded user-information component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawUserInfo() getRawUserInfo} method except that all
     * sequences of escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return  The decoded user-information component of this URI,
     *          or {@code null} if the user information is undefined
     */
    public String getUserInfo() {
        String user = decodedUserInfo;
        if ((user == null) && (userInfo != null)) {
            decodedUserInfo = user = decode(userInfo);
        }
        return user;
    }

    /**
     * Returns the host component of this URI.
     *
     * <p> The host component of a URI, if defined, will have one of the
     * following forms: </p>
     *
     * <ul>
     *
     *   <li><p> A domain name consisting of one or more <i>labels</i>
     *   separated by period characters ({@code '.'}), optionally followed by
     *   a period character.  Each label consists of <i>alphanum</i> characters
     *   as well as hyphen characters ({@code '-'}), though hyphens never
     *   occur as the first or last characters in a label. The rightmost
     *   label of a domain name consisting of two or more labels, begins
     *   with an <i>alpha</i> character. </li>
     *
     *   <li><p> A dotted-quad IPv4 address of the form
     *   <i>digit</i>{@code +.}<i>digit</i>{@code +.}<i>digit</i>{@code +.}<i>digit</i>{@code +},
     *   where no <i>digit</i> sequence is longer than three characters and no
     *   sequence has a value larger than 255. </p></li>
     *
     *   <li><p> An IPv6 address enclosed in square brackets ({@code '['} and
     *   {@code ']'}) and consisting of hexadecimal digits, colon characters
     *   ({@code ':'}), and possibly an embedded IPv4 address.  The full
     *   syntax of IPv6 addresses is specified in <a
     *   href="http://www.ietf.org/rfc/rfc2373.txt"><i>RFC&nbsp;2373: IPv6
     *   Addressing Architecture</i></a>.  </p></li>
     *
     * </ul>
     *
     * The host component of a URI cannot contain escaped octets, hence this
     * method does not perform any decoding.
     *
     * @return  The host component of this URI,
     *          or {@code null} if the host is undefined
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port number of this URI.
     *
     * <p> The port component of a URI, if defined, is a non-negative
     * integer. </p>
     *
     * @return  The port component of this URI,
     *          or {@code -1} if the port is undefined
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the raw path component of this URI.
     *
     * <p> The path component of a URI, if defined, only contains the slash
     * character ({@code '/'}), the commercial-at character ({@code '@'}),
     * and characters in the <i>unreserved</i>, <i>punct</i>, <i>escaped</i>,
     * and <i>other</i> categories. </p>
     *
     * @return  The path component of this URI,
     *          or {@code null} if the path is undefined
     */
    public String getRawPath() {
        return path;
    }

    /**
     * Returns the decoded path component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawPath() getRawPath} method except that all sequences of
     * escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return  The decoded path component of this URI,
     *          or {@code null} if the path is undefined
     */
    public String getPath() {
        String decoded = decodedPath;
        if ((decoded == null) && (path != null)) {
            decodedPath = decoded = decode(path);
        }
        return decoded;
    }

    /**
     * Returns the raw query component of this URI.
     *
     * <p> The query component of a URI, if defined, only contains legal URI
     * characters. </p>
     *
     * @return  The raw query component of this URI,
     *          or {@code null} if the query is undefined
     */
    public String getRawQuery() {
        return query;
    }

    /**
     * Returns the decoded query component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawQuery() getRawQuery} method except that all sequences of
     * escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return  The decoded query component of this URI,
     *          or {@code null} if the query is undefined
     */
    public String getQuery() {
        String decoded = decodedQuery;
        if ((decoded == null) && (query != null)) {
            decodedQuery = decoded = decode(query, false);
        }
        return decoded;
    }

    /**
     * Returns the raw fragment component of this URI.
     *
     * <p> The fragment component of a URI, if defined, only contains legal URI
     * characters. </p>
     *
     * @return  The raw fragment component of this URI,
     *          or {@code null} if the fragment is undefined
     */
    public String getRawFragment() {
        return fragment;
    }

    /**
     * Returns the decoded fragment component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawFragment() getRawFragment} method except that all
     * sequences of escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return  The decoded fragment component of this URI,
     *          or {@code null} if the fragment is undefined
     */
    public String getFragment() {
        String decoded = decodedFragment;
        if ((decoded == null) && (fragment != null)) {
            decodedFragment = decoded = decode(fragment, false);
        }
        return decoded;
    }


    // -- Equality, comparison, hash code, toString, and serialization --

    /**
     * Tests this URI for equality with another object.
     *
     * <p> If the given object is not a URI then this method immediately
     * returns {@code false}.
     *
     * <p> For two URIs to be considered equal requires that either both are
     * opaque or both are hierarchical.  Their schemes must either both be
     * undefined or else be equal without regard to case. Their fragments
     * must either both be undefined or else be equal.
     *
     * <p> For two opaque URIs to be considered equal, their scheme-specific
     * parts must be equal.
     *
     * <p> For two hierarchical URIs to be considered equal, their paths must
     * be equal and their queries must either both be undefined or else be
     * equal.  Their authorities must either both be undefined, or both be
     * registry-based, or both be server-based.  If their authorities are
     * defined and are registry-based, then they must be equal.  If their
     * authorities are defined and are server-based, then their hosts must be
     * equal without regard to case, their port numbers must be equal, and
     * their user-information components must be equal.
     *
     * <p> When testing the user-information, path, query, fragment, authority,
     * or scheme-specific parts of two URIs for equality, the raw forms rather
     * than the encoded forms of these components are compared and the
     * hexadecimal digits of escaped octets are compared without regard to
     * case.
     *
     * <p> This method satisfies the general contract of the {@link
     * java.lang.Object#equals(Object) Object.equals} method. </p>
     *
     * @param   ob   The object to which this object is to be compared
     *
     * @return  {@code true} if, and only if, the given object is a URI that
     *          is identical to this URI
     */
    public boolean equals(Object ob) {
        if (ob == this)
            return true;
        if (!(ob instanceof URI that))
            return false;
        if (this.isOpaque() != that.isOpaque()) return false;
        if (!equalIgnoringCase(this.scheme, that.scheme)) return false;
        if (!equal(this.fragment, that.fragment)) return false;

        // Opaque
        if (this.isOpaque())
            return equal(this.schemeSpecificPart, that.schemeSpecificPart);

        // Hierarchical
        if (!equal(this.path, that.path)) return false;
        if (!equal(this.query, that.query)) return false;

        // Authorities
        if (this.authority == that.authority) return true;
        if (this.host != null) {
            // Server-based
            if (!equal(this.userInfo, that.userInfo)) return false;
            if (!equalIgnoringCase(this.host, that.host)) return false;
            if (this.port != that.port) return false;
        } else if (this.authority != null) {
            // Registry-based
            if (!equal(this.authority, that.authority)) return false;
        } else if (this.authority != that.authority) {
            return false;
        }

        return true;
    }

    /**
     * Returns a hash-code value for this URI.  The hash code is based upon all
     * of the URI's components, and satisfies the general contract of the
     * {@link java.lang.Object#hashCode() Object.hashCode} method.
     *
     * @return  A hash-code value for this URI
     */
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = hashIgnoringCase(0, scheme);
            h = hash(h, fragment);
            if (isOpaque()) {
                h = hash(h, schemeSpecificPart);
            } else {
                h = hash(h, path);
                h = hash(h, query);
                if (host != null) {
                    h = hash(h, userInfo);
                    h = hashIgnoringCase(h, host);
                    h += 1949 * port;
                } else {
                    h = hash(h, authority);
                }
            }
            if (h != 0) {
                hash = h;
            }
        }
        return h;
    }

    /**
     * Compares this URI to another object, which must be a URI.
     *
     * <p> When comparing corresponding components of two URIs, if one
     * component is undefined but the other is defined then the first is
     * considered to be less than the second.  Unless otherwise noted, string
     * components are ordered according to their natural, case-sensitive
     * ordering as defined by the {@link java.lang.String#compareTo(String)
     * String.compareTo} method.  String components that are subject to
     * encoding are compared by comparing their raw forms rather than their
     * encoded forms and the hexadecimal digits of escaped octets are compared
     * without regard to case.
     *
     * <p> The ordering of URIs is defined as follows: </p>
     *
     * <ul>
     *
     *   <li><p> Two URIs with different schemes are ordered according the
     *   ordering of their schemes, without regard to case. </p></li>
     *
     *   <li><p> A hierarchical URI is considered to be less than an opaque URI
     *   with an identical scheme. </p></li>
     *
     *   <li><p> Two opaque URIs with identical schemes are ordered according
     *   to the ordering of their scheme-specific parts. </p></li>
     *
     *   <li><p> Two opaque URIs with identical schemes and scheme-specific
     *   parts are ordered according to the ordering of their
     *   fragments. </p></li>
     *
     *   <li><p> Two hierarchical URIs with identical schemes are ordered
     *   according to the ordering of their authority components: </p>
     *
     *   <ul>
     *
     *     <li><p> If both authority components are server-based then the URIs
     *     are ordered according to their user-information components; if these
     *     components are identical then the URIs are ordered according to the
     *     ordering of their hosts, without regard to case; if the hosts are
     *     identical then the URIs are ordered according to the ordering of
     *     their ports. </p></li>
     *
     *     <li><p> If one or both authority components are registry-based then
     *     the URIs are ordered according to the ordering of their authority
     *     components. </p></li>
     *
     *   </ul></li>
     *
     *   <li><p> Finally, two hierarchical URIs with identical schemes and
     *   authority components are ordered according to the ordering of their
     *   paths; if their paths are identical then they are ordered according to
     *   the ordering of their queries; if the queries are identical then they
     *   are ordered according to the order of their fragments. </p></li>
     *
     * </ul>
     *
     * <p> This method satisfies the general contract of the {@link
     * java.lang.Comparable#compareTo(Object) Comparable.compareTo}
     * method. </p>
     *
     * @param   that
     *          The object to which this URI is to be compared
     *
     * @return  A negative integer, zero, or a positive integer as this URI is
     *          less than, equal to, or greater than the given URI
     *
     * @throws  ClassCastException
     *          If the given object is not a URI
     */
    public int compareTo(URI that) {
        int c;

        if ((c = compareIgnoringCase(this.scheme, that.scheme)) != 0)
            return c;

        if (this.isOpaque()) {
            if (that.isOpaque()) {
                // Both opaque
                if ((c = compare(this.schemeSpecificPart,
                                 that.schemeSpecificPart)) != 0)
                    return c;
                return compare(this.fragment, that.fragment);
            }
            return +1;                  // Opaque > hierarchical
        } else if (that.isOpaque()) {
            return -1;                  // Hierarchical < opaque
        }

        // Hierarchical
        if ((this.host != null) && (that.host != null)) {
            // Both server-based
            if ((c = compare(this.userInfo, that.userInfo)) != 0)
                return c;
            if ((c = compareIgnoringCase(this.host, that.host)) != 0)
                return c;
            if ((c = this.port - that.port) != 0)
                return c;
        } else {
            // If one or both authorities are registry-based then we simply
            // compare them in the usual, case-sensitive way.  If one is
            // registry-based and one is server-based then the strings are
            // guaranteed to be unequal, hence the comparison will never return
            // zero and the compareTo and equals methods will remain
            // consistent.
            if ((c = compare(this.authority, that.authority)) != 0) return c;
        }

        if ((c = compare(this.path, that.path)) != 0) return c;
        if ((c = compare(this.query, that.query)) != 0) return c;
        return compare(this.fragment, that.fragment);
    }

    /**
     * Returns the content of this URI as a string.
     *
     * <p> If this URI was created by invoking one of the constructors in this
     * class then a string equivalent to the original input string, or to the
     * string computed from the originally-given components, as appropriate, is
     * returned.  Otherwise this URI was created by normalization, resolution,
     * or relativization, and so a string is constructed from this URI's
     * components according to the rules specified in <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>,
     * section&nbsp;5.2, step&nbsp;7. </p>
     *
     * @return  The string form of this URI
     */
    public String toString() {
        String s = string;
        if (s == null) {
            s = defineString();
        }
        return s;
    }

    private String defineString() {
        String s = string;
        if (s != null) {
            return s;
        }

        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        if (isOpaque()) {
            sb.append(schemeSpecificPart);
        } else {
            if (host != null) {
                sb.append("//");
                if (userInfo != null) {
                    sb.append(userInfo);
                    sb.append('@');
                }
                boolean needBrackets = ((host.indexOf(':') >= 0)
                        && !host.startsWith("[")
                        && !host.endsWith("]"));
                if (needBrackets) sb.append('[');
                sb.append(host);
                if (needBrackets) sb.append(']');
                if (port != -1) {
                    sb.append(':');
                    sb.append(port);
                }
            } else if (authority != null) {
                sb.append("//");
                sb.append(authority);
            }
            if (path != null)
                sb.append(path);
            if (query != null) {
                sb.append('?');
                sb.append(query);
            }
        }
        if (fragment != null) {
            sb.append('#');
            sb.append(fragment);
        }
        return string = sb.toString();
    }

    /**
     * Returns the content of this URI as a US-ASCII string.
     *
     * <p> If this URI does not contain any characters in the <i>other</i>
     * category then an invocation of this method will return the same value as
     * an invocation of the {@link #toString() toString} method.  Otherwise
     * this method works as if by invoking that method and then <a
     * href="#encode">encoding</a> the result.  </p>
     *
     * @return  The string form of this URI, encoded as needed
     *          so that it only contains characters in the US-ASCII
     *          charset
     */
    public String toASCIIString() {
        return encode(toString());
    }


    // -- Serialization support --

    /**
     * Saves the content of this URI to the given serial stream.
     *
     * <p> The only serializable field of a URI instance is its {@code string}
     * field.  That field is given a value, if it does not have one already,
     * and then the {@link java.io.ObjectOutputStream#defaultWriteObject()}
     * method of the given object-output stream is invoked. </p>
     *
     * @param  os  The object-output stream to which this object
     *             is to be written
     *
     * @throws IOException
     *         If an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream os)
        throws IOException
    {
        defineString();
        os.defaultWriteObject();        // Writes the string field only
    }

    /**
     * Reconstitutes a URI from the given serial stream.
     *
     * <p> The {@link java.io.ObjectInputStream#defaultReadObject()} method is
     * invoked to read the value of the {@code string} field.  The result is
     * then parsed in the usual way.
     *
     * @param  is  The object-input stream from which this object
     *             is being read
     *
     * @throws IOException
     *         If an I/O error occurs
     *
     * @throws ClassNotFoundException
     *         If a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream is)
        throws ClassNotFoundException, IOException
    {
        port = -1;                      // Argh
        is.defaultReadObject();
        try {
            new Parser(string).parse(false);
        } catch (URISyntaxException x) {
            IOException y = new InvalidObjectException("Invalid URI");
            y.initCause(x);
            throw y;
        }
    }


    // -- End of public methods --


    // -- Utility methods for string-field comparison and hashing --

    // These methods return appropriate values for null string arguments,
    // thereby simplifying the equals, hashCode, and compareTo methods.
    //
    // The case-ignoring methods should only be applied to strings whose
    // characters are all known to be US-ASCII.  Because of this restriction,
    // these methods are faster than the similar methods in the String class.

    // US-ASCII only
    private static int toLower(char c) {
        if ((c >= 'A') && (c <= 'Z'))
            return c + ('a' - 'A');
        return c;
    }

    // US-ASCII only
    private static int toUpper(char c) {
        if ((c >= 'a') && (c <= 'z'))
            return c - ('a' - 'A');
        return c;
    }

    private static boolean equal(String s, String t) {
        boolean testForEquality = true;
        int result = percentNormalizedComparison(s, t, testForEquality);
        return result == 0;
    }

    // US-ASCII only
    private static boolean equalIgnoringCase(String s, String t) {
        if (s == t) return true;
        if ((s != null) && (t != null)) {
            int n = s.length();
            if (t.length() != n)
                return false;
            for (int i = 0; i < n; i++) {
                if (toLower(s.charAt(i)) != toLower(t.charAt(i)))
                    return false;
            }
            return true;
        }
        return false;
    }

    private static int hash(int hash, String s) {
        if (s == null) return hash;
        return s.indexOf('%') < 0 ? hash * 127 + s.hashCode()
                                  : normalizedHash(hash, s);
    }


    private static int normalizedHash(int hash, String s) {
        int h = 0;
        for (int index = 0; index < s.length(); index++) {
            char ch = s.charAt(index);
            h = 31 * h + ch;
            if (ch == '%') {
                /*
                 * Process the next two encoded characters
                 */
                for (int i = index + 1; i < index + 3; i++)
                    h = 31 * h + toUpper(s.charAt(i));
                index += 2;
            }
        }
        return hash * 127 + h;
    }

    // US-ASCII only
    private static int hashIgnoringCase(int hash, String s) {
        if (s == null) return hash;
        int h = hash;
        int n = s.length();
        for (int i = 0; i < n; i++)
            h = 31 * h + toLower(s.charAt(i));
        return h;
    }

    private static int compare(String s, String t) {
        boolean testForEquality = false;
        int result = percentNormalizedComparison(s, t, testForEquality);
        return result;
    }

    // The percentNormalizedComparison method does not verify two
    // characters that follow the % sign are hexadecimal digits.
    // Reason being:
    // 1) percentNormalizedComparison method is not called with
    // 'decoded' strings
    // 2) The only place where a percent can be followed by anything
    // other than hexadecimal digits is in the authority component
    // (for a IPv6 scope) and the whole authority component is case
    // insensitive.
    private static int percentNormalizedComparison(String s, String t,
                                                   boolean testForEquality) {

        if (s == t) return 0;
        if (s != null) {
            if (t != null) {
                if (s.indexOf('%') < 0) {
                    return s.compareTo(t);
                }
                int sn = s.length();
                int tn = t.length();
                if ((sn != tn) && testForEquality)
                    return sn - tn;
                int val = 0;
                int n = sn < tn ? sn : tn;
                for (int i = 0; i < n; ) {
                    char c = s.charAt(i);
                    char d = t.charAt(i);
                    val = c - d;
                    if (c != '%') {
                        if (val != 0)
                            return val;
                        i++;
                        continue;
                    }
                    if (d != '%') {
                        if (val != 0)
                            return val;
                    }
                    i++;
                    val = toLower(s.charAt(i)) - toLower(t.charAt(i));
                    if (val != 0)
                        return val;
                    i++;
                    val = toLower(s.charAt(i)) - toLower(t.charAt(i));
                    if (val != 0)
                        return val;
                    i++;
                }
                return sn - tn;
            } else
                return +1;
        } else {
            return -1;
        }
    }

    // US-ASCII only
    private static int compareIgnoringCase(String s, String t) {
        if (s == t) return 0;
        if (s != null) {
            if (t != null) {
                int sn = s.length();
                int tn = t.length();
                int n = sn < tn ? sn : tn;
                for (int i = 0; i < n; i++) {
                    int c = toLower(s.charAt(i)) - toLower(t.charAt(i));
                    if (c != 0)
                        return c;
                }
                return sn - tn;
            }
            return +1;
        } else {
            return -1;
        }
    }


    // -- String construction --

    // If a scheme is given then the path, if given, must be absolute
    //
    private static void checkPath(String s, String scheme, String path)
        throws URISyntaxException
    {
        if (scheme != null) {
            if (path != null && !path.isEmpty() && path.charAt(0) != '/')
                throw new URISyntaxException(s, "Relative path in absolute URI");
        }
    }

    private void appendAuthority(StringBuilder sb,
                                 String authority,
                                 String userInfo,
                                 String host,
                                 int port)
    {
        if (host != null) {
            sb.append("//");
            if (userInfo != null) {
                sb.append(quote(userInfo, L_USERINFO, H_USERINFO));
                sb.append('@');
            }
            boolean needBrackets = ((host.indexOf(':') >= 0)
                                    && !host.startsWith("[")
                                    && !host.endsWith("]"));
            if (needBrackets) sb.append('[');
            sb.append(host);
            if (needBrackets) sb.append(']');
            if (port != -1) {
                sb.append(':');
                sb.append(port);
            }
        } else if (authority != null) {
            sb.append("//");
            if (authority.startsWith("[")) {
                // authority should (but may not) contain an embedded IPv6 address
                int end = authority.indexOf(']');
                String doquote = authority, dontquote = "";
                if (end != -1 && authority.indexOf(':') != -1) {
                    // the authority contains an IPv6 address
                    if (end == authority.length()) {
                        dontquote = authority;
                        doquote = "";
                    } else {
                        dontquote = authority.substring(0 , end + 1);
                        doquote = authority.substring(end + 1);
                    }
                }
                sb.append(dontquote);
                sb.append(quote(doquote,
                            L_REG_NAME | L_SERVER,
                            H_REG_NAME | H_SERVER));
            } else {
                sb.append(quote(authority,
                            L_REG_NAME | L_SERVER,
                            H_REG_NAME | H_SERVER));
            }
        }
    }

    private void appendSchemeSpecificPart(StringBuilder sb,
                                          String opaquePart,
                                          String authority,
                                          String userInfo,
                                          String host,
                                          int port,
                                          String path,
                                          String query)
    {
        if (opaquePart != null) {
            /* check if SSP begins with an IPv6 address
             * because we must not quote a literal IPv6 address
             */
            if (opaquePart.startsWith("//[")) {
                int end =  opaquePart.indexOf(']');
                if (end != -1 && opaquePart.indexOf(':')!=-1) {
                    String doquote, dontquote;
                    if (end == opaquePart.length()) {
                        dontquote = opaquePart;
                        doquote = "";
                    } else {
                        dontquote = opaquePart.substring(0,end+1);
                        doquote = opaquePart.substring(end+1);
                    }
                    sb.append (dontquote);
                    sb.append(quote(doquote, L_URIC, H_URIC));
                }
            } else {
                sb.append(quote(opaquePart, L_URIC, H_URIC));
            }
        } else {
            appendAuthority(sb, authority, userInfo, host, port);
            if (path != null)
                sb.append(quote(path, L_PATH, H_PATH));
            if (query != null) {
                sb.append('?');
                sb.append(quote(query, L_URIC, H_URIC));
            }
        }
    }

    private void appendFragment(StringBuilder sb, String fragment) {
        if (fragment != null) {
            sb.append('#');
            sb.append(quote(fragment, L_URIC, H_URIC));
        }
    }

    private String toString(String scheme,
                            String opaquePart,
                            String authority,
                            String userInfo,
                            String host,
                            int port,
                            String path,
                            String query,
                            String fragment)
    {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        appendSchemeSpecificPart(sb, opaquePart,
                                 authority, userInfo, host, port,
                                 path, query);
        appendFragment(sb, fragment);
        return sb.toString();
    }

    // -- Normalization, resolution, and relativization --

    // RFC2396 5.2 (6)
    private static String resolvePath(String base, String child,
                                      boolean absolute)
    {
        int i = base.lastIndexOf('/');
        int cn = child.length();
        String path = "";

        if (cn == 0) {
            // 5.2 (6a)
            if (i >= 0)
                path = base.substring(0, i + 1);
        } else {
            StringBuilder sb = new StringBuilder(base.length() + cn);
            // 5.2 (6a)
            if (i >= 0)
                sb.append(base, 0, i + 1);
            // 5.2 (6b)
            sb.append(child);
            path = sb.toString();
        }

        // 5.2 (6c-f)
        String np = normalize(path);

        // 5.2 (6g): If the result is absolute but the path begins with "../",
        // then we simply leave the path as-is

        return np;
    }

    // RFC2396 5.2
    private static URI resolve(URI base, URI child) {
        // check if child if opaque first so that NPE is thrown
        // if child is null.
        if (child.isOpaque() || base.isOpaque())
            return child;

        // 5.2 (2): Reference to current document (lone fragment)
        if ((child.scheme == null) && (child.authority == null)
            && child.path.isEmpty() && (child.fragment != null)
            && (child.query == null)) {
            if ((base.fragment != null)
                && child.fragment.equals(base.fragment)) {
                return base;
            }
            URI ru = new URI();
            ru.scheme = base.scheme;
            ru.authority = base.authority;
            ru.userInfo = base.userInfo;
            ru.host = base.host;
            ru.port = base.port;
            ru.path = base.path;
            ru.fragment = child.fragment;
            ru.query = base.query;
            return ru;
        }

        // 5.2 (3): Child is absolute
        if (child.scheme != null)
            return child;

        URI ru = new URI();             // Resolved URI
        ru.scheme = base.scheme;
        ru.query = child.query;
        ru.fragment = child.fragment;

        // 5.2 (4): Authority
        if (child.authority == null) {
            ru.authority = base.authority;
            ru.host = base.host;
            ru.userInfo = base.userInfo;
            ru.port = base.port;

            String cp = (child.path == null) ? "" : child.path;
            if (!cp.isEmpty() && cp.charAt(0) == '/') {
                // 5.2 (5): Child path is absolute
                ru.path = child.path;
            } else {
                // 5.2 (6): Resolve relative path
                ru.path = resolvePath(base.path, cp, base.isAbsolute());
            }
        } else {
            ru.authority = child.authority;
            ru.host = child.host;
            ru.userInfo = child.userInfo;
            ru.host = child.host;
            ru.port = child.port;
            ru.path = child.path;
        }

        // 5.2 (7): Recombine (nothing to do here)
        return ru;
    }

    // If the given URI's path is normal then return the URI;
    // o.w., return a new URI containing the normalized path.
    //
    private static URI normalize(URI u) {
        if (u.isOpaque() || u.path == null || u.path.isEmpty())
            return u;

        String np = normalize(u.path);
        if (np == u.path)
            return u;

        URI v = new URI();
        v.scheme = u.scheme;
        v.fragment = u.fragment;
        v.authority = u.authority;
        v.userInfo = u.userInfo;
        v.host = u.host;
        v.port = u.port;
        v.path = np;
        v.query = u.query;
        return v;
    }

    // If both URIs are hierarchical, their scheme and authority components are
    // identical, and the base path is a prefix of the child's path, then
    // return a relative URI that, when resolved against the base, yields the
    // child; otherwise, return the child.
    //
    private static URI relativize(URI base, URI child) {
        // check if child if opaque first so that NPE is thrown
        // if child is null.
        if (child.isOpaque() || base.isOpaque())
            return child;
        if (!equalIgnoringCase(base.scheme, child.scheme)
            || !equal(base.authority, child.authority))
            return child;

        String bp = normalize(base.path);
        String cp = normalize(child.path);
        if (!bp.equals(cp)) {
            if (!bp.endsWith("/"))
                bp = bp + "/";
            if (!cp.startsWith(bp))
                return child;
        }

        URI v = new URI();
        v.path = cp.substring(bp.length());
        v.query = child.query;
        v.fragment = child.fragment;
        return v;
    }



    // -- Path normalization --

    // The following algorithm for path normalization avoids the creation of a
    // string object for each segment, as well as the use of a string buffer to
    // compute the final result, by using a single char array and editing it in
    // place.  The array is first split into segments, replacing each slash
    // with '\0' and creating a segment-index array, each element of which is
    // the index of the first char in the corresponding segment.  We then walk
    // through both arrays, removing ".", "..", and other segments as necessary
    // by setting their entries in the index array to -1.  Finally, the two
    // arrays are used to rejoin the segments and compute the final result.
    //
    // This code is based upon src/solaris/native/java/io/canonicalize_md.c


    // Check the given path to see if it might need normalization.  A path
    // might need normalization if it contains duplicate slashes, a "."
    // segment, or a ".." segment.  Return -1 if no further normalization is
    // possible, otherwise return the number of segments found.
    //
    // This method takes a string argument rather than a char array so that
    // this test can be performed without invoking path.toCharArray().
    //
    private static int needsNormalization(String path) {
        boolean normal = true;
        int ns = 0;                     // Number of segments
        int end = path.length() - 1;    // Index of last char in path
        int p = 0;                      // Index of next char in path

        // Skip initial slashes
        while (p <= end) {
            if (path.charAt(p) != '/') break;
            p++;
        }
        if (p > 1) normal = false;

        // Scan segments
        while (p <= end) {

            // Looking at "." or ".." ?
            if ((path.charAt(p) == '.')
                && ((p == end)
                    || ((path.charAt(p + 1) == '/')
                        || ((path.charAt(p + 1) == '.')
                            && ((p + 1 == end)
                                || (path.charAt(p + 2) == '/')))))) {
                normal = false;
            }
            ns++;

            // Find beginning of next segment
            while (p <= end) {
                if (path.charAt(p++) != '/')
                    continue;

                // Skip redundant slashes
                while (p <= end) {
                    if (path.charAt(p) != '/') break;
                    normal = false;
                    p++;
                }

                break;
            }
        }

        return normal ? -1 : ns;
    }


    // Split the given path into segments, replacing slashes with nulls and
    // filling in the given segment-index array.
    //
    // Preconditions:
    //   segs.length == Number of segments in path
    //
    // Postconditions:
    //   All slashes in path replaced by '\0'
    //   segs[i] == Index of first char in segment i (0 <= i < segs.length)
    //
    private static void split(char[] path, int[] segs) {
        int end = path.length - 1;      // Index of last char in path
        int p = 0;                      // Index of next char in path
        int i = 0;                      // Index of current segment

        // Skip initial slashes
        while (p <= end) {
            if (path[p] != '/') break;
            path[p] = '\0';
            p++;
        }

        while (p <= end) {

            // Note start of segment
            segs[i++] = p++;

            // Find beginning of next segment
            while (p <= end) {
                if (path[p++] != '/')
                    continue;
                path[p - 1] = '\0';

                // Skip redundant slashes
                while (p <= end) {
                    if (path[p] != '/') break;
                    path[p++] = '\0';
                }
                break;
            }
        }

        if (i != segs.length)
            throw new InternalError();  // ASSERT
    }


    // Join the segments in the given path according to the given segment-index
    // array, ignoring those segments whose index entries have been set to -1,
    // and inserting slashes as needed.  Return the length of the resulting
    // path.
    //
    // Preconditions:
    //   segs[i] == -1 implies segment i is to be ignored
    //   path computed by split, as above, with '\0' having replaced '/'
    //
    // Postconditions:
    //   path[0] .. path[return value] == Resulting path
    //
    private static int join(char[] path, int[] segs) {
        int ns = segs.length;           // Number of segments
        int end = path.length - 1;      // Index of last char in path
        int p = 0;                      // Index of next path char to write

        if (path[p] == '\0') {
            // Restore initial slash for absolute paths
            path[p++] = '/';
        }

        for (int i = 0; i < ns; i++) {
            int q = segs[i];            // Current segment
            if (q == -1)
                // Ignore this segment
                continue;

            if (p == q) {
                // We're already at this segment, so just skip to its end
                while ((p <= end) && (path[p] != '\0'))
                    p++;
                if (p <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else if (p < q) {
                // Copy q down to p
                while ((q <= end) && (path[q] != '\0'))
                    path[p++] = path[q++];
                if (q <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else
                throw new InternalError(); // ASSERT false
        }

        return p;
    }


    // Remove "." segments from the given path, and remove segment pairs
    // consisting of a non-".." segment followed by a ".." segment.
    //
    private static void removeDots(char[] path, int[] segs) {
        int ns = segs.length;
        int end = path.length - 1;

        for (int i = 0; i < ns; i++) {
            int dots = 0;               // Number of dots found (0, 1, or 2)

            // Find next occurrence of "." or ".."
            do {
                int p = segs[i];
                if (path[p] == '.') {
                    if (p == end) {
                        dots = 1;
                        break;
                    } else if (path[p + 1] == '\0') {
                        dots = 1;
                        break;
                    } else if ((path[p + 1] == '.')
                               && ((p + 1 == end)
                                   || (path[p + 2] == '\0'))) {
                        dots = 2;
                        break;
                    }
                }
                i++;
            } while (i < ns);
            if ((i > ns) || (dots == 0))
                break;

            if (dots == 1) {
                // Remove this occurrence of "."
                segs[i] = -1;
            } else {
                // If there is a preceding non-".." segment, remove both that
                // segment and this occurrence of ".."; otherwise, leave this
                // ".." segment as-is.
                int j;
                for (j = i - 1; j >= 0; j--) {
                    if (segs[j] != -1) break;
                }
                if (j >= 0) {
                    int q = segs[j];
                    if (!((path[q] == '.')
                          && (path[q + 1] == '.')
                          && (path[q + 2] == '\0'))) {
                        segs[i] = -1;
                        segs[j] = -1;
                    }
                }
            }
        }
    }


    // DEVIATION: If the normalized path is relative, and if the first
    // segment could be parsed as a scheme name, then prepend a "." segment
    //
    private static void maybeAddLeadingDot(char[] path, int[] segs) {

        if (path[0] == '\0')
            // The path is absolute
            return;

        int ns = segs.length;
        int f = 0;                      // Index of first segment
        while (f < ns) {
            if (segs[f] >= 0)
                break;
            f++;
        }
        if ((f >= ns) || (f == 0))
            // The path is empty, or else the original first segment survived,
            // in which case we already know that no leading "." is needed
            return;

        int p = segs[f];
        while ((p < path.length) && (path[p] != ':') && (path[p] != '\0')) p++;
        if (p >= path.length || path[p] == '\0')
            // No colon in first segment, so no "." needed
            return;

        // At this point we know that the first segment is unused,
        // hence we can insert a "." segment at that position
        path[0] = '.';
        path[1] = '\0';
        segs[0] = 0;
    }


    // Normalize the given path string.  A normal path string has no empty
    // segments (i.e., occurrences of "//"), no segments equal to ".", and no
    // segments equal to ".." that are preceded by a segment not equal to "..".
    // In contrast to Unix-style pathname normalization, for URI paths we
    // always retain trailing slashes.
    //
    private static String normalize(String ps) {

        // Does this path need normalization?
        int ns = needsNormalization(ps);        // Number of segments
        if (ns < 0)
            // Nope -- just return it
            return ps;

        char[] path = ps.toCharArray();         // Path in char-array form

        // Split path into segments
        int[] segs = new int[ns];               // Segment-index array
        split(path, segs);

        // Remove dots
        removeDots(path, segs);

        // Prevent scheme-name confusion
        maybeAddLeadingDot(path, segs);

        // Join the remaining segments and return the result
        String s = new String(path, 0, join(path, segs));
        if (s.equals(ps)) {
            // string was already normalized
            return ps;
        }
        return s;
    }



    // -- Character classes for parsing --

    // RFC2396 precisely specifies which characters in the US-ASCII charset are
    // permissible in the various components of a URI reference.  We here
    // define a set of mask pairs to aid in enforcing these restrictions.  Each
    // mask pair consists of two longs, a low mask and a high mask.  Taken
    // together they represent a 128-bit mask, where bit i is set iff the
    // character with value i is permitted.
    //
    // This approach is more efficient than sequentially searching arrays of
    // permitted characters.  It could be made still more efficient by
    // precompiling the mask information so that a character's presence in a
    // given mask could be determined by a single table lookup.

    // To save startup time, we manually calculate the low-/highMask constants.
    // For reference, the following methods were used to calculate the values:

    // Compute the low-order mask for the characters in the given string
    //     private static long lowMask(String chars) {
    //        int n = chars.length();
    //        long m = 0;
    //        for (int i = 0; i < n; i++) {
    //            char c = chars.charAt(i);
    //            if (c < 64)
    //                m |= (1L << c);
    //        }
    //        return m;
    //    }

    // Compute the high-order mask for the characters in the given string
    //    private static long highMask(String chars) {
    //        int n = chars.length();
    //        long m = 0;
    //        for (int i = 0; i < n; i++) {
    //            char c = chars.charAt(i);
    //            if ((c >= 64) && (c < 128))
    //                m |= (1L << (c - 64));
    //        }
    //        return m;
    //    }

    // Compute a low-order mask for the characters
    // between first and last, inclusive
    //    private static long lowMask(char first, char last) {
    //        long m = 0;
    //        int f = Math.max(Math.min(first, 63), 0);
    //        int l = Math.max(Math.min(last, 63), 0);
    //        for (int i = f; i <= l; i++)
    //            m |= 1L << i;
    //        return m;
    //    }

    // Compute a high-order mask for the characters
    // between first and last, inclusive
    //    private static long highMask(char first, char last) {
    //        long m = 0;
    //        int f = Math.max(Math.min(first, 127), 64) - 64;
    //        int l = Math.max(Math.min(last, 127), 64) - 64;
    //        for (int i = f; i <= l; i++)
    //            m |= 1L << i;
    //        return m;
    //    }

    // Tell whether the given character is permitted by the given mask pair
    private static boolean match(char c, long lowMask, long highMask) {
        if (c == 0) // 0 doesn't have a slot in the mask. So, it never matches.
            return false;
        if (c < 64)
            return ((1L << c) & lowMask) != 0;
        if (c < 128)
            return ((1L << (c - 64)) & highMask) != 0;
        return false;
    }

    // Character-class masks, in reverse order from RFC2396 because
    // initializers for static fields cannot make forward references.

    // digit    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
    //            "8" | "9"
    private static final long L_DIGIT = 0x3FF000000000000L; // lowMask('0', '9');
    private static final long H_DIGIT = 0L;

    // upalpha  = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" |
    //            "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" |
    //            "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
    private static final long L_UPALPHA = 0L;
    private static final long H_UPALPHA = 0x7FFFFFEL; // highMask('A', 'Z');

    // lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" |
    //            "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" |
    //            "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
    private static final long L_LOWALPHA = 0L;
    private static final long H_LOWALPHA = 0x7FFFFFE00000000L; // highMask('a', 'z');

    // alpha         = lowalpha | upalpha
    private static final long L_ALPHA = L_LOWALPHA | L_UPALPHA;
    private static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;

    // alphanum      = alpha | digit
    private static final long L_ALPHANUM = L_DIGIT | L_ALPHA;
    private static final long H_ALPHANUM = H_DIGIT | H_ALPHA;

    // hex           = digit | "A" | "B" | "C" | "D" | "E" | "F" |
    //                         "a" | "b" | "c" | "d" | "e" | "f"
    private static final long L_HEX = L_DIGIT;
    private static final long H_HEX = 0x7E0000007EL; // highMask('A', 'F') | highMask('a', 'f');

    // mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
    //                 "(" | ")"
    private static final long L_MARK = 0x678200000000L; // lowMask("-_.!~*'()");
    private static final long H_MARK = 0x4000000080000000L; // highMask("-_.!~*'()");

    // unreserved    = alphanum | mark
    private static final long L_UNRESERVED = L_ALPHANUM | L_MARK;
    private static final long H_UNRESERVED = H_ALPHANUM | H_MARK;

    // reserved      = ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" |
    //                 "$" | "," | "[" | "]"
    // Added per RFC2732: "[", "]"
    private static final long L_RESERVED = 0xAC00985000000000L; // lowMask(";/?:@&=+$,[]");
    private static final long H_RESERVED = 0x28000001L; // highMask(";/?:@&=+$,[]");

    // The zero'th bit is used to indicate that escape pairs and non-US-ASCII
    // characters are allowed; this is handled by the scanEscape method below.
    private static final long L_ESCAPED = 1L;
    private static final long H_ESCAPED = 0L;

    // uric          = reserved | unreserved | escaped
    private static final long L_URIC = L_RESERVED | L_UNRESERVED | L_ESCAPED;
    private static final long H_URIC = H_RESERVED | H_UNRESERVED | H_ESCAPED;

    // pchar         = unreserved | escaped |
    //                 ":" | "@" | "&" | "=" | "+" | "$" | ","
    private static final long L_PCHAR
        = L_UNRESERVED | L_ESCAPED | 0x2400185000000000L; // lowMask(":@&=+$,");
    private static final long H_PCHAR
        = H_UNRESERVED | H_ESCAPED | 0x1L; // highMask(":@&=+$,");

    // All valid path characters
    private static final long L_PATH = L_PCHAR | 0x800800000000000L; // lowMask(";/");
    private static final long H_PATH = H_PCHAR; // highMask(";/") == 0x0L;

    // Dash, for use in domainlabel and toplabel
    private static final long L_DASH = 0x200000000000L; // lowMask("-");
    private static final long H_DASH = 0x0L; // highMask("-");

    // Dot, for use in hostnames
    private static final long L_DOT = 0x400000000000L; // lowMask(".");
    private static final long H_DOT = 0x0L; // highMask(".");

    // userinfo      = *( unreserved | escaped |
    //                    ";" | ":" | "&" | "=" | "+" | "$" | "," )
    private static final long L_USERINFO
        = L_UNRESERVED | L_ESCAPED | 0x2C00185000000000L; // lowMask(";:&=+$,");
    private static final long H_USERINFO
        = H_UNRESERVED | H_ESCAPED; // | highMask(";:&=+$,") == 0L;

    // reg_name      = 1*( unreserved | escaped | "$" | "," |
    //                     ";" | ":" | "@" | "&" | "=" | "+" )
    private static final long L_REG_NAME
        = L_UNRESERVED | L_ESCAPED | 0x2C00185000000000L; // lowMask("$,;:@&=+");
    private static final long H_REG_NAME
        = H_UNRESERVED | H_ESCAPED | 0x1L; // highMask("$,;:@&=+");

    // All valid characters for server-based authorities
    private static final long L_SERVER
        = L_USERINFO | L_ALPHANUM | L_DASH | 0x400400000000000L; // lowMask(".:@[]");
    private static final long H_SERVER
        = H_USERINFO | H_ALPHANUM | H_DASH | 0x28000001L; // highMask(".:@[]");

    // Special case of server authority that represents an IPv6 address
    // In this case, a % does not signify an escape sequence
    private static final long L_SERVER_PERCENT
        = L_SERVER | 0x2000000000L; // lowMask("%");
    private static final long H_SERVER_PERCENT
        = H_SERVER; // | highMask("%") == 0L;

    // scheme        = alpha *( alpha | digit | "+" | "-" | "." )
    private static final long L_SCHEME = L_ALPHA | L_DIGIT | 0x680000000000L; // lowMask("+-.");
    private static final long H_SCHEME = H_ALPHA | H_DIGIT; // | highMask("+-.") == 0L

    // scope_id = alpha | digit | "_" | "."
    private static final long L_SCOPE_ID
        = L_ALPHANUM | 0x400000000000L; // lowMask("_.");
    private static final long H_SCOPE_ID
        = H_ALPHANUM | 0x80000000L; // highMask("_.");

    // -- Escaping and encoding --

    private static final char[] hexDigits = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static void appendEscape(StringBuilder sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[(b >> 0) & 0x0f]);
    }

    private static void appendEncoded(CharsetEncoder encoder, StringBuilder sb, char c) {
        ByteBuffer bb = null;
        try {
            bb = encoder.encode(CharBuffer.wrap("" + c));
        } catch (CharacterCodingException x) {
            assert false;
        }
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80)
                appendEscape(sb, (byte)b);
            else
                sb.append((char)b);
        }
    }

    // Quote any characters in s that are not permitted
    // by the given mask pair
    //
    private static String quote(String s, long lowMask, long highMask) {
        StringBuilder sb = null;
        CharsetEncoder encoder = null;
        boolean allowNonASCII = ((lowMask & L_ESCAPED) != 0);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '\u0080') {
                if (!match(c, lowMask, highMask)) {
                    if (sb == null) {
                        sb = new StringBuilder();
                        sb.append(s, 0, i);
                    }
                    appendEscape(sb, (byte)c);
                } else {
                    if (sb != null)
                        sb.append(c);
                }
            } else if (allowNonASCII
                       && (Character.isSpaceChar(c)
                           || Character.isISOControl(c))) {
                if (encoder == null)
                    encoder = UTF_8.INSTANCE.newEncoder();
                if (sb == null) {
                    sb = new StringBuilder();
                    sb.append(s, 0, i);
                }
                appendEncoded(encoder, sb, c);
            } else {
                if (sb != null)
                    sb.append(c);
            }
        }
        return (sb == null) ? s : sb.toString();
    }

    // Encodes all characters >= \u0080 into escaped, normalized UTF-8 octets,
    // assuming that s is otherwise legal
    //
    private static String encode(String s) {
        int n = s.length();
        if (n == 0)
            return s;

        // First check whether we actually need to encode
        for (int i = 0;;) {
            if (s.charAt(i) >= '\u0080')
                break;
            if (++i >= n)
                return s;
        }

        String ns = Normalizer.normalize(s, Normalizer.Form.NFC);
        ByteBuffer bb = null;
        try {
            bb = UTF_8.INSTANCE.newEncoder()
                .encode(CharBuffer.wrap(ns));

        } catch (CharacterCodingException x) {
            assert false;
        }

        StringBuilder sb = new StringBuilder();
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80)
                appendEscape(sb, (byte)b);
            else
                sb.append((char)b);
        }
        return sb.toString();
    }

    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        assert false;
        return -1;
    }

    private static byte decode(char c1, char c2) {
        return (byte)(  ((decode(c1) & 0xf) << 4)
                      | ((decode(c2) & 0xf) << 0));
    }

    // Evaluates all escapes in s, applying UTF-8 decoding if needed.  Assumes
    // that escapes are well-formed syntactically, i.e., of the form %XX.  If a
    // sequence of escaped octets is not valid UTF-8 then the erroneous octets
    // are replaced with '\uFFFD'.
    // Exception: any "%" found between "[]" is left alone. It is an IPv6 literal
    //            with a scope_id
    //
    private static String decode(String s) {
        return decode(s, true);
    }

    // This method was introduced as a generalization of URI.decode method
    // to provide a fix for JDK-8037396
    private static String decode(String s, boolean ignorePercentInBrackets) {
        if (s == null)
            return s;
        int n = s.length();
        if (n == 0)
            return s;
        if (s.indexOf('%') < 0)
            return s;

        StringBuilder sb = new StringBuilder(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = UTF_8.INSTANCE.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);

        // This is not horribly efficient, but it will do for now
        char c = s.charAt(0);
        boolean betweenBrackets = false;

        for (int i = 0; i < n;) {
            assert c == s.charAt(i);    // Loop invariant
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }
            if (c != '%' || (betweenBrackets && ignorePercentInBrackets)) {
                sb.append(c);
                if (++i >= n)
                    break;
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            int ui = i;
            for (;;) {
                assert (n - i >= 2);
                bb.put(decode(s.charAt(++i), s.charAt(++i)));
                if (++i >= n)
                    break;
                c = s.charAt(i);
                if (c != '%')
                    break;
            }
            bb.flip();
            cb.clear();
            dec.reset();
            CoderResult cr = dec.decode(bb, cb, true);
            assert cr.isUnderflow();
            cr = dec.flush(cb);
            assert cr.isUnderflow();
            sb.append(cb.flip().toString());
        }

        return sb.toString();
    }


    // -- Parsing --

    // For convenience we wrap the input URI string in a new instance of the
    // following internal class.  This saves always having to pass the input
    // string as an argument to each internal scan/parse method.

    private class Parser {

        private String input;           // URI input string
        private boolean requireServerAuthority = false;

        Parser(String s) {
            input = s;
            string = s;
        }

        // -- Methods for throwing URISyntaxException in various ways --

        private void fail(String reason) throws URISyntaxException {
            throw new URISyntaxException(input, reason);
        }

        private void fail(String reason, int p) throws URISyntaxException {
            throw new URISyntaxException(input, reason, p);
        }

        private void failExpecting(String expected, int p)
            throws URISyntaxException
        {
            fail("Expected " + expected, p);
        }


        // -- Simple access to the input string --

        // Tells whether start < end and, if so, whether charAt(start) == c
        //
        private boolean at(int start, int end, char c) {
            return (start < end) && (input.charAt(start) == c);
        }

        // Tells whether start + s.length() < end and, if so,
        // whether the chars at the start position match s exactly
        //
        private boolean at(int start, int end, String s) {
            int p = start;
            int sn = s.length();
            if (sn > end - p)
                return false;
            int i = 0;
            while (i < sn) {
                if (input.charAt(p++) != s.charAt(i)) {
                    break;
                }
                i++;
            }
            return (i == sn);
        }


        // -- Scanning --

        // The various scan and parse methods that follow use a uniform
        // convention of taking the current start position and end index as
        // their first two arguments.  The start is inclusive while the end is
        // exclusive, just as in the String class, i.e., a start/end pair
        // denotes the left-open interval [start, end) of the input string.
        //
        // These methods never proceed past the end position.  They may return
        // -1 to indicate outright failure, but more often they simply return
        // the position of the first char after the last char scanned.  Thus
        // a typical idiom is
        //
        //     int p = start;
        //     int q = scan(p, end, ...);
        //     if (q > p)
        //         // We scanned something
        //         ...;
        //     else if (q == p)
        //         // We scanned nothing
        //         ...;
        //     else if (q == -1)
        //         // Something went wrong
        //         ...;


        // Scan a specific char: If the char at the given start position is
        // equal to c, return the index of the next char; otherwise, return the
        // start position.
        //
        private int scan(int start, int end, char c) {
            if ((start < end) && (input.charAt(start) == c))
                return start + 1;
            return start;
        }

        // Scan forward from the given start position.  Stop at the first char
        // in the err string (in which case -1 is returned), or the first char
        // in the stop string (in which case the index of the preceding char is
        // returned), or the end of the input string (in which case the length
        // of the input string is returned).  May return the start position if
        // nothing matches.
        //
        private int scan(int start, int end, String err, String stop) {
            int p = start;
            while (p < end) {
                char c = input.charAt(p);
                if (err.indexOf(c) >= 0)
                    return -1;
                if (stop.indexOf(c) >= 0)
                    break;
                p++;
            }
            return p;
        }

        // Scan forward from the given start position.  Stop at the first char
        // in the stop string (in which case the index of the preceding char is
        // returned), or the end of the input string (in which case the length
        // of the input string is returned).  May return the start position if
        // nothing matches.
        //
        private int scan(int start, int end, String stop) {
            int p = start;
            while (p < end) {
                char c = input.charAt(p);
                if (stop.indexOf(c) >= 0)
                    break;
                p++;
            }
            return p;
        }

        // Scan a potential escape sequence, starting at the given position,
        // with the given first char (i.e., charAt(start) == c).
        //
        // This method assumes that if escapes are allowed then visible
        // non-US-ASCII chars are also allowed.
        //
        private int scanEscape(int start, int n, char first)
            throws URISyntaxException
        {
            int p = start;
            char c = first;
            if (c == '%') {
                // Process escape pair
                if ((p + 3 <= n)
                    && match(input.charAt(p + 1), L_HEX, H_HEX)
                    && match(input.charAt(p + 2), L_HEX, H_HEX)) {
                    return p + 3;
                }
                fail("Malformed escape pair", p);
            } else if ((c > 128)
                       && !Character.isSpaceChar(c)
                       && !Character.isISOControl(c)) {
                // Allow unescaped but visible non-US-ASCII chars
                return p + 1;
            }
            return p;
        }

        // Scan chars that match the given mask pair
        //
        private int scan(int start, int n, long lowMask, long highMask)
            throws URISyntaxException
        {
            int p = start;
            while (p < n) {
                char c = input.charAt(p);
                if (match(c, lowMask, highMask)) {
                    p++;
                    continue;
                }
                if ((lowMask & L_ESCAPED) != 0) {
                    int q = scanEscape(p, n, c);
                    if (q > p) {
                        p = q;
                        continue;
                    }
                }
                break;
            }
            return p;
        }

        // Check that each of the chars in [start, end) matches the given mask
        //
        private void checkChars(int start, int end,
                                long lowMask, long highMask,
                                String what)
            throws URISyntaxException
        {
            int p = scan(start, end, lowMask, highMask);
            if (p < end)
                fail("Illegal character in " + what, p);
        }

        // Check that the char at position p matches the given mask
        //
        private void checkChar(int p,
                               long lowMask, long highMask,
                               String what)
            throws URISyntaxException
        {
            checkChars(p, p + 1, lowMask, highMask, what);
        }


        // -- Parsing --

        // [<scheme>:]<scheme-specific-part>[#<fragment>]
        //
        void parse(boolean rsa) throws URISyntaxException {
            requireServerAuthority = rsa;
            int n = input.length();
            int p = scan(0, n, "/?#", ":");
            if ((p >= 0) && at(p, n, ':')) {
                if (p == 0)
                    failExpecting("scheme name", 0);
                checkChar(0, L_ALPHA, H_ALPHA, "scheme name");
                checkChars(1, p, L_SCHEME, H_SCHEME, "scheme name");
                scheme = input.substring(0, p);
                p++;                    // Skip ':'
                if (at(p, n, '/')) {
                    p = parseHierarchical(p, n);
                } else {
                    // opaque; need to create the schemeSpecificPart
                    int q = scan(p, n, "#");
                    if (q <= p)
                        failExpecting("scheme-specific part", p);
                    checkChars(p, q, L_URIC, H_URIC, "opaque part");
                    schemeSpecificPart = input.substring(p, q);
                    p = q;
                }
            } else {
                p = parseHierarchical(0, n);
            }
            if (at(p, n, '#')) {
                checkChars(p + 1, n, L_URIC, H_URIC, "fragment");
                fragment = input.substring(p + 1, n);
                p = n;
            }
            if (p < n)
                fail("end of URI", p);
        }

        // [//authority]<path>[?<query>]
        //
        // DEVIATION from RFC2396: We allow an empty authority component as
        // long as it's followed by a non-empty path, query component, or
        // fragment component.  This is so that URIs such as "file:///foo/bar"
        // will parse.  This seems to be the intent of RFC2396, though the
        // grammar does not permit it.  If the authority is empty then the
        // userInfo, host, and port components are undefined.
        //
        // DEVIATION from RFC2396: We allow empty relative paths.  This seems
        // to be the intent of RFC2396, but the grammar does not permit it.
        // The primary consequence of this deviation is that "#f" parses as a
        // relative URI with an empty path.
        //
        private int parseHierarchical(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            if (at(p, n, '/') && at(p + 1, n, '/')) {
                p += 2;
                int q = scan(p, n, "/?#");
                if (q > p) {
                    p = parseAuthority(p, q);
                } else if (q < n) {
                    // DEVIATION: Allow empty authority prior to non-empty
                    // path, query component or fragment identifier
                } else
                    failExpecting("authority", p);
            }
            int q = scan(p, n, "?#"); // DEVIATION: May be empty
            checkChars(p, q, L_PATH, H_PATH, "path");
            path = input.substring(p, q);
            p = q;
            if (at(p, n, '?')) {
                p++;
                q = scan(p, n, "#");
                checkChars(p, q, L_URIC, H_URIC, "query");
                query = input.substring(p, q);
                p = q;
            }
            return p;
        }

        // authority     = server | reg_name
        //
        // Ambiguity: An authority that is a registry name rather than a server
        // might have a prefix that parses as a server.  We use the fact that
        // the authority component is always followed by '/' or the end of the
        // input string to resolve this: If the complete authority did not
        // parse as a server then we try to parse it as a registry name.
        //
        private int parseAuthority(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q = p;
            URISyntaxException ex = null;

            boolean serverChars;
            boolean regChars;

            if (scan(p, n, "]") > p) {
                // contains a literal IPv6 address, therefore % is allowed
                serverChars = (scan(p, n, L_SERVER_PERCENT, H_SERVER_PERCENT) == n);
            } else {
                serverChars = (scan(p, n, L_SERVER, H_SERVER) == n);
            }
            regChars = (scan(p, n, L_REG_NAME, H_REG_NAME) == n);

            if (regChars && !serverChars) {
                // Must be a registry-based authority
                authority = input.substring(p, n);
                return n;
            }

            if (serverChars) {
                // Might be (probably is) a server-based authority, so attempt
                // to parse it as such.  If the attempt fails, try to treat it
                // as a registry-based authority.
                try {
                    q = parseServer(p, n);
                    if (q < n)
                        failExpecting("end of authority", q);
                    authority = input.substring(p, n);
                } catch (URISyntaxException x) {
                    // Undo results of failed parse
                    userInfo = null;
                    host = null;
                    port = -1;
                    if (requireServerAuthority) {
                        // If we're insisting upon a server-based authority,
                        // then just re-throw the exception
                        throw x;
                    } else {
                        // Save the exception in case it doesn't parse as a
                        // registry either
                        ex = x;
                        q = p;
                    }
                }
            }

            if (q < n) {
                if (regChars) {
                    // Registry-based authority
                    authority = input.substring(p, n);
                } else if (ex != null) {
                    // Re-throw exception; it was probably due to
                    // a malformed IPv6 address
                    throw ex;
                } else {
                    fail("Illegal character in authority", q);
                }
            }

            return n;
        }


        // [<userinfo>@]<host>[:<port>]
        //
        private int parseServer(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q;

            // userinfo
            q = scan(p, n, "/?#", "@");
            if ((q >= p) && at(q, n, '@')) {
                checkChars(p, q, L_USERINFO, H_USERINFO, "user info");
                userInfo = input.substring(p, q);
                p = q + 1;              // Skip '@'
            }

            // hostname, IPv4 address, or IPv6 address
            if (at(p, n, '[')) {
                // DEVIATION from RFC2396: Support IPv6 addresses, per RFC2732
                p++;
                q = scan(p, n, "/?#", "]");
                if ((q > p) && at(q, n, ']')) {
                    // look for a "%" scope id
                    int r = scan (p, q, "%");
                    if (r > p) {
                        parseIPv6Reference(p, r);
                        if (r+1 == q) {
                            fail ("scope id expected");
                        }
                        checkChars (r+1, q, L_SCOPE_ID, H_SCOPE_ID,
                                                "scope id");
                    } else {
                        parseIPv6Reference(p, q);
                    }
                    host = input.substring(p-1, q+1);
                    p = q + 1;
                } else {
                    failExpecting("closing bracket for IPv6 address", q);
                }
            } else {
                q = parseIPv4Address(p, n);
                if (q <= p)
                    q = parseHostname(p, n);
                p = q;
            }

            // port
            if (at(p, n, ':')) {
                p++;
                q = scan(p, n, "/");
                if (q > p) {
                    checkChars(p, q, L_DIGIT, H_DIGIT, "port number");
                    try {
                        port = Integer.parseInt(input, p, q, 10);
                    } catch (NumberFormatException x) {
                        fail("Malformed port number", p);
                    }
                    p = q;
                }
            }
            if (p < n)
                failExpecting("port number", p);

            return p;
        }

        // Scan a string of decimal digits whose value fits in a byte
        //
        private int scanByte(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q = scan(p, n, L_DIGIT, H_DIGIT);
            if (q <= p) return q;
            if (Integer.parseInt(input, p, q, 10) > 255) return p;
            return q;
        }

        // Scan an IPv4 address.
        //
        // If the strict argument is true then we require that the given
        // interval contain nothing besides an IPv4 address; if it is false
        // then we only require that it start with an IPv4 address.
        //
        // If the interval does not contain or start with (depending upon the
        // strict argument) a legal IPv4 address characters then we return -1
        // immediately; otherwise we insist that these characters parse as a
        // legal IPv4 address and throw an exception on failure.
        //
        // We assume that any string of decimal digits and dots must be an IPv4
        // address.  It won't parse as a hostname anyway, so making that
        // assumption here allows more meaningful exceptions to be thrown.
        //
        private int scanIPv4Address(int start, int n, boolean strict)
            throws URISyntaxException
        {
            int p = start;
            int q;
            int m = scan(p, n, L_DIGIT | L_DOT, H_DIGIT | H_DOT);
            if ((m <= p) || (strict && (m != n)))
                return -1;
            for (;;) {
                // Per RFC2732: At most three digits per byte
                // Further constraint: Each element fits in a byte
                if ((q = scanByte(p, m)) <= p) break;   p = q;
                if ((q = scan(p, m, '.')) <= p) break;  p = q;
                if ((q = scanByte(p, m)) <= p) break;   p = q;
                if ((q = scan(p, m, '.')) <= p) break;  p = q;
                if ((q = scanByte(p, m)) <= p) break;   p = q;
                if ((q = scan(p, m, '.')) <= p) break;  p = q;
                if ((q = scanByte(p, m)) <= p) break;   p = q;
                if (q < m) break;
                return q;
            }
            fail("Malformed IPv4 address", q);
            return -1;
        }

        // Take an IPv4 address: Throw an exception if the given interval
        // contains anything except an IPv4 address
        //
        private int takeIPv4Address(int start, int n, String expected)
            throws URISyntaxException
        {
            int p = scanIPv4Address(start, n, true);
            if (p <= start)
                failExpecting(expected, start);
            return p;
        }

        // Attempt to parse an IPv4 address, returning -1 on failure but
        // allowing the given interval to contain [:<characters>] after
        // the IPv4 address.
        //
        private int parseIPv4Address(int start, int n) {
            int p;

            try {
                p = scanIPv4Address(start, n, false);
            } catch (URISyntaxException x) {
                return -1;
            } catch (NumberFormatException nfe) {
                return -1;
            }

            if (p > start && p < n) {
                // IPv4 address is followed by something - check that
                // it's a ":" as this is the only valid character to
                // follow an address.
                if (input.charAt(p) != ':') {
                    p = -1;
                }
            }

            if (p > start)
                host = input.substring(start, p);

            return p;
        }

        // hostname      = domainlabel [ "." ] | 1*( domainlabel "." ) toplabel [ "." ]
        // domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
        // toplabel      = alpha | alpha *( alphanum | "-" ) alphanum
        //
        private int parseHostname(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q;
            int l = -1;                 // Start of last parsed label

            do {
                // domainlabel = alphanum [ *( alphanum | "-" ) alphanum ]
                q = scan(p, n, L_ALPHANUM, H_ALPHANUM);
                if (q <= p)
                    break;
                l = p;
                if (q > p) {
                    p = q;
                    q = scan(p, n, L_ALPHANUM | L_DASH, H_ALPHANUM | H_DASH);
                    if (q > p) {
                        if (input.charAt(q - 1) == '-')
                            fail("Illegal character in hostname", q - 1);
                        p = q;
                    }
                }
                q = scan(p, n, '.');
                if (q <= p)
                    break;
                p = q;
            } while (p < n);

            if ((p < n) && !at(p, n, ':'))
                fail("Illegal character in hostname", p);

            if (l < 0)
                failExpecting("hostname", start);

            // for a fully qualified hostname check that the rightmost
            // label starts with an alpha character.
            if (l > start && !match(input.charAt(l), L_ALPHA, H_ALPHA)) {
                fail("Illegal character in hostname", l);
            }

            host = input.substring(start, p);
            return p;
        }


        // IPv6 address parsing, from RFC2373: IPv6 Addressing Architecture
        //
        // Bug: The grammar in RFC2373 Appendix B does not allow addresses of
        // the form ::12.34.56.78, which are clearly shown in the examples
        // earlier in the document.  Here is the original grammar:
        //
        //   IPv6address = hexpart [ ":" IPv4address ]
        //   hexpart     = hexseq | hexseq "::" [ hexseq ] | "::" [ hexseq ]
        //   hexseq      = hex4 *( ":" hex4)
        //   hex4        = 1*4HEXDIG
        //
        // We therefore use the following revised grammar:
        //
        //   IPv6address = hexseq [ ":" IPv4address ]
        //                 | hexseq [ "::" [ hexpost ] ]
        //                 | "::" [ hexpost ]
        //   hexpost     = hexseq | hexseq ":" IPv4address | IPv4address
        //   hexseq      = hex4 *( ":" hex4)
        //   hex4        = 1*4HEXDIG
        //
        // This covers all and only the following cases:
        //
        //   hexseq
        //   hexseq : IPv4address
        //   hexseq ::
        //   hexseq :: hexseq
        //   hexseq :: hexseq : IPv4address
        //   hexseq :: IPv4address
        //   :: hexseq
        //   :: hexseq : IPv4address
        //   :: IPv4address
        //   ::
        //
        // Additionally we constrain the IPv6 address as follows :-
        //
        //  i.  IPv6 addresses without compressed zeros should contain
        //      exactly 16 bytes.
        //
        //  ii. IPv6 addresses with compressed zeros should contain
        //      less than 16 bytes.

        private int ipv6byteCount = 0;

        private int parseIPv6Reference(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q;
            boolean compressedZeros = false;

            q = scanHexSeq(p, n);

            if (q > p) {
                p = q;
                if (at(p, n, "::")) {
                    compressedZeros = true;
                    p = scanHexPost(p + 2, n);
                } else if (at(p, n, ':')) {
                    p = takeIPv4Address(p + 1,  n, "IPv4 address");
                    ipv6byteCount += 4;
                }
            } else if (at(p, n, "::")) {
                compressedZeros = true;
                p = scanHexPost(p + 2, n);
            }
            if (p < n)
                fail("Malformed IPv6 address", start);
            if (ipv6byteCount > 16)
                fail("IPv6 address too long", start);
            if (!compressedZeros && ipv6byteCount < 16)
                fail("IPv6 address too short", start);
            if (compressedZeros && ipv6byteCount == 16)
                fail("Malformed IPv6 address", start);

            return p;
        }

        private int scanHexPost(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q;

            if (p == n)
                return p;

            q = scanHexSeq(p, n);
            if (q > p) {
                p = q;
                if (at(p, n, ':')) {
                    p++;
                    p = takeIPv4Address(p, n, "hex digits or IPv4 address");
                    ipv6byteCount += 4;
                }
            } else {
                p = takeIPv4Address(p, n, "hex digits or IPv4 address");
                ipv6byteCount += 4;
            }
            return p;
        }

        // Scan a hex sequence; return -1 if one could not be scanned
        //
        private int scanHexSeq(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q;

            q = scan(p, n, L_HEX, H_HEX);
            if (q <= p)
                return -1;
            if (at(q, n, '.'))          // Beginning of IPv4 address
                return -1;
            if (q > p + 4)
                fail("IPv6 hexadecimal digit sequence too long", p);
            ipv6byteCount += 2;
            p = q;
            while (p < n) {
                if (!at(p, n, ':'))
                    break;
                if (at(p + 1, n, ':'))
                    break;              // "::"
                p++;
                q = scan(p, n, L_HEX, H_HEX);
                if (q <= p)
                    failExpecting("digits for an IPv6 address", p);
                if (at(q, n, '.')) {    // Beginning of IPv4 address
                    p--;
                    break;
                }
                if (q > p + 4)
                    fail("IPv6 hexadecimal digit sequence too long", p);
                ipv6byteCount += 2;
                p = q;
            }

            return p;
        }

    }
    static {
        SharedSecrets.setJavaNetUriAccess(
            new JavaNetUriAccess() {
                public URI create(String scheme, String path) {
                    return new URI(scheme, path);
                }
            }
        );
    }
}
