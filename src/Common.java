import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.taskadapter.redmineapi.IssueManager;
import com.taskadapter.redmineapi.ProjectManager;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueCategory;
import com.taskadapter.redmineapi.bean.IssueFactory;
import com.taskadapter.redmineapi.bean.IssueStatus;
import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.User;
import com.taskadapter.redmineapi.bean.Version;
import com.taskadapter.redmineapi.bean.VersionFactory;

/**
 * This Class using for import IT result to Redmine
 *
 * @author BinhMTP
 * @version 1.0
 * @since 2016-08-20
 */
public class Common {
	String uri;
	String apiKey;
	String trackerName;
	int statusId;

	RedmineManager redmineMng;

	String SYMBOL_QUOTA = "";
	String SYMBOL_DOT = ".";
	String SYMBOL_HAIFUN = "_";
	String SYMBOL_PAREN_LEFT = "[";
	String SYMBOL_PAREN_RIGHT = "]";
	String SYMBOL_SLASH = "/";
	String SYMBOL_NEW_LINE = "\n";
	String TEST_RESULT_TYPE = "xml";
	String TRACKER_TEST_RESULT = "Test Result";
	String IMAGE_TYPE = "png";
	String TESTCASE_NODE_ATTRIBUTE = "name";
	String PASSED = "Passed";
	String FAILED = "Failed";
	String TIME_REGEX = "[-:ZT]+";
	String RELATION_TYPE = "relates";
	String TESTCASE = "testcase";
	String NODE_BUG = "exception";
	String STARTED_AT = "started-at";
	String BEFORETEST = "beforeTest";
	String AFTERTEST = "afterTest";
	String CUSTOMIZE_FIELD_TEST_RESULT = "Test Result";
	String TEST_METHOD = "test-method";
	String TEST_COMPLETED = "Testcompleted";
	String EVIDENCE = "/evidence/";
	String EVIDENCE_LOCATION = "images/evidence/";

	public Common(String uri, String apiKey) {

		this.uri = uri;
		this.apiKey = apiKey;
		redmineMng = RedmineManagerFactory.createWithApiKey(uri, apiKey);
		statusId = getStatusIdByName(TEST_COMPLETED);
		trackerName = TRACKER_TEST_RESULT;

	}

