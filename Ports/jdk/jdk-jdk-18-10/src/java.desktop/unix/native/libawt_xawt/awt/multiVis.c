/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
/** ------------------------------------------------------------------------
        This file contains functions to create a list of regions which
        tile a specified window.  Each region contains all visible
        portions of the window which are drawn with the same visual.
        If the window consists of subwindows of two different visual types,
        there will be two regions in the list.  The list can be traversed
        to correctly pull an image of the window using XGetImage or the
        Image Library.

 This file is available under and governed by the GNU General Public
 License version 2 only, as published by the Free Software Foundation.
 However, the following notice accompanied the original version of this
 file:

Copyright 1994 Hewlett-Packard Co.
Copyright 1996, 1998  The Open Group

Permission to use, copy, modify, distribute, and sell this software and its
documentation for any purpose is hereby granted without fee, provided that
the above copyright notice appear in all copies and that both that
copyright notice and this permission notice appear in supporting
documentation.

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE OPEN GROUP BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of The Open Group shall
not be used in advertising or otherwise to promote the sale, use or
other dealings in this Software without prior written authorization
from The Open Group.

    ------------------------------------------------------------------------ **/

#ifdef HEADLESS
    #error This file should not be included in headless library
#endif

#include <stdlib.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/X.h>
#include <stdio.h>
#include "list.h"
#include "wsutils.h"
#include "multiVis.h"
/* These structures are copied from X11/region.h.  For some reason
 * they're invisible from the outside.
 */
typedef struct {
    short x1, x2, y1, y2;
} myBox, myBOX, myBoxRec, *myBoxPtr;

typedef struct my_XRegion {
    long size;
    long numRects;
    myBOX *rects;
    myBOX extents;
} myREGION;

/* Items in long list of windows that have some part in the grabbed area */
typedef struct {
    Window win;
    Visual *vis;
    Colormap cmap;
    int x_rootrel, y_rootrel;   /* root relative location of window */
    int x_vis, y_vis;           /* rt rel x,y of vis part, not parent clipped */
    int width, height;          /* width and height of visible part */
    int border_width;           /* border width of the window */
    Window parent;              /* id of parent (for debugging) */
} image_win_type;

/*  Items in short list of regions that tile the grabbed area.  May have
    multiple windows in the region.
*/
typedef struct {
    Window win;                 /* lowest window of this visual */
    Visual *vis;
    Colormap cmap;
    int x_rootrel, y_rootrel;   /* root relative location of bottom window */
    int x_vis, y_vis;           /* rt rel x,y of vis part, not parent clipped */
    int width, height;          /* w & h of visible rect of bottom window */
    int border;                 /* border width of the window */
    Region visible_region;
} image_region_type;

/** ------------------------------------------------------------------------
        Returns TRUE if the two structs pointed to have the same "vis" &
        "cmap" fields and s2 lies completely within s1.  s1 and s2 can
        point to structs of image_win_type or image_region_type.
    ------------------------------------------------------------------------ **/
#define SAME_REGIONS( s1, s2)   \
        ((s1)->vis == (s2)->vis && (s1)->cmap == (s2)->cmap &&          \
         (s1)->x_vis <= (s2)->x_vis &&                              \
         (s1)->y_vis <= (s2)->y_vis &&                              \
         (s1)->x_vis + (s1)->width  >= (s2)->x_vis + (s2)->width && \
         (s1)->y_vis + (s1)->height >= (s2)->y_vis + (s2)->height)

#ifndef MIN
#define MIN( a, b)      ((a) < (b) ? a : b)
#define MAX( a, b)      ((a) > (b) ? a : b)
#endif

#define RED_SHIFT        16
#define GREEN_SHIFT       8
#define BLUE_SHIFT        0

/*
extern list_ptr new_list();
extern list_ptr dup_list_head();
extern void *   first_in_list();
extern void *   next_in_list();
extern int      add_to_list();
extern void     zero_list();
extern void     delete_list();
extern void     delete_list_destroying();
extern unsigned int list_length();
*/

/* Prototype Declarations for Static Functions */
static void QueryColorMap(
           Display *, Colormap , Visual *,
           XColor **, int *, int *, int *
           );
static void TransferImage(
           Display *, XImage *,int, int , image_region_type*,
           XImage *,int ,int
           );
static XImage * ReadRegionsInList(
           Display *, Visual *, int, int, unsigned int,
           unsigned int, XRectangle, list_ptr
           );

static list_ptr make_region_list(
                  Display*, Window, XRectangle*,
                  int*, int, XVisualInfo**, int *
         );

static void destroy_region_list(
            list_ptr
            ) ;
static void subtr_rect_from_image_region(
           image_region_type *, int , int , int , int
     );
static void add_rect_to_image_region(
           image_region_type *,
           int , int , int , int
     );
static int src_in_region_list(
    image_win_type *, list_ptr
    );
static void add_window_to_list(
    list_ptr, Window, int, int ,
    int , int , int , int, int,
    Visual*, Colormap, Window
    );
static int src_in_image(
    image_win_type      *, int  , XVisualInfo**
    );
static int src_in_overlay(
    image_region_type *, int, OverlayInfo *, int*, int*
    );
static void make_src_list(
    Display *, list_ptr, XRectangle *, Window,
    int, int, XWindowAttributes *, XRectangle *
);
static void destroy_image_region(
    image_region_type *
);

