/*
 * Copyright © 2009,2010  Red Hat, Inc.
 * Copyright © 2010,2011,2012  Google, Inc.
 *
 *  This is part of HarfBuzz, a text shaping library.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE COPYRIGHT HOLDER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * THE COPYRIGHT HOLDER SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE COPYRIGHT HOLDER HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Red Hat Author(s): Behdad Esfahbod
 * Google Author(s): Behdad Esfahbod
 */

#include "hb.hh"

#ifndef HB_NO_OT_SHAPE

#ifdef HB_NO_OT_LAYOUT
#error "Cannot compile 'ot' shaper with HB_NO_OT_LAYOUT."
#endif

#include "hb-shaper-impl.hh"

#include "hb-ot-shape.hh"
#include "hb-ot-shape-complex.hh"
#include "hb-ot-shape-fallback.hh"
#include "hb-ot-shape-normalize.hh"

#include "hb-ot-face.hh"

#include "hb-set.hh"

#include "hb-aat-layout.hh"


#ifndef HB_NO_AAT_SHAPE
static inline bool
_hb_apply_morx (hb_face_t *face, const hb_segment_properties_t *props)
{
  /* https://github.com/harfbuzz/harfbuzz/issues/2124 */
  return hb_aat_layout_has_substitution (face) &&
         (HB_DIRECTION_IS_HORIZONTAL (props->direction) || !hb_ot_layout_has_substitution (face));
}
#endif

/**
 * SECTION:hb-ot-shape
 * @title: hb-ot-shape
 * @short_description: OpenType shaping support
 * @include: hb-ot.h
 *
 * Support functions for OpenType shaping related queries.
 **/


static void
hb_ot_shape_collect_features (hb_ot_shape_planner_t          *planner,
                              const hb_feature_t             *user_features,
                              unsigned int                    num_user_features);

hb_ot_shape_planner_t::hb_ot_shape_planner_t (hb_face_t                     *face,
                                              const hb_segment_properties_t *props) :
                                                face (face),
                                                props (*props),
                                                map (face, props),
                                                aat_map (face, props)
#ifndef HB_NO_AAT_SHAPE
                                                , apply_morx (_hb_apply_morx (face, props))
#endif
{
  shaper = hb_ot_shape_complex_categorize (this);

  script_zero_marks = shaper->zero_width_marks != HB_OT_SHAPE_ZERO_WIDTH_MARKS_NONE;
  script_fallback_mark_positioning = shaper->fallback_position;

  /* https://github.com/harfbuzz/harfbuzz/issues/1528 */
  if (apply_morx && shaper != &_hb_ot_complex_shaper_default)
    shaper = &_hb_ot_complex_shaper_dumber;
}

void
hb_ot_shape_planner_t::compile (hb_ot_shape_plan_t           &plan,
                                const hb_ot_shape_plan_key_t &key)
{
  plan.props = props;
  plan.shaper = shaper;
  map.compile (plan.map, key);
#ifndef HB_NO_AAT_SHAPE
  if (apply_morx)
    aat_map.compile (plan.aat_map);
#endif

#ifndef HB_NO_OT_SHAPE_FRACTIONS
  plan.frac_mask = plan.map.get_1_mask (HB_TAG ('f','r','a','c'));
  plan.numr_mask = plan.map.get_1_mask (HB_TAG ('n','u','m','r'));
  plan.dnom_mask = plan.map.get_1_mask (HB_TAG ('d','n','o','m'));
  plan.has_frac = plan.frac_mask || (plan.numr_mask && plan.dnom_mask);
#endif

  plan.rtlm_mask = plan.map.get_1_mask (HB_TAG ('r','t','l','m'));
  plan.has_vert = !!plan.map.get_1_mask (HB_TAG ('v','e','r','t'));

  hb_tag_t kern_tag = HB_DIRECTION_IS_HORIZONTAL (props.direction) ?
                      HB_TAG ('k','e','r','n') : HB_TAG ('v','k','r','n');
#ifndef HB_NO_OT_KERN
  plan.kern_mask = plan.map.get_mask (kern_tag);
  plan.requested_kerning = !!plan.kern_mask;
#endif
#ifndef HB_NO_AAT_SHAPE
  plan.trak_mask = plan.map.get_mask (HB_TAG ('t','r','a','k'));
  plan.requested_tracking = !!plan.trak_mask;
#endif

  bool has_gpos_kern = plan.map.get_feature_index (1, kern_tag) != HB_OT_LAYOUT_NO_FEATURE_INDEX;
  bool disable_gpos = plan.shaper->gpos_tag &&
                      plan.shaper->gpos_tag != plan.map.chosen_script[1];

  /*
   * Decide who provides glyph classes. GDEF or Unicode.
   */

  if (!hb_ot_layout_has_glyph_classes (face))
    plan.fallback_glyph_classes = true;

  /*
   * Decide who does substitutions. GSUB, morx, or fallback.
   */

#ifndef HB_NO_AAT_SHAPE
  plan.apply_morx = apply_morx;
#endif

  /*
   * Decide who does positioning. GPOS, kerx, kern, or fallback.
   */

  if (0)
    ;
#ifndef HB_NO_AAT_SHAPE
  else if (hb_aat_layout_has_positioning (face))
    plan.apply_kerx = true;
#endif
  else if (!apply_morx && !disable_gpos && hb_ot_layout_has_positioning (face))
    plan.apply_gpos = true;

  if (!plan.apply_kerx && (!has_gpos_kern || !plan.apply_gpos))
  {
    /* Apparently Apple applies kerx if GPOS kern was not applied. */
#ifndef HB_NO_AAT_SHAPE
    if (hb_aat_layout_has_positioning (face))
      plan.apply_kerx = true;
    else
#endif
#ifndef HB_NO_OT_KERN
    if (hb_ot_layout_has_kerning (face))
      plan.apply_kern = true;
#endif
  }

  plan.zero_marks = script_zero_marks &&
                    !plan.apply_kerx &&
                    (!plan.apply_kern
#ifndef HB_NO_OT_KERN
                     || !hb_ot_layout_has_machine_kerning (face)
#endif
                    );
  plan.has_gpos_mark = !!plan.map.get_1_mask (HB_TAG ('m','a','r','k'));

  plan.adjust_mark_positioning_when_zeroing = !plan.apply_gpos &&
                                              !plan.apply_kerx &&
                                              (!plan.apply_kern
#ifndef HB_NO_OT_KERN
                                               || !hb_ot_layout_has_cross_kerning (face)
#endif
                                              );

  plan.fallback_mark_positioning = plan.adjust_mark_positioning_when_zeroing &&
                                   script_fallback_mark_positioning;

#ifndef HB_NO_AAT_SHAPE
  /* Currently we always apply trak. */
  plan.apply_trak = plan.requested_tracking && hb_aat_layout_has_tracking (face);
#endif
}

