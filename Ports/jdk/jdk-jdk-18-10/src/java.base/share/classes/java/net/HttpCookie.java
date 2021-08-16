/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import jdk.internal.access.JavaNetHttpCookieAccess;
import jdk.internal.access.SharedSecrets;

/**
 * An HttpCookie object represents an HTTP cookie, which carries state
 * information between server and user agent. Cookie is widely adopted
 * to create stateful sessions.
 *
 * <p> There are 3 HTTP cookie specifications:
 * <blockquote>
 *   Netscape draft<br>
 *   RFC 2109 - <a href="http://www.ietf.org/rfc/rfc2109.txt">
 * <i>http://www.ietf.org/rfc/rfc2109.txt</i></a><br>
 *   RFC 2965 - <a href="http://www.ietf.org/rfc/rfc2965.txt">
 * <i>http://www.ietf.org/rfc/rfc2965.txt</i></a>
 * </blockquote>
 *
 * <p> HttpCookie class can accept all these 3 forms of syntax.
 *
 * @author Edward Wang
 * @since 1.6
 */
public final class HttpCookie implements Cloneable {
    // ---------------- Fields --------------

    // The value of the cookie itself.
    private final String name;  // NAME= ... "$Name" style is reserved
    private String value;       // value of NAME

    // Attributes encoded in the header's cookie fields.
    private String comment;     // Comment=VALUE ... describes cookie's use
    private String commentURL;  // CommentURL="http URL" ... describes cookie's use
    private boolean toDiscard;  // Discard ... discard cookie unconditionally
    private String domain;      // Domain=VALUE ... domain that sees cookie
    private long maxAge = MAX_AGE_UNSPECIFIED;  // Max-Age=VALUE ... cookies auto-expire
    private String path;        // Path=VALUE ... URLs that see the cookie
    private String portlist;    // Port[="portlist"] ... the port cookie may be returned to
    private boolean secure;     // Secure ... e.g. use SSL
    private boolean httpOnly;   // HttpOnly ... i.e. not accessible to scripts
    private int version = 1;    // Version=1 ... RFC 2965 style

    // The original header this cookie was constructed from, if it was
    // constructed by parsing a header, otherwise null.
    private final String header;

    // Hold the creation time (in seconds) of the http cookie for later
    // expiration calculation
    private final long whenCreated;

    // Since the positive and zero max-age have their meanings,
    // this value serves as a hint as 'not specify max-age'
    private static final long MAX_AGE_UNSPECIFIED = -1;

    // date formats used by Netscape's cookie draft
    // as well as formats seen on various sites
    private static final String[] COOKIE_DATE_FORMATS = {
        "EEE',' dd-MMM-yyyy HH:mm:ss 'GMT'",
        "EEE',' dd MMM yyyy HH:mm:ss 'GMT'",
        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z",
        "EEE',' dd-MMM-yy HH:mm:ss 'GMT'",
        "EEE',' dd MMM yy HH:mm:ss 'GMT'",
        "EEE MMM dd yy HH:mm:ss 'GMT'Z"
    };

    // constant strings represent set-cookie header token
    private static final String SET_COOKIE = "set-cookie:";
    private static final String SET_COOKIE2 = "set-cookie2:";

    // ---------------- Ctors --------------

    /**
     * Constructs a cookie with a specified name and value.
     *
     * <p> The name must conform to RFC 2965. That means it can contain
     * only ASCII alphanumeric characters and cannot contain commas,
     * semicolons, or white space or begin with a $ character. The cookie's
     * name cannot be changed after creation.
     *
     * <p> The value can be anything the server chooses to send. Its
     * value is probably of interest only to the server. The cookie's
     * value can be changed after creation with the
     * {@code setValue} method.
     *
     * <p> By default, cookies are created according to the RFC 2965
     * cookie specification. The version can be changed with the
     * {@code setVersion} method.
     *
     *
     * @param  name
     *         a {@code String} specifying the name of the cookie
     *
     * @param  value
     *         a {@code String} specifying the value of the cookie
     *
     * @throws  IllegalArgumentException
     *          if the cookie name contains illegal characters
     * @throws  NullPointerException
     *          if {@code name} is {@code null}
     *
     * @see #setValue
     * @see #setVersion
     */
    public HttpCookie(String name, String value) {
        this(name, value, null /*header*/);
    }

