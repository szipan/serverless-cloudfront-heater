package edgeLocMetadataIntlr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Map<String, String>, String> {

    public String handleRequest(final Map<String, String> input, final Context context) {
        LambdaLogger logger = context.getLogger();

        String fileKeyTableRegion = input.get("region");
        String edgeLocBucket = input.get("edge_location_bucket");
        String edgeLocCodeListFileKey = input.get("edge_code_file_key");
        String edgeLocMetadataFileKey = input.get("edge_location_metadata_file_key");
        String fileKeyTable = input.get("file_key_table");
        String cloudfrontDistroUrl = input.get("distribution_domain_name");

        // Split a cf distribution domain name, using the first '.' as the delimeter.
        // E.g. d10ss1qk7pe123.cloudfront.net, prefix = d10ss1qk7pe123, suffix =
        // loudfront.net
        int firstDotIndex = cloudfrontDistroUrl.indexOf(".");
        String prefix = cloudfrontDistroUrl.substring(0, firstDotIndex);
        String suffix = cloudfrontDistroUrl.substring(firstDotIndex + 1);

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(fileKeyTableRegion).build();
        InputStream inputStream = null;
        BufferedReader reader = null;
        StringBuilder fileContent = new StringBuilder();

        // Build table headers
        fileContent.append(EdgeMetadata.CLOUDFRONT_EDGE_URL_HEADER).append(',').append(EdgeMetadata.TABLE_NAME_HEADER)
                .append(',').append(EdgeMetadata.REGION_HEADER).append('\n');

        try {
            S3Object s3object = s3Client.getObject(new GetObjectRequest(edgeLocBucket, edgeLocCodeListFileKey));
            inputStream = s3object.getObjectContent();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine(); // Skip the table header.
            line = reader.readLine();
            EdgeMetadata edgeMetadata = new EdgeMetadata();
            edgeMetadata.setRegion(fileKeyTableRegion);
            edgeMetadata.setTableName(fileKeyTable);

            while (line != null) {
                edgeMetadata.setCloudFrontEdgeUrl(prefix + '.' + line.trim() + '.' + suffix);
                logger.log("line: " + edgeMetadata.toString());
                fileContent.append(edgeMetadata.toString()).append('\n');
                line = reader.readLine();
            }
        } catch (IOException e) {
            logger.log(e.getMessage());
        } finally {
            try {
                if (reader != null)
                    reader.close();
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                logger.log(e.getMessage());
            }
        }

        // Write the file content out to S3 bucket.
        PutObjectResult result = s3Client.putObject(edgeLocBucket, edgeLocMetadataFileKey, fileContent.toString());

        return "Finish preparing edge location information. ";
    }
}