bool
hb_ot_shape_plan_t::init0 (hb_face_t                     *face,
                           const hb_shape_plan_key_t     *key)
{
  map.init ();
#ifndef HB_NO_AAT_SHAPE
  aat_map.init ();
#endif

  hb_ot_shape_planner_t planner (face,
                                 &key->props);

  hb_ot_shape_collect_features (&planner,
                                key->user_features,
                                key->num_user_features);

  planner.compile (*this, key->ot);

  if (shaper->data_create)
  {
    data = shaper->data_create (this);
    if (unlikely (!data))
    {
      map.fini ();
#ifndef HB_NO_AAT_SHAPE
      aat_map.fini ();
#endif
      return false;
    }
  }

  return true;
}

void
hb_ot_shape_plan_t::fini ()
{
  if (shaper->data_destroy)
    shaper->data_destroy (const_cast<void *> (data));

  map.fini ();
#ifndef HB_NO_AAT_SHAPE
  aat_map.fini ();
#endif
}

void
hb_ot_shape_plan_t::substitute (hb_font_t   *font,
                                hb_buffer_t *buffer) const
{
#ifndef HB_NO_AAT_SHAPE
  if (unlikely (apply_morx))
    hb_aat_layout_substitute (this, font, buffer);
  else
#endif
    map.substitute (this, font, buffer);
}

void
hb_ot_shape_plan_t::position (hb_font_t   *font,
                              hb_buffer_t *buffer) const
{
  if (this->apply_gpos)
    map.position (this, font, buffer);
#ifndef HB_NO_AAT_SHAPE
  else if (this->apply_kerx)
    hb_aat_layout_position (this, font, buffer);
#endif
#ifndef HB_NO_OT_KERN
  else if (this->apply_kern)
    hb_ot_layout_kern (this, font, buffer);
#endif
  else
    _hb_ot_shape_fallback_kern (this, font, buffer);

#ifndef HB_NO_AAT_SHAPE
  if (this->apply_trak)
    hb_aat_layout_track (this, font, buffer);
#endif
}


static const hb_ot_map_feature_t
common_features[] =
{
  {HB_TAG('a','b','v','m'), F_GLOBAL},
  {HB_TAG('b','l','w','m'), F_GLOBAL},
  {HB_TAG('c','c','m','p'), F_GLOBAL},
  {HB_TAG('l','o','c','l'), F_GLOBAL},
  {HB_TAG('m','a','r','k'), F_GLOBAL_MANUAL_JOINERS},
  {HB_TAG('m','k','m','k'), F_GLOBAL_MANUAL_JOINERS},
  {HB_TAG('r','l','i','g'), F_GLOBAL},
};


static const hb_ot_map_feature_t
horizontal_features[] =
{
  {HB_TAG('c','a','l','t'), F_GLOBAL},
  {HB_TAG('c','l','i','g'), F_GLOBAL},
  {HB_TAG('c','u','r','s'), F_GLOBAL},
  {HB_TAG('d','i','s','t'), F_GLOBAL},
  {HB_TAG('k','e','r','n'), F_GLOBAL_HAS_FALLBACK},
  {HB_TAG('l','i','g','a'), F_GLOBAL},
  {HB_TAG('r','c','l','t'), F_GLOBAL},
};