/* End of Prototype Declarations */

void initFakeVisual(Visual *Vis)
{
    Vis->ext_data=NULL;
    Vis->class = DirectColor ;
    Vis->red_mask =   0x00FF0000;
    Vis->green_mask = 0x0000FF00 ;
    Vis->blue_mask  = 0x000000FF ;
    Vis->map_entries = 256 ;
    Vis->bits_per_rgb = 8 ;
}

static void
QueryColorMap(Display *disp, Colormap src_cmap, Visual *src_vis,
              XColor **src_colors, int *rShift, int *gShift, int *bShift)
{
     unsigned int ncolors,i ;
     unsigned long       redMask, greenMask, blueMask;
     int                 redShift, greenShift, blueShift;
     XColor *colors ;

     ncolors = (unsigned) src_vis->map_entries ;
/* JDK modification.
 * use calloc instead of malloc to initialize allocated memory
 *   *src_colors = colors = (XColor *)malloc(ncolors * sizeof(XColor) ) ;
 */
     *src_colors = colors = (XColor *)calloc(ncolors, sizeof(XColor));

     if(src_vis->class != TrueColor && src_vis->class != DirectColor)
     {
         for(i=0 ; i < ncolors ; i++)
         {
                colors[i].pixel = i ;
                colors[i].pad = 0;
                colors[i].flags = DoRed|DoGreen|DoBlue;
         }
     }
     else /** src is decomposed rgb ***/
     {
        /* Get the X colormap */
        redMask = src_vis->red_mask;
        greenMask = src_vis->green_mask;
        blueMask = src_vis->blue_mask;
        redShift = 0; while (!(redMask&0x1)) {
                redShift++;
                redMask = redMask>>1;
        }
        greenShift = 0; while (!(greenMask&0x1)) {
                greenShift++;
                greenMask = greenMask>>1;
        }
        blueShift = 0; while (!(blueMask&0x1)) {
                blueShift++;
                blueMask = blueMask>>1;
        }
        *rShift = redShift ;
        *gShift = greenShift ;
        *bShift = blueShift ;
        for (i=0; i<ncolors; i++) {
                if( i <= redMask)colors[i].pixel = (i<<redShift) ;
                if( i <= greenMask)colors[i].pixel |= (i<<greenShift) ;
                if( i <= blueMask)colors[i].pixel |= (i<<blueShift) ;
                /***** example :for gecko's 3-3-2 map, blue index should be <= 3.
                colors[i].pixel = (i<<redShift)|(i<<greenShift)|(i<<blueShift);
                *****/
                colors[i].pad = 0;
                colors[i].flags = DoRed|DoGreen|DoBlue;
        }
      }

      XQueryColors(disp, src_cmap, colors, (int) ncolors);
}

int
GetMultiVisualRegions(Display *disp,
                      /* root win on which grab was done */
                      Window srcRootWinid,
                      /* root rel UL corner of bounding box of grab */
                      int x, int y,
                      /* size of bounding box of grab */
                      unsigned int width, unsigned int height,
                      int *transparentOverlays, int *numVisuals,
                      XVisualInfo **pVisuals, int *numOverlayVisuals,
                      OverlayInfo **pOverlayVisuals,
                      int *numImageVisuals, XVisualInfo ***pImageVisuals,
                      /* list of regions to read from */
                      list_ptr *vis_regions,
                      list_ptr *vis_image_regions, int *allImage)
{
    int                 hasNonDefault;
    XRectangle          bbox;           /* bounding box of grabbed area */


    bbox.x = x;                 /* init X rect for bounding box */
    bbox.y = y;
    bbox.width = width;
    bbox.height = height;

    GetXVisualInfo(disp,DefaultScreen(disp),
                    transparentOverlays,
                    numVisuals, pVisuals,
                    numOverlayVisuals, pOverlayVisuals,
                    numImageVisuals, pImageVisuals);

    *vis_regions = *vis_image_regions = NULL ;
    if ((*vis_regions = make_region_list( disp, srcRootWinid, &bbox,
                                         &hasNonDefault, *numImageVisuals,
                                         *pImageVisuals, allImage)) == NULL)
        return 0 ;

    if (*transparentOverlays)
    {
        *allImage = 1; /* until proven otherwise,
                         this flags that it to be an image only list */
        *vis_image_regions =
                make_region_list( disp, srcRootWinid, &bbox, &hasNonDefault,
                                        *numImageVisuals, *pImageVisuals, allImage);
    }

    /* if there is a second region in any of the two lists return 1 **/
    if ( ( *vis_regions && (*vis_regions)->next && (*vis_regions)->next->next ) ||
         ( *vis_image_regions && (*vis_image_regions)->next &&
           (*vis_image_regions)->next->next ) ) return 1 ;
    else return 0 ;

}

