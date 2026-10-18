/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Curation task to set the 'dspace.orcid.authenticated' of an 'Autor' entity type 
 * 
 * @author agomez
 */
public class SetOrcidAuthenticatedSistedesAuthor extends AbstractCurationTask {

    protected String result = null;

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso instanceof Item && "Autor".equals(itemService.getMetadata((Item) dso, "dspace.entity.type"))) {
            Item item = (Item) dso;
            try {
                if (itemService.getMetadata(item, "person.identifier.orcid") != null 
                        && itemService.getMetadata(item, "dspace.orcid.authenticated") == null) {
                    String date = itemService.getMetadata(item, "dc.date.accessioned");
                    itemService.setMetadataSingleValue(Curator.curationContext(), item, new MetadataFieldName("dspace.orcid.authenticated"), null, date);
                }
                result = "Metadata 'dspace.orcid.authenticated' succesfully set in Author " + dso.getID();
                setResult(result);
                report(result);
                return Curator.CURATE_SUCCESS;
            } catch (Exception e) {
                result = "Failed to set 'dspace.orcid.authenticated' in Author " + dso.getID();
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