static void
hb_ot_shape_collect_features (hb_ot_shape_planner_t          *planner,
                              const hb_feature_t             *user_features,
                              unsigned int                    num_user_features)
{
  hb_ot_map_builder_t *map = &planner->map;

  map->enable_feature (HB_TAG('r','v','r','n'));
  map->add_gsub_pause (nullptr);

  switch (planner->props.direction) {
    case HB_DIRECTION_LTR:
      map->enable_feature (HB_TAG ('l','t','r','a'));
      map->enable_feature (HB_TAG ('l','t','r','m'));
      break;
    case HB_DIRECTION_RTL:
      map->enable_feature (HB_TAG ('r','t','l','a'));
      map->add_feature (HB_TAG ('r','t','l','m'));
      break;
    case HB_DIRECTION_TTB:
    case HB_DIRECTION_BTT:
    case HB_DIRECTION_INVALID:
    default:
      break;
  }

#ifndef HB_NO_OT_SHAPE_FRACTIONS
  /* Automatic fractions. */
  map->add_feature (HB_TAG ('f','r','a','c'));
  map->add_feature (HB_TAG ('n','u','m','r'));
  map->add_feature (HB_TAG ('d','n','o','m'));
#endif

  /* Random! */
  map->enable_feature (HB_TAG ('r','a','n','d'), F_RANDOM, HB_OT_MAP_MAX_VALUE);

#ifndef HB_NO_AAT_SHAPE
  /* Tracking.  We enable dummy feature here just to allow disabling
   * AAT 'trak' table using features.
   * https://github.com/harfbuzz/harfbuzz/issues/1303 */
  map->enable_feature (HB_TAG ('t','r','a','k'), F_HAS_FALLBACK);
#endif

  map->enable_feature (HB_TAG ('H','A','R','F'));

  if (planner->shaper->collect_features)
    planner->shaper->collect_features (planner);

  map->enable_feature (HB_TAG ('B','U','Z','Z'));

  for (unsigned int i = 0; i < ARRAY_LENGTH (common_features); i++)
    map->add_feature (common_features[i]);

  if (HB_DIRECTION_IS_HORIZONTAL (planner->props.direction))
    for (unsigned int i = 0; i < ARRAY_LENGTH (horizontal_features); i++)
      map->add_feature (horizontal_features[i]);
  else
  {
    /* We really want to find a 'vert' feature if there's any in the font, no
     * matter which script/langsys it is listed (or not) under.
     * See various bugs referenced from:
     * https://github.com/harfbuzz/harfbuzz/issues/63 */
    map->enable_feature (HB_TAG ('v','e','r','t'), F_GLOBAL_SEARCH);
  }

  for (unsigned int i = 0; i < num_user_features; i++)
  {
    const hb_feature_t *feature = &user_features[i];
    map->add_feature (feature->tag,
                      (feature->start == HB_FEATURE_GLOBAL_START &&
                       feature->end == HB_FEATURE_GLOBAL_END) ?  F_GLOBAL : F_NONE,
                      feature->value);
  }

#ifndef HB_NO_AAT_SHAPE
  if (planner->apply_morx)
  {
    hb_aat_map_builder_t *aat_map = &planner->aat_map;
    for (unsigned int i = 0; i < num_user_features; i++)
    {
      const hb_feature_t *feature = &user_features[i];
      aat_map->add_feature (feature->tag, feature->value);
    }
  }
#endif

  if (planner->shaper->override_features)
    planner->shaper->override_features (planner);
}


/*
 * shaper face data
 */

struct hb_ot_face_data_t {};

hb_ot_face_data_t *
_hb_ot_shaper_face_data_create (hb_face_t *face)
{
  return (hb_ot_face_data_t *) HB_SHAPER_DATA_SUCCEEDED;
}

void
_hb_ot_shaper_face_data_destroy (hb_ot_face_data_t *data)
{
}


/*
 * shaper font data
 */

struct hb_ot_font_data_t {};

hb_ot_font_data_t *
_hb_ot_shaper_font_data_create (hb_font_t *font HB_UNUSED)
{
  return (hb_ot_font_data_t *) HB_SHAPER_DATA_SUCCEEDED;
}

void
_hb_ot_shaper_font_data_destroy (hb_ot_font_data_t *data HB_UNUSED)
{
}


/*
 * shaper
 */

struct hb_ot_shape_context_t
{
  hb_ot_shape_plan_t *plan;
  hb_font_t *font;
  hb_face_t *face;
  hb_buffer_t  *buffer;
  const hb_feature_t *user_features;
  unsigned int        num_user_features;

  /* Transient stuff */
  hb_direction_t target_direction;
};



/* Main shaper */


/* Prepare */

static void
hb_set_unicode_props (hb_buffer_t *buffer)
{
  /* Implement enough of Unicode Graphemes here that shaping
   * in reverse-direction wouldn't break graphemes.  Namely,
   * we mark all marks and ZWJ and ZWJ,Extended_Pictographic
   * sequences as continuations.  The foreach_grapheme()
   * macro uses this bit.
   *
   * https://www.unicode.org/reports/tr29/#Regex_Definitions
   */
  unsigned int count = buffer->len;
  hb_glyph_info_t *info = buffer->info;
  for (unsigned int i = 0; i < count; i++)
  {
    _hb_glyph_info_set_unicode_props (&info[i], buffer);

    /* Marks are already set as continuation by the above line.
     * Handle Emoji_Modifier and ZWJ-continuation. */
    if (unlikely (_hb_glyph_info_get_general_category (&info[i]) == HB_UNICODE_GENERAL_CATEGORY_MODIFIER_SYMBOL &&
                  hb_in_range<hb_codepoint_t> (info[i].codepoint, 0x1F3FBu, 0x1F3FFu)))
    {
        _hb_glyph_info_set_continuation (&info[i]);
    }
#ifndef HB_NO_EMOJI_SEQUENCES
    else if (unlikely (_hb_glyph_info_is_zwj (&info[i])))
    {
      _hb_glyph_info_set_continuation (&info[i]);
      if (i + 1 < count &&
          _hb_unicode_is_emoji_Extended_Pictographic (info[i + 1].codepoint))
      {
        i++;
        _hb_glyph_info_set_unicode_props (&info[i], buffer);
        _hb_glyph_info_set_continuation (&info[i]);
      }
    }
#endif
    /* Or part of the Other_Grapheme_Extend that is not marks.
     * As of Unicode 11 that is just:
     *
     * 200C          ; Other_Grapheme_Extend # Cf       ZERO WIDTH NON-JOINER
     * FF9E..FF9F    ; Other_Grapheme_Extend # Lm   [2] HALFWIDTH KATAKANA VOICED SOUND MARK..HALFWIDTH KATAKANA SEMI-VOICED SOUND MARK
     * E0020..E007F  ; Other_Grapheme_Extend # Cf  [96] TAG SPACE..CANCEL TAG
     *
     * ZWNJ is special, we don't want to merge it as there's no need, and keeping
     * it separate results in more granular clusters.  Ignore Katakana for now.
     * Tags are used for Emoji sub-region flag sequences:
     * https://github.com/harfbuzz/harfbuzz/issues/1556
     */
    else if (unlikely (hb_in_range<hb_codepoint_t> (info[i].codepoint, 0xE0020u, 0xE007Fu)))
      _hb_glyph_info_set_continuation (&info[i]);
  }
}

