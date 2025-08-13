/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.curate.Curator;

/**
 * Curation task to set the 'dc.identifier.citation-bibtex' of an Item
 * 
 * @author agomez
 */
public class GenerateBibTeXCitation extends GenerateCitation {

    @Override
    public int perform(DSpaceObject dso) throws IOException {

        boolean force = taskBooleanProperty(CONF_FORCE, false);

        if (dso instanceof Item) {
            Item item = (Item) dso;
            try {
                if (StringUtils.isEmpty(itemService.getMetadata(item, "dc.identifier.citation-bibtex")) || force) {
                    itemService.clearMetadata(Curator.curationContext(), item, "dc", "identifier", "citation-bibtex", Item.ANY);
                    itemService.setMetadataSingleValue(Curator.curationContext(), item, "dc", "identifier", "citation-bibtex", null, createCitation(item).asBibTexCitation());
                    result = "Property 'dc.identifier.citation-bibtex' succesfully set in Item " + dso.getID();
                    setResult(result);
                    report(result);
                    return Curator.CURATE_SUCCESS;
                } else {
                    result = "Property 'dc.identifier.citation-bibtex' already exists (maybe you forgot the 'force=true' parameter?). Skipping Item " + dso.getID();
                    setResult(result);
                    report(result);
                    return Curator.CURATE_SKIP;
                }
            } catch (Exception e) {
                result = "Failed to set 'dc.identifier.citation-bibtex' in Item " + dso.getID();
                setResult(result);
                report(result);
                return Curator.CURATE_FAIL;
            }
        } else {
            result = "Skipping non-Item element" + dso.getID();
            setResult(result);
            report(result);
            return Curator.CURATE_SKIP;
        }
    }
    
    protected Citation createCitation(Item item) {
        String entityType = itemService.getMetadata(item, "dspace.entity.type");
        if ("Resumen".equals(entityType) || "Artículo".equals(entityType)) {
            return new ConferenceCitation(item);
        } else {
            return new Citation(item);
        }
    }

    protected class Citation extends GenerateCitation.Citation {

        protected Citation(Item item) {
            super(item);
        }

        String asBibTexCitation() {
            return CitationUtilModule.escapeBibtex(
                "@misc{" + getHandle().replace("/", ":") + ",\n"
                + "  title     = {{" + getTitle() + "}},\n"
                + "  author    = {" + StringUtils.join(getAuthors(), " and ") + "},\n"
                + "  url       = {" + getUri() + "},\n"
                + "  year      = {" + getYear() + "},\n"
                + "  publisher = {{" + getPublisher() + "}},\n"
                + "  booktitle = {{" + StringUtils.join(getIsPartOf(), ", ") + "}}\n"
                + "}");
        }
    }

    protected class ConferenceCitation extends Citation {

        protected ConferenceCitation(Item item) {
            super(item);
        }

        String asBibTexCitation() {
            return CitationUtilModule.escapeBibtex(
                "@inproceedings{" + getHandle().replace("/", ":") + ",\n"
                + "  title     = {{" + getTitle() + "}},\n"
                + "  author    = {" + StringUtils.join(getAuthors(), " and ") + "},\n"
                + "  url       = {" + getUri() + "},\n"
                + "  crossref  = {" + getHandle().split("/")[0] + ':' + this.getConferenceAcronym() + ':' + this.getEditionYear() + "}\n"
                + "}\n\n"
                + "@proceedings{" + getHandle().split("/")[0] + ':' + this.getConferenceAcronym() + ':' + this.getEditionYear() + ",\n"
                + "  title     = {{" + StringUtils.join(getIsPartOf(), ", ") + "}},\n"
                + "  author    = {" + StringUtils.join(getEditors(), " and ") + "},\n"
                + "  year      = {" + getYear() + "},\n"
                + "  publisher = {{" + getPublisher() + "}},\n"
                + "}");
        }
    }

    protected class CitationUtilModule {
        public static String escapeBibtex(String text) {
            return text
                .replace("á", "\\'{a}")
                .replace("é", "\\'{e}")
                .replace("í", "\\'{i}")
                .replace("ó", "\\'{o}")
                .replace("ú", "\\'{u}")
                .replace("Á", "\\'{A}")
                .replace("É", "\\'{E}")
                .replace("Í", "\\'{I}")
                .replace("Ó", "\\'{O}")
                .replace("Ú", "\\'{U}")
                .replace("à", "\\`{a}")
                .replace("à", "\\`{e}")
                .replace("ì", "\\`{i}")
                .replace("ò", "\\`{o}")
                .replace("ù", "\\`{u}")
                .replace("À", "\\`{A}")
                .replace("È", "\\`{E}")
                .replace("Ì", "\\`{I}")
                .replace("Ò", "\\`{O}")
                .replace("Ù", "\\`{U}")
                .replace("â", "\\^{a}")
                .replace("ê", "\\^{e}")
                .replace("î", "\\^{i}")
                .replace("ô", "\\^{o}")
                .replace("û", "\\^{u}")
                .replace("Â", "\\^{A}")
                .replace("Ê", "\\^{E}")
                .replace("Î", "\\^{I}")
                .replace("Ô", "\\^{O}")
                .replace("Û", "\\^{U}")
                .replace("ä", "\\\"{a}")
                .replace("ë", "\\\"{e}")
                .replace("ï", "\\\"{i}")
                .replace("ö", "\\\"{o}")
                .replace("ü", "\\\"{u}")
                .replace("Ä", "\\\"{A}")
                .replace("Ë", "\\\"{E}")
                .replace("Ï", "\\\"{I}")
                .replace("Ö", "\\\"{O}")
                .replace("Ü", "\\\"{U}")
                .replace("ã", "\\~{a}")
                .replace("ẽ", "\\~{e}")
                .replace("ĩ", "\\~{i}")
                .replace("õ", "\\~{o}")
                .replace("ũ", "\\~{u}")
                .replace("Ã", "\\~{A}")
                .replace("Ẽ", "\\~{E}")
                .replace("Ĩ", "\\~{I}")
                .replace("Õ", "\\~{O}")
                .replace("Ũ", "\\~{U}");
        }
    }
}