    private HttpCookie(String name, String value, String header) {
        this(name, value, header, System.currentTimeMillis());
    }

    /**
     * Package private for testing purposes.
     */
    HttpCookie(String name, String value, String header, long creationTime) {
        name = name.trim();
        if (name.isEmpty() || !isToken(name) || name.charAt(0) == '$') {
            throw new IllegalArgumentException("Illegal cookie name");
        }

        this.name = name;
        this.value = value;
        toDiscard = false;
        secure = false;

        whenCreated = creationTime;
        portlist = null;
        this.header = header;
    }

    /**
     * Constructs cookies from set-cookie or set-cookie2 header string.
     * RFC 2965 section 3.2.2 set-cookie2 syntax indicates that one header line
     * may contain more than one cookie definitions, so this is a static
     * utility method instead of another constructor.
     *
     * @param  header
     *         a {@code String} specifying the set-cookie header. The header
     *         should start with "set-cookie", or "set-cookie2" token; or it
     *         should have no leading token at all.
     *
     * @return  a List of cookie parsed from header line string
     *
     * @throws  IllegalArgumentException
     *          if header string violates the cookie specification's syntax or
     *          the cookie name contains illegal characters.
     * @throws  NullPointerException
     *          if the header string is {@code null}
     */
    public static List<HttpCookie> parse(String header) {
        return parse(header, false);
    }

    // Private version of parse() that will store the original header used to
    // create the cookie, in the cookie itself. This can be useful for filtering
    // Set-Cookie[2] headers, using the internal parsing logic defined in this
    // class.
    private static List<HttpCookie> parse(String header, boolean retainHeader) {

        int version = guessCookieVersion(header);

        // if header start with set-cookie or set-cookie2, strip it off
        if (startsWithIgnoreCase(header, SET_COOKIE2)) {
            header = header.substring(SET_COOKIE2.length());
        } else if (startsWithIgnoreCase(header, SET_COOKIE)) {
            header = header.substring(SET_COOKIE.length());
        }

        List<HttpCookie> cookies = new java.util.ArrayList<>();
        // The Netscape cookie may have a comma in its expires attribute, while
        // the comma is the delimiter in rfc 2965/2109 cookie header string.
        // so the parse logic is slightly different
        if (version == 0) {
            // Netscape draft cookie
            HttpCookie cookie = parseInternal(header, retainHeader);
            cookie.setVersion(0);
            cookies.add(cookie);
        } else {
            // rfc2965/2109 cookie
            // if header string contains more than one cookie,
            // it'll separate them with comma
            List<String> cookieStrings = splitMultiCookies(header);
            for (String cookieStr : cookieStrings) {
                HttpCookie cookie = parseInternal(cookieStr, retainHeader);
                cookie.setVersion(1);
                cookies.add(cookie);
            }
        }

        return cookies;
    }

    // ---------------- Public operations --------------

    /**
     * Reports whether this HTTP cookie has expired or not.
     *
     * @return  {@code true} to indicate this HTTP cookie has expired;
     *          otherwise, {@code false}
     */
    public boolean hasExpired() {
        if (maxAge == 0) return true;

        // if not specify max-age, this cookie should be
        // discarded when user agent is to be closed, but
        // it is not expired.
        if (maxAge < 0) return false;

        long deltaSecond = (System.currentTimeMillis() - whenCreated) / 1000;
        if (deltaSecond > maxAge)
            return true;
        else
            return false;
    }

    /**
     * Specifies a comment that describes a cookie's purpose.
     * The comment is useful if the browser presents the cookie
     * to the user. Comments are not supported by Netscape Version 0 cookies.
     *
     * @param  purpose
     *         a {@code String} specifying the comment to display to the user
     *
     * @see  #getComment
     */
    public void setComment(String purpose) {
        comment = purpose;
    }

