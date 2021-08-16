/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#import "CommonComponentAccessibility.h"
#import <AppKit/AppKit.h>
#import <JavaRuntimeSupport/JavaRuntimeSupport.h>
#import <dlfcn.h>
#import "JavaAccessibilityAction.h"
#import "JavaAccessibilityUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"
#import "AWTView.h"
#import "sun_lwawt_macosx_CAccessible.h"
#import "sun_lwawt_macosx_CAccessibility.h"


// GET* macros defined in JavaAccessibilityUtilities.h, so they can be shared.
static jclass sjc_CAccessibility = NULL;

static jmethodID sjm_getAccessibleName = NULL;
#define GET_ACCESSIBLENAME_METHOD_RETURN(ret) \
    GET_CACCESSIBILITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(sjm_getAccessibleName, sjc_CAccessibility, "getAccessibleName", \
                     "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljava/lang/String;", ret);

static jmethodID jm_getChildrenAndRoles = NULL;
#define GET_CHILDRENANDROLES_METHOD_RETURN(ret) \
    GET_CACCESSIBILITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(jm_getChildrenAndRoles, sjc_CAccessibility, "getChildrenAndRoles",\
                      "(Ljavax/accessibility/Accessible;Ljava/awt/Component;IZ)[Ljava/lang/Object;", ret);

static jmethodID jm_getChildrenAndRolesRecursive = NULL;
#define GET_CHILDRENANDROLESRECURSIVE_METHOD_RETURN(ret) \
    GET_CACCESSIBILITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(jm_getChildrenAndRolesRecursive, sjc_CAccessibility, "getChildrenAndRolesRecursive",\
                      "(Ljavax/accessibility/Accessible;Ljava/awt/Component;IZI)[Ljava/lang/Object;", ret);

static jmethodID sjm_getAccessibleComponent = NULL;
#define GET_ACCESSIBLECOMPONENT_STATIC_METHOD_RETURN(ret) \
    GET_CACCESSIBILITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(sjm_getAccessibleComponent, sjc_CAccessibility, "getAccessibleComponent", \
           "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljavax/accessibility/AccessibleComponent;", ret);

static jmethodID sjm_getAccessibleIndexInParent = NULL;
#define GET_ACCESSIBLEINDEXINPARENT_STATIC_METHOD_RETURN(ret) \
    GET_CACCESSIBILITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(sjm_getAccessibleIndexInParent, sjc_CAccessibility, "getAccessibleIndexInParent", \
                             "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)I", ret);

static jclass sjc_CAccessible = NULL;
#define GET_CACCESSIBLE_CLASS_RETURN(ret) \
    GET_CLASS_RETURN(sjc_CAccessible, "sun/lwawt/macosx/CAccessible", ret);

static NSMutableDictionary * _Nullable rolesMap;
static NSMutableDictionary * _Nullable rowRolesMapForParent;
NSString *const IgnoreClassName = @"IgnoreAccessibility";
static jobject sAccessibilityClass = NULL;

/*
 * Common ancestor for all the accessibility peers that implements the new method-based accessibility API
 */
@implementation CommonComponentAccessibility

- (BOOL)isMenu
{
    id role = [self accessibilityRole];
    return [role isEqualToString:NSAccessibilityMenuBarRole] || [role isEqualToString:NSAccessibilityMenuRole] || [role isEqualToString:NSAccessibilityMenuItemRole];
}

- (BOOL)isSelected:(JNIEnv *)env
{
    if (fIndex == -1) {
        return NO;
    }

    return isChildSelected(env, ((CommonComponentAccessibility *)[self parent])->fAccessible, fIndex, fComponent);
}

- (BOOL)isSelectable:(JNIEnv *)env
{
    jobject axContext = [self axContextWithEnv:env];
    BOOL selectable = isSelectable(env, axContext, fComponent);
    (*env)->DeleteLocalRef(env, axContext);
    return selectable;
}

- (BOOL)isVisible:(JNIEnv *)env
{
    if (fIndex == -1) {
        return NO;
    }

    jobject axContext = [self axContextWithEnv:env];
    BOOL showing = isShowing(env, axContext, fComponent);
    (*env)->DeleteLocalRef(env, axContext);
    return showing;
}

