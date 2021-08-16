/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6578647 6829283 8171340 8194486
 * @modules java.base/sun.security.util
 *          java.security.jgss/sun.security.krb5.internal:+open
 *          java.security.jgss/sun.security.jgss
 *          java.security.jgss/sun.security.krb5:+open
 *          java.security.jgss/sun.security.jgss.krb5
 *          java.security.jgss/sun.security.krb5.internal.ccache
 *          java.security.jgss/sun.security.krb5.internal.crypto
 *          java.security.jgss/sun.security.krb5.internal.ktab
 *          jdk.security.auth
 *          jdk.security.jgss
 *          jdk.httpserver
 * @summary Undefined requesting URL in java.net.Authenticator
 *          .getPasswordAuthentication()
 * @summary HTTP/Negotiate: Authenticator triggered again when
 *          user cancels the first one
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djava.security.manager=allow -Djdk.net.hosts.file=TestHosts HttpNegotiateServer
 */

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.security.auth.module.Krb5LoginModule;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.Config;
import java.util.Base64;

/**
 * Basic JGSS/krb5 test with 3 parties: client, server, backend server. Each
 * party uses JAAS login to get subjects and executes JGSS calls using
 * Subject.doAs.
 */
public class HttpNegotiateServer {

    // Two realm, web server in one, proxy server in another
    final static String REALM_WEB = "WEB.DOMAIN";
    final static String REALM_PROXY = "PROXY.DOMAIN";
    final static String KRB5_CONF = "web.conf";
    final static String KRB5_TAB = "web.ktab";

    // user principals
    final static String WEB_USER = "web";
    final static char[] WEB_PASS = "webby".toCharArray();
    final static String PROXY_USER = "pro";
    final static char[] PROXY_PASS = "proxy".toCharArray();


    final static String WEB_HOST = "host.web.domain";
    final static String PROXY_HOST = "host.proxy.domain";

    // web page content
    final static String CONTENT = "Hello, World!";

    // For 6829283, count how many times the Authenticator is called.
    static int count = 0;

    static int webPort, proxyPort;

    // URLs for web test, proxy test. The proxy server is not a real proxy
    // since it fakes the same content for any URL. :)
    static URL webUrl, proxyUrl;

    /**
     * This Authenticator checks everything:
     * scheme, protocol, requestor type, host, port, and url
     */
    static class KnowAllAuthenticator extends java.net.Authenticator {
        public PasswordAuthentication getPasswordAuthentication () {
            if (!getRequestingScheme().equalsIgnoreCase("Negotiate")) {
                throw new RuntimeException("Bad scheme");
            }
            if (!getRequestingProtocol().equalsIgnoreCase("HTTP")) {
                throw new RuntimeException("Bad protocol");
            }
            if (getRequestorType() == RequestorType.SERVER) {
                if (!this.getRequestingHost().equalsIgnoreCase(webUrl.getHost())) {
                    throw new RuntimeException("Bad host");
                }
                if (this.getRequestingPort() != webUrl.getPort()) {
                    throw new RuntimeException("Bad port");
                }
                if (!this.getRequestingURL().equals(webUrl)) {
                    throw new RuntimeException("Bad url");
                }
                return new PasswordAuthentication(
                        WEB_USER+"@"+REALM_WEB, WEB_PASS);
            } else if (getRequestorType() == RequestorType.PROXY) {
                if (!this.getRequestingHost().equalsIgnoreCase(PROXY_HOST)) {
                    throw new RuntimeException("Bad host");
                }
                if (this.getRequestingPort() != proxyPort) {
                    throw new RuntimeException("Bad port");
                }
                if (!this.getRequestingURL().equals(proxyUrl)) {
                    throw new RuntimeException("Bad url");
                }
                return new PasswordAuthentication(
                        PROXY_USER+"@"+REALM_PROXY, PROXY_PASS);
            } else  {
                throw new RuntimeException("Bad requster type");
            }
        }
    }

