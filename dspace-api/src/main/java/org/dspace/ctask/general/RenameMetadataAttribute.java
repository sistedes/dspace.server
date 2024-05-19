package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Curation task to move the value of one metadata field to another
 * 
 * @author agomez
 */
public class RenameMetadataAttribute extends AbstractCurationTask {

	private static final String CONF_SOURCE = "source";
	private static final String CONF_TARGET = "target";
	private static final String CONF_LANGUAGE = "language";
	
	protected String result = null;
	
	protected String taskProperty(String name, String defaultValue) {
		return super.taskProperty(name) != null ? super.taskProperty(name) : defaultValue;
	}

	@Override
	public int perform(DSpaceObject dso) throws IOException {

		String source = taskProperty(CONF_SOURCE);
		String target = taskProperty(CONF_TARGET);
		String language = taskProperty(CONF_LANGUAGE, Item.ANY);
		
		if (StringUtils.isEmpty(source)) {
			result = "No source metadata attribute specified!";
			setResult(result);
			report(result);
			return Curator.CURATE_ERROR;
		}
		
		if (StringUtils.isEmpty(target)) {
			result = "No target metadata attribute specified!";
			setResult(result);
			report(result);
			return Curator.CURATE_ERROR;
		}
		
		if (dso instanceof Item) {
			MetadataFieldName sourceField = new MetadataFieldName(source);
			MetadataFieldName targetField = new MetadataFieldName(target);
			try {
				Item item = (Item) dso;
				List<MetadataValue> values = itemService.getMetadata(item, sourceField.schema, sourceField.element, sourceField.qualifier, language);
				if (!values.isEmpty()) {
					values.forEach(v -> {
						try {
							itemService.addMetadata(Curator.curationContext(), item, targetField.schema, targetField.element, targetField.qualifier, v.getLanguage(), v.getValue());
						} catch (SQLException e) {
							report("An error occurred: " + e.getMessage());
						}
					});
					itemService.clearMetadata(Curator.curationContext(), item, sourceField.schema, sourceField.element, sourceField.qualifier, language);
					result = "Attribute succesfully renamed in Item " + dso.getID();
					setResult(result);
					report(result);
					return Curator.CURATE_SUCCESS;
				} else {
					result = "Source attribute not found in Item " + dso.getID();
					setResult(result);
					report(result);
					return Curator.CURATE_SKIP;
				}
			} catch (Exception e) {
				result = "Failed rename attribute in Item " + dso.getID();
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
}
