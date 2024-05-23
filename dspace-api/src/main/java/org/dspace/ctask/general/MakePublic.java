package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * Curation taks to make all {@link DSpaceObject}s public
 * 
 * @author agomez
 */
public class MakePublic extends AbstractCurationTask {

	private ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
	private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

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
			Group anonymous = groupService.findByName(context, Group.ANONYMOUS);
			resourcePolicyService.removePolicies(context, dso, Constants.READ);
			if (dso instanceof Item) {
				Item item = (Item) dso;
				authorizeService.createResourcePolicy(context, dso, anonymous, null, Constants.READ, ResourcePolicy.TYPE_INHERITED);
				List<Bundle> bundles = new ArrayList<Bundle>();
				bundles.addAll(item.getBundles("ORIGINAL"));
				bundles.addAll(item.getBundles("TEXT"));
				bundles.addAll(item.getBundles("THUMBNAIL"));
				for (Bundle bundle : bundles) {
					resourcePolicyService.removePolicies(context, bundle, Constants.READ);
					authorizeService.createResourcePolicy(context, bundle, anonymous, null, Constants.READ, ResourcePolicy.TYPE_INHERITED);
					for (Bitstream bitstream: bundle.getBitstreams()) {
						resourcePolicyService.removePolicies(context, bitstream, Constants.READ);
						authorizeService.createResourcePolicy(context, bitstream, anonymous, null, Constants.READ, null);
					}
				}
			} else {
				authorizeService.createResourcePolicy(context, dso, anonymous, null, Constants.READ, null);
			}
			result = "DSpaceObject has been successfully made public: " + dso.getID();
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