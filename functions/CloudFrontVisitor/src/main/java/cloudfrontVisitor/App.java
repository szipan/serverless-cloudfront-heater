package cloudfrontVisitor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Map<String, String>, String> {

    public String handleRequest(final Map<String, String> input, final Context context) {
        LambdaLogger logger = context.getLogger();

        String region = input.get("region");
        String tableName = input.get("table_name");
        String distEdgeDomainName = input.get("distribution_edge_domain_name");
        AmazonDynamoDB ddbClient = AmazonDynamoDBClientBuilder.standard().withRegion(region).build();
        ScanRequest scanRequest = new ScanRequest().withTableName(tableName);
        ScanResult result = ddbClient.scan(scanRequest);
        StringBuilder url = new StringBuilder();

        List<Map<String, AttributeValue>> items = result.getItems();
        for (Map<String, AttributeValue> item : items) {
            url.append(distEdgeDomainName).append(item.get("s3Key").getS());
            try {
                this.access(url.toString());
            } catch (Exception e) {
                logger.log(e.getMessage());
            }
            url.setLength(0);
        }

        return "Finish warm up cache.";
    }

    private void access(String url) throws ClientProtocolException, IOException {
        HttpGet request = new HttpGet(url);
        CloseableHttpClient client = HttpClientBuilder.create().build();
        client.execute(request);
    }
}