    /**
     * Returns the comment describing the purpose of this cookie, or
     * {@code null} if the cookie has no comment.
     *
     * @return  a {@code String} containing the comment, or {@code null} if none
     *
     * @see  #setComment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Specifies a comment URL that describes a cookie's purpose.
     * The comment URL is useful if the browser presents the cookie
     * to the user. Comment URL is RFC 2965 only.
     *
     * @param  purpose
     *         a {@code String} specifying the comment URL to display to the user
     *
     * @see  #getCommentURL
     */
    public void setCommentURL(String purpose) {
        commentURL = purpose;
    }

    /**
     * Returns the comment URL describing the purpose of this cookie, or
     * {@code null} if the cookie has no comment URL.
     *
     * @return  a {@code String} containing the comment URL, or {@code null}
     *          if none
     *
     * @see  #setCommentURL
     */
    public String getCommentURL() {
        return commentURL;
    }

    /**
     * Specify whether user agent should discard the cookie unconditionally.
     * This is RFC 2965 only attribute.
     *
     * @param  discard
     *         {@code true} indicates to discard cookie unconditionally
     *
     * @see  #getDiscard
     */
    public void setDiscard(boolean discard) {
        toDiscard = discard;
    }

    /**
     * Returns the discard attribute of the cookie
     *
     * @return  a {@code boolean} to represent this cookie's discard attribute
     *
     * @see  #setDiscard
     */
    public boolean getDiscard() {
        return toDiscard;
    }

    /**
     * Specify the portlist of the cookie, which restricts the port(s)
     * to which a cookie may be sent back in a Cookie header.
     *
     * @param  ports
     *         a {@code String} specify the port list, which is comma separated
     *         series of digits
     *
     * @see  #getPortlist
     */
    public void setPortlist(String ports) {
        portlist = ports;
    }

    /**
     * Returns the port list attribute of the cookie
     *
     * @return  a {@code String} contains the port list or {@code null} if none
     *
     * @see  #setPortlist
     */
    public String getPortlist() {
        return portlist;
    }

    /**
     * Specifies the domain within which this cookie should be presented.
     *
     * <p> The form of the domain name is specified by RFC 2965. A domain
     * name begins with a dot ({@code .foo.com}) and means that
     * the cookie is visible to servers in a specified Domain Name System
     * (DNS) zone (for example, {@code www.foo.com}, but not
     * {@code a.b.foo.com}). By default, cookies are only returned
     * to the server that sent them.
     *
     * @param  pattern
     *         a {@code String} containing the domain name within which this
     *         cookie is visible; form is according to RFC 2965
     *
     * @see  #getDomain
     */
    public void setDomain(String pattern) {
        if (pattern != null)
            domain = pattern.toLowerCase();
        else
            domain = pattern;
    }

    /**
     * Returns the domain name set for this cookie. The form of the domain name
     * is set by RFC 2965.
     *
     * @return  a {@code String} containing the domain name
     *
     * @see  #setDomain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets the maximum age of the cookie in seconds.
     *
     * <p> A positive value indicates that the cookie will expire
     * after that many seconds have passed. Note that the value is
     * the <i>maximum</i> age when the cookie will expire, not the cookie's
     * current age.
     *
     * <p> A negative value means that the cookie is not stored persistently
     * and will be deleted when the Web browser exits. A zero value causes the
     * cookie to be deleted.
     *
     * @param  expiry
     *         an integer specifying the maximum age of the cookie in seconds;
     *         if zero, the cookie should be discarded immediately; otherwise,
     *         the cookie's max age is unspecified.
     *
     * @see  #getMaxAge
     */
    public void setMaxAge(long expiry) {
        maxAge = expiry;
    }

    /**
     * Returns the maximum age of the cookie, specified in seconds. By default,
     * {@code -1} indicating the cookie will persist until browser shutdown.
     *
     * @return  an integer specifying the maximum age of the cookie in seconds
     *
     * @see  #setMaxAge
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * Specifies a path for the cookie to which the client should return
     * the cookie.
     *
     * <p> The cookie is visible to all the pages in the directory
     * you specify, and all the pages in that directory's subdirectories.
     * A cookie's path must include the servlet that set the cookie,
     * for example, <i>/catalog</i>, which makes the cookie
     * visible to all directories on the server under <i>/catalog</i>.
     *
     * <p> Consult RFC 2965 (available on the Internet) for more
     * information on setting path names for cookies.
     *
     * @param  uri
     *         a {@code String} specifying a path
     *
     * @see  #getPath
     */
    public void setPath(String uri) {
        path = uri;
    }