+ (void) initializeRolesMap {
    /*
     * Here we should keep all the mapping between the accessibility roles and implementing classes
     */
    rolesMap = [[NSMutableDictionary alloc] initWithCapacity:46];

    [rolesMap setObject:@"ButtonAccessibility" forKey:@"pushbutton"];
    [rolesMap setObject:@"ImageAccessibility" forKey:@"icon"];
    [rolesMap setObject:@"ImageAccessibility" forKey:@"desktopicon"];
    [rolesMap setObject:@"SpinboxAccessibility" forKey:@"spinbox"];
    [rolesMap setObject:@"StaticTextAccessibility" forKey:@"hyperlink"];
    [rolesMap setObject:@"StaticTextAccessibility" forKey:@"label"];
    [rolesMap setObject:@"RadiobuttonAccessibility" forKey:@"radiobutton"];
    [rolesMap setObject:@"CheckboxAccessibility" forKey:@"checkbox"];
    [rolesMap setObject:@"SliderAccessibility" forKey:@"slider"];
    [rolesMap setObject:@"ScrollAreaAccessibility" forKey:@"scrollpane"];
    [rolesMap setObject:@"ScrollBarAccessibility" forKey:@"scrollbar"];
    [rolesMap setObject:@"GroupAccessibility" forKey:@"awtcomponent"];
    [rolesMap setObject:@"GroupAccessibility" forKey:@"canvas"];
    [rolesMap setObject:@"GroupAccessibility" forKey:@"groupbox"];
    [rolesMap setObject:@"GroupAccessibility" forKey:@"internalframe"];
    [rolesMap setObject:@"GroupAccessibility" forKey:@"swingcomponent"];
    [rolesMap setObject:@"ToolbarAccessibility" forKey:@"toolbar"];
    [rolesMap setObject:@"SplitpaneAccessibility" forKey:@"splitpane"];
    [rolesMap setObject:@"StatusbarAccessibility" forKey:@"statusbar"];
    [rolesMap setObject:@"NavigableTextAccessibility" forKey:@"textarea"];
    [rolesMap setObject:@"NavigableTextAccessibility" forKey:@"text"];
    [rolesMap setObject:@"NavigableTextAccessibility" forKey:@"passwordtext"];
    [rolesMap setObject:@"NavigableTextAccessibility" forKey:@"dateeditor"];
    [rolesMap setObject:@"ComboBoxAccessibility" forKey:@"combobox"];
    [rolesMap setObject:@"TabGroupAccessibility" forKey:@"pagetablist"];
    [rolesMap setObject:@"ListAccessibility" forKey:@"list"];
    [rolesMap setObject:@"OutlineAccessibility" forKey:@"tree"];
    [rolesMap setObject:@"TableAccessibility" forKey:@"table"];

    /*
     * All the components below should be ignored by the accessibility subsystem,
     * If any of the enclosed component asks for a parent the first ancestor
     * participating in accessibility exchange should be returned.
     */
    [rolesMap setObject:IgnoreClassName forKey:@"alert"];
    [rolesMap setObject:IgnoreClassName forKey:@"colorchooser"];
    [rolesMap setObject:IgnoreClassName forKey:@"desktoppane"];
    [rolesMap setObject:IgnoreClassName forKey:@"dialog"];
    [rolesMap setObject:IgnoreClassName forKey:@"directorypane"];
    [rolesMap setObject:IgnoreClassName forKey:@"filechooser"];
    [rolesMap setObject:IgnoreClassName forKey:@"filler"];
    [rolesMap setObject:IgnoreClassName forKey:@"fontchooser"];
    [rolesMap setObject:IgnoreClassName forKey:@"frame"];
    [rolesMap setObject:IgnoreClassName forKey:@"glasspane"];
    [rolesMap setObject:IgnoreClassName forKey:@"layeredpane"];
    [rolesMap setObject:IgnoreClassName forKey:@"optionpane"];
    [rolesMap setObject:IgnoreClassName forKey:@"panel"];
    [rolesMap setObject:IgnoreClassName forKey:@"rootpane"];
    [rolesMap setObject:IgnoreClassName forKey:@"separator"];
    [rolesMap setObject:IgnoreClassName forKey:@"tooltip"];
    [rolesMap setObject:IgnoreClassName forKey:@"viewport"];
    [rolesMap setObject:IgnoreClassName forKey:@"window"];

    rowRolesMapForParent = [[NSMutableDictionary alloc] initWithCapacity:2];

    [rowRolesMapForParent setObject:@"ListRowAccessibility" forKey:@"ListAccessibility"];
    [rowRolesMapForParent setObject:@"OutlineRowAccessibility" forKey:@"OutlineAccessibility"];

    /*
     * Initialize CAccessibility instance
     */
#ifdef JAVA_AX_NO_IGNORES
    NSArray *ignoredKeys = [NSArray array];
#else
    NSArray *ignoredKeys = [rolesMap allKeysForObject:IgnoreClassName];
#endif

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBILITY_CLASS();
    DECLARE_STATIC_METHOD(jm_getAccessibility, sjc_CAccessibility, "getAccessibility", "([Ljava/lang/String;)Lsun/lwawt/macosx/CAccessibility;");
    jobjectArray result = NULL;
    jsize count = [ignoredKeys count];

    DECLARE_CLASS(jc_String, "java/lang/String");
    result = (*env)->NewObjectArray(env, count, jc_String, NULL);
    CHECK_EXCEPTION();
    if (!result) {
        NSLog(@"In %s, can't create Java array of String objects", __FUNCTION__);
        return;
    }

    NSInteger i;
    for (i = 0; i < count; i++) {
        jstring jString = NSStringToJavaString(env, [ignoredKeys objectAtIndex:i]);
        (*env)->SetObjectArrayElement(env, result, i, jString);
        (*env)->DeleteLocalRef(env, jString);
    }

    sAccessibilityClass = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getAccessibility, result);
    CHECK_EXCEPTION();
}

/*
 * If new implementation of the accessible component peer for the given role exists
 * return the allocated class otherwise return nil to let old implementation being initialized
 */
+ (CommonComponentAccessibility *) getComponentAccessibility:(NSString *)role
{
    AWT_ASSERT_APPKIT_THREAD;
    if (rolesMap == nil) {
        [self initializeRolesMap];
    }

    NSString *className = [rolesMap objectForKey:role];
    if (className != nil) {
        return [NSClassFromString(className) alloc];
    }
    return [CommonComponentAccessibility alloc];
}

+ (CommonComponentAccessibility *) getComponentAccessibility:(NSString *)role andParent:(CommonComponentAccessibility *)parent
{
    AWT_ASSERT_APPKIT_THREAD;
    if (rolesMap == nil) {
        [self initializeRolesMap];
    }
    NSString *className = [rowRolesMapForParent objectForKey:[[parent class] className]];
    if (className == nil) {
        return [CommonComponentAccessibility getComponentAccessibility:role];
    }
    return [NSClassFromString(className) alloc];
}

- (id)initWithParent:(NSObject *)parent withEnv:(JNIEnv *)env withAccessible:(jobject)accessible withIndex:(jint)index withView:(NSView *)view withJavaRole:(NSString *)javaRole
{
    self = [super init];
    if (self)
    {
        fParent = [parent retain];
        fView = [view retain];
        fJavaRole = [javaRole retain];

        if (accessible != NULL) {
            fAccessible = (*env)->NewWeakGlobalRef(env, accessible);
            CHECK_EXCEPTION();
        }

        jobject jcomponent = [(AWTView *)fView awtComponent:env];
        fComponent = (*env)->NewWeakGlobalRef(env, jcomponent);
        CHECK_EXCEPTION();

        (*env)->DeleteLocalRef(env, jcomponent);

        fIndex = index;

        fActions = nil;
        fActionSelectors = nil;
        fActionsLOCK = [[NSObject alloc] init];
    }
    return self;
}