	/**
	 * This method is used to read IT result xml file
	 * 
	 * @param folderResultTest
	 * @param projectKey
	 * @param site
	 * @param folderFrom
	 * @param folderTo
	 */
	public void readResultIt(String fileResultTest, String projectKey, String site, String folderFrom,
			String folderTo) {

		int issueID;

		String issueSubject;
		String timeStamp;
		String issueContent;
		Node testCase;
		Node testCaseResult;

		NodeList testCaseList;
		NodeList testCaseResultList;

		Issue issue;
		User issueAssignee;
		IssueManager issueManager;
		ProjectManager projectManager;
		Element tesCaseElement;
		Document doc;
		DocumentBuilderFactory dbFactory;
		DocumentBuilder dBuilder;
		File fileResult;

		issueContent = SYMBOL_QUOTA;
		issueManager = redmineMng.getIssueManager();
		projectManager = redmineMng.getProjectManager();

		try {

			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
			fileResult = new File(fileResultTest);
			if (TEST_RESULT_TYPE.equals(getExtension(fileResult.getName(), SYMBOL_DOT))) {

				doc = dBuilder.parse(fileResult);
				doc.getDocumentElement().normalize();
				testCaseList = doc.getElementsByTagName(TEST_METHOD);

				for (int i = 0; i < testCaseList.getLength(); i++) {

					testCase = testCaseList.item(i);
					tesCaseElement = (Element) testCase;
					issueSubject = tesCaseElement.getAttribute(TESTCASE_NODE_ATTRIBUTE);

					if (!(BEFORETEST.equals(issueSubject) || AFTERTEST.equals(issueSubject))) {

						issueID = Integer.parseInt(getIssueID(issueSubject, SYMBOL_HAIFUN));
						testCaseResultList = testCase.getChildNodes();
						timeStamp = tesCaseElement.getAttribute(STARTED_AT);
						timeStamp = formatTimeStamp(timeStamp);

						for (int j = 0; j < testCaseResultList.getLength(); j++) {

							testCaseResult = testCaseResultList.item(j);

							if (testCaseResult.getNodeType() == Node.ELEMENT_NODE) {

								issue = issueManager.getIssueById(issueID);
								issueAssignee = issue.getAssignee();
								
								if (NODE_BUG.equals(testCaseResult.getNodeName())) {

									issueContent = testCaseResult.getTextContent();
									updateTestCaseIssue(issueManager, issueID, FAILED, statusId);
									String issueSubjectLog = SYMBOL_PAREN_LEFT + FAILED + SYMBOL_PAREN_RIGHT
											+ SYMBOL_PAREN_LEFT + timeStamp + SYMBOL_PAREN_RIGHT + issueSubject;
									createIssueIT(issueManager, projectManager, issueID, projectKey, issueSubjectLog,
											issueContent, issueAssignee, trackerName, issueSubject, site, folderFrom, folderTo,
											timeStamp, FAILED);

									break;

								} else {

									issueContent = SYMBOL_QUOTA;
									updateTestCaseIssue(issueManager, issueID, PASSED, statusId);
									String issueSubjectLog = SYMBOL_PAREN_LEFT + PASSED + SYMBOL_PAREN_RIGHT
											+ SYMBOL_PAREN_LEFT + timeStamp + SYMBOL_PAREN_RIGHT + issueSubject;
									createIssueIT(issueManager, projectManager, issueID, projectKey, issueSubjectLog,
											issueContent, issueAssignee, trackerName, issueSubject, site, folderFrom, folderTo,
											timeStamp, PASSED);
								}
							}
						}
					}
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

		}
	}

	/**
	 * This method is used to create issue
	 * 
	 * @param issueManager
	 * @param projectManager
	 * @param relatedIssueID
	 * @param projectKey
	 * @param issueSubjectLog
	 * @param assignee
	 * @param type
	 * @param issueSubject
	 * @param site
	 * @param folderFrom
	 * @param folderTo
	 * @param timeStamp
	 */
	public void createIssueIT(IssueManager issueManager, ProjectManager projectManager, int relatedIssueID,
			String projectKey, String issueSubjectLog, String issueContent, User assignee, String trackerName,
			String issueSubject, String site, String folderFrom, String folderTo, String timeStamp, String categoryName) {

		try {
			int issueID;
			IssueCategory category;
			Project projectByKey;
			Issue issue;
			Version ver;
			
			projectByKey = projectManager.getProjectByKey(projectKey);
			issue = IssueFactory.create(projectByKey.getId(), issueSubjectLog);
			ver = VersionFactory.create(512);
			issue.setTargetVersion(ver);
			issue.setTracker(projectByKey.getTrackerByName(trackerName));
			category = getCategoryByName(categoryName, projectByKey);
			issue.setCategory(category);
			issue.setProject(projectByKey);
			issue.setAssignee(assignee);
			issue.setDescription(updateDescrition(site, folderFrom, folderTo, issueSubject, issueContent, timeStamp));
			issueID = issueManager.createIssue(issue).getId();
			createRelation(issueManager, issueID, relatedIssueID);

		} catch (RedmineException | IOException e) {

			e.printStackTrace();

		}
	}

	/**
	 * This method is to create relation beetwenn UT Testcase & Bug
	 * 
	 * @param issueManager
	 * @param issueID
	 * @param relatedIssueID
	 */
	public void createRelation(IssueManager issueManager, int issueID, int relatedIssueID) {

		try {

			issueManager.createRelation(issueID, relatedIssueID, RELATION_TYPE);

		} catch (RedmineException e) {

			e.printStackTrace();

		}
	}

	/**
	 * This method is update descriptopn
	 * 
	 * @param site
	 * @param folderFrom
	 * @param folderTo
	 * @param issueSubject
	 * @param issueContent
	 * @param timeStamp
	 */
	public String updateDescrition(String site, String folderFrom, String folderTo, String issueSubject,
			String issueContent, String timeStamp) throws IOException {

		int year;
		int month;
		int randomUUIDString;

		String pathCheck;
		String pathCopy;
		String pathEvidence;
		String pathShowImage;
		String pathImage;

		Path pathFrom;
		Path pathTo;
		Path pathFolder;

		File fileCheck;
		File folder;

		Calendar calendar;
		Random random;

		calendar = Calendar.getInstance();
		year = calendar.get(Calendar.YEAR);
		month = calendar.get(Calendar.MONTH) + 1;

		pathCopy = folderFrom + year + SYMBOL_SLASH + month + SYMBOL_SLASH + issueSubject;
		pathEvidence = folderTo + EVIDENCE + year + SYMBOL_SLASH + month + SYMBOL_SLASH + issueSubject;
		pathShowImage = site + EVIDENCE_LOCATION + year + SYMBOL_SLASH + month + SYMBOL_SLASH + issueSubject
				+ SYMBOL_SLASH;
		pathImage = SYMBOL_QUOTA;
		pathFolder = Paths.get(pathEvidence);
		folder = new File(pathCopy);
		random = new Random();

		// if directory exists
		if (!Files.exists(pathFolder)) {

			Files.createDirectories(pathFolder);

		}
		for (File fileImg : folder.listFiles()) {
			randomUUIDString = random.nextInt(9);
			pathFrom = Paths.get(pathCopy + SYMBOL_SLASH + fileImg.getName());
			pathTo = Paths.get(pathFolder + SYMBOL_SLASH + randomUUIDString + timeStamp + fileImg.getName());
			pathCheck = pathEvidence + SYMBOL_SLASH + fileImg.getName();
			fileCheck = new File(pathCheck);

			if (fileCheck.exists() && !fileCheck.isDirectory()) {

				fileCheck.delete();

			}
			Files.copy(pathFrom, pathTo);
			pathImage = pathImage + pathShowImage + randomUUIDString + timeStamp + fileImg.getName() + SYMBOL_NEW_LINE;
		}
		return issueContent + SYMBOL_NEW_LINE + pathImage;

	}

	/**
	 * This method is to format timestamp
	 * 
	 * @param timestamp
	 */
	public String formatTimeStamp(String timeStamp) {
		return timeStamp.replaceAll(TIME_REGEX, SYMBOL_QUOTA);
	}

	/**
	 * This method is used to update status of issue
	 * 
	 * @param issueManager
	 * @param issueID
	 * @param content
	 * @param statusId
	 */
	public void updateTestCaseIssue(IssueManager issueManager, Integer issueID, String content, int statusId) {
		try {

			Issue issue = issueManager.getIssueById(issueID);
			issue.getCustomFieldByName(CUSTOMIZE_FIELD_TEST_RESULT).setValue(content);
			issue.setStatusId(statusId);
			issue.getRelations();
			issueManager.update(issue);

		} catch (RedmineException e) {

			e.printStackTrace();

		}
	}

	/**
	 * This method is to get extension of file
	 * 
	 * @param filename
	 * @param extensionSymbol
	 */
	public String getExtension(String filename, String extensionSymbol) {
		if (filename == null) {

			return null;

		}
		int extensionPos = filename.lastIndexOf(extensionSymbol);
		return filename.substring(extensionPos + 1);
	}

	/**
	 * This method is to get issueID from XML UT Test Result
	 * 
	 * @param filename
	 * @param extensionSymbol
	 */
	public String getIssueID(String filename, String extensionSymbol) {
		if (filename == null) {

			return null;

		}
		return filename.split(extensionSymbol)[1];
	}

	/**
	 * This method is get statisID by name
	 * 
	 * @param statusName
	 */
	public int getStatusIdByName(String statusName) {
		try {

			List<IssueStatus> statusList = redmineMng.getIssueManager().getStatuses();
			for (Iterator<IssueStatus> iter = statusList.iterator(); iter.hasNext();) {

				IssueStatus issueStatus = iter.next();
				if (statusName.equals(issueStatus.getName())) {
					return issueStatus.getId();
				}

			}

		} catch (RedmineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * This method is get category by name
	 * 
	 * @param categoryName
	 * @param projectByKey
	 */
	public IssueCategory getCategoryByName(String categoryName, Project projectByKey) {
		try {
			
			List<IssueCategory> catgoryList = redmineMng.getIssueManager().getCategories(projectByKey.getId());
			for (Iterator<IssueCategory> iter = catgoryList.iterator(); iter.hasNext();) {

				IssueCategory issueCategory = iter.next();
				if (categoryName.equals(issueCategory.getName())) {
					return issueCategory;
				}
			}

		} catch (RedmineException e) {
			e.printStackTrace();
		}
		return null;
	}
}