    /**
     * Returns the path on the server to which the browser returns this cookie.
     * The cookie is visible to all subpaths on the server.
     *
     * @return  a {@code String} specifying a path that contains a servlet name,
     *          for example, <i>/catalog</i>
     *
     * @see  #setPath
     */
    public String getPath() {
        return path;
    }

    /**
     * Indicates whether the cookie should only be sent using a secure protocol,
     * such as HTTPS or SSL.
     *
     * <p> The default value is {@code false}.
     *
     * @param  flag
     *         If {@code true}, the cookie can only be sent over a secure
     *         protocol like HTTPS. If {@code false}, it can be sent over
     *         any protocol.
     *
     * @see  #getSecure
     */
    public void setSecure(boolean flag) {
        secure = flag;
    }

    /**
     * Returns {@code true} if sending this cookie should be restricted to a
     * secure protocol, or {@code false} if the it can be sent using any
     * protocol.
     *
     * @return  {@code false} if the cookie can be sent over any standard
     *          protocol; otherwise, {@code true}
     *
     * @see  #setSecure
     */
    public boolean getSecure() {
        return secure;
    }

    /**
     * Returns the name of the cookie. The name cannot be changed after
     * creation.
     *
     * @return  a {@code String} specifying the cookie's name
     */
    public String getName() {
        return name;
    }

    /**
     * Assigns a new value to a cookie after the cookie is created.
     * If you use a binary value, you may want to use BASE64 encoding.
     *
     * <p> With Version 0 cookies, values should not contain white space,
     * brackets, parentheses, equals signs, commas, double quotes, slashes,
     * question marks, at signs, colons, and semicolons. Empty values may not
     * behave the same way on all browsers.
     *
     * @param  newValue
     *         a {@code String} specifying the new value
     *
     * @see  #getValue
     */
    public void setValue(String newValue) {
        value = newValue;
    }

    /**
     * Returns the value of the cookie.
     *
     * @return  a {@code String} containing the cookie's present value
     *
     * @see  #setValue
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the version of the protocol this cookie complies with. Version 1
     * complies with RFC 2965/2109, and version 0 complies with the original
     * cookie specification drafted by Netscape. Cookies provided by a browser
     * use and identify the browser's cookie version.
     *
     * @return  0 if the cookie complies with the original Netscape
     *          specification; 1 if the cookie complies with RFC 2965/2109
     *
     * @see  #setVersion
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets the version of the cookie protocol this cookie complies
     * with. Version 0 complies with the original Netscape cookie
     * specification. Version 1 complies with RFC 2965/2109.
     *
     * @param  v
     *         0 if the cookie should comply with the original Netscape
     *         specification; 1 if the cookie should comply with RFC 2965/2109
     *
     * @throws  IllegalArgumentException
     *          if {@code v} is neither 0 nor 1
     *
     * @see  #getVersion
     */
    public void setVersion(int v) {
        if (v != 0 && v != 1) {
            throw new IllegalArgumentException("cookie version should be 0 or 1");
        }

        version = v;
    }