- (void)unregisterFromCocoaAXSystem
{
    AWT_ASSERT_APPKIT_THREAD;
    static dispatch_once_t initialize_unregisterUniqueId_once;
    static void (*unregisterUniqueId)(id);
    dispatch_once(&initialize_unregisterUniqueId_once, ^{
        void *jrsFwk = dlopen("/System/Library/Frameworks/JavaVM.framework/Frameworks/JavaRuntimeSupport.framework/JavaRuntimeSupport", RTLD_LAZY | RTLD_LOCAL);
        unregisterUniqueId = dlsym(jrsFwk, "JRSAccessibilityUnregisterUniqueIdForUIElement");
    });
    if (unregisterUniqueId) unregisterUniqueId(self);
}

- (void)dealloc
{
    [self unregisterFromCocoaAXSystem];

    JNIEnv *env = [ThreadUtilities getJNIEnvUncached];

    (*env)->DeleteWeakGlobalRef(env, fAccessible);
    fAccessible = NULL;

    (*env)->DeleteWeakGlobalRef(env, fComponent);
    fComponent = NULL;

    [fParent release];
    fParent = nil;

    [fNSRole release];
    fNSRole = nil;

    [fJavaRole release];
    fJavaRole = nil;

    [fView release];
    fView = nil;

    [fActions release];
    fActions = nil;

    [fActionsLOCK release];
    fActionsLOCK = nil;

    [fActionSelectors release];
    fActionSelectors = nil;

    [super dealloc];
}

- (void)postValueChanged
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification(self, NSAccessibilityValueChangedNotification);
}

- (void)postSelectedTextChanged
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification(self, NSAccessibilitySelectedTextChangedNotification);
}

- (void)postSelectionChanged
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification(self, NSAccessibilitySelectedChildrenChangedNotification);
}

-(void)postTitleChanged
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification(self, NSAccessibilityTitleChangedNotification);
}

- (void)postMenuOpened
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification(self, (NSString *)kAXMenuOpenedNotification);
}

- (void)postMenuClosed
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification(self, (NSString *)kAXMenuClosedNotification);
}

- (void)postMenuItemSelected
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification(self, (NSString *)kAXMenuItemSelectedNotification);
}

- (void)postTreeNodeExpanded
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification([[self accessibilitySelectedRows] firstObject], NSAccessibilityRowExpandedNotification);
}

- (void)postTreeNodeCollapsed
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification([[self accessibilitySelectedRows] firstObject], NSAccessibilityRowCollapsedNotification);
}

- (BOOL)isEqual:(id)anObject
{
    if (![anObject isKindOfClass:[self class]]) return NO;
    CommonComponentAccessibility *accessibility = (CommonComponentAccessibility *)anObject;

    JNIEnv* env = [ThreadUtilities getJNIEnv];
    return (*env)->IsSameObject(env, accessibility->fAccessible, fAccessible);
}

- (BOOL)isAccessibleWithEnv:(JNIEnv *)env forAccessible:(jobject)accessible
{
    return (*env)->IsSameObject(env, fAccessible, accessible);
}

+ (void)initialize
{
    if (sRoles == nil) {
        initializeRoles();
    }
    if (sActions == nil) {
        initializeActions();
    }
}

+ (void)postFocusChanged:(id)message
{
    AWT_ASSERT_APPKIT_THREAD;
    NSAccessibilityPostNotification([NSApp accessibilityFocusedUIElement], NSAccessibilityFocusedUIElementChangedNotification);
}

+ (jobject) getCAccessible:(jobject)jaccessible withEnv:(JNIEnv *)env {
    DECLARE_CLASS_RETURN(sjc_Accessible, "javax/accessibility/Accessible", NULL);
    GET_CACCESSIBLE_CLASS_RETURN(NULL);
    DECLARE_STATIC_METHOD_RETURN(sjm_getCAccessible, sjc_CAccessible, "getCAccessible",
                                "(Ljavax/accessibility/Accessible;)Lsun/lwawt/macosx/CAccessible;", NULL);
    if ((*env)->IsInstanceOf(env, jaccessible, sjc_CAccessible)) {
        return jaccessible;
    } else if ((*env)->IsInstanceOf(env, jaccessible, sjc_Accessible)) {
        jobject o = (*env)->CallStaticObjectMethod(env, sjc_CAccessible,  sjm_getCAccessible, jaccessible);
        CHECK_EXCEPTION();
        return o;
    }
    return NULL;
}

+ (NSArray *)childrenOfParent:(CommonComponentAccessibility *)parent withEnv:(JNIEnv *)env withChildrenCode:(NSInteger)whichChildren allowIgnored:(BOOL)allowIgnored
{
    return [CommonComponentAccessibility childrenOfParent:parent withEnv:env withChildrenCode:whichChildren allowIgnored:allowIgnored recursive:NO];
}

