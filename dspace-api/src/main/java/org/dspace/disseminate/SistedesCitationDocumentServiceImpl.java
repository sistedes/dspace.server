/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.disseminate.service.CitationDocumentService;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Citation Document produces a dissemination package (DIP) that is different that the archival package (AIP).
 * In this case we append the descriptive metadata to the end (configurable) of the document. i.e. last page of PDF.
 * So instead of getting the original PDF, you get a cPDF (with citation information added).
 *
 * @author Peter Dietz (peter@longsight.com)
 */
public class SistedesCitationDocumentServiceImpl implements CitationDocumentService, InitializingBean {
    /**
     * Class Logger
     */
    private static final Logger log = LogManager.getLogger(SistedesCitationDocumentServiceImpl.class);

    /**
     * A set of MIME types that can have a citation page added to them. That is,
     * MIME types in this set can be converted to a PDF which is then prepended
     * with a citation page.
     */
    protected final Set<String> VALID_TYPES = new HashSet<>(2);

    /**
     * A set of MIME types that refer to a PDF
     */
    protected final Set<String> PDF_MIMES = new HashSet<>(2);

    /**
     * A set of MIME types that refer to a JPEG, PNG, or GIF
     */
    protected final Set<String> RASTER_MIMES = new HashSet<>();
    /**
     * A set of MIME types that refer to a SVG
     */
    protected final Set<String> SVG_MIMES = new HashSet<>();

    /**
     * List of all enabled collections, inherited/determined for those under communities.
     */
    protected List<String> citationEnabledCollectionsList;

    protected File tempDir;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected CommunityService communityService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected HandleService handleService;

    @Autowired
    CoverPageService coverPageService;

    @Override
    public void afterPropertiesSet() throws Exception {
        // Add valid format MIME types to set. This could be put in the Schema
        // instead.
        //Populate RASTER_MIMES
        SVG_MIMES.add("image/jpeg");
        SVG_MIMES.add("image/pjpeg");
        SVG_MIMES.add("image/png");
        SVG_MIMES.add("image/gif");
        //Populate SVG_MIMES
        SVG_MIMES.add("image/svg");
        SVG_MIMES.add("image/svg+xml");


        //Populate PDF_MIMES
        PDF_MIMES.add("application/pdf");
        PDF_MIMES.add("application/x-pdf");

        //Populate VALID_TYPES
        VALID_TYPES.addAll(PDF_MIMES);

        // Global enabled?
        citationEnabledGlobally = configurationService.getBooleanProperty("citation-page.enable_globally", false);

        //Load enabled collections
        String[] citationEnabledCollections = configurationService
                .getArrayProperty("citation-page.enabled_collections");
        citationEnabledCollectionsList = Arrays.asList(citationEnabledCollections);

        //Load enabled communities, and add to collection-list
        String[] citationEnabledCommunities = configurationService
                .getArrayProperty("citation-page.enabled_communities");
        if (citationEnabledCollectionsList == null) {
            citationEnabledCollectionsList = new ArrayList<>();
        }

        if (citationEnabledCommunities != null && citationEnabledCommunities.length > 0) {
            Context context = null;
            try {
                context = new Context();
                for (String communityString : citationEnabledCommunities) {
                    DSpaceObject dsoCommunity = handleService.resolveToObject(context, communityString.trim());
                    if (dsoCommunity instanceof Community) {
                        Community community = (Community) dsoCommunity;
                        List<Collection> collections = communityService.getAllCollections(context, community);

                        for (Collection collection : collections) {
                            citationEnabledCollectionsList.add(collection.getHandle());
                        }
                    } else {
                        log.error(
                                "Invalid community for citation.enabled_communities, value:" + communityString.trim());
                    }
                }
            } catch (SQLException e) {
                log.error(e.getMessage());
            } finally {
                if (context != null) {
                    context.abort();
                }
            }
        }

        //Ensure a temp directory is available
        String tempDirString = configurationService.getProperty("dspace.dir") + File.separator + "temp";
        tempDir = new File(tempDirString);
        if (!tempDir.exists()) {
            boolean success = tempDir.mkdir();
            if (success) {
                log.info("Created temp directory at: " + tempDirString);
            } else {
                log.info("Unable to create temp directory at: " + tempDirString);
            }
        }
    }

    protected SistedesCitationDocumentServiceImpl() {
    }

    /**
     * Boolean to determine is citation-functionality is enabled globally for entire site.
     * config/modules/citation-page: enable_globally, default false. true=on, false=off
     */
    protected Boolean citationEnabledGlobally = null;

    protected boolean isCitationEnabledGlobally() {
        return citationEnabledGlobally;
    }

