/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */

package com.sun.inputmethods.internal.codepointim;


import java.awt.Image;
import java.awt.im.spi.InputMethodDescriptor;
import java.awt.im.spi.InputMethod;
import java.util.Locale;


/**
 * The CodePointInputMethod is a simple input method that allows Unicode
 * characters to be entered via their hexadecimal code point values.
 *
 * The class, CodePointInputMethodDescriptor, provides information about the
 * CodePointInputMethod which allows it to be selected and loaded by the
 * Input Method Framework.
 */
public class CodePointInputMethodDescriptor implements InputMethodDescriptor {

    public CodePointInputMethodDescriptor() {
    }

    /**
     * Creates a new instance of the Code Point input method.
     *
     * @return a new instance of the Code Point input method
     * @exception Exception any exception that may occur while creating the
     * input method instance
     */
    public InputMethod createInputMethod() throws Exception {
        return new CodePointInputMethod();
    }

    /**
     * This input method can be used by any locale.
     */
    public Locale[] getAvailableLocales() {
        Locale[] locales = {
            new Locale("", "", ""), };
        return locales;
    }

    public synchronized String getInputMethodDisplayName(Locale inputLocale,
            Locale displayLanguage) {
        return "CodePoint Input Method";
    }

    public Image getInputMethodIcon(Locale inputLocale) {
        return null;
    }

    public boolean hasDynamicLocaleList() {
        return false;
    }
}