    /**
     * This Authenticator knows nothing
     */
    static class KnowNothingAuthenticator extends java.net.Authenticator {
        @Override
        public PasswordAuthentication getPasswordAuthentication () {
            HttpNegotiateServer.count++;
            return null;
        }
    }

    public static void main(String[] args)
            throws Exception {

        System.setProperty("sun.security.krb5.debug", "true");

        KDC kdcw = KDC.create(REALM_WEB);
        kdcw.addPrincipal(WEB_USER, WEB_PASS);
        kdcw.addPrincipalRandKey("krbtgt/" + REALM_WEB);
        kdcw.addPrincipalRandKey("HTTP/" + WEB_HOST);

        KDC kdcp = KDC.create(REALM_PROXY);
        kdcp.addPrincipal(PROXY_USER, PROXY_PASS);
        kdcp.addPrincipalRandKey("krbtgt/" + REALM_PROXY);
        kdcp.addPrincipalRandKey("HTTP/" + PROXY_HOST);

        KDC.saveConfig(KRB5_CONF, kdcw, kdcp,
                "default_keytab_name = " + KRB5_TAB,
                "[domain_realm]",
                "",
                ".web.domain="+REALM_WEB,
                ".proxy.domain="+REALM_PROXY);

        System.setProperty("java.security.krb5.conf", KRB5_CONF);
        Config.refresh();
        KDC.writeMultiKtab(KRB5_TAB, kdcw, kdcp);

        // Write a customized JAAS conf file, so that any kinit cache
        // will be ignored.
        System.setProperty("java.security.auth.login.config", OneKDC.JAAS_CONF);
        File f = new File(OneKDC.JAAS_CONF);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write((
                "com.sun.security.jgss.krb5.initiate {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required;\n};\n"
                ).getBytes());
        fos.close();

        HttpServer h1 = httpd("Negotiate", false,
                "HTTP/" + WEB_HOST + "@" + REALM_WEB, KRB5_TAB);
        webPort = h1.getAddress().getPort();
        HttpServer h2 = httpd("Negotiate", true,
                "HTTP/" + PROXY_HOST + "@" + REALM_PROXY, KRB5_TAB);
        proxyPort = h2.getAddress().getPort();

        webUrl = new URL("http://" + WEB_HOST +":" + webPort + "/a/b/c");
        proxyUrl = new URL("http://nosuchplace/a/b/c");

        try {
            Exception e1 = null, e2 = null, e3 = null;
            try {
                test6578647();
            } catch (Exception e) {
                e1 = e;
                e.printStackTrace();
            }
            try {
                test6829283();
            } catch (Exception e) {
                e2 = e;
                e.printStackTrace();
            }
            try {
                test8077155();
            } catch (Exception e) {
                e3 = e;
                e.printStackTrace();
            }

            if (e1 != null || e2 != null || e3 != null) {
                throw new RuntimeException("Test error");
            }
        } finally {
            // Must stop. Seems there's no HttpServer.startAsDaemon()
            if (h1 != null) h1.stop(0);
            if (h2 != null) h2.stop(0);
        }
    }

    static void test6578647() throws Exception {
        BufferedReader reader;
        java.net.Authenticator.setDefault(new KnowAllAuthenticator());

        reader = new BufferedReader(new InputStreamReader(
                webUrl.openConnection(Proxy.NO_PROXY).getInputStream()));
        if (!reader.readLine().equals(CONTENT)) {
            throw new RuntimeException("Bad content");
        }

        reader = new BufferedReader(new InputStreamReader(
                proxyUrl.openConnection(new Proxy(Proxy.Type.HTTP,
                                new InetSocketAddress(PROXY_HOST, proxyPort)))
                        .getInputStream()));
        if (!reader.readLine().equals(CONTENT)) {
            throw new RuntimeException("Bad content");
        }
    }

