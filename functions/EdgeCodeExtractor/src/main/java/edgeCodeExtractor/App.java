package edgeCodeExtractor;

import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Map<String, String>, String> {
        public String handleRequest(final Map<String, String> input, final Context context) {
                String query = "SELECT distinct(location) FROM cloudfront_logs;";
                String database = "default";
                String outputLocation = "s3://" + input.get("edge_location_bucket");
                String region = input.get("region");

                AthenaClient client = AthenaClient.builder()
                                .region(Region.of(region))
                                .build();

                QueryExecutionContext queryExecutionContext = QueryExecutionContext.builder()
                                .database(database)
                                .build();

                ResultConfiguration resultConfiguration = ResultConfiguration.builder()
                                .outputLocation(outputLocation)
                                .build();

                StartQueryExecutionRequest startQueryExecutionRequest = StartQueryExecutionRequest.builder()
                                .queryString(query)
                                .queryExecutionContext(queryExecutionContext)
                                .resultConfiguration(resultConfiguration)
                                .build();

                StartQueryExecutionResponse startQueryExecutionResponse = client
                                .startQueryExecution(startQueryExecutionRequest);

                return "" + startQueryExecutionResponse.queryExecutionId() + ".csv";
        }
}
