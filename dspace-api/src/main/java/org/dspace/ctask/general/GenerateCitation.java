/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Curation task to set the 'dc.identifier.citation' of an Item
 * 
 * @author agomez
 */
public class GenerateCitation extends AbstractCurationTask {

    protected static final String CONF_FORCE = "force";

    protected String result = null;

    @Override
    public int perform(DSpaceObject dso) throws IOException {

        boolean force = taskBooleanProperty(CONF_FORCE, false);

        if (dso instanceof Item) {
            Item item = (Item) dso;
            if (StringUtils.equals("Autor", itemService.getMetadata(item, "dspace.entity.type"))) {
                result = "Skipping Author element" + dso.getID();
                setResult(result);
                report(result);
                return Curator.CURATE_SKIP;
            }
            try {
                if (StringUtils.isEmpty(itemService.getMetadata(item, "dc.identifier.citation")) || force) {
                    itemService.clearMetadata(Curator.curationContext(), item, "dc", "identifier", "citation", Item.ANY);
                    itemService.setMetadataSingleValue(Curator.curationContext(), item, "dc", "identifier", "citation", null, createCitation(item).asTextCitation());
                    result = "Property 'dc.identifier.citation' succesfully set in Item " + dso.getID();
                    setResult(result);
                    report(result);
                    return Curator.CURATE_SUCCESS;
                } else {
                    result = "Property 'dc.identifier.citation' already exists (maybe you forgot the 'force=true' parameter?). Skipping Item " + dso.getID();
                    setResult(result);
                    report(result);
                    return Curator.CURATE_SKIP;
                }
            } catch (Exception e) {
                result = "Failed to set 'dc.identifier.citation' in Item " + dso.getID();
                setResult(result);
                report(result);
                return Curator.CURATE_FAIL;
            }
        } else {
            result = "Skipping non-Item element " + dso.getID();
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

    protected class Citation {

        protected Item item;

        protected Citation(Item item) {
            this.item = item;
        }

        protected static List<String> abbreviateNames(List<String> fullnames) {
            List<String> result = new ArrayList<>();
            for (String fullname : fullnames) {
                if (!fullname.contains(",")) {
                    result.add(fullname);
                } else {
                    String surname = fullname.split(",")[0];
                    String name = Arrays
                                    .asList(fullname.split(",")[1].trim().split("\\s+"))
                                    .stream()
                                    .map(n -> n.substring(0, 1) + '.')
                                    .collect(Collectors.joining(" "));
                    result.add(surname + ", " + name);
                }
            }
            return result;
        }

        protected String getTitle() {
            return itemService.getMetadata(item, "dc.title");
        }

        protected List<String> getAuthors() {
            List<String> signatures = itemService.getMetadata(item, "dc.contributor.signature", Item.ANY).stream().map(s -> s.getValue()).toList();
            return Citation.abbreviateNames(
                signatures.size() > 0 
                ? signatures
                : itemService.getMetadata(item, "dc.contributor.author", Item.ANY).stream().map(s -> s.getValue()).toList());
        }

        protected List<String> getIsPartOf() {
            return itemService.getMetadata(item, "dc.relation.ispartof", Item.ANY).stream().map(s -> s.getValue()).toList();
        }

        protected String getPublisher() {
            return itemService.getMetadata(item, "dc.publisher");
        }

        protected String getYear() {
            return String.valueOf(new DCDate(itemService.getMetadata(item, "dc.date.issued")).getYear());
        }

        protected String getConferenceAcronym() {
            return itemService.getMetadata(item, "bs.conference.acronym");
        }

        protected String getEditionName() {
            return itemService.getMetadata(item, "bs.edition.name");
        }

        protected String getEditionYear() {
            return String.valueOf(new DCDate(itemService.getMetadata(item, "bs.edition.date")).getYear());
        }

        protected List<String> getEditors() {
            return Citation.abbreviateNames(itemService.getMetadata(item, "bs.proceedings.editor", Item.ANY).stream().map(s -> s.getValue()).toList());
        }

        protected String getUri() {
            return "https://hdl.handle.net/" + this.getHandle();
        }

        protected String getHandle() {
            return StringUtils.isNotBlank(itemService.getMetadata(item, "dc.identifier.sistedes")) ? itemService.getMetadata(item, "dc.identifier.sistedes") : this.item.getHandle();
        }

        
        protected String asTextCitation() {
            return (this.getAuthors().size() > 0 ? StringUtils.join(this.getAuthors(), ", ") + ": " : "")
            + this.getTitle() + ". "
            + "In: "
            + ( this.getEditors().size() > 0 ? ( StringUtils.join(this.getEditors(), ", ") + " (ed" + (this.getEditors().size() > 1 ? "s" : "") + ".) " ) : "" )
            + StringUtils.join(this.getIsPartOf(), ", ") + ". "
            + this.getPublisher()
            + " (" + this.getYear() + "). "
            + this.getUri();
        }
    }

    protected class ConferenceCitation extends Citation {
        protected ConferenceCitation(Item item) {
            super(item);
        }
    }
}
