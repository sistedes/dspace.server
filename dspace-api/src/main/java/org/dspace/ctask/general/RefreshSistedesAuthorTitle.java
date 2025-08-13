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
import org.dspace.content.MetadataFieldName;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Curation task to set the 'dc.title' of an 'Autor' entity type out of its 
 * 'person.familyName' and 'person.givenName' fields
 * 
 * @author agomez
 */
public class RefreshSistedesAuthorTitle extends AbstractCurationTask {

    protected String result = null;

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso instanceof Item && "Autor".equals(itemService.getMetadata((Item) dso, "dspace.entity.type"))) {
            Item item = (Item) dso;
            try {
                String familyName = itemService.getMetadataFirstValue(item, new MetadataFieldName("person.familyName"), Item.ANY);
                String givenName = itemService.getMetadataFirstValue(item, new MetadataFieldName("person.givenName"), Item.ANY);
                String title = StringUtils.isNotBlank(familyName) && StringUtils.isNotBlank(givenName) ?
                                familyName + ", " + givenName : StringUtils.trimToEmpty(familyName + givenName);
                itemService.clearMetadata(Curator.curationContext(), item, "dc", "title", null, Item.ANY);
                itemService.setMetadataSingleValue(Curator.curationContext(), item, "dc", "title", null, null, title);
                result = "Title succesfully set in Author " + dso.getID();
                setResult(result);
                report(result);
                return Curator.CURATE_SUCCESS;
            } catch (Exception e) {
                result = "Failed to set title in Author " + dso.getID();
                setResult(result);
                report(result);
                return Curator.CURATE_FAIL;
            }
        } else {
            result = "Skipping non-Author element" + dso.getID();
            setResult(result);
            report(result);
            return Curator.CURATE_SKIP;
        }
    }
}