static void TransferImage(Display *disp, XImage *reg_image,
                          int srcw, int srch,
                          image_region_type *reg, XImage *target_image,
                          int dst_x, int dst_y)
{
    int i,j,old_pixel,new_pixel,red_ind,green_ind,blue_ind ;
    XColor *colors;
    int rShift = 0, gShift = 0, bShift = 0;

    QueryColorMap(disp,reg->cmap,reg->vis,&colors,
         &rShift,&gShift,&bShift) ;

    switch (reg->vis->class) {
    case TrueColor :
       for(i=0 ; i < srch ; i++)
       {
         for(j=0 ; j < srcw ;  j++)
         {
           old_pixel = XGetPixel(reg_image,j,i) ;

/*
 * JDK modification.
 * commented out since not using server RGB masks in all true color modes
 * causes the R and B values to be swapped around on some X servers
 *    - robi.khan@eng 9/7/1999
 *         if( reg->vis->map_entries == 16) {
 */
                 red_ind = (old_pixel & reg->vis->red_mask) >> rShift ;
                 green_ind = (old_pixel & reg->vis->green_mask) >> gShift ;
                 blue_ind = (old_pixel & reg->vis->blue_mask) >> bShift ;

                 new_pixel = (
                              ((colors[red_ind].red >> 8) << RED_SHIFT)
                              |((colors[green_ind].green >> 8) << GREEN_SHIFT)
                              |((colors[blue_ind].blue >> 8) << BLUE_SHIFT)
                             );
/* JDK modification.
 * else part of above modification
 *
 *         }
 *         else
 *              new_pixel = old_pixel;
 */

           XPutPixel(target_image,dst_x+j, dst_y+i,new_pixel);

         }
       }
       break;
    case DirectColor :
       for(i=0 ; i < srch ; i++)
       {

         for(j=0 ; j < srcw ;  j++)
         {
           old_pixel = XGetPixel(reg_image,j,i) ;
           red_ind = (old_pixel & reg->vis->red_mask) >> rShift ;
           green_ind = (old_pixel & reg->vis->green_mask) >> gShift ;
           blue_ind = (old_pixel & reg->vis->blue_mask) >> bShift ;

           new_pixel = (
                         ((colors[red_ind].red >> 8) << RED_SHIFT)
                        |((colors[green_ind].green >> 8) << GREEN_SHIFT)
                        |((colors[blue_ind].blue >> 8) << BLUE_SHIFT)
                       );
           XPutPixel(target_image,dst_x+j, dst_y+i,new_pixel);

         }
       }
       break;
    default :
       for(i=0 ; i < srch ; i++)
       {
         for(j=0 ; j < srcw ;  j++)
         {
            old_pixel = XGetPixel(reg_image,j,i) ;

           new_pixel = (
                         ((colors[old_pixel].red >> 8) << RED_SHIFT)
                        |((colors[old_pixel].green >> 8) << GREEN_SHIFT)
                        |((colors[old_pixel].blue >> 8) << BLUE_SHIFT)
                       );
           XPutPixel(target_image,dst_x+j, dst_y+i,new_pixel);

         }
       }
       break;
    }
 /* JDK modification
  * Fix memory leak by freeing colors
  *  - robi.khan@eng 9/22/1999
  */
    free(colors);
}

static XImage *
ReadRegionsInList(Display *disp, Visual *fakeVis, int depth, int format,
                  unsigned int width, unsigned int height,
                  XRectangle bbox,      /* bounding box of grabbed area */
                  list_ptr regions)     /* list of regions to read from */
{
    image_region_type   *reg;
    int                 dst_x, dst_y;   /* where in pixmap to write (UL) */
    int                 diff;

    XImage              *reg_image,*ximage ;
    int                 srcRect_x,srcRect_y,srcRect_width,srcRect_height ;
    int                 bytes_per_line;

    ximage = XCreateImage(disp,fakeVis,depth,format,0,NULL,width,height,
                 8,0) ;
    bytes_per_line = ximage->bytes_per_line;

    if (format == ZPixmap)
          ximage->data = malloc((size_t) height * bytes_per_line);
    else
        ximage->data = malloc((size_t) height * bytes_per_line * depth);

    ximage->bits_per_pixel = depth; /** Valid only if format is ZPixmap ***/

    for (reg = (image_region_type *) first_in_list( regions); reg;
         reg = (image_region_type *) next_in_list( regions))
    {
                int rect;
                struct my_XRegion *vis_reg;
                vis_reg = (struct my_XRegion *)(reg->visible_region);
                for (rect = 0;
                     rect < vis_reg->numRects;
                     rect++)
                {
                /** ------------------------------------------------------------------------
                        Intersect bbox with visible part of region giving src rect & output
                        location.  Width is the min right side minus the max left side.
                        Similar for height.  Offset src rect so x,y are relative to
                        origin of win, not the root-relative visible rect of win.
                    ------------------------------------------------------------------------ **/
                    srcRect_width  = MIN( vis_reg->rects[rect].x2, bbox.width + bbox.x) -
                                     MAX( vis_reg->rects[rect].x1, bbox.x);
                    srcRect_height = MIN( vis_reg->rects[rect].y2, bbox.height + bbox.y) -
                                     MAX( vis_reg->rects[rect].y1, bbox.y);
                    diff = bbox.x - vis_reg->rects[rect].x1;
                    srcRect_x = MAX( 0, diff)  + (vis_reg->rects[rect].x1 - reg->x_rootrel - reg->border);
                    dst_x     = MAX( 0, -diff) ;
                    diff = bbox.y - vis_reg->rects[rect].y1;
                    srcRect_y = MAX( 0, diff)  + (vis_reg->rects[rect].y1 - reg->y_rootrel - reg->border);
                    dst_y     = MAX( 0, -diff) ;
                    reg_image = XGetImage(disp,reg->win,srcRect_x,srcRect_y,
                                srcRect_width,srcRect_height,AllPlanes,format) ;

                    /* JDK Modification
                     * Enclose in if test and also call XDestroyImage
                     */
                    if (reg_image) {
                        TransferImage(disp,reg_image,srcRect_width,
                                     srcRect_height,reg,ximage,dst_x,dst_y) ;
                        XDestroyImage(reg_image);
                    }
            }
    }
    return ximage ;
}


