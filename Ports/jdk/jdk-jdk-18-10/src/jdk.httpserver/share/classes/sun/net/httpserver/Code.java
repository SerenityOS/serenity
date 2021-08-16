/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.httpserver;

class Code {

    public static final int HTTP_CONTINUE = 100;
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_RESET = 205;
    public static final int HTTP_PARTIAL = 206;
    public static final int HTTP_MULT_CHOICE = 300;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_SEE_OTHER = 303;
    public static final int HTTP_NOT_MODIFIED = 304;
    public static final int HTTP_USE_PROXY = 305;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    public static final int HTTP_PROXY_AUTH = 407;
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_GONE = 410;
    public static final int HTTP_LENGTH_REQUIRED = 411;
    public static final int HTTP_PRECON_FAILED = 412;
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    public static final int HTTP_REQ_TOO_LONG = 414;
    public static final int HTTP_UNSUPPORTED_TYPE = 415;
    public static final int HTTP_INTERNAL_ERROR = 500;
    public static final int HTTP_NOT_IMPLEMENTED = 501;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_UNAVAILABLE = 503;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    public static final int HTTP_VERSION = 505;

    static String msg (int code) {

      switch (code) {
        case HTTP_OK: return " OK";
        case HTTP_CONTINUE: return " Continue";
        case HTTP_CREATED: return " Created";
        case HTTP_ACCEPTED: return " Accepted";
        case HTTP_NOT_AUTHORITATIVE: return " Non-Authoritative Information";
        case HTTP_NO_CONTENT: return " No Content";
        case HTTP_RESET: return " Reset Content";
        case HTTP_PARTIAL: return " Partial Content";
        case HTTP_MULT_CHOICE: return " Multiple Choices";
        case HTTP_MOVED_PERM: return " Moved Permanently";
        case HTTP_MOVED_TEMP: return " Temporary Redirect";
        case HTTP_SEE_OTHER: return " See Other";
        case HTTP_NOT_MODIFIED: return " Not Modified";
        case HTTP_USE_PROXY: return " Use Proxy";
        case HTTP_BAD_REQUEST: return " Bad Request";
        case HTTP_UNAUTHORIZED: return " Unauthorized" ;
        case HTTP_PAYMENT_REQUIRED: return " Payment Required";
        case HTTP_FORBIDDEN: return " Forbidden";
        case HTTP_NOT_FOUND: return " Not Found";
        case HTTP_BAD_METHOD: return " Method Not Allowed";
        case HTTP_NOT_ACCEPTABLE: return " Not Acceptable";
        case HTTP_PROXY_AUTH: return " Proxy Authentication Required";
        case HTTP_CLIENT_TIMEOUT: return " Request Time-Out";
        case HTTP_CONFLICT: return " Conflict";
        case HTTP_GONE: return " Gone";
        case HTTP_LENGTH_REQUIRED: return " Length Required";
        case HTTP_PRECON_FAILED: return " Precondition Failed";
        case HTTP_ENTITY_TOO_LARGE: return " Request Entity Too Large";
        case HTTP_REQ_TOO_LONG: return " Request-URI Too Large";
        case HTTP_UNSUPPORTED_TYPE: return " Unsupported Media Type";
        case HTTP_INTERNAL_ERROR: return " Internal Server Error";
        case HTTP_NOT_IMPLEMENTED: return " Not Implemented";
        case HTTP_BAD_GATEWAY: return " Bad Gateway";
        case HTTP_UNAVAILABLE: return " Service Unavailable";
        case HTTP_GATEWAY_TIMEOUT: return " Gateway Timeout";
        case HTTP_VERSION: return " HTTP Version Not Supported";
        default: return " ";
      }
    }
}
