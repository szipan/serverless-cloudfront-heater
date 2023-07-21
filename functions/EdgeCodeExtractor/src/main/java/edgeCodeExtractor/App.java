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
import software.amazon.awssdk.utils.StringUtils;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Map<String, String>, String> {
        public String handleRequest(final Map<String, String> input, final Context context) {
                String database = input.get("athena_database_name");
                if (StringUtils.isEmpty(database))
                        database = "default";
                String tableName = input.get("athena_table_name");
                if (StringUtils.isEmpty(tableName))
                        tableName = "cloudfront_logs";
                String query = "SELECT distinct(location) FROM " + tableName + ";";
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