    protected boolean isCitationEnabledThroughCollection(Context context, Bitstream bitstream) throws SQLException {
        //Reject quickly if no-enabled collections
        if (citationEnabledCollectionsList.isEmpty()) {
            return false;
        }

        DSpaceObject owningDSO = bitstreamService.getParentObject(context, bitstream);
        if (owningDSO instanceof Item) {
            Item item = (Item) owningDSO;

            List<Collection> collections = item.getCollections();

            for (Collection collection : collections) {
                if (citationEnabledCollectionsList.contains(collection.getHandle())) {
                    return true;
                }
            }
        }

        // If previous logic didn't return true, then we're false
        return false;
    }

    @Override
    public Boolean isCitationEnabledForBitstream(Bitstream bitstream, Context context) throws SQLException {
        if (isCitationEnabledGlobally() || isCitationEnabledThroughCollection(context, bitstream)) {

            boolean adminUser = authorizeService.isAdmin(context);

            if (!adminUser && canGenerateCitationVersion(context, bitstream)) {
                return true;
            }
        }

        // If previous logic didn't return true, then we're false.
        return false;
    }

    @Override
    public boolean canGenerateCitationVersion(Context context, Bitstream bitstream) throws SQLException {
        var item = (Item) bitstreamService.getParentObject(context, bitstream);
        return VALID_TYPES.contains(bitstream.getFormat(context).getMIMEType())
                && itemService.getMetadata(item, "dc.identifier.citation") != null;
    }

    @Override
    public Pair<byte[], Long> makeCitedDocument(Context context, Bitstream bitstream)
            throws IOException, SQLException {

        try (
                var result = new PDDocument();
                var source = loadDocumentFromDB(context, bitstream)
        ) {
            var item = (Item) bitstreamService.getParentObject(context, bitstream);
            var citation = itemService.getMetadata(item, "dc.identifier.citation");
            addCitationToDocument(result, source, citation);
            return documentAsBytes(result);
        }
    }

    private PDDocument loadDocumentFromDB(Context context, Bitstream bitstream) {
        try (var inputStream = bitstreamService.retrieve(context, bitstream)) {
            return PDDocument.load(inputStream);
        } catch (IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private static Pair<byte[], Long> documentAsBytes(PDDocument document) throws IOException {

        document.setAllSecurityToBeRemoved(true);

        //We already have the full PDF in memory, so keep it there
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.save(out);
            byte[] data = out.toByteArray();
            return Pair.of(data, (long) data.length);
        }
    }

    private void addCitationToDocument(PDDocument result, PDDocument source, String citation) throws IOException {
        for (PDPage page : source.getDocumentCatalog().getPages()) {
            PDPageContentStream contentStream = new PDPageContentStream(result, page, PDPageContentStream.AppendMode.APPEND, true, true);
            PDFont pdfFont = PDType1Font.HELVETICA_OBLIQUE;
            float fontSize = 8;
            float leading = 1.2f * fontSize;

            PDRectangle mediabox = page.getMediaBox();
            float marginX = 32;
            float marginY = 72;
            float width = mediabox.getHeight() - (2 * marginY);

            
            var lines = splitTextInLines(pdfFont, fontSize, width, citation);
            for (int i = 0; i < lines.size(); i++) {
                // We use absolute coordinates because thei are easier for centering text.
                // Nevertheless, after trying,  left-aligned text seems to be more aesthetic...
                Matrix matrix = Matrix.getRotateInstance(
                                        Math.toRadians(90),
                                        marginX + (leading * i),
                                        // (mediabox.getHeight() - getStringWidth(lines.get(i), pdfFont, fontSize)) / 2);
                                        marginY);
                contentStream.beginText();
                contentStream.setTextMatrix(matrix);
                contentStream.setFont(pdfFont, fontSize);
                contentStream.setNonStrokingColor(0.6f,0.6f,0.6f);
                contentStream.newLineAtOffset(0, 0);
                contentStream.showText(lines.get(i));
                contentStream.endText(); 
            }
            contentStream.close();
            result.addPage(page);
        }
    }

    private List<String> splitTextInLines(PDFont pdfFont, float fontSize, float width, String text) throws IOException {
        List<String> lines = new ArrayList<String>();
        int lastSpace = -1;
        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0) {
                spaceIndex = text.length();
            }
            String subString = text.substring(0, spaceIndex);
            float size = getStringWidth(subString, pdfFont, fontSize);
            if (size > width) {
                if (lastSpace < 0) {
                    lastSpace = spaceIndex;
                }
                subString = text.substring(0, lastSpace);
                lines.add(subString);
                text = text.substring(lastSpace).trim();
                lastSpace = -1;
            } else if (spaceIndex == text.length()) {
                lines.add(text);
                text = "";
            } else {
                lastSpace = spaceIndex;
            }
        }
        return lines;
    }

    private float getStringWidth(String text, PDFont font, float fontSize) throws IOException {
        return font.getStringWidth(text) * fontSize / 1000f;
    }
}