/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "splashscreen_impl.h"

#import <Cocoa/Cocoa.h>
#import <objc/objc-auto.h>

#include <Security/AuthSession.h>
#import "NSApplicationAWT.h"

#include <sys/time.h>
#include <pthread.h>
#include <iconv.h>
#include <langinfo.h>
#include <locale.h>
#include <fcntl.h>
#include <poll.h>
#include <errno.h>
#include <sys/types.h>
#include <signal.h>
#include <unistd.h>
#include <dlfcn.h>

#include <sizecalc.h>
#import "ThreadUtilities.h"

NSString* findScaledImageName(NSString *fileName,
                              NSUInteger dotIndex,
                              NSString *strToAppend);

static NSScreen* SplashNSScreen()
{
    return [[NSScreen screens] objectAtIndex: 0];
}

static void SplashCenter(Splash * splash)
{
    NSRect screenFrame = [SplashNSScreen() frame];

    splash->x = (screenFrame.size.width - splash->width) / 2;
    splash->y = (screenFrame.size.height - splash->height) / 2 + screenFrame.origin.y;
}

unsigned
SplashTime(void) {
    struct timeval tv;
    struct timezone tz;
    unsigned long long msec;

    gettimeofday(&tv, &tz);
    msec = (unsigned long long) tv.tv_sec * 1000 +
        (unsigned long long) tv.tv_usec / 1000;

    return (unsigned) msec;
}

/* Could use npt but decided to cut down on linked code size */
char* SplashConvertStringAlloc(const char* in, int* size) {
    const char     *codeset;
    const char     *codeset_out;
    iconv_t         cd;
    size_t          rc;
    char           *buf = NULL, *out;
    size_t          bufSize, inSize, outSize;
    const char* old_locale;

    if (!in) {
        return NULL;
    }
    old_locale = setlocale(LC_ALL, "");

    codeset = nl_langinfo(CODESET);
    if ( codeset == NULL || codeset[0] == 0 ) {
        goto done;
    }
    /* we don't need BOM in output so we choose native BE or LE encoding here */
    codeset_out = (platformByteOrder()==BYTE_ORDER_MSBFIRST) ?
        "UCS-2BE" : "UCS-2LE";

    cd = iconv_open(codeset_out, codeset);
    if (cd == (iconv_t)-1 ) {
        goto done;
    }
    inSize = strlen(in);
    buf = SAFE_SIZE_ARRAY_ALLOC(malloc, inSize, 2);
    if (!buf) {
        return NULL;
    }
    bufSize = inSize*2; // need 2 bytes per char for UCS-2, this is
                        // 2 bytes per source byte max
    out = buf; outSize = bufSize;
    /* linux iconv wants char** source and solaris wants const char**...
       cast to void* */
    rc = iconv(cd, (void*)&in, &inSize, &out, &outSize);
    iconv_close(cd);

    if (rc == (size_t)-1) {
        free(buf);
        buf = NULL;
    } else {
        if (size) {
            *size = (bufSize-outSize)/2; /* bytes to wchars */
        }
    }
done:
    setlocale(LC_ALL, old_locale);
    return buf;
}

BOOL isSWTRunning() {
    char envVar[80];
    // If this property is present we are running SWT
    snprintf(envVar, sizeof(envVar), "JAVA_STARTED_ON_FIRST_THREAD_%d", getpid());
    return getenv(envVar) != NULL;
}

jboolean SplashGetScaledImageName(const char* jar, const char* file,
                                  float *scaleFactor, char *scaledFile,
                                  const size_t scaledImageLength) {
    *scaleFactor = 1;

    if(isSWTRunning()){
        return JNI_FALSE;
    }

    NSAutoreleasePool *pool = [NSAutoreleasePool new];
    __block float screenScaleFactor = 1;

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        // initialize NSApplication and AWT stuff
        [NSApplicationAWT sharedApplication];
        screenScaleFactor = [SplashNSScreen() backingScaleFactor];
    }];

    if (screenScaleFactor > 1) {
        NSString *fileName = [NSString stringWithUTF8String: file];
        NSUInteger length = [fileName length];
        NSRange range = [fileName rangeOfString: @"."
                                        options:NSBackwardsSearch];
        NSUInteger dotIndex = range.location;
        NSString *fileName2x = nil;

        fileName2x = findScaledImageName(fileName, dotIndex, @"@2x");
        if(![[NSFileManager defaultManager]
                fileExistsAtPath: fileName2x]) {
            fileName2x = findScaledImageName(fileName, dotIndex, @"@200pct");
        }
        if (jar || [[NSFileManager defaultManager]
                fileExistsAtPath: fileName2x]){
            if (strlen([fileName2x UTF8String]) > scaledImageLength) {
                [pool drain];
                return JNI_FALSE;
            }
            *scaleFactor = 2;
            strcpy(scaledFile, [fileName2x UTF8String]);
            [pool drain];
            return JNI_TRUE;
        }
    }
    [pool drain];
    return JNI_FALSE;
}