/** ------------------------------------------------------------------------
    ------------------------------------------------------------------------ **/

XImage *ReadAreaToImage(Display *disp,
                        /* root win on which grab was done */
                        Window srcRootWinid,
                        /* root rel UL corner of bounding box of grab */
                        int x, int y,
                        /* size of bounding box of grab */
                        unsigned int width, unsigned int height,
                        int numVisuals, XVisualInfo *pVisuals,
                        int numOverlayVisuals, OverlayInfo *pOverlayVisuals,
                        int numImageVisuals, XVisualInfo **pImageVisuals,
                        /* list of regions to read from */
                        list_ptr vis_regions,
                        /* list of regions to read from */
                        list_ptr vis_image_regions,
                        int format, int allImage)
{
    image_region_type   *reg;
    XRectangle          bbox;           /* bounding box of grabbed area */
    int                 depth ;
    XImage              *ximage, *ximage_ipm = NULL;
    Visual              fakeVis ;
    int         x1, y1;
    XImage      *image;
#if 0
    unsigned char       *pmData ,  *ipmData ;
#endif
    int                 transparentColor, transparentType;
    int                 srcRect_x,srcRect_y,srcRect_width,srcRect_height ;
    int                 diff ;
    int                 dst_x, dst_y;   /* where in pixmap to write (UL) */
    int                 pixel;

    bbox.x = x;                 /* init X rect for bounding box */
    bbox.y = y;
    bbox.width = width;
    bbox.height = height;


    initFakeVisual(&fakeVis) ;

    depth = 24 ;
    ximage = ReadRegionsInList(disp,&fakeVis,depth,format,width,height,
             bbox,vis_regions) ;
#if 0
    pmData = (unsigned char *)ximage -> data ;
#endif

/* if transparency possible do it again, but this time for image planes only */
    if (vis_image_regions && (vis_image_regions->next) && !allImage)
    {
        ximage_ipm = ReadRegionsInList(disp,&fakeVis,depth,format,width,height,
                     bbox,vis_image_regions) ;
#if 0
        ipmData = (unsigned char *)ximage_ipm -> data ;
#endif
    }
/* Now tranverse the overlay visual windows and test for transparency index.  */
/* If you find one, subsitute the value from the matching image plane pixmap. */

    for (reg = (image_region_type *) first_in_list( vis_regions); reg;
         reg = (image_region_type *) next_in_list( vis_regions))
    {

        if (src_in_overlay( reg, numOverlayVisuals, pOverlayVisuals,
                                 &transparentColor, &transparentType))
        {
        int test = 0 ;
             srcRect_width  = MIN( reg->width + reg->x_vis, bbox.width + bbox.x)
                                 - MAX( reg->x_vis, bbox.x);
             srcRect_height = MIN( reg->height + reg->y_vis, bbox.height
                                 + bbox.y) - MAX( reg->y_vis, bbox.y);
             diff = bbox.x - reg->x_vis;
             srcRect_x = MAX( 0, diff)  + (reg->x_vis - reg->x_rootrel - reg->border);
             dst_x     = MAX( 0, -diff) ;
             diff = bbox.y - reg->y_vis;
             srcRect_y = MAX( 0, diff)  + (reg->y_vis - reg->y_rootrel - reg->border);
             dst_y     = MAX( 0, -diff) ;
        /* let's test some pixels for transparency */
             image = XGetImage(disp, reg->win, srcRect_x, srcRect_y,
                 srcRect_width, srcRect_height, 0xffffffff, ZPixmap);

        /* let's assume byte per pixel for overlay image for now */
             if ((image->depth == 8) && (transparentType == TransparentPixel))
             {
                 unsigned char *pixel_ptr;
                 unsigned char *start_of_line = (unsigned char *) image->data;

                 for (y1 = 0; y1 < srcRect_height; y1++) {
                    pixel_ptr = start_of_line;
                    for (x1 = 0; x1 < srcRect_width; x1++)
                    {
                        if (*pixel_ptr++ == transparentColor)
                        {
#if 0
                            *pmData++ = *ipmData++;
                            *pmData++ = *ipmData++;
                            *pmData++ = *ipmData++;
#endif
                        pixel = XGetPixel(ximage_ipm,dst_x+x1,dst_y+y1) ;
                        XPutPixel(ximage,dst_x+x1, dst_y+y1,pixel);

                        if(!test){
                           test = 1 ;
                        }
                        }
#if 0
                        else {
                            pmData +=3;
                            ipmData +=3;
                        }
#endif
                    }
                    start_of_line += image->bytes_per_line;
                }
        } else {
                if (transparentType == TransparentPixel) {
                for (y1 = 0; y1 < srcRect_height; y1++) {
                      for (x1 = 0; x1 < srcRect_width; x1++)
                      {
                            int pixel_value = XGetPixel(image, x1, y1);
                            if (pixel_value == transparentColor)
                            {
#if 0
                                *pmData++ = *ipmData++;
                                *pmData++ = *ipmData++;
                                *pmData++ = *ipmData++;
#endif
                        pixel = XGetPixel(ximage_ipm,dst_x+x1,dst_y+y1) ;
                        XPutPixel(ximage,dst_x+x1, dst_y+y1,pixel);
                        if(!test){
                           test = 1 ;
                        }
                            }
#if 0
                            else {
                                pmData +=3;
                                ipmData +=3;
                            }
#endif
                        }
                    }
                } else {
                    for (y1 = 0; y1 < srcRect_height; y1++) {
                        for (x1 = 0; x1 < srcRect_width; x1++)
                        {
                            int pixel_value = XGetPixel(image, x1, y1);
                            if (pixel_value & transparentColor)
                            {
#if 0
                                *pmData++ = *ipmData++;
                                *pmData++ = *ipmData++;
                                *pmData++ = *ipmData++;
#endif
                                pixel = XGetPixel(ximage_ipm,dst_x+x1,dst_y+y1) ;
                                XPutPixel(ximage,dst_x+x1, dst_y+y1,pixel);
                        if(!test){
                           test = 1 ;
                        }
                            }
#if 0
                            else {
                                pmData +=3;
                                ipmData +=3;
                            }
#endif
                        }
                    }
                }
        }
        XDestroyImage (image);
      } /* end of src_in_overlay */
    } /** end transparency **/
    /* JDK modification - call XDestroyImage if non-null */
    if (ximage_ipm != NULL) {
        XDestroyImage(ximage_ipm);
    }
    destroy_region_list( vis_regions);
    if (vis_image_regions) destroy_region_list( vis_image_regions );
    FreeXVisualInfo(pVisuals, pOverlayVisuals, pImageVisuals);
    XSync(disp, 0);

    return ximage;
}

