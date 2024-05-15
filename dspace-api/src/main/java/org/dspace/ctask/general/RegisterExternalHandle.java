package org.dspace.ctask.general;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.SiteService;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractRequest;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Util;

/**
 * Curation taks to register in an external Handle server
 * the identifier stored in a specific DC property.
 * 
 * Used by the Sistedes Digital Library, which uses "semantic"
 * handles as a legacy decision
 * 
 * @author agomez
 */
public class RegisterExternalHandle extends AbstractCurationTask {

	private static final Logger logger = LogManager.getLogger(RegisterExternalHandle.class);

	private static final String CONF_PREFIX = "prefix";
	private static final String CONF_KEY = "key";
	private static final String CONF_PASSWORD = "password";
	private static final String CONF_PROPERTY = "property";
	private static final String CONF_FORCE = "force";

	private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
	private SiteService siteService = ContentServiceFactory.getInstance().getSiteService();

	protected String result = null;

	protected String taskProperty(String name, String defaultValue) {
		return super.taskProperty(name) != null ? super.taskProperty(name) : defaultValue;
	}
	
	@Override
	public int perform(DSpaceObject dso) throws IOException {

		String prefix = taskProperty(CONF_PREFIX, configurationService.getProperty("handle.prefix"));
		File keyFile = new File(taskProperty(CONF_KEY, "admpriv.bin"));
		String password = taskProperty(CONF_PASSWORD, "");
		String property = taskProperty(CONF_PROPERTY, "dc.identifier.uri");
		boolean force = taskBooleanProperty(CONF_FORCE, false);

		if (StringUtils.isEmpty(dso.getHandle())) {
			result = "DSpaceObject " + dso.getID() + " does not have a permanent Handle yet, skipping...: ";
			setResult(result);
			report(result);
			return Curator.CURATE_SKIP;
		}
		
		if (!keyFile.exists()) {
			result = "Key file " + keyFile.getAbsolutePath() + " to authenticate in prefix " + prefix + " does not exist";
			setResult(result);
			report(result);
			return Curator.CURATE_ERROR;
		}
		
		PublicKeyAuthenticationInfo auth;
		try {
			auth = getAuth(prefix, keyFile, password);
		} catch (Exception e) {
			result = "Unable to load authentication key on prefix " + prefix + ". Maybe wrong password?";
			setResult(result);
			report(result);
			return Curator.CURATE_ERROR;
		}

		List<MetadataValue> metadataList;
		if (dso instanceof Item) {
			metadataList = itemService.getMetadataByMetadataString((Item) dso, property);
		} else if (dso instanceof Community) {
			metadataList = communityService.getMetadataByMetadataString((Community) dso, property);
		} else if (dso instanceof Collection) {
			metadataList = collectionService.getMetadataByMetadataString((Collection) dso, property);
		} else if (dso instanceof Site) {
			metadataList = siteService.getMetadataByMetadataString((Site) dso, property);
		} else {
			result = "Unsupported DSpaceObject type: " + dso.getClass().getName();
			setResult(result);
			report(result);
			return Curator.CURATE_SKIP;
		}
		
		if (metadataList.isEmpty()) {
			result = "No metadata property " + property + " found for " + dso.getID();
			setResult(result);
			report(result);
			return Curator.CURATE_SKIP;
		}

		try {
			for (MetadataValue sistedesIdMetadata : metadataList) {
				if (!sistedesIdMetadata.getValue().startsWith(prefix + "/")) {
					report("Handle " + sistedesIdMetadata.getValue() + " does not reside in prefix " + prefix + ". Skiping." );
					continue;
				}
				registerHandleMapping(dso, prefix, sistedesIdMetadata.getValue(), auth, force);
			}
			result = "Sistedes Handles succesfully registered for " + dso.getID();
			setResult(result);
			report(result);
			return Curator.CURATE_SUCCESS;
		} catch (Exception e) {
			result = "Failed to register external Handle for " + dso.getID();
			setResult(result);
			report(result);
			return Curator.CURATE_FAIL;
		}
	}

	private void registerHandleMapping(DSpaceObject object, String externalPrefix, String externalHandle, PublicKeyAuthenticationInfo auth, boolean force)
			throws Exception {
		String targetUrl = handleService.getCanonicalForm(object.getHandle());
		logger.debug(MessageFormat.format("Creating handle ''{0}'' -> ''{1}''", externalHandle, object.getHandle()));
		HandleResolver resolver = new HandleResolver();
		int timestamp = (int) (System.currentTimeMillis() / 1000);

		HandleValue urlVal = new HandleValue(1, Util.encodeString("URL"), Util.encodeString(targetUrl), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null,
				true, true, true, false);

		if (force) {
			AbstractRequest request = new DeleteHandleRequest(Util.encodeString(externalHandle), auth);
			request.authoritative = true;
			if (resolver.processRequest(request).responseCode != AbstractMessage.RC_SUCCESS) {
				logger.error(MessageFormat.format("Unable to delete existing Handle for {0}", object.getHandle()));
			}
		}

		AdminRecord adminRecord = new AdminRecord(Util.encodeString("0.NA/" + externalPrefix), 300, true, true, true, true, true, true, true, true, true, true,
				true, true);
		HandleValue[] values = { urlVal, new HandleValue(100, Util.encodeString("HS_ADMIN"), Encoder.encodeAdminRecord(adminRecord),
				HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false) };
		AbstractRequest request = new CreateHandleRequest(Util.encodeString(externalHandle), values, auth);

		AbstractResponse response = resolver.processRequest(request);
		if (response.responseCode == AbstractMessage.RC_SUCCESS) {
			logger.info(MessageFormat.format("Created handle ''{0}'' -> ''{1}''", externalHandle, object.getHandle()));
		} else {
			throw new Exception("Unable to create external handle " + externalHandle + " for " + object.getHandle());
		}
	}

	private static PublicKeyAuthenticationInfo getAuth(String prefix, File keyFile, String password) throws Exception {
		byte key[] = IOUtils.toByteArray(keyFile.toURI());
		PrivateKey privkey = null;
		if (Util.requiresSecretKey(key) && StringUtils.isBlank(password)) {
			throw new Exception(MessageFormat.format("Private key in ''{0}'' requires a password", keyFile));
		}
		key = Util.decrypt(key, password.getBytes());
		privkey = Util.getPrivateKeyFromBytes(key, 0);
		return new PublicKeyAuthenticationInfo(Util.encodeString("0.NA/" + prefix), 300, privkey);
	}
}
