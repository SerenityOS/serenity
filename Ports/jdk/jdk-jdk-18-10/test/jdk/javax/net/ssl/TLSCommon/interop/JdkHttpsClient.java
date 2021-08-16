/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/*
 * A JDK client based on HttpsURLConnection.
 */
public class JdkHttpsClient extends AbstractClient {

    protected final int timeout;
    protected final String path;

    protected final SSLContext context;

    public JdkHttpsClient(Builder builder) throws Exception {
        timeout = builder.getTimeout() * 1000;
        path = builder.getPath();

        context = getContext(builder);
    }

    protected SSLContext getContext(Builder builder) throws Exception {
        return builder.getContext() == null
                ? Utilities.createSSLContext(builder.getCertTuple())
                : builder.getContext();
    }

    public static class Builder extends AbstractClient.Builder {

        private String path = "";

        private SSLContext context;

        public String getPath() {
            return path;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public SSLContext getContext() {
            return context;
        }

        public Builder setContext(SSLContext context) {
            this.context = context;
            return this;
        }

        @Override
        public JdkHttpsClient build() throws Exception {
            return new JdkHttpsClient(this);
        }
    }

    @Override
    public Product getProduct() {
        return Jdk.DEFAULT;
    }

    @Override
    public void connect(String host, int port) throws IOException {
        String urlStr = String.format("https://%s:%d/%s", host, port, path);
        if (Utilities.DEBUG) {
            System.out.println("URL: " + urlStr);
        }

        HttpsURLConnection conn
                = (HttpsURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setSSLSocketFactory(context.getSocketFactory());
        conn.connect();
        System.out.println(Utilities.readIn(conn.getInputStream()));
    }
}