/** ------------------------------------------------------------------------
        Creates a list of the subwindows of a given window which have a
        different visual than their parents.  The function is recursive.
        This list is used in make_region_list(), which coalesces the
        windows with the same visual into a region.
        image_wins must point to an existing list struct that's already
        been zeroed (zero_list()).
    ------------------------------------------------------------------------ **/
static void make_src_list(Display *disp, list_ptr image_wins,
                          /* bnding box of area we want */
                          XRectangle *bbox,
                          Window curr,
                          /* pos of curr WRT root */
                          int x_rootrel, int y_rootrel,
                          XWindowAttributes *curr_attrs,
                          /* visible part of curr, not obscurred by ancestors */
                          XRectangle *pclip)
{
    XWindowAttributes child_attrs;
    Window root, parent, *child;        /* variables for XQueryTree() */
    Window *save_child_list;            /* variables for XQueryTree() */
    unsigned int nchild;                /* variables for XQueryTree() */
    XRectangle child_clip;              /* vis part of child */
    int curr_clipX, curr_clipY, curr_clipRt, curr_clipBt;

    /* check that win is mapped & not outside bounding box */
    if (curr_attrs->map_state == IsViewable &&
        curr_attrs->class == InputOutput &&
        !( pclip->x >= (int) (bbox->x + bbox->width)    ||
           pclip->y >= (int) (bbox->y + bbox->height)   ||
           (int) (pclip->x + pclip->width)  <= bbox->x  ||
           (int) (pclip->y + pclip->height) <= bbox->y)) {

        XQueryTree( disp, curr, &root, &parent, &child, &nchild );
        save_child_list = child;      /* so we can free list when we're done */
        add_window_to_list( image_wins, curr, x_rootrel, y_rootrel,
                            pclip->x, pclip->y,
                            pclip->width, pclip->height,
                            curr_attrs->border_width,curr_attrs->visual,
                            curr_attrs->colormap, parent);


/** ------------------------------------------------------------------------
        set RR coords of right (Rt), left (X), bottom (Bt) and top (Y)
        of rect we clip all children by.  This is our own clip rect (pclip)
        inflicted on us by our parent plus our own borders.  Within the
        child loop, we figure the clip rect for each child by adding in
        it's rectangle (not taking into account the child's borders).
    ------------------------------------------------------------------------ **/
        curr_clipX = MAX( pclip->x, x_rootrel + (int) curr_attrs->border_width);
        curr_clipY = MAX( pclip->y, y_rootrel + (int) curr_attrs->border_width);
        curr_clipRt = MIN( pclip->x + (int) pclip->width,
                           x_rootrel + (int) curr_attrs->width +
                           2 * (int) curr_attrs->border_width);
        curr_clipBt = MIN( pclip->y + (int) pclip->height,
                           y_rootrel + (int) curr_attrs->height +
                           2 * (int) curr_attrs->border_width);

        while (nchild--) {
            int new_width, new_height;
            int child_xrr, child_yrr;   /* root relative x & y of child */

            XGetWindowAttributes( disp, *child, &child_attrs);

            /* intersect parent & child clip rects */
            child_xrr = x_rootrel + child_attrs.x + curr_attrs->border_width;
            child_clip.x = MAX( curr_clipX, child_xrr);
            new_width = MIN( curr_clipRt, child_xrr + (int) child_attrs.width
                             + 2 * child_attrs.border_width)
                        - child_clip.x;
            if (new_width >= 0) {
                child_clip.width = new_width;

                child_yrr = y_rootrel + child_attrs.y +
                            curr_attrs->border_width;
                child_clip.y = MAX( curr_clipY, child_yrr);
                new_height = MIN( curr_clipBt,
                                  child_yrr + (int) child_attrs.height +
                                      2 * child_attrs.border_width)
                             - child_clip.y;
                if (new_height >= 0) {
                    child_clip.height = new_height;
                    make_src_list( disp, image_wins, bbox, *child,
                                   child_xrr, child_yrr,
                                   &child_attrs, &child_clip);
                }
            }
            child++;
        }
        XFree( save_child_list);
    }
}