    static void test6829283() throws Exception {
        BufferedReader reader;
        java.net.Authenticator.setDefault(new KnowNothingAuthenticator());
        try {
            new BufferedReader(new InputStreamReader(
                    webUrl.openConnection(Proxy.NO_PROXY).getInputStream()));
        } catch (IOException ioe) {
            // Will fail since no username and password is provided.
        }
        if (count > 1) {
            throw new RuntimeException("Authenticator called twice");
        }
    }

    static void testConnect() {
        InputStream inputStream = null;
        try {
            URL url = webUrl;

            URLConnection conn = url.openConnection(Proxy.NO_PROXY);
            conn.connect();
            inputStream = conn.getInputStream();
            byte[] b = new byte[inputStream.available()];
            for (int j = 0; j < b.length; j++) {
                b[j] = (byte) inputStream.read();
            }
            String s = new String(b);
            System.out.println("Length: " + s.length());
            System.out.println(s);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static void test8077155() throws Exception {
        final String username = WEB_USER;
        final char[] password = WEB_PASS;

        SecurityManager security = new SecurityManager();
        Policy.setPolicy(new SecurityPolicy());
        System.setSecurityManager(security);

        CallbackHandler callback = new CallbackHandler() {
            @Override
            public void handle(Callback[] pCallbacks)
                    throws IOException, UnsupportedCallbackException {
                for (Callback cb : pCallbacks) {
                    if (cb instanceof NameCallback) {
                        NameCallback ncb = (NameCallback)cb;
                        ncb.setName(username);

                    } else  if (cb instanceof PasswordCallback) {
                        PasswordCallback pwdcb = (PasswordCallback) cb;
                        pwdcb.setPassword(password);
                    }
                }
            }

        };

        final String jaasConfigName = "oracle.test.kerberos.login";
        final String krb5LoginModule
                = "com.sun.security.auth.module.Krb5LoginModule";

        Configuration loginConfig = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                if (! jaasConfigName.equals(name)) {
                    return new AppConfigurationEntry[0];
                }

                Map<String, String> options = new HashMap<String, String>();
                options.put("useTicketCache", Boolean.FALSE.toString());
                options.put("useKeyTab", Boolean.FALSE.toString());

                return new AppConfigurationEntry[] {
                        new AppConfigurationEntry(krb5LoginModule,
                                LoginModuleControlFlag.REQUIRED,
                                options)
                        };
            }
        };

        // oracle context/subject/login
        LoginContext context = null;
        try {
            context = new LoginContext(
                    "oracle.test.kerberos.login", null, callback, loginConfig);
            context.login();

        } catch (LoginException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }


        Subject subject = context.getSubject();

        final PrivilegedExceptionAction<Object> test_action
                = new PrivilegedExceptionAction<Object>() {
            public Object run() throws Exception {
                testConnect();
                return null;
            }
        };

        System.err.println("\n\nExpecting to succeed when executing " +
                "with the the logged in subject.");

        try {
            Subject.doAs(subject, test_action);
            System.err.println("\n\nConnection succeed when executing " +
                    "with the the logged in subject.");
        } catch (PrivilegedActionException e) {
            System.err.println("\n\nFailure unexpected when executing " +
                    "with the the logged in subject.");
            e.printStackTrace();
            throw new RuntimeException("Failed to login as subject");
        }

        try {
            System.err.println("\n\nExpecting to fail when running " +
                    "with the current user's login.");
            testConnect();
        } catch (Exception ex) {
            System.err.println("\nConnect failed when running " +
                    "with the current user's login:\n" + ex.getMessage());
        }
    }

    /**
     * Creates and starts an HTTP or proxy server that requires
     * Negotiate authentication.
     * @param scheme "Negotiate" or "Kerberos"
     * @param principal the krb5 service principal the server runs with
     * @return the server
     */
    public static HttpServer httpd(String scheme, boolean proxy,
            String principal, String ktab) throws Exception {
        MyHttpHandler h = new MyHttpHandler();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        HttpContext hc = server.createContext("/", h);
        hc.setAuthenticator(new MyServerAuthenticator(
                proxy, scheme, principal, ktab));
        server.start();
        return server;
    }