static void
hb_insert_dotted_circle (hb_buffer_t *buffer, hb_font_t *font)
{
  if (unlikely (buffer->flags & HB_BUFFER_FLAG_DO_NOT_INSERT_DOTTED_CIRCLE))
    return;

  if (!(buffer->flags & HB_BUFFER_FLAG_BOT) ||
      buffer->context_len[0] ||
      !_hb_glyph_info_is_unicode_mark (&buffer->info[0]))
    return;

  if (!font->has_glyph (0x25CCu))
    return;

  hb_glyph_info_t dottedcircle = {0};
  dottedcircle.codepoint = 0x25CCu;
  _hb_glyph_info_set_unicode_props (&dottedcircle, buffer);

  buffer->clear_output ();

  buffer->idx = 0;
  hb_glyph_info_t info = dottedcircle;
  info.cluster = buffer->cur().cluster;
  info.mask = buffer->cur().mask;
  (void) buffer->output_info (info);
  buffer->swap_buffers ();
}

static void
hb_form_clusters (hb_buffer_t *buffer)
{
  if (!(buffer->scratch_flags & HB_BUFFER_SCRATCH_FLAG_HAS_NON_ASCII))
    return;

  if (buffer->cluster_level == HB_BUFFER_CLUSTER_LEVEL_MONOTONE_GRAPHEMES)
    foreach_grapheme (buffer, start, end)
      buffer->merge_clusters (start, end);
  else
    foreach_grapheme (buffer, start, end)
      buffer->unsafe_to_break (start, end);
}

static void
hb_ensure_native_direction (hb_buffer_t *buffer)
{
  hb_direction_t direction = buffer->props.direction;
  hb_direction_t horiz_dir = hb_script_get_horizontal_direction (buffer->props.script);

  /* TODO vertical:
   * The only BTT vertical script is Ogham, but it's not clear to me whether OpenType
   * Ogham fonts are supposed to be implemented BTT or not.  Need to research that
   * first. */
  if ((HB_DIRECTION_IS_HORIZONTAL (direction) &&
       direction != horiz_dir && horiz_dir != HB_DIRECTION_INVALID) ||
      (HB_DIRECTION_IS_VERTICAL   (direction) &&
       direction != HB_DIRECTION_TTB))
  {

    if (buffer->cluster_level == HB_BUFFER_CLUSTER_LEVEL_MONOTONE_CHARACTERS)
      foreach_grapheme (buffer, start, end)
      {
        buffer->merge_clusters (start, end);
        buffer->reverse_range (start, end);
      }
    else
      foreach_grapheme (buffer, start, end)
        /* form_clusters() merged clusters already, we don't merge. */
        buffer->reverse_range (start, end);

    buffer->reverse ();

    buffer->props.direction = HB_DIRECTION_REVERSE (buffer->props.direction);
  }
}


/*
 * Substitute
 */

static hb_codepoint_t
hb_vert_char_for (hb_codepoint_t u)
{
  switch (u >> 8)
  {
    case 0x20: switch (u) {
      case 0x2013u: return 0xfe32u; // EN DASH
      case 0x2014u: return 0xfe31u; // EM DASH
      case 0x2025u: return 0xfe30u; // TWO DOT LEADER
      case 0x2026u: return 0xfe19u; // HORIZONTAL ELLIPSIS
    } break;
    case 0x30: switch (u) {
      case 0x3001u: return 0xfe11u; // IDEOGRAPHIC COMMA
      case 0x3002u: return 0xfe12u; // IDEOGRAPHIC FULL STOP
      case 0x3008u: return 0xfe3fu; // LEFT ANGLE BRACKET
      case 0x3009u: return 0xfe40u; // RIGHT ANGLE BRACKET
      case 0x300au: return 0xfe3du; // LEFT DOUBLE ANGLE BRACKET
      case 0x300bu: return 0xfe3eu; // RIGHT DOUBLE ANGLE BRACKET
      case 0x300cu: return 0xfe41u; // LEFT CORNER BRACKET
      case 0x300du: return 0xfe42u; // RIGHT CORNER BRACKET
      case 0x300eu: return 0xfe43u; // LEFT WHITE CORNER BRACKET
      case 0x300fu: return 0xfe44u; // RIGHT WHITE CORNER BRACKET
      case 0x3010u: return 0xfe3bu; // LEFT BLACK LENTICULAR BRACKET
      case 0x3011u: return 0xfe3cu; // RIGHT BLACK LENTICULAR BRACKET
      case 0x3014u: return 0xfe39u; // LEFT TORTOISE SHELL BRACKET
      case 0x3015u: return 0xfe3au; // RIGHT TORTOISE SHELL BRACKET
      case 0x3016u: return 0xfe17u; // LEFT WHITE LENTICULAR BRACKET
      case 0x3017u: return 0xfe18u; // RIGHT WHITE LENTICULAR BRACKET
    } break;
    case 0xfe: switch (u) {
      case 0xfe4fu: return 0xfe34u; // WAVY LOW LINE
    } break;
    case 0xff: switch (u) {
      case 0xff01u: return 0xfe15u; // FULLWIDTH EXCLAMATION MARK
      case 0xff08u: return 0xfe35u; // FULLWIDTH LEFT PARENTHESIS
      case 0xff09u: return 0xfe36u; // FULLWIDTH RIGHT PARENTHESIS
      case 0xff0cu: return 0xfe10u; // FULLWIDTH COMMA
      case 0xff1au: return 0xfe13u; // FULLWIDTH COLON
      case 0xff1bu: return 0xfe14u; // FULLWIDTH SEMICOLON
      case 0xff1fu: return 0xfe16u; // FULLWIDTH QUESTION MARK
      case 0xff3bu: return 0xfe47u; // FULLWIDTH LEFT SQUARE BRACKET
      case 0xff3du: return 0xfe48u; // FULLWIDTH RIGHT SQUARE BRACKET
      case 0xff3fu: return 0xfe33u; // FULLWIDTH LOW LINE
      case 0xff5bu: return 0xfe37u; // FULLWIDTH LEFT CURLY BRACKET
      case 0xff5du: return 0xfe38u; // FULLWIDTH RIGHT CURLY BRACKET
    } break;
  }

  return u;
}