    /**
     * Returns {@code true} if this cookie contains the <i>HttpOnly</i>
     * attribute. This means that the cookie should not be accessible to
     * scripting engines, like javascript.
     *
     * @return  {@code true} if this cookie should be considered HTTPOnly
     *
     * @see  #setHttpOnly(boolean)
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * Indicates whether the cookie should be considered HTTP Only. If set to
     * {@code true} it means the cookie should not be accessible to scripting
     * engines like javascript.
     *
     * @param  httpOnly
     *         if {@code true} make the cookie HTTP only, i.e. only visible as
     *         part of an HTTP request.
     *
     * @see  #isHttpOnly()
     */
    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * The utility method to check whether a host name is in a domain or not.
     *
     * <p> This concept is described in the cookie specification.
     * To understand the concept, some terminologies need to be defined first:
     * <blockquote>
     * effective host name = hostname if host name contains dot<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;or = hostname.local if not
     * </blockquote>
     * <p>Host A's name domain-matches host B's if:
     * <blockquote><ul>
     *   <li>their host name strings string-compare equal; or</li>
     *   <li>A is a HDN string and has the form NB, where N is a non-empty
     *   name string, B has the form .B', and B' is a HDN string.  (So,
     *   x.y.com domain-matches .Y.com but not Y.com.)</li>
     * </ul></blockquote>
     *
     * <p>A host isn't in a domain (RFC 2965 sec. 3.3.2) if:
     * <blockquote><ul>
     *   <li>The value for the Domain attribute contains no embedded dots,
     *   and the value is not .local.</li>
     *   <li>The effective host name that derives from the request-host does
     *   not domain-match the Domain attribute.</li>
     *   <li>The request-host is a HDN (not IP address) and has the form HD,
     *   where D is the value of the Domain attribute, and H is a string
     *   that contains one or more dots.</li>
     * </ul></blockquote>
     *
     * <p>Examples:
     * <blockquote><ul>
     *   <li>A Set-Cookie2 from request-host y.x.foo.com for Domain=.foo.com
     *   would be rejected, because H is y.x and contains a dot.</li>
     *   <li>A Set-Cookie2 from request-host x.foo.com for Domain=.foo.com
     *   would be accepted.</li>
     *   <li>A Set-Cookie2 with Domain=.com or Domain=.com., will always be
     *   rejected, because there is no embedded dot.</li>
     *   <li>A Set-Cookie2 from request-host example for Domain=.local will
     *   be accepted, because the effective host name for the request-
     *   host is example.local, and example.local domain-matches .local.</li>
     * </ul></blockquote>
     *
     * @param  domain
     *         the domain name to check host name with
     *
     * @param  host
     *         the host name in question
     *
     * @return  {@code true} if they domain-matches; {@code false} if not
     */
    public static boolean domainMatches(String domain, String host) {
        if (domain == null || host == null)
            return false;

        // if there's no embedded dot in domain and domain is not .local
        boolean isLocalDomain = ".local".equalsIgnoreCase(domain);
        int embeddedDotInDomain = domain.indexOf('.');
        if (embeddedDotInDomain == 0)
            embeddedDotInDomain = domain.indexOf('.', 1);
        if (!isLocalDomain
            && (embeddedDotInDomain == -1 ||
                embeddedDotInDomain == domain.length() - 1))
            return false;

        // if the host name contains no dot and the domain name
        // is .local or host.local
        int firstDotInHost = host.indexOf('.');
        if (firstDotInHost == -1 &&
            (isLocalDomain ||
             domain.equalsIgnoreCase(host + ".local"))) {
            return true;
        }

        int domainLength = domain.length();
        int lengthDiff = host.length() - domainLength;
        if (lengthDiff == 0) {
            // if the host name and the domain name are just string-compare equal
            return host.equalsIgnoreCase(domain);
        }
        else if (lengthDiff > 0) {
            // need to check H & D component
            String H = host.substring(0, lengthDiff);
            String D = host.substring(lengthDiff);

            return (H.indexOf('.') == -1 && D.equalsIgnoreCase(domain));
        }
        else if (lengthDiff == -1) {
            // if domain is actually .host
            return (domain.charAt(0) == '.' &&
                        host.equalsIgnoreCase(domain.substring(1)));
        }

        return false;
    }

    /**
     * Constructs a cookie header string representation of this cookie,
     * which is in the format defined by corresponding cookie specification,
     * but without the leading "Cookie:" token.
     *
     * @return  a string form of the cookie. The string has the defined format
     */
    @Override
    public String toString() {
        if (getVersion() > 0) {
            return toRFC2965HeaderString();
        } else {
            return toNetscapeHeaderString();
        }
    }

