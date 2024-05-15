package org.dspace.ctask.general;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.dspace.app.mediafilter.FormatFilter;
import org.dspace.app.mediafilter.factory.MediaFilterServiceFactory;
import org.dspace.app.mediafilter.service.MediaFilterService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Curation taks to apply media filters. Useful to execute this curation task as
 * part of a submission workflow.
 * 
 * @author agomez
 */
public class FilterMedia extends AbstractCurationTask {
    
    // Key (in dspace.cfg) which lists all enabled filters by name
    private static final String MEDIA_FILTER_PLUGINS_KEY = "filter.plugins";

    // Prefix (in dspace.cfg) for all filter properties
    private static final String FILTER_PREFIX = "filter";

    // Suffix (in dspace.cfg) for input formats supported by each filter
    private static final String INPUT_FORMATS_SUFFIX = "inputFormats";
    
    // The MediaFilterService
	private MediaFilterService mediaFilterService = MediaFilterServiceFactory.getInstance().getMediaFilterService();

	protected String result = null;

	// Static initializer
	{
		// Configure all enabled filters in the MediaFilterService
		// Adapted from org.dspace.app.mediafilter.MediaFilterScript
		String[] filterNames = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty(MEDIA_FILTER_PLUGINS_KEY);

        List<FormatFilter> filterList = new ArrayList<>();
        Map<String, List<String>> filterFormats = new HashMap<>();
		
        for (int i = 0; i < filterNames.length; i++) {
			FormatFilter filter = (FormatFilter) CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(FormatFilter.class, filterNames[i]);		            
            filterList.add(filter);
            String filterClassName = filter.getClass().getName();
            String pluginName = null;
			if (SelfNamedPlugin.class.isAssignableFrom(filter.getClass())) {
				pluginName = ((SelfNamedPlugin) filter).getPluginInstanceName();
			}
			String[] formats = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty(
					FILTER_PREFIX + "." + filterClassName + (pluginName != null ? "." + pluginName : "") + "." + INPUT_FORMATS_SUFFIX);
			if (ArrayUtils.isNotEmpty(formats)) {
				filterFormats.put(filterClassName + (pluginName != null ? MediaFilterService.FILTER_PLUGIN_SEPARATOR + pluginName : ""),
						Arrays.asList(formats));
			}
		}
		mediaFilterService.setFilterFormats(filterFormats);
		mediaFilterService.setFilterClasses(filterList);
	}
	
	protected String taskProperty(String name, String defaultValue) {
		return super.taskProperty(name) != null ? super.taskProperty(name) : defaultValue;
	}

	@Override
	public int perform(DSpaceObject dso) throws IOException {
		if (dso instanceof Item) {
			try {
				if (mediaFilterService.filterItem(Curator.curationContext(), (Item) dso)) {
					result = "Media filters succesfully applied on Item " + dso.getID();
					setResult(result);
					report(result);
					return Curator.CURATE_SUCCESS;
				} else {
					result = "No media filters applied on Item " + dso.getID();
					setResult(result);
					report(result);
					return Curator.CURATE_SKIP;
				}
			} catch (Exception e) {
				result = "Failed to filter media on Item " + dso.getID();
				setResult(result);
				report(result);
				return Curator.CURATE_FAIL;
			}
		} else {
			result = "Skipping element that cannot be media filtered: " + dso.getID() + "[" + Constants.typeText[dso.getType()] + "]";
			setResult(result);
			report(result);
			return Curator.CURATE_SKIP;
		}
	}
}