+ (NSArray *)childrenOfParent:(CommonComponentAccessibility *)parent withEnv:(JNIEnv *)env withChildrenCode:(NSInteger)whichChildren allowIgnored:(BOOL)allowIgnored recursive:(BOOL)recursive
{
    if (parent->fAccessible == NULL) return nil;
    jobjectArray jchildrenAndRoles = NULL;
    if (recursive) {
        GET_CHILDRENANDROLESRECURSIVE_METHOD_RETURN(nil);
        jchildrenAndRoles = (jobjectArray)(*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getChildrenAndRolesRecursive,
                      parent->fAccessible, parent->fComponent, whichChildren, allowIgnored, 0);
        CHECK_EXCEPTION();
    } else {
        GET_CHILDRENANDROLES_METHOD_RETURN(nil);
        jchildrenAndRoles = (jobjectArray)(*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getChildrenAndRoles,
                      parent->fAccessible, parent->fComponent, whichChildren, allowIgnored);
        CHECK_EXCEPTION();
    }
    if (jchildrenAndRoles == NULL) return nil;

    jsize arrayLen = (*env)->GetArrayLength(env, jchildrenAndRoles);
    NSMutableArray *children = [NSMutableArray arrayWithCapacity:(recursive ? arrayLen/3 : arrayLen/2)]; //childrenAndRoles array contains two elements (child, role) for each child

    NSInteger i;
    NSUInteger childIndex = (whichChildren >= 0) ? whichChildren : 0; // if we're getting one particular child, make sure to set its index correctly
    int inc = recursive ? 3 : 2;
    for(i = 0; i < arrayLen; i+=inc)
    {
        jobject /* Accessible */ jchild = (*env)->GetObjectArrayElement(env, jchildrenAndRoles, i);
        jobject /* String */ jchildJavaRole = (*env)->GetObjectArrayElement(env, jchildrenAndRoles, i+1);

        NSString *childJavaRole = nil;
        if (jchildJavaRole != NULL) {
            DECLARE_CLASS_RETURN(sjc_AccessibleRole, "javax/accessibility/AccessibleRole", nil);
            DECLARE_FIELD_RETURN(sjf_key, sjc_AccessibleRole, "key", "Ljava/lang/String;", nil);
            jobject jkey = (*env)->GetObjectField(env, jchildJavaRole, sjf_key);
            CHECK_EXCEPTION();
            childJavaRole = JavaStringToNSString(env, jkey);
            (*env)->DeleteLocalRef(env, jkey);
        }

        CommonComponentAccessibility *child = [self createWithParent:parent accessible:jchild role:childJavaRole index:childIndex withEnv:env withView:parent->fView];

        if (recursive && [child respondsToSelector:@selector(accessibleLevel)]) {
            jobject jLevel = (*env)->GetObjectArrayElement(env, jchildrenAndRoles, i+2);
            NSString *sLevel = nil;
            if (jLevel != NULL) {
                sLevel = JavaStringToNSString(env, jLevel);
                if (sLevel != nil) {
                    int level = sLevel.intValue;
                    [child setAccessibleLevel:level];
                }
                (*env)->DeleteLocalRef(env, jLevel);
            }
        }

        (*env)->DeleteLocalRef(env, jchild);
        (*env)->DeleteLocalRef(env, jchildJavaRole);

        [children addObject:child];
        childIndex++;
    }
    (*env)->DeleteLocalRef(env, jchildrenAndRoles);

    return children;
}

+ (CommonComponentAccessibility *) createWithAccessible:(jobject)jaccessible withEnv:(JNIEnv *)env withView:(NSView *)view
{
    return [CommonComponentAccessibility createWithAccessible:jaccessible withEnv:env withView:view isCurrent:NO];
}

+ (CommonComponentAccessibility *) createWithAccessible:(jobject)jaccessible withEnv:(JNIEnv *)env withView:(NSView *)view isCurrent:(BOOL)current
{
    GET_ACCESSIBLEINDEXINPARENT_STATIC_METHOD_RETURN(nil);
    CommonComponentAccessibility *ret = nil;
    jobject jcomponent = [(AWTView *)view awtComponent:env];
    jint index = (*env)->CallStaticIntMethod(env, sjc_CAccessibility, sjm_getAccessibleIndexInParent, jaccessible, jcomponent);
    CHECK_EXCEPTION();
    if (index >= 0 || current) {
      NSString *javaRole = getJavaRole(env, jaccessible, jcomponent);
      ret = [self createWithAccessible:jaccessible role:javaRole index:index withEnv:env withView:view];
    }
    (*env)->DeleteLocalRef(env, jcomponent);
    return ret;
}

+ (CommonComponentAccessibility *) createWithAccessible:(jobject)jaccessible role:(NSString *)javaRole index:(jint)index withEnv:(JNIEnv *)env withView:(NSView *)view
{
    return [self createWithParent:nil accessible:jaccessible role:javaRole index:index withEnv:env withView:view];
}

+ (CommonComponentAccessibility *) createWithParent:(CommonComponentAccessibility *)parent accessible:(jobject)jaccessible role:(NSString *)javaRole index:(jint)index withEnv:(JNIEnv *)env withView:(NSView *)view
{
    return [CommonComponentAccessibility createWithParent:parent accessible:jaccessible role:javaRole index:index withEnv:env withView:view isWrapped:NO];
}

+ (CommonComponentAccessibility *) createWithParent:(CommonComponentAccessibility *)parent accessible:(jobject)jaccessible role:(NSString *)javaRole index:(jint)index withEnv:(JNIEnv *)env withView:(NSView *)view isWrapped:(BOOL)wrapped
{
    GET_CACCESSIBLE_CLASS_RETURN(NULL);
    DECLARE_FIELD_RETURN(jf_ptr, sjc_CAccessible, "ptr", "J", NULL);
    // try to fetch the jCAX from Java, and return autoreleased
    jobject jCAX = [CommonComponentAccessibility getCAccessible:jaccessible withEnv:env];
    if (jCAX == NULL) return nil;
    if (!wrapped) { // If wrapped is true, then you don't need to get an existing instance, you need to create a new one
        CommonComponentAccessibility *value = (CommonComponentAccessibility *) jlong_to_ptr((*env)->GetLongField(env, jCAX, jf_ptr));
        if (value != nil) {
            (*env)->DeleteLocalRef(env, jCAX);
            return [[value retain] autorelease];
        }
    }

    // otherwise, create a new instance
    CommonComponentAccessibility *newChild = [CommonComponentAccessibility getComponentAccessibility:javaRole andParent:parent];

    // must init freshly -alloc'd object
    [newChild initWithParent:parent withEnv:env withAccessible:jCAX withIndex:index withView:view withJavaRole:javaRole]; // must init new instance

    // If creating a JPopupMenu (not a combobox popup list) need to fire menuOpened.
    // This is the only way to know if the menu is opening; visible state change
    // can't be caught because the listeners are not set up in time.
    if ( [javaRole isEqualToString:@"popupmenu"] &&
         ![[parent javaRole] isEqualToString:@"combobox"] ) {
        [newChild postMenuOpened];
    }

    // must hard retain pointer poked into Java object
    [newChild retain];
    (*env)->SetLongField(env, jCAX, jf_ptr, ptr_to_jlong(newChild));

    // the link is removed in the wrapper
    if (!wrapped) {
        (*env)->DeleteLocalRef(env, jCAX);
    }

    // return autoreleased instance
    return [newChild autorelease];
}

