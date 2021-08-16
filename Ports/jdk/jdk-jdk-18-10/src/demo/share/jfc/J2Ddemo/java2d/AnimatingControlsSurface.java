/*
 *
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package java2d;


import static java2d.CustomControlsContext.State.START;
import java.awt.Component;


/**
 * Demos that animate and have custom controls extend this class.
 */
@SuppressWarnings("serial")
public abstract class AnimatingControlsSurface extends AnimatingSurface
        implements CustomControlsContext {

    @Override
    public void setControls(Component[] controls) {
        this.controls = controls;
    }

    @Override
    public void setConstraints(String[] constraints) {
        this.constraints = constraints;
    }

    @Override
    public String[] getConstraints() {
        return constraints;
    }

    @Override
    public Component[] getControls() {
        return controls;
    }

    @Override
    public void handleThread(CustomControlsContext.State state) {
        for (Component control : controls) {
            if (control instanceof CustomControls) {
                if (state == START) {
                    ((CustomControls) control).start();
                } else {
                    ((CustomControls) control).stop();
                }
            }
        }
    }

    private Component[] controls;
    private String[] constraints = { java.awt.BorderLayout.NORTH };
}