/** ------------------------------------------------------------------------
        This function creates a list of regions which tile a specified
        window.  Each region contains all visible portions of the window
        which are drawn with the same visual.  For example, if the
        window consists of subwindows of two different visual types,
        there will be two regions in the list.
        Returns a pointer to the list.
    ------------------------------------------------------------------------ **/
static list_ptr make_region_list(Display *disp, Window win, XRectangle *bbox,
                                 int *hasNonDefault, int numImageVisuals,
                                 XVisualInfo **pImageVisuals, int *allImage)
{
    XWindowAttributes   win_attrs;
    list                image_wins;
    list_ptr            image_regions;
    list_ptr            srcs_left;
    image_region_type   *new_reg;
    image_win_type      *base_src, *src;
    Region              bbox_region = XCreateRegion();
    XRectangle          clip;
    int                 image_only;

    int                 count=0 ;

    *hasNonDefault = False;
    XUnionRectWithRegion( bbox, bbox_region, bbox_region);
    XGetWindowAttributes( disp, win, &win_attrs);

    zero_list( &image_wins);
    clip.x = 0;
    clip.y = 0;
    clip.width  = win_attrs.width;
    clip.height = win_attrs.height;
    make_src_list( disp, &image_wins, bbox, win,
                   0 /* x_rootrel */, 0 /* y_rootrel */, &win_attrs, &clip);

    image_regions = new_list();
    image_only = (*allImage) ? True:False;

    for (base_src = (image_win_type *) first_in_list( &image_wins); base_src;
         base_src = (image_win_type *) next_in_list( &image_wins))
    {
        /* test for image visual */
        if (!image_only || src_in_image(base_src, numImageVisuals, pImageVisuals))
        {
            /* find a window whose visual hasn't been put in list yet */
            if (!src_in_region_list( base_src, image_regions))
            {
                if (! (new_reg = (image_region_type *)
                                        malloc( sizeof( image_region_type)))) {
                    return (list_ptr) NULL;
                }
                count++;

                new_reg->visible_region = XCreateRegion();
                new_reg->win            = base_src->win;
                new_reg->vis            = base_src->vis;
                new_reg->cmap           = base_src->cmap;
                new_reg->x_rootrel      = base_src->x_rootrel;
                new_reg->y_rootrel      = base_src->y_rootrel;
                new_reg->x_vis          = base_src->x_vis;
                new_reg->y_vis          = base_src->y_vis;
                new_reg->width          = base_src->width;
                new_reg->height         = base_src->height;
                new_reg->border         = base_src->border_width;

                srcs_left = (list_ptr) dup_list_head( &image_wins, START_AT_CURR);
                for (src = (image_win_type *) first_in_list( srcs_left); src;
                     src = (image_win_type *) next_in_list( srcs_left)) {
                    if (SAME_REGIONS( base_src, src)) {
                        add_rect_to_image_region( new_reg, src->x_vis, src->y_vis,
                                                  src->width, src->height);
                    }
                    else {
                        if (!image_only || src_in_image(src, numImageVisuals, pImageVisuals))
                        {
                            subtr_rect_from_image_region( new_reg, src->x_vis,
                                          src->y_vis, src->width, src->height);
                        }
                    }
                }
                XIntersectRegion( bbox_region, new_reg->visible_region,
                                  new_reg->visible_region);
                if (! XEmptyRegion( new_reg->visible_region)) {
                    add_to_list( image_regions, new_reg);
                    if (new_reg->vis != DefaultVisualOfScreen( win_attrs.screen) ||
                        new_reg->cmap != DefaultColormapOfScreen(
                                                            win_attrs.screen)) {
                        *hasNonDefault = True;
                    }
                }
                else {
                    XDestroyRegion( new_reg->visible_region);
                    free( (void *) new_reg);
                }
            }
        } else *allImage = 0;
    }
    delete_list( &image_wins, True);
    XDestroyRegion( bbox_region);
    return image_regions;
}
/** ------------------------------------------------------------------------
        Destructor called from destroy_region_list().
    ------------------------------------------------------------------------ **/
static void destroy_image_region(image_region_type *image_region)
{
    XDestroyRegion( image_region->visible_region);
    free( (void *) image_region);
}

/** ------------------------------------------------------------------------
        Destroys the region list, destroying all the regions contained in it.
    ------------------------------------------------------------------------ **/
static void destroy_region_list(list_ptr rlist)
{
    delete_list_destroying( rlist, (DESTRUCT_FUNC_PTR)destroy_image_region);
}