- (NSDictionary *)getActions:(JNIEnv *)env
{
    @synchronized(fActionsLOCK) {
        if (fActions == nil) {
            [self getActionsWithEnv:env];
        }
    }

    return fActions;
}

- (void)getActionsWithEnv:(JNIEnv *)env
{
    GET_CACCESSIBILITY_CLASS();
    DECLARE_STATIC_METHOD(jm_getAccessibleAction, sjc_CAccessibility, "getAccessibleAction",
                           "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljavax/accessibility/AccessibleAction;");

    jobject axAction = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getAccessibleAction, fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (axAction != NULL) {
        jclass jc_AccessibleAction = NULL;
        GET_CLASS(jc_AccessibleAction, "javax/accessibility/AccessibleAction");
        jmethodID jm_getAccessibleActionCount = NULL;
        GET_METHOD(jm_getAccessibleActionCount, jc_AccessibleAction, "getAccessibleActionCount", "()I");
        jint count = (*env)->CallIntMethod(env, axAction, jm_getAccessibleActionCount);
        fActions = [[NSMutableDictionary alloc] initWithCapacity:count];
        fActionSelectors = [[NSMutableArray alloc] initWithCapacity:count];
        for (int i =0; i < count; i++) {
            JavaAxAction *action = [[JavaAxAction alloc] initWithEnv:env withAccessibleAction:axAction withIndex:i withComponent:fComponent];
            if ([fParent isKindOfClass:[CommonComponentAccessibility class]] &&
                [(CommonComponentAccessibility *)fParent isMenu] &&
                [[sActions objectForKey:[action getDescription]] isEqualToString:NSAccessibilityPressAction]) {
                [fActions setObject:action forKey:NSAccessibilityPickAction];
                [fActionSelectors addObject:[sActionSelectors objectForKey:NSAccessibilityPickAction]];
            } else {
                [fActions setObject:action forKey:[sActions objectForKey:[action getDescription]]];
                [fActionSelectors addObject:[sActionSelectors objectForKey:[sActions objectForKey:[action getDescription]]]];
            }
            [action release];
        }
        (*env)->DeleteLocalRef(env, axAction);
    }
}

- (BOOL)accessiblePerformAction:(NSAccessibilityActionName)actionName {
    NSMutableDictionary *currentAction = [self getActions:[ThreadUtilities getJNIEnv]];
    if (currentAction == nil) {
        return NO;
    }
    if ([[currentAction allKeys] containsObject:actionName]) {
        [(JavaAxAction *)[currentAction objectForKey:actionName] perform];
        return YES;;
    }
    return NO;
}

- (NSArray *)actionSelectors {
    @synchronized(fActionsLOCK) {
        if (fActionSelectors == nil) {
            [self getActionsWithEnv:[ThreadUtilities getJNIEnv]];
        }
    }

    return [NSArray arrayWithArray:fActionSelectors];
}

- (NSArray *)accessibleChildrenWithChildCode:(NSInteger)childCode
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    NSArray *children = [CommonComponentAccessibility childrenOfParent:self
                                                    withEnv:env
                                                    withChildrenCode:childCode
                                                    allowIgnored:([[self accessibilityRole] isEqualToString:NSAccessibilityListRole] || [[self accessibilityRole] isEqualToString:NSAccessibilityOutlineRole] || [[self accessibilityRole] isEqualToString:NSAccessibilityTableRole])
                                                             recursive:[[self accessibilityRole] isEqualToString:NSAccessibilityOutlineRole]];

    NSArray *value = nil;
    if ([children count] > 0) {
        value = children;
    }

    return value;
}

- (NSView *)view
{
    return fView;
}

- (NSWindow *)window
{
    return [[self view] window];
}

- (id)parent
{
    if(fParent == nil) {
        JNIEnv* env = [ThreadUtilities getJNIEnv];
        GET_CACCESSIBILITY_CLASS_RETURN(nil);
        DECLARE_STATIC_METHOD_RETURN(sjm_getAccessibleParent, sjc_CAccessibility, "getAccessibleParent",
                                 "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljavax/accessibility/Accessible;", nil);
        GET_CACCESSIBLE_CLASS_RETURN(nil);
        DECLARE_STATIC_METHOD_RETURN(sjm_getSwingAccessible, sjc_CAccessible, "getSwingAccessible",
                                 "(Ljavax/accessibility/Accessible;)Ljavax/accessibility/Accessible;", nil);
        DECLARE_CLASS_RETURN(sjc_Window, "java/awt/Window", nil);

        jobject jparent = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility,  sjm_getAccessibleParent, fAccessible, fComponent);
        CHECK_EXCEPTION();

        if (jparent == NULL) {
            fParent = fView;
        } else {
            AWTView *view = fView;
            jobject jax = (*env)->CallStaticObjectMethod(env, sjc_CAccessible, sjm_getSwingAccessible, fAccessible);
            CHECK_EXCEPTION();

            if ((*env)->IsInstanceOf(env, jax, sjc_Window)) {
                // In this case jparent is an owner toplevel and we should retrieve its own view
                view = [AWTView awtView:env ofAccessible:jparent];
            }
            if (view != nil) {
                fParent = [CommonComponentAccessibility createWithAccessible:jparent withEnv:env withView:view];
            }
            if (fParent == nil) {
                fParent = fView;
            }
            (*env)->DeleteLocalRef(env, jparent);
            (*env)->DeleteLocalRef(env, jax );
        }
        [fParent retain];
    }
    return fParent;
}

- (NSString *)javaRole
{
    if(fJavaRole == nil) {
        JNIEnv* env = [ThreadUtilities getJNIEnv];
        fJavaRole = getJavaRole(env, fAccessible, fComponent);
        [fJavaRole retain];
    }
    return fJavaRole;
}