static inline void
hb_ot_rotate_chars (const hb_ot_shape_context_t *c)
{
  hb_buffer_t *buffer = c->buffer;
  unsigned int count = buffer->len;
  hb_glyph_info_t *info = buffer->info;

  if (HB_DIRECTION_IS_BACKWARD (c->target_direction))
  {
    hb_unicode_funcs_t *unicode = buffer->unicode;
    hb_mask_t rtlm_mask = c->plan->rtlm_mask;

    for (unsigned int i = 0; i < count; i++) {
      hb_codepoint_t codepoint = unicode->mirroring (info[i].codepoint);
      if (unlikely (codepoint != info[i].codepoint && c->font->has_glyph (codepoint)))
        info[i].codepoint = codepoint;
      else
        info[i].mask |= rtlm_mask;
    }
  }

  if (HB_DIRECTION_IS_VERTICAL (c->target_direction) && !c->plan->has_vert)
  {
    for (unsigned int i = 0; i < count; i++) {
      hb_codepoint_t codepoint = hb_vert_char_for (info[i].codepoint);
      if (unlikely (codepoint != info[i].codepoint && c->font->has_glyph (codepoint)))
        info[i].codepoint = codepoint;
    }
  }
}

static inline void
hb_ot_shape_setup_masks_fraction (const hb_ot_shape_context_t *c)
{
#ifdef HB_NO_OT_SHAPE_FRACTIONS
  return;
#endif

  if (!(c->buffer->scratch_flags & HB_BUFFER_SCRATCH_FLAG_HAS_NON_ASCII) ||
      !c->plan->has_frac)
    return;

  hb_buffer_t *buffer = c->buffer;

  hb_mask_t pre_mask, post_mask;
  if (HB_DIRECTION_IS_FORWARD (buffer->props.direction))
  {
    pre_mask = c->plan->numr_mask | c->plan->frac_mask;
    post_mask = c->plan->frac_mask | c->plan->dnom_mask;
  }
  else
  {
    pre_mask = c->plan->frac_mask | c->plan->dnom_mask;
    post_mask = c->plan->numr_mask | c->plan->frac_mask;
  }

  unsigned int count = buffer->len;
  hb_glyph_info_t *info = buffer->info;
  for (unsigned int i = 0; i < count; i++)
  {
    if (info[i].codepoint == 0x2044u) /* FRACTION SLASH */
    {
      unsigned int start = i, end = i + 1;
      while (start &&
             _hb_glyph_info_get_general_category (&info[start - 1]) ==
             HB_UNICODE_GENERAL_CATEGORY_DECIMAL_NUMBER)
        start--;
      while (end < count &&
             _hb_glyph_info_get_general_category (&info[end]) ==
             HB_UNICODE_GENERAL_CATEGORY_DECIMAL_NUMBER)
        end++;

      buffer->unsafe_to_break (start, end);

      for (unsigned int j = start; j < i; j++)
        info[j].mask |= pre_mask;
      info[i].mask |= c->plan->frac_mask;
      for (unsigned int j = i + 1; j < end; j++)
        info[j].mask |= post_mask;

      i = end - 1;
    }
  }
}

static inline void
hb_ot_shape_initialize_masks (const hb_ot_shape_context_t *c)
{
  hb_ot_map_t *map = &c->plan->map;
  hb_buffer_t *buffer = c->buffer;

  hb_mask_t global_mask = map->get_global_mask ();
  buffer->reset_masks (global_mask);
}

static inline void
hb_ot_shape_setup_masks (const hb_ot_shape_context_t *c)
{
  hb_ot_map_t *map = &c->plan->map;
  hb_buffer_t *buffer = c->buffer;

  hb_ot_shape_setup_masks_fraction (c);

  if (c->plan->shaper->setup_masks)
    c->plan->shaper->setup_masks (c->plan, buffer, c->font);

  for (unsigned int i = 0; i < c->num_user_features; i++)
  {
    const hb_feature_t *feature = &c->user_features[i];
    if (!(feature->start == HB_FEATURE_GLOBAL_START && feature->end == HB_FEATURE_GLOBAL_END)) {
      unsigned int shift;
      hb_mask_t mask = map->get_mask (feature->tag, &shift);
      buffer->set_masks (feature->value << shift, mask, feature->start, feature->end);
    }
  }
}

static void
hb_ot_zero_width_default_ignorables (const hb_buffer_t *buffer)
{
  if (!(buffer->scratch_flags & HB_BUFFER_SCRATCH_FLAG_HAS_DEFAULT_IGNORABLES) ||
      (buffer->flags & HB_BUFFER_FLAG_PRESERVE_DEFAULT_IGNORABLES) ||
      (buffer->flags & HB_BUFFER_FLAG_REMOVE_DEFAULT_IGNORABLES))
    return;

  unsigned int count = buffer->len;
  hb_glyph_info_t *info = buffer->info;
  hb_glyph_position_t *pos = buffer->pos;
  unsigned int i = 0;
  for (i = 0; i < count; i++)
    if (unlikely (_hb_glyph_info_is_default_ignorable (&info[i])))
      pos[i].x_advance = pos[i].y_advance = pos[i].x_offset = pos[i].y_offset = 0;
}