    /**
     * Test the equality of two HTTP cookies.
     *
     * <p> The result is {@code true} only if two cookies come from same domain
     * (case-insensitive), have same name (case-insensitive), and have same path
     * (case-sensitive).
     *
     * @return  {@code true} if two HTTP cookies equal to each other;
     *          otherwise, {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof HttpCookie other))
            return false;

        // One http cookie is equal to another cookie (RFC 2965 sec. 3.3.3) if:
        //   1. they come from same domain (case-insensitive),
        //   2. have same name (case-insensitive),
        //   3. and have same path (case-sensitive).
        return equalsIgnoreCase(getName(), other.getName()) &&
               equalsIgnoreCase(getDomain(), other.getDomain()) &&
               Objects.equals(getPath(), other.getPath());
    }

    /**
     * Returns the hash code of this HTTP cookie. The result is the sum of
     * hash code value of three significant components of this cookie: name,
     * domain, and path. That is, the hash code is the value of the expression:
     * <blockquote>
     * getName().toLowerCase().hashCode()<br>
     * + getDomain().toLowerCase().hashCode()<br>
     * + getPath().hashCode()
     * </blockquote>
     *
     * @return  this HTTP cookie's hash code
     */
    @Override
    public int hashCode() {
        int h1 = name.toLowerCase().hashCode();
        int h2 = (domain!=null) ? domain.toLowerCase().hashCode() : 0;
        int h3 = (path!=null) ? path.hashCode() : 0;

        return h1 + h2 + h3;
    }

    /**
     * Create and return a copy of this object.
     *
     * @return  a clone of this HTTP cookie
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    // ---------------- Package private operations --------------

    long getCreationTime() {
        return whenCreated;
    }

    // ---------------- Private operations --------------

    // Note -- disabled for now to allow full Netscape compatibility
    // from RFC 2068, token special case characters
    //
    // private static final String tspecials = "()<>@,;:\\\"/[]?={} \t";
    private static final String tspecials = ",; ";  // deliberately includes space

    /*
     * Tests a string and returns true if the string counts as a token.
     *
     * @param  value
     *         the {@code String} to be tested
     *
     * @return  {@code true} if the {@code String} is a token;
     *          {@code false} if it is not
     */
    private static boolean isToken(String value) {
        int len = value.length();

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);

