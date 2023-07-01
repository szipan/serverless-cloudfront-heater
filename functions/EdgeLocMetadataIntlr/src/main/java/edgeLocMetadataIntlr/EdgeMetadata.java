package edgeLocMetadataIntlr;

public class EdgeMetadata {
    public final static String CLOUDFRONT_EDGE_URL_HEADER = "distribution_edge_domain_name";

    public final static String REGION_HEADER = "region";

    public final static String TABLE_NAME_HEADER = "table_name";

    private String cloudFrontEdgeUrl;

    private String tableName;

    private String region;

    public EdgeMetadata() {
    }

    public EdgeMetadata(String cloudFrontEdgeUrl, String tableName, String region) {
        this.cloudFrontEdgeUrl = cloudFrontEdgeUrl;
        this.tableName = tableName;
        this.region = region;
    }

    public String getCloudFrontEdgeUrl() {
        return cloudFrontEdgeUrl;
    }

    public void setCloudFrontEdgeUrl(String cloudFrontEdgeUrl) {
        this.cloudFrontEdgeUrl = cloudFrontEdgeUrl;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(cloudFrontEdgeUrl).append(',').append(tableName).append(',').append(region);
        return sb.toString();
    }
}