/** ------------------------------------------------------------------------
        Subtracts the specified rectangle from the region in image_region.
        First converts the rectangle to a region of its own, since X
        only provides a way to subtract one region from another, not a
        rectangle from a region.
    ------------------------------------------------------------------------ **/
static void subtr_rect_from_image_region(image_region_type *image_region,
                                         int x, int y, int width, int height)
{
    XRectangle rect;
    Region rect_region;

    rect_region = XCreateRegion();
    rect.x = x;
    rect.y = y;
    rect.width = width;
    rect.height = height;
    XUnionRectWithRegion( &rect, rect_region, rect_region);
    XSubtractRegion( image_region->visible_region, rect_region,
                     image_region->visible_region);
    XDestroyRegion( rect_region);
}


/** ------------------------------------------------------------------------
        Adds the specified rectangle to the region in image_region.
    ------------------------------------------------------------------------ **/
static void add_rect_to_image_region(image_region_type *image_region,
                                     int x, int y, int width, int height)
{
    XRectangle rect;

    rect.x = x;
    rect.y = y;
    rect.width = width;
    rect.height = height;
    XUnionRectWithRegion( &rect, image_region->visible_region,
                          image_region->visible_region);
}


/** ------------------------------------------------------------------------
        Returns TRUE if the given src's visual is already represented in
        the image_regions list, FALSE otherwise.
    ------------------------------------------------------------------------ **/
static int src_in_region_list(image_win_type *src, list_ptr image_regions)
{
    image_region_type   *ir;

    for (ir = (image_region_type *) first_in_list( image_regions); ir;
         ir = (image_region_type *) next_in_list( image_regions)) {
        if (SAME_REGIONS( ir, src)) {

            return 1;
        }
    }

    return 0;
}


/** ------------------------------------------------------------------------
        Makes a new entry in image_wins with the given fields filled in.
    ------------------------------------------------------------------------ **/
static void add_window_to_list(list_ptr image_wins, Window w,
                               int xrr, int yrr, int x_vis, int y_vis,
                               int width, int height, int border_width,
                               Visual *vis, Colormap cmap, Window parent)
{
    image_win_type      *new_src;

    if ((new_src = (image_win_type *) malloc( sizeof( image_win_type))) == NULL)

        return;

    new_src->win = w;
    new_src->x_rootrel = xrr;
    new_src->y_rootrel = yrr;
    new_src->x_vis = x_vis;
    new_src->y_vis = y_vis;
    new_src->width = width;
    new_src->height = height;
    new_src->border_width = border_width;
    new_src->vis = vis;
    new_src->cmap = cmap;
    new_src->parent = parent;
    add_to_list( image_wins, new_src);
}

/** ------------------------------------------------------------------------
        Returns TRUE if the given src's visual is in the image planes,
        FALSE otherwise.
    ------------------------------------------------------------------------ **/
static int src_in_image(image_win_type *src, int numImageVisuals,
                        XVisualInfo **pImageVisuals)
{
    int                 i;

    for (i = 0 ; i < numImageVisuals ; i++)
    {
        if (pImageVisuals[i]->visual == src->vis)
            return 1;
    }
    return 0;
}


/** ------------------------------------------------------------------------
        Returns TRUE if the given src's visual is in the overlay planes
        and transparency is possible, FALSE otherwise.
    ------------------------------------------------------------------------ **/
static int src_in_overlay(image_region_type *src, int numOverlayVisuals,
                          OverlayInfo *pOverlayVisuals,
                          int *transparentColor, int *transparentType)
{
    int                 i;

    for (i = 0 ; i < numOverlayVisuals ; i++)
    {
        if (((pOverlayVisuals[i].pOverlayVisualInfo)->visual == src->vis)
                && (pOverlayVisuals[i].transparentType != None))
        {
            *transparentColor = pOverlayVisuals[i].value;
            *transparentType = pOverlayVisuals[i].transparentType;
            return 1;
        }

        else {
        }

    }
    return 0;
}


/********************** from wsutils.c ******************************/

/******************************************************************************
 *
 * This file contains a set of example utility procedures; procedures that can
 * help a "window-smart" Starbase or PHIGS program determine information about
 * a device, and create image and overlay plane windows.  To use these
 * utilities, #include "wsutils.h" and compile this file and link the results
 * with your program.
 *
 ******************************************************************************/



#define STATIC_GRAY     0x01
#define GRAY_SCALE      0x02
#define PSEUDO_COLOR    0x04
#define TRUE_COLOR      0x10
#define DIRECT_COLOR    0x11


static int      weCreateServerOverlayVisualsProperty = False;


/******************************************************************************
 *
 * GetXVisualInfo()
 *
 * This routine takes an X11 Display, screen number, and returns whether the
 * screen supports transparent overlays and three arrays:
 *
 *      1) All of the XVisualInfo struct's for the screen.
 *      2) All of the OverlayInfo struct's for the screen.
 *      3) An array of pointers to the screen's image plane XVisualInfo
 *         structs.
 *
 * The code below obtains the array of all the screen's visuals, and obtains
 * the array of all the screen's overlay visual information.  It then processes
 * the array of the screen's visuals, determining whether the visual is an
 * overlay or image visual.
 *
 * If the routine sucessfully obtained the visual information, it returns zero.
 * If the routine didn't obtain the visual information, it returns non-zero.
 *
 ******************************************************************************/

