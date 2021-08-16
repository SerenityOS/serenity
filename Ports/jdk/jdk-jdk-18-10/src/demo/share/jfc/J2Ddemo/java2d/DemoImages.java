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


import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A collection of all the demo images found in the images directory.
 * Certain classes are preloaded; the rest are loaded lazily.
 */
@SuppressWarnings("serial")
public class DemoImages extends Component {

    private static final String[] names = {
        "bld.jpg", "boat.png", "box.png",
        "boxwave.png", "clouds.jpg", "duke.gif", "duke.running.gif",
        "dukeplug.png", "fight.png", "globe.png",
        "jumptojavastrip.png", "magnify.png", "painting.png",
        "remove.gif", "snooze.png", "star7.gif", "surfing.png",
        "thumbsup.png", "tip.png", "duke.png", "print.gif",
        "loop.gif", "looping.gif", "start.gif", "start2.gif",
        "stop.gif", "stop2.gif", "clone.gif"
    };
    private static final Map<String, Image> cache =
            new ConcurrentHashMap<String, Image>(names.length);

    private DemoImages() {
    }

    public static void newDemoImages() {
        DemoImages demoImages = new DemoImages();
        for (String name : names) {
            cache.put(name, getImage(name, demoImages));
        }
    }


    /*
     * Gets the named image using the toolkit of the specified component.
     * Note that this has to work even before we have had a chance to
     * instantiate DemoImages and preload the cache.
     */
    public static Image getImage(String name, Component cmp) {
        Image img = null;
        if (cache != null) {
            if ((img = cache.get(name)) != null) {
                return img;
            }
        }

        ClassLoader cl = cmp.getClass().getClassLoader();
        URL fileLoc = cl.getResource("images/" + name);
        img = cmp.getToolkit().getImage(fileLoc);

        MediaTracker tracker = new MediaTracker(cmp);
        tracker.addImage(img, 0);
        try {
            tracker.waitForID(0);
            if (tracker.isErrorAny()) {
                System.out.println("Error loading image " + name);
            }
        } catch (Exception ex) {
            Logger.getLogger(DemoImages.class.getName()).log(Level.SEVERE, null, ex);
        }
        return img;
    }
}