static void
hb_ot_hide_default_ignorables (hb_buffer_t *buffer,
                               hb_font_t   *font)
{
  if (!(buffer->scratch_flags & HB_BUFFER_SCRATCH_FLAG_HAS_DEFAULT_IGNORABLES) ||
      (buffer->flags & HB_BUFFER_FLAG_PRESERVE_DEFAULT_IGNORABLES))
    return;

  unsigned int count = buffer->len;
  hb_glyph_info_t *info = buffer->info;

  hb_codepoint_t invisible = buffer->invisible;
  if (!(buffer->flags & HB_BUFFER_FLAG_REMOVE_DEFAULT_IGNORABLES) &&
      (invisible || font->get_nominal_glyph (' ', &invisible)))
  {
    /* Replace default-ignorables with a zero-advance invisible glyph. */
    for (unsigned int i = 0; i < count; i++)
    {
      if (_hb_glyph_info_is_default_ignorable (&info[i]))
        info[i].codepoint = invisible;
    }
  }
  else
    hb_ot_layout_delete_glyphs_inplace (buffer, _hb_glyph_info_is_default_ignorable);
}


static inline void
hb_ot_map_glyphs_fast (hb_buffer_t  *buffer)
{
  /* Normalization process sets up glyph_index(), we just copy it. */
  unsigned int count = buffer->len;
  hb_glyph_info_t *info = buffer->info;
  for (unsigned int i = 0; i < count; i++)
    info[i].codepoint = info[i].glyph_index();

  buffer->content_type = HB_BUFFER_CONTENT_TYPE_GLYPHS;
}

static inline void
hb_synthesize_glyph_classes (hb_buffer_t *buffer)
{
  unsigned int count = buffer->len;
  hb_glyph_info_t *info = buffer->info;
  for (unsigned int i = 0; i < count; i++)
  {
    hb_ot_layout_glyph_props_flags_t klass;

    /* Never mark default-ignorables as marks.
     * They won't get in the way of lookups anyway,
     * but having them as mark will cause them to be skipped
     * over if the lookup-flag says so, but at least for the
     * Mongolian variation selectors, looks like Uniscribe
     * marks them as non-mark.  Some Mongolian fonts without
     * GDEF rely on this.  Another notable character that
     * this applies to is COMBINING GRAPHEME JOINER. */
    klass = (_hb_glyph_info_get_general_category (&info[i]) !=
             HB_UNICODE_GENERAL_CATEGORY_NON_SPACING_MARK ||
             _hb_glyph_info_is_default_ignorable (&info[i])) ?
            HB_OT_LAYOUT_GLYPH_PROPS_BASE_GLYPH :
            HB_OT_LAYOUT_GLYPH_PROPS_MARK;
    _hb_glyph_info_set_glyph_props (&info[i], klass);
  }
}

static inline void
hb_ot_substitute_default (const hb_ot_shape_context_t *c)
{
  hb_buffer_t *buffer = c->buffer;

  hb_ot_rotate_chars (c);

  HB_BUFFER_ALLOCATE_VAR (buffer, glyph_index);

  _hb_ot_shape_normalize (c->plan, buffer, c->font);

  hb_ot_shape_setup_masks (c);

  /* This is unfortunate to go here, but necessary... */
  if (c->plan->fallback_mark_positioning)
    _hb_ot_shape_fallback_mark_position_recategorize_marks (c->plan, c->font, buffer);

  hb_ot_map_glyphs_fast (buffer);

  HB_BUFFER_DEALLOCATE_VAR (buffer, glyph_index);
}

static inline void
hb_ot_substitute_complex (const hb_ot_shape_context_t *c)
{
  hb_buffer_t *buffer = c->buffer;

  hb_ot_layout_substitute_start (c->font, buffer);

  if (c->plan->fallback_glyph_classes)
    hb_synthesize_glyph_classes (c->buffer);

  c->plan->substitute (c->font, buffer);
}

static inline void
hb_ot_substitute_pre (const hb_ot_shape_context_t *c)
{
  hb_ot_substitute_default (c);

  _hb_buffer_allocate_gsubgpos_vars (c->buffer);

  hb_ot_substitute_complex (c);
}

static inline void
hb_ot_substitute_post (const hb_ot_shape_context_t *c)
{
  hb_ot_hide_default_ignorables (c->buffer, c->font);
#ifndef HB_NO_AAT_SHAPE
  if (c->plan->apply_morx)
    hb_aat_layout_remove_deleted_glyphs (c->buffer);
#endif

  if (c->plan->shaper->postprocess_glyphs &&
    c->buffer->message(c->font, "start postprocess-glyphs")) {
    c->plan->shaper->postprocess_glyphs (c->plan, c->buffer, c->font);
    (void) c->buffer->message(c->font, "end postprocess-glyphs");
  }
}


/*
 * Position
 */

static inline void
adjust_mark_offsets (hb_glyph_position_t *pos)
{
  pos->x_offset -= pos->x_advance;
  pos->y_offset -= pos->y_advance;
}

static inline void
zero_mark_width (hb_glyph_position_t *pos)
{
  pos->x_advance = 0;
  pos->y_advance = 0;
}

static inline void
zero_mark_widths_by_gdef (hb_buffer_t *buffer, bool adjust_offsets)
{
  unsigned int count = buffer->len;
  hb_glyph_info_t *info = buffer->info;
  for (unsigned int i = 0; i < count; i++)
    if (_hb_glyph_info_is_mark (&info[i]))
    {
      if (adjust_offsets)
        adjust_mark_offsets (&buffer->pos[i]);
      zero_mark_width (&buffer->pos[i]);
    }
}

