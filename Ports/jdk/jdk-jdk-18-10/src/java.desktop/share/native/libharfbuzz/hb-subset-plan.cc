/*
 * Copyright © 2018  Google, Inc.
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
 * Google Author(s): Garret Rieger, Roderick Sheeter
 */

#include "hb-subset-plan.hh"
#include "hb-map.hh"
#include "hb-set.hh"

#include "hb-ot-cmap-table.hh"
#include "hb-ot-glyf-table.hh"
#include "hb-ot-layout-gdef-table.hh"
#include "hb-ot-layout-gpos-table.hh"
#include "hb-ot-layout-gsub-table.hh"
#include "hb-ot-cff1-table.hh"
#include "hb-ot-color-colr-table.hh"
#include "hb-ot-var-fvar-table.hh"
#include "hb-ot-stat-table.hh"


#ifndef HB_NO_SUBSET_CFF
static inline void
_add_cff_seac_components (const OT::cff1::accelerator_t &cff,
                          hb_codepoint_t gid,
                          hb_set_t *gids_to_retain)
{
  hb_codepoint_t base_gid, accent_gid;
  if (cff.get_seac_components (gid, &base_gid, &accent_gid))
  {
    gids_to_retain->add (base_gid);
    gids_to_retain->add (accent_gid);
  }
}
#endif

#ifndef HB_NO_SUBSET_LAYOUT
static void
_remap_indexes (const hb_set_t *indexes,
                hb_map_t       *mapping /* OUT */)
{
  unsigned count = indexes->get_population ();

  for (auto _ : + hb_zip (indexes->iter (), hb_range (count)))
    mapping->set (_.first, _.second);

}

static inline void
_gsub_closure_glyphs_lookups_features (hb_face_t *face,
                                       hb_set_t *gids_to_retain,
                                       hb_map_t *gsub_lookups,
                                       hb_map_t *gsub_features)
{
  hb_set_t lookup_indices;
  hb_ot_layout_collect_lookups (face,
                                HB_OT_TAG_GSUB,
                                nullptr,
                                nullptr,
                                nullptr,
                                &lookup_indices);
  hb_ot_layout_lookups_substitute_closure (face,
                                           &lookup_indices,
                                           gids_to_retain);
  hb_blob_ptr_t<OT::GSUB> gsub = hb_sanitize_context_t ().reference_table<OT::GSUB> (face);
  gsub->closure_lookups (face,
                         gids_to_retain,
                         &lookup_indices);
  _remap_indexes (&lookup_indices, gsub_lookups);

  // Collect and prune features
  hb_set_t feature_indices;
  hb_ot_layout_collect_features (face,
                                 HB_OT_TAG_GSUB,
                                 nullptr,
                                 nullptr,
                                 nullptr,
                                 &feature_indices);
  gsub->prune_features (gsub_lookups, &feature_indices);
  _remap_indexes (&feature_indices, gsub_features);

  gsub.destroy ();
}

static inline void
_gpos_closure_lookups_features (hb_face_t      *face,
                                const hb_set_t *gids_to_retain,
                                hb_map_t       *gpos_lookups,
                                hb_map_t       *gpos_features)
{
  hb_set_t lookup_indices;
  hb_ot_layout_collect_lookups (face,
                                HB_OT_TAG_GPOS,
                                nullptr,
                                nullptr,
                                nullptr,
                                &lookup_indices);
  hb_blob_ptr_t<OT::GPOS> gpos = hb_sanitize_context_t ().reference_table<OT::GPOS> (face);
  gpos->closure_lookups (face,
                         gids_to_retain,
                         &lookup_indices);
  _remap_indexes (&lookup_indices, gpos_lookups);

  // Collect and prune features
  hb_set_t feature_indices;
  hb_ot_layout_collect_features (face,
                                 HB_OT_TAG_GPOS,
                                 nullptr,
                                 nullptr,
                                 nullptr,
                                 &feature_indices);
  gpos->prune_features (gpos_lookups, &feature_indices);
  _remap_indexes (&feature_indices, gpos_features);
  gpos.destroy ();
}
#endif

#ifndef HB_NO_VAR
static inline void
  _collect_layout_variation_indices (hb_face_t *face,
                                     const hb_set_t *glyphset,
                                     const hb_map_t *gpos_lookups,
                                     hb_set_t  *layout_variation_indices,
                                     hb_map_t  *layout_variation_idx_map)
{
  hb_blob_ptr_t<OT::GDEF> gdef = hb_sanitize_context_t ().reference_table<OT::GDEF> (face);
  hb_blob_ptr_t<OT::GPOS> gpos = hb_sanitize_context_t ().reference_table<OT::GPOS> (face);

  if (!gdef->has_data ())
  {
    gdef.destroy ();
    gpos.destroy ();
    return;
  }
  OT::hb_collect_variation_indices_context_t c (layout_variation_indices, glyphset, gpos_lookups);
  gdef->collect_variation_indices (&c);

  if (hb_ot_layout_has_positioning (face))
    gpos->collect_variation_indices (&c);

  gdef->remap_layout_variation_indices (layout_variation_indices, layout_variation_idx_map);

  gdef.destroy ();
  gpos.destroy ();
}
#endif