int GetXVisualInfo(/* Which X server (aka "display"). */
                   Display *display,
                   /* Which screen of the "display". */
                   int screen,
                   /* Non-zero if there's at least one overlay visual and
                    * if at least one of those supports a transparent pixel. */
                   int *transparentOverlays,
                   /* Number of XVisualInfo struct's pointed to by pVisuals. */
                   int *numVisuals,
                   /* All of the device's visuals. */
                   XVisualInfo **pVisuals,
                   /* Number of OverlayInfo's pointed to by pOverlayVisuals.
                    * If this number is zero, the device does not have
                    * overlay planes. */
                   int *numOverlayVisuals,
                   /* The device's overlay plane visual information. */
                   OverlayInfo  **pOverlayVisuals,
                   /* Number of XVisualInfo's pointed to by pImageVisuals. */
                   int *numImageVisuals,
                   /* The device's image visuals. */
                   XVisualInfo ***pImageVisuals)
{
    XVisualInfo getVisInfo;             /* Paramters of XGetVisualInfo */
    int         mask;
    XVisualInfo *pVis, **pIVis;         /* Faster, local copies */
    OverlayInfo *pOVis;
    OverlayVisualPropertyRec    *pOOldVis;
    int         nVisuals, nOVisuals;
    Atom        overlayVisualsAtom;     /* Parameters for XGetWindowProperty */
    Atom        actualType;
    unsigned long numLongs, bytesAfter;
    int         actualFormat;
    int         nImageVisualsAlloced;   /* Values to process the XVisualInfo */
    int         imageVisual;            /* array */


    /* First, get the list of visuals for this screen. */
    getVisInfo.screen = screen;
    mask = VisualScreenMask;

    *pVisuals = XGetVisualInfo(display, mask, &getVisInfo, numVisuals);
    if ((nVisuals = *numVisuals) <= 0)
    {
        /* Return that the information wasn't sucessfully obtained: */
        return(1);
    }
    pVis = *pVisuals;


    /* Now, get the overlay visual information for this screen.  To obtain
     * this information, get the SERVER_OVERLAY_VISUALS property.
     */
    overlayVisualsAtom = XInternAtom(display, "SERVER_OVERLAY_VISUALS", True);
    if (overlayVisualsAtom != None)
    {
        /* Since the Atom exists, we can request the property's contents.  The
         * do-while loop makes sure we get the entire list from the X server.
         */
        bytesAfter = 0;
        numLongs = sizeof(OverlayVisualPropertyRec) / sizeof(long);
        do
        {
            numLongs += bytesAfter * sizeof(long);
            XGetWindowProperty(display, RootWindow(display, screen),
                               overlayVisualsAtom, 0, numLongs, False,
                               overlayVisualsAtom, &actualType, &actualFormat,
                               &numLongs, &bytesAfter, (unsigned char**) pOverlayVisuals);
        } while (bytesAfter > 0);


        /* Calculate the number of overlay visuals in the list. */
        *numOverlayVisuals = numLongs / (sizeof(OverlayVisualPropertyRec) / sizeof(long));
    }
    else
    {
        /* This screen doesn't have overlay planes. */
        *numOverlayVisuals = 0;
        *pOverlayVisuals = NULL;
        *transparentOverlays = 0;
    }


    /* Process the pVisuals array. */
    *numImageVisuals = 0;
    nImageVisualsAlloced = 1;
    pIVis = *pImageVisuals = (XVisualInfo **) malloc(sizeof(XVisualInfo *));
    while (--nVisuals >= 0)
    {
        nOVisuals = *numOverlayVisuals;
        pOVis = *pOverlayVisuals;
        imageVisual = True;
        while (--nOVisuals >= 0)
        {
            pOOldVis = (OverlayVisualPropertyRec *) pOVis;
            if (pVis->visualid == pOOldVis->visualID)
            {
                imageVisual = False;
                pOVis->pOverlayVisualInfo = pVis;
                if (pOVis->transparentType == TransparentPixel)
                    *transparentOverlays = 1;
            }
            pOVis++;
        }
        if (imageVisual)
        {
            if ((*numImageVisuals += 1) > nImageVisualsAlloced)
            {
                nImageVisualsAlloced++;
                *pImageVisuals = (XVisualInfo **)
                    realloc(*pImageVisuals, (nImageVisualsAlloced * sizeof(XVisualInfo *)));
                pIVis = *pImageVisuals + (*numImageVisuals - 1);
            }
            *pIVis++ = pVis;
        }
        pVis++;
    }


    /* Return that the information was sucessfully obtained: */
    return(0);

} /* GetXVisualInfo() */


/******************************************************************************
 *
 * FreeXVisualInfo()
 *
 * This routine frees the data that was allocated by GetXVisualInfo().
 *
 ******************************************************************************/

void FreeXVisualInfo(XVisualInfo *pVisuals, OverlayInfo *pOverlayVisuals,
                     XVisualInfo **pImageVisuals)
{
    XFree(pVisuals);
    if (weCreateServerOverlayVisualsProperty)
        free(pOverlayVisuals);
    else
        XFree(pOverlayVisuals);
    free(pImageVisuals);

} /* FreeXVisualInfo() */