static inline void
hb_ot_position_default (const hb_ot_shape_context_t *c)
{
  hb_direction_t direction = c->buffer->props.direction;
  unsigned int count = c->buffer->len;
  hb_glyph_info_t *info = c->buffer->info;
  hb_glyph_position_t *pos = c->buffer->pos;

  if (HB_DIRECTION_IS_HORIZONTAL (direction))
  {
    c->font->get_glyph_h_advances (count, &info[0].codepoint, sizeof(info[0]),
                                   &pos[0].x_advance, sizeof(pos[0]));
    /* The nil glyph_h_origin() func returns 0, so no need to apply it. */
    if (c->font->has_glyph_h_origin_func ())
      for (unsigned int i = 0; i < count; i++)
        c->font->subtract_glyph_h_origin (info[i].codepoint,
                                          &pos[i].x_offset,
                                          &pos[i].y_offset);
  }
  else
  {
    c->font->get_glyph_v_advances (count, &info[0].codepoint, sizeof(info[0]),
                                   &pos[0].y_advance, sizeof(pos[0]));
    for (unsigned int i = 0; i < count; i++)
    {
      c->font->subtract_glyph_v_origin (info[i].codepoint,
                                        &pos[i].x_offset,
                                        &pos[i].y_offset);
    }
  }
  if (c->buffer->scratch_flags & HB_BUFFER_SCRATCH_FLAG_HAS_SPACE_FALLBACK)
    _hb_ot_shape_fallback_spaces (c->plan, c->font, c->buffer);
}

static inline void
hb_ot_position_complex (const hb_ot_shape_context_t *c)
{
  unsigned int count = c->buffer->len;
  hb_glyph_info_t *info = c->buffer->info;
  hb_glyph_position_t *pos = c->buffer->pos;

  /* If the font has no GPOS and direction is forward, then when
   * zeroing mark widths, we shift the mark with it, such that the
   * mark is positioned hanging over the previous glyph.  When
   * direction is backward we don't shift and it will end up
   * hanging over the next glyph after the final reordering.
   *
   * Note: If fallback positinoing happens, we don't care about
   * this as it will be overriden.
   */
  bool adjust_offsets_when_zeroing = c->plan->adjust_mark_positioning_when_zeroing &&
                                     HB_DIRECTION_IS_FORWARD (c->buffer->props.direction);

  /* We change glyph origin to what GPOS expects (horizontal), apply GPOS, change it back. */

  /* The nil glyph_h_origin() func returns 0, so no need to apply it. */
  if (c->font->has_glyph_h_origin_func ())
    for (unsigned int i = 0; i < count; i++)
      c->font->add_glyph_h_origin (info[i].codepoint,
                                   &pos[i].x_offset,
                                   &pos[i].y_offset);

  hb_ot_layout_position_start (c->font, c->buffer);

  if (c->plan->zero_marks)
    switch (c->plan->shaper->zero_width_marks)
    {
      case HB_OT_SHAPE_ZERO_WIDTH_MARKS_BY_GDEF_EARLY:
        zero_mark_widths_by_gdef (c->buffer, adjust_offsets_when_zeroing);
        break;

      default:
      case HB_OT_SHAPE_ZERO_WIDTH_MARKS_NONE:
      case HB_OT_SHAPE_ZERO_WIDTH_MARKS_BY_GDEF_LATE:
        break;
    }

  c->plan->position (c->font, c->buffer);

  if (c->plan->zero_marks)
    switch (c->plan->shaper->zero_width_marks)
    {
      case HB_OT_SHAPE_ZERO_WIDTH_MARKS_BY_GDEF_LATE:
        zero_mark_widths_by_gdef (c->buffer, adjust_offsets_when_zeroing);
        break;

      default:
      case HB_OT_SHAPE_ZERO_WIDTH_MARKS_NONE:
      case HB_OT_SHAPE_ZERO_WIDTH_MARKS_BY_GDEF_EARLY:
        break;
    }

  /* Finish off.  Has to follow a certain order. */
  hb_ot_layout_position_finish_advances (c->font, c->buffer);
  hb_ot_zero_width_default_ignorables (c->buffer);
#ifndef HB_NO_AAT_SHAPE
  if (c->plan->apply_morx)
    hb_aat_layout_zero_width_deleted_glyphs (c->buffer);
#endif
  hb_ot_layout_position_finish_offsets (c->font, c->buffer);

  /* The nil glyph_h_origin() func returns 0, so no need to apply it. */
  if (c->font->has_glyph_h_origin_func ())
    for (unsigned int i = 0; i < count; i++)
      c->font->subtract_glyph_h_origin (info[i].codepoint,
                                        &pos[i].x_offset,
                                        &pos[i].y_offset);

  if (c->plan->fallback_mark_positioning)
    _hb_ot_shape_fallback_mark_position (c->plan, c->font, c->buffer,
                                         adjust_offsets_when_zeroing);
}

static inline void
hb_ot_position (const hb_ot_shape_context_t *c)
{
  c->buffer->clear_positions ();

  hb_ot_position_default (c);

  hb_ot_position_complex (c);

  if (HB_DIRECTION_IS_BACKWARD (c->buffer->props.direction))
    hb_buffer_reverse (c->buffer);

  _hb_buffer_deallocate_gsubgpos_vars (c->buffer);
}

static inline void
hb_propagate_flags (hb_buffer_t *buffer)
{
  /* Propagate cluster-level glyph flags to be the same on all cluster glyphs.
   * Simplifies using them. */

  if (!(buffer->scratch_flags & HB_BUFFER_SCRATCH_FLAG_HAS_UNSAFE_TO_BREAK))
    return;

  hb_glyph_info_t *info = buffer->info;

  foreach_cluster (buffer, start, end)
  {
    unsigned int mask = 0;
    for (unsigned int i = start; i < end; i++)
      if (info[i].mask & HB_GLYPH_FLAG_UNSAFE_TO_BREAK)
      {
         mask = HB_GLYPH_FLAG_UNSAFE_TO_BREAK;
         break;
      }
    if (mask)
      for (unsigned int i = start; i < end; i++)
        info[i].mask |= mask;
  }
}