- (jobject)axContextWithEnv:(JNIEnv *)env
{
    return getAxContext(env, fAccessible, fComponent);
}

// NSAccessibilityElement protocol implementation

- (BOOL)isAccessibilityElement
{
    return ![[self accessibilityRole] isEqualToString:JavaAccessibilityIgnore];
}

- (NSString *)accessibilityLabel
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];

    GET_ACCESSIBLENAME_METHOD_RETURN(nil);
    jobject val = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, sjm_getAccessibleName, fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (val == NULL) {
        return nil;
    }
    NSString* str = JavaStringToNSString(env, val);
    (*env)->DeleteLocalRef(env, val);
    return str;
}

- (NSString *)accessibilityHelp
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];

    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(sjm_getAccessibleDescription, sjc_CAccessibility, "getAccessibleDescription",
                                 "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljava/lang/String;", nil);
    jobject val = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility,
                                   sjm_getAccessibleDescription, fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (val == NULL) {
        return nil;
    }
    NSString* str = JavaStringToNSString(env, val);
    (*env)->DeleteLocalRef(env, val);
    return str;
}

- (NSArray *)accessibilityChildren
{
    return [self accessibleChildrenWithChildCode:sun_lwawt_macosx_CAccessibility_JAVA_AX_ALL_CHILDREN];
}

- (NSArray *)accessibilitySelectedChildren
{
    return [self accessibleChildrenWithChildCode:sun_lwawt_macosx_CAccessibility_JAVA_AX_SELECTED_CHILDREN];
}

- (NSArray *)accessibilityVisibleChildren
{
    return [self accessibleChildrenWithChildCode:sun_lwawt_macosx_CAccessibility_JAVA_AX_VISIBLE_CHILDREN];
}

- (NSRect)accessibilityFrame
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    GET_ACCESSIBLECOMPONENT_STATIC_METHOD_RETURN(NSZeroRect);
    jobject axComponent = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility,
                                                         sjm_getAccessibleComponent,
                                                         fAccessible, fComponent);
    CHECK_EXCEPTION();

    NSSize size = getAxComponentSize(env, axComponent, fComponent);
    NSPoint point = getAxComponentLocationOnScreen(env, axComponent, fComponent);
    (*env)->DeleteLocalRef(env, axComponent);
    point.y += size.height;

    point.y = [[[[self view] window] screen] frame].size.height - point.y;

    return NSMakeRect(point.x, point.y, size.width, size.height);
}

- (id _Nullable)accessibilityParent
{
    return NSAccessibilityUnignoredAncestor([self parent]);
}

- (BOOL)isAccessibilityEnabled
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBILITY_CLASS_RETURN(NO);
    DECLARE_STATIC_METHOD_RETURN(jm_isEnabled, sjc_CAccessibility, "isEnabled", "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Z", NO);

    BOOL value = (*env)->CallStaticBooleanMethod(env, sjc_CAccessibility, jm_isEnabled, fAccessible, fComponent);
    CHECK_EXCEPTION();

    return value;
}

- (id)accessibilityApplicationFocusedUIElement
{
    return [self accessibilityFocusedUIElement];
}

- (NSAccessibilityRole)accessibilityRole
{
    if (fNSRole == nil) {
        NSString *javaRole = [self javaRole];
        fNSRole = [sRoles objectForKey:javaRole];
        // The sRoles NSMutableDictionary maps popupmenu to Mac's popup button.
        // JComboBox behavior currently relies on this.  However this is not the
        // proper mapping for a JPopupMenu so fix that.
        if ( [javaRole isEqualToString:@"popupmenu"] &&
             ![[[self parent] javaRole] isEqualToString:@"combobox"] ) {
             fNSRole = NSAccessibilityMenuRole;
        }
        if (fNSRole == nil) {
            // this component has assigned itself a custom AccessibleRole not in the sRoles array
            fNSRole = javaRole;
        }
        [fNSRole retain];
    }
    return fNSRole;
}

- (NSString *)accessibilityRoleDescription
{
    // first ask AppKit for its accessible role description for a given AXRole
    NSString *value = NSAccessibilityRoleDescription([self accessibilityRole], nil);

    if (value == nil) {
        // query java if necessary
        JNIEnv* env = [ThreadUtilities getJNIEnv];
        GET_CACCESSIBILITY_CLASS_RETURN(nil);
        DECLARE_STATIC_METHOD_RETURN(jm_getAccessibleRoleDisplayString, sjc_CAccessibility, "getAccessibleRoleDisplayString",
                                     "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljava/lang/String;", nil);

        jobject axRole = (*env)->CallStaticObjectMethod(env, jm_getAccessibleRoleDisplayString, fAccessible, fComponent);
        CHECK_EXCEPTION();
        if (axRole != NULL) {
            value = JavaStringToNSString(env, axRole);
            (*env)->DeleteLocalRef(env, axRole);
        } else {
            value = @"unknown";
        }
    }

    return value;
}

- (BOOL)isAccessibilityFocused
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBILITY_CLASS_RETURN(NO);
    DECLARE_STATIC_METHOD_RETURN(sjm_isFocusTraversable, sjc_CAccessibility, "isFocusTraversable",
                                 "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Z", NO);
    // According to javadoc, a component that is focusable will return true from isFocusTraversable,
    // as well as having AccessibleState.FOCUSABLE in its AccessibleStateSet.
    // We use the former heuristic; if the component focus-traversable, add a focused attribute
    // See also initializeAttributeNamesWithEnv:
    if ((*env)->CallStaticBooleanMethod(env, sjc_CAccessibility, sjm_isFocusTraversable, fAccessible, fComponent)) {
        return [self isEqual:[NSApp accessibilityFocusedUIElement]];
    }
    CHECK_EXCEPTION();

    return NO;
}

