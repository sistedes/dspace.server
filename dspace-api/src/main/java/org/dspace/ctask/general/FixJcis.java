package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Curation task to fix the JCIS name
 * 
 * @author agomez
 */
public class FixJcis extends AbstractCurationTask {

	private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

	protected String result = null;

	protected String taskProperty(String name, String defaultValue) {
		return super.taskProperty(name) != null ? super.taskProperty(name) : defaultValue;
	}

	@Override
	public int perform(DSpaceObject dso) throws IOException {

		List<MetadataFieldName> fields = new ArrayList<MetadataFieldName>();

		fields.add(new MetadataFieldName("bs.edition.name"));
		fields.add(new MetadataFieldName("dc.relation.ispartof"));
		fields.add(new MetadataFieldName("bs.conference.name"));
		fields.add(new MetadataFieldName("bs.proceedings.name"));
		fields.add(new MetadataFieldName("dc.description"));
		fields.add(new MetadataFieldName("dc.description.abstract"));

		try {

			for (MetadataFieldName field : fields) {
				if (dso instanceof Item) {
					ItemService service = itemService;
					List<MetadataValue> values= service.getMetadata((Item) dso, field.schema, field.element, field.qualifier, Item.ANY);
					if (!values.isEmpty()) {
						service.removeMetadataValues(Curator.curationContext(), (Item) dso, values);
						values.forEach(v -> {
							try {
								service.addMetadata(Curator.curationContext(), (Item) dso, field.schema, field.element, field.qualifier, null,
										v.getValue().replaceAll("Jornadas de Ingeniería de Ciencia e Ingeniería de Servicios",
												"Jornadas de Ciencia e Ingeniería de Servicios"));
							} catch (SQLException e) {
								report("An error occurred: " + e.getMessage());
							}
						});
					}
				} else if (dso instanceof Community) {
					CommunityService service = communityService;
					List<MetadataValue> values= service.getMetadata((Community) dso, field.schema, field.element, field.qualifier, Item.ANY);
					if (!values.isEmpty()) {
						service.removeMetadataValues(Curator.curationContext(), (Community) dso, values);
						values.forEach(v -> {
							try {
								service.addMetadata(Curator.curationContext(), (Community) dso, field.schema, field.element, field.qualifier, null,
										v.getValue().replaceAll("Jornadas de Ingeniería de Ciencia e Ingeniería de Servicios",
												"Jornadas de Ciencia e Ingeniería de Servicios"));
							} catch (SQLException e) {
								report("An error occurred: " + e.getMessage());
							}
						});
					}
				} else if (dso instanceof Collection) {
					CollectionService service = collectionService;
					List<MetadataValue> values= service.getMetadata((Collection) dso, field.schema, field.element, field.qualifier, Item.ANY);
					if (!values.isEmpty()) {
						service.removeMetadataValues(Curator.curationContext(), (Collection) dso, values);
						values.forEach(v -> {
							try {
								service.addMetadata(Curator.curationContext(), (Collection) dso, field.schema, field.element, field.qualifier, null,
										v.getValue().replaceAll("Jornadas de Ingeniería de Ciencia e Ingeniería de Servicios",
												"Jornadas de Ciencia e Ingeniería de Servicios"));
							} catch (SQLException e) {
								report("An error occurred: " + e.getMessage());
							}
						});
					}
				} else {
					result = "Unsupported DSpaceObject type: " + dso.getClass().getName();
					setResult(result);
					report(result);
					return Curator.CURATE_SKIP;
				}
			}
		} catch (Exception e) {
			result = "Failed rename attribute in DSpaceObject " + dso.getID();
			setResult(result);
			report(result);
			return Curator.CURATE_FAIL;
		}
		result = "JCIS name succesfully fixed in DSpaceObject " + dso.getID();
		setResult(result);
		report(result);
		return Curator.CURATE_SUCCESS;
	}
}
