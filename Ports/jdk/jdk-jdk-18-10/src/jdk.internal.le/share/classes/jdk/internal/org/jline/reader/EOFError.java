/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package jdk.internal.org.jline.reader;

public class EOFError extends SyntaxError {

    private static final long serialVersionUID = 1L;

    private final String missing;
    private final int openBrackets;
    private final String nextClosingBracket;

    public EOFError(int line, int column, String message) {
        this(line, column, message, null);
    }

    public EOFError(int line, int column, String message, String missing) {
        this(line, column, message, missing, 0, null);
    }

    public EOFError(int line, int column, String message, String missing, int openBrackets, String nextClosingBracket) {
        super(line, column, message);
        this.missing = missing;
        this.openBrackets = openBrackets;
        this.nextClosingBracket = nextClosingBracket;
    }

    public String getMissing() {
        return missing;
    }

    public int getOpenBrackets(){
        return openBrackets;
    }

    public String getNextClosingBracket() {
        return nextClosingBracket;
    }
}
