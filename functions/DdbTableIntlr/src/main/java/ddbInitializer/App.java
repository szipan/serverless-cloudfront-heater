package ddbInitializer;

import java.util.Map;
import java.util.HashMap;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Map<String, String>, String> {
    private AmazonS3 s3;
    private DynamoDB ddb;

    // Get all the keys under a certain prefix in a bucket, and insert all the keys
    // into Dynamodb.
    public String handleRequest(final Map<String, String> input, final Context context) {
        String coldFileBucketName = input.get("cold_file_bucket_name");
        String coldFilePrefix = input.get("cold_file_prefix");
        String region = input.get("region");
        String fileKeyTable = input.get("file_key_table");

        AmazonDynamoDB ddbClient = AmazonDynamoDBClientBuilder.standard().withRegion(region).build();
        ddb = new DynamoDB(ddbClient);

        Table table = this.initTable(fileKeyTable, context);

        Item item = null;

        // Get all keys under a S3 bucket and insert them into ddb.
        s3 = AmazonS3ClientBuilder.standard().withRegion(region).build();
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(coldFileBucketName)
                .withPrefix(coldFilePrefix);
        ListObjectsV2Result objects = s3.listObjectsV2(req);
        for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
            item = new Item()
                    .withString("s3Key", "/" + coldFileBucketName + "/" + objectSummary.getKey());
            table.putItem(item);
        }

        return "All keys under" + coldFileBucketName + '\\' + coldFilePrefix + " have been inserted into dynamodb.";
    }

    // If table exists, delete and re-create it, or else create it.
    private Table initTable(String tableName, final Context context) {
        LambdaLogger logger = context.getLogger();
        Table table = null;

        try {
            table = ddb.getTable(tableName);
            table.delete();
            table.waitForDelete();
        } catch (Exception e) {
            logger.log("Failed to delete table " + tableName + ". " + e.getMessage());
        }

        try {
            CreateTableRequest request = new CreateTableRequest()
                    .withKeySchema(new KeySchemaElement("s3Key", "HASH"))
                    .withAttributeDefinitions(new AttributeDefinition("s3Key", "S"))
                    .withProvisionedThroughput(new ProvisionedThroughput(new Long(10), new Long(10)))
                    .withTableName(tableName);

            table = ddb.createTable(request);
            table.waitForActive();
        } catch (Exception e) {
            logger.log("Failed to create table " + tableName + ". " + e.getMessage());
        }

        return table;
    }
}
