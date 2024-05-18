package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Curation taks to make all {@link DSpaceObject}s public with the exception
 * {@link Bundle}s and {@link Bitstream}s, that are left unchanged
 * 
 * @author agomez
 */
public class MakeAdminOnly extends AbstractCurationTask {

	private ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();

	protected String result = null;

	protected String taskProperty(String name, String defaultValue) {
		return super.taskProperty(name) != null ? super.taskProperty(name) : defaultValue;
	}

	@Override
	public int perform(DSpaceObject dso) throws IOException {
		if (dso instanceof Item) {
			if (!((Item) dso).isArchived()) {
				result = "Skipping not archived Item: " + dso.getID();
				setResult(result);
				report(result);
				return Curator.CURATE_SKIP;
			}
		}
		try {
			Context context = Curator.curationContext();
			resourcePolicyService.removePolicies(context, dso, Constants.READ);
			result = "DSpaceObject has been successfully made private only for Administrators: " + dso.getID();
			setResult(result);
			report(result);
			return Curator.CURATE_SUCCESS;
		} catch (SQLException | AuthorizeException e) {
			result = "Unable to change authorization policy for " + dso.getID();
			setResult(result);
			report(result);
			return Curator.CURATE_ERROR;
		}
	}
}