static inline void
_cmap_closure (hb_face_t           *face,
               const hb_set_t      *unicodes,
               hb_set_t            *glyphset)
{
  OT::cmap::accelerator_t cmap;
  cmap.init (face);
  cmap.table->closure_glyphs (unicodes, glyphset);
  cmap.fini ();
}

static inline void
_remove_invalid_gids (hb_set_t *glyphs,
                      unsigned int num_glyphs)
{
  hb_codepoint_t gid = HB_SET_VALUE_INVALID;
  while (glyphs->next (&gid))
  {
    if (gid >= num_glyphs)
      glyphs->del (gid);
  }
}

static void
_populate_gids_to_retain (hb_subset_plan_t* plan,
                          const hb_set_t *unicodes,
                          const hb_set_t *input_glyphs_to_retain,
                          bool close_over_gsub,
                          bool close_over_gpos,
                          bool close_over_gdef)
{
  OT::cmap::accelerator_t cmap;
  OT::glyf::accelerator_t glyf;
#ifndef HB_NO_SUBSET_CFF
  OT::cff1::accelerator_t cff;
#endif
  OT::COLR::accelerator_t colr;
  cmap.init (plan->source);
  glyf.init (plan->source);
#ifndef HB_NO_SUBSET_CFF
  cff.init (plan->source);
#endif
  colr.init (plan->source);

  plan->_glyphset_gsub->add (0); // Not-def
  hb_set_union (plan->_glyphset_gsub, input_glyphs_to_retain);

  hb_codepoint_t cp = HB_SET_VALUE_INVALID;
  while (unicodes->next (&cp))
  {
    hb_codepoint_t gid;
    if (!cmap.get_nominal_glyph (cp, &gid))
    {
      DEBUG_MSG(SUBSET, nullptr, "Drop U+%04X; no gid", cp);
      continue;
    }
    plan->unicodes->add (cp);
    plan->codepoint_to_glyph->set (cp, gid);
    plan->_glyphset_gsub->add (gid);
  }

  _cmap_closure (plan->source, plan->unicodes, plan->_glyphset_gsub);

#ifndef HB_NO_SUBSET_LAYOUT
  if (close_over_gsub)
    // closure all glyphs/lookups/features needed for GSUB substitutions.
    _gsub_closure_glyphs_lookups_features (plan->source, plan->_glyphset_gsub, plan->gsub_lookups, plan->gsub_features);

  if (close_over_gpos)
    _gpos_closure_lookups_features (plan->source, plan->_glyphset_gsub, plan->gpos_lookups, plan->gpos_features);
#endif
  _remove_invalid_gids (plan->_glyphset_gsub, plan->source->get_num_glyphs ());

  // Populate a full set of glyphs to retain by adding all referenced
  // composite glyphs.
  hb_codepoint_t gid = HB_SET_VALUE_INVALID;
  while (plan->_glyphset_gsub->next (&gid))
  {
    glyf.add_gid_and_children (gid, plan->_glyphset);
#ifndef HB_NO_SUBSET_CFF
    if (cff.is_valid ())
      _add_cff_seac_components (cff, gid, plan->_glyphset);
#endif
    if (colr.is_valid ())
      colr.closure_glyphs (gid, plan->_glyphset);
  }

  _remove_invalid_gids (plan->_glyphset, plan->source->get_num_glyphs ());

#ifndef HB_NO_VAR
  if (close_over_gdef)
    _collect_layout_variation_indices (plan->source,
                                       plan->_glyphset_gsub,
                                       plan->gpos_lookups,
                                       plan->layout_variation_indices,
                                       plan->layout_variation_idx_map);
#endif

#ifndef HB_NO_SUBSET_CFF
  cff.fini ();
#endif
  glyf.fini ();
  cmap.fini ();
}