    static class MyHttpHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, 0);
            t.getResponseBody().write(CONTENT.getBytes());
            t.close();
        }
    }

    static class MyServerAuthenticator
            extends com.sun.net.httpserver.Authenticator {
        Subject s = new Subject();
        GSSManager m = null;
        GSSCredential cred = null;
        String scheme = null;
        String reqHdr = "WWW-Authenticate";
        String respHdr = "Authorization";
        int err = HttpURLConnection.HTTP_UNAUTHORIZED;

        public MyServerAuthenticator(boolean proxy, String scheme,
                String principal, String ktab) throws Exception {

            this.scheme = scheme;
            if (proxy) {
                reqHdr = "Proxy-Authenticate";
                respHdr = "Proxy-Authorization";
                err = HttpURLConnection.HTTP_PROXY_AUTH;
            }

            Krb5LoginModule krb5 = new Krb5LoginModule();
            Map<String, String> map = new HashMap<>();
            Map<String, Object> shared = new HashMap<>();

            map.put("storeKey", "true");
            map.put("isInitiator", "false");
            map.put("useKeyTab", "true");
            map.put("keyTab", ktab);
            map.put("principal", principal);
            krb5.initialize(s, null, shared, map);
            krb5.login();
            krb5.commit();
            m = GSSManager.getInstance();
            cred = Subject.doAs(s, new PrivilegedExceptionAction<GSSCredential>() {
                @Override
                public GSSCredential run() throws Exception {
                    System.err.println("Creating GSSCredential");
                    return m.createCredential(
                            null,
                            GSSCredential.INDEFINITE_LIFETIME,
                            MyServerAuthenticator.this.scheme
                                        .equalsIgnoreCase("Negotiate") ?
                                    GSSUtil.GSS_SPNEGO_MECH_OID :
                                    GSSUtil.GSS_KRB5_MECH_OID,
                            GSSCredential.ACCEPT_ONLY);
                }
            });
        }

        @Override
        public Result authenticate(HttpExchange exch) {
            // The GSContext is stored in an HttpContext attribute named
            // "GSSContext" and is created at the first request.
            GSSContext c = null;
            String auth = exch.getRequestHeaders().getFirst(respHdr);
            try {
                c = (GSSContext)exch.getHttpContext()
                        .getAttributes().get("GSSContext");
                if (auth == null) {                 // First request
                    Headers map = exch.getResponseHeaders();
                    map.set (reqHdr, scheme);        // Challenge!
                    c = Subject.doAs(s, new PrivilegedExceptionAction<GSSContext>() {
                        @Override
                        public GSSContext run() throws Exception {
                            return m.createContext(cred);
                        }
                    });
                    exch.getHttpContext().getAttributes().put("GSSContext", c);
                    return new com.sun.net.httpserver.Authenticator.Retry(err);
                } else {                            // Later requests
                    byte[] token = Base64.getMimeDecoder()
                            .decode(auth.split(" ")[1]);
                    token = c.acceptSecContext(token, 0, token.length);
                    Headers map = exch.getResponseHeaders();
                    map.set (reqHdr, scheme + " " + Base64.getMimeEncoder()
                            .encodeToString(token).replaceAll("\\s", ""));
                    if (c.isEstablished()) {
                        return new com.sun.net.httpserver.Authenticator.Success(
                                new HttpPrincipal(c.getSrcName().toString(), ""));
                    } else {
                        return new com.sun.net.httpserver.Authenticator.Retry(err);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class SecurityPolicy extends Policy {

    private static Permissions perms;

    public SecurityPolicy() {
        super();
        if (perms == null) {
            perms = new Permissions();
            perms.add(new AllPermission());
        }
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return perms;
    }

}