static int isInAquaSession() {
    // environment variable to bypass the aqua session check
    char *ev = getenv("AWT_FORCE_HEADFUL");
    if (ev && (strncasecmp(ev, "true", 4) == 0)) {
        // if "true" then tell the caller we're in
        // an Aqua session without actually checking
        return 1;
    }
    // Is the WindowServer available?
    SecuritySessionId session_id;
    SessionAttributeBits session_info;
    OSStatus status = SessionGetInfo(callerSecuritySession, &session_id, &session_info);
    if (status == noErr) {
        if (session_info & sessionHasGraphicAccess) {
            return 1;
        }
    }
    return 0;
}

int
SplashInitPlatform(Splash * splash) {
    if (!isInAquaSession()) {
        return 0;
    }
    pthread_mutex_init(&splash->lock, NULL);

    splash->maskRequired = 0;


    //TODO: the following is too much of a hack but should work in 90% cases.
    //      besides we don't use device-dependent drawing, so probably
    //      that's very fine indeed
    splash->byteAlignment = 1;
    initFormat(&splash->screenFormat, 0xff << 8,
            0xff << 16, 0xff << 24, 0xff << 0);
    splash->screenFormat.byteOrder = 1 ?  BYTE_ORDER_LSBFIRST : BYTE_ORDER_MSBFIRST;
    splash->screenFormat.depthBytes = 4;

    // If we are running SWT we should not start a runLoop
    if (!isSWTRunning()) {
        [ThreadUtilities performOnMainThreadWaiting:NO block:^() {
            [NSApplicationAWT runAWTLoopWithApp:[NSApplicationAWT sharedApplication]];
        }];
    }
    return 1;
}

void
SplashCleanupPlatform(Splash * splash) {
    splash->maskRequired = 0;
}

void
SplashDonePlatform(Splash * splash) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    pthread_mutex_destroy(&splash->lock);
    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        if (splash->window) {
            [splash->window orderOut:nil];
            [splash->window release];
        }
    }];
    [pool drain];
}

void
SplashLock(Splash * splash) {
    pthread_mutex_lock(&splash->lock);
}

void
SplashUnlock(Splash * splash) {
    pthread_mutex_unlock(&splash->lock);
}

void
SplashInitFrameShape(Splash * splash, int imageIndex) {
    // No shapes, we rely on alpha compositing
}

void * SplashScreenThread(void *param);
void
SplashCreateThread(Splash * splash) {
    pthread_t thr;
    pthread_attr_t attr;
    int rc;

    pthread_attr_init(&attr);
    rc = pthread_create(&thr, &attr, SplashScreenThread, (void *) splash);
}

void
SplashRedrawWindow(Splash * splash) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        // drop the reference to the old view and image
        [splash->window setContentView: nil];
        SplashUpdateScreenData(splash);

        // NSDeviceRGBColorSpace vs. NSCalibratedRGBColorSpace ?
        NSBitmapImageRep * rep = [[NSBitmapImageRep alloc]
            initWithBitmapDataPlanes: (unsigned char**)&splash->screenData
                          pixelsWide: splash->width
                          pixelsHigh: splash->height
                       bitsPerSample: 8
                     samplesPerPixel: 4
                            hasAlpha: YES
                            isPlanar: NO
                      colorSpaceName: NSDeviceRGBColorSpace
                        bitmapFormat: NSAlphaFirstBitmapFormat | NSAlphaNonpremultipliedBitmapFormat
                         bytesPerRow: splash->width * 4
                        bitsPerPixel: 32];

        NSImage * image = [[NSImage alloc]
            initWithSize: NSMakeSize(splash->width, splash->height)];
        [image setBackgroundColor: [NSColor clearColor]];

        [image addRepresentation: rep];
        float scaleFactor = splash->scaleFactor;
        if (scaleFactor > 0 && scaleFactor != 1) {
            NSSize size = [image size];
            size.width /= scaleFactor;
            size.height /= scaleFactor;
            [image setSize: size];
        }

        NSImageView * view = [[NSImageView alloc] init];

        [view setImage: image];
        [view setEditable: NO];
        //NOTE: we don't set a 'wait cursor' for the view because:
        //      1. The Cocoa GUI guidelines suggest to avoid it, and use a progress
        //         bar instead.
        //      2. There simply isn't an instance of NSCursor that represent
        //         the 'wait cursor'. So that is undoable.

        //TODO: only the first image in an animated gif preserves transparency.
        //      Loos like the splash->screenData contains inappropriate data
        //      for all but the first frame.

        [image release];
        [rep release];

        [splash->window setContentView: view];
        [splash->window orderFrontRegardless];
    }];

    [pool drain];
}