static void
_create_old_gid_to_new_gid_map (const hb_face_t *face,
                                bool             retain_gids,
                                const hb_set_t  *all_gids_to_retain,
                                hb_map_t        *glyph_map, /* OUT */
                                hb_map_t        *reverse_glyph_map, /* OUT */
                                unsigned int    *num_glyphs /* OUT */)
{
  if (!retain_gids)
  {
    + hb_enumerate (hb_iter (all_gids_to_retain), (hb_codepoint_t) 0)
    | hb_sink (reverse_glyph_map)
    ;
    *num_glyphs = reverse_glyph_map->get_population ();
  } else {
    + hb_iter (all_gids_to_retain)
    | hb_map ([] (hb_codepoint_t _) {
                return hb_pair_t<hb_codepoint_t, hb_codepoint_t> (_, _);
              })
    | hb_sink (reverse_glyph_map)
    ;

    unsigned max_glyph =
    + hb_iter (all_gids_to_retain)
    | hb_reduce (hb_max, 0u)
    ;
    *num_glyphs = max_glyph + 1;
  }

  + reverse_glyph_map->iter ()
  | hb_map (&hb_pair_t<hb_codepoint_t, hb_codepoint_t>::reverse)
  | hb_sink (glyph_map)
  ;
}

static void
_nameid_closure (hb_face_t *face,
                 hb_set_t  *nameids)
{
#ifndef HB_NO_STYLE
  face->table.STAT->collect_name_ids (nameids);
#endif
#ifndef HB_NO_VAR
  face->table.fvar->collect_name_ids (nameids);
#endif
}

/**
 * hb_subset_plan_create:
 * Computes a plan for subsetting the supplied face according
 * to a provided input. The plan describes
 * which tables and glyphs should be retained.
 *
 * Return value: New subset plan.
 *
 * Since: 1.7.5
 **/
hb_subset_plan_t *
hb_subset_plan_create (hb_face_t         *face,
                       hb_subset_input_t *input)
{
  hb_subset_plan_t *plan;
  if (unlikely (!(plan = hb_object_create<hb_subset_plan_t> ())))
    return const_cast<hb_subset_plan_t *> (&Null (hb_subset_plan_t));

  plan->successful = true;
  plan->drop_hints = input->drop_hints;
  plan->desubroutinize = input->desubroutinize;
  plan->retain_gids = input->retain_gids;
  plan->name_legacy = input->name_legacy;
  plan->unicodes = hb_set_create ();
  plan->name_ids = hb_set_reference (input->name_ids);
  _nameid_closure (face, plan->name_ids);
  plan->name_languages = hb_set_reference (input->name_languages);
  plan->glyphs_requested = hb_set_reference (input->glyphs);
  plan->drop_tables = hb_set_reference (input->drop_tables);
  plan->source = hb_face_reference (face);
  plan->dest = hb_face_builder_create ();

  plan->_glyphset = hb_set_create ();
  plan->_glyphset_gsub = hb_set_create ();
  plan->codepoint_to_glyph = hb_map_create ();
  plan->glyph_map = hb_map_create ();
  plan->reverse_glyph_map = hb_map_create ();
  plan->gsub_lookups = hb_map_create ();
  plan->gpos_lookups = hb_map_create ();
  plan->gsub_features = hb_map_create ();
  plan->gpos_features = hb_map_create ();
  plan->layout_variation_indices = hb_set_create ();
  plan->layout_variation_idx_map = hb_map_create ();

  _populate_gids_to_retain (plan,
                            input->unicodes,
                            input->glyphs,
                            !input->drop_tables->has (HB_OT_TAG_GSUB),
                            !input->drop_tables->has (HB_OT_TAG_GPOS),
                            !input->drop_tables->has (HB_OT_TAG_GDEF));

  _create_old_gid_to_new_gid_map (face,
                                  input->retain_gids,
                                  plan->_glyphset,
                                  plan->glyph_map,
                                  plan->reverse_glyph_map,
                                  &plan->_num_output_glyphs);

  return plan;
}

/**
 * hb_subset_plan_destroy:
 *
 * Since: 1.7.5
 **/
void
hb_subset_plan_destroy (hb_subset_plan_t *plan)
{
  if (!hb_object_destroy (plan)) return;

  hb_set_destroy (plan->unicodes);
  hb_set_destroy (plan->name_ids);
  hb_set_destroy (plan->name_languages);
  hb_set_destroy (plan->glyphs_requested);
  hb_set_destroy (plan->drop_tables);
  hb_face_destroy (plan->source);
  hb_face_destroy (plan->dest);
  hb_map_destroy (plan->codepoint_to_glyph);
  hb_map_destroy (plan->glyph_map);
  hb_map_destroy (plan->reverse_glyph_map);
  hb_set_destroy (plan->_glyphset);
  hb_set_destroy (plan->_glyphset_gsub);
  hb_map_destroy (plan->gsub_lookups);
  hb_map_destroy (plan->gpos_lookups);
  hb_map_destroy (plan->gsub_features);
  hb_map_destroy (plan->gpos_features);
  hb_set_destroy (plan->layout_variation_indices);
  hb_map_destroy (plan->layout_variation_idx_map);


  free (plan);
}