            if (c < 0x20 || c >= 0x7f || tspecials.indexOf(c) != -1)
                return false;
        }
        return true;
    }

    /*
     * Parse header string to cookie object.
     *
     * @param  header
     *         header string; should contain only one NAME=VALUE pair
     *
     * @return  an HttpCookie being extracted
     *
     * @throws  IllegalArgumentException
     *          if header string violates the cookie specification
     */
    private static HttpCookie parseInternal(String header,
                                            boolean retainHeader)
    {
        HttpCookie cookie = null;
        String namevaluePair = null;

        StringTokenizer tokenizer = new StringTokenizer(header, ";");

        // there should always have at least on name-value pair;
        // it's cookie's name
        try {
            namevaluePair = tokenizer.nextToken();
            int index = namevaluePair.indexOf('=');
            if (index != -1) {
                String name = namevaluePair.substring(0, index).trim();
                String value = namevaluePair.substring(index + 1).trim();
                if (retainHeader)
                    cookie = new HttpCookie(name,
                                            stripOffSurroundingQuote(value),
                                            header);
                else
                    cookie = new HttpCookie(name,
                                            stripOffSurroundingQuote(value));
            } else {
                // no "=" in name-value pair; it's an error
                throw new IllegalArgumentException("Invalid cookie name-value pair");
            }
        } catch (NoSuchElementException ignored) {
            throw new IllegalArgumentException("Empty cookie header string");
        }

        // remaining name-value pairs are cookie's attributes
        while (tokenizer.hasMoreTokens()) {
            namevaluePair = tokenizer.nextToken();
            int index = namevaluePair.indexOf('=');
            String name, value;
            if (index != -1) {
                name = namevaluePair.substring(0, index).trim();
                value = namevaluePair.substring(index + 1).trim();
            } else {
                name = namevaluePair.trim();
                value = null;
            }

            // assign attribute to cookie
            assignAttribute(cookie, name, value);
        }

        return cookie;
    }

    /*
     * assign cookie attribute value to attribute name;
     * use a map to simulate method dispatch
     */
    static interface CookieAttributeAssignor {
            public void assign(HttpCookie cookie,
                               String attrName,
                               String attrValue);
    }
    static final java.util.Map<String, CookieAttributeAssignor> assignors =
            new java.util.HashMap<>();
    static {
        assignors.put("comment", new CookieAttributeAssignor() {
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    if (cookie.getComment() == null)
                        cookie.setComment(attrValue);
                }
            });
        assignors.put("commenturl", new CookieAttributeAssignor() {
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    if (cookie.getCommentURL() == null)
                        cookie.setCommentURL(attrValue);
                }
            });
        assignors.put("discard", new CookieAttributeAssignor() {
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    cookie.setDiscard(true);
                }
            });
        assignors.put("domain", new CookieAttributeAssignor(){
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    if (cookie.getDomain() == null)
                        cookie.setDomain(attrValue);
                }
            });
        assignors.put("max-age", new CookieAttributeAssignor(){
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    try {
                        long maxage = Long.parseLong(attrValue);
                        if (cookie.getMaxAge() == MAX_AGE_UNSPECIFIED)
                            cookie.setMaxAge(maxage);
                    } catch (NumberFormatException ignored) {
                        throw new IllegalArgumentException(
                                "Illegal cookie max-age attribute");
                    }
                }
            });
        assignors.put("path", new CookieAttributeAssignor(){
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    if (cookie.getPath() == null)
                        cookie.setPath(attrValue);
                }
            });
        assignors.put("port", new CookieAttributeAssignor(){
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    if (cookie.getPortlist() == null)
                        cookie.setPortlist(attrValue == null ? "" : attrValue);
                }
            });
        assignors.put("secure", new CookieAttributeAssignor(){
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    cookie.setSecure(true);
                }
            });
        assignors.put("httponly", new CookieAttributeAssignor(){
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    cookie.setHttpOnly(true);
                }
            });
        assignors.put("version", new CookieAttributeAssignor(){
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    try {
                        int version = Integer.parseInt(attrValue);
                        cookie.setVersion(version);
                    } catch (NumberFormatException ignored) {
                        // Just ignore bogus version, it will default to 0 or 1
                    }
                }
            });
        assignors.put("expires", new CookieAttributeAssignor(){ // Netscape only
                public void assign(HttpCookie cookie,
                                   String attrName,
                                   String attrValue) {
                    if (cookie.getMaxAge() == MAX_AGE_UNSPECIFIED) {
                        long delta = cookie.expiryDate2DeltaSeconds(attrValue);
                        cookie.setMaxAge(delta > 0 ? delta : 0);
                    }
                }
            });
    }
    private static void assignAttribute(HttpCookie cookie,
                                        String attrName,
                                        String attrValue)
    {
        // strip off the surrounding "-sign if there's any
        attrValue = stripOffSurroundingQuote(attrValue);

        CookieAttributeAssignor assignor = assignors.get(attrName.toLowerCase());
        if (assignor != null) {
            assignor.assign(cookie, attrName, attrValue);
        } else {
            // Ignore the attribute as per RFC 2965
        }
    }

    static {
        SharedSecrets.setJavaNetHttpCookieAccess(
            new JavaNetHttpCookieAccess() {
                public List<HttpCookie> parse(String header) {
                    return HttpCookie.parse(header, true);
                }

                public String header(HttpCookie cookie) {
                    return cookie.header;
                }
            }
        );
    }

    /*
     * Returns the original header this cookie was constructed from, if it was
     * constructed by parsing a header, otherwise null.
     */
    private String header() {
        return header;
    }

    /*
     * Constructs a string representation of this cookie. The string format is
     * as Netscape spec, but without leading "Cookie:" token.
     */
    private String toNetscapeHeaderString() {
        return getName() + "=" + getValue();
    }

    /*
     * Constructs a string representation of this cookie. The string format is
     * as RFC 2965/2109, but without leading "Cookie:" token.
     */
    private String toRFC2965HeaderString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getName()).append("=\"").append(getValue()).append('"');
        if (getPath() != null)
            sb.append(";$Path=\"").append(getPath()).append('"');
        if (getDomain() != null)
            sb.append(";$Domain=\"").append(getDomain()).append('"');
        if (getPortlist() != null)
            sb.append(";$Port=\"").append(getPortlist()).append('"');

        return sb.toString();
    }

    static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    /*
     * @param  dateString
     *         a date string in one of the formats defined in Netscape cookie spec
     *
     * @return  delta seconds between this cookie's creation time and the time
     *          specified by dateString
     */
    private long expiryDate2DeltaSeconds(String dateString) {
        Calendar cal = new GregorianCalendar(GMT);
        for (int i = 0; i < COOKIE_DATE_FORMATS.length; i++) {
            SimpleDateFormat df = new SimpleDateFormat(COOKIE_DATE_FORMATS[i],
                                                       Locale.US);
            cal.set(1970, 0, 1, 0, 0, 0);
            df.setTimeZone(GMT);
            df.setLenient(false);
            df.set2DigitYearStart(cal.getTime());
            try {
                cal.setTime(df.parse(dateString));
                if (!COOKIE_DATE_FORMATS[i].contains("yyyy")) {
                    // 2-digit years following the standard set
                    // out it rfc 6265
                    int year = cal.get(Calendar.YEAR);
                    year %= 100;
                    if (year < 70) {
                        year += 2000;
                    } else {
                        year += 1900;
                    }
                    cal.set(Calendar.YEAR, year);
                }
                return (cal.getTimeInMillis() - whenCreated) / 1000;
            } catch (Exception e) {
                // Ignore, try the next date format
            }
        }
        return 0;
    }

    /*
     * try to guess the cookie version through set-cookie header string
     */
    private static int guessCookieVersion(String header) {
        int version = 0;

        header = header.toLowerCase();
        if (header.indexOf("expires=") != -1) {
            // only netscape cookie using 'expires'
            version = 0;
        } else if (header.indexOf("version=") != -1) {
            // version is mandatory for rfc 2965/2109 cookie
            version = 1;
        } else if (header.indexOf("max-age") != -1) {
            // rfc 2965/2109 use 'max-age'
            version = 1;
        } else if (startsWithIgnoreCase(header, SET_COOKIE2)) {
            // only rfc 2965 cookie starts with 'set-cookie2'
            version = 1;
        }

        return version;
    }

    private static String stripOffSurroundingQuote(String str) {
        if (str != null && str.length() > 2 &&
            str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"') {
            return str.substring(1, str.length() - 1);
        }
        if (str != null && str.length() > 2 &&
            str.charAt(0) == '\'' && str.charAt(str.length() - 1) == '\'') {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static boolean equalsIgnoreCase(String s, String t) {
        if (s == t) return true;
        if ((s != null) && (t != null)) {
            return s.equalsIgnoreCase(t);
        }
        return false;
    }

    private static boolean startsWithIgnoreCase(String s, String start) {
        if (s == null || start == null) return false;

        if (s.length() >= start.length() &&
                start.equalsIgnoreCase(s.substring(0, start.length()))) {
            return true;
        }

        return false;
    }

    /*
     * Split cookie header string according to rfc 2965:
     *   1) split where it is a comma;
     *   2) but not the comma surrounding by double-quotes, which is the comma
     *      inside port list or embedded URIs.
     *
     * @param  header
     *         the cookie header string to split
     *
     * @return  list of strings; never null
     */
    private static List<String> splitMultiCookies(String header) {
        List<String> cookies = new java.util.ArrayList<>();
        int quoteCount = 0;
        int p, q;

        for (p = 0, q = 0; p < header.length(); p++) {
            char c = header.charAt(p);
            if (c == '"') quoteCount++;
            if (c == ',' && (quoteCount % 2 == 0)) {
                // it is comma and not surrounding by double-quotes
                cookies.add(header.substring(q, p));
                q = p + 1;
            }
        }

        cookies.add(header.substring(q));

        return cookies;
    }
}
