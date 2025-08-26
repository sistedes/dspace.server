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
 * Curation task to delete the values of a metadata field
 * 
 * @author agomez
 */
public class DeleteMetadataAttribute extends AbstractCurationTask {

    private static final String CONF_FIELD = "field";
    private static final String CONF_LANGUAGE = "language";

    protected String result = null;

    protected String taskProperty(String name, String defaultValue) {
        return super.taskProperty(name) != null ? super.taskProperty(name) : defaultValue;
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {

        String field = taskProperty(CONF_FIELD);
        String language = taskProperty(CONF_LANGUAGE, Item.ANY);
        
        if (StringUtils.isEmpty(field)) {
            result = "No metadata attribute specified!";
            setResult(result);
            report(result);
            return Curator.CURATE_ERROR;
        }

        MetadataFieldName metadataField = new MetadataFieldName(field);
        try {
            Item item = (Item) dso;
            itemService.clearMetadata(Curator.curationContext(), 
                            item, metadataField.schema, metadataField.element, metadataField.qualifier, language);
            result = "Attribute succesfully deleted in Item " + dso.getID();
            setResult(result);
            report(result);
            return Curator.CURATE_SUCCESS;
        } catch (Exception e) {
            result = "Failed rename attribute in Item " + dso.getID();
            setResult(result);
            report(result);
            return Curator.CURATE_FAIL;
        }
    }
}