- (void)setAccessibilityFocused:(BOOL)accessibilityFocused
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];

    GET_CACCESSIBILITY_CLASS();
    DECLARE_STATIC_METHOD(jm_requestFocus, sjc_CAccessibility, "requestFocus", "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)V");

    if (accessibilityFocused)
    {
        (*env)->CallStaticVoidMethod(env, sjc_CAccessibility, jm_requestFocus, fAccessible, fComponent);
        CHECK_EXCEPTION();
    }
}

- (NSUInteger)accessibilityIndexOfChild:(id)child
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_ACCESSIBLEINDEXINPARENT_STATIC_METHOD_RETURN(0);
    jint returnValue =
        (*env)->CallStaticIntMethod( env,
                                sjc_CAccessibility,
                                sjm_getAccessibleIndexInParent,
                                ((CommonComponentAccessibility *)child)->fAccessible,
                                ((CommonComponentAccessibility *)child)->fComponent );
    CHECK_EXCEPTION();
    return (returnValue == -1) ? NSNotFound : returnValue;
}

- (NSInteger)accessibilityIndex
{
    int index = 0;
    if (fParent != NULL) {
        index = [fParent accessibilityIndexOfChild:self];
    }
    return index;
}

- (id)accessibilityMaxValue
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(jm_getMaximumAccessibleValue, sjc_CAccessibility, "getMaximumAccessibleValue",
                                  "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljava/lang/Number;", nil);

    jobject axValue = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getMaximumAccessibleValue, fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (axValue == NULL) {
        return [NSNumber numberWithInt:0];
    }
    NSNumber* num = JavaNumberToNSNumber(env, axValue);
    (*env)->DeleteLocalRef(env, axValue);
    return num;
}

- (id)accessibilityMinValue
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(jm_getMinimumAccessibleValue, sjc_CAccessibility, "getMinimumAccessibleValue",
                                  "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljava/lang/Number;", nil);

    jobject axValue = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getMinimumAccessibleValue, fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (axValue == NULL) {
        return [NSNumber numberWithInt:0];
    }
    NSNumber* num = JavaNumberToNSNumber(env, axValue);
    (*env)->DeleteLocalRef(env, axValue);
    return num;
}

- (NSAccessibilityOrientation)accessibilityOrientation
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    jobject axContext = [self axContextWithEnv:env];

    // cmcnote - should batch these two calls into one that returns an array of two bools, one for vertical and one for horiz
    if (isVertical(env, axContext, fComponent)) {
        (*env)->DeleteLocalRef(env, axContext);
        return NSAccessibilityOrientationVertical;
    }
    if (isHorizontal(env, axContext, fComponent)) {
        (*env)->DeleteLocalRef(env, axContext);
        return NSAccessibilityOrientationHorizontal;
    }
    return NSAccessibilityOrientationUnknown;
}

- (NSPoint)accessibilityActivationPoint
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    GET_ACCESSIBLECOMPONENT_STATIC_METHOD_RETURN(NSPointFromString(@""));
    jobject axComponent = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, sjm_getAccessibleComponent,
                           fAccessible, fComponent);
    CHECK_EXCEPTION();

    // NSAccessibility wants the bottom left point of the object in
    // bottom left based screen coords

    // Get the java screen coords, and make a NSPoint of the bottom left of the AxComponent.
    NSSize size = getAxComponentSize(env, axComponent, fComponent);
    NSPoint point = getAxComponentLocationOnScreen(env, axComponent, fComponent);
    (*env)->DeleteLocalRef(env, axComponent);

    point.y += size.height;

    // Now make it into Cocoa screen coords.
    point.y = [[[[self view] window] screen] frame].size.height - point.y;

    return point;
}

- (BOOL)isAccessibilitySelected
{
    return [self isSelected:[ThreadUtilities getJNIEnv]];
}

- (void)setAccessibilitySelected:(BOOL)accessibilitySelected
{
   JNIEnv* env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBILITY_CLASS();
    DECLARE_STATIC_METHOD(jm_requestSelection,
                          sjc_CAccessibility,
                          "requestSelection",
                          "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)V");

    if (accessibilitySelected) {
        (*env)->CallStaticVoidMethod(env, sjc_CAccessibility, jm_requestSelection, fAccessible, fComponent);
        CHECK_EXCEPTION();
    }
}

- (id)accessibilityValue
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];

    // Need to handle popupmenus differently.
    //
    // At least for now don't handle combo box menus.
    // This may change when later fixing issues which currently
    // exist for combo boxes, but for now the following is only
    // for JPopupMenus, not for combobox menus.
    id parent = [self parent];
    if ( [[self javaRole] isEqualToString:@"popupmenu"] &&
         ![[parent javaRole] isEqualToString:@"combobox"] ) {
        NSArray *children =
            [CommonComponentAccessibility childrenOfParent:self
                                        withEnv:env
                                        withChildrenCode:sun_lwawt_macosx_CAccessibility_JAVA_AX_ALL_CHILDREN
                                        allowIgnored:YES];
        if ([children count] > 0) {
            // handle case of AXMenuItem
            // need to ask menu what is selected
            NSArray *selectedChildrenOfMenu =
                [self accessibilitySelectedChildren];
            CommonComponentAccessibility *selectedMenuItem =
                [selectedChildrenOfMenu objectAtIndex:0];
            if (selectedMenuItem != nil) {
                GET_CACCESSIBILITY_CLASS_RETURN(nil);
                GET_ACCESSIBLENAME_METHOD_RETURN(nil);
                jobject itemValue =
                        (*env)->CallStaticObjectMethod( env,
                                                   sjm_getAccessibleName,
                                                   selectedMenuItem->fAccessible,
                                                   selectedMenuItem->fComponent );
                CHECK_EXCEPTION();
                if (itemValue == NULL) {
                    return nil;
                }
                NSString* itemString = JavaStringToNSString(env, itemValue);
                (*env)->DeleteLocalRef(env, itemValue);
                return itemString;
            } else {
                return nil;
            }
        }
    }

    // ask Java for the component's accessibleValue. In java, the "accessibleValue" just means a numerical value
    // a text value is taken care of in JavaTextAccessibility

    // cmcnote should coalesce these calls into one java call
    NSNumber *num = nil;
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(sjm_getAccessibleValue, sjc_CAccessibility, "getAccessibleValue",
                "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljavax/accessibility/AccessibleValue;", nil);
    jobject axValue = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, sjm_getAccessibleValue, fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (axValue != NULL) {
        DECLARE_STATIC_METHOD_RETURN(jm_getCurrentAccessibleValue, sjc_CAccessibility, "getCurrentAccessibleValue",
                                     "(Ljavax/accessibility/AccessibleValue;Ljava/awt/Component;)Ljava/lang/Number;", nil);
        jobject str = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getCurrentAccessibleValue, axValue, fComponent);
        CHECK_EXCEPTION();
        if (str != NULL) {
            num = JavaNumberToNSNumber(env, str);
            (*env)->DeleteLocalRef(env, str);
        }
        (*env)->DeleteLocalRef(env, axValue);
    }
    if (num == nil) {
        num = [NSNumber numberWithInt:0];
    }
    return num;
}

