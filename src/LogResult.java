/**
 * This Class using for import IT result to Redmine
 *
 * @author BinhMTP
 * @version 1.0
 * @since 2016-08-20
 */
public class LogResult {
	public static void main(String[] args) {
		String site = args[0];
		String ApiKey = args[1];
		String fileResultTest = args[2];
		String projectKey = args[3];
		String uri = args[4];
		String folderFrom = args[5];
		String folderTo = args[6];
		Common common = new Common(site, ApiKey);
		common.readResultIt(fileResultTest, projectKey, uri, folderFrom, folderTo);
	}
}