/* Pull it all together! */

static void
hb_ot_shape_internal (hb_ot_shape_context_t *c)
{
  c->buffer->deallocate_var_all ();
  c->buffer->scratch_flags = HB_BUFFER_SCRATCH_FLAG_DEFAULT;
  if (likely (!hb_unsigned_mul_overflows (c->buffer->len, HB_BUFFER_MAX_LEN_FACTOR)))
  {
    c->buffer->max_len = hb_max (c->buffer->len * HB_BUFFER_MAX_LEN_FACTOR,
                                 (unsigned) HB_BUFFER_MAX_LEN_MIN);
  }
  if (likely (!hb_unsigned_mul_overflows (c->buffer->len, HB_BUFFER_MAX_OPS_FACTOR)))
  {
    c->buffer->max_ops = hb_max (c->buffer->len * HB_BUFFER_MAX_OPS_FACTOR,
                                 (unsigned) HB_BUFFER_MAX_OPS_MIN);
  }

  /* Save the original direction, we use it later. */
  c->target_direction = c->buffer->props.direction;

  _hb_buffer_allocate_unicode_vars (c->buffer);

  c->buffer->clear_output ();

  hb_ot_shape_initialize_masks (c);
  hb_set_unicode_props (c->buffer);
  hb_insert_dotted_circle (c->buffer, c->font);

  hb_form_clusters (c->buffer);

  hb_ensure_native_direction (c->buffer);

  if (c->plan->shaper->preprocess_text &&
    c->buffer->message(c->font, "start preprocess-text")) {
    c->plan->shaper->preprocess_text (c->plan, c->buffer, c->font);
    (void) c->buffer->message(c->font, "end preprocess-text");
  }

  hb_ot_substitute_pre (c);
  hb_ot_position (c);
  hb_ot_substitute_post (c);

  hb_propagate_flags (c->buffer);

  _hb_buffer_deallocate_unicode_vars (c->buffer);

  c->buffer->props.direction = c->target_direction;

  c->buffer->max_len = HB_BUFFER_MAX_LEN_DEFAULT;
  c->buffer->max_ops = HB_BUFFER_MAX_OPS_DEFAULT;
  c->buffer->deallocate_var_all ();
}


hb_bool_t
_hb_ot_shape (hb_shape_plan_t    *shape_plan,
              hb_font_t          *font,
              hb_buffer_t        *buffer,
              const hb_feature_t *features,
              unsigned int        num_features)
{
  hb_ot_shape_context_t c = {&shape_plan->ot, font, font->face, buffer, features, num_features};
  hb_ot_shape_internal (&c);

  return true;
}


/**
 * hb_ot_shape_plan_collect_lookups:
 * @shape_plan: #hb_shape_plan_t to query
 * @table_tag: GSUB or GPOS
 * @lookup_indexes: (out): The #hb_set_t set of lookups returned
 *
 * Computes the complete set of GSUB or GPOS lookups that are applicable
 * under a given @shape_plan.
 *
 * Since: 0.9.7
 **/
void
hb_ot_shape_plan_collect_lookups (hb_shape_plan_t *shape_plan,
                                  hb_tag_t         table_tag,
                                  hb_set_t        *lookup_indexes /* OUT */)
{
  shape_plan->ot.collect_lookups (table_tag, lookup_indexes);
}


/* TODO Move this to hb-ot-shape-normalize, make it do decompose, and make it public. */
static void
add_char (hb_font_t          *font,
          hb_unicode_funcs_t *unicode,
          hb_bool_t           mirror,
          hb_codepoint_t      u,
          hb_set_t           *glyphs)
{
  hb_codepoint_t glyph;
  if (font->get_nominal_glyph (u, &glyph))
    glyphs->add (glyph);
  if (mirror)
  {
    hb_codepoint_t m = unicode->mirroring (u);
    if (m != u && font->get_nominal_glyph (m, &glyph))
      glyphs->add (glyph);
  }
}


/**
 * hb_ot_shape_glyphs_closure:
 * @font: #hb_font_t to work upon
 * @buffer: The input buffer to compute from
 * @features: (array length=num_features): The features enabled on the buffer
 * @num_features: The number of features enabled on the buffer
 * @glyphs: (out): The #hb_set_t set of glyphs comprising the transitive closure of the query
 *
 * Computes the transitive closure of glyphs needed for a specified
 * input buffer under the given font and feature list. The closure is
 * computed as a set, not as a list.
 *
 * Since: 0.9.2
 **/
void
hb_ot_shape_glyphs_closure (hb_font_t          *font,
                            hb_buffer_t        *buffer,
                            const hb_feature_t *features,
                            unsigned int        num_features,
                            hb_set_t           *glyphs)
{
  const char *shapers[] = {"ot", nullptr};
  hb_shape_plan_t *shape_plan = hb_shape_plan_create_cached (font->face, &buffer->props,
                                                             features, num_features, shapers);

  bool mirror = hb_script_get_horizontal_direction (buffer->props.script) == HB_DIRECTION_RTL;

  unsigned int count = buffer->len;
  hb_glyph_info_t *info = buffer->info;
  for (unsigned int i = 0; i < count; i++)
    add_char (font, buffer->unicode, mirror, info[i].codepoint, glyphs);

  hb_set_t *lookups = hb_set_create ();
  hb_ot_shape_plan_collect_lookups (shape_plan, HB_OT_TAG_GSUB, lookups);
  hb_ot_layout_lookups_substitute_closure (font->face, lookups, glyphs);

  hb_set_destroy (lookups);

  hb_shape_plan_destroy (shape_plan);
}


#endif