- (id)accessibilityHitTest:(NSPoint)point
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];

    DECLARE_CLASS_RETURN(jc_Container, "java/awt/Container", nil);
    DECLARE_STATIC_METHOD_RETURN(jm_accessibilityHitTest, sjc_CAccessibility, "accessibilityHitTest",
                                 "(Ljava/awt/Container;FF)Ljavax/accessibility/Accessible;", nil);

    // Make it into java screen coords
    point.y = [[[[self view] window] screen] frame].size.height - point.y;

    jobject jparent = fComponent;

    id value = nil;
    if ((*env)->IsInstanceOf(env, jparent, jc_Container)) {
        jobject jaccessible = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_accessibilityHitTest,
                               jparent, (jfloat)point.x, (jfloat)point.y);
        CHECK_EXCEPTION();
        if (jaccessible != NULL) {
            value = [CommonComponentAccessibility createWithAccessible:jaccessible withEnv:env withView:fView];
            (*env)->DeleteLocalRef(env, jaccessible);
        }
    }

    if (value == nil) {
        value = self;
    }

    if (![value isAccessibilityElement]) {
        value = NSAccessibilityUnignoredAncestor(value);
    }

#ifdef JAVA_AX_DEBUG
    NSLog(@"%s: %@", __FUNCTION__, value);
#endif
    return value;
}

- (id)accessibilityFocusedUIElement
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(jm_getFocusOwner, sjc_CAccessibility, "getFocusOwner",
                                  "(Ljava/awt/Component;)Ljavax/accessibility/Accessible;", nil);
    id value = nil;

    NSWindow* hostWindow = [[self->fView window] retain];
    jobject focused = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getFocusOwner, fComponent);
    [hostWindow release];
    CHECK_EXCEPTION();

    if (focused != NULL) {
        DECLARE_CLASS_RETURN(sjc_Accessible, "javax/accessibility/Accessible", nil);
        if ((*env)->IsInstanceOf(env, focused, sjc_Accessible)) {
            value = [CommonComponentAccessibility createWithAccessible:focused withEnv:env withView:fView];
        }
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, focused);
    }

    if (value == nil) {
        value = self;
    }
#ifdef JAVA_AX_DEBUG
    NSLog(@"%s: %@", __FUNCTION__, value);
#endif
    return value;
}

- (id)accessibilityWindow {
    return [self window];
}

// AccessibleAction support

- (BOOL)performAccessibleAction:(int)index
{
    AWT_ASSERT_APPKIT_THREAD;
    JNIEnv* env = [ThreadUtilities getJNIEnv];

    GET_CACCESSIBILITY_CLASS_RETURN(FALSE);
    DECLARE_STATIC_METHOD_RETURN(jm_doAccessibleAction, sjc_CAccessibility, "doAccessibleAction",
                                 "(Ljavax/accessibility/AccessibleAction;ILjava/awt/Component;)V", FALSE);
    (*env)->CallStaticVoidMethod(env, sjc_CAccessibility, jm_doAccessibleAction,
                                 [self axContextWithEnv:(env)], index, fComponent);
    CHECK_EXCEPTION();

    return TRUE;
}

// NSAccessibilityActions methods

- (BOOL)isAccessibilitySelectorAllowed:(SEL)selector {
    if ([sAllActionSelectors containsObject:NSStringFromSelector(selector)] &&
        ![[self actionSelectors] containsObject:NSStringFromSelector(selector)]) {
        return NO;
    }
    return [super isAccessibilitySelectorAllowed:selector];
}

- (BOOL)accessibilityPerformPick {
    return [self accessiblePerformAction:NSAccessibilityPickAction];
}

- (BOOL)accessibilityPerformPress {
    return [self accessiblePerformAction:NSAccessibilityPressAction];
}

- (BOOL)accessibilityPerformShowMenu {
    return [self accessiblePerformAction:NSAccessibilityShowMenuAction];
}

- (BOOL)accessibilityPerformDecrement {
    return [self accessiblePerformAction:NSAccessibilityDecrementAction];
}

- (BOOL)accessibilityPerformIncrement {
    return [self accessiblePerformAction:NSAccessibilityIncrementAction];
}

@end

/*
 * Class:     sun_lwawt_macosx_CAccessible
 * Method:    treeNodeExpanded
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CAccessible_treeNodeExpanded
  (JNIEnv *env, jclass jklass, jlong element)
{
    JNI_COCOA_ENTER(env);
        [ThreadUtilities performOnMainThread:@selector(postTreeNodeExpanded)
                         on:(CommonComponentAccessibility *)jlong_to_ptr(element)
                         withObject:nil
                         waitUntilDone:NO];
    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CAccessible
 * Method:    treeNodeCollapsed
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CAccessible_treeNodeCollapsed
  (JNIEnv *env, jclass jklass, jlong element)
{
    JNI_COCOA_ENTER(env);
        [ThreadUtilities performOnMainThread:@selector(postTreeNodeCollapsed)
                         on:(CommonComponentAccessibility *)jlong_to_ptr(element)
                         withObject:nil
                         waitUntilDone:NO];
    JNI_COCOA_EXIT(env);
}