void SplashReconfigureNow(Splash * splash) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        SplashCenter(splash);

        if (!splash->window) {
            return;
        }

        [splash->window orderOut:nil];
        [splash->window setFrame: NSMakeRect(splash->x, splash->y, splash->width, splash->height)
                         display: NO];
    }];

    [pool drain];

    SplashRedrawWindow(splash);
}

void
SplashEventLoop(Splash * splash) {

    /* we should have splash _locked_ on entry!!! */

    while (1) {
        struct pollfd pfd[1];
        int timeout = -1;
        int ctl = splash->controlpipe[0];
        int rc;
        int pipes_empty;

        pfd[0].fd = ctl;
        pfd[0].events = POLLIN | POLLPRI;

        errno = 0;
        if (splash->isVisible>0 && SplashIsStillLooping(splash)) {
            timeout = splash->time + splash->frames[splash->currentFrame].delay
                - SplashTime();
            if (timeout < 0) {
                timeout = 0;
            }
        }
        SplashUnlock(splash);
        rc = poll(pfd, 1, timeout);
        SplashLock(splash);
        if (splash->isVisible > 0 && splash->currentFrame >= 0 &&
                SplashTime() >= splash->time + splash->frames[splash->currentFrame].delay) {
            SplashNextFrame(splash);
            SplashRedrawWindow(splash);
        }
        if (rc <= 0) {
            errno = 0;
            continue;
        }
        pipes_empty = 0;
        while(!pipes_empty) {
            char buf;

            pipes_empty = 1;
            if (read(ctl, &buf, sizeof(buf)) > 0) {
                pipes_empty = 0;
                switch (buf) {
                case SPLASHCTL_UPDATE:
                    if (splash->isVisible>0) {
                        SplashRedrawWindow(splash);
                    }
                    break;
                case SPLASHCTL_RECONFIGURE:
                    if (splash->isVisible>0) {
                        SplashReconfigureNow(splash);
                    }
                    break;
                case SPLASHCTL_QUIT:
                    return;
                }
            }
        }
    }
}

void *
SplashScreenThread(void *param) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    Splash *splash = (Splash *) param;

    SplashLock(splash);
    pipe(splash->controlpipe);
    fcntl(splash->controlpipe[0], F_SETFL,
        fcntl(splash->controlpipe[0], F_GETFL, 0) | O_NONBLOCK);
    splash->time = SplashTime();
    splash->currentFrame = 0;
    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        SplashCenter(splash);

        splash->window = (void*) [[NSWindow alloc]
            initWithContentRect: NSMakeRect(splash->x, splash->y, splash->width, splash->height)
                      styleMask: NSBorderlessWindowMask
                        backing: NSBackingStoreBuffered
                          defer: NO
                         screen: SplashNSScreen()];

        [splash->window setOpaque: NO];
        [splash->window setBackgroundColor: [NSColor clearColor]];
    }];
    fflush(stdout);
    if (splash->window) {
        [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
            [splash->window orderFrontRegardless];
        }];
        SplashRedrawWindow(splash);
        SplashEventLoop(splash);
    }
    SplashUnlock(splash);
    SplashDone(splash);

    splash->isVisible=-1;

    [pool drain];

    return 0;
}

void
sendctl(Splash * splash, char code) {
    if (splash && splash->controlpipe[1]) {
        write(splash->controlpipe[1], &code, 1);
    }
}

void
SplashClosePlatform(Splash * splash) {
    sendctl(splash, SPLASHCTL_QUIT);
}

void
SplashUpdate(Splash * splash) {
    sendctl(splash, SPLASHCTL_UPDATE);
}

void
SplashReconfigure(Splash * splash) {
    sendctl(splash, SPLASHCTL_RECONFIGURE);
}

NSString* findScaledImageName(NSString *fileName, NSUInteger dotIndex, NSString *strToAppend) {
    NSString *fileName2x = nil;
    if (dotIndex == NSNotFound) {
        fileName2x = [fileName stringByAppendingString: strToAppend];
    } else {
        fileName2x = [fileName substringToIndex: dotIndex];
        fileName2x = [fileName2x stringByAppendingString: strToAppend];
        fileName2x = [fileName2x stringByAppendingString:
                      [fileName substringFromIndex: dotIndex]];
    }
    return fileName2x;
}

